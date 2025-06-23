package com.demich.cps.accounts.userinfo

import com.demich.cps.platforms.api.clients.DmojUrls
import kotlinx.serialization.Serializable


@Serializable
data class DmojUserInfo(
    override val handle: String,
    override val rating: Int? = null
): RatedUserInfo() {
    override val userPageUrl get() = DmojUrls.user(handle)
}