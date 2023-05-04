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
import io.github.absdf15.chatbot.module.common.Constants.Companion.getCurrentModel
import io.github.absdf15.chatbot.utils.HttpUtils
import io.github.absdf15.chatbot.utils.MarkdownUtils.Companion.convertMarkdownToImg
import io.github.absdf15.chatbot.utils.MarkdownUtils.Companion.hasLatexFormulas
import io.github.absdf15.chatbot.utils.MessageUtils.Companion.safeGetCode
import io.github.absdf15.chatbot.utils.MessageUtils.Companion.safeSendAndRecallAsync
import io.github.absdf15.chatbot.utils.MessageUtils.Companion.safeSendMessage
import io.github.absdf15.chatbot.utils.OpenAiUtils.Companion.chatHandle
import io.github.absdf15.chatbot.utils.OpenAiUtils.Companion.filter
import io.github.absdf15.chatbot.utils.OpenAiUtils.Companion.generateCallApi
import io.github.absdf15.chatbot.utils.OpenAiUtils.Companion.initPrompt
import io.github.absdf15.chatbot.utils.OpenAiUtils.Companion.replyText
import io.github.absdf15.chatbot.utils.PermissionUtils.Companion.getPermission
import io.github.absdf15.chatbot.utils.TextUtils.Companion.executeSubCommand
import io.github.absdf15.chatbot.utils.TextUtils.Companion.filter
import io.github.absdf15.chatbot.utils.TextUtils.Companion.getUnformattedCommand
import io.github.absdf15.chatbot.utils.TextUtils.Companion.parseCommandAndExecute
import io.github.absdf15.chatbot.utils.TextUtils.Companion.replaceFilterWords
import io.github.absdf15.openai.module.OpenAIModel
import io.github.absdf15.openai.module.search.Api
import kotlinx.coroutines.sync.withLock
import net.mamoe.mirai.console.data.Value
import net.mamoe.mirai.console.data.findBackingFieldValue
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.toPlainText
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage

internal object ChatMessageHandler : SimpleListenerHost() {

