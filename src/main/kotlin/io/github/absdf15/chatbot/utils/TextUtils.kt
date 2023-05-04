package io.github.absdf15.chatbot.utils


import com.hankcs.hanlp.dictionary.stopword.CoreStopWordDictionary
import com.hankcs.hanlp.seg.common.Term
import com.hankcs.hanlp.tokenizer.StandardTokenizer
import io.github.absdf15.chatbot.ChatBot
import io.github.absdf15.chatbot.module.Permission
import io.github.absdf15.chatbot.module.common.Constants
import io.github.absdf15.chatbot.utils.PermissionUtils.Companion.getPermission
import net.mamoe.mirai.console.command.CommandContext
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.AtAll
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.QuoteReply
import java.util.regex.Pattern

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
                var result = false
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

        suspend fun Contact.executeSubCommand(
            inputCommand: String,
            subCommand: String,
            permission: Permission? = Permission.MEMBER,
            groupCode: Long? = null,
            action: suspend () -> Unit
        ): Boolean {
            if (inputCommand == subCommand) {
                if (permission != null) {
                    val code = groupCode ?: if (this is Member) group.id else null
                    val userPermission = getPermission(code)
                    ChatBot.logger.info("当前方法:$subCommand 用户权限:$userPermission 所需权限:$permission 是否有拥有执行权限: ${userPermission.hasPermission(permission)}")
                    if (userPermission.hasPermission(permission))
                        with(this) {
                            action()
                            return true
                        }
                } else {
                    ChatBot.logger.info("当前方法:$subCommand 用户权限:${getPermission()} 所需权限:${Permission.VISITOR} 是否有拥有执行权限: ${getPermission().hasPermission(Permission.VISITOR)}")
                    with(this) {
                        action()
                        return true
                    }
                }
            }
            return false
        }


        /**
         * 获取原始数据
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

        /**
         * 解析并执行指令
         */
        suspend fun MessageEvent.parseCommandAndExecute(
            sendText: String?,
            isRootCommand: Boolean = false,
            action: suspend Contact.(String, List<String>, String) -> Unit
        ): Boolean {
            val unformattedCommand: String = getUnformattedCommand(message)
            val commands = unformattedCommand.split("\\s+".toRegex(), limit = 2)
            if (unformattedCommand.isEmpty() || ((commands.size < 2 || commands[1].isEmpty()) && !isRootCommand)
            ) {
                if (sendText?.isNotEmpty() == true)
                    subject.sendMessage(sendText)
                return false
            }
            val (command, args) = if (isRootCommand) unformattedCommand.parseCommand()
            else commands[1].parseCommand()

            this.sender.action(command, args, unformattedCommand)
            return true
        }


        fun String?.notEmpty(): Boolean = this?.isNotEmpty() == true


        fun StringBuilder.appendIfNotEmpty(prefix: String, value: String?, newLine: Boolean = true) {
            value?.takeIf { it.isNotEmpty() }?.let {
                if (newLine) {
                    append("$prefix${it.trim()}\n")
                } else {
                    append("$prefix${it.trim()}")
                }
            }
        }

        fun String.parseCommand(): Pair<String, List<String>> {
            val tokens = this.split("\\s+".toRegex())
            val command = tokens.first()
            val arguments = mutableListOf<String>()
            var i = 1
            while (i < tokens.size) {
                val token = tokens[i]
                if (token.startsWith("{")) {
                    val argument = extractArgument(tokens, i, "{", "}")
                    if (argument == null) {
                        return command to emptyList<String>() // ERROR
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

        private fun unescape(c: Char): Char {
            return when (c) {
                'n' -> '\n'
                'r' -> '\r'
                't' -> '\t'
                '\\' -> '\\'
                else -> throw IllegalArgumentException("Invalid escape character: $c")
            }
        }


    }
}