package io.github.absdf15.chatbot.utils

import io.github.absdf15.chatbot.utils.MessageUtils.Companion.safeSendMessage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.Message
import java.util.concurrent.TimeUnit

class MessageUtils {
    companion object {

        fun Contact.safeGetCode(): Long{
            return if (this is Member) group.id
            else id
        }

        /**
         * 如果接收者是一个 [Member] 对象，则使用 [Group] 对象发送消息，否则使用常规的 [sendMessage()] 方法发送消息。
         *
         * @param message 要发送的消息。
         * @return  发送消息后得到的回执. 可用于撤回, 引用回复等。
         */
        suspend fun Contact.safeSendMessage(message: Message): MessageReceipt<Contact> {
            return if (this is Member) group.sendMessage(message)
            else sendMessage(message)
        }

        /**
         * 如果接收者是一个 [Member] 对象，则使用 [Group] 对象发送消息，否则使用常规的 [sendMessage()] 方法发送消息。
         *
         * @param text 要发送的文本。
         * @return  发送消息后得到的回执. 可用于撤回, 引用回复等。
         */
        suspend fun Contact.safeSendMessage(text: String): MessageReceipt<Contact> {
            return if (this is Member) group.sendMessage(text)
            else sendMessage(text)
        }

        /**
         * 发送指定的消息到当前联系人，并在指定的时间间隔后撤回该消息。
         *
         * @param message 要发送的消息。
         * @param isSafe 指示是否使用“安全调用”模式来检查接收者的类型。
         * @param time 撤回消息的时间间隔。
         * @param unit 撤回消息时间间隔的时间单位，默认为分钟。
         */
        private suspend fun Contact.sendAndRecall(
            message: Message,
            isSafe: Boolean,
            time: Long,
            unit: TimeUnit = TimeUnit.MINUTES
        ) {
            // 使用 sendMessage 或 safeSendMessage 方法发送消息，具体取决于 isSafe 参数。
            val receipt = if (isSafe) safeSendMessage(message)
            else sendMessage(message)

            // 等待指定的时间间隔，然后撤回已发送的消息。
            receipt.let { messageReceipt ->
                delay(unit.toMillis(time))
                messageReceipt.recall()
            }
        }

        /**
         * 发送指定的消息到当前联系人，并在指定的时间间隔后撤回该消息。
         *
         * @param text 要发送的文本。
         * @param isSafe 指示是否使用“安全调用”模式来检查接收者的类型。
         * @param time 撤回消息的时间间隔。
         * @param unit 撤回消息时间间隔的时间单位，默认为分钟。
         */
        private suspend fun Contact.sendAndRecall(
            text: String,
            isSafe: Boolean,
            time: Long,
            unit: TimeUnit = TimeUnit.MINUTES
        ) {
            // 使用 sendMessage 或 safeSendMessage 方法发送消息，具体取决于 isSafe 参数。
            val receipt = if (isSafe) safeSendMessage(text)
            else sendMessage(text)

            // 等待指定的时间间隔，然后撤回已发送的消息。
            receipt.let { messageReceipt ->
                delay(unit.toMillis(time))
                messageReceipt.recall()
            }
        }

        /**
         * 异步向当前联系对象发送指定的消息，并在指定的时间间隔后撤回该消息。
         *
         * @param message 要发送的消息。
         * @param timeInMinutes 撤回消息的时间间隔（以分钟为单位）。
         */
        @OptIn(DelicateCoroutinesApi::class)
        suspend fun Contact.sendAndRecallAsync(message: Message, timeInMinutes: Long) {
            // 创建一个新协程，在其中异步地发送和撤回消息。
            GlobalScope.launch {
                sendAndRecall(message,false,timeInMinutes)
            }
        }

        /**
         * 异步向当前联系对象发送指定的消息，并在指定的时间间隔后撤回该消息。
         *
         * @param text 要发送的文本。
         * @param timeInMinutes 撤回消息的时间间隔（以分钟为单位）。
         */
        @OptIn(DelicateCoroutinesApi::class)
        suspend fun Contact.sendAndRecallAsync(text: String, timeInMinutes: Long) {
            // 创建一个新协程，在其中异步地发送和撤回消息。
            GlobalScope.launch {
                sendAndRecall(text,false,timeInMinutes)
            }
        }

        /**
         * 异步向当前联系对象安全地发送指定的消息，并在指定的时间间隔后撤回该消息。
         *
         * @param message 要发送的消息。
         * @param timeInMinutes 撤回消息的时间间隔（以分钟为单位）。
         */
        @OptIn(DelicateCoroutinesApi::class)
        suspend fun Contact.safeSendAndRecallAsync(message: Message, timeInMinutes: Long) {
            GlobalScope.launch {
                sendAndRecall(message,true,timeInMinutes)
            }
        }


        /**
         * 异步向当前联系对象安全地发送指定的消息，并在指定的时间间隔后撤回该消息。
         *
         * @param text 要发送的文本。
         * @param timeInMinutes 撤回消息的时间间隔（以分钟为单位）。
         */
        @OptIn(DelicateCoroutinesApi::class)
        suspend fun Contact.safeSendAndRecallAsync(text: String, timeInMinutes: Long) {
            GlobalScope.launch {
                sendAndRecall(text,true,timeInMinutes)
            }
        }
    }
}