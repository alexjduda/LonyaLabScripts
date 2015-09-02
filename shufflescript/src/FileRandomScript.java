import com.sun.xml.internal.bind.v2.runtime.output.Encoded;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Random;
import javax.swing.*;



/**
 * Class containing the logic for the file obfuscation script
 *
 * This script is used to obfuscate files during procedures where human readable filenames might lead to human bias.
 * Running this script in the forward direction will create a temporary directory with copies of all files from a source
 * directory. The files in the new directory will have their names obfuscated. After the user interacts with the files
 * in whatever way they need to, running this script in the reverse direction will de-obfuscate the files. The
 * de-obfuscated files will remain in the new directory to preserve any transformative actions the user may have taken,
 * and the source files will remain unchanged as backups. The work of obfuscation is done in
 * {@link #genName(String, String)}
 */
public class FileRandomScript {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Please provide only one argument, r or d");
            return;
        }

        String firstArgument = args[0];
        String csvName = "Randomization_List.csv";
        String csvNameReverse = "Randomization_List_Final.csv";

        // First decide what direction the user wants us to go
        if (!firstArgument.equals("r") && !firstArgument.equals("d")) {
            System.out.println("Please enter r or d");
            return;
        }

        // Print the direction in which we're going to the user.
        if (firstArgument.equals("r")) {
            System.out.println("Randomizing");
        } else {
            System.out.println("Derandomizing");
        }

        // Set up our input and output targets. If we are randomizing them the output folder will be a separate location
        // from the input folder. If we're derandomizing (de-obfuscating) then we're staying in the same folder.
        File inDir = getInputDirectory();

        File outDir = null;
        String outCSV = null;
        BufferedWriter writeF = null;
        if (firstArgument.equals("r")) {
            outDir = getOutputDirectory();
            outCSV = (outDir.getAbsoluteFile() + "/");
            writeF = csvGeneratorR(outCSV, csvName);
        } else {
            outDir = inDir;
            outCSV = (outDir.getAbsoluteFile() + "/");
        }

        //TODO: we should be skipping this if we're derandomizing

        // Process each file
        File[] listOfInFiles = inDir.listFiles();
        System.out.println("Beginning process on " + listOfInFiles.length + " files from " + inDir.getAbsolutePath());
        BufferedWriter writeR = null;
        for (File inFile : listOfInFiles) {
            String fileFullName = inFile.getName();
            if (fileFullName.equals("Randomization_List.csv")) {
                continue;
            }
            if (fileFullName.equals("Randomization_List_Final.csv")) {
                continue;
            }
            System.out.println("Processing " + fileFullName);

            // We only want to obfuscate the filename, the file extension needs to be extracted and added back in later
            String[] fileNameArray = fileFullName.split("\\.");
            String fileName = fileNameArray[0];
            for (int i = 1; i < fileNameArray.length - 1; i++) {
                fileName += "." + fileNameArray[i];
            }

            // If the file name array only has one element, then there's no file extension
            String fileExt = "";
            if (fileNameArray.length > 1) {
                fileExt = "." + fileNameArray[fileNameArray.length - 1];
            }


            String newFileNamenoExt = genName(fileName, firstArgument);
            String newFileName = newFileNamenoExt + fileExt;


            File outFile = new File(outDir.getAbsoluteFile() + "/" + newFileName);
            if (firstArgument.equals("r")) {
                copyFile(inFile, outFile);
                csvWriterR(newFileNamenoExt, writeF);
            } else {
                moveFile(inFile, outFile);
            }

            if (firstArgument.equals("d")){
                writeR = csvWriterD(outCSV, csvName, csvNameReverse);
            }

        }
        if (firstArgument.equals("r")) {
            try {
                writeF.close();
            } catch (IOException e) {
                System.out.println("Failed to write to csv");
            }
        }
        if (firstArgument.equals("d")) {
            try {
                writeR.close();
            } catch (IOException e) {
                System.out.println("Failed to write to csv");
            }
        }
    }

    /**
     * Prompts the user to select the input directory.
     */
    private static File getInputDirectory() {
        System.out.println("Requesting input directory from user.");
        return getDirectory("Select Input Folder");
    }

    /**
     * Prompts the user to select the output directory.
     */
    private static File getOutputDirectory() {
        System.out.println("Requesting output directory from user.");
        return getDirectory("Select Output Folder");
    }

    /**
     * Using a {@link JFileChooser}, prompts the user for a directory selection using the given message.
     *
     * @param message user prompt to guide the user's directory decision
     */
    private static File getDirectory(String message) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(message);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.showSaveDialog(null);
        File tmpFile = fileChooser.getSelectedFile();
        System.out.println("User selected: " + tmpFile.getAbsolutePath());
        return tmpFile;
    }

    /**
     * Generate the new name of the given file based on the direction in which we're going.
     *
     * @param fileName  filename to alter
     * @param direction "r" for randomize or "d" for derandomize
     * @return final filename
     */
    public static String genName(String fileName, String direction) {
        String editedName;
        String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        int alphabetlength = alphabet.length();
        if (direction.equals("r")) {
            char[] NametoArray = fileName.toCharArray();
            int NameArrayLength = NametoArray.length;
            int ExtendedLength = NameArrayLength * 2 + 1;
            char[] NameforEncoding;
            NameforEncoding = new char[ExtendedLength];
            Random NumberGenerator = new Random();

            for (int i = 0; i < NameforEncoding.length; i++) {
                if (i % 2 == 0) { // We are at an even position (0, 2, ...)
                    int randomIndex = NumberGenerator.nextInt(alphabetlength);
                    char randomChar = alphabet.charAt(randomIndex);
                    NameforEncoding[i] = randomChar;
                } else { // We are at an odd position
                    NameforEncoding[i] = NametoArray[i / 2];
                }
            }
            String toEncode = new String(NameforEncoding);


            byte[] encoded = toEncode.getBytes(StandardCharsets.UTF_8);
            editedName = Base64.getEncoder().encodeToString(encoded);
            return editedName;

        } else {
            byte[] decoded = Base64.getDecoder().decode(fileName);
            String decodedName = new String(decoded, StandardCharsets.UTF_8);


            char[] EncodedName = decodedName.toCharArray();
            int EncodedLength = EncodedName.length;
            int OriginalLength = (EncodedLength / 2);
            char[] OriginalName = new char[OriginalLength];
            for (int i = 0; i < EncodedName.length; i++) {
                if (i % 2 == 1) {
                    OriginalName[i - (i / 2) - 1] = EncodedName[i];
                }
            }

            String InitialName = new String(OriginalName);

            return InitialName;
        }
    }

    /**
     * Copies the input file to the location of the output file. Informs the user if the file already exists.
     *
     * @param inFile  location of the input file
     * @param outFile location of the output file
     */
    public static void copyFile(File inFile, File outFile) {
        try {
            Files.copy(inFile.toPath(), outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (FileAlreadyExistsException e) {
            System.out.println("The File Already Exists");
        } catch (Exception e) {
            System.out.println("Unknown error occured while copying " + inFile.getName() + " to " + outFile.getName());
            e.printStackTrace();
        }
    }

    /**
     * Copies the input file to the location of the output file. Informs the user if the file already exists.
     *
     * @param inFile  location of the input file
     * @param outFile location of the output file
     */
    public static void moveFile(File inFile, File outFile) {
        try {
            Files.move(inFile.toPath(), outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.out.println("IOException occured while moving " + outFile.getName() + " to " + outFile.getName());
        } catch (Exception e) {
            System.out.println("Unknown error occured while moving " + inFile.getName() + " to " + outFile.getName());
            e.printStackTrace();
        }
    }

    public static BufferedWriter csvGeneratorR(String outCSV, String csvName) {
        BufferedWriter writeF = null;
        try {
            FileWriter forwardcsv = new FileWriter(outCSV + csvName);
            writeF = new BufferedWriter(forwardcsv);
            writeF.write("Original Name,");
            writeF.write("Obfuscated Name,");
        } catch (IOException ex) {
            System.out.println("Error Creating .csv");
        }
        return writeF;


    }


    public static void csvWriterR(String newFileNamenoExt, BufferedWriter writeF) {
        try {
            writeF.newLine();
            writeF.write(",");
            writeF.write(newFileNamenoExt);


        } catch (IOException ex) {
            System.out.println("Error Filling .csv");
        }
    }

    public static BufferedWriter csvWriterD(String outCSV, String csvName, String csvNameReverse) {
        String line = null;
        BufferedWriter writeD = null;

        try {
            FileWriter reversecsv = new FileWriter(outCSV + csvNameReverse);
            writeD = new BufferedWriter(reversecsv);
            FileReader excelreader = new FileReader(outCSV + csvName);
            BufferedReader reader = new BufferedReader(excelreader);

            String readline = reader.readLine();
            String[] readlinearray = readline.split(",");
            for (int i = 0; i < readlinearray.length; i++) {
                writeD.write(readlinearray[i]);
                writeD.write(",");
            }
                writeD.newLine();

            while((line = reader.readLine()) != null) {
                String[] linearray = line.split(",");
                String derandomizeforexcel = linearray[1];


                byte[] decoded = Base64.getDecoder().decode(derandomizeforexcel);
                String decodedName = new String(decoded, StandardCharsets.UTF_8);


                char[] EncodedName = decodedName.toCharArray();
                int EncodedLength = EncodedName.length;
                int OriginalLength = (EncodedLength / 2);
                char[] OriginalName = new char[OriginalLength];
                for (int i = 0; i < EncodedName.length; i++) {
                    if (i % 2 == 1) {
                        OriginalName[i - (i / 2) - 1] = EncodedName[i];
                    }
                }

                String originalexcelname = new String(OriginalName);

                writeD.write(originalexcelname);
                writeD.write(",");
                for (int i = 1; i < linearray.length; i++) {
                    writeD.write(linearray[i]);
                    writeD.write(",");

                }
                writeD.newLine();
            }

        } catch (FileNotFoundException e) {
            System.out.println("Cannot find Randomization_List.csv");
        } catch (IOException e) {
            System.out.println("Can't read");
        }
        try {
            writeD.newLine();
        } catch (IOException ex) {
            System.out.println("Error Filling .csv");
        }
        return writeD;
    }
}
