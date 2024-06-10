package org.kbroman.android.polarpvc2

import android.graphics.Color
import android.util.Log
import com.androidplot.xy.BoundaryMode
import com.androidplot.xy.LineAndPointFormatter
import com.androidplot.xy.SimpleXYSeries
import com.androidplot.xy.StepMode
import com.androidplot.xy.XYGraphWidget
import com.androidplot.xy.XYPlot
import com.androidplot.xy.XYRegionFormatter
import com.androidplot.xy.XYSeriesFormatter
import java.text.DecimalFormat
import java.text.FieldPosition
import java.text.Format
import java.text.ParsePosition
import java.text.SimpleDateFormat

class PVCplotter (private var mActivity: MainActivity?, private var Plot: XYPlot?) {
    private var yMax: Double = 40.0
    private var xMin: Double = Double.MAX_VALUE
    private var xMax: Double = -Double.MAX_VALUE

    companion object {
        private const val TAG = "PolarPVC2plotpvc"
        private const val N_TOTAL_POINTS: Int = 150*60*24*7   // maximum number of data points
    }

    private var formatterPVC: XYSeriesFormatter<XYRegionFormatter>? = null
    var seriesPVC: SimpleXYSeries? = null


    init {
        formatterPVC = LineAndPointFormatter(Color.rgb(0x00 , 0x74, 0xD9), // blue lines
            null, null, null)
        formatterPVC!!.setLegendIconEnabled(false)
        seriesPVC = SimpleXYSeries("PVC")

        Plot!!.getGraph().setLineLabelEdges(XYGraphWidget.Edge.LEFT, XYGraphWidget.Edge.BOTTOM)

        // round y-axis labels
        val df = DecimalFormat("#")
        Plot!!.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(df)

        // x-axis labels as times
        Plot!!.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat( object : Format() {
            private val formatter = SimpleDateFormat("HH:mm")

            override fun format(
                obj: Any?,
                toAppendTo: StringBuffer?,
                pos: FieldPosition?
            ): StringBuffer {
                var timestamp: Number = obj as? Number ?: 0.0
                return formatter.format(timestamp, toAppendTo, pos)
            }

            override fun parseObject(source: String, pos: ParsePosition): Object? {
                return null
            }
        })

        Plot!!.addSeries(seriesPVC, formatterPVC)
        setupPlot()
    }

    fun setupPlot() {
        try {
            // frequency of x- and y-axis lines
            Plot!!.setDomainStep(StepMode.INCREMENT_BY_VAL, 60.0)
            Plot!!.setRangeStep(StepMode.INCREMENT_BY_VAL, 10.0)

            update()
        } catch (ex: Exception) {
            Log.e(TAG, "Problem setting up pvc plot")
        }
    }

    fun getNewInstance(activity: MainActivity, plot: XYPlot): PVCplotter {
        val newPlotter = PVCplotter(activity, plot)
        newPlotter.Plot = plot
        newPlotter.mActivity = this.mActivity

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



    fun addValues(time: Double, pvc: Double) {
        if (time != null && pvc != null) {
            if (seriesPVC!!.size() >= N_TOTAL_POINTS) {
                seriesPVC!!.removeFirst()
            }
            seriesPVC!!.addLast(time, pvc)

            if(pvc > yMax) { yMax = pvc }
            if(time > xMax) { xMax = time }
            if(time < xMin) { xMin = time }
        }

        update()
    }

    fun updateBoundaries() {
        Plot!!.setDomainBoundaries(xMin, xMax, BoundaryMode.FIXED)
        Plot!!.setDomainStep(StepMode.INCREMENT_BY_VAL, domainLines())

        Plot!!.setRangeBoundaries(0.0, yMax, BoundaryMode.FIXED)
    }

    fun update() {
        updateBoundaries()
        mActivity!!.runOnUiThread { Plot!!.redraw() }
    }

    fun clear() {
        seriesPVC!!.clear()
        update()
    }

    fun domainLines(): Double {
        val timespan_min = (xMax - xMin)/60.0

        return when {  // returns time in seconds
            timespan_min <= 5.0  -> 60.0
            timespan_min <= 20.0 -> 300.0
            timespan_min <= 120.0 -> 900.0
            timespan_min <= 480.0 -> 1800.0
            else -> 3600.0
        }
    }
}
