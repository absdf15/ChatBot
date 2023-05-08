package io.github.absdf15.chatbot.utils


import com.google.gson.JsonObject
import io.github.absdf15.chatbot.ChatBot
import io.github.absdf15.chatbot.config.ChatConfig
import io.github.absdf15.chatbot.config.ChatSettings
import io.github.absdf15.chatbot.module.chat.PromptChatSettings
import io.github.absdf15.chatbot.module.common.Constants
import io.github.absdf15.chatbot.module.common.Constants.Companion.fetchApiResult
import io.github.absdf15.chatbot.module.common.Constants.Companion.getCurrentModel
import io.github.absdf15.chatbot.module.common.Constants.Companion.getPrompt
import io.github.absdf15.chatbot.utils.TextUtils.Companion.filter
import io.github.absdf15.chatbot.utils.TextUtils.Companion.getSessionId
import io.github.absdf15.openai.exception.OpenAIException
import io.github.absdf15.openai.module.ChatMessage
import io.github.absdf15.openai.module.OpenAIModel
import io.github.absdf15.openai.module.Role
import io.github.absdf15.openai.module.chat.ChatCompletion
import io.github.absdf15.openai.module.chat.ChatInfo
import io.github.absdf15.openai.module.search.Call
import io.github.absdf15.qbot.core.utils.MessageUtils.Companion.safeSendAndRecallAsync
import io.github.absdf15.qbot.core.utils.MessageUtils.Companion.safeSendMessage
import io.ktor.client.network.sockets.*
import kotlinx.coroutines.sync.withLock
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent


