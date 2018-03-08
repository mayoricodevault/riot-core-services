package com.tierconnect.riot.api.mongoShell;

import com.tierconnect.riot.api.mongoShell.operations.AggregateOperation;
import com.tierconnect.riot.api.mongoShell.operations.FindOperation;
import com.tierconnect.riot.api.mongoShell.operations.OperationExecutor;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Created by achambi on 2/2/17.
 * Class to test mongo collection test.
 */
public class MongoCollectionTest {
    @Test
    public void getExecutor() throws Exception {
        OperationExecutor expectExecutor = Mockito.mock(OperationExecutor.class);
        MongoDataBase mockDataBase = Mockito.spy(new MongoDataBase(expectExecutor));
        MongoCollection result = mockDataBase.getCollection("test");
        OperationExecutor resultExecutor = result.getExecutor();
        assertEquals(expectExecutor, resultExecutor);
        assertEquals(expectExecutor.hashCode(), resultExecutor.hashCode());
    }

    @Test
    public void aggregate() throws Exception {
        OperationExecutor expectExecutor = Mockito.mock(OperationExecutor.class);
        MongoCollection mongoCollection = new MongoCollection("collectionName", expectExecutor);
        AggregateOperation expectedOperation = new AggregateOperation("collectionName",
                expectExecutor,
                "");
        AggregateOperation resultOperation = mongoCollection.aggregate("");
        assertSame("Expected the result and the mock object are the same instance.",
                expectExecutor,
                resultOperation.getOperationExecutor());
        assertThat("Expected the result and the mock object are the same values.",
                expectExecutor,
                is(resultOperation.getOperationExecutor()));
        assertEquals(expectExecutor, resultOperation.getOperationExecutor());
        assertEquals(expectedOperation.getCollection(), resultOperation.getCollection());
        assertEquals(expectedOperation.isAllowDiskUse(), resultOperation.isAllowDiskUse());
        assertEquals(expectedOperation.getOperationExecutor(), resultOperation.getOperationExecutor());
    }

    @Test
    @Ignore
    public void find() {
        OperationExecutor expectExecutor = Mockito.mock(OperationExecutor.class);
        MongoCollection mongoCollection = new MongoCollection("collectionName", expectExecutor);
        FindOperation expectOperation = new FindOperation("collectionName", expectExecutor, "", "");
        FindOperation resultOperation = mongoCollection.find("", "");
        assertSame("Expected the result and the mock object are the same instance.",
                expectExecutor,
                resultOperation.getOperationExecutor());
        assertThat("Expected the result and the mock object are the same values.",
                expectExecutor,
                is(resultOperation.getOperationExecutor()));
        assertEquals(expectExecutor, resultOperation.getOperationExecutor());
        assertEquals(expectOperation.getCollection(), resultOperation.getCollection());
        assertEquals(expectOperation.getFields(), resultOperation.getFields());
        assertEquals(expectOperation.getQuery(), resultOperation.getQuery());
    }
}