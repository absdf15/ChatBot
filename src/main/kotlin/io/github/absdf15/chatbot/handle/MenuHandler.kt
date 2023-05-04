package io.github.absdf15.chatbot.handle

import io.github.absdf15.chatbot.ChatBot
import io.github.absdf15.chatbot.module.Permission
import io.github.absdf15.chatbot.utils.MessageUtils.Companion.safeSendMessage
import io.github.absdf15.chatbot.utils.TextUtils.Companion.executeSubCommand
import io.github.absdf15.chatbot.utils.TextUtils.Companion.parseCommandAndExecute
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.MessageEvent


internal object MenuHandler: SimpleListenerHost()  {
    @EventHandler
    suspend fun MessageEvent.menuHandle(){

        parseCommandAndExecute(null, true){command,_,_->
            ChatBot.logger.info(command)
            executeSubCommand(command,"菜单"){
                val text = buildString {
                    appendLine("1. 切换人格 [人格名]")
                    appendLine("2. 人格列表")
                    appendLine("3. 清空会话")
                    appendLine("4. @机器人 即可对话")
                    append("5. 指令列表")
                }
                safeSendMessage(text)
            }.takeIf { it } ?: executeSubCommand(command,"指令列表",Permission.BOT_ADMIN){
                val text = buildString {
                    appendLine("1. 群列表")
                    appendLine("2. 启用群 [群号/空]")
                    append("3. 禁用群 [群号/空]")
                }
                safeSendMessage(text)
            }
        }
    }
}