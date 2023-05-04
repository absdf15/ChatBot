package io.github.absdf15.chatbot.utils

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.absdf15.chatbot.ChatBot
import io.github.absdf15.chatbot.ChatBot.resolveDataFile
import io.github.absdf15.chatbot.config.ApiConfig
import io.github.absdf15.chatbot.module.common.Constants
import io.github.absdf15.chatbot.module.common.Constants.Companion.GSON
import io.github.absdf15.chatbot.module.common.Constants.Companion.HTTP_CLIENT
import io.github.absdf15.chatbot.utils.TextUtils.Companion.extractPathFromUrl
import io.github.absdf15.chatbot.utils.TextUtils.Companion.removeHtmlTags
import io.github.absdf15.chatbot.utils.TextUtils.Companion.removeStopWords
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLEncoder


class HttpUtils {
    companion object {
        /**
         * 初始化敏感词
         */
        suspend fun initFilterWord(): File {
            val url = "https://raw.githubusercontent.com/toolgood/ToolGood.Words/master/java/sensi_words.txt"
            val response = HTTP_CLIENT.get(url)
            val file = resolveDataFile("sensi_words.txt")
            file.writeBytes(response.bodyAsChannel().toByteArray())
            return file
        }

        /**
         * 初始化模板
         */
        suspend fun initPrompts() {
            Constants.URLS.forEach {
                val savePath = it.extractPathFromUrl()
                if (savePath?.isNotEmpty() == true)
                    getPromptFile(it, savePath)
            }
        }

        private suspend fun getPromptFile(url: String, savePath: String) {
            val response = HTTP_CLIENT.get(url)
            ChatBot.logger.info("savePath:$savePath")
            val file = resolveDataFile(savePath)
            file.writeBytes(response.bodyAsChannel().toByteArray())

        }

        /**
         * 请求Wiki百科
         */
        suspend fun wiki(query: String): JsonArray? {
            val params = mapOf(
                "action" to "query",
                "format" to "json",
                "list" to "search",
                "srsearch" to query
            )
            val wiki = "https://en.wikipedia.org/w/api.php".handleUrl(params)
            val response = HTTP_CLIENT.get(wiki) {
                contentType(ContentType.Application.Json)
            }
            return response.body<JsonObject>().wikiHandle()
        }

        private fun JsonObject.wikiHandle(): JsonArray? {
            val array = this.getAsJsonObject("query")
                .getAsJsonArray("search")
            val list = mutableListOf<String>()
            array.forEach {
                val jo = it.asJsonObject
                val title = jo.get("title")
                val snippet = jo.get("snippet").asString.removeHtmlTags()
                list.add("$title:$snippet")
            }
            return GSON.toJsonTree(list).asJsonArray
        }

        suspend fun google(query: String, numResults: Int = 2): JsonArray? {
            val params = mapOf(
                "q" to query,
                "key" to ApiConfig.googleApiKey,
                "cx" to ApiConfig.googleSearchEngineId,
                "c2coff" to "0",
                "num" to numResults.toString()
            )
            val google: String = "https://customsearch.googleapis.com/customsearch/v1".handleUrl(params)
            val response = HTTP_CLIENT.get(google) {
                contentType(ContentType.Application.Json)
            }
            return response.body<JsonObject>().googleHandle()
        }

        private fun JsonObject.googleHandle(): JsonArray? {
            val items = this.getAsJsonArray("items")
            val list = mutableListOf<String>()
            items.forEach {
                val jo = it.asJsonObject
                val title = jo.get("title")
                val snippet = jo.get("snippet").asString.removeStopWords()
                list.add("$title:$snippet")
            }
            return GSON.toJsonTree(list).asJsonArray
        }


        suspend fun wolfram(query: String, numResults: Int = 3): JsonArray? {
            val params = mapOf(
                "input" to query.replace("+", " plus "),
                "format" to "plaintext",
                "output" to "JSON",
                "appid" to ApiConfig.wolframApiId
            )

            val wolfram: String = "https://api.wolframalpha.com/v2/query".handleUrl(params)

            val headers = mapOf(
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                "Accept-Language" to "zh-CN,zh;q=0.9,en;q=0.8",
                "Cache-Control" to "no-cache",
                "Connection" to "keep-alive",
                "DNT" to "1",
                "Pragma" to "no-cache",
                "Sec-Fetch-Dest" to "document",
                "Sec-Fetch-Mode" to "navigate",
                "Sec-Fetch-Site" to "none",
                "Sec-Fetch-User" to "?1",
                "Upgrade-Insecure-Requests" to "1",
                "User-Agent" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36",
                "sec-ch-ua" to "\"Chromium\";v=\"110\", \"Not A(Brand\";v=\"24\", \"Google Chrome\";v=\"110\"",
                "sec-ch-ua-mobile" to "?0",
                "sec-ch-ua-platform" to "\"macOS\""
            )

            val response = HTTP_CLIENT.get(wolfram) {
                contentType(ContentType.Application.Json)
                headers {
                    headers.forEach { (key, value) ->
                        append(key, value)
                    }
                }
            }
            return response.body<JsonObject>().wolframHandle()
        }

        private fun JsonObject.wolframHandle(): JsonArray? {
            val pods = this.getAsJsonObject("queryresult")
                .getAsJsonArray("pods")
            val podIds = mutableListOf<String>()
            val subPods = mutableListOf<JsonArray>()
            val podsPlaintext = mutableListOf<String>()
            pods.forEach {
                podIds.add(it.asJsonObject.get("id").asString)
                subPods.add(it.asJsonObject.getAsJsonArray("subpods"))
            }

            subPods.forEach {
                val plaintext = it.joinToString("\n") { i -> i.asJsonObject.get("plaintext").asString }
                podsPlaintext.add(plaintext)
            }
            val result = mutableListOf<String>()
            podIds.forEachIndexed { index, id ->
                result.add("$id:${podsPlaintext[index]}")
            }
            return GSON.toJsonTree(result).asJsonArray
        }

        private fun String.handleUrl(param: Map<String, String>): String {
            return "$this?${param.toUrlEncodedString()}"
        }

        private fun Map<String, String>.toUrlEncodedString(): String {
            return this.map {
                "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
            }.joinToString(separator = "&")
        }
    }
}