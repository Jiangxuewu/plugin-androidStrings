# ExportAndroidStrings

这是一个由 Gemini-cli 创建的 Android Studio 插件，用于将 Android 项目中的字符串资源导出为 Excel 文件。

## 简介

该插件可以方便地将您 Android 项目中定义的所有字符串资源（包括所有语言环境）导出到一个单一的 `.xlsx` Excel 文件中。这对于翻译、审计或与非开发人员共享字符串资源非常有用。

## 功能

*   **简单易用的界面:** 通过一个简单的对话框选择模块和导出目录。
*   **支持多语言:** 自动查找并导出项目中 `values*` 目录下的所有 `strings.xml` 文件。
*   **导出为 Excel:** 将字符串资源以结构化的格式导出为 `.xlsx` 文件，包含模块名、字符串键（Key）以及每种语言的翻译。
*   **自动记忆路径:** 插件会记住上次使用的模块和导出目录，方便下次使用。

## 安装方法

1.  前往本项目的 [GitHub Releases](https://github.com/Jiangxuewu/plugin-androidStrings/releases) 页面。
2.  下载最新的 `.zip` 插件文件。
3.  打开 Android Studio，进入 `Settings` (或 `Preferences`) > `Plugins`。
4.  点击齿轮图标，选择 `Install Plugin from Disk...`。
5.  选择您刚刚下载的 `.zip` 文件并安装。
6.  重启 Android Studio。

## 使用方法

1.  在 Android Studio 的顶部菜单栏中，点击 `Tools` > `Export Android Strings...`。
2.  在弹出的对话框中，选择您要导出字符串的 Android 模块的根目录。
3.  选择您希望保存导出文件的目录。
4.  点击 `Export All Strings` 按钮。
5.  插件将会在您选择的导出目录中生成一个名为 `[模块名]_exported_strings_[时间戳].xlsx` 的 Excel 文件。

## 开发

您可以克隆本仓库并在本地进行开发和构建。

**构建要求:**
*   Java 11
*   Gradle

**构建步骤:**
1.  克隆仓库: `git clone https://github.com/Jiangxuewu/plugin-androidStrings.git`
2.  进入项目目录: `cd plugin-androidStrings`
3.  使用 Gradle 构建项目: `./gradlew build`
4.  构建完成后，插件的 `zip` 文件会生成在 `build/distributions/` 目录下。

## 贡献

欢迎对本项目做出贡献！如果您有任何问题或建议，请随时提交 [Issue](https://github.com/Jiangxuewu/plugin-androidStrings/issues) 或 [Pull Request](https://github.com/Jiangxuewu/plugin-androidStrings/pulls)。

## 许可证

本项目采用 [MIT 许可证](LICENSE)。
