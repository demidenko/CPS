package com.demich.cps.accounts.userinfo

import com.demich.cps.data.api.CodeChefApi
import kotlinx.serialization.Serializable

@Serializable
data class CodeChefUserInfo(
    override val status: STATUS,
    override val handle: String,
    override val rating: Int? = null
): RatedUserInfo() {
    override val userPageUrl get() = CodeChefApi.urls.user(handle)
}