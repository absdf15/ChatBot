package io.github.absdf15.chatbot.command

import io.github.absdf15.qbot.core.annotation.Command
import io.github.absdf15.qbot.core.annotation.Component
import io.github.absdf15.qbot.core.annotation.PointedBy
import io.github.absdf15.qbot.core.annotation.PointsTo
import io.github.absdf15.qbot.core.module.common.ActionParams
import io.github.absdf15.qbot.core.module.common.Permission
import io.github.absdf15.qbot.core.utils.MessageUtils.Companion.safeSendMessage

@Component
public object MenuCommand {
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
    @Command(searchTerm = "管理员指令", permission = Permission.BOT_ADMIN)
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
    @Command( "GPT指令", permission = Permission.BOT_ADMIN)
    suspend fun ActionParams.gptMenu(){
        messageEvent.sender.apply {
            val text = buildString {
                appendLine("1. 切换人格 [人格名]")
                appendLine("2. 人格列表")
                appendLine("3. 清空会话")
                appendLine("4. @机器人 即可对话")
                appendLine("5. /q[文本] 查询内容并根据API回复")
                appendLine("6. #切换[消息序号] 切换消息并回复")
                appendLine("7. #响应 获取机器人响应")
                appendLine("8. #切换至[消息序号] 切换消息不回复")
                appendLine("9. #聊天记录 查看你与BOT的聊天记录及序号")
                append("10. #导出聊天记录 导出聊天记录至群聊")
            }
            safeSendMessage(text)
        }
    }
}