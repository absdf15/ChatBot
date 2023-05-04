package io.github.absdf15.openai

import io.github.absdf15.chatbot.config.ChatConfig
import io.github.absdf15.chatbot.config.ChatSettings
import io.github.absdf15.openai.module.ChatMessage
import io.github.absdf15.openai.exception.ErrorInfoWrapper
import io.github.absdf15.openai.exception.OpenAIException
import io.github.absdf15.openai.module.OpenAIModel
import io.github.absdf15.openai.module.chat.ChatCompletion
import io.github.absdf15.openai.module.chat.ChatInfo
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.serialization.gson.*
import java.nio.charset.MalformedInputException

public open class OpenAI(apiKey: String) {
    public open val http: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            gson {
                disableHtmlEscaping()
            }
        }
        install(HttpTimeout) {
            socketTimeoutMillis =  ChatConfig.timeout
            connectTimeoutMillis = ChatConfig.timeout
            requestTimeoutMillis = ChatConfig.timeout
        }
        Auth {
            bearer {
                loadTokens {
                    BearerTokens(apiKey, "")
                }
                refreshTokens {
                    BearerTokens(apiKey, "")
                }
                sendWithoutRequest { builder ->
                    builder.url.host == "api.openai.com"
                }
            }
        }
        HttpResponseValidator {
            validateResponse { response ->
                val statusCode = response.status.value
                val originCall = response.call
                if (statusCode < 400) return@validateResponse

                val exceptionCall = originCall.save()
                val exceptionResponse = exceptionCall.response

                throw try {
                    val error = exceptionResponse.body<ErrorInfoWrapper>().error
                    OpenAIException(info = error)
                } catch (_: ContentConvertException) {
                    val exceptionResponseText = try {
                        exceptionResponse.bodyAsText()
                    } catch (_: MalformedInputException) {
                        "<body failed decoding>"
                    }
                    when (statusCode) {
                        in 400..499 -> {
                            ClientRequestException(response, exceptionResponseText)
                        }

                        in 500..599 -> {
                            ServerResponseException(response, exceptionResponseText)
                        }

                        else -> ResponseException(response, exceptionResponseText)
                    }
                }
            }
        }
        BrowserUserAgent()
        ContentEncoding()
    }

    suspend fun chat(messages: List<ChatMessage>, model: OpenAIModel = OpenAIModel.GPT3_5): ChatInfo{
        val completion = ChatCompletion(
            model,
            messages
        )
        val response = http.post("https://api.openai.com/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(
              completion
            )
        }
        return response.body()
    }

    suspend fun chat(chatCompletion: ChatCompletion): ChatInfo {
        //StarRobot.logger.info(Constants.GSON.toJson(chatCompletion))
        val response = http.post("https://api.openai.com/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(
                chatCompletion
            )
        }
        return response.body()
    }
}