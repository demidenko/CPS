package com.example.test3.contests

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.R
import com.example.test3.makeIntentOpenUrl
import com.example.test3.timeDifference2
import com.example.test3.ui.CPSFragment
import com.example.test3.ui.FlowItemsAdapter
import com.example.test3.ui.TimeDepends
import com.example.test3.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ContestsAdapter(
    fragment: CPSFragment,
    dataFlow: Flow<List<Contest>>,
    val previewType: Int
): FlowItemsAdapter<ContestViewHolder, List<Contest>>(fragment, dataFlow) {

    private var items: Array<Contest> = emptyArray()
    override fun getItemCount() = items.size

    init {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    val currentTimeSeconds = getCurrentTimeSeconds()
                    val comparator = Contest.getComparator(currentTimeSeconds)
                    getActiveViewHolders().forEach { it.refreshTime(currentTimeSeconds) }
                    if(!items.isSortedWith(comparator)) {
                        val oldItems = items.clone()
                        items.sortWith(comparator)
                        DiffUtil.calculateDiff(diffCallback(oldItems, items)).dispatchUpdatesTo(this@ContestsAdapter)
                    }
                    delay(1000)
                }
            }
        }
    }

    override suspend fun applyData(data: List<Contest>): DiffUtil.DiffResult {
        val newItems = data.sortedWith(Contest.getComparator(getCurrentTimeSeconds())).toTypedArray()
        return DiffUtil.calculateDiff(diffCallback(items, newItems)).also { items = newItems }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContestViewHolder {
        return when (previewType) {
            1 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.contest_list_item, parent, false) as ConstraintLayout
                ContestViewHolder1(view)
            }
            2 -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.contest_list_item2, parent, false) as ConstraintLayout
                ContestViewHolder2(view)
            }
            else -> throw IllegalArgumentException("Unexpected preview type $previewType")
        }
    }

    override fun onBindViewHolder(holder: ContestViewHolder, position: Int) {
        with(holder) {
            val contest = items[position]
            holder.contest = contest
            refreshTime(getCurrentTimeSeconds())
        }
    }

    companion object {
        private fun diffCallback(old: Array<Contest>, new: Array<Contest>) =
            object : DiffUtil.Callback() {
                override fun getOldListSize() = old.size
                override fun getNewListSize() = new.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldContest = old[oldItemPosition]
                    val newContest = new[newItemPosition]
                    return (oldContest.platform == newContest.platform) && (oldContest.id == newContest.id)
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return old[oldItemPosition] == new[newItemPosition]
                }
            }
    }
}


@DrawableRes
internal fun Contest.Platform.getIcon(): Int {
    return when(this) {
        Contest.Platform.codeforces -> R.drawable.ic_logo_codeforces
        Contest.Platform.atcoder -> R.drawable.ic_logo_atcoder
        Contest.Platform.topcoder -> R.drawable.ic_logo_topcoder
        Contest.Platform.codechef -> R.drawable.ic_logo_codechef
        Contest.Platform.google -> R.drawable.ic_logo_google
        else -> R.drawable.ic_cup
    }
}

abstract class ContestViewHolder(protected val view: ConstraintLayout): RecyclerView.ViewHolder(view), TimeDepends {
    private var companionContest: Contest? = null
    var contest: Contest
        get() = companionContest!!
        set(value) {
            companionContest = value
            lastPhase = null
            applyContest(value)
        }

    abstract fun applyContest(contest: Contest)

    final override var startTimeSeconds: Long
        get() = contest.startTimeSeconds
        @Deprecated("no effect, use contest = ")
        set(value) {}

    private var lastPhase: Contest.Phase? = null
    final override fun refreshTime(currentTimeSeconds: Long) {
        val phase = contest.getPhase(currentTimeSeconds)
        refresh(currentTimeSeconds, phase, lastPhase)
    }

    abstract fun refresh(currentTimeSeconds: Long, phase: Contest.Phase, oldPhase: Contest.Phase?)

    protected fun showPhase(title: TextView, phase: Contest.Phase) {
        title.setTextColor(getColorFromResource(view.context,
            when (phase) {
                Contest.Phase.FINISHED -> R.color.contest_finished
                Contest.Phase.RUNNING -> R.color.contest_running
                else -> R.color.textColor
            }
        ))
    }