class OpenAiUtils {
    companion object {

        /**
         * 查询或聊天
         */
        suspend fun MessageEvent.queryOrChat(text: String? = null, action: suspend MessageEvent.() -> Unit) {
            if (!text.isNullOrEmpty()){
                val isFilter = text.filter()
                ChatBot.logger.info("isFilter:$isFilter")
                if (isFilter == true) {
                    sender.filter(text).takeIf { it }?.let {
                        sender.safeSendMessage("该内容不合规，请重新编辑后输出！")
                        return
                    }
                }
            }
            sender.fetchApiResult {
                if (this is GroupMessageEvent && ChatSettings.hasSessionShared[subject.id] == true) {
                    Constants.USER_MUTEX.withLock(subject.id) {
                        action.invoke(this)
                    }
                } else {
                    action.invoke(this)
                }
            }
        }

        /**
         * 聊天
         */
        suspend fun MessageEvent.chat(text: String? = null, isDirect: Boolean = false) {
            val sessionId = getSessionId()
            val result = initPrompt(sessionId)
            ChatBot.logger.info("模板初始化结果：$result")
            if (result >= 0) {
                ChatBot.logger.info("进入If语句：result >= 0")
                if (!isDirect && !text.isNullOrEmpty()) sender.chatHandle(text,sessionId)
                else if (isDirect) sender.chatHandle(sessionId)
            }
        }

        /**
         * 根据消息文本判断是否需要过滤
         * @param message 消息文本
         */
        suspend fun User.filter(message: String): Boolean {
            val filterText = """
                You are a United Nations filtering AI.
                Rules:[
                Rule1:If the text contains any political-related elements, output 1.
                Rule2:If the text contains any elements related to crime (murder, drug use, etc.), output 1.
                Rule3:If the text contains any elements related to violence and destruction, output 1.
                Rule4:If there are any relevant nouns in this series (exploitation，proletariat，bourgeoisie),output 1.
                Rule5:If there are works or relevant elements related to Marx (Western Marx，Postmodern Marx) in text,output 1.
                Rule6:In all other cases,output 0.
                Rule7:Understand the statements in the text and determine whether the statement is valid or not.]
                You can only output Single digit,Don‘t output any other content.
            """.trimIndent()
            val text = "message:{\"${message}\"}You can only output Single digit,Don‘t output any other content."
            val messages = arrayListOf(
                ChatMessage(Role.SYSTEM, filterText),
                ChatMessage(Role.USER, text)
            )

            val chatInfo = chat(
                chatCompletion = ChatCompletion(
                    model = OpenAIModel.GPT3_5,
                    messages = messages,
                    maxTokens = 1
                ),
                isOutput = false
            ) ?: return true
            val output = chatInfo.choices[0].message ?: return true
            if (output.content.contains("1")) return true
            return false
        }

        /**
         * 初始化模板
         * @param sessionId 群号或QQ号，用于存储聊天记录（若会话共享则为群号）
         */
        fun initPrompt(sessionId: Long): Int {
            val promptName = Constants.safeGetPrompt(sessionId)
            if (promptName == "NONE") {
                Constants.CHAT_MESSAGES[sessionId] = arrayListOf()
                return 1
            }
            val prompt = Constants.PROMPT_FILES[promptName] ?: return -1
            if (Constants.CHAT_MESSAGES[sessionId]?.isNotEmpty() == true) return 0
            val messages = arrayListOf(ChatMessage(Role.SYSTEM, prompt))
            Constants.CHAT_MESSAGES[sessionId] = messages
            return 1
        }

        /**
         * 根据查询内容生成需要调用的API
         * @param query 查询内容
         */
        suspend fun User.generateCallApi(query: String): Pair<Call, ChatInfo>? {
            val completion = ChatCompletion(
                model = OpenAIModel.GPT3_5,
                messages = arrayListOf(
                    ChatMessage(role = Role.SYSTEM, content = Constants.SYSTEM_API_PROMPT),
                    ChatMessage(
                        role = Role.USER,
                        content = Constants.API_PROMPT.handleText(
                            this,
                            "#{query}" to "'$query'",
                            "#{max_token}" to "250"
                        )
                    )
                ),
                maxTokens = 250
            )
            val result = chat(completion) ?: return null
            return if (result.choices[0].message == null) null
            else Constants.GSON.fromJson(result.choices[0].message?.content, Call::class.java) to result

        }

        /**
         * 根据API回调生成回复内容
         * @param query 查询内容
         * @param result 查询结果
         */
        suspend fun User.replyText(query: String, result: JsonObject): ChatInfo? {
            val completion = ChatCompletion(
                model = OpenAIModel.GPT3_5,
                messages = arrayListOf(
                    ChatMessage(role = Role.SYSTEM, content = Constants.SYSTEM_REPLY_PROMPT),
                    ChatMessage(
                        role = Role.USER,
                        content = Constants.REPLY_PROMPT.handleText(
                            this,
                            "#{query}" to "'$query'",
                            "#{api_calls}" to result.toString()
                        )
                    )
                )
            )
            val chatInfo = chat(completion) ?: return null
            return if (chatInfo.choices[0].message == null) null
            else chatInfo
        }


        /**
         * 处理chat请求
         * @param text 需要发送的文本
         * @param sessionId 群号或QQ号，用于存储聊天记录（若会话共享则为群号）
         */
        suspend fun User.chatHandle(
            text: String, sessionId: Long
        ) {
            ChatBot.logger.info("进入chatHandle函数")
            val promptName = Constants.safeGetPrompt(sessionId)
            val settings = Constants.PROMPT_SETTING_FILES[promptName]
            val senderText = (text to this).handleText(promptName)
            val currentModule = getCurrentModel()
            val messages = Constants.CHAT_MESSAGES[sessionId] ?: let {
                ChatBot.logger.info("messages为空，return。")
                return
            }
            messages.add(ChatMessage(Role.USER, senderText))
            sendRequest(settings,messages,currentModule)
        }

        /**
         * 处理chat请求
         * @param sessionId 群号或QQ号，用于存储聊天记录（若会话共享则为群号）
         */
        suspend fun User.chatHandle(
            sessionId: Long
        ) {
            ChatBot.logger.info("进入chatHandle函数")
            val promptName = Constants.safeGetPrompt(sessionId)
            val settings = Constants.PROMPT_SETTING_FILES[promptName]
            val currentModule = getCurrentModel()
            val messages = Constants.CHAT_MESSAGES[sessionId] ?: let {
                ChatBot.logger.info("messages为空，return。")
                return
            }
            sendRequest(settings,messages,currentModule)
        }

        /**
         * 发送chat请求
         *
         */
        private suspend fun User.sendRequest(
            settings: PromptChatSettings?,
            messages: ArrayList<ChatMessage>,
            currentModule:OpenAIModel
        ) {

            val completion = if (settings == null)
                ChatCompletion(
                    model = currentModule,
                    messages = messages,
                    maxTokens = ChatConfig.maxTokens,
                    temperature = ChatConfig.temperature,
                    topP = ChatConfig.topP,
                    presencePenalty = ChatConfig.presencePenalty,
                    frequencyPenalty = ChatConfig.frequencyPenalty
                )
            else ChatCompletion(
                model = currentModule,
                messages = messages,
                maxTokens = ChatConfig.maxTokens,
                temperature = settings.temperature,
                topP = settings.topP,
                presencePenalty = settings.presencePenalty,
                frequencyPenalty = settings.frequencyPenalty
            )
            val result = chat(completion) ?: return
            val content = result.choices[0].message?.content ?: let {
                safeSendMessage("未知错误！请联系管理员！")
                return
            }
            messages.add(ChatMessage(Role.ASSISTANT, content))

            ChatBot.logger.info(Constants.GSON.toJson(result))
            val uText = result.choices[0].message?.content

            safeSendAndRecallAsync(
                text = "当前模板:${getPrompt(id)}\n响应结果:${uText}\nusage:${result.usage.totalTokens}",
                timeInMinutes = 5
            )
        }



        /**
         * 处理前后缀
         */
        private suspend fun Pair<String, User>.handleText(promptName: String): String {
            val prefix = (Constants.PREFIX_FILES[promptName] ?: "").handleText(second)
            val suffix = (Constants.SUFFIX_FILES[promptName] ?: "").handleText(second)
            return "$prefix$first$suffix"
        }

        private suspend fun String.handleText(user: User, vararg args: Pair<String, String>): String {
            var text = this.replace("#{userCode}", user.id.toString()).replace("#{userName}", user.nameCardOrNick)
            for (arg in args) {
                text = text.replace(arg.first, arg.second)
            }
            return text
        }

        /**
         * 对话
         */
        private suspend fun User.chat(chatCompletion: ChatCompletion, isOutput: Boolean = true): ChatInfo? {
            return try {
                ChatBot.logger.info("开始处理请求...")
                Constants.OPEN_AI.chat(chatCompletion)
            } catch (e: OpenAIException) {
                ChatBot.logger.error(e.toString())
                if (isOutput) safeSendMessage("$e")
                return null
            } catch (e: SocketTimeoutException) {
                if (isOutput) safeSendMessage("会话超时。")
                return null
            }
        }
    }
}





