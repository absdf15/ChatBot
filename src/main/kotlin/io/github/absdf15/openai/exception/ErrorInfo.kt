package io.github.absdf15.openai.exception

import com.fasterxml.jackson.annotation.JsonInclude
import com.google.gson.JsonElement

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorInfo(
    val code: String? = null,
    val message: String = "",
    val param: JsonElement? = null,
    val type: String = ""
)