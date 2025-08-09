package com.geminicli.exportandroidstrings;

import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StringTranslator {

    private final Project project;
    private final StringResourceParser parser;
    private final StringResourceWriter writer;

    public StringTranslator(@NotNull Project project,
                            @NotNull StringResourceParser parser,
                            @NotNull StringResourceWriter writer) {
        this.project = project;
        this.parser = parser;
        this.writer = writer;
    }

    public void translateMissingStrings(String modulePath, String apiKey) {
        Messages.showInfoMessage("Starting machine translation...", "Translate Strings");

        // Set Google Cloud credentials
        // This is a simplified approach. In a real plugin, you might want to use a more secure way to handle API keys.
        // For now, we'll assume the API key is handled by the environment or a more robust mechanism.
        // System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", "path/to/your/credentials.json"); // Not directly using API key here, but a placeholder for future.

        try (TranslationServiceClient client = TranslationServiceClient.create()) {
            // Get the project ID from the API key (this is a simplification, usually project ID is separate)
            // For now, we'll use a placeholder project ID.
            String projectId = "your-gcp-project-id"; // TODO: Get actual project ID or make it configurable

            LocationName parent = LocationName.of(projectId, "global"); // Or specific region like "us-central1"

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
                        // String is missing in this locale, translate it
                        String targetLanguageCode = getLanguageCodeFromLocale(locale); // e.g., "values-fr" -> "fr"
                        if (targetLanguageCode == null) {
                            continue; // Skip invalid locales
                        }

                        // Perform translation
                        TranslateTextRequest request =
                                TranslateTextRequest.newBuilder()
                                        .setParent(parent.toString())
                                        .setMimeType("text/plain")
                                        .setTargetLanguageCode(targetLanguageCode)
                                        .addContents(defaultValue)
                                        .build();

                        TranslateTextResponse response = client.translateText(request);
                        if (response.getTranslationsCount() > 0) {
                            String translatedText = response.getTranslations(0).getTranslatedText();
                            // Add translated string to the map
                            targetLocaleStrings.put(key, translatedText);
                            // Update the XML file
                            writer.updateStringsXml(stringsXmlFiles.get(locale), key, translatedText);
                        }
                    }
                }
            }

            Messages.showInfoMessage("Translation process completed.", "Translate Strings");

        } catch (Exception e) {
            Messages.showErrorDialog(project, "Error during translation: " + e.getMessage(), "Translation Error");
        }
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
}
