package com.demich.cps.accounts.userinfo

import com.demich.cps.platforms.api.clients.CodeChefUrls
import kotlinx.serialization.Serializable

@Serializable
data class CodeChefUserInfo(
    override val handle: String,
    override val rating: Int? = null
): RatedUserInfo() {
    override val userPageUrl get() = CodeChefUrls.user(handle)
}