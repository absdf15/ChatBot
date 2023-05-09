package io.github.absdf15.chatbot.command

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.absdf15.chatbot.ChatBot
import io.github.absdf15.chatbot.ChatBot.resolveDataFile
import io.github.absdf15.chatbot.config.ApiConfig
import io.github.absdf15.chatbot.config.ChatSettings
import io.github.absdf15.chatbot.module.chat.ChatPromptData
import io.github.absdf15.chatbot.module.chat.TempInfo
import io.github.absdf15.chatbot.module.chat.TempType
import io.github.absdf15.chatbot.module.common.Constants
import io.github.absdf15.chatbot.module.common.Constants.Companion.getCurrentModel
import io.github.absdf15.chatbot.utils.HttpUtils
import io.github.absdf15.chatbot.utils.MarkdownUtils
import io.github.absdf15.chatbot.utils.MarkdownUtils.Companion.convertMarkdownToImg
import io.github.absdf15.chatbot.utils.OpenAiUtils.Companion.chat
import io.github.absdf15.chatbot.utils.OpenAiUtils.Companion.generateCallApi
import io.github.absdf15.chatbot.utils.OpenAiUtils.Companion.queryOrChat
import io.github.absdf15.chatbot.utils.OpenAiUtils.Companion.replyText
import io.github.absdf15.chatbot.utils.TextUtils.Companion.getCurrentDateTime
import io.github.absdf15.chatbot.utils.TextUtils.Companion.getSessionId
import io.github.absdf15.openai.module.ChatMessage
import io.github.absdf15.openai.module.OpenAIModel
import io.github.absdf15.openai.module.Role
import io.github.absdf15.openai.module.search.Api
import io.github.absdf15.openai.module.search.Call
import io.github.absdf15.qbot.core.annotation.Command
import io.github.absdf15.qbot.core.annotation.Component
import io.github.absdf15.qbot.core.annotation.PointedBy
import io.github.absdf15.qbot.core.annotation.PointsTo
import io.github.absdf15.qbot.core.config.CoreConfig
import io.github.absdf15.qbot.core.module.common.*
import io.github.absdf15.qbot.core.utils.ConfigUtils
import io.github.absdf15.qbot.core.utils.MessageUtils.Companion.safeSendAndRecallAsync
import io.github.absdf15.qbot.core.utils.MessageUtils.Companion.safeSendMessage
import io.ktor.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import java.io.File
import java.util.*

@Component
object ChatCommand {


    @Command("#导入模板", MatchType.EXACT_MATCH)
    suspend fun ActionParams.importPrompt() {
        messageEvent.apply {
            val text = rawCommand.split("\\s+".toRegex(), limit = 2)[1].trim()
            if (text.isEmpty()) {
                sender.safeSendMessage("模板不能为空！")
                return
            }
            CoreConfig.botOwners.forEach {
                val friend = bot.getFriend(it)
                if (friend != null) {
                    friend.sendMessage("是否要添加下列模板(Y/N):")
                    val tempId = "${getCurrentDateTime()}_${sender.id}"
                    val forwardMessage = buildForwardMessage {
                        bot.id named bot.nick says "id: $tempId"
                        sender.id named senderName says text
                    }
                    friend.sendMessage("如果该请求不是最后一条,请使用 #handle [id] 来处理该请求。")
                    Constants.TEMP_DATA.add(TempInfo(tempId, TempType.PROMPT, text))
                    ConfigUtils.put(PointPair(friend.id, friend.id), "#导入模板" to 1)
                    friend.sendMessage(forwardMessage)
                }
            }
        }

    }


