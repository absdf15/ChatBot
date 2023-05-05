package io.github.absdf15.chatbot.utils


import com.hankcs.hanlp.dictionary.stopword.CoreStopWordDictionary
import com.hankcs.hanlp.seg.common.Term
import com.hankcs.hanlp.tokenizer.StandardTokenizer
import io.github.absdf15.chatbot.ChatBot
import io.github.absdf15.chatbot.annotation.Command
import io.github.absdf15.chatbot.module.Permission
import io.github.absdf15.chatbot.module.common.ActionParams
import io.github.absdf15.chatbot.module.common.Constants
import io.github.absdf15.chatbot.module.common.MatchType
import io.github.absdf15.chatbot.utils.PermissionUtils.Companion.getPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.CommandContext
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.AtAll
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.QuoteReply
import java.util.regex.Pattern
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.memberExtensionFunctions

class TextUtils {


    companion object {


        data class CommandData(val list: List<String>, val context: CommandContext)

        /**
         * 获取存储位置
         */
        fun String.extractPathFromUrl(): String? {
            val baseUrl = "https://raw.githubusercontent.com/absdf15/promot/main/"
            return if (this.startsWith(baseUrl)) {
                "prompt/" + this.substringAfter(baseUrl)
            } else {
                null
            }
        }

        /**
         * 判断是是否有关键词
         */
        fun String.filter(): Boolean? {
            if (Constants.SENSI_WORDS.isEmpty()) return null
            return Constants.SEARCH_APP.ContainsAny(this)
        }

        fun String.replaceFilterWords(): String? {
            if (Constants.SENSI_WORDS.isEmpty()) return null
            return Constants.SEARCH_APP.Replace(this)
        }

        fun String.extractLatexFormulas(): List<String> {
            val multiLinePattern = Pattern.compile("""(\$\$[\s\S]*?\$\$)""")
            val latexFormulas = mutableListOf<String>()
            // 匹配多行公式
            val multiLineMatcher = multiLinePattern.matcher(this)
            while (multiLineMatcher.find()) {
                latexFormulas.add(multiLineMatcher.group())
            }
            // 匹配单行公式
            val singleLineFormulas = mutableListOf<String>()
            val splitText = this.split("$$")
            splitText.forEach { part ->
                var startIndex = part.indexOf('$')
                while (startIndex != -1) {
                    val endIndex = part.indexOf('$', startIndex + 1)
                    startIndex = if (endIndex != -1) {
                        singleLineFormulas.add(part.substring(startIndex, endIndex + 1))
                        part.indexOf('$', endIndex + 1)
                    } else {
                        -1
                    }
                }
            }
            // 移除重复的公式
            return (latexFormulas + singleLineFormulas).distinct()
        }


        fun String.removeHtmlTags(): String {
            val pattern = Pattern.compile("<.*?>")
            return pattern.matcher(this).replaceAll("")
        }

        fun String.removeStopWords(): String {
            val words: List<Term> = StandardTokenizer.segment(this)
            val keyList = listOf(" ", ".", "。")
            var hasWord = false
            val filteredWords = words.filterNot {
                val result: Boolean
                if (keyList.contains(it.word)) {
                    if (!hasWord) hasWord = false
                    result = !hasWord
                } else {
                    result = CoreStopWordDictionary.contains(it.word) && (!keyList.contains(it.word))
                    hasWord = !result
                }
                result
            }

            val output = filteredWords.joinToString("") {
                it.word
            }
            return output
        }


        private fun Permission.permissionLog(command: String, permission: Permission) {
            ChatBot.logger.info(
                "当前方法:$command 用户权限:$this 所需权限:$permission 是否有拥有执行权限: ${
                    this.hasPermission(
                        permission
                    )
                }"
            )
        }


        /**
         * 解析并执行子指令
         */
        suspend fun MessageEvent.executeCommandFunction(
            vararg classes: KClass<*>
        ) {
            val rawCommand: String = getUnformattedCommand(message)
            val commands = rawCommand.split("\\s+".toRegex(), limit = 2)
            var isRootCommand = false
            var sendText: String? = null
            for (clazz in classes) {
                clazz.annotations.filterIsInstance<Command>().forEach { annotation ->
                    if (commands[0].match(annotation.value, annotation.matchType)) {
                        ChatBot
                        isRootCommand = true
                        sendText = annotation.sendText
                        return@forEach
                    }
                }
                if (isRootCommand) break
            }

            if ((commands.size < 2 || commands[1].isEmpty()) && isRootCommand) {
                if (sendText?.isNotEmpty() == true)
                    subject.sendMessage(sendText ?: "")
                return
            }

            val (command, args) = if (isRootCommand) commands[1].parseCommand() else rawCommand.parseCommand()
            for (clazz in classes) {
                clazz.memberExtensionFunctions.forEach { function ->
                    processCommandAnnotation(function, command, args, rawCommand, clazz)
                }

            }
        }

