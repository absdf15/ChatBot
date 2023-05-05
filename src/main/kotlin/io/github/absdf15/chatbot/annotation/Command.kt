package io.github.absdf15.chatbot.annotation

import io.github.absdf15.chatbot.module.Permission
import io.github.absdf15.chatbot.module.common.MatchType

annotation class Command(
    val value: String,
    val matchType: MatchType = MatchType.EXACT_MATCH,
    val permission: Permission = Permission.MEMBER,
    // 在方法上该参数无用
    val sendText: String = ""
)
