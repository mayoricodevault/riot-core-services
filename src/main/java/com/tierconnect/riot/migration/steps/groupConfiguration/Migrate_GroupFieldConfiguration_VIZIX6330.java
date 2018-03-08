package com.tierconnect.riot.migration.steps.groupConfiguration;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupField;
import com.tierconnect.riot.appcore.entities.QField;
import com.tierconnect.riot.appcore.services.FieldService;
import com.tierconnect.riot.appcore.services.GroupFieldService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_GroupFieldConfiguration_VIZIX6330 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_GroupFieldConfiguration_VIZIX6330.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFeature();
    }

    private void migrateFeature() {
        Group group = GroupService.getInstance().getRootGroup();
        Map groupValues = new HashMap();
        groupValues.put("language","en");
        groupValues.put("defaultNavBar","tenants");
        groupValues.put("pageSize",15);
        groupValues.put("thing",3);
        groupValues.put("thingType",3);
        groupValues.put("role",2);
        groupValues.put("groupType",2);
        groupValues.put("zone",3);
        groupValues.put("localMap",3);
        groupValues.put("report",3);
        groupValues.put("maxReportRecords",1000000);
        groupValues.put("pagePanel",15);
        groupValues.put("alertPollFrequency",15);
        groupValues.put("logicalReader",3);
        groupValues.put("alert","false");
        groupValues.put("shift",3);
        groupValues.put("zoneType",3);
        groupValues.put("edgebox",3);
        groupValues.put("emailSmtpHost","tcexchange2010.tierconnect.com");
        groupValues.put("emailSmtpPort",25);
        groupValues.put("emailSmtpUser","riottest@tierconnect.com");
        groupValues.put("emailSmtpPassword","");
        groupValues.put("emailSmptTls","false");
        groupValues.put("emailSmtpSsl","false");
        groupValues.put("shiftZoneValidation",60);
        groupValues.put("sessionTimeout",10);
        groupValues.put("authenticationMode","nativeAuthentication");
        groupValues.put("ldapAdConnection","");
        groupValues.put("ldapValidateUserCreation","false");
        groupValues.put("ldapUserIdentifier","sAMAccountName");
        groupValues.put("zoneGroup",3);
        groupValues.put("ipAddress","10.0.31.160");
        groupValues.put("shiftZoneValidationEnabled","false");
        groupValues.put("genetecVideoLinksVisible","false");
        groupValues.put("cutoffTimeseries",400000);
        groupValues.put("executeRulesForLastDetectTime","true");
        groupValues.put("playbackMaxThings",100);
        groupValues.put("reloadAllThingsThreshold",1000);
        groupValues.put("sendThingFieldTickle","false");
        groupValues.put("mapReportLimit",50000);
        groupValues.put("tableSummaryReportLimit",1000000);
        groupValues.put("allowedExtensions","pdf,doc,*");
        groupValues.put("blockedExtensions","exe,bat");
        groupValues.put("maxUploadSize",2048);
        groupValues.put("inheritThingVisibility","true");
        groupValues.put("maxImageWidth",1000);
        groupValues.put("maxImageHeight",1000);
        groupValues.put("fileSystemPath","");
        groupValues.put("fmcSapUrl","");
        groupValues.put("fmcSapUsername","");
        groupValues.put("fmcSapPassword","");
        groupValues.put("fmcSapNumberOfRetries",5);
        groupValues.put("fmcSapWaitSecondsToRetry",30);
        groupValues.put("fmcSapEnableSapSyncOnImport","false");
        groupValues.put("batchUpdateLogDirectory","");
        groupValues.put("genetecServer","10.100.0.83");
        groupValues.put("genetecPort",8686);
        groupValues.put("reportTimeOutCache",15000);
        groupValues.put("i18NDirectory","");
        groupValues.put("thingListInTreeView ","false");
        groupValues.put("notification_backProcessReport",15);
        groupValues.put("reloadAllThingsThreshold_bulkProcess",1000);
        groupValues.put("sendThingFieldTickle_bulkProcess","false");
        groupValues.put("fmcSapEnableSapSync_bulkProcess","false");
        groupValues.put("batchSize_bulkProcess",10000);
        groupValues.put("reportLogEnable","false");
        groupValues.put("reportLogThreshold",300000);
        groupValues.put("indexMinDaysToMaintain",30);
        groupValues.put("indexCleanupSchedule","0 10 0 ? * * *");
        groupValues.put("indexStatisticSchedule","0 0 0 ? * * *");
        groupValues.put("notification_importProcess",15);
        groupValues.put("background_percentUpdate",5);
        groupValues.put("max_number_of_columns",50);
        groupValues.put("max_number_of_rows",1000);
        groupValues.put("emailConfigurationError","riottest@tierconnect.com");
        groupValues.put("max_recentItem",50);
        groupValues.put("max_favoriteItem",50);
        groupValues.put("autoLoadTabMenus","true");
        groupValues.put("timeZoneConfiguration","EST5EDT");
        groupValues.put("dateFormatConfiguration","MM/DD/YYYY hh:mm:ss A");
        groupValues.put("bridgeStatusUpdateInterval",10000);
        groupValues.put("bridgeErrorStatusTimeout",30000);
        groupValues.put("autoLoadThingList","false");
        groupValues.put("autoLoadThingTypeList","false");
        groupValues.put("homeUrl","home.mojix.com");
        groupValues.put("consortium",3);
        groupValues.put("max_number_of_table_columns",50);
        groupValues.put("scheduledRule",2);
        groupValues.put("oauth2Connection","");
        groupValues.put("oauth2LoginMessage","Log in with:");
        groupValues.put("passwordMinLength",8);
        groupValues.put("passwordMaxLength",128);
        groupValues.put("passwordUppercaseRequired","true");
        groupValues.put("passwordNumberRequired","true");
        groupValues.put("passwordSpecialCharRequired","true");
        groupValues.put("passwordConsecutiveChar",2);
        groupValues.put("passwordStrength","moderate");
        groupValues.put("passwordUseReservedWords","true");
        groupValues.put("passwordReservedWords","root,mojix,vizix");
        groupValues.put("passwordReusePrevious",4);
        groupValues.put("passwordLoginChange","true");
        groupValues.put("passwordExpirationDays",90);
        groupValues.put("passwordExpirationNoticeDays",3);
        groupValues.put("passwordFailedAttempts",3);
        groupValues.put("passwordFailedLockTime",30);

        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QField.field.group.eq(group));
        be = be.and(QField.field.parentField.isNull());
        List<Field> fieldList = FieldService.getInstance().listPaginated(be, null, null);
        for (Field field:fieldList){
            GroupField groupField = GroupFieldService.getInstance().selectByGroupField(group,field);
            if (groupField == null){
                groupField = new GroupField();
                groupField.setField(field);
                groupField.setGroup(group);
                groupField.setValue(groupValues.get(field.getName()).toString());
                GroupFieldService.getInstance().insert(groupField);
            }
        }

    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
