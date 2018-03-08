package com.tierconnect.riot.iot.reports.autoindex.dao;

import com.tierconnect.riot.iot.reports.autoindex.entities.indexInformation.IndexInformation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by achambi on 6/29/17.
 * Test for new Class Mongo DAO.
 */
public class MongoDAOTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    //TODO: WE NEED SET SECONDARIES SERVERS FOR THIS TEST.
    public void getIndexStatistics() throws Exception {
        MongoDAO mongoDAO = new MongoDAO("admin", "control123!", "admin", "localhost", 27018, "riot_main");
        List<IndexInformation> indexStatistics = mongoDAO.getIndexStatistics("things", (String[]) null);
        assertNotNull(indexStatistics);
        for (IndexInformation indexInformation : indexStatistics) {
            assertNotNull(indexInformation.getId());
            assertNull(indexInformation.getLastRunDate());
            assertNotNull(indexInformation.getStatus());
            assertNull(indexInformation.getStatsLog());
            assertNull(indexInformation.getCollectionName());
            assertNull(indexInformation.getCreationDate());
            assertNull(indexInformation.getLastRunDate());
        }
    }

    @Test
    //TODO: WE NEED SET SECONDARIES SERVERS FOR THIS TEST.
    public void getIndexStatisticsCase2() throws Exception {
        MongoDAO mongoDAO = new MongoDAO("admin", "control123!", "admin", "localhost", 27018, "riot_main");
        List<IndexInformation> indexStatistics = mongoDAO.getIndexStatistics("things", "_id_");
        assertNotNull(indexStatistics);
        assertEquals(1, indexStatistics.size());
        assertNotNull(indexStatistics.get(0).getId());
        assertNull(indexStatistics.get(0).getLastRunDate());
        assertNotNull(indexStatistics.get(0).getStatus());
        assertNull(indexStatistics.get(0).getStatsLog());
        assertNull(indexStatistics.get(0).getCollectionName());
        assertNull(indexStatistics.get(0).getCreationDate());
        assertNull(indexStatistics.get(0).getLastRunDate());
    }

}