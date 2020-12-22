package com.example.test3.account_view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.example.test3.R
import com.example.test3.account_manager.*
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class RatingGraphView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap


    private lateinit var accountManager: RatedAccountManager

    fun setManager(ratedAccountManager: RatedAccountManager){
        accountManager = ratedAccountManager
    }

    private var ratingHistory: List<RatingChange> = listOf(RatingChange(0,0L))
    private var toShow = ratingHistory
    lateinit var ratingHistoryLast10: List<RatingChange>
    lateinit var ratingHistoryLastMonth: List<RatingChange>
    lateinit var ratingHistoryLastYear: List<RatingChange>

    fun setHistory(history: List<RatingChange>) {
        ratingHistory = history
        toShow = ratingHistory

        ratingHistoryLast10 = history.takeLast(10)

        val currentSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
        ratingHistoryLastMonth = history.filter { ratingChange ->
            TimeUnit.SECONDS.toDays(currentSeconds - ratingChange.timeSeconds) <= 30
        }

        ratingHistoryLastYear = history.filter { ratingChange ->
            TimeUnit.SECONDS.toDays(currentSeconds - ratingChange.timeSeconds) <= 365
        }

        drawRating()
        invalidate()
    }

    fun showAll(){
        toShow = ratingHistory
        drawRating()
        invalidate()
    }

    fun showLast10(){
        toShow = ratingHistoryLast10
        drawRating()
        invalidate()
    }

    fun showLastMonth(){
        toShow = ratingHistoryLastMonth
        drawRating()
        invalidate()
    }

    fun showLastYear(){
        toShow = ratingHistoryLastYear
        drawRating()
        invalidate()
    }

    private fun drawRating(){

        if(width == 0|| height == 0) return

        if (::extraBitmap.isInitialized) extraBitmap.recycle()
        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
        extraCanvas.scale(1f, -1f, width/2f, height/2f)

        val minY = toShow.minOf { it.rating } - 100f
        val maxY = toShow.maxOf { it.rating } + 100f
        val minX = toShow.minOf { it.timeSeconds }.toFloat()
        val maxX = toShow.maxOf { it.timeSeconds }.toFloat()

        val circleRadius = 8f
        val circleStroke = 3f

        val m = Matrix()
        if(minX == maxX){
            val bound = TimeUnit.DAYS.toSeconds(1).toFloat()
            m.preScale(width/(bound*2), height/(maxY-minY))
            m.preTranslate(-(minX-bound), -minY)
        }else{
            m.preScale(width/(width+(circleRadius+circleStroke)*3), 1f, width/2f, height/2f)
            m.preScale(width/(maxX-minX), height/(maxY-minY))
            m.preTranslate(-minX, -minY)
        }

        //rating stripes
        val ratingBounds = accountManager.ratingsUpperBounds.toMutableList()
        ratingBounds.add(Pair(Int.MAX_VALUE, HandleColor.RED))
        ratingBounds.reversed().forEachIndexed { index, (upper, ratingColor) ->

            val y = if(index == 0) height.toFloat() else {
                val arr = floatArrayOf(0f, upper.toFloat())
                m.mapPoints(arr)
                arr[1]
            }

            extraCanvas.drawRect(0f, 0f, width.toFloat(), y, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ratingColor.getARGB(accountManager)
                style = Paint.Style.FILL
            })
        }

        //rating path
        val pathWidth = 4f
        val path = Path()
        ratingHistory.forEachIndexed { index, ratingChange ->
            val arr = floatArrayOf(ratingChange.timeSeconds.toFloat(), ratingChange.rating.toFloat())
            m.mapPoints(arr)
            val (x,y) = arr
            if(index == 0) path.moveTo(x, y)
            else path.lineTo(x, y)
        }

        //path shadow
        val shadowColor = 0x66000000
        val shadowX = 4f
        val shadowY = -4f
        val pathShadow = Path()
        pathShadow.addPath(path, shadowX, shadowY)
        extraCanvas.drawPath(pathShadow, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = shadowColor
            style = Paint.Style.STROKE
            strokeWidth = pathWidth
        })

        ratingHistory.forEach { ratingChange ->
            val arr = floatArrayOf(ratingChange.timeSeconds.toFloat(), ratingChange.rating.toFloat())
            m.mapPoints(arr)
            val (x,y) = arr

            //circle shadow
            extraCanvas.drawCircle(x+shadowX, y+shadowY, circleRadius+circleStroke/2, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = shadowColor
            })
        }

        extraCanvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = pathWidth
        })

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.BLACK
            strokeWidth = circleStroke
        }

        //rating points
        ratingHistory.forEach { ratingChange ->
            val arr = floatArrayOf(ratingChange.timeSeconds.toFloat(), ratingChange.rating.toFloat())
            m.mapPoints(arr)
            val (x,y) = arr

            //circle inner
            extraCanvas.drawCircle(x, y, circleRadius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = accountManager.getHandleColorARGB(ratingChange.rating)
            })

            //circle outer
            extraCanvas.drawCircle(x, y, circleRadius, circlePaint)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        drawRating()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(extraBitmap, 0f, 0f, null)
    }


    companion object {
        suspend fun showInAccountViewFragment(fragment: AccountViewFragment, manager: RatedAccountManager){

            val view = fragment.requireView().findViewById<ConstraintLayout>(R.id.account_view_rating_graph_view)

            val info = manager.getSavedInfo()
            if(info.status != STATUS.OK || manager.getRating(info) == NOT_RATED){
                view.visibility = View.GONE
                return
            }

            val ratingGraphView = view.findViewById<RatingGraphView>(R.id.account_view_rating_graph).apply {
                setManager(manager)
                visibility = View.GONE
            }

            val buttonAll = view.findViewById<MaterialButton>(R.id.account_view_rating_graph_button_all)
            val buttonLast10 = view.findViewById<MaterialButton>(R.id.account_view_rating_graph_button_last10)
            val buttonLastMonth = view.findViewById<MaterialButton>(R.id.account_view_rating_graph_button_last_month)
            val buttonLastYear = view.findViewById<MaterialButton>(R.id.account_view_rating_graph_button_last_year)

            listOf(buttonAll, buttonLast10, buttonLastMonth, buttonLastYear).forEach { button -> button.visibility = View.GONE }


            view.findViewById<TextView>(R.id.account_view_rating_graph_title).apply {
                text = "Show rating graph"
                setOnClickListener { title -> title as TextView
                    fragment.lifecycleScope.launch {
                        title.isEnabled = false
                        title.text = "Loading..."
                        val history = manager.getRatingHistory(info)
                        title.isEnabled = true
                        if(history == null || history.isEmpty()){
                            title.text = "Show rating graph"
                            return@launch
                        }
                        title.text = ""
                        ratingGraphView.apply {
                            setHistory(history)
                            visibility = View.VISIBLE
                        }
                        var something = false
                        if(history.size > 10){
                            something = true
                            buttonLast10.apply {
                                visibility = View.VISIBLE
                                setOnClickListener { ratingGraphView.showLast10() }
                            }
                        }
                        if(ratingGraphView.ratingHistoryLastMonth.isNotEmpty()){
                           something = true
                            buttonLastMonth.apply {
                                visibility = View.VISIBLE
                                setOnClickListener { ratingGraphView.showLastMonth() }
                            }
                        }
                        if(ratingGraphView.ratingHistoryLastYear.size > ratingGraphView.ratingHistoryLastMonth.size){
                            something = true
                            buttonLastYear.apply {
                                visibility = View.VISIBLE
                                setOnClickListener { ratingGraphView.showLastYear() }
                            }
                        }
                        if(something){
                            buttonAll.apply {
                                visibility = View.VISIBLE
                                setOnClickListener { ratingGraphView.showAll() }
                            }
                        }
                    }
                }
            }


        }
    }
}