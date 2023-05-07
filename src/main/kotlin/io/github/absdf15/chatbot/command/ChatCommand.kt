package io.github.absdf15.chatbot.command

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.absdf15.chatbot.ChatBot
import io.github.absdf15.chatbot.ChatBot.resolveDataFile
import io.github.absdf15.chatbot.config.ApiConfig
import io.github.absdf15.chatbot.config.ChatSettings
import io.github.absdf15.chatbot.module.common.Constants
import io.github.absdf15.chatbot.module.common.Constants.Companion.getCurrentModel
import io.github.absdf15.chatbot.utils.HttpUtils
import io.github.absdf15.chatbot.utils.MarkdownUtils
import io.github.absdf15.chatbot.utils.MarkdownUtils.Companion.convertMarkdownToImg
import io.github.absdf15.chatbot.utils.OpenAiUtils.Companion.generateCallApi
import io.github.absdf15.chatbot.utils.OpenAiUtils.Companion.queryOrChat
import io.github.absdf15.chatbot.utils.OpenAiUtils.Companion.replyText
import io.github.absdf15.chatbot.utils.TextUtils.Companion.getSessionId
import io.github.absdf15.openai.module.OpenAIModel
import io.github.absdf15.openai.module.Role
import io.github.absdf15.openai.module.search.Api
import io.github.absdf15.openai.module.search.Call
import io.github.absdf15.qbot.core.annotation.Command
import io.github.absdf15.qbot.core.annotation.Component
import io.github.absdf15.qbot.core.module.common.ActionParams
import io.github.absdf15.qbot.core.module.common.MatchType
import io.github.absdf15.qbot.core.utils.MessageUtils.Companion.safeGetCode
import io.github.absdf15.qbot.core.utils.MessageUtils.Companion.safeSendAndRecallAsync
import io.github.absdf15.qbot.core.utils.MessageUtils.Companion.safeSendMessage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.buildForwardMessage
import net.mamoe.mirai.message.data.toPlainText
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
object ChatCommand {

    /**
     * 根据API回调结果回复
     */
    @Command("^(\\/q|question|问)", MatchType.REGEX_MATCH)
    suspend fun ActionParams.query() {
        val text = rawCommand.split(" ", limit = 2)[1]
        messageEvent.queryOrChat(text) {

            if (ApiConfig.wolframApiId.isEmpty() ||
                ApiConfig.googleApiKey.isEmpty() ||
                ApiConfig.googleSearchEngineId.isEmpty()
            ) {
                sender.safeSendMessage("管理员未设置密钥！")
                return@queryOrChat
            }
            val (call, chatInfo) = sender.generateCallApi(text) ?: return@queryOrChat
            val (jsonObject, exceptionNumber) = callApi(call)

            val replyInfo = sender.replyText(query = text, result = jsonObject) ?: return@queryOrChat
            var senderText = replyInfo.choices[0].message?.content ?: return@queryOrChat
            val usage = replyInfo.usage.totalTokens + chatInfo.usage.totalTokens
            ChatBot.logger.info("exceptionNumber:$exceptionNumber")
            exceptionNumber.takeIf { it != 0 }?.let {
                sender.safeSendMessage("总请求数为:${call.calls.size}\n累计请求错误数为：${exceptionNumber}\n该数据会影响回答的准确性！")
                val tmpFile = ChatBot.resolveDataFile("tmp/${sender.id}.json")
                tmpFile.writeText(jsonObject.toString())
            }
            senderText += "\nusage:${usage}"
            val tags = MarkdownUtils.hasLatexFormulas(senderText) ?: let {
                sender.safeSendAndRecallAsync(senderText, 5)
                return@queryOrChat
            }
            val image = senderText.convertMarkdownToImg(tags).uploadAsImage(sender)
            sender.safeSendAndRecallAsync(image, 5)
        }
    }


