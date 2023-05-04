package io.github.absdf15.openai.module.search

enum class Api(private val text: String) {
    WikiSearch("WikiSearch"),
    Calculator("Calculator"),
    Google("Google");


    override fun toString(): String {
        return this.text
    }
}