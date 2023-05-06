package io.github.absdf15.chatbot.annotation

/**
 * 标记被指向的方法
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PointedBy(
    val source: String,
    val index: Int
)