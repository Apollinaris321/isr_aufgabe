import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Hello world!
 *
 */
public class App 
{
    private static final Logger logger = LogManager.getLogger(App.class);

    public static boolean moveFile(String sourcePath, String targetPath, String targetName){
        File sourceFile = new File(sourcePath);
        File targetDir = new File(targetPath);

        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        File destinationFile = new File(targetDir , targetName);

        if (sourceFile.renameTo(destinationFile)) {
            logger.info("File moved successfully.");
            return true;
        } else {
            logger.error("Failed to move the file.");
            return false;
        }
    }

    public static List<String> readFileNames(String folderPath){
        String directoryPath = "import";
        File directory = new File(folderPath);
        List<String> paths = new ArrayList<String>();

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    paths.add(file.getName());
                    System.out.println(file.getName());
                }
            }
        } else {
            logger.error("The specified path is not a directory or an I/O error occurred.");
        }

        return paths;
    }

    public static String cleanFileName(String fileName){
        try{
            String cleanedFileName = fileName.replaceAll("[<>:\"/\\\\|?*]", "_");
            return cleanedFileName;
        }catch (Exception e){
            logger.error(e);
            return null;
        }
    }

    public static void importFiles(String sourceDir, String targetDir){
        List<String> fileNames = readFileNames(sourceDir);
        for(String filename : fileNames){
            String cleanedName = cleanFileName(filename);
            if(cleanedName != null){
                moveFile(sourceDir + "/" + filename, targetDir, cleanedName);
            }
        }
    }

    public static void main( String[] args )
    {
        String s1 = "input";
        String s2 = "import";

        importFiles(s1, s2);
        //SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Folder Selection");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 300);
        frame.setLayout(new GridBagLayout());
        frame.setLocationRelativeTo(null); // Center the frame

        JButton selectDestinationButton = new JButton("Select Destination Folder");
        JLabel destinationPathLabel = new JLabel("");
        JButton selectSourceButton = new JButton("Select Source Folder");
        JLabel sourcePathLabel = new JLabel("");
        JButton runButton = new JButton("Run");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // Padding
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;

        frame.add(selectDestinationButton, gbc);

        gbc.gridy++;
        frame.add(destinationPathLabel, gbc);

        gbc.gridy++;
        frame.add(selectSourceButton, gbc);

        gbc.gridy++;
        frame.add(sourcePathLabel, gbc);

        gbc.gridy++;
        frame.add(runButton, gbc);

        selectDestinationButton.addActionListener(e -> {
            File selectedFolder = chooseFolder(frame);
            if (selectedFolder != null) {
                destinationPathLabel.setText(selectedFolder.getAbsolutePath());
            }
        });

        selectSourceButton.addActionListener(e -> {
            File selectedFolder = chooseFolder(frame);
            if (selectedFolder != null) {
                sourcePathLabel.setText(selectedFolder.getAbsolutePath());
            }
        });

        runButton.addActionListener(e -> {
            importFiles(sourcePathLabel.getText(), destinationPathLabel.getText());
        });

        frame.setVisible(true);
    }

    private static File chooseFolder(Component parent) {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setDialogTitle("Select a Folder");
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        folderChooser.setAcceptAllFileFilterUsed(false);

        int returnValue = folderChooser.showOpenDialog(parent);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            return folderChooser.getSelectedFile();
        } else {
            return null;
        }
    }
}
