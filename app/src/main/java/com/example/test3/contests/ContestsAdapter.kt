package com.example.test3.contests

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class ContestsAdapter(
    fragment: CPSFragment,
    dataFlow: Flow<List<Contest>>
): FlowItemsAdapter<ContestViewHolder, List<Contest>>(fragment, dataFlow) {

    private var items: Array<Contest> = emptyArray()
    override fun getItemCount() = items.size

    init {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                startTimer(1000) {
                    val currentTimeSeconds = getCurrentTimeSeconds()
                    val comparator = Contest.getComparator(currentTimeSeconds)
                    getActiveViewHolders().forEach { it.refreshTime(currentTimeSeconds) }
                    if(!items.isSortedWith(comparator)) {
                        val oldItems = items.clone()
                        items.sortWith(comparator)
                        DiffUtil.calculateDiff(diffCallback(oldItems, items)).dispatchUpdatesTo(this@ContestsAdapter)
                    }
                }
            }
        }
    }

    override suspend fun applyData(data: List<Contest>): DiffUtil.DiffResult {
        val newItems = data.sortedWith(Contest.getComparator(getCurrentTimeSeconds())).toTypedArray()
        return DiffUtil.calculateDiff(diffCallback(items, newItems)).also { items = newItems }
    }

    private val expandedItems = mutableSetOf<Pair<Contest.Platform,String>>()
    override fun getItemViewType(position: Int): Int {
        if (items[position].getCompositeId() in expandedItems) return type_bigview
        return type_preview
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContestViewHolder {
        return when (viewType) {
            type_preview -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.contest_list_item_preview, parent, false) as ConstraintLayout
                ContestItemPreviewHolder(view)
            }
            type_bigview -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.contest_list_item_bigview, parent, false) as ConstraintLayout
                ContestItemBigViewHolder(view)
            }
            else -> throw IllegalArgumentException("unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: ContestViewHolder, position: Int) {
        when (holder) {
            is ContestItemPreviewHolder -> bindViewHolder(holder, position)
            is ContestItemBigViewHolder -> bindViewHolder(holder, position)
        }
    }

    fun bindViewHolder(holder: ContestItemPreviewHolder, position: Int) {
        with(holder) {
            val contest = items[position]
            holder.contest = contest
            refreshTime(getCurrentTimeSeconds())
            setOnClickListener {
                expandedItems.add(contest.getCompositeId())
                notifyItemChanged(bindingAdapterPosition)
            }
        }
    }

    fun bindViewHolder(holder: ContestItemBigViewHolder, position: Int) {
        with(holder) {
            val contest = items[position]
            holder.contest = contest
            refreshTime(getCurrentTimeSeconds())
            setOnMinimizeClickListener {
                expandedItems.remove(contest.getCompositeId())
                notifyItemChanged(bindingAdapterPosition)
            }
            setOnRemoveClickListener {
                val context = it.context
                MaterialAlertDialogBuilder(context)
                    .setTitle("Remove contest from list?")
                    .setPositiveButton("Yes") { d, _ ->
                        with(context.settingsContests) {
                            runBlocking {
                                val ids = removedContestsIds()
                                removedContestsIds(ids + contest.getCompositeId())
                            }
                        }
                    }
                    .setNegativeButton("No") { _, _ -> }
                    .show()
            }
        }
    }

    companion object {
        const val type_preview = 0
        const val type_bigview = 1
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

        fun cutTrailingBrackets(title: String): Pair<String, String> {
            if (title.isEmpty() || title.last()!=')') return title to ""
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
            if (ballance!=0) return title to ""
            return title.substring(0, i) to title.substring(i)
        }
    }
}

class ContestItemPreviewHolder(view: ConstraintLayout): ContestViewHolder(view) {
    private val title: TextView = view.findViewById(R.id.contests_list_item_title)
    private val titleAdditional: TextView = view.findViewById(R.id.contests_list_item_title_additional)
    private val date: TextView = view.findViewById(R.id.contests_list_item_date)
    private val counterTextView: TextView = view.findViewById(R.id.contests_list_item_counter)
    private val icon: ImageView = view.findViewById(R.id.contests_list_item_icon)

    override fun applyContest(contest: Contest) {
        with(cutTrailingBrackets(contest.title)) {
            title.text = first
            titleAdditional.text = second
        }
        icon.setImageResource(contest.platform.getIcon())
    }

    fun setOnClickListener(action: (View) -> Unit) {
        view.setOnClickListener(action)
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

class ContestItemBigViewHolder(view: ConstraintLayout): ContestViewHolder(view) {
    private val title: TextView = view.findViewById(R.id.contests_list_item_title)
    private val titleAdditional: TextView = view.findViewById(R.id.contests_list_item_title_additional)
    private val dateStart: TextView = view.findViewById(R.id.contests_list_item_start_date)
    private val dateEnd: TextView = view.findViewById(R.id.contests_list_item_end_date)
    private val counterTextView: TextView = view.findViewById(R.id.contests_list_item_counter)
    private val icon: ImageView = view.findViewById(R.id.contests_list_item_icon)

    override fun applyContest(contest: Contest) {
        with(cutTrailingBrackets(contest.title)) {
            title.text = first
            titleAdditional.text = second
            titleAdditional.isVisible = second.isNotBlank()
        }
        icon.setImageResource(contest.platform.getIcon())
        view.findViewById<ImageButton>(R.id.contests_list_item_open).setOnClickListener {
            contest.link?.let { url -> it.context.startActivity(makeIntentOpenUrl(url)) }
        }
    }

    override fun refresh(currentTimeSeconds: Long, phase: Contest.Phase, oldPhase: Contest.Phase?) {
        dateStart.text = "start: ${makeDate(contest.startTimeSeconds)}"
        dateEnd.text = "end: ${makeDate(contest.endTimeSeconds)}"
        counterTextView.text = when (phase) {
            Contest.Phase.BEFORE -> {
                "starts in " + timeDifference2(currentTimeSeconds, contest.startTimeSeconds)
            }
            Contest.Phase.RUNNING -> {
                "ends in " + timeDifference2(currentTimeSeconds, contest.endTimeSeconds)
            }
            Contest.Phase.FINISHED -> {
                ""
            }
        }
        if(phase != oldPhase) showPhase(title, phase)
    }

    fun setOnMinimizeClickListener(action: (View) -> Unit) =
        view.findViewById<ImageButton>(R.id.contests_list_item_minimize).setOnClickListener(action)

    fun setOnRemoveClickListener(action: (View) -> Unit) =
        view.findViewById<ImageButton>(R.id.contests_list_item_remove).setOnClickListener(action)

}