package com.geminicli.exportandroidstrings;

/**
 * StringResourceParser 类负责解析 Android 项目中的 strings.xml 文件。
 * 它能够从文件中提取字符串资源，并根据目录名称识别对应的语言环境。
 *
 * 如何使用：
 * 1. 实例化 StringResourceParser 类，传入当前项目对象。
 * 2. 调用 parseStringsXml 方法来解析用于导出的所有字符串。
 *    例如：parser.parseStringsXml(stringsXmlFile, allStrings, locales);
 * 3. 调用 parseStringsXmlForTranslation 方法来解析用于翻译的单个语言环境的字符串。
 *    例如：parser.parseStringsXmlForTranslation(stringsXmlFile, localeStrings);
 * 4. 调用 getLocaleFromValuesDir 方法来从 values 目录名称中提取语言环境代码。
 *    例如：String locale = parser.getLocaleFromValuesDir(dirName);
 */

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StringResourceParser {

    private final Project project;

    public StringResourceParser(@NotNull Project project) {
        this.project = project;
    }

    public void parseStringsXml(@NotNull VirtualFile stringsXmlFile,
                                @NotNull Map<String, Map<String, String>> allStrings,
                                @NotNull Set<String> locales) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(stringsXmlFile);
        if (psiFile instanceof XmlFile) {
            XmlFile xmlFile = (XmlFile) psiFile;
            XmlTag rootTag = xmlFile.getRootTag();
            if (rootTag != null && "resources".equals(rootTag.getName())) {
                String locale = getLocaleFromValuesDir(stringsXmlFile.getParent().getName());
                if (locale != null) { // Only add valid locales
                    locales.add(locale);

                    for (XmlTag stringTag : rootTag.findSubTags("string")) {
                        String name = stringTag.getAttributeValue("name");
                        String value = StringUtil.unescapeXmlEntities(stringTag.getValue().getText());
                        if (name != null && value != null) {
                            allStrings.computeIfAbsent(name, k -> new HashMap<>()).put(locale, value);
                        }
                    }
                }
            }
        }
    }

    public void parseStringsXmlForTranslation(@NotNull VirtualFile stringsXmlFile,
                                              @NotNull Map<String, String> localeStrings) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(stringsXmlFile);
        if (psiFile instanceof XmlFile) {
            XmlFile xmlFile = (XmlFile) psiFile;
            XmlTag rootTag = xmlFile.getRootTag();
            if (rootTag != null && "resources".equals(rootTag.getName())) {
                for (XmlTag stringTag : rootTag.findSubTags("string")) {
                    String name = stringTag.getAttributeValue("name");
                    String value = StringUtil.unescapeXmlEntities(stringTag.getValue().getText());
                    if (name != null && value != null) {
                        localeStrings.put(name, value);
                    }
                }
            }
        }
    }

    public String getLocaleFromValuesDir(@NotNull String dirName) {
        String trimmedDirName = dirName.trim();
        if ("values".equals(trimmedDirName)) {
            return "default"; // Default locale
        } else if (trimmedDirName.startsWith("values-")) {
            String localePart = trimmedDirName.substring("values-".length());
            if (!localePart.isEmpty()) {
                return trimmedDirName; // Return the full directory name as requested
            }
        }
        return null; // Return null for invalid or empty locale parts
    }
}
