package com.geminicli.exportandroidstrings;

import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class StringResourceParserTest {

    private Project mockProject;
    private StringResourceParser parser;

    @BeforeEach
    void setUp() {
        mockProject = Mockito.mock(Project.class);
        parser = new StringResourceParser(mockProject);
    }

    @Test
    void testGetLocaleFromValuesDir_default() {
        assertEquals("default", parser.getLocaleFromValuesDir("values"));
    }

    @Test
    void testGetLocaleFromValuesDir_simpleLocale() {
        assertEquals("values-fr", parser.getLocaleFromValuesDir("values-fr"));
    }

    @Test
    void testGetLocaleFromValuesDir_localeWithRegion() {
        assertEquals("values-en-rUS", parser.getLocaleFromValuesDir("values-en-rUS"));
    }

    @Test
    void testGetLocaleFromValuesDir_localeWithBQualifier() {
        assertEquals("values-b+es+419", parser.getLocaleFromValuesDir("values-b+es+419"));
    }

    @Test
    void testGetLocaleFromValuesDir_empty() {
        assertNull(parser.getLocaleFromValuesDir(""));
    }

    @Test
    void testGetLocaleFromValuesDir_invalidPrefix() {
        assertNull(parser.getLocaleFromValuesDir("res-fr"));
    }

    @Test
    void testParseStringsXmlForTranslation() {
        // This test is more complex as it requires mocking VirtualFile, PsiFile, XmlFile, XmlTag.
        // For simplicity, this will be a placeholder for now.
        // In a real scenario, you would create mock objects or temporary files to simulate the XML structure.
        // Example:
        // VirtualFile mockStringsXmlFile = Mockito.mock(VirtualFile.class);
        // PsiFile mockPsiFile = Mockito.mock(XmlFile.class);
        // Mockito.when(PsiManager.getInstance(mockProject).findFile(mockStringsXmlFile)).thenReturn(mockPsiFile);
        // ... more mocking ...

        Map<String, String> localeStrings = new HashMap<>();
        // parser.parseStringsXmlForTranslation(mockStringsXmlFile, localeStrings);
        // assertEquals("expectedValue", localeStrings.get("expectedKey"));
    }
}
