package com.example.test3.account_view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.example.test3.account_manager.HandleColor
import com.example.test3.account_manager.RatedAccountManager
import com.example.test3.account_manager.RatingChange
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

    fun setHistory(history: List<RatingChange>) {
        ratingHistory = history.sortedBy { it.timeSeconds }
        drawRating()
        invalidate()
    }

    private fun drawRating(){

        if(width == 0|| height == 0) return

        if (::extraBitmap.isInitialized) extraBitmap.recycle()
        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
        extraCanvas.scale(1f, -1f, width/2f, height/2f)

        val minY = ratingHistory.minOf { it.rating } - 100f
        val maxY = ratingHistory.maxOf { it.rating } + 100f

        val circleRadius = 8f

        val m = Matrix()
        if(ratingHistory.size == 1){
            val x = ratingHistory[0].timeSeconds
            val bound = TimeUnit.DAYS.toSeconds(1).toFloat()
            m.preScale(width/(bound*2), height/(maxY-minY))
            m.preTranslate(-(x-bound), -minY)
        }else{
            m.preScale(width/(width+circleRadius*3), 1f, width/2f, height/2f)
            val minX = ratingHistory.minOf { it.timeSeconds }.toFloat()
            val maxX = ratingHistory.maxOf { it.timeSeconds }.toFloat()
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
        ratingHistory.mapIndexed { index, ratingChange ->
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

        extraCanvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = pathWidth
        })

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.BLACK
            strokeWidth = 3f
        }

        //rating points
        ratingHistory.mapIndexed { index, ratingChange ->
            val arr = floatArrayOf(ratingChange.timeSeconds.toFloat(), ratingChange.rating.toFloat())
            m.mapPoints(arr)
            val (x,y) = arr

            //circle shadow
            extraCanvas.drawCircle(x+shadowX, y+shadowY, circleRadius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = shadowColor
            })

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
        fun showInAccountViewFragment(fragment: AccountViewFragment, manager: RatedAccountManager, ratingGraphView: RatingGraphView){
            ratingGraphView.setManager(manager)
            fragment.lifecycleScope.launch {
                val info = manager.getSavedInfo()
                val history = manager.getRatingHistory(info)
                if(history == null || history.isEmpty()){
                    ratingGraphView.visibility = View.GONE
                    return@launch
                }
                ratingGraphView.apply {
                    setHistory(history)
                    visibility = View.VISIBLE
                }
            }
        }
    }
}