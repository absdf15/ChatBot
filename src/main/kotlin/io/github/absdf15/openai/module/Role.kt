package io.github.absdf15.openai.module

import com.google.gson.annotations.SerializedName

enum class Role(private val type: String) {
    @SerializedName("system")
    SYSTEM("system"),
    @SerializedName("user")
    USER("user"),
    @SerializedName("assistant")
    ASSISTANT("assistant");

    override fun toString(): String {
        return type
    }
}