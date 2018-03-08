package com.tierconnect.riot.migration.older;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.ReportEntryOptionProperty;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.iot.services.ReportEntryOptionPropertyService;
import com.tierconnect.riot.migration.DBHelper;

import java.util.*;

/**
 * Created by fflores on 10/30/15.
 */
@Deprecated
public class V_030302_030303 implements MigrationStepOld
{
    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(30302);
    }

    @Override
    public int getToVersion() {
        return 30303;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        DBHelper dbHelper = new DBHelper();
        String databaseType = dbHelper.getDataBaseType();
        dbHelper.executeSQLFile("sql/" + databaseType + "/V030302_to_030303.sql");
    }

    @Override
    public void migrateHibernate() throws Exception {

        List<ReportEntryOptionProperty> reportEntryOptionPropertyList = ReportEntryOptionPropertyService.getReportEntryOptionPropertyDAO().selectAll();
        if (null != reportEntryOptionPropertyList){
            for (ReportEntryOptionProperty reportEntryOptionProperty : reportEntryOptionPropertyList){
                reportEntryOptionProperty.setAllPropertyData(Boolean.FALSE);
            }
        }
        migrateSequenceNumberLogging();
    }

    @Override
    public void migrateSQLAfter() throws Exception {

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
}

