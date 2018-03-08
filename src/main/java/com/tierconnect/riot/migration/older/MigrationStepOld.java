package com.tierconnect.riot.migration.older;

import java.util.List;

/**
 * Created by agutierrez on 2/23/15.
 * A interface to Implement new step migration.
 */
@Deprecated
public interface MigrationStepOld {

     List<Integer> getFromVersions();

     int getToVersion();

    //Code to Execute before Hibernate Starts Up to do alter tables, rename tables, drop tables, create tables
     void migrateSQLBefore() throws Exception;

    //Code to Execute on an Hibernate Transaction to do update of rows, creation of rows
     void migrateHibernate() throws Exception;

    //Code to Execute after Hibernate has started to do cleaning up, remove columns and tables
     void migrateSQLAfter() throws Exception;

}