    /**
     * ChatGPT 默认处理方法
     */
    @EventHandler
    suspend fun GroupMessageEvent.handler() {
        if(sender.getPermission(group.id).hasPermission(Permission.MEMBER).not()) return
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
           queryOrChat(text,false)
        }
    }

    /**
     * GPT相关指令
     */
    @EventHandler
    suspend fun MessageEvent.characterCommand() {
        parseCommandAndExecute(null, true) { command, args, unformattedCommand ->
            executeSubCommand(command, "切换人格") {
                switchCharacter(args)
            }.takeIf { it } ?: executeSubCommand(command, "人格列表") {
                displayCharacterList()
            }.takeIf { it } ?: executeSubCommand(command, "切换模型", Permission.BOT_ADMIN) {
                switchModel(args)
            }.takeIf { it } ?: executeSubCommand(command, "/q") {
                queryOrChat(unformattedCommand,true)
            }.takeIf { it } ?: executeSubCommand(command, "清空会话"){
                Constants.CHAT_MESSAGES[safeGetCode()]?.clear()
                if (Constants.CHAT_MESSAGES[safeGetCode()] == null){
                    safeSendMessage("会话已清空！")
                }
            }.takeIf { it } ?: executeSubCommand(command,"会话共享"){
                val boolean = when (args[0]) {
                    "开启" -> true
                    "关闭" -> false
                    "true" -> true
                    "false" -> false
                    else -> null
                }?: return@executeSubCommand
                ChatSettings.hasSessionShared[subject.id] = boolean
                if (ChatSettings.hasSessionShared[subject.id] == true)
                    safeSendMessage("会话共享启用！")
                else safeSendMessage("会话共享关闭")
            }
        }
    }

    /**
     * 查询或聊天
     */
    private suspend fun MessageEvent.queryOrChat(text: String,isQuery: Boolean = true){
        val isFilter = text.filter()
        ChatBot.logger.info("isFilter:$isFilter")
        if (isFilter == true) {
            sender.filter(text).takeIf { it }?.let {
                sender.safeSendMessage("该内容不合规，请重新编辑后输出！")
                return
            }
        }
        sender.fetchApiResult {
            if (this is GroupMessageEvent&&ChatSettings.hasSessionShared[subject.id] == true){
              Constants.USER_MUTEX.withLock(subject.id) {
                  if (isQuery) query() else chat(text)
              }
            }
            else{
                if (isQuery) query() else chat(text)
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

    /**
     * 根据API回调结果回复
     */
    private suspend fun MessageEvent.query() {
        if (ApiConfig.wolframApiId.isEmpty() ||
            ApiConfig.googleApiKey.isEmpty() ||
            ApiConfig.googleSearchEngineId.isEmpty()
        ) {
            sender.safeSendMessage("管理员未设置密钥！")
            return
        }
        val text = getUnformattedCommand(message).replace("/q", "")
        val (call, chatInfo) = sender.generateCallApi(text) ?: return
        var hasGoogleRequest = false
        var hasCalculatorRequest = false
        var hasWikiRequest = false
        val jsonObject = JsonObject()
        var wikiArray: JsonArray? = null
        var wolframArray: JsonArray? = null
        var googleArray: JsonArray? = null
        var exceptionNumber: Int = 0
        for (request in call.calls) {
            when (request.api) {
                Api.WikiSearch.toString() -> {
                    val response =
                        try {
                            HttpUtils.wiki(request.query)
                        } catch (e: Exception) {
                            exceptionNumber++
                            continue
                        }
                    if (!hasWikiRequest) {
                        wikiArray = JsonArray()
                        hasWikiRequest = true
                    }
                    wikiArray?.add(response)
                }

                Api.Calculator.toString() -> {
                    val response =
                        try {
                            HttpUtils.wolfram(request.query) ?: continue
                        } catch (e: Exception) {
                            exceptionNumber++
                            continue
                        }

                    if (!hasCalculatorRequest) {
                        wolframArray = JsonArray()
                        hasCalculatorRequest = true
                    }
                    wolframArray?.add(response)
                }

                Api.Google.toString() -> {
                    val response = try {
                        HttpUtils.google(request.query) ?: continue
                    } catch (e: Exception) {
                        exceptionNumber++
                        continue
                    }
                    if (!hasGoogleRequest) {
                        googleArray = JsonArray()
                        hasGoogleRequest = true
                    }
                    googleArray?.add(response)
                }
            }
        }
        wikiArray.takeIf { it != null }?.let {
            jsonObject.add("wiki", it)
        }

        wolframArray.takeIf { it != null }?.let {
            jsonObject.add("wolfram", it)
        }

        googleArray.takeIf { it != null }?.let {
            jsonObject.add("google", it)
        }


        val replyInfo = sender.replyText(query = text, result = jsonObject) ?: return
        var senderText = replyInfo.choices[0].message?.content ?: return
        val usage = replyInfo.usage.totalTokens + chatInfo.usage.totalTokens

        ChatBot.logger.info("exceptionNumber:$exceptionNumber")
        exceptionNumber.takeIf { it != 0 }?.let {
            sender.safeSendMessage("总请求数为:${call.calls.size}\n累计请求错误数为：${exceptionNumber}\n该数据会影响回答的准确性！")
            val tmpFile = resolveDataFile("tmp/${sender.id}.json")
            tmpFile.writeText(jsonObject.toString())
        }
        senderText += "\nusage:${usage}"
        val tags = hasLatexFormulas(senderText) ?: let {
            sender.safeSendAndRecallAsync(senderText,5)
            return
        }

        val image = senderText.convertMarkdownToImg(tags).uploadAsImage(sender)
        sender.safeSendAndRecallAsync(image,5)
    }

    /**
     * 切换模型
     */
    private suspend fun MessageEvent.switchModel(args: List<String>) {
        if (args.isEmpty()) return
        when (args[0]) {
            "gpt3.5" -> Constants.CHAT_MODEL[sender.id] = OpenAIModel.GPT3_5
            "gpt4" -> Constants.CHAT_MODEL[sender.id] = OpenAIModel.GPT4
            else -> sender.safeSendMessage("该模型未载入或未授权！")
        }
        sender.safeSendMessage("当前模型：${sender.getCurrentModel()}")
    }


    /**
     * gpt显示提示列表
     */
    private suspend fun MessageEvent.displayCharacterList() {
        val mid = if (ChatSettings.hasSessionShared[subject.id] == true) subject.id else sender.id
        val promptName = Constants.safeGetPrompt(mid)
        val sendText = buildString {
            append("当前为：${if (ChatSettings.hasSessionShared[subject.id] == true) "共享会话" else "独立会话"}\n")
            append("当前模型：${sender.getCurrentModel()}\n")
            append("当前人格为：${promptName}\n")
            append("[人格列表]：\n")
            Constants.PROMPT_FILES.keys.forEachIndexed { index, s ->
                append("${index + 1}.$s\n")
            }
            append("当前人格数为${Constants.PROMPT_FILES.size}")
        }
        subject.sendMessage(sendText)
    }

    /**
     * gpt初始提示词切换
     */
    private suspend fun MessageEvent.switchCharacter(args: List<String>) {
        if (args.isEmpty()) return
        val mid = if (ChatSettings.hasSessionShared[subject.id] == true) subject.id else sender.id
        Constants.setPrompt(mid, args[0]).takeIf { it } ?: run {
            subject.sendMessage("当前模板不存在！")
            return
        }
        Constants.CHAT_MESSAGES[mid]?.clear()
        val prompt = Constants.getPrompt(mid)
        subject.sendMessage("已自动清空会话。\n当前人格模板为：${prompt}！\nTips: 初始对话质量不佳，会影响后续对话哦")
        subject.sendMessage(
            ForwardMessageBuilder(subject).add(
                sender,
                "模板：${Constants.PROMPT_FILES[prompt]}".toPlainText()
            ).build()
        )
    }
}