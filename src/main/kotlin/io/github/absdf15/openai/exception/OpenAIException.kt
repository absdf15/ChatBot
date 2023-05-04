package io.github.absdf15.openai.exception

public class OpenAIException(val info: ErrorInfo) : IllegalStateException() {
    override val message: String get() = info.message
}