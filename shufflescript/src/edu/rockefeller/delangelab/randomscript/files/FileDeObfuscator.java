package edu.rockefeller.delangelab.randomscript.files;

import edu.rockefeller.delangelab.randomscript.constants.Constants;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by aduda on 8/31/15.
 */
public class FileDeObfuscator extends FileManipulator{

    private static final Logger LOGGER = Logger.getLogger(FileDeObfuscator.class.getName());

    public FileDeObfuscator(File directory) throws IOException {
        super(directory, directory);
        this.csvFile = new File(outputDirectory, Constants.CSV_FILE_NAME_REVERSE);
        this.bufferedWriter = new BufferedWriter(new FileWriter(csvFile));
    }

    @Override
    public void manipulate() {
        super.manipulate();
        csvWriterD();
        try {
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String genName(String fileName) {
        byte[] decoded = Base64.getDecoder().decode(fileName);
        String decodedName = new String(decoded, StandardCharsets.UTF_8);


        char[] encodedName = decodedName.toCharArray();
        int encodedLength = encodedName.length;
        int originalLength = (encodedLength / 2);
        char[] originalName = new char[originalLength];
        for (int i = 0; i < encodedName.length; i++) {
            if (i % 2 == 1) {
                originalName[i - (i / 2) - 1] = encodedName[i];
            }
        }

       return new String(originalName);
    }

    /**
     * Copies the input file to the location of the output file. Informs the user if the file already exists.
     * @param inFile location of the input file
     * @param outFile location of the output file
     */
    @Override
    public void transferFile(File inFile, File outFile) {
        try {
            Files.move(inFile.toPath(), outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to move " + outFile.getName() + " to " + outFile.getName(), e);
        }
    }

    private BufferedWriter csvWriterD() {
        try {
            FileReader excelreader = new FileReader(new File(this.outputDirectory, Constants.CSV_FILE_NAME));
            BufferedReader reader = new BufferedReader(excelreader);

            String readline = reader.readLine();
            String[] readlinearray = readline.split(",");
            for (int i = 0; i < readlinearray.length; i++) {
                bufferedWriter.write(readlinearray[i]);
                bufferedWriter.write(",");
            }
            bufferedWriter.newLine();

            String line;
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

                bufferedWriter.write(originalexcelname);
                bufferedWriter.write(",");
                for (int i = 1; i < linearray.length; i++) {
                    bufferedWriter.write(linearray[i]);
                    bufferedWriter.write(",");

                }
                bufferedWriter.newLine();
            }

        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Cannot find " + Constants.CSV_FILE_NAME, e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read " + Constants.CSV_FILE_NAME, e);
        }
        try {
            bufferedWriter.newLine();
            bufferedWriter.close();


        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to write to " + Constants.CSV_FILE_NAME_REVERSE, ex);
        }

        try {
            Deleter();
        } finally {

        }

        return bufferedWriter;


    }

   private void Deleter () {
        File todelete = new File(this.outputDirectory, Constants.CSV_FILE_NAME);
        todelete.delete();
        System.out.println(todelete);

    }


}
