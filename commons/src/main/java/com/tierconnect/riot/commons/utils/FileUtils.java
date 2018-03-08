package com.tierconnect.riot.commons.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Created by dbascope on 10/6/16
 */
public class FileUtils {
    private static Logger logger = Logger.getLogger(FileUtils.class);
    private File file;
    private String path;
    private String fileName;

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public FileUtils(String fileName, String fileContent) throws IOException {
        File temp;
        try {
            temp = File.createTempFile(fileName, ".tmp");
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp), "UTF-8"));
            bw.write(fileContent);
            bw.close();
        } catch (IOException e) {
            logger.error("An error occurred Input/Output", e);
            throw new IOException("An error occurred Input/Output.");
        } catch (NullPointerException e) {
            logger.error("Temporal file name or content is null", e);
            throw new NullPointerException("Temporal file name or content is null.");
        }
        this.file = temp;
        this.path = temp.getAbsolutePath();
        this.fileName = temp.getName();
    }

    /**
     * Removes the temporary file
     *
     * @return a boolean that defines if remove was successful
     */
    public boolean removeFile() {
        try {
            return this.file.delete();
        } catch (NullPointerException e) {
            logger.error("File remove failed, File is null.", e);
            throw new NullPointerException("File remove failed, File is null.");
        }
    }

}
