package com.tierconnect.riot.migration.steps.sequence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_SequenceNumberLogging_RIOT8326 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_SequenceNumberLogging_RIOT8326.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateSequenceNumberLogging();
    }

    private void migrateSequenceNumberLogging()
    {
        Edgebox edgebox = EdgeboxService.getInstance().selectByCode( "MCB" );
        String configurationMCB = edgebox.getConfiguration();
        if ( !configurationMCB.contains( "sequenceNumberLogging" ) )
        {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Map<String, Object>> map;
            Map<String, Object> mapSequence = new LinkedHashMap<>();

            try
            {
                map = mapper.readValue(configurationMCB, Map.class);
                mapSequence.put( "active", 0 );
                mapSequence.put( "TTL", 86400 );
                mapSequence.put( "GC_GRACE_SECONDS", 0 );
                map.put( "sequenceNumberLogging", mapSequence );
                String json = mapper.writeValueAsString(map);
                edgebox.setConfiguration( json );
                EdgeboxService.getInstance().update( edgebox );
            }
            catch( Exception e )
            {
                // do nothing
            }
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
