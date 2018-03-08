package com.tierconnect.riot.api.mongoShell.utils;

import org.apache.commons.io.Charsets;
import org.apache.log4j.Logger;


import java.io.*;
import java.nio.charset.StandardCharsets;

import static com.tierconnect.riot.api.assertions.Assertions.*;

/**
 * Created by achambi on 10/19/16.
 * Render class to convert InputStream to StringBuffer.
 */
public class FileUtils {

    private FileUtils() {
    }

    private static Logger logger = Logger.getLogger(FileUtils.class);

    /**
     * Load a InputStream and return  StringBuffer.
     *
     * @param inputStream to convert to StringBuffer
     * @return a instance of StringBuffer
     * @throws IOException If error exists.
     */
    static StringBuffer loadInputStream(InputStream inputStream) throws IOException {
        StringBuffer output;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            output = new StringBuffer();
            if (reader.ready()) {
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
        } catch (NullPointerException ex) {
            logger.error("inputStream is Null", ex);
            throw new NullPointerException("inputStream is Null.");
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return output;
    }

    public static void writeFile(File file, String content) throws IOException {
        Writer bw = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
        bw.write(content);
        bw.close();
    }

    public static File getFile(String filePath) throws NullPointerException {
        voidNotNull("filePath", filePath);
        File file = new File(filePath);
        isTrueArgument("filePath", "exists: true", file.exists());
        isTrueArgument("filePath", "read: true", file.canRead());
        return file;
    }

    public static void deleteFile(File file) {
        try {
            isTrue("The file delete", file.delete());
        } catch (IllegalArgumentException | SecurityException e) {
            logger.warn("Error deleting file" + e);
        }
    }

    public static String readFile(String filePath) throws IOException {
        voidNotNull("filePath", filePath);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), Charsets
                .UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            return sb.toString();
        }
    }
}
