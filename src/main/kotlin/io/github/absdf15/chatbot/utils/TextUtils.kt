package io.github.absdf15.chatbot.utils


import com.hankcs.hanlp.dictionary.stopword.CoreStopWordDictionary
import com.hankcs.hanlp.seg.common.Term
import com.hankcs.hanlp.tokenizer.StandardTokenizer
import io.github.absdf15.chatbot.module.common.Constants
import java.util.regex.Pattern

class TextUtils {


    companion object {
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

    }
}