    @Command("#聊天记录")
    suspend fun ActionParams.chatMessageHistories() {
        messageEvent.apply {
             val messages = Constants.CHAT_MESSAGES[getSessionId()] ?: let {
                sender.safeSendMessage("消息列表为空哦～请聊天后再来吧～")
                return
            }
            val forwardMessage = buildForwardMessage {
                messages.forEachIndexed { index, it ->
                    if (it.role == Role.USER)
                        sender.id named sender.nameCardOrNick says it.content+ "\n 消息序列: $index"
                    else
                        bot.id named bot.nameCardOrNick says it.content + "\n 消息序列: $index"
                }
            }
            sender.safeSendMessage(forwardMessage)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Command("#导出聊天记录")
    suspend fun ActionParams.exportChatMessageHistories() {
        messageEvent.apply {
            val messages = Constants.CHAT_MESSAGES[getSessionId()] ?: let {
                sender.safeSendMessage("消息列表为空哦～请聊天后再来吧～")
                return
            }
            val text = buildString {
                messages.forEach {
                    if (it.role == Role.USER) {
                        appendLine("${sender.nameCardOrNick}:")
                        appendLine(it.content)
                    } else {
                        appendLine("${bot.nameCardOrNick}:")
                        appendLine(it.content)
                    }
                }
            }
            val currentDateTime = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            val formattedDateTime = currentDateTime.format(formatter)
            val fileName = "tmp/${formattedDateTime}_${sender.id}.txt"
            val file = resolveDataFile(fileName)
            file.writeBytes(text.toByteArray())
            if (messageEvent is GroupMessageEvent) {
                val resource = file.toExternalResource()
                val absoluteFile =
                    (this as GroupMessageEvent).group.files.uploadNewFile(fileName, resource)
                resource.close()
                GlobalScope.launch {
                    delay(5 * 60 * 1000)
                    println("Blocked for 5 minutes.")
                }
            } else if (this is FriendMessageEvent) {
                // TODO 找不到好友发送文件的方法
            }
        }
    }

    /**
     * gpt显示提示列表
     */
    @Command("人格列表")
    suspend fun ActionParams.displayCharacterList() {
        messageEvent.apply {
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
    }


    /**
     * gpt初始提示词切换
     */
    @Command("切换人格")
    suspend fun ActionParams.switchCharacter() {
        messageEvent.apply {
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

    /**
     * 切换模型
     */
    @Command("切换模型")
    suspend fun ActionParams.switchModel() {
        messageEvent.apply {
            if (args.isEmpty()) return
            when (args[0]) {
                "gpt3.5" -> Constants.CHAT_MODEL[sender.id] = OpenAIModel.GPT3_5
                "gpt4" -> Constants.CHAT_MODEL[sender.id] = OpenAIModel.GPT4
                else -> sender.safeSendMessage("该模型未载入或未授权！")
            }
            sender.safeSendMessage("当前模型：${sender.getCurrentModel()}")
        }
    }

    @Command("清空会话")
    suspend fun ActionParams.cleanSession() {
        messageEvent.sender.apply {
            val code = safeGetCode()
            Constants.CHAT_MESSAGES[code]?.clear()
            if (Constants.CHAT_MESSAGES[code] == null) {
                safeSendMessage("会话已清空！")
            }
        }
    }

    @Command("会话共享(?:开启|关闭)", MatchType.REGEX_MATCH)
    suspend fun ActionParams.setSessionShared() {
        messageEvent.apply {
            val boolean = command.contains("开启")
            ChatSettings.hasSessionShared[subject.id] = boolean
            if (ChatSettings.hasSessionShared[subject.id] == true)
                sender.safeSendMessage("会话共享启用！")
            else sender.safeSendMessage("会话共享关闭")
        }
    }

    /**
     * 调用API并存入[JsonObject]
     * @param call 需要调用的 API
     * @return 处理后的[JsonObject],以及调用过程中报错的次数
     */
    private suspend fun ActionParams.callApi(call: Call): Pair<JsonObject, Int> {
        var hasGoogleRequest = false
        var hasCalculatorRequest = false
        var hasWikiRequest = false
        val jsonObject = JsonObject()
        var wikiArray: JsonArray? = null
        var wolframArray: JsonArray? = null
        var googleArray: JsonArray? = null
        var exceptionNumber = 0
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
        return jsonObject to exceptionNumber
    }

}