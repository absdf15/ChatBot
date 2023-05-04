package io.github.absdf15.openai


import com.fasterxml.jackson.annotation.JsonInclude
import com.google.gson.annotations.SerializedName

@JsonInclude(JsonInclude.Include.NON_NULL)
public data class Usage(
    @SerializedName("completion_tokens")
    val completionTokens: Int = 0,
    @SerializedName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerializedName("total_tokens")
    val totalTokens: Int = 0
)