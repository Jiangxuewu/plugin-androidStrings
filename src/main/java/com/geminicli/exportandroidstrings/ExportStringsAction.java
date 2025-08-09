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
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

        // Placeholder for actual string extraction and export logic
        // In a real scenario, you would:
        // 1. Find all res/values-XX/strings.xml files in the project.
        // 2. Parse each strings.xml file to extract string key-value pairs.
        // 3. Consolidate the strings, handling different languages.
        // 4. Write the consolidated data to a file (e.g., CSV, Excel) at exportPath.

        // For now, let's just create a dummy file to show it's working
        File outputFile = new File(exportPath, "exported_strings.csv");
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.append("key,default_en,es,fr\n");
            writer.append("app_name,My App,Mi Aplicación,Mon Application\n");
            writer.append("hello_world,Hello World,Hola Mundo,Bonjour le monde\n");
            Messages.showMessageDialog(project, "Dummy strings exported to: " + outputFile.getAbsolutePath(), "Export Strings", Messages.getInformationIcon());
        } catch (IOException e) {
            Messages.showErrorDialog(project, "Error writing dummy file: " + e.getMessage(), "Export Error", Messages.getErrorIcon());
        }
    }
}
