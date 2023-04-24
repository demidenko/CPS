package com.demich.cps.notifications

class CPSNotificationChannels: NotificationIdProvider() {

    inner class CodeforcesNotificationsGroup: NotificationChannelGroupInfo("codeforces", "CodeForces") {
        val rating_changes = channel(nextId(), "rating_changes", "Rating changes", Importance.HIGH)
        val contribution_changes get() = channel(nextId(), "contribution_changes", "Contribution changes", Importance.MIN)
        val follow_progress get() = channel(nextId(), "follow_progress", "Follow: update progress", Importance.MIN)
        val contest_monitor get() = channel(nextId(), "contest_monitor", "Contest monitor")
    }

    inner class AtCoderNotificationGroup: NotificationChannelGroupInfo("atcoder", "AtCoder") {
        val rating_changes = channel(nextId(), "rating_changes", "Rating changes", Importance.HIGH)
        val news = channel(nextRangeId(), "news", "News")
    }

    val codeforces = CodeforcesNotificationsGroup()
    val atcoder = AtCoderNotificationGroup()
}

val notificationChannels get() = CPSNotificationChannels()