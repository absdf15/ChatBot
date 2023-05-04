package io.github.absdf15.chatbot.config

import net.mamoe.mirai.console.data.*
import xyz.cssxsh.selenium.*

@PublishedApi
internal object WebScreenshotConfig : ReadOnlyPluginConfig("screenshot"), RemoteWebDriverConfig {
    @ValueName("user_agent")
    @ValueDescription("截图UA")
    override val userAgent: String by value(UserAgents.IPAD)

    @ValueName("width")
    @ValueDescription("截图宽度")
    override val width: Int by value(768)

    @ValueName("height")
    @ValueDescription("截图高度")
    override val height: Int by value(1024)

    @ValueName("headless")
    @ValueDescription("无头模式（后台模式）")
    override val headless: Boolean by value(true)

    @ValueName("browser")
    @ValueDescription("默认浏览器")
    override val browser: String by value("Firefox")
}