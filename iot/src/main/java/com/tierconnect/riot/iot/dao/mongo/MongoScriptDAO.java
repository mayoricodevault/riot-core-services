package com.tierconnect.riot.iot.dao.mongo;

import com.mongodb.*;

import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.sdk.dao.MongoExecutionException;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import org.bson.types.Code;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;


public class MongoScriptDAO {

    /**
     * Singleton instance.
     */
    static MongoScriptDAO instance;

    /**
     * Logger To registry all mongo exceptions.
     */
    private static Logger logger = Logger.getLogger(MongoScriptDAO.class);

    /**
     * System Collection TO save all mongo functions.
     */
    private DBCollection systemCollection;

    /**
     * Prefix to set on all scripts.
     */
    public static final String SCRIPTNAME = "vizixFunction";

    /**
     * Create a a system collection to save all functions.
     */
    public void setup() {
        systemCollection = MongoDAOUtil.getInstance().db.getCollection("system.js");
    }

    /**
     * A static constructor.
     */
    static {
        instance = new MongoScriptDAO();
        instance.setup();
    }

    /**
     * Get Singleton Instance.
     *
     * @return return instance.
     */
    public static MongoScriptDAO getInstance() {
        return instance;
    }

    /**
     * Inserts new mongo Script, Use this method to insert the whole script in just one call to database
     *
     * @param name The unique name of a function to insert in mongo with Prefix.
     * @param code The mongo script code to insert.
     */
    public void insert(String name, String code)
            throws MongoExecutionException {
        insertRaw(generateScriptName(name), code);
    }

    /**
     * Inserts new mongo Script, Use this method to insert the whole script in just one call to database with a given
     * name
     *
     * @param name The unique name of a function to insert in mongo.
     * @param code The mongo script code to insert.
     */
    public void insertRaw(String name, String code) throws MongoExecutionException {
        try {
            if (code != null) {
                code = code.replace("\u0000", "");
            }
            Code functionCode = new Code(code);
            DBObject doc = new BasicDBObject("_id", name);
            doc.put("value", functionCode);
            systemCollection.update(new BasicDBObject("_id", name), doc, true, false);
        } catch (DuplicateKeyException e) {
            logger.error("Error to create a mongo script, The script already exists: ", e);
            throw new MongoExecutionException("Error to create a mongo script, The script already exists.", e);
        } catch (WriteConcernException e) {
            logger.error("The mongo Script failed due some other failure specific to the insert command: ", e);
            throw new MongoExecutionException("The mongo Script failed due some other failure specific to the insert." +
                    "command: ", e);
        } catch (MongoException e) {
            logger.error("Unknown Error to create a mongo script: ", e);
            throw new MongoExecutionException("Unknown Error to create a mongo script.", e);
        }
    }


    /**
     * Update the mongodb Script , Use this method to update the whole script in just one call to database.
     *
     * @param name The unique name of a function to insert in mongo.
     * @param code The mongo script code to insert.
     * @throws MongoExecutionException
     */
    public void update(String name, String code)
            throws MongoExecutionException {
        try {
            if (code != null) {
                code = code.replace("\u0000", "");
            }
            Code functionCode = new Code(code);
            DBObject docId = new BasicDBObject("_id", generateScriptName(name));
            DBObject docValue = new BasicDBObject("value", functionCode);
            systemCollection.update(docId, docValue, true, false);
        } catch (DuplicateKeyException e) {
            logger.error("The mongo script has two instances saved in the database: ", e);
            throw new MongoExecutionException("The mongo script has two instances saved in the database", e);
        } catch (WriteConcernException e) {
            logger.error("The mongo Script failed due some other failure specific to the update command: ", e);
            throw new MongoExecutionException("The mongo Script failed due some other failure specific to the update " +
                    "command", e);
        } catch (MongoException e) {
            logger.error("Unknown Error to create a mongo script: ", e);
            throw new MongoExecutionException("Unknown Error to create a mongo script", e);
        }
    }

