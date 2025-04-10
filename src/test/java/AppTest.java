import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {

    @TempDir
    Path tempDir;

    Path sourceDir;
    Path targetDir;

    //Path testLogFile;
    private ListAppender listAppender;
    private LoggerConfig loggerConfig;
    private Level originalLevel;

    @BeforeEach
    void setUp() throws IOException {
        sourceDir = tempDir.resolve("input");
        targetDir = tempDir.resolve("import");
        Files.createDirectory(sourceDir);

        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();

        listAppender = new ListAppender("List");
        listAppender.start();

        loggerConfig = config.getLoggerConfig(App.class.getName()); // Get config for "App" logger

        // Add the appender to the logger configuration
        loggerConfig.addAppender(listAppender, null, null); // Add appender with default level/filter

        // Update the context to apply changes
        ctx.updateLoggers();
    }

    @AfterEach
    public void after(){
        if (loggerConfig != null && listAppender != null) {
            // Remove the appender from the logger configuration
            loggerConfig.removeAppender("List");

            // Stop the appender
            listAppender.stop();

            // Update the context
            final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            ctx.updateLoggers();
        }
    }

    // --- Helper to get log messages ---
    private List<String> getLogMessages() {
        if (listAppender == null) {
            return Collections.emptyList();
        }
        // Get logged events and format them as strings
        return listAppender.getEvents().stream()
                .map(LogEvent::getMessage)
                .map(org.apache.logging.log4j.message.Message::getFormattedMessage)
                .collect(Collectors.toList());
    }

    // Helper to check if a specific message exists
    private boolean logContains(String expectedMessage) {
        return getLogMessages().stream().anyMatch(log -> log.contains(expectedMessage));
    }

    private void createTestFile(Path directory, String filename, String content) throws IOException {
        Files.write(directory.resolve(filename), content.getBytes(StandardCharsets.UTF_8));
    }


    @Test
    @DisplayName("moveFile should move and rename a file")
    void testMoveFile_Success() throws IOException {
        // --- Arrange ---
        String sourceFileName = "original_file.txt";
        String targetFileName = "moved_and_renamed_file.log";
        String fileContent = "This is the content of the file to be moved.";
        Path sourceFilePath = sourceDir.resolve(sourceFileName);
        createTestFile(sourceDir, sourceFileName, fileContent);
        assertTrue(Files.exists(sourceFilePath));
        assertFalse(Files.exists(targetDir));

        // --- Act ---
        boolean result = App.moveFile(sourceFilePath.toString(), targetDir.toString(), targetFileName);

        // --- Assert ---
        assertTrue(result);
        Path targetFilePath = targetDir.resolve(targetFileName);
        assertFalse(Files.exists(sourceFilePath));
        assertTrue(Files.exists(targetFilePath));
        assertTrue(Files.isDirectory(targetDir));
        String movedContent = new String(Files.readAllBytes(targetFilePath), StandardCharsets.UTF_8);
        assertEquals(fileContent, movedContent);

        // Verify Log Output using the ListAppender
        assertTrue(logContains("File moved successfully."), "Log should contain success message.");
        assertFalse(logContains("Failed to move the file."), "Log should NOT contain failure message.");
    }


    @Test
    @DisplayName("cleanFileName should replace invalid characters with underscores")
    void testCleanFileName_ReplacesInvalidChars() {
        // --- Arrange ---
        String dirtyFileName = "report<v1>:\"final\"/draft\\maybe|latest?.xlsx";
        String expectedCleanedName = "report_v1___final__draft_maybe_latest_.xlsx";
        String alreadyCleanName = "normal-document_v2.pdf";
        String emptyName = "";

        // --- Act ---
        String actualCleanedName = App.cleanFileName(dirtyFileName);
        String resultForCleanName = App.cleanFileName(alreadyCleanName);
        String resultForEmptyName = App.cleanFileName(emptyName);

        // --- Assert ---
        assertEquals(expectedCleanedName, actualCleanedName);
        assertEquals(alreadyCleanName, resultForCleanName);
        assertEquals(emptyName, resultForEmptyName);

        // No logging expected from cleanFileName unless an exception occurs (unlikely here)
        List<String> logs = getLogMessages();
        assertTrue(logs.isEmpty(), "No log messages expected from successful cleanFileName calls.");
    }

    @Test
    @DisplayName("readFileNames should return list of files, ignoring subdirectories")
    void testReadFileNames_SuccessWithMixedContent() throws IOException {
        // --- Arrange ---
        String file1Name = "document.txt";
        String file2Name = "image.jpg";
        String subDirName = "a_folder";
        createTestFile(sourceDir, file1Name, "Text content");
        createTestFile(sourceDir, file2Name, "Image data placeholder");
        Files.createDirectory(sourceDir.resolve(subDirName));
        createTestFile(sourceDir.resolve(subDirName), "nested_file.dat", "nested data");
        assertTrue(Files.exists(sourceDir.resolve(file1Name)));
        assertTrue(Files.exists(sourceDir.resolve(file2Name)));
        assertTrue(Files.isDirectory(sourceDir.resolve(subDirName)));

        // --- Act ---
        List<String> actualFileNames = App.readFileNames(sourceDir.toString());

        // --- Assert ---
        assertNotNull(actualFileNames);
        assertEquals(2, actualFileNames.size());
        List<String> expectedFileNames = Arrays.asList(file1Name, file2Name);
        Collections.sort(actualFileNames);
        Collections.sort(expectedFileNames);
        assertEquals(expectedFileNames, actualFileNames);

        // Verify Log Output - Ensure the error message is NOT present
        assertFalse(logContains("The specified path is not a directory or an I/O error occurred."),
                "Log should NOT contain the directory error message for a successful read.");
    }
}