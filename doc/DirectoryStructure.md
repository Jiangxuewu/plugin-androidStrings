# 项目目录结构说明

## 概述

本文件旨在解释 `ExportAndroidStrings` 项目的目录结构及其每个部分的用途。

```
ExportAndroidStrings/
├── README.md
├── build.gradle
└── src/
    └── main/
        ├── java/
        └── resources/
            └── META-INF/
```

## 目录和文件说明

*   `ExportAndroidStrings/`
    *   这是我们 Android Studio 插件项目的根目录。

*   `README.md`
    *   这个文件提供了项目的总体概述、目的以及如何使用它。

*   `build.gradle`
    *   这是项目的 Gradle 构建脚本。它定义了项目依赖、任务以及插件的构建方式。对于 IntelliJ 平台插件，它配置了 `org.jetbrains.intellij` 插件。

*   `src/`
    *   这个目录包含了项目的源代码和资源文件。

*   `src/main/`
    *   这是 Maven/Gradle 项目中主源代码集的标准约定。

*   `src/main/java/`
    *   这个目录将包含插件的 Java（或 Kotlin）源代码。您将在这里编写导出字符串、创建 UI 元素等逻辑。

*   `src/main/resources/`
    *   这个目录包含插件的非代码资源，例如配置文件、图标以及 `plugin.xml` 文件。

*   `src/main/resources/META-INF/`
    *   这是 IntelliJ 平台要求的特殊目录。

*   `src/main/resources/META-INF/plugin.xml` (即将创建)
    *   这个 XML 文件是插件的描述符。它包含插件的基本元数据，如 ID、名称、版本、供应商、描述，以及最重要的，它提供的扩展点和组件。