package com.demich.cps.notifications

val notificationChannels get() = CPSNotificationChannels()


class CPSNotificationChannels: NotificationIdProvider(intervalLength = 1 shl 22) {

    inner class CodeforcesNotificationsGroup: NotificationChannelGroupInfo("codeforces", "CodeForces") {
        val rating_changes = channel(nextId(), "rating_changes", "Rating changes", Importance.HIGH)
        val contribution_changes = channel(nextId(), "contribution_changes", "Contribution changes", Importance.MIN)
        val follow_progress = channel(nextId(), "follow_progress", "Follow: update progress", Importance.MIN)
        val new_blog_entry = channel(nextIdRange(), "new_blog_entry", "New blog entries")
        val contest_monitor = channel(nextId(), "contest_monitor", "Contest monitor")
        val submission_result = channel(nextIdRange(), "submission_result", "Submissions results")
        val upsolving_suggestion = channel(nextIdRange(), "upsolving_suggestion", "Upsolving suggestions")
    }

    inner class AtCoderNotificationGroup: NotificationChannelGroupInfo("atcoder", "AtCoder") {
        val rating_changes = channel(nextId(), "rating_changes", "Rating changes", Importance.HIGH)
        val news = channel(nextIdRange(), "news", "News")
    }

    inner class ProjectEulerNotificationGroup: NotificationChannelGroupInfo("project_euler", "Project Euler") {
        val problems = channel(nextIdRange(), "problems", "Recent problems")
        val news = channel(nextIdRange(), "news", "News")
    }

    val codeforces = CodeforcesNotificationsGroup()
    val atcoder = AtCoderNotificationGroup()
    val project_euler = ProjectEulerNotificationGroup()
}
