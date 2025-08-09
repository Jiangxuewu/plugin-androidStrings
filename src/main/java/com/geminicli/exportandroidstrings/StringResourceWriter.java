package com.geminicli.exportandroidstrings;

/**
 * StringResourceWriter 类负责将字符串资源写入或更新到 Android 项目的 strings.xml 文件中。
 * 它能够处理现有字符串的更新和新字符串的添加。
 *
 * 如何使用：
 * 1. 实例化 StringResourceWriter 类，传入当前项目对象。
 * 2. 调用 updateStringsXml 方法，提供要更新的 strings.xml 文件的 VirtualFile 对象、字符串的键和值。
 *    例如：writer.updateStringsXml(stringsXmlFile, key, translatedText);
 */

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.ui.Messages; // NEW IMPORT
import com.intellij.psi.PsiFileFactory; // NEW IMPORT
import com.intellij.openapi.command.WriteCommandAction; // NEW IMPORT

public class StringResourceWriter {

    private final Project project;

    public StringResourceWriter(@NotNull Project project) {
        this.project = project;
    }

    public void updateStringsXml(@NotNull VirtualFile stringsXmlFile,
                                 @NotNull String key, @NotNull String value) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(stringsXmlFile);
        if (psiFile instanceof XmlFile) {
            XmlFile xmlFile = (XmlFile) psiFile;
            XmlTag rootTag = xmlFile.getRootTag();
            if (rootTag != null && "resources".equals(rootTag.getName())) {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    // Check if the string already exists
                    XmlTag existingTag = null;
                    for (XmlTag stringTag : rootTag.findSubTags("string")) {
                        if (key.equals(stringTag.getAttributeValue("name"))) {
                            existingTag = stringTag;
                            break;
                        }
                    }

                    if (existingTag != null) {
                        // Update existing string
                        existingTag.getValue().setText(value);
                    } else {
                        // Add new string
                        String tagText = "<string name=\"" + key + "\">" + value + "</string>";
                        PsiFile dummyFile = PsiFileFactory.getInstance(project).createFileFromText("dummy.xml", tagText);
                        if (dummyFile instanceof XmlFile) {
                            XmlFile dummyXmlFile = (XmlFile) dummyFile;
                            XmlTag newTag = dummyXmlFile.getRootTag();
                            if (newTag != null) {
                                rootTag.addSubTag(newTag, false);
                            }
                        }
                    }
                });
            }
        }
    }
}

