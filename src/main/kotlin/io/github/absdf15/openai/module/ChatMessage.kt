package io.github.absdf15.openai.module

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ChatMessage (
    val role: Role,
    val content: String,
    val name: String? = null
)