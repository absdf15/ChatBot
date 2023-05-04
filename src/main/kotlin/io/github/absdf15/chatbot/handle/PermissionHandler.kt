package io.github.absdf15.chatbot.handle

import io.github.absdf15.chatbot.ChatBot.save
import io.github.absdf15.chatbot.config.CoreConfig
import io.github.absdf15.chatbot.module.Permission
import io.github.absdf15.chatbot.utils.MessageUtils.Companion.safeSendMessage
import io.github.absdf15.chatbot.utils.TextUtils.Companion.executeSubCommand
import io.github.absdf15.chatbot.utils.TextUtils.Companion.parseCommandAndExecute
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.MessageEvent

/**
 * 权限控制
 */
internal object PermissionHandler : SimpleListenerHost() {
    @EventHandler
    suspend fun MessageEvent.permissionCommand() {
        parseCommandAndExecute(null, true) { command, args, _ ->
            executeSubCommand(command, "群列表", Permission.BOT_ADMIN) {
                val text = buildString {
                    append("群列表:\n")
                    bot.groups.forEachIndexed { index, group ->
                        val status = if (CoreConfig.enableGroup.contains(group.id)) "激活" else "未启用"
                        append("${index + 1}. ${group.name}[${group.id}][$status]")
                        if (bot.groups.size - 1 != index) append("\n")
                    }
                }
                safeSendMessage(text)
            }.takeIf { it } ?: executeSubCommand(command, "启用群", Permission.BOT_ADMIN) {
                val id = args.getOrNull(0)?.toLongOrNull() ?: if (this is Member) group.id else {
                    safeSendMessage("请输入要启用的群号！")
                    return@executeSubCommand
                }
                if (CoreConfig.enableGroup.contains(id)) {
                    safeSendMessage("该群聊己启用")
                    return@executeSubCommand
                }
                CoreConfig.enableGroup.add(id)
                CoreConfig.save()
            }.takeIf { it } ?: executeSubCommand(command, "禁用群", Permission.BOT_ADMIN) {
                val id = args.getOrNull(0)?.toLongOrNull() ?: if (this is Member) group.id else {
                    safeSendMessage("请输入要禁用的群号！")
                    return@executeSubCommand
                }
                if (CoreConfig.enableGroup.contains(id).not()) {
                    safeSendMessage("该群聊未启用！")
                    return@executeSubCommand
                }
                CoreConfig.enableGroup.remove(id)
                CoreConfig.save()
            }

        }
    }
}