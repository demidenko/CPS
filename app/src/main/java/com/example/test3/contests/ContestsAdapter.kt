package com.example.test3.contests

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.R
import com.example.test3.makeIntentOpenUrl
import com.example.test3.timeDifference2
import com.example.test3.ui.FlowItemsAdapter
import com.example.test3.ui.TimeDepends
import com.example.test3.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import java.util.concurrent.TimeUnit

class ContestsAdapter(
    fragment: Fragment,
    dataFlow: Flow<List<Contest>>
): FlowItemsAdapter<ContestsAdapter.ContestViewHolder, List<Contest>>(fragment, dataFlow) {

    private var items: Array<Contest> = emptyArray()
    override fun getItemCount() = items.size

    init {
        addRepeatingJob(Lifecycle.State.RESUMED) {
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

    class ContestViewHolder(val view: ConstraintLayout) : RecyclerView.ViewHolder(view), TimeDepends {
        val title: TextView = view.findViewById(R.id.contests_list_item_title)
        val date: TextView = view.findViewById(R.id.contests_list_item_date)
        private val durationTextView: TextView = view.findViewById(R.id.contests_list_item_duration)
        private val phaseTextView: TextView = view.findViewById(R.id.contests_list_item_phase)
        val icon: ImageView = view.findViewById(R.id.contests_list_item_icon)

        var duration: Long = 0
            set(value) {
                field = value
                durationTextView.text = durationHHMM(value)
            }

        override var startTimeSeconds: Long = 0
        private var lastPhase: Contest.Phase? = null

        override fun refreshTime(currentTimeSeconds: Long) {
            val endTimeSeconds = startTimeSeconds + duration
            val phase = Contest.getPhase(currentTimeSeconds, startTimeSeconds, endTimeSeconds)
            phaseTextView.text =
                when(phase) {
                    Contest.Phase.BEFORE -> "in " + timeDifference2(currentTimeSeconds, startTimeSeconds)
                    Contest.Phase.RUNNING -> "left " + timeDifference2(currentTimeSeconds, endTimeSeconds)
                    Contest.Phase.FINISHED -> ""
                }
            if(phase != lastPhase) {
                lastPhase = phase
                title.setTextColor(
                    if(phase == Contest.Phase.FINISHED) getColorFromResource(view.context, R.color.textColorAdditional)
                    else getColorFromResource(view.context, R.color.textColor)
                )
            }
        }
    }

    override suspend fun applyData(data: List<Contest>): DiffUtil.DiffResult {
        val newItems = data.sortedWith(Contest.getComparator(getCurrentTimeSeconds())).toTypedArray()
        return DiffUtil.calculateDiff(diffCallback(items, newItems)).also { items = newItems }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.contest_list_item, parent, false) as ConstraintLayout
        return ContestViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContestViewHolder, position: Int) {
        with(holder) {
            val contest = items[position]
            title.text = contest.title
            date.text = DateFormat.format("dd.MM E HH:mm", TimeUnit.SECONDS.toMillis(contest.startTimeSeconds))
            startTimeSeconds = contest.startTimeSeconds
            duration = contest.durationSeconds
            refreshTime(getCurrentTimeSeconds())
            icon.setImageResource(getIcon(contest.platform))
            contest.link?.let { url ->
                view.setOnClickListener { it.context.startActivity(makeIntentOpenUrl(url)) }
            }
        }
    }

    companion object {
        @DrawableRes
        private fun getIcon(platform: Contest.Platform): Int {
            return when(platform) {
                Contest.Platform.codeforces -> R.drawable.ic_logo_codeforces
                else -> R.drawable.ic_cup
            }
        }

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