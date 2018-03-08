package com.tierconnect.riot.migration.steps.statusBar;

import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_StatusBar_VIZIX1098 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_StatusBar_VIZIX1098.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateStatusBar();
    }

    private void migrateStatusBar() {
        DBHelper dbHelper = new DBHelper();
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();

        Field field = PopDBUtils.popFieldService("notification_bulkProcess", "notification_backProcessReport", "Background Notification Time (Report secs)",
                rootGroup, "Background Process", "java.lang.Integer", null, true);
        PopDBUtils.popGroupField(rootGroup, field, "15");

        PopDBUtils.migrateFieldService("notification_importProcess", "notification_importProcess", "Background Notification Time (Import/Export secs)",
                rootGroup, "Background Process", "java.lang.Integer", null, true, "15");
        PopDBUtils.migrateFieldService("background_percentUpdate", "background_percentUpdate", "Background Update Percent (Import/Export %)",
                rootGroup, "Background Process", "java.lang.Integer", null, true, "5");

        Connection connection = DBHelper.getConnection();
        Statement statement = null;
        ResultSet resultSet = null;
        String query = "select * from reportbulkprocess";

        try{
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);

            while (resultSet.next()){
                BackgroundProcess backgroundProcess = new BackgroundProcess();
                if (dbHelper.existColumn("reportbulkprocess","checked")) {
                    backgroundProcess.setChecked(resultSet.getBoolean("checked"));
                }else{
                    backgroundProcess.setChecked(true);
                }
                backgroundProcess.setEndDate(resultSet.getDate("endDate"));
                backgroundProcess.setIniDate(resultSet.getDate("iniDate"));
                backgroundProcess.setProcessTime(resultSet.getLong("processTime"));
                if (dbHelper.existColumn("reportbulkprocess","progress")) {
                    backgroundProcess.setProgress(resultSet.getInt("progress"));
                }else{
                    backgroundProcess.setProgress(100);
                }
                backgroundProcess.setStatus(resultSet.getString("status"));
                if (dbHelper.existColumn("reportbulkprocess","threadName")) {
                    backgroundProcess.setThreadName(resultSet.getString("threadName"));
                }
                backgroundProcess.setTotalAffectedRecords(resultSet.getLong("totalAffectedRecords"));
                backgroundProcess.setTotalOmittedRecords(resultSet.getLong("totalOmittedRecords"));
                backgroundProcess.setTotalRecords(resultSet.getLong("totalRecords"));
                backgroundProcess.setTypeProcess(resultSet.getString("typeProcess"));
                User user = UserService.getInstance().get(resultSet.getLong("createdByUser_id"));
                backgroundProcess.setCreatedByUser(user);
                backgroundProcess = BackgroundProcessService.getInstance().insert(backgroundProcess);
                BackgroundProcessEntity backgroundProcessEntity = new BackgroundProcessEntity();
                backgroundProcessEntity.setColumnName("reportDefinitionId");
                backgroundProcessEntity.setModuleName("reports");
                backgroundProcessEntity.setColumnValue(String.valueOf(resultSet.getLong("reportDefinition_id")));
                backgroundProcessEntity.setBackgroundProcess(backgroundProcess);
                BackgroundProcessEntityService.getInstance().insert(backgroundProcessEntity);
            }
            query = "select * from reportbulkprocessdetail";
            resultSet = statement.executeQuery(query);

            while (resultSet.next()){
                BackgroundProcessDetail backgroundProcessDetail = new BackgroundProcessDetail();
                backgroundProcessDetail.setEndDate(resultSet.getDate("endDate"));
                backgroundProcessDetail.setIniDate(resultSet.getDate("iniDate"));
                backgroundProcessDetail.setProcessTime(resultSet.getLong("processTime"));
                backgroundProcessDetail.setQuery(resultSet.getString("query"));
                backgroundProcessDetail.setStatus(resultSet.getString("status"));
                backgroundProcessDetail.setTotalAffectedRecords(resultSet.getLong("totalAffectedRecords"));
                backgroundProcessDetail.setTotalOmittedRecords(resultSet.getLong("totalOmittedRecords"));
                backgroundProcessDetail.setTotalRecords(resultSet.getLong("totalRecords"));
                backgroundProcessDetail.setValuesToChange(resultSet.getString("valuesToChange"));
                ThingType thingType = ThingTypeService.getInstance().get(resultSet.getLong("thingType_id"));
                backgroundProcessDetail.setThingType(thingType);
                BackgroundProcess backgroundProcess = BackgroundProcessService.getInstance().get(resultSet.getLong("reportBulkProcess_id"));
                backgroundProcessDetail.setBackgroundProcess(backgroundProcess);
                BackgroundProcessDetailService.getInstance().insert(backgroundProcessDetail);
            }

            query = "select * from reportbulkprocessdetaillog";
            resultSet = statement.executeQuery(query);

            while (resultSet.next()){
                BackgroundProcessDetailLog backgroundProcessDetailLog = new BackgroundProcessDetailLog();
                backgroundProcessDetailLog.setStatus(resultSet.getString("status"));
                backgroundProcessDetailLog.setThingId(resultSet.getLong("thingId"));
                if (dbHelper.existColumn("reportbulkprocessdetaillog","serialNumber")) {
                    backgroundProcessDetailLog.setSerialNumber(resultSet.getString("serialNumber"));
                }
                if (dbHelper.existColumn("reportbulkprocessdetaillog","thingTypeCode")) {
                    backgroundProcessDetailLog.setThingTypeCode(resultSet.getString("thingTypeCode"));
                }
                BackgroundProcessDetail backgroundProcessDetail = BackgroundProcessDetailService.getInstance().get(resultSet.getLong("backgroundProcessDetail_id"));
                backgroundProcessDetailLog.setBackgroundProcessDetail(backgroundProcessDetail);
                BackgroundProcessDetailLogService.getInstance().insert(backgroundProcessDetailLog);
            }

        }catch (SQLException e){
            logger.warn(e.getMessage());
        }finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
