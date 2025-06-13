package org.utcook.i18nHelper

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import kotlinx.coroutines.*
import org.utcook.i18nHelper.utils.MapUtils
import org.utcook.i18nHelper.utils.StringsXmlUtils
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import kotlin.system.measureTimeMillis

class MainAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        // 触发保存所有文件，避免没有保存，扫描的时候没有扫描到
        val fileDocumentManager = FileDocumentManager.getInstance()
        fileDocumentManager.saveAllDocuments()

        val project = event.project ?: return
        val rootDir = Paths.get(project.basePath ?: "") // 获取项目根目录

        // 打印根目录路径
        println("Root Directory: ${rootDir.toAbsolutePath()}")

        // 找到包含 src 文件夹的一级模块文件夹
        val moduleDirs = findModuleDirsWithSrc(rootDir)
        if (moduleDirs.isEmpty()) {
            Messages.showErrorDialog("此操作仅适用于 strings.xml 文件。", "无效文件")
            return
        }

        // 提取模块名称（最后一个文件夹名称）
        val moduleNames = moduleDirs.map { it.fileName.toString() }.distinct()

        // 显示选择对话框
        val selectedIndex = Messages.showChooseDialog(
            "选择需要翻译的模块:",
            "选择模块",
            moduleNames.toTypedArray(),
            moduleNames.firstOrNull(), // 默认选中第一个模块
            null
        )

        // 检查用户是否取消了选择
        if (selectedIndex < 0) {
            println("取消模块选择")
            return
        }

        // 根据用户选择的索引获取对应的模块路径
        val selectedModuleName = moduleNames[selectedIndex]
        val selectedModuleDir = moduleDirs.firstOrNull { it.fileName.toString() == selectedModuleName }
        if (selectedModuleDir == null) {
            Messages.showErrorDialog("无法找到选中的模块。", "错误")
            return
        }

        // 处理选中的模块
        println("处理选中的模块: $selectedModuleName ，模块的路径: ${selectedModuleDir.toAbsolutePath()}")


        // 创建并运行后台任务
        val backgroundTask = object : Task.Backgroundable(
            project, "大模型接口调用", true
        ) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true // 设置为不确定进度模式
                indicator.text = "努力翻译中，请耐心等待..." // 设置进度文本

                val latch = CountDownLatch(1)
                ApplicationManager.getApplication().executeOnPooledThread {
                    CoroutineScope(Dispatchers.IO).launch {
                        // 查找模块中的所有 strings.xml 文件并标记语言
                        val stringsFiles = StringsXmlUtils.readAllStringsResources(project, selectedModuleDir)
                        if (stringsFiles.isEmpty()) {
                            Messages.showErrorDialog("指定模块中找不到 strings.xml 文件，请检查", "错误")
                            latch.countDown()
                            return@launch
                        }

                        val defaultStringsXml = stringsFiles.filter { it.language == "default" }[0]
                        val otherLanguageStringsXml = stringsFiles.filter { it.language != "default" }
                        processTranslationsConcurrently(defaultStringsXml, otherLanguageStringsXml)
                        latch.countDown()
                    }
                }
                // 等待任务完成
                latch.await()
            }


            override fun onSuccess() {
                super.onSuccess()
                Messages.showMessageDialog(
                    project,
                    "自动翻译完成。",
                    "Success",
                    Messages.getInformationIcon()
                )
            }

            override fun onCancel() {
                super.onCancel()
                Messages.showMessageDialog(
                    project,
                    "操作已取消.",
                    "Canceled",
                    Messages.getWarningIcon()
                )
            }
        }

        // 启动任务
        backgroundTask.queue()
    }

    /**
     * 并发处理多语言翻译任务。
     *
     * @param defaultStringsXml 默认字符串资源。
     * @param otherLanguageStringsXml 其他语言的字符串资源列表。
     */
    fun processTranslationsConcurrently(
        defaultStringsXml: StringsXmlUtils.StringsResource,
        otherLanguageStringsXml: List<StringsXmlUtils.StringsResource>
    ) {
        // 创建一个协程作用域
        runBlocking {
            val timeTaken = measureTimeMillis {
                // 并发处理每个语言的翻译任务
                val jobs = otherLanguageStringsXml.map { stringsResource ->
                    async(Dispatchers.IO) {
                        try {
                            // 找出在默认strings存在，但是当前语言不存在翻译
                            val diffKeyValueMap =
                                MapUtils.getDifferenceByKey(defaultStringsXml.keyValueMap, stringsResource.keyValueMap)
                            println("${stringsResource.language}语言遗漏的翻译：$diffKeyValueMap")

                            if (diffKeyValueMap.isNotEmpty()) {
                                // 调用翻译服务
                                translateByLLM(stringsResource.language, diffKeyValueMap) { translatedText ->
                                    try {
                                        // 将翻译完成后的内容追加到遗漏的文件中
                                        StringsXmlUtils.insertMapToStringsXml(
                                            MapUtils.jsonToMap(translatedText) as Map<String, String>,
                                            stringsResource
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            println("处理 ${stringsResource.language} 时发生错误: ${e.message}")
                        }
                    }
                }

                // 等待所有任务完成
                jobs.awaitAll()
            }

            println("所有翻译任务完成，耗时: $timeTaken ms")
        }
    }

    private fun translateByLLM(
        targetLanguage: String,
        translateMap: Map<String, String>,
        onFinish: (result: String) -> Unit
    ) {
        val prompt = """
            ## 角色
            你是一个资深的本地化翻译专家，精通各种国家的语言。
            
            ## 任务
            1. 翻译任务：请严格遵循【翻译任务规则】规则，可以将【原文】翻译为【目标语言】
            2. 检查译文任务：请严格遵循【检查译文规则】规则，检查翻译后的译文是否符合规范
                  
            ## 翻译任务规则
            1. 禁止翻译key。 
            2. 请翻译value内容，保留XML标签和占位符（如 %s、@string等）。
            3. 请直接输出结果，不要给多余的解释。
            4. 请返回json格式数据。
            5. 请保证翻译的译文顺序和原文一致。
            6. 当原文的key是中文的时候，禁止翻译key为繁体中文。
            
            ## 检查译文规则
            1. 如果译文中有已转义字符（如 \n 等），请使用反斜杠（\）转义，即改为 \\n。
            2. 请保证同一个词或句子的译文一致。
            3. 请确保key没有被翻译，value的翻译准确。

            ## 目标语言
            ```json
            $targetLanguage
            ```

            ## 原文
            ${MapUtils.mapToJsonString(translateMap)}
        """.trimIndent().replace("\\", "\\\\") // 转义反斜杠
            .replace("\"", "\\\"") // 转义双引号
            .replace("\n", "\\n")  // 转义换行符
            .replace("\r", "\\r")  // 转义回车符

        // 使用 SiliconFlowRequester 发送翻译请求
        val token = "sk-dqulrmprimvvdzmmsdqfngdzojtfbkpzymafzbxuygxvoguc" // 替换为实际的 API Token
        val requester = SiliconFlowRequester(token)
        val latch = CountDownLatch(1) // 用于等待异步操作完成
        val result = StringBuilder()
        requester.sendRequestStream(prompt, { message ->
            print(message)
            result.append(message)
        }) {
            println("流式调用完成： $result")
            // 异步回调函数
            onFinish(result.toString())
            latch.countDown() // 在异步操作完成后减少计数器
        }
        // 等待异步操作完成
        latch.await()
    }

    /**
     * 查找包含 src 文件夹的一级模块文件夹
     */
    private fun findModuleDirsWithSrc(rootDir: Path): List<Path> {
        return Files.list(rootDir)
            .filter { Files.isDirectory(it) }
            .filter { dir ->
                val srcDir = dir.resolve("src")
                Files.exists(srcDir) && Files.isDirectory(srcDir)
            }
            .toList()
    }
}