package com.tierconnect.riot.iot.reports.autoindex.dao;


import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;

import com.tierconnect.riot.iot.reports.autoindex.entities.indexInformation.IndexInformation;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.IOException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Created by achambi on 6/29/17.
 * CLass for DAO Only Replica Set IndexInformation Mongo
 */
public class MongoDAO {

    static Logger logger = Logger.getLogger(MongoDAO.class);
    private List<MongoCredential> mongoCredentials;
    private ServerAddress serverAddress;
    private String dataBase;

    /**
     * Mongo default constructor.
     *
     * @param userName A {@link String} containing the userName.
     * @param password A {@link String} containing the password.
     */
    public MongoDAO(String userName, String password, String dataBaseAdmin, String host, int port, String dataBase) {
        mongoCredentials = new ArrayList<>();
        this.serverAddress = new ServerAddress(host, port);
        this.mongoCredentials.add(MongoCredential.createScramSha1Credential(
                userName,
                dataBaseAdmin,
                password.toCharArray()));
        this.dataBase = dataBase;
    }

    /**
     * This method is only available to get index statistics in secondary or single servers.
     *
     * @param collectionName A {@link String} containing the collection name.
     * @param indexNames     A {@link String}[] containing index names.
     * @return A instance of {@link List}<{@link IndexInformation}>.
     * @throws IOException If Input/Output error exists.
     */
    public List<IndexInformation> getIndexStatistics(String collectionName, String... indexNames) throws IOException {
        MongoClient mongoClient = null;
        MongoCursor<Document> iterator = null;
        List<IndexInformation> indexInformationList = new LinkedList<>();
        try {
            mongoClient = new MongoClient(this.serverAddress, this.mongoCredentials);
            List<Bson> pipeline = new LinkedList<>();
            pipeline.add(new Document("$indexStats", new Document()));
            if (indexNames != null && indexNames.length != 0) {
                pipeline.add(Aggregates.match(Filters.in("name", indexNames)));
            }
            iterator = mongoClient.getDatabase(dataBase).getCollection(collectionName).aggregate(pipeline).iterator();
            while (iterator.hasNext()) {
                indexInformationList.add(new IndexInformation(iterator.next()));
            }
        } catch (Exception ex) {
            logger.error("error mongo connection exception: ", ex);
        } finally {
            if (iterator != null) {
                iterator.close();
            }
            if (mongoClient != null) {
                mongoClient.close();
            }
        }
        return indexInformationList;
    }
}
