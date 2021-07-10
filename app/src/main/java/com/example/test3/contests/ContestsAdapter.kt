package com.example.test3.contests

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.R
import com.example.test3.makeIntentOpenUrl
import com.example.test3.ui.FlowItemsAdapter
import com.example.test3.utils.durationHHMM
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class ContestsAdapter(
    fragment: Fragment,
    dataFlow: Flow<List<Contest>>
): FlowItemsAdapter<ContestsAdapter.ContestViewHolder, List<Contest>>(fragment, dataFlow) {

    private var items: Array<Contest> = emptyArray()
    override fun getItemCount() = items.size

    class ContestViewHolder(val view: ConstraintLayout) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.contests_list_item_title)
        val date: TextView = view.findViewById(R.id.contests_list_item_date)
        val duration: TextView = view.findViewById(R.id.contests_list_item_duration)
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
            duration.text = durationHHMM(contest.durationSeconds)
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