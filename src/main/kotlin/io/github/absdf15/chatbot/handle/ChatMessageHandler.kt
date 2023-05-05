package io.github.absdf15.chatbot.handle

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.absdf15.chatbot.ChatBot
import io.github.absdf15.chatbot.ChatBot.resolveDataFile
import io.github.absdf15.chatbot.config.ApiConfig
import io.github.absdf15.chatbot.config.ChatSettings
import io.github.absdf15.chatbot.module.Permission
import io.github.absdf15.chatbot.module.common.Constants
import io.github.absdf15.chatbot.module.common.Constants.Companion.fetchApiResult
import io.github.absdf15.chatbot.utils.HttpUtils
import io.github.absdf15.chatbot.utils.MarkdownUtils.Companion.convertMarkdownToImg
import io.github.absdf15.chatbot.utils.MarkdownUtils.Companion.hasLatexFormulas
import io.github.absdf15.chatbot.utils.MessageUtils.Companion.safeSendAndRecallAsync
import io.github.absdf15.chatbot.utils.MessageUtils.Companion.safeSendMessage
import io.github.absdf15.chatbot.utils.OpenAiUtils.Companion.chatHandle
import io.github.absdf15.chatbot.utils.OpenAiUtils.Companion.filter
import io.github.absdf15.chatbot.utils.OpenAiUtils.Companion.generateCallApi
import io.github.absdf15.chatbot.utils.OpenAiUtils.Companion.initPrompt
import io.github.absdf15.chatbot.utils.OpenAiUtils.Companion.queryOrChat
import io.github.absdf15.chatbot.utils.OpenAiUtils.Companion.replyText
import io.github.absdf15.chatbot.utils.PermissionUtils.Companion.getPermission
import io.github.absdf15.chatbot.utils.TextUtils.Companion.filter
import io.github.absdf15.chatbot.utils.TextUtils.Companion.getUnformattedCommand
import io.github.absdf15.openai.module.search.Api
import kotlinx.coroutines.sync.withLock
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage

internal object ChatMessageHandler : SimpleListenerHost() {

    /**
     * ChatGPT 默认处理方法
     */
    @EventHandler
    suspend fun GroupMessageEvent.handler() {
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
            queryOrChat(text){
                chat(text)
            }
        }
    }

    /**
     * 聊天
     */
    private suspend fun MessageEvent.chat(text: String) {
        val sessionId = if (ChatSettings.hasSessionShared[subject.id] == true) subject.id else sender.id
        val result = initPrompt(sessionId)
        ChatBot.logger.info("初始化结果：$result")
        if (result >= 0) {
            ChatBot.logger.info("进入If语句：result >= 0")
            sender.chatHandle(text, sessionId)
        }
    }



}