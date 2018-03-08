package com.tierconnect.riot.api.mongoShell.operations;

import com.tierconnect.riot.api.mongoShell.MongoShellClientOption;
import com.tierconnect.riot.api.mongoShell.ReadShellPreference;
import com.tierconnect.riot.api.mongoShell.ServerStringFormat;
import com.tierconnect.riot.api.mongoShell.connections.SingleServerShellServerCluster;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * Created by achambi on 2/10/17.
 */
public class MongoURIBuilderTest {

    @Test
    public void buildMongoURI() throws Exception {
        MongoShellClientOption mongoShellClientOption = Mockito.mock(MongoShellClientOption.class);
        Mockito.when(mongoShellClientOption.getSslEnabled()).thenReturn(false);
        Mockito.when(mongoShellClientOption.getConnectTimeout()).thenReturn(100);
        Mockito.when(mongoShellClientOption.getMaxPoolSize()).thenReturn(10);
        Mockito.when(mongoShellClientOption.getReadPreference()).thenReturn(ReadShellPreference.primary());
        SingleServerShellServerCluster singleServerShellServerCluster = Mockito.mock(SingleServerShellServerCluster
                .class);
        Mockito.when(singleServerShellServerCluster.getShellAddress(ServerStringFormat.URI)).thenReturn
                ("127.0.0.1:27017/riot_main_test");
        String URIString = MongoURIBuilder.buildMongoURI("userName", "hello", "admin", mongoShellClientOption,
                singleServerShellServerCluster, "riot_main_test");
        assertEquals("mongodb://userName:hello" +
                "@127.0.0.1:27017/riot_main_test/riot_main_test?authSource=admin&ssl=false&connectTimeoutMS=100" +
                "&maxPoolSize=10&readPreference=db.getMongo().setReadPref('primary');", URIString);
    }

    @Test
    public void buildMongoURICommand() throws Exception {
        MongoShellClientOption mongoShellClientOption = Mockito.mock(MongoShellClientOption.class);
        Mockito.when(mongoShellClientOption.getSslEnabled()).thenReturn(false);
        Mockito.when(mongoShellClientOption.getConnectTimeout()).thenReturn(100);
        Mockito.when(mongoShellClientOption.getMaxPoolSize()).thenReturn(10);
        Mockito.when(mongoShellClientOption.getReadPreference()).thenReturn(ReadShellPreference.primary());
        SingleServerShellServerCluster singleServerShellServerCluster = Mockito.mock(SingleServerShellServerCluster
                .class);
        Mockito.when(singleServerShellServerCluster.getShellAddress(ServerStringFormat.URI)).thenReturn
                ("127.0.0.1:27017/riot_main_test");
        String URIString = MongoURIBuilder.buildMongoURICommand("userName", "(jIJ9Iy\")@7'.?'", "admin",
                mongoShellClientOption, singleServerShellServerCluster);
        assertEquals("mongo \"mongodb://127.0.0.1:27017/riot_main_test?readPreference=primary&connectTimeoutMS=100" +
                        "&maxPoolSize=10\" --authenticationDatabase=admin --quiet --username=userName --password=\"" +
                        "(jIJ9Iy\\\")@7\\'.?\\'\"",
                URIString);
    }

    @Test
    public void buildMongoURICommandWithReplicaSet() throws Exception {
        MongoShellClientOption mongoShellClientOption = Mockito.mock(MongoShellClientOption.class);
        Mockito.when(mongoShellClientOption.getSslEnabled()).thenReturn(false);
        Mockito.when(mongoShellClientOption.getRequiredReplicaSetName()).thenReturn("replicaSetName");
        Mockito.when(mongoShellClientOption.getConnectTimeout()).thenReturn(100);
        Mockito.when(mongoShellClientOption.getMaxPoolSize()).thenReturn(10);
        Mockito.when(mongoShellClientOption.getReadPreference()).thenReturn(ReadShellPreference.primary());
        SingleServerShellServerCluster singleServerShellServerCluster = Mockito.mock(SingleServerShellServerCluster
                .class);
        Mockito.when(singleServerShellServerCluster.getShellAddress(ServerStringFormat.URI)).thenReturn
                ("127.0.0.1:27017/riot_main_test");
        String URIString = MongoURIBuilder.buildMongoURICommand("userName", "(jIJ9Iy\")@7'.?'", "admin",
                mongoShellClientOption, singleServerShellServerCluster);
        assertEquals("mongo \"mongodb://127.0.0.1:27017/riot_main_test?replicaSet=replicaSetName&readPreference" +
                        "=primary&connectTimeoutMS=100&maxPoolSize=10\" --authenticationDatabase=admin --quiet " +
                        "--username=userName --password=\"(jIJ9Iy\\\")@7\\'.?\\'\"",
                URIString);
    }
}