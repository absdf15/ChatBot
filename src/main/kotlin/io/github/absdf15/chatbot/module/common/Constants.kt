package io.github.absdf15.chatbot.module.common

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.absdf15.chatbot.command.ChatCommand
import io.github.absdf15.chatbot.command.MenuCommand
import io.github.absdf15.chatbot.command.PermissionCommand
import io.github.absdf15.chatbot.config.ApiConfig
import io.github.absdf15.chatbot.config.ChatConfig
import io.github.absdf15.chatbot.config.ChatSettings
import io.github.absdf15.chatbot.module.chat.PromptChatSettings
import io.github.absdf15.chatbot.module.chat.TempInfo
import io.github.absdf15.openai.OpenAI
import io.github.absdf15.openai.module.ChatMessage
import io.github.absdf15.openai.module.OpenAIModel
import io.github.absdf15.qbot.core.utils.MessageUtils.Companion.safeSendMessage
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.sync.Mutex
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.At
import toolgood.words.StringSearch
import kotlin.reflect.KClass

class Constants {
    companion object {

        /**
         * 需要下载的模板列表
         */
        val URLS: List<String> = listOf(
            "https://raw.githubusercontent.com/absdf15/promot/main/searchPrompt/prompt-reply.txt",
            "https://raw.githubusercontent.com/absdf15/promot/main/searchPrompt/prompt-api.txt",
            "https://raw.githubusercontent.com/absdf15/promot/main/searchPrompt/system-reply.txt",
            "https://raw.githubusercontent.com/absdf15/promot/main/searchPrompt/system-api.txt",
            "https://raw.githubusercontent.com/absdf15/promot/main/prompt/prompt-英语翻译.txt",
            "https://raw.githubusercontent.com/absdf15/promot/main/prompt/prompt-魔法全典.txt",
            "https://raw.githubusercontent.com/absdf15/promot/main/prompt/prompt-阿波罗.txt",
            "https://raw.githubusercontent.com/absdf15/promot/main/prefix/prefix-英语翻译.txt",
            "https://raw.githubusercontent.com/absdf15/promot/main/suffix/suffix-英语翻译.txt",
        )

        /**
         * 待审核数据集 TODO
         */
        val TEMP_DATA: MutableList<TempInfo> = mutableListOf()
        /**
         * 聊天会话缓存
         */
        val CHAT_MESSAGES: MutableMap<Long, ArrayList<ChatMessage>> = mutableMapOf<Long, ArrayList<ChatMessage>>()

        val CHAT_MODEL: MutableMap<Long, OpenAIModel> = mutableMapOf<Long, OpenAIModel>()

        private val DEFAULT_MODEL: OpenAIModel = OpenAIModel.GPT3_5

        /**
         * Prompt Name储存
         */
        private val INDEPENDENT_PROMPT: MutableMap<Long, String> = mutableMapOf()

        /**
         * OpenAI请求
         */
        val OPEN_AI: OpenAI by lazy {
            OpenAI(ChatConfig.apiKey)
        }

        /**
         * 协程锁
         */
        private val USER_REQUEST_STATUS = HashMap<Long, Boolean>()
        val USER_MUTEX = Mutex()

        val HTTP_CLIENT: HttpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                gson {
                    disableHtmlEscaping()
                }
            }
            install(HttpTimeout) {
                socketTimeoutMillis = 30_000L
                connectTimeoutMillis = 30_000L
                requestTimeoutMillis = 30_000L
            }
            BrowserUserAgent()
            ContentEncoding()
        }

        val GSON: Gson = GsonBuilder()
            .disableHtmlEscaping()
            .create()

        /**
         * 模板缓存
         */
        val PROMPT_FILES: MutableMap<String, String> = mutableMapOf()
        val PREFIX_FILES: MutableMap<String, String> = mutableMapOf()
        val SUFFIX_FILES: MutableMap<String, String> = mutableMapOf()
        val PROMPT_SETTING_FILES: MutableMap<String, PromptChatSettings> = mutableMapOf()

        /**
         * query 模板
         */
        lateinit var SYSTEM_API_PROMPT: String
        lateinit var API_PROMPT: String
        lateinit var SYSTEM_REPLY_PROMPT: String
        lateinit var REPLY_PROMPT: String

        /**
         * 敏感词词库
         */
        lateinit var SENSI_WORDS: List<String>

        val SEARCH_APP = StringSearch()

        private suspend fun Long.lock(): Boolean {
            if (USER_REQUEST_STATUS[this] == true) return false
            USER_REQUEST_STATUS[this] = true
            return true
        }

        private suspend fun Long.unlock() {
            USER_REQUEST_STATUS[this] = false
        }

        suspend fun <T> User.fetchApiResult(action: suspend () -> T): T? {
            id.lock().takeIf { !it }?.let {
                safeSendMessage(At(this) + " 已有一个请求正在执行，请稍后...")
                return null
            }
            var result: T? = null
            try {
                result = action.invoke()
            } catch (e: Exception) {
                safeSendMessage("未知错误，请联系管理员。\nTips: 数学公式解析有一定概率失败，可以重新试试。")
            } finally {
                id.unlock()
            }
            return result
        }

        fun User.getCurrentModel(): OpenAIModel {
            return if (this is Member && ChatSettings.hasSessionShared[group.id] == true) CHAT_MODEL[group.id]
                ?: DEFAULT_MODEL
            else CHAT_MODEL[id] ?: DEFAULT_MODEL
        }

        fun getPrompt(id: Long): String? {
            if (INDEPENDENT_PROMPT[id] == null)
                INDEPENDENT_PROMPT[id] = ChatSettings.defaultPrompt
            return INDEPENDENT_PROMPT[id]
        }

        fun safeGetPrompt(id: Long): String {
            val prompt = getPrompt(id)
            return prompt ?: ChatSettings.defaultPrompt
        }

        fun setPrompt(id: Long, value: String): Boolean {
            val valueT = value.trim()
            if (PROMPT_FILES[valueT] == null) return false
            INDEPENDENT_PROMPT[id] = valueT
            return true
        }
    }
}