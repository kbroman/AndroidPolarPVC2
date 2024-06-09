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

class HRplotter (private var mActivity: MainActivity?, private var Plot: XYPlot?) {
    private var nData: Long = 0
    companion object {
        private const val TAG = "PolarPVC2plotpvc"
        private const val N_TOTAL_POINTS: Int = 150*60*24   // maximum number of data points
    }

    private var formatterHR: XYSeriesFormatter<XYRegionFormatter>? = null
    var seriesHR: SimpleXYSeries? = null


    init {
        nData = 0L
        formatterHR = LineAndPointFormatter(Color.rgb(0xFF , 0x41, 0x36), // red lines
            null, null, null)
        seriesHR = SimpleXYSeries("HR")

        Plot!!.addSeries(seriesHR, formatterHR)
        setupPlot()
    }

    fun setupPlot() {
        try {
            // HR range (y-axis)
            Plot!!.setRangeBoundaries(50.0, 135.0, BoundaryMode.FIXED)
            Plot!!.setRangeStep(StepMode.INCREMENT_BY_VAL, 10.0)

            // domain (x-axis)
            updateDomainBoundaries()

            update()
        } catch (ex: Exception) {
            Log.e(TAG, "Problem setting up hr plot")
        }
    }

    fun getNewInstance(activity: MainActivity, plot: XYPlot): HRplotter {
        val newPlotter = HRplotter(activity, plot)
        newPlotter.Plot = plot
        newPlotter.mActivity = this.mActivity
        newPlotter.nData = this.nData

        newPlotter.formatterHR = this.formatterHR
        newPlotter.seriesHR = this.seriesHR

        try {
            newPlotter.Plot!!.addSeries(seriesHR, formatterHR)
            newPlotter.setupPlot()
        } catch (ex: Exception) {
            Log.e(TAG, "trouble setting up new hr plot")
        }

        return newPlotter
    }



    fun addValues(time: Double?, hr: Double?) {
        if (time != null && hr != null) {
            if (seriesHR!!.size() >= N_TOTAL_POINTS) {
                seriesHR!!.removeFirst()
            }
            seriesHR!!.addLast(time, hr)
            nData++
        }

        // Reset the domain boundaries
        updateDomainBoundaries()
        update()
    }

    fun updateDomainBoundaries() {
        val xMax: Double = seriesHR!!.getxVals().getLast().toDouble()
        val xMin: Double = seriesHR!!.getxVals().getFirst().toDouble()

        Plot!!.setDomainBoundaries(xMin, xMax, BoundaryMode.FIXED)
        Plot!!.setDomainStep(StepMode.INCREMENT_BY_VAL, 60*15.0)
    }

    fun update() {
        mActivity!!.runOnUiThread { Plot!!.redraw() }
    }

    fun clear() {
        nData = 0
        seriesHR!!.clear()
        update()
    }
}
