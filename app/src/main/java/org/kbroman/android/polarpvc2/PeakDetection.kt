package org.kbroman.android.polarpvc2

import android.util.Log
import com.polar.sdk.api.model.PolarEcgData
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

class PeakDetection(var mActivity: MainActivity) {

    companion object {
        private const val TAG = "PolarPVC2peaks"
        private const val N_ECG_VALS: Int = 130*60*5
        private const val N_PEAKS: Int = 150*5
        private const val N_PEAKS_FOR_RR_AVE = 25
        private const val N_PEAKS_FOR_PVC_AVE = 100
        private const val PVC_RS_DIST: Double = 5.0
        private const val PVC_TEST_STAT_THRESH: Double = 0.78
        private const val INITIAL_PEAKS_TO_SKIP = 4
        private const val INITIAL_ECG_TO_SKIP = 500
        private const val MIN_PEAK_VALUE: Double = 1.5
        private const val HR_200_INTERVAL: Int = 39  // = (60.0/200.0*130)
        private const val MAX_RR_SEC: Double = 60.0/35.0
        private const val MOVING_AVESD_WINDOW: Int = 500
        const val TIMESTAMP_OFFSET: Long = 946684800000000000
        // for time offset, see https://github.com/polarofficial/polar-ble-sdk/blob/master/documentation/TimeSystemExplained.md
    }

    var ecgData: ECGdata = ECGdata(N_ECG_VALS)
    var pvcData: RunningAverage = RunningAverage(N_PEAKS_FOR_PVC_AVE)
    var rrData: RunningAverage = RunningAverage(N_PEAKS_FOR_RR_AVE)
    private var peakIndexes = FixedSizedList<Int>(N_PEAKS)
    private var movingAveSDsmsqdiff = RunningAveSD(MOVING_AVESD_WINDOW)
    private var last_smsqdiff: Double = -Double.MAX_VALUE
    private var smsqdiff = ArrayList<Double>()
    private var lastPeakIndex: Int = -1
    private var thisPeakIndex: Int = -1

    fun processData(polarEcgData: PolarEcgData) {
        // last index
        var start: Int = ecgData.maxIndex()

        // grab a batch of data
        for (data in polarEcgData.samples) {
            val voltage : Double = (data.voltage.toFloat() / 1000.0)
            val timestamp = data.timeStamp + TIMESTAMP_OFFSET

            ecgData.add(timestamp, voltage)
            mActivity.ecgPlotter!!.addValues(timestamp/1e9, voltage)
        }
        val n = ecgData.maxIndex() - start

        if (ecgData.maxIndex() < INITIAL_ECG_TO_SKIP) return  // wait to start looking for peaks

        // look for peaks in first half
        var end = start + n / 2
        start = max(0.0, (start - 5).toDouble()).toInt()
        find_peaks(start, end)

        // look for peaks in second half
        start = max(0.0, (end - 5).toDouble()).toInt()
        find_peaks(start, ecgData.maxIndex())
    }

