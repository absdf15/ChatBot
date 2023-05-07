package io.github.absdf15.chatbot.command

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.absdf15.chatbot.ChatBot
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
import io.github.absdf15.openai.module.OpenAIModel
import io.github.absdf15.openai.module.search.Api
import io.github.absdf15.qbot.core.annotation.Command
import io.github.absdf15.qbot.core.annotation.Component
import io.github.absdf15.qbot.core.module.common.ActionParams
import io.github.absdf15.qbot.core.module.common.MatchType
import io.github.absdf15.qbot.core.utils.MessageUtils.Companion.safeGetCode
import io.github.absdf15.qbot.core.utils.MessageUtils.Companion.safeSendAndRecallAsync
import io.github.absdf15.qbot.core.utils.MessageUtils.Companion.safeSendMessage
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.toPlainText
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage

@Component
public object ChatCommand {

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
}