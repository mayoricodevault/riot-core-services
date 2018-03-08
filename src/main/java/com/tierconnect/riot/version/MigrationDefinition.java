package com.tierconnect.riot.version;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.MigrationStepResult;
import com.tierconnect.riot.appcore.entities.MigrationStepResultBase;
import com.tierconnect.riot.appcore.version.CodeVersion;
import com.tierconnect.riot.appcore.version.DBVersion;
import com.tierconnect.riot.migration.MigrationStepExecutor;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by cvertiz on 2/23/15.
 */
public class MigrationDefinition {
    static Logger logger = Logger.getLogger(MigrationDefinition.class);

    private List<MigrationStepExecutor> migrationStepsDefinition = new ArrayList<>();

    private MigrationStepExecutor missingMigrationStepsDefinition;

    private static MigrationDefinition INSTANCE = new MigrationDefinition();

    public static MigrationDefinition getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MigrationDefinition();
        }
        return INSTANCE;
    }

    public void init() throws IOException {
        URL resource = MigrationStepExecutor.class.getClassLoader().getResource("migration/migrationSteps.json");

        Integer currentCodeVersion = 0;
        String currentCodeVersionName = "NO-VERSION";

        if (resource != null) {
            String path = URLDecoder.decode(resource.getPath(), "utf-8");
            File file = new File(path);
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            int readResult = fis.read(data);
            fis.close();
            if (readResult != -1) {
                String input = new String(data, "UTF-8");
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> definition = objectMapper.readValue(input, (new HashMap<String, Object>()).getClass());
                currentCodeVersion = (Integer) definition.get("codeVersionNumber");
                currentCodeVersionName = (String) definition.get("codeVersionName");
                //noinspection unchecked
                for (Map<String, Object> entry : (List<Map<String, Object>>) definition.get("steps")) {
                    //noinspection unchecked
                    migrationStepsDefinition.add(
                            new MigrationStepExecutor(
                                    (List<Integer>) entry.get("from"),
                                    (Integer) entry.get("to"),
                                    (String) entry.get("name"),
                                    (List<Map<String, Object>>) entry.get("migrate"),
                                    (String) entry.get("description")
                            )
                    );
                }
            }
        }

        CodeVersion.getInstance().setCodeVersion(currentCodeVersion);
        CodeVersion.getInstance().setCodeVersionName(currentCodeVersionName);

        DBVersionHelper dbVersion = DBVersionHelper.getCurrentVersion();
        //TODO: REMOVE THIS WHEN MNS IS MIGRATING TO LATEST VERSION
        int versionNumber = dbVersion.getVersionNumber();
        if (versionNumber == 40300) {
            versionNumber = 4030001;
        }
        DBVersion.getInstance().setDbVersion(versionNumber);
        DBVersion.getInstance().setDbVersionName(dbVersion.getVersionName());

        //If code version is equals to database version, try to migrate missing steps
        if(CodeVersion.getInstance().getCodeVersion() == DBVersion.getInstance().getDbVersion()){

            List<MigrationStepResult> migrationStepResults = DBVersionHelper.getLastVersionMigrationResult(dbVersion.getVersionNumber());

            final List<String> allStepResults = migrationStepResults
                    .stream()
                    .map(MigrationStepResultBase::getMigrationPath)
                    .collect(Collectors.toList());

//        String filePAth = MigrationDefinition.class.getClassLoader().getResource("").getPath()
//                + "com" + File.separator
//                + "tierconnect" + File.separator
//                + "riot" + File.separator
//                + "migration" + File.separator
//                + "steps"
//                + File.separator + "%1s.class";

            //Commented because migration of modified or fixed steps moved to future improvement
//        final List<String> migrationStepResultsToMigrate = migrationStepResults
//                .stream()
//                .filter(item -> {
//                    String hash = null;
//                    try {
//                        hash = Files.hash(new File(String.format(filePAth, item.getMigrationPath())), Hashing.crc32()).toString();
//                    } catch (IOException e) {
//                        logger.error(e.getMessage(), e);
//                    }
//                    return hash != null && !hash.equals(item.getHash());
//                })
//                .map(MigrationStepResultBase::getMigrationPath)
//                .collect(Collectors.toList());


            MigrationStepExecutor currentMigrationStepExecutor = migrationStepsDefinition
                    .stream()
                    .filter(migrationStepExecutor -> migrationStepExecutor.getToVersion() == CodeVersion.getInstance().getCodeVersion())
                    .findFirst()
                    .get();

            missingMigrationStepsDefinition = new MigrationStepExecutor(
                    currentMigrationStepExecutor.getFromVersions(),
                    currentMigrationStepExecutor.getToVersion(),
                    currentMigrationStepExecutor.getName(),
                    currentMigrationStepExecutor.getSteps()
                            .stream()
//                        .filter(step -> migrationStepResultsToMigrate.contains((String)step.get("path")) || !allStepResults.contains((String)step.get("path")))
                            .filter(step -> !allStepResults.contains((String)step.get("path")))
                            .collect(Collectors.toList()),
                    currentMigrationStepExecutor.getMigrationDesc()
            );
        }

    }

    public List<MigrationStepExecutor> getMigrationStepsDefinition() {
        return migrationStepsDefinition;
    }

    public MigrationStepExecutor getMissingMigrationStepsDefinition() {
        return missingMigrationStepsDefinition;
    }
}
