package io.github.absdf15.chatbot.module

data class PointPair(
    val userCode: Long,
    val groupCode: Long
) {
    override fun equals(other: Any?): Boolean {
        if (other !is PointPair) return false
        if (this === other) return true
        return userCode == other.userCode && groupCode == other.groupCode
    }

    override fun hashCode(): Int {
        var result = userCode.hashCode()
        result = 31 * result + groupCode.hashCode()
        return result
    }
}