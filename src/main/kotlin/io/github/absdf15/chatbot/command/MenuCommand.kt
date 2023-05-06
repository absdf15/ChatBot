package io.github.absdf15.chatbot.command

import io.github.absdf15.chatbot.annotation.Command
import io.github.absdf15.chatbot.annotation.PointedBy
import io.github.absdf15.chatbot.annotation.PointsTo
import io.github.absdf15.chatbot.module.Permission
import io.github.absdf15.chatbot.module.ActionParams
import io.github.absdf15.chatbot.utils.MessageUtils.Companion.safeSendMessage

object MenuCommand {
    @PointsTo
    @Command("菜单")
    suspend fun ActionParams.menu(){
        messageEvent.sender.apply {
            val text = buildString {
                appendLine("1. GPT指令")
                appendLine("2.管理员指令")
                append("输入序号，可直接查看子菜单")
            }
            safeSendMessage(text)
        }
    }

    @PointedBy(source = "菜单", index = 2)
    @Command(value = "管理员指令", permission = Permission.BOT_ADMIN)
    suspend fun ActionParams.adminMenu(){
        messageEvent.sender.apply {
            val text = buildString {
                appendLine("1. 群列表")
                appendLine("2. 启用群[群号/空]")
                append("3. 禁用群[群号/空]")
            }
            safeSendMessage(text)
        }
    }
    @PointedBy(source = "菜单", index = 1)
    @Command(value = "GPT指令", permission = Permission.BOT_ADMIN)
    suspend fun ActionParams.gptMenu(){
        messageEvent.sender.apply {
            val text = buildString {
                appendLine("1. 切换人格 [人格名]")
                appendLine("2. 人格列表")
                appendLine("3. 清空会话")
                appendLine("4. @机器人 即可对话")
                append("5. /q[文本] 查询内容并根据API回复")
            }
            safeSendMessage(text)
        }
    }
}