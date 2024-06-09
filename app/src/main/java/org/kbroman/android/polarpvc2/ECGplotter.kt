package org.kbroman.android.polarpvc2

import android.graphics.Color
import android.util.Log
import com.androidplot.util.PixelUtils
import com.androidplot.xy.BoundaryMode
import com.androidplot.xy.LineAndPointFormatter
import com.androidplot.xy.SimpleXYSeries
import com.androidplot.xy.StepMode
import com.androidplot.xy.XYPlot
import com.androidplot.xy.XYRegionFormatter
import com.androidplot.xy.XYSeriesFormatter

class ECGplotter (private var mActivity: MainActivity?, private var mPlot: XYPlot?) {
    private var nData: Long = 0
    companion object {
        private const val TAG = "PolarPVC2plot"
        private const val SEC_TO_PLOT: Double = 8.0   // Show this many seconds
        private const val N_TOTAL_POINTS: Int = 130*8   // corresponding number of ECG data points
    }

    // ECG
    private var mFormatter1: XYSeriesFormatter<XYRegionFormatter>? = null
    var mSeries1: SimpleXYSeries? = null

    // Peaks
    private var mFormatter2: XYSeriesFormatter<XYRegionFormatter>? = null
    var mSeries2: SimpleXYSeries? = null

    // PVC
    private var mFormatter3: XYSeriesFormatter<XYRegionFormatter>? = null
    var mSeries3: SimpleXYSeries? = null


    init {
        nData = 0L
        mFormatter1 = LineAndPointFormatter( Color.rgb(0x11, 0x11, 0x11),  // black lines
            null, null, null)
        mFormatter1!!.setLegendIconEnabled(false)
        mSeries1 = SimpleXYSeries("ECG")

        mFormatter2 = LineAndPointFormatter(null, Color.rgb(0xFF, 0x41, 0x36), // red points
            null, null) // red color
        mFormatter2!!.setLegendIconEnabled(false)
        mSeries2 = SimpleXYSeries("Peaks")

        mFormatter3 = LineAndPointFormatter(null, Color.rgb(0x00, 0x74, 0xD9), // blue points
            null, null)
        mFormatter3!!.setLegendIconEnabled(false)
        mSeries3 = SimpleXYSeries("PVC")

        mPlot!!.addSeries(mSeries1, mFormatter1)
        mPlot!!.addSeries(mSeries2, mFormatter2)
        mPlot!!.addSeries(mSeries3, mFormatter3)
        setupPlot()
    }

    fun setupPlot() {
        try {
            // range (y-axis)
            mPlot!!.setRangeBoundaries(-1.5, 1.5, BoundaryMode.FIXED)
            mPlot!!.setRangeStep(StepMode.INCREMENT_BY_VAL, 0.5)
            mPlot!!.setUserRangeOrigin(0.0)

            // domain (x-axis)
            updateDomainBoundaries()

            update()
        } catch (ex: Exception) {
            Log.e(TAG, "Problem setting up plot")
        }
    }

    fun getNewInstance(activity: MainActivity, plot: XYPlot): ECGplotter {
        val newPlotter = ECGplotter(activity, plot)
        newPlotter.mPlot = plot
        newPlotter.mActivity = this.mActivity
        newPlotter.nData = this.nData

        newPlotter.mFormatter1 = this.mFormatter1
        newPlotter.mSeries1 = this.mSeries1

        newPlotter.mFormatter2 = this.mFormatter2
        newPlotter.mSeries2 = this.mSeries2

        newPlotter.mFormatter3 = this.mFormatter3
        newPlotter.mSeries3 = this.mSeries3


        try {
            newPlotter.mPlot!!.addSeries(mSeries1, mFormatter1)
            newPlotter.mPlot!!.addSeries(mSeries2, mFormatter2)
            newPlotter.mPlot!!.addSeries(mSeries3, mFormatter3)
            newPlotter.setupPlot()
        } catch (ex: Exception) {
            Log.e(TAG, "trouble setting up new plot")
        }

        return newPlotter
    }



    fun addValues(time: Double?, volt: Double?) {
        if (time != null && volt != null) {
            if (mSeries1!!.size() >= N_TOTAL_POINTS) {
                mSeries1!!.removeFirst()
            }
            mSeries1!!.addLast(time, volt)
            nData++
        }

        // Reset the domain boundaries
        updateDomainBoundaries()
        update()
    }

    fun addPeakValue(time: Double?, volt: Double?) {
        removeOutOfRangeValues()
        mSeries2!!.addLast(time, volt)
        update()
    }

    fun replaceLastPeakValue(time: Double?, volt: Double?) {
        removeOutOfRangeValues()
        mSeries2!!.removeLast()
        mSeries2!!.addLast(time, volt)

        update()
    }

    fun addPVCValue(time: Double?, volt: Double?) {
        removeOutOfRangeValues()
        mSeries3!!.addLast(time, volt)
        update()
    }

    fun removeOutOfRangeValues() {
        var xMin: Double = mSeries1!!.getxVals().getLast().toDouble() - SEC_TO_PLOT

        // Remove old values if needed
        while (mSeries2!!.size() > 0 && mSeries2!!.getxVals().getFirst().toDouble() < xMin) {
            mSeries2!!.removeFirst()
        }

        while (mSeries3!!.size() > 0 && mSeries3!!.getxVals().getFirst().toDouble() < xMin) {
            mSeries3!!.removeFirst()
        }
    }

    fun updateDomainBoundaries() {
        val xMax: Double = mSeries1!!.getxVals().getLast().toDouble()
        val xMin: Double = xMax - SEC_TO_PLOT

        mPlot!!.setDomainBoundaries(xMin, xMax, BoundaryMode.FIXED)
        mPlot!!.setDomainStep(StepMode.INCREMENT_BY_VAL, 1.0)
    }

    fun update() {
        if (nData % 73 == 0L) {
            mActivity!!.runOnUiThread { mPlot!!.redraw() }
        }
    }

    fun clear() {
        nData = 0
        mSeries1!!.clear()
        mSeries2!!.clear()
        mSeries3!!.clear()
        update()
    }
}
