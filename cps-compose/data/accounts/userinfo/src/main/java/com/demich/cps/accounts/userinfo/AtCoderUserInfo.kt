package com.demich.cps.accounts.userinfo

import com.demich.cps.platforms.api.atcoder.AtCoderUrls
import kotlinx.serialization.Serializable

@Serializable
data class AtCoderUserInfo(
    override val handle: String,
    override val rating: Int? = null
): RatedUserInfo() {
    override val userPageUrl: String
        get() = AtCoderUrls.user(handle)
}