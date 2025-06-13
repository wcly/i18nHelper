# **i18nHelper**
**i18nHelper** 是一个 IntelliJ IDEA 插件，旨在帮助开发者自动处理国际化（i18n）相关任务。它可以扫描默认字符串资源文件，识别缺失的翻译，并支持在其他语言中添加翻译，从而简化多语言应用的开发流程。
## **功能特性**
- **自动扫描字符串资源**：扫描项目中的默认字符串资源文件（如 `strings.xml`），并提取所有未翻译的键值对。
- **支持多语言翻译**：自动检测缺失的翻译，并提供工具帮助开发者快速补充翻译。
- **集成到 IDE 工具菜单**：通过点击菜单项即可触发插件功能，操作简单直观。
- **高效开发体验**：减少手动查找和添加翻译的工作量，提升开发效率。

## **安装方法**
### **从 JetBrains 插件市场安装**
1. 打开 Android Studio 或 IntelliJ IDEA。
2. 进入 **File > Settings > Plugins**。
3. 搜索插件名称 **i18nHelper**。
4. 点击 **Install** 安装插件。
5. 安装完成后，重启 IDE。

### **手动安装**
1. 下载最新的插件 `.zip` 文件（从 [Releases](https://github.com/your-repo/i18nHelper/releases) 页面获取）。
2. 打开 Android Studio 或 IntelliJ IDEA。
3. 进入 **File > Settings > Plugins**。
4. 点击右上角的齿轮图标，选择 **Install Plugin from Disk**。
5. 选择下载的 `.zip` 文件并安装。
6. 安装完成后，重启 IDE。

## **使用说明**
1. 安装并启动插件后，在顶部菜单栏找到 **Tools > 自动翻译**。
2. 点击菜单项，插件会自动扫描项目中的默认字符串资源文件（如 `strings.xml`）。
3. 插件会列出所有未翻译的键值对，并提示用户进行补充翻译。
4. 用户可以将翻译结果直接保存到对应的资源文件中。

## **开发指南**
### **环境要求**
- **JDK**: Java 17 或更高版本。
- **构建工具**: Gradle 7.x 或更高版本。
- **IDE**: Android Studio 或 IntelliJ IDEA Ultimate Edition。

### **构建与运行**
1. 克隆项目代码：
``` bash
   git clone https://github.com/your-repo/i18nHelper.git
   cd i18nHelper
```
1. 使用 Gradle 构建项目：
``` bash
   ./gradlew build
```
1. 启动插件开发环境：
``` bash
   ./gradlew runIde
```
此命令会启动一个新的 Android Studio 或 IntelliJ IDEA 实例，用于测试插件功能。
### **项目结构**
- **`build.gradle.kts`**: 定义了项目的构建配置，包括依赖项和插件设置。
- **`plugin.xml`**: 插件的配置文件，定义了插件的基本信息、扩展点和菜单项。
- **`src/main/kotlin`**: 插件的核心逻辑代码。
- **`src/main/resources`**: 插件的资源文件，如图标和配置文件。

### **依赖项**
- **Kotlin**: 使用 Kotlin 编写核心逻辑。
- **OkHttp**: 用于网络请求（如调用翻译 API）。
- **org.json**: 用于解析和生成 JSON 数据。

## **贡献指南**
欢迎提交 Pull Request 或 Issue！如果你希望为项目做出贡献，请遵循以下步骤：
1. Fork 本仓库。
2. 创建一个新的分支 (`git checkout -b feature/your-feature`)。
3. 提交你的更改 (`git commit -m 'Add some feature'`)。
4. 推送到分支 (`git push origin feature/your-feature`)。
5. 提交 Pull Request。

## **许可证**
本项目采用 [MIT License](LICENSE)。详情请查看 `LICENSE` 文件。
## **联系我们**
如有任何问题或建议，请通过以下方式联系我们：
- **GitHub Issues**: [https://github.com/wcly/i18nHelper/issues](https://github.com/your-repo/i18nHelper/issues)

希望这份文档能帮助你更好地了解和使用 **i18nHelper** 插件！如果有任何问题，请随时联系我们的团队。
