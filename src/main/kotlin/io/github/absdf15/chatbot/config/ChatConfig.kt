package io.github.absdf15.chatbot.config

import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.ValueName
import net.mamoe.mirai.console.data.value

@PublishedApi
internal object ChatConfig:ReadOnlyPluginConfig("openai") {
    @ValueName("api_key")
    @ValueDescription("OpenAI的api密钥")
    val apiKey:String by value("")

    @ValueName("timeout")
    @ValueDescription("等待停止时间")
    val timeout: Long by value(300_000L)

    @ValueName("max_tokens")
    @ValueDescription("Maximum length")
    val maxTokens: Int by value(512)

    @ValueName("temperature")
    @ValueDescription("Temperature")
    val temperature: Double by value(0.9)

    @ValueName("top_p")
    @ValueDescription("Top P")
    val topP: Double by value(1.0)

    @ValueName("presence_penalty")
    @ValueDescription("Presence Penalty")
    val presencePenalty: Double by value(0.6)

    @ValueName("frequency_penalty")
    @ValueDescription("Frequency Penalty")
    val frequencyPenalty: Double by value(0.0)
}