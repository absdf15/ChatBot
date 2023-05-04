package io.github.absdf15.openai.module.chat

import com.fasterxml.jackson.annotation.JsonInclude
import com.google.gson.annotations.SerializedName
import io.github.absdf15.openai.module.ChatMessage
import io.github.absdf15.openai.module.OpenAIModel


@JsonInclude(JsonInclude.Include.NON_NULL)
data class ChatCompletion(
    @SerializedName("model")
    val model: OpenAIModel,
    @SerializedName("messages")
    val messages: List<ChatMessage>,
    @SerializedName("max_tokens")
    val maxTokens: Int? = null,
    @SerializedName("n")
    val n: Int? = null,
    @SerializedName("temperature")
    val temperature: Double? = null,
    @SerializedName("top_p")
    val topP: Double? = null,
    @SerializedName("presence_penalty")
    val presencePenalty: Double? = null,
    @SerializedName("frequency_penalty")
    val frequencyPenalty: Double? = null,
    @SerializedName("logit_bias")
    val logitBias: Map<String, Int>? = null,
    @SerializedName("stop")
    val stop: List<String>? = null,
    @SerializedName("stream")
    val stream: Boolean? = null,
    @SerializedName("user")
    val user: String? = null
)