        private suspend fun MessageEvent.processCommandAnnotation(
            function: KFunction<*>,
            command: String,
            args: List<String>,
            rawData: String,
            clazz: KClass<*>
        ) {
            function.annotations.filterIsInstance<Command>().forEach { annotation ->
                if (command.match(annotation.value, annotation.matchType)) {
                    val actionParams = ActionParams(
                        command, args, rawData, this.sender,
                        sender.getPermission(subject.id), this
                    )
                    if(actionParams.permission.hasPermission(annotation.permission).not()){
                        ChatBot.logger.info("该用户没有权限！")
                        actionParams.permission.permissionLog(command,annotation.permission)
                        return
                    }
                    ChatBot.logger.info("匹配成功！")
                    if (function.isSuspend) {
                        CoroutineScope(Dispatchers.Default).launch {
                            function.callSuspend(clazz.objectInstance,actionParams)
                        }
                    }
                }
            }
        }

        /**
         * 不知道干啥用的方法
         */
        fun StringBuilder.appendIfNotEmpty(prefix: String, value: String?, newLine: Boolean = true) {
            value?.takeIf { it.isNotEmpty() }?.let {
                if (newLine) {
                    append("$prefix${it.trim()}\n")
                } else {
                    append("$prefix${it.trim()}")
                }
            }
        }

        /**
         * 解析参数列表
         * @return 返回 Command指令和后续参数列表
         */
        private fun String.parseCommand(): Pair<String, List<String>> {
            val tokens = this.split("\\s+".toRegex())
            val command = tokens.first()
            val arguments = mutableListOf<String>()
            var i = 1
            while (i < tokens.size) {
                val token = tokens[i]
                if (token.startsWith("{")) {
                    val argument = extractArgument(tokens, i, "{", "}")
                    if (argument == null) {
                        return command to emptyList() // ERROR
                    } else {
                        arguments.add("{$argument}")
                        i += argument.split("\\s+".toRegex()).size
                    }
                } else if (token.startsWith("[")) {
                    val argument = extractArgument(tokens, i, "[", "]")
                    if (argument == null) {
                        return command to emptyList<String>() // ERROR
                    } else {
                        arguments.add("[$argument]")
                        i += argument.split("\\s+".toRegex()).size
                    }
                } else {
                    arguments.add(token)
                    i++
                }
            }
            return Pair(command, arguments)
        }

        /**
         * 解析消息来源的 [MessageChain]，并拼接合成字符串
         *
         * @return 拼接后的字符串
         */
        fun getUnformattedCommand(message: MessageChain): String {
            return if (message.size > 2) {
                buildString {
                    val dropNumber = if (message[1] is QuoteReply) 2 else 1
                    //drop跳过第一个，dropLast跳过最后一个
                    message.drop(dropNumber).dropLast(1).forEach { it ->
                        val content = if (it is At && it !is AtAll) it.contentToString()
                            .substringAfter("@") else it.contentToString()
                        append("$content ")
                    }
                    val last = message.last()
                    if (last is At && last !is AtAll) append(last.contentToString().substringAfter("@"))
                    else append(last.contentToString())
                }
            } else message.getOrNull(1)?.contentToString() ?: ""
        }

        private fun extractArgument(tokens: List<String>, startIndex: Int, opening: String, closing: String): String? {
            val argumentTokens = mutableListOf<String>()
            var i = startIndex
            while (i < tokens.size) {
                val token = tokens[i]
                if (token.endsWith(closing)) {
                    argumentTokens.add(token.removeSuffix(closing))
                    break
                } else {
                    argumentTokens.add(token)
                    i++
                }
            }
            if (i == tokens.size) {
                return null // ERROR
            }
            val argument = argumentTokens.joinToString(" ")
            return argument.removePrefix(opening).replace("\\{", "{").replace("\\}", "}")
        }


        /**
         * 使用指定的匹配方式对给定的字符串进行匹配
         *
         * @param target 用于匹配的字段
         * @param matchType 匹配方式
         *
         * @return 如果匹配成功则返回true，否则返回false
         */
        fun String.match(target: String, matchType: MatchType): Boolean {
            return when (matchType) {
                MatchType.EXACT_MATCH -> this == target
                MatchType.PARTIAL_MATCH -> this.contains(target)
                MatchType.REGEX_MATCH -> Regex(target).matches(this)
            }
        }

    }
}


