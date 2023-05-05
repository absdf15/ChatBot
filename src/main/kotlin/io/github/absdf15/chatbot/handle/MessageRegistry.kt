package io.github.absdf15.chatbot.handle

import io.github.absdf15.chatbot.command.ChatCommand
import io.github.absdf15.chatbot.command.MenuCommand
import io.github.absdf15.chatbot.command.PermissionCommand
import io.github.absdf15.chatbot.utils.TextUtils.Companion.executeCommandFunction
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.MessageEvent

internal object MessageRegistry : SimpleListenerHost() {
    @EventHandler
    suspend fun MessageEvent.handle() {
        executeCommandFunction(
            ChatCommand::class,
            MenuCommand::class,
            PermissionCommand::class
        )
    }
}