package com.example.test3.contests

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
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
import com.example.test3.utils.durationHHMM
import com.example.test3.utils.getColorFromResource
import com.example.test3.utils.getCurrentTimeSeconds
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
                getActiveViewHolders().takeIf { it.isNotEmpty() }?.let { holders ->
                    val currentTimeSeconds = getCurrentTimeSeconds()
                    holders.forEach { it.refreshTime(currentTimeSeconds) }
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
                    Contest.Phase.BEFORE -> "starts in " + timeDifference2(currentTimeSeconds, startTimeSeconds)
                    Contest.Phase.RUNNING -> "ends in " + timeDifference2(currentTimeSeconds, endTimeSeconds)
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
        val oldItems = items
        items = data.toTypedArray()
        return DiffUtil.calculateDiff(diffCallback(oldItems, items))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.contest_list_item, parent, false) as ConstraintLayout
        return ContestViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContestViewHolder, position: Int) {
        with(holder) {
            val contest = items[position]
            title.text = contest.title
            date.text = DateFormat.format("dd.MM.yyyy E HH:mm", TimeUnit.SECONDS.toMillis(contest.startTimeSeconds))
            startTimeSeconds = contest.startTimeSeconds
            duration = contest.durationSeconds
            refreshTime(getCurrentTimeSeconds())
            contest.link?.let { url ->
                view.setOnClickListener { it.context.startActivity(makeIntentOpenUrl(url)) }
            }
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