package com.geminicli.exportandroidstrings;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
                Messages.showMessageDialog(project, "Exporting strings to: " + exportPath, "Export Strings", Messages.getInformationIcon());
                dialog.dispose(); // Close the dialog after export (for now)
            }
        });

        dialog.setVisible(true);
    }
}