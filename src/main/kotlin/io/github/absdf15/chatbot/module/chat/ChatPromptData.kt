package io.github.absdf15.chatbot.module.chat

import com.google.gson.Gson
import io.github.absdf15.chatbot.ChatBot
import io.github.absdf15.chatbot.ChatBot.resolveDataFile
import io.github.absdf15.chatbot.module.common.Constants
import java.io.File

internal object ChatPromptData {

    private val promptRootFolder: File = resolveDataFile("prompt")

    val promptFolder: File = resolveDataFile("prompt/prompt")
    val prefixFolder: File = resolveDataFile("prompt/prefix")
    val suffixFolder: File = resolveDataFile("prompt/suffix")
    private val settingsFolder: File = resolveDataFile("prompt/settings")
    private val searchPromptFolder: File = resolveDataFile("prompt/searchPrompt")
    fun reload() {
        if (!promptRootFolder.exists()) promptRootFolder.mkdirs()
        if (!promptFolder.exists()) promptFolder.mkdir()
        if (!prefixFolder.exists()) prefixFolder.mkdir()
        if (!suffixFolder.exists()) suffixFolder.mkdir()
        if (!settingsFolder.exists()) settingsFolder.mkdir()
        if (!searchPromptFolder.exists()) searchPromptFolder.mkdir()

        val promptRegex = Regex("^prompt-(.*)\\.txt$")
        val promptFiles = promptFolder.listFiles { file ->
            file.isFile && promptRegex.matches(file.name)
        }
        val prefixRegex = Regex("^prefix-(.*)\\.txt$")
        val prefixFiles = prefixFolder.listFiles { file ->
            file.isFile && prefixRegex.matches(file.name)
        }
        val suffixRegex = Regex("^suffix-(.*)\\.txt$")
        val suffixFiles = suffixFolder.listFiles { file ->
            file.isFile && suffixRegex.matches(file.name)
        }
        val settingsFile = settingsFolder.listFiles()

        searchPromptFolder.listFiles()?.forEach { file ->
            when (file.name) {
                "prompt-api.txt" -> Constants.API_PROMPT = file.readText()
                "system-api.txt" -> Constants.SYSTEM_API_PROMPT = file.readText()
                "prompt-reply.txt" -> Constants.REPLY_PROMPT = file.readText()
                "system-reply.txt" -> Constants.SYSTEM_REPLY_PROMPT = file.readText()

            }
        }
        Constants.PROMPT_FILES.clear()
        Constants.PREFIX_FILES.clear()
        Constants.SUFFIX_FILES.clear()
        Constants.PROMPT_SETTING_FILES.clear()

        promptFiles.forEach {
            Constants.PROMPT_FILES[it.name.removePrefix("prompt-").removeSuffix(".txt")] = it.readText().trim()
        }
        prefixFiles.forEach {
            Constants.PREFIX_FILES[it.name.removePrefix("prefix-").removeSuffix(".txt")] = it.readText().trim()
        }
        suffixFiles.forEach {
            Constants.SUFFIX_FILES[it.name.removePrefix("suffix-").removeSuffix(".txt")] = it.readText().trim()
        }
        settingsFile.forEach {
            val gson = Gson()
            val text = it.readText()
            val chatSettings = gson.fromJson(text, PromptChatSettings::class.java)
            Constants.PROMPT_SETTING_FILES[it.name.removeSuffix(".json")] = chatSettings
        }
    }

}

