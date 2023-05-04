package io.github.absdf15.chatbot.config

import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.ValueName
import net.mamoe.mirai.console.data.value


internal object CoreConfig: ReadOnlyPluginConfig("core-config")  {
    @ValueName("bot_owner")
    val botOwners:MutableList<Long> by value(mutableListOf())

    @ValueName("bot_admin")
    val botAdmins:MutableList<Long>by value(mutableListOf())

    @ValueName("enable_group")
    val enableGroup:MutableList<Long> by value(mutableListOf())
}