package io.github.absdf15.chatbot.command

import io.github.absdf15.chatbot.annotation.Command
import io.github.absdf15.chatbot.module.Permission
import io.github.absdf15.chatbot.module.common.ActionParams
import io.github.absdf15.chatbot.utils.MessageUtils.Companion.safeSendMessage

object MenuCommand {
    @Command("菜单")
    suspend fun ActionParams.menu(){
        messageEvent.sender.apply {
            val text = buildString {
                appendLine("1. 切换人格 [人格名]")
                appendLine("2. 人格列表")
                appendLine("3. 清空会话")
                appendLine("4. @机器人 即可对话")
                append("5. 指令列表")
            }
            safeSendMessage(text)
        }
    }

    @Command(value = "指令列表", permission = Permission.BOT_ADMIN)
    suspend fun ActionParams.adminMenu(){
        messageEvent.sender.apply {
            val text = buildString {
                appendLine("1. 群列表")
                appendLine("2. 启用群 [群号/空]")
                append("3. 禁用群 [群号/空]")
            }
            safeSendMessage(text)
        }
    }
}