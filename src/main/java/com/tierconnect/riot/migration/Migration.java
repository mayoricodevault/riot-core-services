package com.tierconnect.riot.migration;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.entities.MigrationStepResult;
import com.tierconnect.riot.appcore.entities.Version;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.servlet.SecurityFilter;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.appcore.version.CodeVersion;
import com.tierconnect.riot.appcore.utils.VersionUtils;
import com.tierconnect.riot.appcore.version.DBVersion;
import com.tierconnect.riot.iot.services.BrokerClientHelper;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.servlet.security.ApiKeyToken;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import com.tierconnect.riot.version.MigrationDefinition;
import joptsimple.internal.Strings;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by cvertiz on 01/11/17.
 */
public class Migration {
    static Logger logger = Logger.getLogger(Migration.class);

    public static Map<Integer, MigrationStepExecutor> getMigrationSteps(MigrationDefinition migrationDefinition) throws IOException {

        Map<Integer, MigrationStepExecutor> migrationSteps = new HashMap<>();
        for (MigrationStepExecutor migrationStepExecutor : migrationDefinition.getMigrationStepsDefinition()) {
            List<Integer> froms = migrationStepExecutor.getFromVersions();
            for (Integer from : froms) {
                migrationSteps.put(from, migrationStepExecutor);
            }
        }
        return migrationSteps;
    }

