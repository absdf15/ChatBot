# [ChatBot](https://github.com/absdf15/ChatBot)

> Mirai Console 下的 Chat Bot 插件 
> 
> QQ群(零号线): 765751115

相关项目（前置项目）:  
* [Mirai Selenium Plugin](https://github.com/cssxsh/mirai-selenium-plugin) 前置插件，用于生成图片（还未正式使用）
* [QBotCore](https://github.com/absdf15/QBotCore)

参考项目:
1. [EX-ChatGPT](https://github.com/circlestarzero/EX-chatGPT)
2. [Mirai OpenAI Plugin](https://github.com/cssxsh/mirai-openai-plugin)

## 项目介绍

* 一个与`GPT`聊天的插件。~~论坛里已经有那么多了，啊喂。~~

* 接入了`Google`、`WolframAlpha`API，可以让GPT总结。见例图4。

## 使用建议

1. 需要对`gpt`等API进行代理，注意不要选择香港的代理。
2. 需要申请 [OpenAI](https://platform.openai.com) / [WolframAlpha](https://products.wolframalpha.com/api/) / [Google](https://developers.google.com/custom-search/v1/overview?hl=en) / [Search Engine](https://developers.google.com/custom-search/v1/overview?hl=en) 的令牌。
3. `data/io.github.absdf15.chatbot/prompt/prompt` 可以读取该文件夹下的 `prompt` 模板。模板文件需要以 `prompt-*.txt` 为格式，例如：`prompt-魔法全典.txt`。
4. 支持前后缀， `prompt`下的`suffix`和`prefix`文件夹，在与 Bot 进行对话时，会将内容加以前缀。
   - 例如现在使用的模板是`prompt-英语翻译.txt`，那么`prefix-英语翻译.txt` 和 `suffix-英语翻译.txt`的文件就会读取 
   - 处理前的字符串：`今天星期几`。处理后：`Translation：{“今天星期几”}`
   - 目前支持的替换内容`#{userCode}`、`#{userName}`，替换列表还在添加中。
5. 配置文件存储位置`config/io.github.absdf15.chatbot/`，未介绍的直接看配置文件中的注释即可。
6. 配置文件`chat-setting.yml`
    ```
	# 过滤会话启用（没用着，下次迭代修改）
	has_filter_session: true
	# 会话共享（这里是个Map：群号:是否开启。默认关闭。详见：指令列表）
	hasSession_shared: {}
	# 默认Prompt（需要修改默认模板，None会导致请求无法成功）
	default_prompt: NONE
	# 默认Prompt（这里应该是每人每天的最大使用上限，还未实现功能）
	request_limit: 200
    ```
7. 配置文件 `api-config.yml`，填入申请的令牌 [OpenAI](https://platform.openai.com) / [WolframAlpha](https://products.wolframalpha.com/api/) / [Google](https://developers.google.com/custom-search/v1/overview?hl=en)。
    ```
    google_api_key: ''
    google_search_engine_id: ''
    wolfram_api_id: ''
    ```
## 指令
### 指令列表
1. 菜单 （查看基础指令）
2. 指令列表（查看管理指令）
3. 切换人格 [人格名] （可以切换模板）
4. 人格列表 （字面意思）
5. 清空会话 （清空对话历史）
6. /q[对话文本]（该查询接入了外置API）
7. @机器人 [对话文本]
8. 群列表 （查看机器人进了哪些群）
9. 启用群[群号/空]
10. 禁用群[群号/空]
## 初始化启动

### Mirai Console

1.  [MCL下载链接](https://github.com/iTXTech/mcl-installer)

`//TODO`

### 项目启动

1. 首先应从 [Releases](https://github.com/absdf15/ChatBot/releases) 下载 ChatBot 插件，并放入 Plugins 文件夹
2. 启动后，需输入`QQ号`和`OpenAI API KEY` 输入完成后，关闭项目。
3. 编辑`/config/io.github.absdf15.chatbot/api-config.yml`文件，按顺序填入即可
    ```api-config.yml
    google_api_key: ''
    google_search_engine_id: ''
    wolfram_api_id: ''
    ```
## 计划更新

-[ ] 调用链。例如输入`菜单`，后输入`1`可以获取`子菜单`。
-[ ] 接入萌娘本科，Steam API。丰富/q的输出。
-[ ] 群友现在添加模板，由管理审核后加入模板列表。
-[ ] 更丰富的功能，例如：爬取网站，制作成 `execl` 表格。

## 现有功能示例

![图一](img/example-1.jpg)
![图二](img/example-2.jpg)
![图三](img/example-3.jpg)
![图四](img/example-4.jpg)
