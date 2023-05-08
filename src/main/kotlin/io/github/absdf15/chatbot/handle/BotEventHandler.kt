package io.github.absdf15.chatbot.handle

import io.github.absdf15.chatbot.ChatBot
import io.github.absdf15.qbot.core.config.CoreConfig
import io.github.absdf15.qbot.core.module.common.Permission
import io.github.absdf15.qbot.core.utils.PermissionUtils.Companion.hasPermission
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent

object BotEventHandler: SimpleListenerHost() {
    suspend fun BotInvitedJoinGroupRequestEvent.call(){
       val friend = bot.getFriend(invitorId)?: return
        ChatBot.logger.info("$friend:" + friend.hasPermission(Permission.BOT_ADMIN))
        if (friend.hasPermission(Permission.BOT_ADMIN)){
            accept()
        }
    }
}