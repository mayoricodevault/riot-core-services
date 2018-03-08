package com.tierconnect.riot.migration.steps.folder;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.iot.entities.Folder;
import com.tierconnect.riot.iot.entities.QFolder;
import com.tierconnect.riot.iot.services.FolderService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_FolderAddColumn_VIZIX7442 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_FolderAddColumn_VIZIX7442.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFeature();
    }

    private void migrateFeature() {
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QFolder.folder.createdByUser.isNull());
        List<Folder> folderList = FolderService.getInstance().listPaginated(be, null, null);
        for (Folder folder:folderList){
            folder.setCreatedByUser(UserService.getInstance().getRootUser());
            FolderService.getInstance().update(folder);
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
