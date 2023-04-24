package com.demich.cps.notifications

class CPSNotificationChannels: NotificationIdProvider() {

    inner class CodeforcesNotificationsGroup: NotificationChannelGroupInfo("codeforces", "CodeForces") {
        val rating_changes = channel(nextId(), "rating_changes", "Rating changes", Importance.HIGH)
        val contribution_changes get() = channel(nextId(), "contribution_changes", "Contribution changes", Importance.MIN)
        val follow_progress get() = channel(nextId(), "follow_progress", "Follow: update progress", Importance.MIN)
        val new_blog_entry get() = channel(nextIdRange(), "new_blog_entry", "New blog entries")
        val contest_monitor get() = channel(nextId(), "contest_monitor", "Contest monitor")
        val submission_result get() = channel(nextIdRange(), "submission_result", "Submissions results")
        val upsolving_suggestion get() = channel(nextIdRange(), "upsolving_suggestion", "Upsolving suggestions")
    }

    inner class AtCoderNotificationGroup: NotificationChannelGroupInfo("atcoder", "AtCoder") {
        val rating_changes = channel(nextId(), "rating_changes", "Rating changes", Importance.HIGH)
        val news = channel(nextIdRange(), "news", "News")
    }

    val codeforces = CodeforcesNotificationsGroup()
    val atcoder = AtCoderNotificationGroup()
}

val notificationChannels get() = CPSNotificationChannels()