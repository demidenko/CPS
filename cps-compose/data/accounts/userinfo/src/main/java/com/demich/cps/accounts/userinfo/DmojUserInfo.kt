package com.demich.cps.accounts.userinfo

import com.demich.cps.data.api.DmojApi
import kotlinx.serialization.Serializable


@Serializable
data class DmojUserInfo(
    override val status: STATUS,
    override val handle: String,
    override val rating: Int? = null
): RatedUserInfo() {
    override val userPageUrl get() = DmojApi.urls.user(handle)
}