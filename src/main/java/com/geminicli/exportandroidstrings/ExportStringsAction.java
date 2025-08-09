package com.geminicli.exportandroidstrings;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

public class ExportStringsAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // This is where we will show our custom dialog
        Messages.showMessageDialog(e.getProject(), "Hello from Export Android Strings Plugin!", "Export Strings", Messages.getInformationIcon());
    }
}