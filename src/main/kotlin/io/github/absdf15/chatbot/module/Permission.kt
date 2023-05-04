package io.github.absdf15.chatbot.module

enum class Permission(private val roleName: String, private val roleCode: Int) {
    BOT_OWNER("bot_owner", 0),
    BOT_ADMIN("bot_admin", 1),
    GROUP_OWNER("group_owner", 2),
    GROUP_ADMIN("group_admin", 3),
    MEMBER("member", 4),
    VISITOR("visitor", 5);

    fun hasPermission(level: Permission): Boolean {
        return this.roleCode <= level.roleCode
    }

    override fun toString(): String {
        return roleName
    }
}