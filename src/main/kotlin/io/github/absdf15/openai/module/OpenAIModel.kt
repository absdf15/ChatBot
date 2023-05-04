package io.github.absdf15.openai.module

import com.google.gson.annotations.SerializedName

enum class OpenAIModel(private val modelName: String) {
    @SerializedName("gpt-4")
    GPT4("gpt-4"),
    @SerializedName("gpt-4-32k")
    GPT4_32K("gpt-4-32k"),
    @SerializedName("gpt-3.5-turbo")
    GPT3_5("gpt-3.5-turbo");
    override fun toString(): String {
        return modelName
    }
}