    /**
     * Delete
     */
    public void delete(String name) throws WriteConcernException {
        try {
            DBObject doc = new BasicDBObject("_id", generateScriptName(name));
            systemCollection.remove(doc);
        } catch (WriteConcernException e) {
            logger.error("the report was not deleted,  It failed due some other failure specific to the delete " +
                    "command: ", e);
            throw e;
        } catch (MongoException e) {
            logger.error(" the delete operation failed for some other reason, It can't remove the mongo script: ", e);
            throw new MongoException("the delete operation failed for some other reason, It can't remove the mongo " +
                    "script", e);
        }
    }

    /**
     * DeleteAll
     */
    public void deleteAll() throws WriteConcernException {
        try {
            java.util.regex.Pattern compile = java.util.regex.Pattern.compile("^vizixFunction",
                    java.util.regex.Pattern.CASE_INSENSITIVE);
            DBObject doc = new BasicDBObject("_id", compile);
            systemCollection.remove(doc);
        } catch (WriteConcernException e) {
            logger.error("the report was not deleted,  It failed due some other failure specific to the delete " +
                    "command: ", e);
            throw e;
        } catch (MongoException e) {
            logger.error(" the delete operation failed for some other reason, It can't remove the mongo script: ", e);
            throw new MongoException("the delete operation failed for some other reason, It can't remove the mongo " +
                    "script", e);
        }
    }

    /**
     * Method to create a unique mongo function script name.
     *
     * @param name the Type is a suffix to set on function name.
     * @return The mongo function string name. [SCRIPTNAME + type(without spaces) + name(without spaces)]
     */
    public static String generateScriptName(String name) {
        return SCRIPTNAME + name.replaceAll("\\s+", ".");
    }

    /**
     * Method to create a unique mongo function script name.
     *
     * @param name the Type is a suffix to set on function name.
     * @return The mongo function string name. [SCRIPTNAME + type(without spaces) + name(without spaces)]
     */
    public static String generateFileName(String name) {
        return generateScriptName(name) + "_" + System.currentTimeMillis();
    }

    public static String generateTempFileName(String name) {
        return "tmp" + name;
    }

    /**
     * @param path         path where files are located
     * @param name         name of the las report file
     * @param cacheTimeOut timeout to report file validity
     * @return file report available in cache timeout     *
     */
    public static File getLastReportFile(String path, final String name, Long cacheTimeOut) {
        File outFile = null;
        try {
            File folder = new File(path);
            @SuppressWarnings("Convert2Lambda")
            File[] listOfFiles = folder.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String file) {
                    return file.startsWith(name) && !file.equals(name);
                }
            });
            if(listOfFiles != null) {
                Arrays.sort(listOfFiles, Collections.reverseOrder());
                for (File repoFile : listOfFiles) {
                    Long repoTime = Long.parseLong(repoFile.getName().substring((repoFile.getName().indexOf("_") + 1)));
                    if ((repoTime + cacheTimeOut) >= System.currentTimeMillis() && outFile == null) {
                        outFile = repoFile;
                    } else {
                        if (!repoFile.delete()) {
                            logger.debug("Cannot delete file " + repoFile.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return outFile;
    }

    /**
     * @param path    path where files are located
     * @param inFile  original file
     * @param outName output file name
     * @return true if rename was successful, false otherwise
     */
    public static File renameReportFile(String path, File inFile, String outName) {
        File outFile = new File(path + outName);
        if (!outFile.exists() && inFile.renameTo(outFile)) {
            return outFile;
        } else {
            return inFile;
        }
    }

    public void createFromResource(String name, String resource){
        DBObject doc = new BasicDBObject("_id", name);
        if (systemCollection.findOne(doc) == null) {
            logger.info("Creating " + name + ".");
            try {
                URL fileURL = MongoScriptDAO.class.getClassLoader().getResource(resource);
                if (fileURL != null) {
                    String text = IOUtils.toString(fileURL, Charset.forName("UTF-8"));
                    MongoScriptDAO.getInstance().insertRaw(name, text);
                }
            } catch (Exception e) {
                logger.error("Cannot create " + name + ".", e);
            }
        }

    }

}
