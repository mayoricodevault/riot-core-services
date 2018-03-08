package com.tierconnect.riot.api.mongoShell.utils;

import com.google.common.base.Charsets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.tierconnect.riot.api.assertions.Assertions.voidNotNull;

/**
 * Created by achambi on 1/31/17.
 * Class to manage the resource folder.
 */
public class ResourceUtils {

    /**
     * Method to read a file and return the content.
     *
     * @param filePath A {@link String} containing file path.
     * @return A {@link String} containing file content.
     * @throws IOException If I/O error exists.
     */
    //TODO: Change this code or delete it to create the query while writing the file.
    public static String readFile(String filePath) throws IOException {
        voidNotNull("filePath", filePath);
        InputStream inputStream = ResourceUtils.class.getClassLoader().getResourceAsStream(filePath);
        voidNotNull("inputStream", inputStream);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, Charsets.UTF_8))) {
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
