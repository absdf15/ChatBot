package io.github.absdf15.chatbot.command

import io.github.absdf15.qbot.core.annotation.Command
import io.github.absdf15.qbot.core.annotation.Component
import io.github.absdf15.qbot.core.config.CoreConfig
import io.github.absdf15.qbot.core.module.common.ActionParams
import io.github.absdf15.qbot.core.module.common.MatchType
import io.github.absdf15.qbot.core.module.common.Permission
import io.github.absdf15.qbot.core.utils.ConfigUtils.Companion.save
import io.github.absdf15.qbot.core.utils.MessageUtils.Companion.safeSendMessage
import net.mamoe.mirai.event.events.GroupMessageEvent

@Component
object PermissionCommand {
    @Command("群列表", permission = Permission.BOT_ADMIN)
    suspend fun ActionParams.groups() {
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

    @Command("^(禁用|启用)*群\\s*(\\d*)\$", MatchType.REGEX_MATCH, Permission.BOT_ADMIN)
    suspend fun ActionParams.setGroupStatus() {
        messageEvent.apply {
            val list = command.split("群", limit = 2)
            val code = list.getOrNull(0)?.trim()?.toLongOrNull() ?: args.getOrNull(0)?.toLongOrNull()
            ?: if (this is GroupMessageEvent) subject.id else {
                sender.safeSendMessage("请输入群号！")
                return
            }
            when (list[0]) {
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
            save(CoreConfig)
            sender.safeSendMessage("${bot.groups[code]?.name}的状态为:${CoreConfig.enableGroup.contains(code)}")
        }

    }

    @Command("^(添加|删除)*管理\\s*(\\d*)\$", MatchType.REGEX_MATCH, Permission.BOT_OWNER)
    suspend fun ActionParams.setBotAdmin() {
        messageEvent.apply {
            val list = command.split("管理", limit = 2)
            val code = list.getOrNull(0)?.trim()?.toLongOrNull() ?: args.getOrNull(0)?.toLongOrNull()
            ?:let{
                sender.safeSendMessage("请输入QQ号！")
                return
            }
            when (list[0]) {
                "删除" -> {
                    CoreConfig.botAdmins.remove(code)
                }

                "添加" -> {
                    CoreConfig.botAdmins.add(code)
                }

                else -> {
                    sender.safeSendMessage("奇怪的情况发生了！")
                }
            }
            save(CoreConfig)
            sender.safeSendMessage("$code 的状态为:${CoreConfig.botAdmins.contains(code)}")
        }
    }
}