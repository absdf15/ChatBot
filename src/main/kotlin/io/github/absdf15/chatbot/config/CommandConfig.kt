package io.github.absdf15.chatbot.config

import io.github.absdf15.chatbot.module.CommandData
import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

internal object CommandConfig: ReadOnlyPluginConfig("command-config") {

    @ValueDescription("需要映射的指令列表\n匹配内容[context]: 若不确定内容中是否有特殊符号会导致报错，请用引号包裹\n匹配方式[matchType]: “EXACT_MATCH”完全相等|“PARTIAL_MATCH”包含文本|“REGEX_MATCH”正则匹配")
    val command:MutableMap<String,CommandData> by value(mutableMapOf())


}