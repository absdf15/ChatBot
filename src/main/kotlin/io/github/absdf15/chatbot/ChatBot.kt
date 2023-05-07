package io.github.absdf15.chatbot


import io.github.absdf15.chatbot.config.*
import io.github.absdf15.chatbot.handle.MessageHandler
import io.github.absdf15.chatbot.module.chat.ChatPromptData
import io.github.absdf15.chatbot.module.common.Constants
import io.github.absdf15.chatbot.utils.HttpUtils
import io.github.absdf15.qbot.core.module.QBotPlugin
import io.github.absdf15.qbot.core.module.common.QBotData
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.console.data.Value
import net.mamoe.mirai.console.data.findBackingFieldValue
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.util.ConsoleInput
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.registerTo
import net.mamoe.mirai.utils.info

object ChatBot : QBotPlugin(
    QBotData(
        JvmPluginDescription(
            id = "io.github.absdf15.chatbot",
            name = "ChatBot",
            version = "1.1.2",
        ) {
            author("absdf15")
            dependsOn("xyz.cssxsh.mirai.plugin.mirai-selenium-plugin", true)
            dependsOn("io.github.absdf15.qbot.core","0.1.0",false)
        },
        commandPath = "io.github.absdf15.chatbot.command",
        classes = arrayListOf(MessageHandler::class)
    )
) {
    override fun onEnable() {
        logger.info { "ChatBot-Plugin loaded." }
        loadChatSettings()
        loadFilter()
    }

    private fun loadFilter() {
        val file = resolveDataFile("sensi_words.txt")
        if (file.exists().not()) {
            runBlocking {
                Constants.SENSI_WORDS = try {
                    HttpUtils.initFilterWord().readText().split("\n")
                } catch (e: Exception) {
                    emptyList()
                }
            }
        } else {
            Constants.SENSI_WORDS = try {
                file.readText().split("\n")
            } catch (e: Exception) {
                emptyList()
            }
        }
        if (Constants.SENSI_WORDS.isEmpty()) logger.info("加载过滤词库失败！")
        else {
            Constants.SEARCH_APP.SetKeywords(Constants.SENSI_WORDS)
            logger.info("加载过滤词库成功！")
        }
    }

    private fun loadChatConfig() {
        ChatConfig.reload()
        if (ChatConfig.apiKey.isEmpty()) {
            runBlocking {
                val apiKey = ConsoleInput.requestInput("请输入OpenAI的api key:").trim()
                val value = ChatConfig.findBackingFieldValue<String>("api_key") as Value<String>
                value.value = apiKey
                ChatConfig.save()
            }
        }
    }

    private fun loadChatSettings() {
        logger.info("开始加载...")
        ChatSettings.reload()
        ApiConfig.reload()
        WebScreenshotConfig.reload()
        ChatPromptData.reload()
        if (Constants.PROMPT_FILES.isEmpty())
            runBlocking {
                HttpUtils.initPrompts()
            }
        ChatPromptData.reload()
        loadChatConfig()
        logger.info("加载完成...")
    }
}