package org.kbroman.android.polarpvc2

import android.util.Log
import com.polar.sdk.api.model.PolarEcgData
import kotlin.math.max
import kotlin.math.min

class PeakDetection(var mActivity: MainActivity) {

    companion object {
        private const val TAG = "PolarPVC2peaks"
        private const val N_ECG_VALS = 130*60*60
        private const val N_PEAKS = 60*60*2
        private const val N_PEAKS_FOR_RR_AVE = 25
        private const val N_PEAKS_FOR_PVC_AVE = 100
        private const val PVC_RS_DIST: Double = 5.0
        private const val INITIAL_PEAKS_TO_SKIP = 2
        private const val INITIAL_ECG_TO_SKIP = 500
        private const val MIN_PEAK_VALUE: Double = 1.5
        private const val HR_200_INTERVAL: Int = 39  // = (60.0/200.0*130)
        private const val MOVING_AVESD_WINDOW: Int = 500
        const val TIMESTAMP_OFFSET: Long = 946684800000000000
        // for time offset, see https://github.com/polarofficial/polar-ble-sdk/blob/master/documentation/TimeSystemExplained.md
    }

    var ecgData: ECGdata = ECGdata(N_ECG_VALS)
    var pvcData: RunningAverage = RunningAverage(N_PEAKS_FOR_PVC_AVE)
    var rrData: RunningAverage = RunningAverage(N_PEAKS_FOR_RR_AVE)
    private var peakIndexes = FixedSizedList<Int>(N_PEAKS)
    private var movingAveSDecg = RunningAveSD(MOVING_AVESD_WINDOW)
    private var last_smsqdiff: Double = -Double.MAX_VALUE
    private var smsqdiff = ArrayList<Double>()
    private var lastPeakIndex: Int = -1
    private var thisPeakIndex: Int = -1

    fun processData(polarEcgData: PolarEcgData) {
        val end: Int

        // last index
        var start = ecgData.maxIndex()

        // grab a batch of data
        for (data in polarEcgData.samples) {
            val voltage : Double = (data.voltage.toFloat() / 1000.0)
            val timestamp = data.timeStamp + TIMESTAMP_OFFSET

            ecgData.add(timestamp, voltage)
            mActivity.ecgPlotter!!.addValues(timestamp/1e9, voltage)
        }
        val n: Int = ecgData.maxIndex() - start

        if (ecgData.maxIndex() < INITIAL_ECG_TO_SKIP) return  // wait to start looking for peaks

        // look for peaks in first half
        end = start + n / 2
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

            movingAveSDecg.add(smsqdiff[i]) // get running mean and SD
        }

        // find maximum
        val max_index = which_max(smsqdiff)
        val this_smsqdiff: Double = smsqdiff[max_index]
        thisPeakIndex = max_index + start

        var peakFound = false

        if ((this_smsqdiff - movingAveSDecg.average()) / movingAveSDecg.sd() >= MIN_PEAK_VALUE) { // peak only if large
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
                    Log.i(TAG,"adjusted peak")
                }
            }
        }

        // now look at whether penultimate peak was a PVC
        if (peakFound && peakIndexes.size() > INITIAL_PEAKS_TO_SKIP) {
            val lastPeakIndex: Int = peakIndexes.get(peakIndexes.size() - 2)
            val thisPeakIndex: Int = peakIndexes.get(peakIndexes.size() - 1)

            val temp_ecg = ArrayList<Double>()

            val endSearch =
                min((thisPeakIndex - lastPeakIndex).toDouble() / 2.0, 20.0).toInt()

            for (i in 1 until endSearch) temp_ecg.add(ecgData.volt.get(lastPeakIndex + i))
            val minPeakIndex = which_min(temp_ecg)

            if (minPeakIndex >= PVC_RS_DIST) { // looks like a PVC
                pvcData.add(1.0)
                pvcData.lastTime = ecgData.time.get(lastPeakIndex)/1e9
                Log.wtf(TAG, "*** PVC ***")
                mActivity.ecgPlotter!!.addPVCValue(ecgData.time.get(lastPeakIndex)/1e9, ecgData.volt.get(lastPeakIndex))
            } else {                          // not a PVC
                pvcData.add(0.0)
                pvcData.lastTime = ecgData.time.get(lastPeakIndex)/1e9
                Log.i(TAG, "not PVC")
            }

            // get RR distance based on timestamps, in seconds
            rrData.add(
                (ecgData.time.get(peakIndexes.get(peakIndexes.size() - 2)) -
                        ecgData.time.get(peakIndexes.get(peakIndexes.size() - 3)) )*1e-9

            )
            rrData.lastTime = ecgData.time.get(lastPeakIndex)/1e9

            val hr_bpm = 60.0 / rrData.average()
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
}
