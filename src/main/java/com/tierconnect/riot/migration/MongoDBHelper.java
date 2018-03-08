package com.tierconnect.riot.migration;


import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.sdk.dao.UserException;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.IOException;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;

/**
 * Created by angelchambi on 6/3/16.
 *
 * A class to help the mongo data migrations.
 */
public class MongoDBHelper {

    static Logger logger = Logger.getLogger(MongoDBHelper.class);

    /**
     * Execute a File in Mongo Data Base.
     *
     */
    public void executeMongoFile(String file) throws Exception {
        executeMongoQuery(file);
    }

    /**
     * Method to convert file content to string.
     *
     * @param filename File name to convert to string.
     * @return The file content in String.
     */
    @SuppressWarnings("unused")
    public String fileToString(String filename) {
        try {
            URL fileURL = MongoDBHelper.class.getClassLoader().getResource(filename);
            if (fileURL != null) {
                return IOUtils.toString(fileURL, Charset.forName("UTF-8"));
            } else {
                return null;
            }
        } catch (IOException e) {
            return "";
        }
    }

    @SuppressWarnings("NullArgumentToVariableArgMethod")
    /**
     * Execute a mongo String query.
     */
    public void executeMongoQuery(String mongoFileName) throws UserException {
        try {
            String filePath;
            URL fileURL = MongoDBHelper.class.getClassLoader().getResource(mongoFileName);
            if (fileURL != null) {
                filePath = fileURL.getPath();
                filePath = URLDecoder.decode(filePath, "utf-8");

                MongoDAOUtil.getInstance().runFileCommand(filePath);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new UserException("'Mongo script' can not be executed.", e);
        }
    }
}



