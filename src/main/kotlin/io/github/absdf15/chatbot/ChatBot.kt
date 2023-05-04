package io.github.absdf15.chatbot

import com.vladsch.flexmark.ast.Code
import io.github.absdf15.chatbot.ChatBot.reload
import io.github.absdf15.chatbot.config.*
import io.github.absdf15.chatbot.config.ApiConfig
import io.github.absdf15.chatbot.config.ChatConfig
import io.github.absdf15.chatbot.config.ChatSettings
import io.github.absdf15.chatbot.config.WebScreenshotConfig
import io.github.absdf15.chatbot.handle.ChatMessageHandler
import io.github.absdf15.chatbot.handle.MenuHandler
import io.github.absdf15.chatbot.handle.PermissionHandler
import io.github.absdf15.chatbot.module.chat.ChatPromptData
import io.github.absdf15.chatbot.module.common.Constants
import io.github.absdf15.chatbot.utils.HttpUtils
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.console.data.Value
import net.mamoe.mirai.console.data.findBackingFieldValue
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.util.ConsoleInput
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.registerTo
import net.mamoe.mirai.utils.info

object ChatBot : KotlinPlugin(
    JvmPluginDescription(
        id = "io.github.absdf15.chatbot",
        name = "ChatBot",
        version = "1.0",
    ) {
        author("absdf15")
        dependsOn("xyz.cssxsh.mirai.plugin.mirai-selenium-plugin", true)
    }
) {
    override fun onEnable() {
        logger.info { "ChatBot-Plugin loaded." }
        loadCoreConfig()
        loadChatSettings()
        loadFilter()
        MenuHandler.registerTo(GlobalEventChannel)
        ChatMessageHandler.registerTo(GlobalEventChannel)
        PermissionHandler.registerTo(GlobalEventChannel)

    }

    private fun loadFilter(){
        val file = resolveDataFile("sensi_words.txt")
        if (file.exists().not()) {
            runBlocking {
                Constants.SENSI_WORDS = try {
                    HttpUtils.initFilterWord().readText().split("\n")
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }else{
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

    private fun loadCoreConfig() {
        CoreConfig.reload()
        if (CoreConfig.botOwners.isEmpty()) {
            runBlocking {
                var code: Long? = null
                while (code == null) {
                    code = ConsoleInput.requestInput("请输入你（机器人主人）的QQ号码:").trim().toLongOrNull()
                }
                CoreConfig.botOwners.add(code)
            }
            CoreConfig.save()
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
    }
}