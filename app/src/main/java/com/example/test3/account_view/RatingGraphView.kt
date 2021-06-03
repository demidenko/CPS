package com.example.test3.account_view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.test3.R
import com.example.test3.account_manager.*
import com.example.test3.ui.disable
import com.example.test3.ui.enable
import com.example.test3.utils.getColorFromResource
import com.example.test3.utils.getCurrentTimeSeconds
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class RatingGraphView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap


    private lateinit var accountManager: RatedAccountManager<*>

    fun setManager(ratedAccountManager: RatedAccountManager<*>){
        accountManager = ratedAccountManager
    }

    private var ratingHistory: List<RatingChange> = listOf(RatingChange(0,0L))
    private var toShow = ratingHistory
    private lateinit var ratingHistoryLast10: List<RatingChange>
    private lateinit var ratingHistoryLastMonth: List<RatingChange>
    private lateinit var ratingHistoryLastYear: List<RatingChange>

    fun setHistory(history: List<RatingChange>) {
        ratingHistory = history
        toShow = ratingHistory

        ratingHistoryLast10 = history.takeLast(10)

        val currentSeconds = getCurrentTimeSeconds()
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

    fun senseToShowLast10() = ratingHistory.size > 10
    fun senseToShowLastMonth() = ratingHistoryLastMonth.size in 1 until ratingHistory.size
    fun senseToShowLastYear() = ratingHistoryLastYear.size in ratingHistoryLastMonth.size+1 until ratingHistory.size

    private fun drawRating(){

        if(width == 0 || height == 0) return

        if (::extraBitmap.isInitialized) extraBitmap.recycle()
        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
        extraCanvas.scale(1f, -1f, width/2f, height/2f)

        val minY = toShow.minOf { it.rating } - 100f
        val maxY = toShow.maxOf { it.rating } + 100f

        val currentSeconds = getCurrentTimeSeconds()
        val (minX, maxX) = when {
            toShow === ratingHistoryLastMonth -> Pair(currentSeconds - TimeUnit.DAYS.toSeconds(30), currentSeconds)
            toShow === ratingHistoryLastYear -> Pair(currentSeconds - TimeUnit.DAYS.toSeconds(365), currentSeconds)
            else -> Pair(toShow.first().timeSeconds, toShow.last().timeSeconds)
        }.let { Pair(it.first.toFloat(), it.second.toFloat()) }

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
        val allRatingBounds = accountManager.ratingUpperBoundRevolutions.toMutableList()
        allRatingBounds.add(getCurrentTimeSeconds() to accountManager.ratingsUpperBounds)
        allRatingBounds.reversed().forEachIndexed { index, it ->
            val lastTimeSeconds = it.first
            val x = if(index == 0) width.toFloat() else {
                val arr = floatArrayOf(lastTimeSeconds.toFloat(), 0f)
                m.mapPoints(arr)
                arr[0]
            }

            val ratingBounds = it.second.toMutableList()
            ratingBounds.add(Pair(Int.MAX_VALUE, HandleColor.RED))
            ratingBounds.reversed().forEachIndexed { index, (upper, ratingColor) ->

                val y = if(index == 0) height.toFloat() else {
                    val arr = floatArrayOf(0f, upper.toFloat())
                    m.mapPoints(arr)
                    arr[1]
                }

                extraCanvas.drawRect(0f, 0f, x, y, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = ratingColor.getARGB(accountManager)
                    style = Paint.Style.FILL
                })
            }
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
        suspend fun<U: UserInfo> showInAccountViewFragment(fragment: AccountViewFragment<U>, manager: RatedAccountManager<U>){

            val view = fragment.requireView().findViewById<ConstraintLayout>(R.id.account_view_rating_graph_view)

            val info = manager.getSavedInfo()
            if(info.status != STATUS.OK || manager.getRating(info) == NOT_RATED){
                view.isVisible = false
                return
            }

            val ratingGraphView = view.findViewById<RatingGraphView>(R.id.account_view_rating_graph).apply {
                setManager(manager)
                isVisible = false
            }

            val buttonAll = view.findViewById<MaterialButton>(R.id.account_view_rating_graph_button_all)
            val buttonLast10 = view.findViewById<MaterialButton>(R.id.account_view_rating_graph_button_last10)
            val buttonLastMonth = view.findViewById<MaterialButton>(R.id.account_view_rating_graph_button_last_month)
            val buttonLastYear = view.findViewById<MaterialButton>(R.id.account_view_rating_graph_button_last_year)

            val activeTextColor = getColorFromResource(manager.context, R.color.textColor)
            val inactiveTextColor = getColorFromResource(manager.context, R.color.textColorAdditional)
            val buttons = listOf(buttonAll, buttonLast10, buttonLastMonth, buttonLastYear).onEach { button ->
                button.isVisible = false
                button.setTextColor(inactiveTextColor)
            }
            buttonAll.setTextColor(activeTextColor)

            fun buttonClick(button: MaterialButton){
                buttons.forEach {
                    it.setTextColor(
                        if(it == button) activeTextColor
                        else inactiveTextColor
                    )
                }
            }

            fragment.requireBottomPanel().findViewById<ImageButton>(R.id.navigation_rated_account_rating_graph).apply {
                enable()
                setOnClickListener { button -> button as ImageButton
                    fragment.lifecycleScope.launch {
                        button.disable()
                        val history = manager.getRatingHistory(info)
                        if(history == null || history.isEmpty()){
                            fragment.mainActivity.showToast("Load failed. Try again.")
                            button.enable()
                            return@launch
                        }

                        ratingGraphView.apply {
                            setHistory(history)
                            isVisible = true
                        }

                        var available = 0
                        fun setAvailable(button: MaterialButton, onClick: () -> Unit){
                            available++
                            button.apply {
                                isVisible = true
                                setOnClickListener {
                                    onClick()
                                    buttonClick(this)
                                }
                            }
                        }

                        if(ratingGraphView.senseToShowLast10()) setAvailable(buttonLast10){ ratingGraphView.showLast10() }

                        if(ratingGraphView.senseToShowLastMonth()) setAvailable(buttonLastMonth){ ratingGraphView.showLastMonth() }

                        if(ratingGraphView.senseToShowLastYear()) setAvailable(buttonLastYear){ ratingGraphView.showLastYear() }

                        if(available>0){
                            setAvailable(buttonAll){ ratingGraphView.showAll() }
                        }

                        view.isVisible = true
                    }
                }
            }
        }
    }
}