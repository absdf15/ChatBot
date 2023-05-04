package io.github.absdf15.chatbot.module.chat

data class PromptChatSettings(
    val model: String?,
    val maxTokens: Int?,
    val temperature: Double?,
    val topP: Double?,
    val presencePenalty: Double?,
    val frequencyPenalty: Double?,
    val sessionLimitLength: Int?,
    val currenciesNumber: Int?,
    val sendForwardMessage: Boolean = false
)