package com.demich.cps.accounts.userinfo

import com.demich.cps.platforms.api.AtCoderApi
import kotlinx.serialization.Serializable

@Serializable
data class AtCoderUserInfo(
    override val handle: String,
    override val rating: Int? = null
): RatedUserInfo() {
    override val userPageUrl: String
        get() = AtCoderApi.urls.user(handle)
}