    public static void migrate(MigrationDefinition migrationDefinition) throws Exception {

        logger.info("**************************************************");
        logger.info("*************** Starting Migration ***************");
        logger.info("**************************************************");

        Map<Integer, MigrationStepExecutor> migrationSteps = getMigrationSteps(migrationDefinition);

        Map<Version, List<MigrationStepResult>> results = new HashMap<>();

        int targetVersion = CodeVersion.getInstance().getCodeVersion();
        int currentDBVersion = DBVersion.getInstance().getDbVersion();
        logger.info("Data version: " + VersionUtils.getAppVersionString(currentDBVersion) + ", Application version: " + VersionUtils.getAppVersionString(targetVersion));
        if (targetVersion != currentDBVersion) {
            logger.warn("Trying to Migrate from version: " +
                    DBVersion.getInstance().getDbVersionName() +
                    "(" + DBVersion.getInstance().getDbVersion() + ")" +
                    " to version: " +
                    CodeVersion.getInstance().getCodeVersionName() +
                    "(" + CodeVersion.getInstance().getCodeVersion() + ")");
        }else{
            logger.warn("Trying to migrate missing steps added after last migration or popdb on this installation (" + DBVersion.getInstance().getDbVersionName() + ")");
        }
        //Verify that the migration steps are set and can migrate current CodeVersion to targetVersion
        {
            int currentVersionCursor = currentDBVersion;
            while (currentVersionCursor != targetVersion) {
                MigrationStepExecutor migrationStepExecutor = migrationSteps.get(currentVersionCursor);
                if (migrationStepExecutor == null) {
                    logger.error(
                            "There is not compatible migration code for your database and this application version " +
                                    "currentDBVersion:" +
                                    currentDBVersion +
                                    " targetVersion:" +
                                    targetVersion);
                    throw new RuntimeException(
                            "There is not compatible migration code for your database and this application version " +
                                    "currentDBVersion:" +
                                    currentDBVersion +
                                    " targetVersion:" +
                                    targetVersion);
                }
                currentVersionCursor = migrationStepExecutor.getToVersion();
            }
            if(currentDBVersion == targetVersion
                    && (migrationDefinition.getMissingMigrationStepsDefinition() == null
                        || migrationDefinition.getMissingMigrationStepsDefinition().getSteps().isEmpty())){
                logger.warn(
                        "There are no missing migration step for current version " +
                                "currentDBVersion:" +
                                currentDBVersion +
                                " targetVersion:" +
                                targetVersion);
            }
        }
        //Execute SQL migration step by step, including NOT DESTRUCTIVE, CREATE, ALTER, ADD, INSERT, UPDATE for
        // Table, Columns, Constraints
        {
            int currentVersionCursor = currentDBVersion;
            while (currentVersionCursor != targetVersion) {
                MigrationStepExecutor migrationStepExecutor = migrationSteps.get(currentVersionCursor);
                if (migrationStepExecutor != null) {
                    logger.warn("Migrating SQL Before from: " +
                            migrationStepExecutor.getFromVersions() +
                            " to: " +
                            migrationStepExecutor.getToVersion());
                    try {
                        migrationStepExecutor.migrateSQLBefore();
                    } catch (Exception e) {
                        logger.error("Error on: " + migrationStepExecutor.getClass(), e);
                        throw e;
                    }
                    currentVersionCursor = migrationStepExecutor.getToVersion();
                }
            }
            //Migrate missing steps in current installation
            if(currentDBVersion == targetVersion
                    && migrationDefinition.getMissingMigrationStepsDefinition() != null
                    && !migrationDefinition.getMissingMigrationStepsDefinition().getSteps().isEmpty()){
                logger.warn("Migrating missing SQL Before steps from: " +
                        migrationDefinition.getMissingMigrationStepsDefinition().getFromVersions() +
                        " to: " +
                        migrationDefinition.getMissingMigrationStepsDefinition().getToVersion());
                try {
                    migrationDefinition.getMissingMigrationStepsDefinition().migrateSQLBefore();
                } catch (Exception e) {
                    logger.error("Error on: " + migrationDefinition.getMissingMigrationStepsDefinition().getClass(), e);
                    throw e;
                }
            }
        }
        //Initialize Hibernate
        Session session;
        try {
            session = HibernateSessionFactory.getInstance().getCurrentSession();
        } catch (Exception ex) {
            logger.error("Hibernate Session Error", ex);
            throw ex;
        }
        boolean errorOnHibernateMigration = false;
        Transaction transaction = session.getTransaction();
        {
            int currentVersionCursor = currentDBVersion;
            try {
                //Execute Hibernate Migration to UPDATE, INSERT, ALTER records on the DB's
                transaction.begin();
                SecurityFilter securityFilter = new SecurityFilter();
                //Please DO NOT DELETE the following line
                logger.warn("Initializing Shiro" + securityFilter.toString());
                RiotShiroRealm.initCaches();
                ApiKeyToken token = new ApiKeyToken(UserService.getInstance().getRootUser().getApiKey());
                PermissionsUtils.loginUser(token);

                List<MigrationStepResult> result;
                while (currentVersionCursor != targetVersion) {
                    TokenService.getInstance().deactivateAllBy(GroupService.getInstance().getRootGroup());
                    MigrationStepExecutor migrationStepExecutor = migrationSteps.get(currentVersionCursor);
                    if (migrationStepExecutor != null) {
                        logger.warn("Migrating Hibernate from: " +
                                migrationStepExecutor.getFromVersions() +
                                " to: " +
                                migrationStepExecutor.getToVersion());
                        try {
                            result = migrationStepExecutor.migrateHibernate();
                        } catch (Exception e) {
                            throw new RuntimeException("Error on: " + migrationStepExecutor.getClass(), e);
                        }
                        currentVersionCursor = migrationStepExecutor.getToVersion();
                        registerSuccessfulMigration(results, migrationStepExecutor, result, false);
                    }
                }
                //Migrate missing steps in current installation
                if(currentDBVersion == targetVersion
                        && migrationDefinition.getMissingMigrationStepsDefinition() != null
                        && !migrationDefinition.getMissingMigrationStepsDefinition().getSteps().isEmpty()){
                    logger.warn("Migrating missing Hibernate steps from: " +
                            migrationDefinition.getMissingMigrationStepsDefinition().getFromVersions() +
                            " to: " +
                            migrationDefinition.getMissingMigrationStepsDefinition().getToVersion());
                    try {
                        result = migrationDefinition.getMissingMigrationStepsDefinition().migrateHibernate();
                    } catch (Exception e) {
                        throw new RuntimeException("Error on: " + migrationDefinition.getMissingMigrationStepsDefinition().getClass(), e);
                    }
                    registerSuccessfulMigration(results, migrationDefinition.getMissingMigrationStepsDefinition(), result, true);
                }

                transaction.commit();
            } catch (Exception ex) { //Unsupported expression type
                errorOnHibernateMigration = true;
                logger.error(ex.getMessage(), ex);
                HibernateDAOUtils.rollback(transaction);
            }
        }


        //Execute DESTRUCTIVE SQL, AS DROP Table, Columns on the DB's onc we know everything went WELL
        if (!errorOnHibernateMigration) {
            int currentVersionCursor = currentDBVersion;
            while (currentVersionCursor != targetVersion) {
                MigrationStepExecutor migrationStepExecutor = migrationSteps.get(currentVersionCursor);
                if (migrationStepExecutor != null) {
                    logger.warn("Migrating SQL After from: " +
                            migrationStepExecutor.getFromVersions() +
                            " to: " +
                            migrationStepExecutor.getToVersion());
                    try {
                        migrationStepExecutor.migrateSQLAfter();
                    } catch (Exception e) {
                        logger.error("Error on: " + migrationStepExecutor.getClass(), e);
                    }
                    currentVersionCursor = migrationStepExecutor.getToVersion();
                } else {
                    logger.error("Migrating SQL After, migrationStepExecutor is null");
                    break;
                }
            }
            //Migrate missing steps in current installation
            if(currentDBVersion == targetVersion
                    && migrationDefinition.getMissingMigrationStepsDefinition() != null
                    && !migrationDefinition.getMissingMigrationStepsDefinition().getSteps().isEmpty()){
                logger.warn("Migrating missing SQL After steps from: " +
                        migrationDefinition.getMissingMigrationStepsDefinition().getFromVersions() +
                        " to: " +
                        migrationDefinition.getMissingMigrationStepsDefinition().getToVersion());
                try {
                    migrationDefinition.getMissingMigrationStepsDefinition().migrateSQLAfter();
                } catch (Exception e) {
                    logger.error("Error on: " + migrationDefinition.getMissingMigrationStepsDefinition().getClass(), e);
                }
            }
        }

        printMigrationSummary(results);

        logger.info("**************************************************");
        logger.info("*************** Migration Finished ***************");
        logger.info("**************************************************");

        //Cleanup migration tickles
        BrokerClientHelper.cleanMessagesQueue();

    }

