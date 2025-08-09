package com.geminicli.exportandroidstrings;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

public class ExportStringsAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        // Create the dialog
        JDialog dialog = new JDialog();
        dialog.setTitle("Export Android Strings");
        dialog.setSize(400, 200);
        dialog.setModal(true); // Make it modal to block other interactions
        dialog.setLocationRelativeTo(null); // Center the dialog on screen

        // Create UI components
        JPanel panel = new JPanel(new BorderLayout());
        dialog.add(panel);

        // Export Directory selection
        JPanel exportDirPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel exportDirLabel = new JLabel("Export Directory:");
        JTextField exportDirField = new JTextField(20);
        JButton browseButton = new JButton("Browse...");

        exportDirPanel.add(exportDirLabel);
        exportDirPanel.add(exportDirField);
        exportDirPanel.add(browseButton);
        panel.add(exportDirPanel, BorderLayout.NORTH);

        // Export Button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton exportButton = new JButton("Export");
        buttonPanel.add(exportButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Add action listeners
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int result = fileChooser.showOpenDialog(dialog);
                if (result == JFileChooser.APPROVE_OPTION) {
                    exportDirField.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });

        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                String exportPath = exportDirField.getText();
                if (exportPath.isEmpty()) {
                    Messages.showErrorDialog(project, "Please select an export directory.", "Error");
                    return;
                }
                
                // Call the export logic
                exportStrings(project, exportPath);
                dialog.dispose(); // Close the dialog after export
            }
        });

        dialog.setVisible(true);
    }

    private void exportStrings(@NotNull Project project, @NotNull String exportPath) {
        Messages.showMessageDialog(project, "Starting string export to: " + exportPath, "Export Strings", Messages.getInformationIcon());

        Map<String, Map<String, String>> allStrings = new HashMap<>(); // key: string_name, value: Map<locale, string_value>
        Set<String> locales = new HashSet<>(); // To keep track of all found locales

        try {
            ModuleManager moduleManager = ModuleManager.getInstance(project);
            for (Module module : moduleManager.getModules()) {
                // Find Android 'res' directories
                ModuleRootManager.getInstance(module).getContentEntries().forEach(contentEntry -> {
                    for (SourceFolder sourceFolder : contentEntry.getSourceFolders()) {
                        VirtualFile sourceRoot = sourceFolder.getFile();
                        if (sourceRoot != null && sourceRoot.isDirectory()) {
                            VirtualFile resDir = sourceRoot.findChild("res");
                            if (resDir != null && resDir.isDirectory()) {
                                VfsUtilCore.visitChildrenRecursively(resDir, new VirtualFileVisitor<Void>() {
                                    @Override
                                    public boolean visitFile(@NotNull VirtualFile file) {
                                        if (file.isDirectory() && file.getName().startsWith("values")) {
                                            VirtualFile stringsXml = file.findChild("strings.xml");
                                            if (stringsXml != null && stringsXml.exists() && !stringsXml.isDirectory()) {
                                                parseStringsXml(project, stringsXml, allStrings, locales);
                                            }
                                        }
                                        return true;
                                    }
                                });
                            }
                        }
                    }
                });
            }

            // Now, write to CSV
            writeStringsToCsv(project, exportPath, allStrings, locales);

        } catch (Exception e) {
            Messages.showErrorDialog(project, "Error during string export: " + e.getMessage(), "Export Error");
        }
    }

    private void parseStringsXml(@NotNull Project project, @NotNull VirtualFile stringsXmlFile,
                                  @NotNull Map<String, Map<String, String>> allStrings,
                                  @NotNull Set<String> locales) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(stringsXmlFile);
        if (psiFile instanceof XmlFile) {
            XmlFile xmlFile = (XmlFile) psiFile;
            XmlTag rootTag = xmlFile.getRootTag();
            if (rootTag != null && "resources".equals(rootTag.getName())) {
                String locale = getLocaleFromValuesDir(stringsXmlFile.getParent().getName());
                locales.add(locale);

                for (XmlTag stringTag : rootTag.findSubTags("string")) {
                    String name = stringTag.getAttributeValue("name");
                    String value = stringTag.getValue().getText();
                    if (name != null && value != null) {
                        allStrings.computeIfAbsent(name, k -> new HashMap<>()).put(locale, value);
                    }
                }
            }
        }
    }

    private String getLocaleFromValuesDir(@NotNull String dirName) {
        if ("values".equals(dirName)) {
            return "default"; // Default locale
        } else if (dirName.startsWith("values-")) {
            return dirName.substring("values-".length());
        }
        return "unknown"; // Should not happen for valid values-XX directories
    }

    private void writeStringsToCsv(@NotNull Project project, @NotNull String exportPath,
                                   @NotNull Map<String, Map<String, String>> allStrings,
                                   @NotNull Set<String> locales) {
        File outputFile = new File(exportPath, "exported_strings.csv");
        try (FileWriter writer = new FileWriter(outputFile)) {
            // Prepare header
            List<String> sortedLocales = locales.stream()
                                                .sorted((l1, l2) -> {
                                                    if ("default".equals(l1)) return -1;
                                                    if ("default".equals(l2)) return 1;
                                                    return l1.compareTo(l2);
                                                })
                                                .collect(Collectors.toList());

            writer.append("key");
            for (String locale : sortedLocales) {
                writer.append(",").append(locale);
            }
            writer.append("\n");

            // Write data rows
            for (Map.Entry<String, Map<String, String>> entry : allStrings.entrySet()) {
                String key = entry.getKey();
                Map<String, String> localizedStrings = entry.getValue();
                writer.append(escapeCsv(key));
                for (String locale : sortedLocales) {
                    writer.append(",").append(escapeCsv(localizedStrings.getOrDefault(locale, "")));
                }
                writer.append("\n");
            }

            Messages.showMessageDialog(project, "Strings exported to: " + outputFile.getAbsolutePath(), "Export Strings", Messages.getInformationIcon());
        } catch (IOException e) {
            Messages.showErrorDialog(project, "Error writing CSV file: " + e.getMessage(), "Export Error");
        }
    }

    // Simple CSV escaping for now
    private String escapeCsv(@NotNull String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\\\"") + "\"";
        }
        return value;
    }
}