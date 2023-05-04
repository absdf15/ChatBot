package io.github.absdf15.openai

import com.google.gson.annotations.SerializedName

public data class ChoiceMessage(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: String
)