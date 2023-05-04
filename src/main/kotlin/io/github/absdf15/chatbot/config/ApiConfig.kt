package io.github.absdf15.chatbot.config

import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.ValueName
import net.mamoe.mirai.console.data.value

internal object ApiConfig: ReadOnlyPluginConfig("api-config") {
    @ValueName("google_api_key")
    val googleApiKey:String by value("")

    @ValueName("google_search_engine_id")
    val googleSearchEngineId: String by value("")

    @ValueName("wolfram_api_id")
    val wolframApiId: String by value("")
}