package com.geminicli.exportandroidstrings;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StringExporter {

    private final Project project;

    public StringExporter(@NotNull Project project) {
        this.project = project;
    }

    public void writeStringsToExcel(@NotNull String exportPath, @NotNull String moduleName,
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
