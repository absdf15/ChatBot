package io.github.absdf15.chatbot.utils


import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import io.github.absdf15.chatbot.ChatBot
import io.github.absdf15.chatbot.ChatBot.resolveDataFile
import io.github.absdf15.chatbot.config.WebScreenshotConfig
import io.github.absdf15.chatbot.utils.TextUtils.Companion.extractLatexFormulas
import net.mamoe.mirai.utils.info
import org.openqa.selenium.OutputType
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import org.scilab.forge.jlatexmath.TeXIcon
import xyz.cssxsh.selenium.useRemoteWebDriver
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.util.*
import javax.imageio.ImageIO

class MarkdownUtils {
    companion object {

        fun List<String>.convertLatexToImgTags(): MutableMap<String, String> {
            val mutableMap = mutableMapOf<String, String>()
            this.forEach {
                mutableMap[it] = it.convertLatexToImgTag()
            }
            return mutableMap
        }

        fun hasLatexFormulas(text: String): MutableMap<String, String>? {
            val latexFormulas = text.extractLatexFormulas()
            latexFormulas.takeIf { it.isEmpty() }?.let {
                return null
            }
            return latexFormulas.convertLatexToImgTags()
        }

        private fun String.convertLatexToImgTag(): String {
            val formula = TeXFormula(this)
            val icon: TeXIcon = formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, 20f) // 创建 TeXIcon
            val image: BufferedImage = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB)
            val g = image.createGraphics()
            g.color = java.awt.Color.white
            g.fillRect(0, 0, icon.iconWidth, icon.iconHeight)
            // 绘制 TeXIcon 到 BufferedImage
            icon.paintIcon(null, g, 0, 0)
            val os = ByteArrayOutputStream()
            // 将 BufferedImage 转换为 PNG 格式
            ImageIO.write(image, "png", os)
            // 将 PNG 数据转换为 Base64 字符串
            val base64 = Base64.getEncoder().encodeToString(os.toByteArray())
            return "<img src=\"data:image/png;base64,$base64\">"
        }

        fun screenshot(url: URL): File{
            resolveDataFile("tmp/script/").mkdirs()
            val temp = useRemoteWebDriver(config = WebScreenshotConfig) { driver ->
                driver.get(url.toExternalForm())
                ChatBot.logger.info { "Screenshot For $url" }
               driver.getScreenshotAs(OutputType.FILE)
            }
            val screenshot = resolveDataFile("/tmp/${url.host}.png")
            temp.renameTo(screenshot)
            return screenshot
        }

        fun String.convertMarkdownToImg(tags: MutableMap<String, String>): File {
            val fileName = generateRandomFileName()
            val outputFile = resolveDataFile("/tmp/${fileName}.png")
            val htmlFile = resolveDataFile("/tmp/${fileName}.html")
            val url = htmlFile.absolutePath
            ChatBot.logger.info { "url:$url"  }
            val options = MutableDataSet()
            val parser = Parser.builder(options).build()
            val renderer = HtmlRenderer.builder(options).build()
            val html = renderer.render(parser.parse(this))
            htmlFile.writeText(
                "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"></head><body>${
                    html.replaces(
                        tags
                    )
                }</body></html>"
            )
            val temp = useRemoteWebDriver(config = WebScreenshotConfig) { driver ->
                driver.get("file://$url")
                ChatBot.logger.info { "Screenshot For $url" }
                driver.getScreenshotAs(OutputType.FILE)
            }
            val screenshot = resolveDataFile("/tmp/${fileName}.png")
            temp.renameTo(screenshot)
            return screenshot
        }

        private fun String.replaces(map: MutableMap<String, String>): String {
            var text = this
            map.forEach { (latex, image) ->
                text = text.replace(latex, image)
            }
            return text
        }


        private fun generateRandomFileName(): String =
            UUID.randomUUID().toString().substring(0, 10)

    }

}