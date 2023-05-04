package io.github.absdf15.chatbot.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.ValueName
import net.mamoe.mirai.console.data.value

@PublishedApi
internal object ChatSettings:AutoSavePluginConfig("chat-settings") {
    @ValueName("has_filter_session")
    @ValueDescription("过滤会话启用")
    val hasFilterSession:Boolean by value(true)

    @ValueName("hasSession_shared")
    @ValueDescription("会话共享")
    val hasSessionShared :MutableMap<Long,Boolean> by value(mutableMapOf())

    @ValueName("default_prompt")
    @ValueDescription("默认Prompt")
    val defaultPrompt: String by value("NONE")

    @ValueName("request_limit")
    @ValueDescription("默认Prompt")
    val requestLimit: Int by value(200)

}