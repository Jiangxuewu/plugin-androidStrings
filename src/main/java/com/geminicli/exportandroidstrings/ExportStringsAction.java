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
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.ide.util.PropertiesComponent; // Import for persistence
import com.intellij.openapi.util.text.StringUtil; // NEW IMPORT
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime; // Import for timestamp
import java.time.format.DateTimeFormatter; // Import for timestamp formatting
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

public class ExportStringsAction extends AnAction {

    private static final String LAST_EXPORT_PATH_KEY = "ExportAndroidStrings.lastExportPath";
    private static final String LAST_MODULE_PATH_KEY = "ExportAndroidStrings.lastModulePath";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        // Create the dialog
        JDialog dialog = new JDialog();
        dialog.setTitle("Export Android Strings");
        dialog.setSize(500, 250); // Increased size for new fields
        dialog.setModal(true); // Make it modal to block other interactions
        dialog.setLocationRelativeTo(null); // Center the dialog on screen

        // Create UI components
        JPanel panel = new JPanel(new GridBagLayout());
        dialog.add(panel);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Module Directory selection
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Module Directory:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField moduleDirField = new JTextField(PropertiesComponent.getInstance().getValue(LAST_MODULE_PATH_KEY, ""));
        panel.add(moduleDirField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton browseModuleButton = new JButton("Browse...");
        panel.add(browseModuleButton, gbc);

        // Export Directory selection
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Export Directory:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField exportDirField = new JTextField(PropertiesComponent.getInstance().getValue(LAST_EXPORT_PATH_KEY, ""));
        panel.add(exportDirField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton browseExportButton = new JButton("Browse...");
        panel.add(browseExportButton, gbc);

        // Export Button
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.EAST;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton exportButton = new JButton("Export");
        buttonPanel.add(exportButton);
        panel.add(buttonPanel, gbc);

        // Add action listeners
        browseModuleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                VirtualFile[] files = FileChooserFactory.getInstance().createFileChooser(
                        FileChooserDescriptorFactory.createSingleFolderDescriptor(), project, null)
                        .choose(project);
                if (files.length > 0) {
                    moduleDirField.setText(files[0].getPath());
                }
            }
        });

        browseExportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                VirtualFile[] files = FileChooserFactory.getInstance().createFileChooser(
                        FileChooserDescriptorFactory.createSingleFolderDescriptor(), project, null)
                        .choose(project);
                if (files.length > 0) {
                    exportDirField.setText(files[0].getPath());
                }
            }
        });

        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                String modulePath = moduleDirField.getText();
                String exportPath = exportDirField.getText();

                if (modulePath.isEmpty()) {
                    Messages.showErrorDialog(project, "Please select a module directory.", "Error");
                    return;
                }
                if (exportPath.isEmpty()) {
                    Messages.showErrorDialog(project, "Please select an export directory.", "Error");
                    return;
                }
                
                // Save paths for next time
                PropertiesComponent.getInstance().setValue(LAST_MODULE_PATH_KEY, modulePath);
                PropertiesComponent.getInstance().setValue(LAST_EXPORT_PATH_KEY, exportPath);

                // Call the export logic
                exportStrings(project, modulePath, exportPath);
                dialog.dispose(); // Close the dialog after export
            }
        });

        dialog.setVisible(true);
    }

    private void exportStrings(@NotNull Project project, @NotNull String modulePath, @NotNull String exportPath) {
        Messages.showMessageDialog(project, "Starting string export from module: " + modulePath + " to: " + exportPath, "Export Strings", Messages.getInformationIcon());

        Map<String, Map<String, String>> allStrings = new HashMap<>(); // key: string_name, value: Map<locale, string_value>
        Set<String> locales = new HashSet<>(); // To keep track of all found locales
        String moduleName = new File(modulePath).getName(); // Get module name from path

        try {
            VirtualFile moduleRoot = VfsUtil.findFileByIoFile(new File(modulePath), true);
            if (moduleRoot == null || !moduleRoot.isDirectory()) {
                Messages.showErrorDialog(project, "Invalid module directory: " + modulePath, "Export Error");
                return;
            }

            VirtualFile resDir = null;
            // Try common Android res directory paths
            VirtualFile[] potentialResDirs = {
                moduleRoot.findChild("res"), // module/res
                moduleRoot.findFileByRelativePath("src/main/res"), // module/src/main/res
                moduleRoot.findFileByRelativePath("src/debug/res"), // module/src/debug/res
                moduleRoot.findFileByRelativePath("src/release/res") // module/src/release/res
            };

            for (VirtualFile potentialResDir : potentialResDirs) {
                if (potentialResDir != null && potentialResDir.isDirectory()) {
                    resDir = potentialResDir;
                    break;
                }
            }

            if (resDir == null || !resDir.isDirectory()) {
                Messages.showErrorDialog(project, "Could not find any 'res' directory in module: " + modulePath + ". Tried: " + String.join(", ", java.util.Arrays.stream(potentialResDirs).filter(f -> f != null).map(VirtualFile::getPath).collect(Collectors.toList())), "Export Error");
                return;
            }

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

            // Debugging: Print allStrings and locales
            System.out.println("---" + " Debugging allStrings ---");
            for (Map.Entry<String, Map<String, String>> entry : allStrings.entrySet()) {
                System.out.println("Key: " + entry.getKey());
                Map<String, String> subMap = entry.getValue();
                for (Map.Entry<String, String> subEntry : subMap.entrySet()) {
                    System.out.println("   " + subEntry.getKey() + "=" + subEntry.getValue());
                }
            }
            System.out.println("---" + " Debugging locales ---");
            System.out.println("Locales: " + locales);
            System.out.println("--------------------------");

            // Now, write to Excel
            writeStringsToExcel(project, exportPath, moduleName, allStrings, locales);

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

    private String getLocaleFromValuesDir(@NotNull String dirName) {
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

    private void writeStringsToExcel(@NotNull Project project, @NotNull String exportPath, @NotNull String moduleName,
                                     @NotNull Map<String, Map<String, String>> allStrings,
                                     @NotNull Set<String> locales) {
        // Generate timestamp for filename
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("_yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);

        File outputFile = new File(exportPath, moduleName + "_exported_strings" + timestamp + ".xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Strings");

            // Prepare header
            List<String> sortedLocales = locales.stream()
                    .sorted((l1, l2) -> {
                        if ("default".equals(l1)) return -1;
                        if ("default".equals(l2)) return 1;
                        return l1.compareTo(l2);
                    })
                    .collect(Collectors.toList());

            // Create header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Module Name");
            headerRow.createCell(1).setCellValue("Key");
            for (int i = 0; i < sortedLocales.size(); i++) {
                headerRow.createCell(i + 2).setCellValue(sortedLocales.get(i));
            }

            // Write data rows
            int rowNum = 1;
            for (Map.Entry<String, Map<String, String>> entry : allStrings.entrySet()) {
                String key = entry.getKey();
                Map<String, String> localizedStrings = entry.getValue();

                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(moduleName);
                row.createCell(1).setCellValue(key);

                for (int i = 0; i < sortedLocales.size(); i++) {
                    String locale = sortedLocales.get(i);
                    row.createCell(i + 2).setCellValue(localizedStrings.getOrDefault(locale, ""));
                }
            }

            // Write the output to a file
            try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                workbook.write(fileOut);
            }

            Messages.showMessageDialog(project, "Strings exported to: " + outputFile.getAbsolutePath(), "Export Strings", Messages.getInformationIcon());
        } catch (IOException e) {
            Messages.showErrorDialog(project, "Error writing Excel file: " + e.getMessage(), "Export Error");
        }
    }
}
