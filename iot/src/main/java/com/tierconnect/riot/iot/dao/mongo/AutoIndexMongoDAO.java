package com.tierconnect.riot.iot.dao.mongo;

import com.mongodb.*;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.sdk.dao.NotFoundException;
import com.tierconnect.riot.sdk.utils.HashUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by julio.rocha on 10-07-17.
 */
public class AutoIndexMongoDAO {
    private static Logger logger = Logger.getLogger(AutoIndexMongoDAO.class);
    private static final AutoIndexMongoDAO INSTANCE = new AutoIndexMongoDAO();
    private DBCollection autoIndexCollection;

    public static AutoIndexMongoDAO getInstance() {
        return INSTANCE;
    }

    public static String generateAutoIndexId(String collection, String query, String sortField) {
        return "idx_" + HashUtils.hashMD5(collection + query +
                ((StringUtils.isNotEmpty(sortField)) ? sortField : ""));
    }

    private AutoIndexMongoDAO() {
        autoIndexCollection = MongoDAOUtil.getInstance().db.getCollection("autoIndex");
    }

    public void insert(String collection, String query, String sortField, String definition) {
        String id = generateAutoIndexId(collection, query, sortField);
        DBObject autoIndex = new BasicDBObject();
        autoIndex.put("_id", id);
        autoIndex.put("created", Boolean.FALSE);
        autoIndex.put("name", id);
        autoIndex.put("definition", definition);
        WriteResult result = autoIndexCollection.insert(autoIndex, WriteConcern.ACKNOWLEDGED);
        logger.info("\n\n**** AUTO INDEX INSERTED ****\n\n");
        logger.info(result);
    }

    public void updateStatusAndName(String id, Boolean status, String name) {
        DBObject queryFind = new BasicDBObject("_id", id);
        BasicDBObject fields = new BasicDBObject("created", status);
        fields.append("name", name);
        WriteResult result = autoIndexCollection.update(queryFind, new BasicDBObject("$set", fields));
        logger.info("\n\n**** AUTO INDEX UPDATED ****\n\n");
        logger.info(result);
    }

    public void updateStatusByAssociatedIndex(String associatedIndex, Boolean status) {
        DBObject queryFind = new BasicDBObject("name", associatedIndex);
        BasicDBObject fields = new BasicDBObject("created", status);
        WriteResult result = autoIndexCollection.update(queryFind, new BasicDBObject("$set", fields));
        logger.info("\n\n**** AUTO INDEX UPDATED ****\n\n");
        logger.info(result);
    }

    public DBObject getDocument(String id) {
        BasicDBObject queryFind = new BasicDBObject("_id", id);
        return autoIndexCollection.find(queryFind).one();
    }

    public String getIndexName(String id) {
        DBObject document = getDocument(id);
        if (document == null) {
            throw new NotFoundException("Index name not found");
        } else if (Boolean.FALSE.equals(document.get("created"))) {
            throw new IllegalStateException("Index creation is still in progress");
        }
        return document.get("name").toString();
    }

    public String[] getAssociatedIndexes() {
        List<String> associatedReportIndexes = new LinkedList<>();
        Pattern reportLogIndexPattern = Pattern.compile("^auto_");
        BasicDBObject queryFind = new BasicDBObject("name", reportLogIndexPattern);
        queryFind.append("created", Boolean.TRUE);
        DBCursor cursor = autoIndexCollection.find(queryFind);
        while (cursor.hasNext()) {
            associatedReportIndexes.add(cursor.next().get("name").toString());
        }
        return associatedReportIndexes.toArray(new String[0]);
    }
}
