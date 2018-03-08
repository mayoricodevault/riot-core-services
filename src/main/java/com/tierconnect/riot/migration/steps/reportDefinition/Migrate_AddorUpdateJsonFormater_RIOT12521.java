package com.tierconnect.riot.migration.steps.reportDefinition;

import com.tierconnect.riot.iot.dao.mongo.MongoScriptDAO;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.net.URL;
import java.nio.charset.Charset;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_AddorUpdateJsonFormater_RIOT12521 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_AddorUpdateJsonFormater_RIOT12521.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFeature();
    }

    private void migrateFeature() {
        //TODO rename this function and put your code here
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        try {
            URL fileURL = this.getClass().getClassLoader().getResource("mongo/JsonFormatter.js");
            if (fileURL != null) {
                String text = IOUtils.toString(fileURL, Charset.forName("UTF-8"));
                MongoScriptDAO.getInstance().insertRaw("JSONFormatter", text);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
