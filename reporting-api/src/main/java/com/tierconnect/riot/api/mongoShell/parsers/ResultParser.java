package com.tierconnect.riot.api.mongoShell.parsers;


import com.tierconnect.riot.api.mongoShell.MongoErrorMessage;
import com.tierconnect.riot.api.mongoShell.ResultQuery;
import com.tierconnect.riot.api.mongoTransform.BsonToMap;
import org.apache.commons.io.Charsets;
import org.bson.BsonDocument;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.tierconnect.riot.api.assertions.Assertions.isTrueArgument;
import static com.tierconnect.riot.api.assertions.Assertions.voidNotNull;

/**
 * Created by achambi on 12/9/16.
 * Class to parse the Result File.
 */
public class ResultParser {

    private ResultParser() {
    }

    //TODO: Improve the algorithm, Victor Angel Chambi Nina. 15/08/2017 (Tuesday, August 15, 2017)

    /**
     * Method that parse the result file to ResultQuery format.
     *
     * @param filePath      the file path to parse.
     * @param enableExplain flag to enable or disable parse explain information.
     * @return A instance of {@link ResultQuery}.
     * @throws IOException If there is an error checking that the file exists, or it can not be read or there is a
     *                     parse error.
     */
    public static ResultQuery parseFile(String filePath, boolean enableExplain, boolean addQuery, boolean addCount) throws IOException {
        voidNotNull("filePath", filePath);
        File file = new File(filePath);
        isTrueArgument("filePath", "exists: true", file.exists());
        isTrueArgument("filePath", "read: true", file.canRead());
        List<Map<String, Object>> result = new LinkedList<>();
        Map<String, Object> explainResult = null;
        InputStream inputStream = new FileInputStream(file);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, Charsets.UTF_8))) {
            String line = br.readLine();

            MongoErrorMessage mongoErrorMessage = MongoErrorMessage.errorMessageHandler(line);
            if (mongoErrorMessage != null && addCount) {//empty file is just permitted when is query
                return new ResultQuery(-2, mongoErrorMessage);
            }

            int total = 0;
            //noinspection ConstantConditions
            if (addCount && line != null) {
                total = Integer.parseInt(line);
            }
            if (addQuery) {
                if (addCount) {
                    line = br.readLine();
                }

                if (line != null) {
                    mongoErrorMessage = MongoErrorMessage.errorMessageHandler(line);
                    if (mongoErrorMessage != null) {
                        return new ResultQuery(-2, mongoErrorMessage);
                    }

                    if (enableExplain) {
                        explainResult = BsonToMap.getMap(BsonDocument.parse(line));
                        line = br.readLine();
                    }
                }

                while (line != null) {
                    result.add(BsonToMap.getMap(BsonDocument.parse(line)));
                    line = br.readLine();
                }
            }
            return new ResultQuery(total, result, explainResult);
        }
    }
}
