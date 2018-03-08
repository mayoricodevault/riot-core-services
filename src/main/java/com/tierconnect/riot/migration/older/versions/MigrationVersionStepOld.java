package com.tierconnect.riot.migration.older.versions;

import com.tierconnect.riot.appcore.entities.Version;
import com.tierconnect.riot.appcore.services.VersionService;
import com.tierconnect.riot.migration.older.MigrationStepOld;
import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Created by rsejas on 9/1/16.
 */
@Deprecated
public abstract class MigrationVersionStepOld {
    private Logger logger = Logger.getLogger(MigrationVersionStepOld.class);
    Map<Integer, MigrationStepOld> migrationSteps = new HashMap<>();
    public List<Integer> getFromVersions() {
        return new ArrayList<>();
    }

    public Integer getToVersion() {
        return 0;
    }

    public void fillMigrationSteps() {
    }

    public void migrateReleaseCandidatesSQLBefore() throws Exception {}
    public void migrateReleaseCandidatesSQLHibernate() throws Exception {}
    public void migrateReleaseCandidatesSQLAfter() throws Exception {}

    public void addMigrationStep(MigrationStepOld migrationStep) {
        List<Integer> froms = migrationStep.getFromVersions();
        for (Integer from : froms) {
            migrationSteps.put(from, migrationStep);
        }
    }

    public void migrateByDefaultSQLBefore(int fromReleaseCandidate, int toReleaseCandidate) throws Exception {
        int tmpCurrentVersion = fromReleaseCandidate;
        while (tmpCurrentVersion != toReleaseCandidate) {
            logger.info("Migration SQLBefore, trying to migrate from ["+fromReleaseCandidate+"] to [" + toReleaseCandidate + "].");
            MigrationStepOld migrationStep = migrationSteps.get(tmpCurrentVersion);
            if (migrationStep != null) {
                migrationStep.migrateSQLBefore();
                tmpCurrentVersion = migrationStep.getToVersion();
            } else {
                throw new Exception("Migration Step not found [" + tmpCurrentVersion + "].");
            }
        }
    }

    public void migrateByDefaultSQLHibernate(int fromReleaseCandidate, int toReleaseCandidate) throws Exception {
        int tmpCurrentVersion = fromReleaseCandidate;
        while (tmpCurrentVersion != toReleaseCandidate) {
            logger.info("Migration SQLHibernate, trying to migrate from ["+fromReleaseCandidate+"] to [" + toReleaseCandidate + "].");
            MigrationStepOld migrationStep = migrationSteps.get(tmpCurrentVersion);
            if (migrationStep != null) {
                migrationStep.migrateHibernate();
                registerSuccessfulMigration(migrationStep);
                tmpCurrentVersion = migrationStep.getToVersion();
            } else {
                throw new Exception("Migration Step not found [" + tmpCurrentVersion + "].");
            }
        }
    }

    public void migrateByDefaultSQLAfter(int fromReleaseCandidate, int toReleaseCandidate) throws Exception {
        int tmpCurrentVersion = fromReleaseCandidate;
        while (tmpCurrentVersion != toReleaseCandidate) {
            logger.info("Migration SQLAfter, trying to migrate from ["+fromReleaseCandidate+"] to [" + toReleaseCandidate + "].");
            MigrationStepOld migrationStep = migrationSteps.get(tmpCurrentVersion);
            if (migrationStep != null) {
                migrationStep.migrateSQLAfter();
                tmpCurrentVersion = migrationStep.getToVersion();
            } else {
                throw new Exception("Migration Step not found [" + tmpCurrentVersion + "].");
            }
        }
    }

    public void registerSuccessfulMigration(MigrationStepOld migrationStep) {
        logger.warn("Successfully Migrated from: " + migrationStep.getFromVersions() + " to: " + migrationStep.getToVersion());
        int intVersion = migrationStep.getToVersion();
        Version version = new Version();
        version.setDbVersion(intVersion+"");
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

        version.setComputerUser(computerUser);
        version.setComputerName(computerName);
        version.setComputerIP(computerIP);
        version.setInstallTime(new Date());
        VersionService.getInstance().getVersionDAO().insert(version);
    }

    public Boolean hasReleaseCandidates() {
        return migrationSteps.size() > 1;
    }
}
