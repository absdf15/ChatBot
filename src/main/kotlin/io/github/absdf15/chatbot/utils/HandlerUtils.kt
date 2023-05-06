package io.github.absdf15.chatbot.utils

import io.github.absdf15.chatbot.ChatBot
import io.github.absdf15.chatbot.annotation.Command
import io.github.absdf15.chatbot.annotation.PointedBy
import io.github.absdf15.chatbot.annotation.PointsTo
import io.github.absdf15.chatbot.config.CommandConfig
import io.github.absdf15.chatbot.module.ActionParams
import io.github.absdf15.chatbot.module.Permission
import io.github.absdf15.chatbot.module.PointPair
import io.github.absdf15.chatbot.module.common.Constants
import io.github.absdf15.chatbot.utils.PermissionUtils.Companion.getPermission
import io.github.absdf15.chatbot.utils.TextUtils.Companion.match
import io.github.absdf15.chatbot.utils.TextUtils.Companion.parseCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.mamoe.mirai.event.events.MessageEvent
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.memberExtensionFunctions
import kotlin.reflect.full.memberFunctions

class HandlerUtils {
    companion object {
        private fun Permission.permissionLog(command: String, permission: Permission) {
            ChatBot.logger.info(
                "当前方法:$command 用户权限:$this 所需权限:$permission 是否有拥有执行权限: ${
                    this.hasPermission(
                        permission
                    )
                }"
            )
        }


        /**
         * 解析并执行子指令
         */
        suspend fun MessageEvent.executeCommandFunction(
            vararg classes: KClass<*>
        ): Boolean {
            val rawCommand: String = TextUtils.getUnformattedCommand(message)
            val pointPair = PointPair(sender.id, subject.id)
            val pointSource = Constants.POINT_MAP[pointPair]
            val pointIndex = rawCommand.toIntOrNull()
            val commands = rawCommand.split("\\s+".toRegex(), limit = 2)
            var isRootCommand = false
            var sendText: String? = null
            if (pointIndex == null || pointSource == null) {
                for (clazz in classes) {
                    clazz.annotations.filterIsInstance<Command>().forEach { annotation ->
                        if (commands[0].match(annotation.value, annotation.matchType)) {
                            ChatBot
                            isRootCommand = true
                            sendText = annotation.sendText
                            return@forEach
                        }
                    }
                    if (isRootCommand) break
                }

                if ((commands.size < 2 || commands[1].isEmpty()) && isRootCommand) {
                    if (sendText?.isNotEmpty() == true)
                        subject.sendMessage(sendText ?: "")
                    return true
                }

                val (command, args) = if (isRootCommand) commands[1].parseCommand() else rawCommand.parseCommand()
                for (clazz in classes) {
                    CommandConfig.command.forEach { (fullName, commandData) ->
                        if (command.match(commandData.context, commandData.matchType)) {
                            val result = processCommandAliasAnnotation(fullName, command, args, rawCommand)
                            if (result) return true
                        }
                    }
                    clazz.memberExtensionFunctions.forEach { function ->
                        val result = processCommandAnnotation(function, command, args, rawCommand, clazz)
                        if (result) return true
                    }
                }
            } else {
                for (clazz in classes) {
                    clazz.memberExtensionFunctions.forEach { function ->
                        val result = processPointedByAnnotation(function, pointIndex, pointSource, pointPair, clazz)
                        if (result) return true
                    }
                }
            }
            return false
        }

        private suspend fun MessageEvent.processCommandAnnotation(
            function: KFunction<*>,
            command: String,
            args: List<String>,
            rawData: String,
            clazz: KClass<*>
        ): Boolean {
            function.annotations.filterIsInstance<Command>().forEach { annotation ->
                if (command.match(annotation.value, annotation.matchType)) {
                    val actionParams = ActionParams(
                        command, args, rawData, this.sender,
                        sender.getPermission(subject.id), this
                    )
                    actionParams.processCommand(this, annotation, function, clazz)
                }
            }
            return false
        }

        private suspend fun MessageEvent.processCommandAliasAnnotation(
            fullMethodName: String,
            command: String,
            args: List<String>,
            rawData: String,
        ): Boolean {
            // 分割类名和方法名
            val lastDotIndex = fullMethodName.lastIndexOf('.')
            val className = fullMethodName.substring(0, lastDotIndex)
            val methodName = fullMethodName.substring(lastDotIndex + 1)
            // 获取 KClass 实例
            val clazz = Class.forName(className).kotlin
            // 找到需要的方法
            val function = clazz.memberFunctions.firstOrNull { it.name == methodName }

            function?.annotations?.filterIsInstance<Command>()?.forEach { annotation ->
                val actionParams = ActionParams(
                    command, args, rawData, this.sender,
                    sender.getPermission(subject.id), this
                )
                actionParams.processCommand(this, annotation, function, clazz)
            }
            return false
        }


        private suspend fun ActionParams.processCommand(
            messageEvent: MessageEvent,
            annotation: Command,
            function: KFunction<*>,
            clazz: KClass<*>
        ): Boolean {
            if (permission.hasPermission(annotation.permission).not()) {
                ChatBot.logger.info("该用户没有权限！")
                permission.permissionLog(command, annotation.permission)
                return false
            }
            ChatBot.logger.info("匹配成功！")
            ChatBot.logger.info("该方法名为:${function.name}")
            if (function.isSuspend) {
                CoroutineScope(Dispatchers.Default).launch {
                    function.callSuspend(clazz.objectInstance, this)
                    function.annotations.forEach {
                        if (it is PointsTo) {
                            Constants.POINT_MAP[PointPair(messageEvent.sender.id, messageEvent.subject.id)] =
                                annotation.value
                            return@forEach
                        }
                    }
                }
                return true
            }
            return false
        }

        private suspend fun MessageEvent.processPointedByAnnotation(
            function: KFunction<*>,
            index: Int,
            source: String,
            pointPair: PointPair,
            clazz: KClass<*>
        ): Boolean {
            function.annotations.filterIsInstance<PointedBy>().forEach { annotation ->
                if (index == annotation.index && annotation.source == source) {
                    val actionParams = ActionParams(
                        index.toString(), emptyList(), index.toString(), this.sender,
                        sender.getPermission(subject.id), this
                    )
                    var hasCommandAnnotation = false
                    function.annotations.filterIsInstance<Command>().forEach {
                        if (actionParams.permission.hasPermission(it.permission).not()) {
                            ChatBot.logger.info("该用户没有权限！")
                            actionParams.permission.permissionLog(index.toString(), it.permission)
                            return false
                        } else hasCommandAnnotation = true
                    }
                    if (function.isSuspend && hasCommandAnnotation) {
                        ChatBot.logger.info("匹配成功！")
                        CoroutineScope(Dispatchers.Default).launch {
                            function.callSuspend(clazz.objectInstance, actionParams)
                            Constants.POINT_MAP.remove(pointPair)
                        }
                        return true
                    }
                }
            }
            return false
        }
    }
}