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

class PVCplotter (private var mActivity: MainActivity?, private var Plot: XYPlot?) {
    private var nData: Long = 0
    companion object {
        private const val TAG = "PolarPVC2plotpvc"
        private const val N_TOTAL_POINTS: Int = 150*60*24   // maximum number of data points
    }

    private var formatterPVC: XYSeriesFormatter<XYRegionFormatter>? = null
    var seriesPVC: SimpleXYSeries? = null


    init {
        nData = 0L
        formatterPVC = LineAndPointFormatter(Color.rgb(0x00 , 0x74, 0xD9), // blue lines
            null, null, null)
        seriesPVC = SimpleXYSeries("PVC")

        Plot!!.addSeries(seriesPVC, formatterPVC)
        setupPlot()
    }

    fun setupPlot() {
        try {
            // PVC range (y-axis)
            Plot!!.setRangeBoundaries(0.0, 40.0, BoundaryMode.FIXED)
            Plot!!.setRangeStep(StepMode.INCREMENT_BY_VAL, 5.0)

            // domain (x-axis)
            updateDomainBoundaries()

            update()
        } catch (ex: Exception) {
            Log.e(TAG, "Problem setting up pvc plot")
        }
    }

    fun getNewInstance(activity: MainActivity, plot: XYPlot): PVCplotter {
        val newPlotter = PVCplotter(activity, plot)
        newPlotter.Plot = plot
        newPlotter.mActivity = this.mActivity
        newPlotter.nData = this.nData

        newPlotter.formatterPVC = this.formatterPVC
        newPlotter.seriesPVC = this.seriesPVC

        try {
            newPlotter.Plot!!.addSeries(seriesPVC, formatterPVC)
            newPlotter.setupPlot()
        } catch (ex: Exception) {
            Log.e(TAG, "trouble setting up new pvc plot")
        }

        return newPlotter
    }



    fun addValues(time: Double?, pvc: Double?) {
        if (time != null && pvc != null) {
            if (seriesPVC!!.size() >= N_TOTAL_POINTS) {
                seriesPVC!!.removeFirst()
            }
            seriesPVC!!.addLast(time, pvc)
            nData++
        }

        // Reset the domain boundaries
        updateDomainBoundaries()
        update()
    }

    fun updateDomainBoundaries() {
        val xMax: Double = seriesPVC!!.getxVals().getLast().toDouble()
        val xMin: Double = seriesPVC!!.getxVals().getFirst().toDouble()

        Plot!!.setDomainBoundaries(xMin, xMax, BoundaryMode.FIXED)
        Plot!!.setDomainStep(StepMode.INCREMENT_BY_VAL, 60*15.0)
    }

    fun update() {
        mActivity!!.runOnUiThread { Plot!!.redraw() }
    }

    fun clear() {
        nData = 0
        seriesPVC!!.clear()
        update()
    }
}