    companion object {
        fun makeDate(timeSeconds: Long): CharSequence = DateFormat.format("dd.MM E HH:mm", TimeUnit.SECONDS.toMillis(timeSeconds))

        fun muteTrailingBrackets(title: String, context: Context): CharSequence {
            if (title.isEmpty() || title.last()!=')') return title
            var i = title.length-2
            var ballance = 1
            while (ballance>0 && i>0) {
                when (title[i]) {
                    '(' -> --ballance
                    ')' -> ++ballance
                }
                if (ballance == 0) break
                --i
            }
            if (ballance!=0) return title
            return SpannableStringBuilder(title).apply {
                setSpan(ForegroundColorSpan(getColorFromResource(context,R.color.textColorAdditional)), i, title.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        }
    }
}

class ContestViewHolder1(view: ConstraintLayout): ContestViewHolder(view) {
    private val title: TextView = view.findViewById(R.id.contests_list_item_title)
    private val date: TextView = view.findViewById(R.id.contests_list_item_date)
    private val durationTextView: TextView = view.findViewById(R.id.contests_list_item_duration)
    private val counterTextView: TextView = view.findViewById(R.id.contests_list_item_counter)
    private val icon: ImageView = view.findViewById(R.id.contests_list_item_icon)

    override fun applyContest(contest: Contest) {
        title.text = contest.title
        date.text = makeDate(contest.startTimeSeconds)
        durationTextView.text = durationHHMM(contest.durationSeconds)
        icon.setImageResource(contest.platform.getIcon())
        view.setOnClickListener { contest.link?.let { url -> it.context.startActivity(makeIntentOpenUrl(url)) } }
    }

    override fun refresh(currentTimeSeconds: Long, phase: Contest.Phase, oldPhase: Contest.Phase?) {
        counterTextView.text =
            when(phase) {
                Contest.Phase.BEFORE -> "in " + timeDifference2(currentTimeSeconds, contest.startTimeSeconds)
                Contest.Phase.RUNNING -> "left " + timeDifference2(currentTimeSeconds, contest.endTimeSeconds)
                Contest.Phase.FINISHED -> ""
            }
        if(phase != oldPhase) showPhase(title, phase)
    }
}

class ContestViewHolder2(view: ConstraintLayout): ContestViewHolder(view) {
    private val title: TextView = view.findViewById(R.id.contests_list_item_title)
    private val date: TextView = view.findViewById(R.id.contests_list_item_date)
    private val counterTextView: TextView = view.findViewById(R.id.contests_list_item_counter)
    private val icon: ImageView = view.findViewById(R.id.contests_list_item_icon)

    override fun applyContest(contest: Contest) {
        title.text = muteTrailingBrackets(contest.title, view.context)
        icon.setImageResource(contest.platform.getIcon())
        view.setOnClickListener { contest.link?.let { url -> it.context.startActivity(makeIntentOpenUrl(url)) } }
    }

    override fun refresh(currentTimeSeconds: Long, phase: Contest.Phase, oldPhase: Contest.Phase?) {
        counterTextView.text = when (phase) {
            Contest.Phase.BEFORE -> {
                if(phase!=oldPhase) date.text = makeDate(contest)
                "in " + timeDifference2(currentTimeSeconds, contest.startTimeSeconds)
            }
            Contest.Phase.RUNNING -> {
                if(phase!=oldPhase) date.text = "ends ${makeDate(contest.endTimeSeconds)}"
                "left " + timeDifference2(currentTimeSeconds, contest.endTimeSeconds)
            }
            Contest.Phase.FINISHED -> {
                if(phase!=oldPhase) date.text = "${makeDate(contest.startTimeSeconds)} - ${makeDate(contest.endTimeSeconds)}"
                ""
            }
        }
        if(phase != oldPhase) showPhase(title, phase)
    }

    companion object {
        fun makeDate(contest: Contest): String {
            val begin = makeDate(contest.startTimeSeconds)
            val end =
                if (contest.durationSeconds < TimeUnit.DAYS.toSeconds(1))
                    DateFormat.format("HH:mm", TimeUnit.SECONDS.toMillis(contest.endTimeSeconds))
                else "..."
            return "$begin-$end"
        }
    }
}