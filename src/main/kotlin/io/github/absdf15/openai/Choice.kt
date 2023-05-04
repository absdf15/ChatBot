package io.github.absdf15.openai

import com.fasterxml.jackson.annotation.JsonInclude
import com.google.gson.annotations.SerializedName

@JsonInclude(JsonInclude.Include.NON_NULL)
public data class Choice(
    @SerializedName("finish_reason")
    val finishReason: String? = null,
    @SerializedName("index")
    private val index: Int = 0,
    @SerializedName("logprobs")
    val logprobs: Int? = null,
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("message")
    val message: ChoiceMessage? = null
)
