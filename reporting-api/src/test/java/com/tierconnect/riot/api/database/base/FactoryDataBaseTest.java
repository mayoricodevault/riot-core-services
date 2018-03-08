package com.tierconnect.riot.api.database.base;

import com.tierconnect.riot.api.assertions.Assertions;
import com.tierconnect.riot.api.configuration.PropertyReader;
import com.tierconnect.riot.api.database.base.conditions.ConditionBuilder;
import com.tierconnect.riot.api.database.mongo.Mongo;
import com.tierconnect.riot.api.database.mongoDrive.MongoClientFactory;
import com.tierconnect.riot.api.database.mongoDrive.MongoDriver;
import com.tierconnect.riot.api.database.sql.SQL;
import com.tierconnect.riot.api.mongoShell.parsers.MongoParser;
import com.tierconnect.riot.api.mongoShell.parsers.ResultParser;
import com.tierconnect.riot.api.mongoShell.testUtils.PropertiesReaderUtil;
import com.tierconnect.riot.api.mongoShell.utils.CharacterUtils;
import com.tierconnect.riot.api.mongoShell.utils.FileUtils;
import com.tierconnect.riot.api.mongoShell.utils.ShellCommand;
import com.tierconnect.riot.api.mongoTransform.BsonToMap;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.hibernate.dialect.SQLServer2005Dialect;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.sql.SQLData;

import static junitparams.JUnitParamsRunner.$;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by vealaro on 12/13/16.
 */
@RunWith(JUnitParamsRunner.class)
public class FactoryDataBaseTest {

    @Before
    public void setUp() throws Exception {
        PropertiesReaderUtil.setConfigurationFile("propertiesMongoShellLocalHost.properties");
    }

    @Test
    @Parameters(method = "listClass")
    public void testValidatesConstructorClass(Class<?> clazz) throws Exception {
        assert clazz != null;
        Constructor constructor = clazz.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    public void testGetINSTANCE() {
        Mongo mongo = FactoryDataBase.get(Mongo.class, new ConditionBuilder());
        assertNotNull(mongo);
        SQL sql = FactoryDataBase.get(SQL.class, new ConditionBuilder());
        assertNotNull(sql);
        MongoDriver mongoDriver = FactoryDataBase.get(MongoDriver.class, new ConditionBuilder());
        assertNotNull(mongoDriver);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetINSTANCEError1() {
        FactoryDataBase.get(com.mongodb.Mongo.class, new ConditionBuilder());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetINSTANCEError2() {
        FactoryDataBase.get(SQLData.class, new ConditionBuilder());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetINSTANCEError3() {
        FactoryDataBase.get(SQLServer2005Dialect.class, new ConditionBuilder());
    }

    private Object[] listClass() {
        return $(
                FactoryDataBase.class, Operation.class, Assertions.class, ResultParser.class,
                PropertyReader.class, MongoClientFactory.class, MongoParser.class, FileUtils.class,
                CharacterUtils.class, BsonToMap.class
        );
    }
}