package com.tierconnect.riot.api.mongoShell.operations;

import com.tierconnect.riot.api.mongoShell.MongoShellClient;

import com.tierconnect.riot.api.mongoShell.testUtils.MongoDataBaseTestUtils;
import com.tierconnect.riot.api.mongoShell.testUtils.PropertiesReaderUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;

import static com.tierconnect.riot.api.mongoShell.operations.ExplainShellVerbosity.*;
import static org.junit.Assert.*;

/**
 * Created by achambi on 4/27/17.
 * Unit Test For Find Operation.
 */
public class FindOperationTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    @Ignore
    //FIXME Fix The test changing the find Logic from QueryBuilder to FindOperation class.
    public void execute() throws Exception {
        MongoDataBaseTestUtils.prepareDataBase();
        PropertiesReaderUtil.setConfigurationFile("propertiesMongoShellLocalHostTest.properties");
        MongoShellClient mockMongoShellClient = Mockito.spy(new MongoShellClient());
        FindOperation findOperation = new FindOperation("things", mockMongoShellClient.getExecutor(), "{}", "");
        Map<String, Object> resultQuery = findOperation.explain(ALL_PLANS_EXECUTIONS).execute();
        assertNotNull(resultQuery);
    }
}