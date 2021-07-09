package com.example.test3.contests

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
import com.example.test3.utils.CodeforcesContest
import com.example.test3.utils.CodeforcesURLFactory
import com.example.test3.utils.durationHHMM
import kotlinx.coroutines.flow.Flow

class ContestsAdapter(
    fragment: Fragment,
    dataFlow: Flow<List<CodeforcesContest>>
): FlowItemsAdapter<ContestsAdapter.ContestViewHolder, List<CodeforcesContest>>(fragment, dataFlow) {

    private var items: Array<CodeforcesContest> = emptyArray()
    override fun getItemCount() = items.size

    class ContestViewHolder(val view: ConstraintLayout) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.contests_list_item_title)
        val date: TextView = view.findViewById(R.id.contests_list_item_date)
        val duration: TextView = view.findViewById(R.id.contests_list_item_duration)
    }

    override suspend fun applyData(data: List<CodeforcesContest>): DiffUtil.DiffResult? {
        items = data.toTypedArray()
        return null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.contest_list_item, parent, false) as ConstraintLayout
        return ContestViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContestViewHolder, position: Int) {
        with(holder) {
            val contest = items[position]
            title.text = contest.name
            date.text = "${contest.startTimeSeconds} ${contest.phase}"
            duration.text = durationHHMM(contest.durationSeconds)
            view.setOnClickListener { it.context.startActivity(makeIntentOpenUrl(CodeforcesURLFactory.contest(contest))) }
        }
    }

}