package io.github.absdf15.chatbot.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.ValueName
import net.mamoe.mirai.console.data.value

object SlackConfig: AutoSavePluginConfig("slack-config")  {

    @ValueName("slack-user-token")
    @ValueDescription("只需要打开你想要发送消息的频道，然后在地址栏中找到")
    val slackUserToken: String by value("")
    @ValueName("claude-id")
    @ValueDescription("本ID是用来标识Claude回复的消息的")
    val claudeId: String by value("")
//    @ValueName("wait-message")
//    @ValueDescription("等待消息")
//    val waitMessage by value(true)

}