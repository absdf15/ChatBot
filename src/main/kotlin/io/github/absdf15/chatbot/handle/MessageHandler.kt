package io.github.absdf15.chatbot.handle


import io.github.absdf15.chatbot.utils.OpenAiUtils.Companion.chat
import io.github.absdf15.chatbot.utils.OpenAiUtils.Companion.queryOrChat
import io.github.absdf15.qbot.core.module.common.Permission
import io.github.absdf15.qbot.core.utils.PermissionUtils.Companion.getPermission
import io.github.absdf15.qbot.core.utils.PermissionUtils.Companion.hasPermission
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.QuoteReply

/**
 * 消息事件注册类
 */
internal object MessageHandler : SimpleListenerHost() {
    @EventHandler
    suspend fun MessageEvent.handle() {
        if (this is GroupMessageEvent &&
            sender.hasPermission(Permission.MEMBER)) {
            handler()
        }
    }


    /**
     * ChatGPT 默认处理方法
     */
    private suspend fun GroupMessageEvent.handler() {
        if (sender.getPermission(group.id).hasPermission(Permission.MEMBER).not()) return
        var isExecute = false
        val text = buildString {
            message.drop(0).forEach {
                if (it is At && it.target == bot.id)
                    isExecute = true
                else if (it !is QuoteReply)
                    append(it.contentToString())
            }
        }
        if (isExecute) {
            queryOrChat(text) {
                chat(text)
            }
        }
    }
}