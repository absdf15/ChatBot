package io.github.absdf15.chatbot.annotation

import java.util.concurrent.TimeUnit

/**
 * 标记指向的方法
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PointsTo(
    // 0 不添加，-1 永久指向
    val count: Int = 1,
    // 过期时期
    val timeout: Int = 1,
    // 时间单位
    val timeUnit: TimeUnit = TimeUnit.MINUTES
)