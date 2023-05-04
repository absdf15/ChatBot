package io.github.absdf15.openai.module.chat

import com.fasterxml.jackson.annotation.JsonInclude
import com.google.gson.annotations.SerializedName
import io.github.absdf15.openai.Choice
import io.github.absdf15.openai.Usage

@JsonInclude(JsonInclude.Include.NON_NULL)
public data class ChatInfo(
    @SerializedName("choices")
    val choices: List<Choice>,
    @SerializedName("created")
    val created: Long = 0,
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("model")
    val model: String? = null,
    @SerializedName("object")
    val type: String? = null,
    @SerializedName("usage")
    val usage: Usage
)