package com.geminicli.exportandroidstrings;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserFactory;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ExportStringsAction extends AnAction {

    private static final String LAST_EXPORT_PATH_KEY = "ExportAndroidStrings.lastExportPath";
    private static final String LAST_MODULE_PATH_KEY = "ExportAndroidStrings.lastModulePath";
    private static final String LAST_API_KEY = "ExportAndroidStrings.apiKey";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        // Create the dialog
        JDialog dialog = new JDialog();
        dialog.setTitle("Export and Translate Strings");
        dialog.setModal(true);
        dialog.setLocationRelativeTo(null);

        // Create UI components
        JPanel panel = new JPanel(new GridBagLayout());
        dialog.add(panel);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Function Selection ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        JPanel functionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JRadioButton exportRadio = new JRadioButton("Export All Strings", true);
        JRadioButton translateRadio = new JRadioButton("Translate Missing Strings");
        ButtonGroup functionGroup = new ButtonGroup();
        functionGroup.add(exportRadio);
        functionGroup.add(translateRadio);
        functionPanel.add(exportRadio);
        functionPanel.add(translateRadio);
        panel.add(functionPanel, gbc);

        // --- Module Directory Selection ---
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        panel.add(new JLabel("Module Directory:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField moduleDirField = new JTextField(PropertiesComponent.getInstance().getValue(LAST_MODULE_PATH_KEY, ""));
        panel.add(moduleDirField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton browseModuleButton = new JButton("Browse...");
        panel.add(browseModuleButton, gbc);

        // --- Export Directory Panel ---
        JPanel exportPanel = new JPanel(new GridBagLayout());
        GridBagConstraints exportGbc = new GridBagConstraints();
        exportGbc.fill = GridBagConstraints.HORIZONTAL;
        exportGbc.weightx = 1.0;

        exportPanel.add(new JLabel("Export Directory:"), exportGbc);

        JTextField exportDirField = new JTextField(PropertiesComponent.getInstance().getValue(LAST_EXPORT_PATH_KEY, ""));
        exportGbc.gridx = 1;
        exportPanel.add(exportDirField, exportGbc);

        JButton browseExportButton = new JButton("Browse...");
        exportGbc.gridx = 2;
        exportGbc.weightx = 0;
        exportPanel.add(browseExportButton, exportGbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        panel.add(exportPanel, gbc);

        // --- API Key Panel ---
        JPanel apiKeyPanel = new JPanel(new GridBagLayout());
        GridBagConstraints apiKeyGbc = new GridBagConstraints();
        apiKeyGbc.fill = GridBagConstraints.HORIZONTAL;
        apiKeyGbc.weightx = 1.0;

        apiKeyPanel.add(new JLabel("Google API Key:"), apiKeyGbc);

        JTextField apiKeyField = new JTextField(PropertiesComponent.getInstance().getValue(LAST_API_KEY, ""));
        apiKeyGbc.gridx = 1;
        apiKeyPanel.add(apiKeyField, apiKeyGbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        panel.add(apiKeyPanel, gbc);
        apiKeyPanel.setVisible(false); // Initially hidden

        // --- Action Listeners for Radio Buttons ---
        exportRadio.addActionListener(e1 -> {
            exportPanel.setVisible(true);
            apiKeyPanel.setVisible(false);
            dialog.pack();
        });

        translateRadio.addActionListener(e1 -> {
            exportPanel.setVisible(false);
            apiKeyPanel.setVisible(true);
            dialog.pack();
        });

        // --- Run Button ---
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.EAST;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton runButton = new JButton("Run");
        buttonPanel.add(runButton);
        panel.add(buttonPanel, gbc);

        // --- Action Listeners for Browse Buttons ---
        browseModuleButton.addActionListener(event -> {
            VirtualFile[] files = FileChooserFactory.getInstance().createFileChooser(
                    FileChooserDescriptorFactory.createSingleFolderDescriptor(), project, null)
                    .choose(project);
            if (files.length > 0) {
                moduleDirField.setText(files[0].getPath());
            }
        });

        browseExportButton.addActionListener(event -> {
            VirtualFile[] files = FileChooserFactory.getInstance().createFileChooser(
                    FileChooserDescriptorFactory.createSingleFolderDescriptor(), project, null)
                    .choose(project);
            if (files.length > 0) {
                exportDirField.setText(files[0].getPath());
            }
        });

        // --- Action Listener for Run Button ---
        runButton.addActionListener(event -> {
            String modulePath = moduleDirField.getText();
            if (modulePath.isEmpty()) {
                Messages.showErrorDialog(project, "Please select a module directory.", "Error");
                return;
            }
            PropertiesComponent.getInstance().setValue(LAST_MODULE_PATH_KEY, modulePath);

            // Instantiate service classes
            StringResourceParser parser = new StringResourceParser(project);
            StringExporter exporter = new StringExporter(project);
            StringResourceWriter writer = new StringResourceWriter(project);
            StringTranslator translator = new StringTranslator(project, parser, writer);

            if (exportRadio.isSelected()) {
                String exportPath = exportDirField.getText();
                if (exportPath.isEmpty()) {
                    Messages.showErrorDialog(project, "Please select an export directory.", "Error");
                    return;
                }
                PropertiesComponent.getInstance().setValue(LAST_EXPORT_PATH_KEY, exportPath);

                // Collect all strings for export
                Map<String, Map<String, String>> allStrings = new HashMap<>();
                Set<String> locales = new HashSet<>();
                String moduleName = new File(modulePath).getName();

                try {
                    VirtualFile moduleRoot = VfsUtil.findFileByIoFile(new File(modulePath), true);
                    if (moduleRoot == null || !moduleRoot.isDirectory()) {
                        Messages.showErrorDialog(project, "Invalid module directory: " + modulePath, "Export Error");
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
                        Messages.showErrorDialog(project, "Could not find any 'res' directory in module: " + modulePath + ". Tried: " + String.join(", ", java.util.Arrays.stream(potentialResDirs).filter(f -> f != null).map(VirtualFile::getPath).collect(Collectors.toList())), "Export Error");
                        return;
                    }

                    VfsUtilCore.visitChildrenRecursively(resDir, new VirtualFileVisitor<Void>() {
                        @Override
                        public boolean visitFile(@NotNull VirtualFile file) {
                            if (file.isDirectory() && file.getName().startsWith("values")) {
                                VirtualFile stringsXml = file.findChild("strings.xml");
                                if (stringsXml != null && stringsXml.exists() && !stringsXml.isDirectory()) {
                                    parser.parseStringsXml(stringsXml, allStrings, locales);
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

                    exporter.writeStringsToExcel(exportPath, moduleName, allStrings, locales);

                } catch (Exception ex) {
                    Messages.showErrorDialog(project, "Error during string export: " + ex.getMessage(), "Export Error");
                }

            } else { // Translate is selected
                String apiKey = apiKeyField.getText();
                if (apiKey.isEmpty()) {
                    Messages.showErrorDialog(project, "Please enter your Google API Key.", "Error");
                    return;
                }
                PropertiesComponent.getInstance().setValue(LAST_API_KEY, apiKey);
                translator.translateMissingStrings(modulePath, apiKey);
            }
            dialog.dispose();
        });

        dialog.pack();
        dialog.setVisible(true);
    }
}
