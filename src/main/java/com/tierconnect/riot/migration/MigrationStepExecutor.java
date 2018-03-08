package com.tierconnect.riot.migration;

import com.tierconnect.riot.appcore.entities.MigrationStepResult;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by cvertiz on 01/09/17.
 * Executes a step migration set up from definition file.
 */
public class MigrationStepExecutor {

    static Logger logger = Logger.getLogger(MigrationStepExecutor.class);

    private List<Integer> from = new ArrayList<>();
    private int to;
    private String name;
    private List<Map<String, Object>> steps;
    private String migrationDesc;

    private PriorityQueue<MigrationStep> stepInstances = new PriorityQueue<>();
//    private Map<String, String> packageToPath = new HashMap<>();

    public MigrationStepExecutor(List<Integer> from, int to, String name, List<Map<String, Object>> steps, String migrationDesc) {
        this.from = from;
        this.to = to;
        this.name = name;
        this.steps = steps;
        this.migrationDesc = migrationDesc;

        for (Map<String, Object> step : this.steps) {
            try {
                stepInstances.offer(
                        new MigrationStep(
                                getStepOrder(step, this.steps),
                                getStepPath(step),
                                getStepDescription(step),
                                Class.forName("com.tierconnect.riot.migration.steps." + getStepPackage(step)).newInstance())
                );
//                stepInstances.put(getStepPath(step), Class.forName("com.tierconnect.riot.migration.steps." + getStepPackage(step)).newInstance());
//                packageToPath.put("com.tierconnect.riot.migration.steps." + getStepPackage(step), getStepPath(step));
            } catch (Exception e) {
                logger.error("Unable to create instance in MigrationStepExecutor for " + getStepPackage(step), e.getCause());
            }
        }
    }

    public List<Integer> getFromVersions() {
        return from;
    }

    public int getToVersion() {
        return to;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Map<String, Object>> getSteps() {
        return steps;
    }

    public void setSteps(List<Map<String, Object>> steps) {
        this.steps = steps;
    }

    public String getMigrationDesc() {
        return migrationDesc;
    }

    //Code to Execute before Hibernate Starts Up to do alter tables, rename tables, drop tables, create tables
    public void migrateSQLBefore() throws Exception {

        for (MigrationStep step : stepInstances) {
            String databaseType = DBHelper.getDataBaseType();
            String scriptPath = "migration/sql/" + databaseType.toLowerCase() + "/before/" + step.getPath() + ".sql";
            try {
                step.getClazz().getMethod("migrateSQLBefore", String.class).invoke(step.getStepInstance(), scriptPath);
                //logger.warn("Result migrating " + step.getKey() + " migration step in migrateSQLBefore " + obj);
            } catch (Exception e) {
                logger.error("ERROR migrating " + step.getPath() + " migration step in migrateSQLBefore" , e);
            }
        }
    }


    //Code to Execute on an Hibernate Transaction to do update of rows, creation of rows
    public List<MigrationStepResult> migrateHibernate() throws Exception {
        List<MigrationStepResult> resultSteps = new ArrayList<>();
        for (MigrationStep step : stepInstances) {

            //Commented because migration of modified or fixed steps moved to future improvement
            //Using crc32 because its faster then other checksum
//            HashCode hash = Files.hash(new File(step.getPath()), Hashing.crc32());
//            String fileHash = hash.toString();
//            resultStep.setHash(fileHash);

            MigrationStepResult resultStep = new MigrationStepResult();
            resultStep.setMigrationPath(step.getPath());
            resultStep.setDescription(step.getDescription());
//            resultStep.setHash(step.getHash());
            try {
                step.getClazz().getMethod("migrateHibernate").invoke(step.getStepInstance());
                resultStep.setMigrationResult("SUCCEED");
                resultStep.setMessage(step.getPath() + " migration step succeed");
            } catch (Exception e) {
                resultStep.setMigrationResult("ERROR");
                resultStep.setMessage(e.getCause().getMessage());
                resultStep.setStackTrace(stackTraceToString(e.getCause(), 4000));
            }
            resultSteps.add(resultStep);
        }
        return resultSteps;
    }

    //Code to Execute after Hibernate has started to do cleaning up, remove columns and tables
    public void migrateSQLAfter() throws Exception {
        for (MigrationStep step : stepInstances) {
            try {
                String databaseType = DBHelper.getDataBaseType();
                String scriptPath = "migration/sql/" + databaseType.toLowerCase() + "/after/" + step.getPath() + ".sql";
                Object obj = step.getClazz().getMethod("migrateSQLAfter", String.class).invoke(step.getStepInstance(), scriptPath);
                //logger.warn("Result migrating " + step.getKey() + " migration step in migrateSQLAfter" + obj);
            } catch (Exception e) {
                logger.error("ERROR migrating " + step.getPath() + " migration step in migrateSQLAfter" , e);
            }
        }
    }

    private String getStepPackage(Map<String, Object > stepName){
        return ((String)stepName.get("path")).replaceAll("/", ".");
    }

    private String getStepPath(Map<String, Object > stepName){
        return (String)stepName.get("path");
    }

    private Integer getStepOrder(Map<String, Object> stepName, List<Map<String, Object>> steps){
        return steps.indexOf(stepName);
    }

    private String getStepDescription(Map<String, Object > stepName){
        return (String) stepName.get("description");
    }

    public String stackTraceToString(Throwable e, int cutoff) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString());
            sb.append("\n");
        }
        return sb.toString().substring(0, sb.toString().length() >= cutoff ? cutoff : sb.toString().length() - 1);
    }
}
