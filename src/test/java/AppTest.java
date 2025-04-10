import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {

    @TempDir
    Path tempDir;

    Path sourceDir;
    Path targetDir;
    Path testLogFile;

    @BeforeEach
    void setUp() throws IOException {
        sourceDir = tempDir.resolve("input");
        targetDir = tempDir.resolve("import");
        Files.createDirectory(sourceDir);

        testLogFile = Paths.get("target", "test-protocoll.log");

        // Clear log file before each test
        Files.deleteIfExists(testLogFile);

        // Ensure target log directory exists
        if (testLogFile.getParent() != null) {
            Files.createDirectories(testLogFile.getParent());
        }
    }

    private String readLogFileContent() throws IOException {
        if (!Files.exists(testLogFile)) {
            return "";
        }
        byte[] bytes = Files.readAllBytes(testLogFile);
        return new String(bytes, StandardCharsets.UTF_8);
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

        // Create the source file in the temporary source directory
        Path sourceFilePath = sourceDir.resolve(sourceFileName);
        createTestFile(sourceDir, sourceFileName, fileContent);

        assertTrue(Files.exists(sourceFilePath), "Precondition failed: Source file should exist");
        assertFalse(Files.exists(targetDir), "Precondition failed: Target directory should not exist yet");

        // --- Act ---
        boolean result = App.moveFile(sourceFilePath.toString(), targetDir.toString(), targetFileName);

        // --- Assert ---
        assertTrue(result, "moveFile should return true on successful move.");

        Path targetFilePath = targetDir.resolve(targetFileName);
        assertFalse(Files.exists(sourceFilePath), "Source file should be removed after move.");
        assertTrue(Files.exists(targetFilePath), "Target file should exist after move.");
        assertTrue(Files.isDirectory(targetDir), "Target directory should have been created.");

        // Optional: Verify content of the moved file
        String movedContent = new String(Files.readAllBytes(targetFilePath), StandardCharsets.UTF_8);
        assertEquals(fileContent, movedContent, "Content of moved file should match original.");

        // Verify Log Output
        String logContent = readLogFileContent();
        assertTrue(logContent.contains("File moved successfully."), "Log should contain success message.");
        assertFalse(logContent.contains("Failed to move the file."), "Log should NOT contain failure message.");
    }


    @Test
    @DisplayName("cleanFileName should replace invalid characters with underscores")
    void testCleanFileName_ReplacesInvalidChars() {
        // --- Arrange ---
        // Input string containing all specified invalid characters: <>:"/\|?*
        String dirtyFileName = "report<v1>:\"final\"/draft\\maybe|latest?.xlsx";
        String expectedCleanedName = "report_v1___final__draft_maybe_latest_.xlsx";

        String alreadyCleanName = "normal-document_v2.pdf";
        String emptyName = "";

        // --- Act ---
        String actualCleanedName = App.cleanFileName(dirtyFileName);
        String resultForCleanName = App.cleanFileName(alreadyCleanName);
        String resultForEmptyName = App.cleanFileName(emptyName);


        // --- Assert ---
        assertEquals(expectedCleanedName, actualCleanedName,
                "Invalid characters were not correctly replaced with underscores.");

        assertEquals(alreadyCleanName, resultForCleanName,
                "A file name with no invalid characters should remain unchanged.");

        assertEquals(emptyName, resultForEmptyName,
                "An empty file name should result in an empty string.");
    }

    @Test
    @DisplayName("readFileNames should return list of files, ignoring subdirectories")
    void testReadFileNames_SuccessWithMixedContent() throws IOException {
        // --- Arrange ---
        String file1Name = "document.txt";
        String file2Name = "image.jpg";
        String subDirName = "a_folder";

        // Create files in the temporary source directory
        createTestFile(sourceDir, file1Name, "Text content");
        createTestFile(sourceDir, file2Name, "Image data placeholder");

        // Create a subdirectory in the source directory
        Files.createDirectory(sourceDir.resolve(subDirName));
        createTestFile(sourceDir.resolve(subDirName), "nested_file.dat", "nested data");


        assertTrue(Files.exists(sourceDir.resolve(file1Name)), "Precondition failed: File 1 should exist.");
        assertTrue(Files.exists(sourceDir.resolve(file2Name)), "Precondition failed: File 2 should exist.");
        assertTrue(Files.isDirectory(sourceDir.resolve(subDirName)), "Precondition failed: Subdirectory should exist.");


        // --- Act ---
        List<String> actualFileNames = App.readFileNames(sourceDir.toString());


        // --- Assert ---
        assertNotNull(actualFileNames, "The returned list should not be null.");
        assertEquals(2, actualFileNames.size(),
                "Should find exactly 2 files, ignoring the subdirectory.");

        // Sort lists before comparison for reliable results regardless of OS listing order
        List<String> expectedFileNames = Arrays.asList(file1Name, file2Name);
        Collections.sort(actualFileNames);
        Collections.sort(expectedFileNames);

        assertEquals(expectedFileNames, actualFileNames,
                "The list of filenames returned does not match the expected files.");

        // Verify Log Output - Ensure the error message is NOT present
        String logContent = readLogFileContent();
        assertFalse(logContent.contains("The specified path is not a directory or an I/O error occurred."),
                "Log should NOT contain the directory error message for a successful read.");
    }
}