    @Command("#handle", permission = Permission.BOT_OWNER)
    suspend fun ActionParams.handlePromptRequestById(){
        if(messageEvent is FriendMessageEvent){
            messageEvent.apply {
                var data: TempInfo? = null
                Constants.TEMP_DATA.forEach {
                    if (args[0] == it.id) {
                        data = it
                    }
                }
                if (data == null) return
                ConfigUtils.put(PointPair(sender.id, subject.id), "#导入模板" to 1)
                sender.sendMessage("是否要添加下列模板(Y/N):")
                val forwardMessage = buildForwardMessage {
                    bot.id named bot.nick says "id: ${data!!.id}"
                    sender.id named senderName says data!!.content
                }
                sender.safeSendMessage(forwardMessage)

            }
        }
    }
    @PointedBy("#导入模板")
    suspend fun ActionParams.handlePrompt() {
        if (messageEvent is FriendMessageEvent) {
            messageEvent.apply {
                val boolean = when (rawCommand.trim().lowercase(Locale.ENGLISH)) {
                    "yes" -> true
                    "y" -> true
                    "n" -> false
                    "no" -> false
                    "是" -> true
                    "否" -> false
                    else -> false
                }

                val data = Constants.TEMP_DATA.last()
                if (boolean) {
                    sender.safeSendMessage("已同意id为${data.id}的请求，请输入模板命名:")
                    ConfigUtils.put(
                        PointPair(sender.id, subject.id),
                        "io.github.absdf15.chatbot.command.ChatCommand.handlePrompt" to 1
                    )
                } else {
                    Constants.TEMP_DATA.remove(Constants.TEMP_DATA.last())
                    sender.safeSendMessage("已拒绝id${data.id}的请求。")
                }
            }
        }
    }

