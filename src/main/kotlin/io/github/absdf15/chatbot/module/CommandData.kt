package io.github.absdf15.chatbot.module

import io.github.absdf15.chatbot.module.common.MatchType
import kotlinx.serialization.Serializable


@Serializable
data class CommandData (
    val context: String = "",
    val matchType: MatchType = MatchType.EXACT_MATCH
)