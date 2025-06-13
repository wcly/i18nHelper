package org.utcook.i18nHelper.utils

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.xml.XmlFile
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.streams.asSequence

object StringsXmlUtils {
    /**
     * 表示一个字符串资源文件的信息
     */
    data class StringsResource(
        val language: String, // 语言标识符（如 "en", "zh-rCN"）
        val filePath: Path,  // 文件路径
        val fileContent: String, // 文件内容
        val keyValueMap: Map<String, String> // 解析后的键值对
    )

    /**
     * 工具方法：解析 strings.xml 文件，提取所有 <string> 标签的内容
     *
     * @param project 当前项目实例
     * @param xmlContent strings.xml 文件内容（字符串形式）
     * @return 包含 <string> 标签的 Map，其中 key 是 name 属性，value 是标签内容
     */
    fun parseStringsXml(project: Project, xmlContent: String): Map<String, String> {
        // 使用 PsiFileFactory 创建 PsiFile 对象
        val psiFile = PsiFileFactory.getInstance(project).createFileFromText("temp.xml", xmlContent)
        if (psiFile !is XmlFile) {
            throw IllegalArgumentException("内容无法解析为有效的 XML 文件")
        }

        // 获取 XML 文件的根标签
        val rootTag = psiFile.rootTag ?: throw IllegalArgumentException("XML 内容没有根标签")

        // 遍历所有 <string> 标签
        val stringMap = mutableMapOf<String, String>()
        for (stringTag in rootTag.findSubTags("string")) {
            // 检查是否具有 translatable="false" 属性
            val translatableAttribute = stringTag.getAttribute("translatable")?.value
            if (translatableAttribute == "false") {
                continue // 跳过不可翻译的标签
            }

            // 提取 name 属性和内容
            val nameAttribute = stringTag.getAttribute("name")?.value
            val content = stringTag.value.text.trim()

            if (!nameAttribute.isNullOrEmpty() && content.isNotEmpty()) {
                stringMap[nameAttribute] = content
            }
        }

        return stringMap
    }

    /**
     * 工具方法：读取 strings.xml 文件并将其内容转换为字符串
     *
     * @param filePath 文件路径（Path 对象）
     * @return 文件内容的字符串表示
     */
    fun readFileToString(filePath: Path): String {
        return try {
            // 使用 Files.readAllLines 读取文件内容，并将所有行拼接成单个字符串
            Files.readAllLines(filePath, StandardCharsets.UTF_8).joinToString("\n")
        } catch (e: Exception) {
            throw RuntimeException("无法读取文件: ${filePath.fileName}", e)
        }
    }

    /**
     * 工具方法：读取模块中的所有 strings.xml 文件
     *
     * @param project 当前项目实例
     * @param moduleDir 模块目录的路径
     * @return 包含所有字符串资源文件信息的列表
     */
    fun readAllStringsResources(project: Project, moduleDir: Path): List<StringsResource> {
        // 查找模块中的所有 strings.xml 文件，并按语言分类
        val stringsXmlFiles = findStringsXmlFiles(moduleDir)

        // 遍历每个语言的文件，读取内容并解析
        return stringsXmlFiles.flatMap { (language, files) ->
            files.map { file ->
                // 读取文件内容
                val fileContent = readFileToString(file)

                // 解析文件内容为键值对
                val keyValueMap = parseStringsXml(project, fileContent)

                // 封装为 StringsResource 对象
                StringsResource(
                    language = language,
                    filePath = file,
                    fileContent = fileContent,
                    keyValueMap = keyValueMap
                )
            }
        }
    }

    /**
     * 查找模块中的所有 strings.xml 文件并标记语言
     *
     * @param moduleDir 模块目录的路径
     * @return 语言到文件路径列表的映射
     */
    private fun findStringsXmlFiles(moduleDir: Path): Map<String, List<Path>> {
        val resDir = moduleDir.resolve("src/main/res")
        if (!Files.exists(resDir)) throw IllegalArgumentException("未找到资源目录: $resDir")

        return Files.walk(resDir, 1) // 仅遍历直接子目录
            .use { stream ->
                stream
                    .asSequence()
                    .filter {
                        Files.isDirectory(it) &&
                                it.fileName.toString().startsWith("values")
                    }
                    .mapNotNull { dir ->
                        val language = dir.fileName.toString()
                            .removePrefix("values")
                            .takeIf { it.isNotEmpty() }?.trim('-') ?: "default"
                        val stringsXmlFile = dir.resolve("strings.xml")
                        if (Files.exists(stringsXmlFile)) language to stringsXmlFile else null
                    }
                    .toList()
            }
            .groupBy({ it.first }, { it.second }) // 隐式类型推断
            .mapKeys { it.key } // 无需显式泛型
    }

    /**
     * 将 Map 转换为字符串资源并插入到指定的 strings.xml 文件尾部。
     *
     * @param map 新增的键值对。
     * @param stringsResource 目标 strings.xml 文件。
     */
    fun insertMapToStringsXml(map: Map<String, String>, stringsResource: StringsResource) {
        // 读取现有文件内容
        val closingTag = "</resources>"

        // 检查文件是否包含 </resources> 结束标签
        if (!stringsResource.fileContent.contains(closingTag)) {
            throw IllegalStateException("目标文件不是一个有效的 strings.xml 文件，缺少 </resources> 标签")
        }

        // 将 Map 转换为 XML 格式的字符串资源
        val newResources = map.entries.joinToString("\n") { (key, value) ->
            """    <string name="$key">${value.replace("'", "\\'")}</string>""" // 法语翻译完可能会带单引号 '，需要转义，否则android项目编译会报错
        }

        // 构造新的文件内容
        val updatedContent = stringsResource.fileContent.replace(closingTag, "$newResources\n$closingTag")

        // 写回文件
        Files.write(
            stringsResource.filePath,
            updatedContent.toByteArray(),
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.CREATE
        )
    }
}