    @PointedBy("io.github.absdf15.chatbot.command.ChatCommand.handlePrompt", permission = Permission.BOT_OWNER)
    suspend fun ActionParams.addPromptFile() {
        if (messageEvent is FriendMessageEvent) {
            messageEvent.apply {
                Constants.PROMPT_FILES.keys.contains(rawCommand).takeIf { it }?.let {
                    sender.safeSendMessage("模板存在，请重新输入!")
                    Params.POINT_MAP.put(
                        PointPair(sender.id, subject.id),
                        "io.github.absdf15.chatbot.command.ChatCommand.handlePrompt" to 1
                    )
                    return
                }
                sender.safeSendMessage("好的,模板名字为:$rawCommand，开始处理...")

                val fileName = "prompt-$rawCommand.txt"
                val output = File(ChatPromptData.promptFolder, fileName)
                output.writeBytes(fileName.toByteArray())
                ChatPromptData.reload()
                displayCharacterList()
            }
        }
    }

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
            val messages = Constants.CHAT_MESSAGES[getSessionId()]
            if (messages.isNullOrEmpty()) {
                sender.safeSendMessage("消息列表为空哦～请聊天后再来吧～")
                return
            }
            sender.safeSendMessage("下面是消息列表：")
            buildMessageHistories(messages).forEach {
                sender.safeSendMessage(it)
            }

        }
    }


    @OptIn(DelicateCoroutinesApi::class)
    @Command("#导出聊天记录")
    suspend fun ActionParams.exportChatMessageHistories() {
        messageEvent.apply {
            val messages = Constants.CHAT_MESSAGES[getSessionId()]
            if (messages.isNullOrEmpty()) {
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

            val fileName = "tmp/${getCurrentDateTime()}_${sender.id}.txt"
            val file = resolveDataFile(fileName)
            file.writeBytes(text.toByteArray())
            if (messageEvent is GroupMessageEvent) {
                val resource = file.toExternalResource()
                val absoluteFile =
                    (this as GroupMessageEvent).group.files.uploadNewFile(fileName, resource)
                resource.close()
                GlobalScope.launch {
                    delay(5 * 60 * 1000)
                    absoluteFile.delete()
                }
            } else if (this is FriendMessageEvent) {
                // TODO 找不到好友发送文件的方法
            }
        }
    }

    @Command("^#切换至\\s*\\d*?\$", matchType = MatchType.REGEX_MATCH)
    suspend fun ActionParams.switchMessage() {
        messageEvent.apply {
            val (code, messages) = parsecAndGetCode() ?: return
            val message = messages[code]
            val role = message.role
            val aList = arrayListOf<ChatMessage>()
            aList.addAll(messages.dropLast(messages.size - code))
            messages.clear()
            messages.addAll(aList)
            val sendText = buildString {
                append("当前最后一条消息为")
                when (role) {
                    Role.USER -> append("${sender.nick}发送的内容，请使用@继续对话吧！")
                    else -> append("机器人的响应结果，请输入”#响应“来获取结果。")
                }
            }
            sender.safeSendMessage(sendText)
        }
    }

    @Command("^#切换\\s*\\d*?\$", matchType = MatchType.REGEX_MATCH)
    suspend fun ActionParams.switchAndSendMessage() {
        messageEvent.apply {
            val (code, messages) = parsecAndGetCode() ?: return
            val message = messages[code]
            val role = message.role
            val aList = arrayListOf<ChatMessage>()
            when (role) {
                Role.USER -> aList.addAll(messages.dropLast(messages.size - code + 1))
                Role.ASSISTANT -> aList.addAll(messages.dropLast(messages.size - code))
                else -> {
                    sender.safeSendMessage("奇怪的事情发生了！")
                    return
                }
            }
            messages.clear()
            messages.addAll(aList)
            sendRequest()

        }
    }

    @Command("#响应")
    suspend fun ActionParams.sendRequest() {
        messageEvent.apply {
            queryOrChat {
                chat(isDirect = true)
            }
        }
    }

    @Command("#撤回")
    suspend fun ActionParams.recallRequest() {
        messageEvent.apply {
            queryOrChat {
                val messages = Constants.CHAT_MESSAGES[getSessionId()] ?: return@queryOrChat
                if (messages.size >= 4) {
                    val tmp = arrayListOf<ChatMessage>()
                    tmp.addAll(messages.dropLast(2))
                    messages.clear()
                    messages.addAll(tmp)
                    sender.safeSendMessage("撤回成功！下面是聊天记录：")
                    buildMessageHistories(messages).forEach {
                        sender.safeSendMessage(it)
                    }
                }
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
        messageEvent.apply {
            val code = getSessionId()
            Constants.CHAT_MESSAGES[code]?.clear()
            if (Constants.CHAT_MESSAGES[code].isNullOrEmpty()) {
                sender.safeSendMessage("会话已清空！")
            }
        }
    }

    @Command("会话共享(?:开启|关闭)", MatchType.REGEX_MATCH, Permission.BOT_ADMIN)
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

    private suspend fun ActionParams.parsecAndGetCode(): Pair<Int, ArrayList<ChatMessage>>? {
        messageEvent.apply {
            val sessionId = getSessionId()
            val messages = Constants.CHAT_MESSAGES[sessionId] ?: return null
            val list = Regex("#切换(至)?(\\d+)?").find(rawCommand)
            val code = list?.groupValues?.getOrNull(2)?.trim()?.toIntOrNull() ?: args.getOrNull(0)?.toIntOrNull()
            ?: let {
                sender.safeSendMessage("请输入消息序列！")
                return null
            }
            if (messages.size == 0 || messages.size < code) return null
            if (code == 1 || code == 0) {
                sender.safeSendMessage("清空会话！")
                messages.clear()
                return null
            }
            return code to messages
        }
    }

    private suspend fun MessageEvent.buildMessageHistories(messages: ArrayList<ChatMessage>): ArrayList<ForwardMessage> {
        // 拆分数组
        val messageBatches = messages.chunked(100)
        val forwardMessages = arrayListOf<ForwardMessage>()
        messageBatches.forEach { batch ->
            forwardMessages.add(buildForwardMessage {
                batch.forEachIndexed { index, it ->
                    if (it.role == Role.USER)
                        sender.id named sender.nameCardOrNick says it.content + "\n 消息序列: ${index + 1}"
                    else
                        bot.id named bot.nameCardOrNick says it.content + "\n 消息序列: ${index + 1}"
                }
            })
        }
        return forwardMessages
    }
}