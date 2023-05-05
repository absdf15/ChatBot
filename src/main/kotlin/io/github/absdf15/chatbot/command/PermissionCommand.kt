package io.github.absdf15.chatbot.command

import io.github.absdf15.chatbot.ChatBot.save
import io.github.absdf15.chatbot.annotation.Command
import io.github.absdf15.chatbot.config.CoreConfig
import io.github.absdf15.chatbot.module.Permission
import io.github.absdf15.chatbot.module.common.ActionParams
import io.github.absdf15.chatbot.module.common.MatchType
import io.github.absdf15.chatbot.utils.MessageUtils.Companion.safeSendMessage
import net.mamoe.mirai.event.events.GroupMessageEvent

object PermissionCommand {
    @Command(value = "群列表", permission = Permission.BOT_ADMIN)
    suspend fun ActionParams.groups(){
        messageEvent.sender.apply {
            val text = buildString {
                append("群列表:\n")
                bot.groups.forEachIndexed { index, group ->
                    val status = if (CoreConfig.enableGroup.contains(group.id)) "激活" else "未启用"
                    append("${index + 1}. ${group.name}[${group.id}][$status]")
                    if (bot.groups.size - 1 != index) append("\n")
                }
            }
            safeSendMessage(text)
        }
    }

    @Command("^(禁用|启用)\\s*群\\s*(\\d*)\$", MatchType.REGEX_MATCH,Permission.BOT_ADMIN)
    suspend fun ActionParams.setGroupStatus(){
        messageEvent.apply {
            val list = command.split("群", limit = 2)
            val code = list[1].trim().toLongOrNull() ?:
            if (this is GroupMessageEvent) subject.id else {
                sender.safeSendMessage("请输入群号！")
                return
            }
            when(list[0]){
                "禁用" -> {
                    CoreConfig.enableGroup.remove(code)
                }
                "启用" -> {
                    CoreConfig.enableGroup.add(code)
                }
                else -> {
                    sender.safeSendMessage("奇怪的情况发生了！")
                }
            }
            CoreConfig.save()
            sender.safeSendMessage("${bot.groups[code]?.name}的状态为:${CoreConfig.enableGroup.contains(code)}")
        }
    }
}