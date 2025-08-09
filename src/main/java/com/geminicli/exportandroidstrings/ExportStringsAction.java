package com.geminicli.exportandroidstrings;

/**
 * ExportStringsAction 类是 Android Studio 插件的入口点。
 * 它负责创建和管理插件的用户界面 (GUI) 对话框，
 * 并根据用户的选择（导出字符串或翻译缺失字符串）将任务委托给不同的服务类。
 *
 * 如何使用：
 * 1. 在 Android Studio 中安装此插件。
 * 2. 在顶部菜单栏中，点击 'Tools' -> 'Export and Translate Strings...'。
 * 3. 在弹出的对话框中，选择所需的功能（导出或翻译）。
 * 4. 根据所选功能提供必要的输入（模块目录、导出目录或 Google API Key）。
 * 5. 点击 'Run' 按钮执行操作。
 */

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
    private static final String LAST_PROJECT_ID_KEY = "ExportAndroidStrings.projectId";

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

        // --- Translation Options Panel (Project ID and API Key) ---
        JPanel translationOptionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints transGbc = new GridBagConstraints();
        transGbc.insets = new Insets(5, 5, 5, 5);
        transGbc.fill = GridBagConstraints.HORIZONTAL;

        // Project ID field
        transGbc.gridx = 0;
        transGbc.gridy = 0;
        translationOptionsPanel.add(new JLabel("Project ID:"), transGbc);

        transGbc.gridx = 1;
        transGbc.weightx = 1.0;
        JTextField projectIdField = new JTextField(PropertiesComponent.getInstance().getValue(LAST_PROJECT_ID_KEY, ""));
        translationOptionsPanel.add(projectIdField, transGbc);

        // gcloud auth instructions
        transGbc.gridx = 0;
        transGbc.gridy = 2; // Place it below Project ID and API Key (if it were there)
        transGbc.gridwidth = 2; // Span across two columns
        JLabel authHintLabel = new JLabel("<html><p>Authentication is via Application Default Credentials (ADC).<br>" +
                "Run <b><code>gcloud auth application-default login</code></b> in your terminal.</p>" +
                "<p>More info: <a href=\"https://cloud.google.com/docs/authentication/getting-started\">Google Cloud Auth Docs</a></p></html>");
        translationOptionsPanel.add(authHintLabel, transGbc);

        

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        panel.add(translationOptionsPanel, gbc);
        translationOptionsPanel.setVisible(false); // Initially hidden

        // --- Action Listeners for Radio Buttons ---
        exportRadio.addActionListener(e1 -> {
            exportPanel.setVisible(true);
            translationOptionsPanel.setVisible(false);
            dialog.pack();
        });

        translateRadio.addActionListener(e1 -> {
            exportPanel.setVisible(false);
            translationOptionsPanel.setVisible(true);
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
                String projectId = projectIdField.getText();

                if (projectId.isEmpty()) {
                    Messages.showErrorDialog(project, "Please enter your Google Cloud Project ID.", "Error");
                    return;
                }
                PropertiesComponent.getInstance().setValue(LAST_PROJECT_ID_KEY, projectId);
                translator.translateMissingStrings(modulePath, projectId);
            }
            dialog.dispose();
        });

        dialog.pack();
        dialog.setVisible(true);
    }
}