    private static void printMigrationSummary(Map<Version, List<MigrationStepResult>> results) {
        if(!results.isEmpty()){
            logger.info("**************** Migration Summary ****************");
        }
        results.forEach((key, value) -> {
            logger.info("Migration from: [" + key.getPrevDbVersion() + "] to: [" + key.getDbVersion() + "]");
            value.forEach(step -> logger.info("\tStep: " + step.getMigrationPath()
                    + ", Result: " + step.getMigrationResult()
                    + ", Message: " + step.getMessage()
                    + ", StackTrace:" + (step.getStackTrace()!= null ? step.getStackTrace().split("\n")[0] + "..." : "")));
        });
    }

    public static void registerSuccessfulMigration(Map<Version, List<MigrationStepResult>> results, MigrationStepExecutor migrationStep, List<MigrationStepResult> result, boolean missingSteps) {
        int intVersion = migrationStep.getToVersion();
        Version version = new Version();
        version.setDbVersion(intVersion + "");
        version.setPrevDbVersion(Strings.join(Lists.newArrayList(migrationStep.getFromVersions().stream().map(Functions.toStringFunction()::apply).collect(Collectors.toList())), ","));
        String computerUser, computerName, computerIP;

        try {
            computerUser = System.getProperty("user.name");
            computerName = InetAddress.getLocalHost().getHostName();
            computerIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            computerUser = System.getProperty("user.name");
            computerName = "Host Name can not be resolved";
            computerIP = "Host IP can not be resolved";
        }

        version.setVersionName(migrationStep.getName());
        version.setVersionDesc(migrationStep.getMigrationDesc() + (missingSteps?" (Missing Steps Executed)":""));
        version.setGitBranch(Configuration.getProperty("git.branch"));

        version.setComputerUser(computerUser);
        version.setComputerName(computerName);
        version.setComputerIP(computerIP);
        version.setInstallTime(new Date());
        VersionService.getVersionDAO().insert(version);

        List<MigrationStepResult> migrationStepResults = new ArrayList<>();

        if(result.size() > 0){
            for(MigrationStepResult migrationStepResult : result){
                migrationStepResult.setVersion(version);

                MigrationStepResultService.getMigrationStepResultDAO().insert(migrationStepResult);

                migrationStepResults.add(migrationStepResult);
            }
        }

        results.put(version, migrationStepResults);
//        logger.warn("Successfully Migrated from: " + migrationStep.getFromVersions() + " to: " + migrationStep.getToVersion());
    }

}
