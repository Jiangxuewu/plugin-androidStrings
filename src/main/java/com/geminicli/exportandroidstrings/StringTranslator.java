package com.geminicli.exportandroidstrings;

/**
 * StringTranslator 类负责处理 Android 字符串资源的机器翻译。
 * 它使用 Google Cloud Translation API 来翻译缺失的字符串，
 * 并将翻译结果写回相应的 strings.xml 文件。
 *
 * 如何使用：
 * 1. 实例化 StringTranslator 类，传入当前项目对象、StringResourceParser 实例和 StringResourceWriter 实例。
 * 2. 调用 translateMissingStrings 方法，提供模块路径和 Google Cloud Project ID。
 *    例如：translator.translateMissingStrings(modulePath, projectId);
 * 注意：Google Cloud Translation API 认证通过 Application Default Credentials (ADC) 处理。
 */

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class StringTranslator {

    private final Project project;
    private final StringResourceParser parser;
    private final StringResourceWriter writer;
    private final OkHttpClient httpClient;
    private final Gson gson;

    public StringTranslator(@NotNull Project project,
                            @NotNull StringResourceParser parser,
                            @NotNull StringResourceWriter writer) {
        this.project = project;
        this.parser = parser;
        this.writer = writer;
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
    }

    public void translateMissingStrings(String modulePath, String projectId, String apiKey) {
        Messages.showInfoMessage("Starting machine translation...", "Translate Strings");

        // Note: Authentication for Google Cloud Translation API is handled via Application Default Credentials (ADC).
        // Ensure your Google Cloud environment is configured correctly (e.g., by running 'gcloud auth application-default login').

        try {
            // Base URL for Google Cloud Translation API (v2 for simpler API key usage)
            // For v3, the endpoint is different and requires more complex request body.
            // We will use v2 for direct API key usage as it's simpler.
            String baseUrl = "https://translation.googleapis.com/language/translate/v2";

            VirtualFile moduleRoot = VfsUtil.findFileByIoFile(new File(modulePath), true);
            if (moduleRoot == null || !moduleRoot.isDirectory()) {
                Messages.showErrorDialog(project, "Invalid module directory: " + modulePath, "Translation Error");
                return;
            }

            VirtualFile resDir = null;
            VirtualFile[] potentialResDirs = {
                    moduleRoot.findChild("res"),
                    moduleRoot.findFileByRelativePath("src/main/res"),
                    moduleRoot.findFileByRelativePath("src/debug/res"),
                    moduleRoot.findFileByRelativePath("src/release/res")
            };

            for (VirtualFile potentialResDir : potentialResDirs) {
                if (potentialResDir != null && potentialResDir.isDirectory()) {
                    resDir = potentialResDir;
                    break;
                }
            }

            if (resDir == null || !resDir.isDirectory()) {
                Messages.showErrorDialog(project, "Could not find any 'res' directory in module: " + modulePath, "Translation Error");
                return;
            }

            // Map to store all strings by locale and key
            Map<String, Map<String, String>> allStringsByLocale = new HashMap<>(); // locale -> (key -> value)
            Map<String, VirtualFile> stringsXmlFiles = new HashMap<>(); // locale -> strings.xml VirtualFile

            // Find all strings.xml files and parse them
            VfsUtilCore.visitChildrenRecursively(resDir, new VirtualFileVisitor<Void>() {
                @Override
                public boolean visitFile(@NotNull VirtualFile file) {
                    if (file.isDirectory() && file.getName().startsWith("values")) {
                        VirtualFile stringsXml = file.findChild("strings.xml");
                        if (stringsXml != null && stringsXml.exists() && !stringsXml.isDirectory()) {
                            String locale = parser.getLocaleFromValuesDir(file.getName());
                            if (locale != null) {
                                stringsXmlFiles.put(locale, stringsXml);
                                Map<String, String> localeStrings = new HashMap<>();
                                parser.parseStringsXmlForTranslation(stringsXml, localeStrings);
                                allStringsByLocale.put(locale, localeStrings);
                            }
                        }
                    }
                    return true;
                }
            });

            Map<String, String> defaultStrings = allStringsByLocale.get("default");
            if (defaultStrings == null || defaultStrings.isEmpty()) {
                Messages.showErrorDialog(project, "No default strings.xml found or it's empty.", "Translation Error");
                return;
            }

            List<TranslationTask> translationTasks = new ArrayList<>();

            // Iterate through each default string and find missing translations
            for (Map.Entry<String, String> defaultEntry : defaultStrings.entrySet()) {
                String key = defaultEntry.getKey();
                String defaultValue = defaultEntry.getValue();

                for (Map.Entry<String, Map<String, String>> localeEntry : allStringsByLocale.entrySet()) {
                    String locale = localeEntry.getKey();
                    if ("default".equals(locale)) {
                        continue; // Skip default locale
                    }

                    Map<String, String> targetLocaleStrings = localeEntry.getValue();
                    if (!targetLocaleStrings.containsKey(key)) {
                        // String is missing in this locale, add to translation tasks
                        String targetLanguageCode = getLanguageCodeFromLocale(locale); // e.g., "values-fr" -> "fr"
                        if (targetLanguageCode == null) {
                            continue; // Skip invalid locales
                        }
                        translationTasks.add(new TranslationTask(key, defaultValue, locale, targetLanguageCode, stringsXmlFiles.get(locale)));
                    }
                }
            }

            if (translationTasks.isEmpty()) {
                Messages.showInfoMessage("No missing strings found for translation.", "Translate Strings");
                return;
            }

            // Show confirmation dialog
            boolean confirmed = showTranslationConfirmationDialog(translationTasks);

            if (confirmed) {
                // Perform translation for each task
                for (TranslationTask task : translationTasks) {
                    // Build the request body for translation
                    JsonObject requestBody = new JsonObject();
                    requestBody.addProperty("q", task.defaultValue);
                    requestBody.addProperty("target", task.targetLanguageCode);
                    requestBody.addProperty("format", "text"); // or html

                    RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestBody.toString());

                    Request request = new Request.Builder()
                            .url(baseUrl + "?key=" + apiKey) // Add API key as query parameter
                            .post(body)
                            .build();

                    try (Response response = httpClient.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            throw new IOException("Unexpected code " + response + " - " + response.body().string());
                        }

                        String responseBody = response.body().string();
                        JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                        JsonArray translations = jsonResponse.getAsJsonObject("data").getAsJsonArray("translations");

                        if (translations.size() > 0) {
                            String translatedText = translations.get(0).getAsJsonObject().get("translatedText").getAsString();
                            // Update the XML file
                            writer.updateStringsXml(task.targetStringsXmlFile, task.key, translatedText);
                        }
                    }
                }
                Messages.showInfoMessage("Translation process completed.", "Translate Strings");
            } else {
                Messages.showInfoMessage("Translation cancelled by user.", "Translate Strings");
            }

        } catch (Exception e) {
            Messages.showErrorDialog(project, "Error during translation: " + e.getMessage(), "Translation Error");
        }
    }

    private boolean showTranslationConfirmationDialog(List<TranslationTask> tasks) {
        StringBuilder message = new StringBuilder();
        message.append("The following strings will be translated:\n\n");

        for (TranslationTask task : tasks) {
            message.append("Key: ").append(task.key)
                    .append("\n  Default Value: ").append(task.defaultValue)
                    .append("\n  Target Language: ").append(task.targetLocale)
                    .append(" (").append(task.targetLanguageCode).append(")\n\n");
        }

        message.append("Do you want to proceed with the translation? (Translation may incur costs)");

        // Print the full message to the console for debugging
        System.out.println("Translation Confirmation Message:\n" + message.toString());

        // Pass a short message to the dialog to avoid truncation issues
        int result = Messages.showYesNoDialog(project, "Confirm translation for " + tasks.size() + " strings?", "Confirm Translation", "Proceed", "Cancel", Messages.getQuestionIcon());
        return result == Messages.YES;
    }

    // Helper method to get language code from locale directory name (e.g., "values-fr" -> "fr")
    private String getLanguageCodeFromLocale(@NotNull String dirName) {
        if (dirName.startsWith("values-")) {
            String localePart = dirName.substring("values-".length());
            if (!localePart.isEmpty()) {
                // Handle cases like values-b+es+419 (region code) or values-fr-rCA (country code)
                // For simplicity, we'll just take the first part before '-' or '+'
                int dashIndex = localePart.indexOf('-');
                int plusIndex = localePart.indexOf('+');
                if (dashIndex != -1) {
                    return localePart.substring(0, dashIndex);
                } else if (plusIndex != -1) {
                    return localePart.substring(0, plusIndex);
                }
                return localePart;
            }
        }
        return null; // Return null for invalid or default locale
    }

    // Data class to hold translation details for confirmation
    private static class TranslationTask {
        String key;
        String defaultValue;
        String targetLocale;
        String targetLanguageCode;
        VirtualFile targetStringsXmlFile;

        TranslationTask(String key, String defaultValue, String targetLocale, String targetLanguageCode, VirtualFile targetStringsXmlFile) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.targetLocale = targetLocale;
            this.targetLanguageCode = targetLanguageCode;
            this.targetStringsXmlFile = targetStringsXmlFile;
        }
    }
}