    private fun find_peaks(start: Int, end: Int) {
        smsqdiff.clear()
        // get squared differences
        for (i in start until end - 1) {
            val diff: Double = (ecgData.volt.get(i) - ecgData.volt.get(i + 1))
            smsqdiff.add(diff * diff)
        }
        smsqdiff.add(0.0)
        smsqdiff.add(0.0)

        // smooth in groups of three
        for (i in 0 until smsqdiff.size - 2) {
            smsqdiff[i] = (smsqdiff[i] + smsqdiff[i + 1] + smsqdiff[i + 2]) / 3.0

            movingAveSDsmsqdiff.add(smsqdiff[i]) // get running mean and SD
        }

        // find maximum
        val max_index = which_max(smsqdiff)
        val this_smsqdiff: Double = smsqdiff[max_index]
        thisPeakIndex = max_index + start

        var peakFound = false

        if ((this_smsqdiff - movingAveSDsmsqdiff.average()) / movingAveSDsmsqdiff.sd() >= MIN_PEAK_VALUE) { // peak only if large
            if (peakIndexes.size() == 0 || thisPeakIndex - lastPeakIndex >= HR_200_INTERVAL) { // new peak
                peakFound = true
                last_smsqdiff = this_smsqdiff
                lastPeakIndex = thisPeakIndex
                peakIndexes.add(thisPeakIndex)
                mActivity.ecgPlotter!!.addPeakValue(ecgData.time.get(thisPeakIndex)/1e9, ecgData.volt.get(thisPeakIndex))
            } else { // too close to previous peak
                if (this_smsqdiff > last_smsqdiff) {
                    last_smsqdiff = this_smsqdiff
                    lastPeakIndex = thisPeakIndex
                    peakIndexes.setLast(thisPeakIndex)
                    mActivity.ecgPlotter!!.replaceLastPeakValue(ecgData.time.get(thisPeakIndex)/1e9, ecgData.volt.get(thisPeakIndex))
                    Log.d(TAG,"adjusted peak")
                }
            }
        }

        // now look at whether penultimate peak was a PVC
        if (peakFound && peakIndexes.size() > INITIAL_PEAKS_TO_SKIP) {
            val prevPeakIndex = peakIndexes.thirdLastValue()
            val lastPeakIndex = peakIndexes.secondLastValue()
            val thisPeakIndex = peakIndexes.lastValue()

            val temp_ecg = ArrayList<Double>()

            val endSearch =
                min((thisPeakIndex - lastPeakIndex).toDouble() / 2.0, 20.0).toInt()

            for (i in 1 until endSearch) temp_ecg.add(ecgData.volt.get(lastPeakIndex + i))
            val minPeakIndex = which_min(temp_ecg)  // compare to PVC_RS_DIST
            val pvcTestStat = calcPVCTestStat(temp_ecg) // compare to PVC_TEST_STAT_THRESH
            Log.d(TAG, "minPeakIndex: $minPeakIndex   pvcTestStat: ${myround(pvcTestStat, 4)}")

            if (pvcTestStat > PVC_TEST_STAT_THRESH) { // looks like a PVC
                pvcData.add(1.0)
                pvcData.lastTime = ecgData.time.get(lastPeakIndex)/1e9
                Log.wtf(TAG, "*** PVC ***")
                mActivity.ecgPlotter!!.addPVCValue(ecgData.time.get(lastPeakIndex)/1e9, ecgData.volt.get(lastPeakIndex))
            } else {                          // not a PVC
                pvcData.add(0.0)
                pvcData.lastTime = ecgData.time.get(lastPeakIndex)/1e9
                Log.d(TAG, "not PVC")
            }

            // get RR distance based on timestamps, in seconds
            val rr: Double = (ecgData.time.get(lastPeakIndex) - ecgData.time.get(prevPeakIndex) )/1e9
            if(rr < MAX_RR_SEC) {
                rrData.add(rr)
                rrData.lastTime = ecgData.time.get(lastPeakIndex) / 1e9
            } else {
                Log.d(TAG, "Ignoring RR = ${myround(rr, 2)} sec")
            }
        }
    }


    private fun which_max(v: ArrayList<Double>): Int {
        if (v.isEmpty()) return (-1)

        val n = v.size
        if (n == 1) return (0)

        var max = v[0]
        var max_index = 0

        for (i in 1 until n) {
            if (v[i] > max) {
                max_index = i
                max = v[i]
            }
        }

        return (max_index)
    }

    private fun which_min(v: ArrayList<Double>): Int {
        if (v.isEmpty()) return (-1)

        val n = v.size
        if (n == 1) return (0)

        var min = v[0]
        var min_index = 0

        for (i in 1 until n) {
            if (v[i] < min) {
                min_index = i
                min = v[i]
            }
        }

        return (min_index)
    }

    // test statistic for determining PVC
    //    as proportion of values (from peak to half-way to next peak) that are below
    //    the mid-point of the range of those values
    private fun calcPVCTestStat(v: ArrayList<Double>): Double {
        var n = v.size
        var count: Int = 0

        if(n==0) return(0.0)

        var min = v[0]
        var max = v[0]

        for(vv in v) {
            if(vv < min) min = vv
            if(vv > max) max = vv
        }

        var mid_range = (max + min)/2.0


        for (vv in v) {
            if(vv < mid_range) count++
        }
        return count.toDouble() / n.toDouble()
    }

    fun clear() {
        pvcData.clear()
        rrData.clear()
        peakIndexes.clear()
        lastPeakIndex = -1
        thisPeakIndex = -1
    }
}
