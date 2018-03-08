package com.tierconnect.riot.appcore.popdb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.appcore.utils.MlConfiguration;
import com.tierconnect.riot.commons.Constants;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

import static com.tierconnect.riot.commons.Constants.*;
import static com.tierconnect.riot.commons.Constants.AutoIndexMessages.GENERATE_STATICS;
import static com.tierconnect.riot.commons.Constants.AutoIndexMessages.INDEX_MIN_EXECUTIONS;


/*
 * SHOW ROLE-RESOURCES RECORDS
 *
 * select group0.name as GroupName, groupType.name as GroupType, role.name as
 * Role, resource.name as Resource, permissions, acceptedAttributes from
 * roleResource, role, resource, groupType, group0 where
 * role.id=roleResource.role_id and resource.id= roleResource.resource_id and
 * role.groupType_id = groupType.id and group0.id=role.group_id order by
 * group0.id, role.name, resource.name;
 */

/**
 * @author terry
 *         <p>
 *         This class populates the minimum required records for any appcore
 *         instance
 */
public class PopDBRequired {

    private static Logger logger = Logger.getLogger(PopDBRequired.class);

    /**
     * initialize JDBC Drivers.
     * Explicitly load the mysql and msSql drivers otherwise popDb would fail
     */
    public static void initJDBCDrivers() {
        try {
            Class.forName("org.gjt.mm.mysql.Driver");
            logger.info("registering mysql jdbc driver");
        } catch (Exception ex) {
            //empty
        }
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            logger.info("registering sqlServer jdbc driver");
        } catch (Exception ex) {
            //empty
        }
    }

    /**
     * Close JDBC Drivers
     * Explicitly close the JDBC drivers.
     */
    public static void closeJDBCDrivers() {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            try {
                DriverManager.deregisterDriver(driver);
                logger.info(String.format("deregistering jdbc driver: %s", driver));
            } catch (SQLException e) {
                logger.error(String.format("Error deregistering driver %s", driver), e);
            }
        }
    }

    public void run() {

        Group rootGroup = PopDBUtils.popGroup("root", "root", null, null, "");
        PopDBUtils.popGroupResource(rootGroup);

        GroupType rootGroupType = PopDBUtils.popGroupType("root", rootGroup, null, "");

        rootGroup.setGroupType(rootGroupType);
        GroupService.getGroupDAO().update(rootGroup);

        HashSet<Resource> resources = PopulateResources.populatePopDBRequired(rootGroup);


        GroupType tenantGroupType = new GroupType();
        tenantGroupType.setGroup(rootGroup);
        tenantGroupType.setName("Company");
        tenantGroupType.setParent(rootGroupType);
        tenantGroupType.setCode("tenant");
        GroupTypeService.getInstance().insert(tenantGroupType);


        // Populate Resources and Roles
        Role rootRole = PopDBUtils.popRole("root", "root", resources, rootGroup, rootGroupType);

        User rootUser = PopDBUtils.popUser("root", "root", rootGroup, rootRole);
        rootUser.setFirstName("Root");
        rootUser.setLastName("User");
        rootUser.setEmail("");
        rootUser.setApiKey("7B4BCCDC");
        UserService.getInstance().update(rootUser);

        Field f = PopDBUtils.popFieldService("language", "language", "Language", rootGroup, "Internationalization",
                "java.lang" +
                        ".String", null, true);
        PopDBUtils.popGroupField(rootGroup, f, "en");
        Field f1 = PopDBUtils.popFieldService("defaultNavBar", "defaultNavBar", "Default Module", rootGroup, "Look & " +
                "Feel", "java.lang.String", null, true);
        PopDBUtils.popGroupField(rootGroup, f1, "tenants");
        Field f2 = PopDBUtils.popFieldService("pageSize", "pageSize", "Page Size", rootGroup, "Look & Feel", "java" +
                ".lang.Integer", null, true);
        PopDBUtils.popGroupField(rootGroup, f2, "15");
        Field f3 = PopDBUtils.popFieldService("thing", "thing", "Thing", rootGroup, "Ownership Levels", "java.lang" +
                ".Integer", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f3, "3");
        Field f4 = PopDBUtils.popFieldService("thingType", "thingType", "Thing Type", rootGroup, "Ownership Levels",
                "java.lang.Integer", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f4, "3");
        Field f5 = PopDBUtils.popFieldService("role", "role", "Role", rootGroup, "Ownership Levels", "java.lang" +
                ".Integer", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f5, "2");
        Field f6 = PopDBUtils.popFieldService("groupType", "groupType", "Group Type", rootGroup, "Ownership Levels",
                "java.lang.Integer", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f6, "2");
        Field f7 = PopDBUtils.popFieldService("zone", "zone", "Zone", rootGroup, "Ownership Levels", "java.lang" +
                ".Integer", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f7, "3");
        Field f8 = PopDBUtils.popFieldService("localMap", "localMap", "Local Map", rootGroup, "Ownership Levels",
                "java.lang.Integer", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f8, "3");
        Field f9 = PopDBUtils.popFieldService("report", "report", "Report", rootGroup, "Ownership Levels", "java.lang" +
                ".Integer", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f9, "3");
        Field f10 = PopDBUtils.popFieldService("maxReportRecords", "maxReportRecords", "Max Report Records",
                rootGroup, "Reports", "java.lang.Integer", null, true);
        PopDBUtils.popGroupField(rootGroup, f10, "1000000");
        Field f11 = PopDBUtils.popFieldService("pagePanel", "pagePanel", "Panel Pagination Size", rootGroup,
                "Reports", "java.lang.Integer", null, true);
        PopDBUtils.popGroupField(rootGroup, f11, "15");
        Field f12 = PopDBUtils.popFieldService("alertPollFrequency", "alertPollFrequency", "Alert Poll Frequency " +
                "(secs)", rootGroup, "Alerting & Notification", "java.lang.Integer", null, true);
        PopDBUtils.popGroupField(rootGroup, f12, "15");
        Field f13 = PopDBUtils.popFieldService("logicalReader", "logicalReader", "Logical Reader", rootGroup,
                "Ownership Levels", "java.lang.String", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f13, "3");
        Field f14 = PopDBUtils.popFieldService("stopAlerts", "alert", "Alert", rootGroup, "Alerting & Notification",
                "java.lang.Boolean", null, true);
        PopDBUtils.popGroupField(rootGroup, f14, "false");
        Field f15 = PopDBUtils.popFieldService("shift", "shift", "Shifts", rootGroup, "Ownership Levels", "java.lang" +
                ".Integer", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f15, "3");
        Field f16 = PopDBUtils.popFieldService("zoneType", "zoneType", "Zone Type", rootGroup, "Ownership Levels",
                "java.lang.Integer", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f16, "3");
        Field f17 = PopDBUtils.popFieldService("edgebox", "edgebox", "Edgebox", rootGroup, "Ownership Levels", "java" +
                ".lang.Integer", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f17, "3");
        Field f18 = PopDBUtils.popFieldService("emailSmtpHost", "emailSmtpHost", "Host", rootGroup, "SMTP Email " +
                "Configuration", "java.lang.String", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f18, "tcexchange2010.tierconnect.com");
        Field f19 = PopDBUtils.popFieldService("emailSmtpPort", "emailSmtpPort", "Port", rootGroup, "SMTP Email " +
                "Configuration", "java.lang.Integer", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f19, "25");
        Field f20 = PopDBUtils.popFieldService(Constants.EMAIL_SMTP_USER, Constants.EMAIL_SMTP_USER, "User",
                rootGroup, "SMTP Email " +
                        "Configuration", "java.lang.String", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f20, Constants.EMAIL_RIOT_TEST);
        Field f21 = PopDBUtils.popFieldService("emailSmtpPassword", "emailSmtpPassword", "Password", rootGroup, "SMTP" +
                " " +
                "Email Configuration", "java.lang.String", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f21, "");
        Field f22 = PopDBUtils.popFieldService("emailSmtpTls", "emailSmtpTls", "TLS", rootGroup, "SMTP Email " +
                "Configuration", "java.lang.Boolean", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f22, "false");
        Field f23 = PopDBUtils.popFieldService("emailSmtpSsl", "emailSmtpSsl", "SSL", rootGroup, "SMTP Email " +
                "Configuration", "java.lang.Boolean", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f23, "false");

        Field f24 = PopDBUtils.popFieldService("shiftZoneValidation", "shiftZoneValidation", "Shift-Zone Validation",
                rootGroup, "Job Scheduling", "java.lang.Integer", null, true);
        PopDBUtils.popGroupField(rootGroup, f24, "60");
        Field f25 = PopDBUtils.popFieldService("sessionTimeout", "sessionTimeout", "Session TimeOut (mins)",
                rootGroup, "Security Configuration", "java.lang.Integer", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f25, "10");
        // Authentication values begin
        Field f50 = PopDBUtils.popFieldService("authenticationMode", "authenticationMode", "Authentication Mode",
                rootGroup, "Security Configuration", "java.lang.String", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f50, "nativeAuthentication");
        PopDBUtils.popFieldWithParentService("ldapAdAuthentication", "ldapAdAuthentication", "LDAP/AD",
                rootGroup, "Security Configuration", "java.lang.String", 3L, false, f50);
        PopDBUtils.popFieldWithParentService("nativeAuthentication", "nativeAuthentication", "Native",
                rootGroup, "Security Configuration", "java.lang.String", 3L, false, f50);
        PopDBUtils.popFieldWithParentService("oauth2Authentication", "oauth2Authentication", "OAuth 2.0",
                rootGroup, "Security Configuration", "java.lang.String", 3L, false, f50);
        Field f53 = PopDBUtils.popFieldService("ldapAdConnection", "ldapAdConnection", "LDAP/AD Connection",
                rootGroup, "Security Configuration", "java.lang.String", 3L, true);
        PopDBUtils.popGroupField(rootGroup, f53, "");
        Field f54 = PopDBUtils.popFieldService("ldapValidateUserCreation", "ldapValidateUserCreation", "LDAP/AD " +
                        "Validate User Creation",
                rootGroup, "Security Configuration", "java.lang.Boolean", 3L, true);
        PopDBUtils.popGroupField(rootGroup, f54, "false");
        Field f55 = PopDBUtils.popFieldService("ldapUserIdentifier", "ldapUserIdentifier", "LDAP/AD User Identifier",
                rootGroup, "Security Configuration", "java.lang.String", 3L, true);
        PopDBUtils.popGroupField(rootGroup, f55, "sAMAccountName");

        // Authentication values end.
        Field f26 = PopDBUtils.popFieldService("zoneGroup", "zoneGroup", "Zone Group", rootGroup, "Ownership Levels",
                "java.lang.Integer", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f26, "3");
        Field f27 = PopDBUtils.popFieldService("ipAddress", "ipAddress", "Image Server", rootGroup, "Integration",
                "java.lang.String", null, true);
        PopDBUtils.popGroupField(rootGroup, f27, "10.0.31.160");
        Field f29 = PopDBUtils.popFieldService("shiftZoneValidationEnabled", "shiftZoneValidationEnabled",
                "Shift-Zone" +
                        " Validation Enabled", rootGroup, "Job Scheduling", "java.lang.Boolean", null, true);
        PopDBUtils.popGroupField(rootGroup, f29, "false");
        Field f30 = PopDBUtils.popFieldService("genetecVideoLinksVisible", "genetecVideoLinksVisible", "Genetec Video" +
                " " +
                "Links Visible", rootGroup, "Integration", "java.lang.Boolean", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f30, "false");

//		Field f31 = PopDBUtils.popFieldService("saveDwellTimeHistory", "saveDwellTimeHistory", "Save DwellTime " +
//				"History", rootGroup, "Data Storage Configuration", "java.lang.Boolean", 3L, true);
//		PopDBUtils.popGroupField(rootGroup, f31, "true");
        Field f32 = PopDBUtils.popFieldService("cutoffTimeseries", "cutoffTimeseries", "Max size of snapshots ids",
                rootGroup, "Data Storage Configuration", "java.lang.Long", 3L, true);
        PopDBUtils.popGroupField(rootGroup, f32, "400000");
        Field f33 = PopDBUtils.popFieldService("executeRulesForLastDetectTime", "executeRulesForLastDetectTime",
                "Execute CEP rules when only lastDetectTime is sent", rootGroup, "Data Storage Configuration",
                "java" +
                        ".lang.Boolean", 3L, true);
        PopDBUtils.popGroupField(rootGroup, f33, "true");
        Field f34 = PopDBUtils.popFieldService("playbackMaxThings", "playbackMaxThings", "Playback Max Things",
                rootGroup, "Reports", "java.lang.Integer", 2L, true);
        PopDBUtils.popGroupField(rootGroup, f34, "100");

        Field f35 = PopDBUtils.popFieldService("reloadAllThingsThreshold", "reloadAllThingsThreshold", "Things Cache " +
                "Reload Threshold", rootGroup, "Import Configuration", "java.lang.Long", 3L, true);
        PopDBUtils.popGroupField(rootGroup, f35, "1000");
        Field f36 = PopDBUtils.popFieldService("sendThingFieldTickle", "sendThingFieldTickle", "Run Rules After " +
                "Import", rootGroup, "Import Configuration", "java.lang.Boolean", 3L, true);
        PopDBUtils.popGroupField(rootGroup, f36, "false");
        Field f37 = PopDBUtils.popFieldService("mapReportLimit", "mapReportLimit", "Map Report Limit", rootGroup,
                "Reports", "java.lang.Integer", null, true);
        PopDBUtils.popGroupField(rootGroup, f37, "50000");
        Field f38 = PopDBUtils.popFieldService("tableSummaryReportLimit", "tableSummaryReportLimit", "Table Summary "
                + "Report Limit", rootGroup, "Reports", "java.lang.Integer", null, true);
        PopDBUtils.popGroupField(rootGroup, f38, "1000000");
        //AttachmentConfig popFieldService
        PopDBRequired.populateAttachmentConfig(rootGroup);

        PopDBUtils.migrateFieldService("fmcSapUrl", "fmcSapUrl", "SAP hostname", rootGroup, "Integration", "java.lang" +
                ".String", 2L, false, "");
        PopDBUtils.migrateFieldService("fmcSapUsername", "fmcSapUsername", "SAP username", rootGroup, "Integration",
                "java.lang.String", 2L, false, "");
        PopDBUtils.migrateFieldService("fmcSapPassword", "fmcSapPassword", "SAP password", rootGroup, "Integration",
                "java.lang.String", 2L, false, "");
        PopDBUtils.migrateFieldService("fmcSapNumberOfRetries", "fmcSapNumberOfRetries", "SAP number of retries",
                rootGroup, "Integration", "java.lang.Integer", 2L, false,
                "5");
        PopDBUtils.migrateFieldService("fmcSapWaitSecondsToRetry", "fmcSapWaitSecondsToRetry", "SAP seconds between " +
                        "retries", rootGroup, "Integration", "java.lang.Integer", 2L, false,
                "30");
        PopDBUtils.migrateFieldService("fmcSapEnableSapSyncOnImport", "fmcSapEnableSapSyncOnImport", "Enable SAP Sync" +
                        " " +
                        "on import", rootGroup, "Import Configuration", "java.lang.Boolean", 2L, false,
                "false");

        PopDBUtils.migrateFieldService("batchUpdateLogDirectory", "batchUpdateLogDirectory", "Batch Thing Update log " +
                        "directory", rootGroup, "Integration", "java.lang.String", 2L, false,
                "");

        PopDBUtils.migrateFieldService("genetecServer", "genetecServer", "Video Provider Server Host", rootGroup,
                "Integration", "java.lang.String", 1L, true,
                "10.100.0.83");
        PopDBUtils.migrateFieldService("genetecPort", "genetecPort", "Video Provider Server Port", rootGroup,
                "Integration", "java.lang.String", 1L, true,
                "8686");


        PopDBUtils.migrateFieldService("reportTimeOutCache", "reportTimeOutCache", "Report Time Out Cache",
                rootGroup, "Reports", "java.lang.Integer", 1L, true,
                "15000");

        PopDBUtils.migrateFieldService("i18NDirectory", "i18NDirectory", "I18N Directory", rootGroup,
                "Internationalization",
                "java.lang.String", 1L, true,
                "");
        Field f43 = PopDBUtils.popFieldService("thingListInTreeView", "thingListInTreeView", "Thing List In Tree " +
                        "View", rootGroup, "Look & Feel",
                "java.lang.Boolean", null, true);
        PopDBUtils.popGroupField(rootGroup, f43, "false");

        Field f39 = PopDBUtils.popFieldService("notification_backProcessReport", "notification_backProcessReport",
                "Background Notification Time " +
                        "(Report secs)", rootGroup, "Background Process", "java.lang.Integer", null, true);
        PopDBUtils.popGroupField(rootGroup, f39, "15");

        Field f40 = PopDBUtils.popFieldService("reloadAllThingsThreshold_bulkProcess",
                "reloadAllThingsThreshold_bulkProcess", "Things Cache " +
                        "Reload Threshold", rootGroup, "Reports", "java.lang.Long", 3L, true);
        PopDBUtils.popGroupField(rootGroup, f40, "1000");

        Field f41 = PopDBUtils.popFieldService("sendThingFieldTickle_bulkProcess",
                "sendThingFieldTickle_bulkProcess", "Run Rules After " +
                        "Bulk Process", rootGroup, "Reports", "java.lang.Boolean", 3L, true);
        PopDBUtils.popGroupField(rootGroup, f41, "false");

        Field f42 = PopDBUtils.popFieldService("fmcSapEnableSapSync_bulkProcess", "fmcSapEnableSapSync_bulkProcess",
                "Enable SAP Sync on " +
                        "Bulk Process", rootGroup, "Reports", "java.lang.Boolean", 2L, false);
        PopDBUtils.popGroupField(rootGroup, f42, "false");

        Field f44 = PopDBUtils.popFieldService("batchSize_bulkProcess", "batchSize_bulkProcess",
                "Batch Size Bulk Process", rootGroup, "Reports", "java.lang.Long", null, true);
        PopDBUtils.popGroupField(rootGroup, f44, "10000");

        // reportLog fields
        addAutoIndexFields();
        addAutoIndexDeleteIndex();

        Field f45 = PopDBUtils.popFieldService("notification_importProcess", "notification_importProcess",
                "Background Notification Time " +
                        "(Import/Export secs)", rootGroup, "Background Process", "java.lang.Integer", null, true);
        PopDBUtils.popGroupField(rootGroup, f45, "15");

        Field f46 = PopDBUtils.popFieldService("background_percentUpdate", "background_percentUpdate", "Background " +
                "Update Percent " +
                "(Import/Export %)", rootGroup, "Background Process", "java.lang.Integer", null, true);
        PopDBUtils.popGroupField(rootGroup, f46, "5");

        Field f47 = PopDBUtils.popFieldService("max_number_of_columns", "max_number_of_columns",
                "Max Table Summary Columns", rootGroup, "Reports", "java.lang.Long", null, true);
        PopDBUtils.popGroupField(rootGroup, f47, "50");

        Field f48 = PopDBUtils.popFieldService("max_number_of_rows", "max_number_of_rows",
                "Max Table Summary Rows", rootGroup, "Reports", "java.lang.Long", null, true);
        PopDBUtils.popGroupField(rootGroup, f48, "1000");

        Field f49 = PopDBUtils.popFieldService("emailConfigurationError", "emailConfigurationError", "Email " +
                        "Configuration Error",
                rootGroup, "SMTP Email Configuration", "java.lang.String", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f49, Constants.EMAIL_RIOT_TEST);

        Field f56 = PopDBUtils.popFieldService("max_recentItem", "max_recentItem", "Max Recent Items", rootGroup,
                "Look & Feel", "java" +
                        ".lang.Integer", null, true);
        PopDBUtils.popGroupField(rootGroup, f56, "50");

        Field f57 = PopDBUtils.popFieldService("max_favoriteItem", "max_favoriteItem", "Max Favorite Items",
                rootGroup, "Look & Feel", "java" +
                        ".lang.Integer", null, true);
        PopDBUtils.popGroupField(rootGroup, f57, "50");

        Field f58 = PopDBUtils.popFieldService("autoLoadTabMenus", "autoLoadTabMenus", "Auto-Load Tab Menus",
                rootGroup, "Look & Feel", "java.lang.Boolean", 2L, true);
        PopDBUtils.popGroupField(rootGroup, f58, "true");

        populateGroupFieldsRegionalSettings(rootGroup);

        //Alerting and Notification fields
        Field f59 = PopDBUtils.popFieldService("bridgeStatusUpdateInterval", "bridgeStatusUpdateInterval",
                "Bridge Status Update Interval (ms)", rootGroup, "Alerting & Notification", "java.lang.Long", 1L, true);
        PopDBUtils.popGroupField(rootGroup, f59, "10000");

        Field f60 = PopDBUtils.popFieldService("bridgeErrorStatusTimeout", "bridgeErrorStatusTimeout", "Bridge Error " +
                        "Status Timeout (ms)",
                rootGroup, "Alerting & Notification", "java.lang.Long", 1L, true);
        PopDBUtils.popGroupField(rootGroup, f60, "30000");

        Field f61 = PopDBUtils.popFieldService("autoLoadThingList", "autoLoadThingList", "Auto-Load Thing List",
                rootGroup, "Look & Feel", "java.lang.Boolean", 3L, true);
        PopDBUtils.popGroupField(rootGroup, f61, "false");

        Field f62 = PopDBUtils.popFieldService("autoLoadThingTypeList", "autoLoadThingTypeList", "Auto-Load Thing " +
                        "Type List",
                rootGroup, "Look & Feel", "java.lang.Boolean", 3L, true);
        PopDBUtils.popGroupField(rootGroup, f62, "false");

        Field f63 = PopDBUtils.popFieldService("homeUrl", "homeUrl", "Home URL",
                rootGroup, "Home Configuration", "java.lang.String", 1L, true);
        PopDBUtils.popGroupField(rootGroup, f63, "home.mojix.com");
        Field f64 = PopDBUtils.popFieldService("consortium", "consortium", "Blockchain Consortium", rootGroup,
                "Ownership Levels",
                "java.lang.Integer", 3L, false);
        PopDBUtils.popGroupField(rootGroup, f64, "3");
        Field f65 = PopDBUtils.popFieldService("max_number_of_table_columns", "max_number_of_table_columns",
                "Max Table Columns", rootGroup, "Reports", "java.lang.Long", null, true);
        PopDBUtils.popGroupField(rootGroup, f65, "50");
		Field f66 = PopDBUtils.popFieldService("scheduledRule", "scheduledRule", "Scheduled Rule", rootGroup, "Ownership Levels",
				"java.lang.Integer", 3L, true);
		PopDBUtils.popGroupField(rootGroup, f66, "2");
        Field f68 = PopDBUtils.popFieldService("oauth2Connection", "oauth2Connection", "OAuth 2.0 Connection",
                rootGroup, "Security Configuration", "java.lang.String", 1L, true);
        PopDBUtils.popGroupField(rootGroup, f68, "");

        Field f67 = PopDBUtils.popFieldService("oauth2LoginMessage", "oauth2LoginMessage", "OAuth 2.0 Login Message",
                rootGroup, "Security Configuration", "java.lang.String", 1L, true);
        PopDBUtils.popGroupField(rootGroup, f67, "Log in with:");
        populatePasswordPoliciesFields(rootGroup);
        PopDBUtils.popDBVersion();
        populateConnectionTypes(rootGroup);
        populateLDAPConnection(rootGroup);
    }

    private static void populateGroupFieldsRegionalSettings(Group rootGroup) {
        Field field = PopDBUtils.popFieldService(Constants.TIME_ZONE_CONFIG, Constants.TIME_ZONE_CONFIG, "Time Zone",
                rootGroup, "Regional Settings", "java.lang.String", 3L, true);
        PopDBUtils.popGroupField(rootGroup, field, Constants.DEFAULT_TIME_ZONE);
        Field fieldDate = PopDBUtils.popFieldService(Constants.DATE_FORMAT_CONFIG, Constants.DATE_FORMAT_CONFIG,
                "Date Format",
                rootGroup, "Regional Settings", "java.lang.String", 3L, true);
        PopDBUtils.popGroupField(rootGroup, fieldDate, Constants.DEFAULT_DATE_FORMAT);
    }

    public static void addAutoIndexFields() {
        Group rootGroup = GroupService.getInstance().getRootGroup();
        Field f01 = PopDBUtils.popFieldService("reportLogEnable", "reportLogEnable", "Indexing: Enable/Disable Index " +
                "Stats & Analysis", rootGroup, "Reports", "java.lang.Boolean", null, true);
        PopDBUtils.popGroupField(rootGroup, f01, "false");
        Field f02 = PopDBUtils.popFieldService("reportLogThreshold", "reportLogThreshold", "Indexing: Threshold for " +
                "Slow Reports (ms)", rootGroup, "Reports", "java.lang.Long", null, true);
        PopDBUtils.popGroupField(rootGroup, f02, "300000");
    }

    /**
     * Method To set all auto index delete parameters.
     */
    public static void addAutoIndexDeleteIndex() {
        Group rootGroup = GroupService.getInstance().getRootGroup();

        //TODO: Set this message when the name column exists.
        /*Indexing: Maximum number of days to maintain unused report query indexes. Zero disables this check. Default
         recommendation is 30 days.*/
        Field f01 = PopDBUtils.popFieldService(
                INDEX_MIN_DAYS_TO_MAINTAIN,
                INDEX_MIN_DAYS_TO_MAINTAIN,
                AutoIndexMessages.INDEX_ANALYZE.getMessage(),
                rootGroup,
                "Reports",
                "java.lang.Long",
                1L,
                true);

        //TODO: Set this message when the name column exists.
        /*Advanced Option. Indexing: This field defines each when the deletion process is executed automatically.
        This should run at least a minute after the Index Statistics Schedule.*/
        Field f02 = PopDBUtils.popFieldService(
                INDEX_CLEANUP_SCHEDULE,
                INDEX_CLEANUP_SCHEDULE,
                AutoIndexMessages.DELETE_PROCESS.getMessage(),
                rootGroup,
                "Reports",
                "java.lang.String",
                1L,
                true);

        //TODO: Set this message when the name column exists.
        /*Advanced Option. Indexing: This field defines each when the process of generating statistics is executed
        automatically. This should run at same or higher frequency than Index Cleanup Schedule. This should run at
        least a minute before the Index Cleanup Schedule. Default recommendation is run once a day, before the Index
        Cleanup Schedule.*/
        Field f03 = PopDBUtils.popFieldService(
                INDEX_STATISTIC_SCHEDULE,
                INDEX_STATISTIC_SCHEDULE,
                GENERATE_STATICS.getMessage(),
                rootGroup, "Reports",
                "java.lang.String",
                1L,
                true);

        //TODO: Set this message when the name column exists.
        /*Advanced Option.Indexing: Minimum count of report index queries required between cleanups to maintain index
        .Zero disables this check.Default recommendation is 1 report index query per day, based on Index Cleanup
        Schedule.*/
        Field f04 = PopDBUtils.popFieldService(
                INDEX_MIN_COUNT_TO_MAINTAIN,
                INDEX_MIN_COUNT_TO_MAINTAIN,
                INDEX_MIN_EXECUTIONS.getMessage(),
                rootGroup,
                "Reports",
                "java.lang.Long",
                1L,
                true);
        PopDBUtils.popGroupField(rootGroup, f01, "30");
        PopDBUtils.popGroupField(rootGroup, f02, "0 10 0 ? * * *");
        PopDBUtils.popGroupField(rootGroup, f03, "0 0 0 ? * * *");
        PopDBUtils.popGroupField(rootGroup, f04, "1");
    }

    /**
     * Populate Connection Types
     *
     * @param rootGroup A root Group
     */
    public static void populateConnectionTypes(Group rootGroup) {

        populateSQLConnection0(rootGroup);
        populateSQLConnection(rootGroup);
        populateMQTTConnection(rootGroup);
        populateMongoShardingConnection(rootGroup);
        populateFTPConnection(rootGroup);
        populateAnalyticsConnection(rootGroup);
        populateRestConnection(rootGroup);
        populateKafkaConnection(rootGroup);
        populateGooglePubSubConnection(rootGroup);
        populateOAuth2Connection(rootGroup);
    }


    public static void populateLDAPConnection(Group rootGroup) {
        if (ConnectionTypeService.getInstance().getConnectionTypeByCode("ldap") == null) {
            ConnectionType ldapConnectionType = new ConnectionType();
            ldapConnectionType.setGroup(rootGroup);
            ldapConnectionType.setCode("ldap");
            ldapConnectionType.setDescription("LDAP/AD Connection");
            ldapConnectionType.setRequiredTestOnCreateEdit(false);

            // Adding Properties definition
            List<Map<String, Object>> propertiesDefinition = new ArrayList<>();
            propertiesDefinition.add(newPropertyDefinition("userDn", "UserDn", "String", true));
            propertiesDefinition.add(newPropertyDefinition("password", "Password", "String", true));
            propertiesDefinition.add(newPropertyDefinition("base", "Base", "String", true));
            propertiesDefinition.add(newPropertyDefinition("url", "Url", "String", true));
            propertiesDefinition.add(newPropertyDefinition("referral", "Referral ('follow' by default)", "String",
                    false));

            ObjectMapper objectMapper = new ObjectMapper();
            try {
                ldapConnectionType.setPropertiesDefinitions(objectMapper.writeValueAsString(propertiesDefinition));
                ConnectionTypeService.getInstance().insert(ldapConnectionType);
            } catch (JsonProcessingException e) {
                logger.error("PopDB LDAP, error populating connectionType", e);
            }
        }
    }

    public static void populateOAuth2Connection(Group rootGroup) {
        if (ConnectionTypeService.getInstance().getConnectionTypeByCode("OAUTH2") == null) {
            ConnectionType oauthConnectionType = new ConnectionType();
            oauthConnectionType.setGroup(rootGroup);
            oauthConnectionType.setCode("OAUTH2");
            oauthConnectionType.setDescription("OAUTH2 Connection");
            oauthConnectionType.setRequiredTestOnCreateEdit(false);
            List<Map<String, String>> possibleValues=new ArrayList<>();
            possibleValues.add(newServerValue("custom","Custom","username"));
            possibleValues.add(newServerValue("google","Google","email","email"));
            possibleValues.add(newServerValue("facebook","Facebook","email", "email"));
            possibleValues.add(newServerValue("linkedin","LinkedIn","emailAddress"));
            possibleValues.add(newServerValue("github","GitHub","login"));

            List<Map<String, String>> methodOptions=new ArrayList<>();
            methodOptions.add(newServerValue("get","GET"));
            methodOptions.add(newServerValue("post","POST"));

            List<Map<String, String>> strategyOptions=new ArrayList<>();
            strategyOptions.add(newServerValue("implicit","Implicit Grant"));
            strategyOptions.add(newServerValue("authorizationCode","Authorization Code Grant"));
            // Adding Properties definition
            List<Map<String, Object>> propertiesDefinition = new ArrayList<>();
            propertiesDefinition.add(newPropertyDefinition("clientId", "Client Id", "String", true));
            propertiesDefinition.add(newPropertyDefinition("clientSecret", "Client Secret", "String", true));
            propertiesDefinition.add(newPropertyDefinition("accessTokenUri", "Access Token Uri", "String", true));
            propertiesDefinition.add(newPropertyDefinition("userAuthUri", "User Authorization Uri", "String", true));
            propertiesDefinition.add(newPropertyDefinition("userInfoUri", "User Information Uri", "String", true));
            propertiesDefinition.add(newPropertyDefinition("redirectUri", "Redirection Uri", "String", true));
            propertiesDefinition.add(newPropertyDefinition("provider", "Provider", "String",possibleValues, true));
            propertiesDefinition.add(newPropertyDefinition("accessTokenMethod", "Access Token Method", "String",methodOptions, true));
            propertiesDefinition.add(newPropertyDefinition("userIdentifier", "User Identifier", "String", true));
            propertiesDefinition.add(newPropertyDefinition("grantType", "Grant Type", "String", strategyOptions, true));
            propertiesDefinition.add(newPropertyDefinition("scope", "Scope", "String", false));

            ObjectMapper objectMapper = new ObjectMapper();
            try {
                oauthConnectionType.setPropertiesDefinitions(objectMapper.writeValueAsString(propertiesDefinition));
                ConnectionTypeService.getInstance().insert(oauthConnectionType);
            } catch (JsonProcessingException e) {
                logger.error("PopDB OAuth2, error populating connectionType", e);
            }
        }
    }

    public static Map<String, String> newServerValue(String code, String label,String userIdentifier){
         return newServerValue(code, label, userIdentifier, null);
    }
    public static Map<String, String> newServerValue(String code, String label){
        return newServerValue(code, label, null, null);
    }

    public static Map<String, String> newServerValue(String code, String label,String userIdentifier, String scope){
        Map<String, String> propertyDefinition = new LinkedHashMap<>();
        propertyDefinition.put("code", code);
        propertyDefinition.put("label", label);
        if (userIdentifier != null) {
            propertyDefinition.put("userIdentifier", userIdentifier);
        }
        if (scope != null){
            propertyDefinition.put("scope", scope);
        }
        return propertyDefinition;
    }

    public static Map<String, Object> newPropertyDefinition(String code, String label, String type, boolean required) {
        return newPropertyDefinition(code, label, type, null,null, required);
    }
    public static Map<String, Object> newPropertyDefinition(String code, String label, String type, List<Map<String, String>> possibleValues, boolean required) {
        return newPropertyDefinition(code, label, type, null,possibleValues, required);
    }

    public static Map<String, Object> newPropertyDefinition(String code, String label, String type, String
            defaultValue, List<Map<String, String>> possibleValues, boolean required) {
        Map<String, Object> propertyDefinition = new LinkedHashMap<>();
        propertyDefinition.put("code", code);
        propertyDefinition.put("label", label);
        propertyDefinition.put("type", type);
        if (defaultValue != null) {
            propertyDefinition.put("defaultValue", defaultValue);
        }
        if (possibleValues != null){
            propertyDefinition.put("possibleValues",possibleValues);
        }
        propertyDefinition.put("required", required);
        return propertyDefinition;
    }

    public static void populateSQLConnection0(Group rootGroup) {
        //SQL CONNECTION
        ConnectionType dbConnectionType = new ConnectionType();
        dbConnectionType.setGroup(rootGroup);
        dbConnectionType.setCode("SQL");
        dbConnectionType.setDescription("Internal SQL Connection");
        dbConnectionType.setRequiredTestOnCreateEdit(false);

        List<Map<String, Object>> propertyDefinitions = new ArrayList<>();

        propertyDefinitions.add(newPropertyDefinition("driver", "Driver", "String", true));
        propertyDefinitions.add(newPropertyDefinition("dialect", "Dialect", "String", true));
        propertyDefinitions.add(newPropertyDefinition("username", "Username", "String", true));
        propertyDefinitions.add(newPropertyDefinition("password", "Password", "String", true));
        propertyDefinitions.add(newPropertyDefinition("url", "URL", "String", true));
        propertyDefinitions.add(newPropertyDefinition("hazelcastNativeClientAddress", "hazelcastNativeClientAddress",
                "String", "hazelcast",null, true));

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            dbConnectionType.setPropertiesDefinitions(objectMapper.writeValueAsString(propertyDefinitions));
            ConnectionTypeService.getInstance().insert(dbConnectionType);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Populate SQL Connection
     *
     * @param rootGroup Root {@link Group} instance.
     */
    private static void populateSQLConnection(Group rootGroup) {
        //SQL CONNECTION
        ConnectionType dbConnectionType = new ConnectionType();
        dbConnectionType.setGroup(rootGroup);
        dbConnectionType.setCode("DBConnection");
        dbConnectionType.setDescription("External Relational DB");
        dbConnectionType.setRequiredTestOnCreateEdit(false);

        List<Map<String, Object>> propertyDefinitions = new ArrayList<>();
        Map<String, Object> propertyDefinitionDriver = newPropertyDefinition("driver", "Driver", "Array", true);

        List<Map<String, String>> propertyArrayValues = new ArrayList<>();
        Map<String, String> propertyArrayValue = new HashMap<>();
        propertyArrayValue.put("label", "SQLServer");
        propertyArrayValue.put("driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        propertyArrayValue.put("urlExample", "jdbc:sqlserver://localhost;DatabaseName=database");
        propertyArrayValues.add(propertyArrayValue);

        propertyArrayValue = new HashMap<>();
        propertyArrayValue.put("label", "MySQL");
        propertyArrayValue.put("driver", "com.mysql.jdbc.Driver");
        propertyArrayValue.put("urlExample", "jdbc:mysql://localhost:3306/database");
        propertyArrayValues.add(propertyArrayValue);

        propertyDefinitionDriver.put("values", propertyArrayValues);

        propertyDefinitions.add(propertyDefinitionDriver);
        propertyDefinitions.add(newPropertyDefinition("user", "User", "String", true));
        propertyDefinitions.add(newPropertyDefinition("password", "Password", "String", true));
        propertyDefinitions.add(newPropertyDefinition("url", "URL", "String", true));
        propertyDefinitions.add(newPropertyDefinition("schema", "Schema", "String", false));

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            dbConnectionType.setPropertiesDefinitions(objectMapper.writeValueAsString(propertyDefinitions));
            ConnectionTypeService.getInstance().insert(dbConnectionType);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Populate Mongo Connection
     *
     * @param rootGroup Root {@link Group} instance.
     */
    public static void populateMongoConnection(Group rootGroup) {
        //MONGO CONNECTION
        ConnectionType dbConnectionType = new ConnectionType();
        dbConnectionType.setGroup(rootGroup);
        dbConnectionType.setCode("MONGO");
        dbConnectionType.setDescription("Mongo DB");
        dbConnectionType.setRequiredTestOnCreateEdit(false);

        List<Map<String, Object>> propertyDefinitions = new ArrayList<>();

        propertyDefinitions.add(newPropertyDefinition("host", "Host", "String", true));
        propertyDefinitions.add(newPropertyDefinition("port", "Port", "Number", true));
        propertyDefinitions.add(newPropertyDefinition("dbname", "DBname", "String", true));
        propertyDefinitions.add(newPropertyDefinition("username", "Username", "String", true));
        propertyDefinitions.add(newPropertyDefinition("password", "Password", "String", true));
        propertyDefinitions.add(newPropertyDefinition("secure", "Secure", "Boolean", true));

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            dbConnectionType.setPropertiesDefinitions(objectMapper.writeValueAsString(propertyDefinitions));
            ConnectionTypeService.getInstance().insert(dbConnectionType);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }


    /**
     * Mongo sharding population
     *
     * @param rootGroup Root {@link Group} instance.
     */
    public static void populateMongoShardingConnection(Group rootGroup) {
        //MONGO CONNECTION
        ConnectionType dbConnectionType = new ConnectionType();
        dbConnectionType.setGroup(rootGroup);
        dbConnectionType.setCode("MONGO");
        dbConnectionType.setDescription("Mongo DB");
        dbConnectionType.setRequiredTestOnCreateEdit(false);

        List<Map<String, Object>> propertyDefinitions = new ArrayList<>();
        propertyDefinitions.add(newPropertyDefinition("mongoPrimary", "Mongo Primary", "String", true));
        //100.10.0.25:27017
        propertyDefinitions.add(newPropertyDefinition("mongoSecondary", "Mongo Secondary", "String", false));
        //100.10.0.25:27017,100.10.0.26:27017
        propertyDefinitions.add(newPropertyDefinition("mongoReplicaSet", "Mongo Replica Set", "String", false));
        //rs0_name
        propertyDefinitions.add(newPropertyDefinition("mongoSSL", "Mongo SSL", "Boolean", false));//true|false
        propertyDefinitions.add(newPropertyDefinition("username", "Username", "String", true));
        propertyDefinitions.add(newPropertyDefinition("password", "Password", "String", true));
        propertyDefinitions.add(newPropertyDefinition("mongoAuthDB", "Mongo Authentication DB", "String", true));
        //admin (--authenticationDatabase)
        propertyDefinitions.add(newPropertyDefinition("mongoDB", "Mongo Data Base", "String", true));//riot_main
        propertyDefinitions.add(newPropertyDefinition("mongoSharding", "Mongo Sharding", "Boolean", true));
        //true|false (*)
        propertyDefinitions.add(newPropertyDefinition("mongoConnectTimeout", "Connection Timeout (ms)", "Number",
                true));//30000 | 0
        propertyDefinitions.add(newPropertyDefinition("mongoMaxPoolSize", "Max Pool Size (Connections)", "Number",
                true));//100 | 0

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            dbConnectionType.setPropertiesDefinitions(objectMapper.writeValueAsString(propertyDefinitions));
            ConnectionTypeService.getInstance().insert(dbConnectionType);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Populate MQTT connections
     *
     * @param rootGroup Root {@link Group} instance.
     */
    public static void populateMQTTConnection(Group rootGroup) {
        //MQTT CONNECTION
        ConnectionType dbConnectionType = new ConnectionType();
        dbConnectionType.setGroup(rootGroup);
        dbConnectionType.setCode("MQTT");
        dbConnectionType.setDescription("MQTT Broker (Mosquitto)");
        dbConnectionType.setRequiredTestOnCreateEdit(false);

        List<Map<String, Object>> propertyDefinitions = new ArrayList<>();

        propertyDefinitions.add(newPropertyDefinition("host", "Host", "String", true));
        propertyDefinitions.add(newPropertyDefinition("port", "Port", "Number", true));
        propertyDefinitions.add(newPropertyDefinition("qos", "QoS", "Number", true));
        propertyDefinitions.add(newPropertyDefinition("secure", "Secure", "Boolean", false));
        propertyDefinitions.add(newPropertyDefinition("username", "Username", "String", false));
        propertyDefinitions.add(newPropertyDefinition("password", "Password", "String", false));

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            dbConnectionType.setPropertiesDefinitions(objectMapper.writeValueAsString(propertyDefinitions));
            ConnectionTypeService.getInstance().insert(dbConnectionType);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Populate FTP connection
     *
     * @param rootGroup Root {@link Group} instance.
     */
    public static void populateFTPConnection(Group rootGroup) {
        //FTP CONNECTION
        ConnectionType dbConnectionType = new ConnectionType();
        dbConnectionType.setGroup(rootGroup);
        dbConnectionType.setCode("FTP");
        dbConnectionType.setDescription("FTP Server");
        dbConnectionType.setRequiredTestOnCreateEdit(false);

        List<Map<String, Object>> propertyDefinitions = new ArrayList<>();
        propertyDefinitions.add(newPropertyDefinition("username", "Username", "String", true));
        propertyDefinitions.add(newPropertyDefinition("password", "Password", "String", true));
        propertyDefinitions.add(newPropertyDefinition("host", "Host", "String", true));
        propertyDefinitions.add(newPropertyDefinition("port", "Port", "Number", true));
        propertyDefinitions.add(newPropertyDefinition("secure", "Secure", "Boolean", false));

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            dbConnectionType.setPropertiesDefinitions(objectMapper.writeValueAsString(propertyDefinitions));
            ConnectionTypeService.getInstance().insert(dbConnectionType);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Populate Default Attachment configuration.
     *
     * @param rootGroup Root {@link Group} instance.
     */
    public static void populateAttachmentConfig(Group rootGroup) {

        Field f39 = PopDBUtils.popFieldService("allowedExtensions", "allowedExtensions", "Allowed Extensions",
                rootGroup, "Thing Attachments", "java.lang.String", 4L, true);
        PopDBUtils.popGroupField(rootGroup, f39, "pdf,doc,*");
        Field f40 = PopDBUtils.popFieldService("blockedExtensions", "blockedExtensions", "Blocked Extensions",
                rootGroup, "Thing Attachments", "java.lang.String", 4L, true);
        PopDBUtils.popGroupField(rootGroup, f40, "exe,bat");
        Field f41 = PopDBUtils.popFieldService("maxUploadSize", "maxUploadSize", "Max Upload Size (KB)", rootGroup,
                "Thing Attachments", "java.lang.Integer", 4L, true);
        PopDBUtils.popGroupField(rootGroup, f41, "2048");
        Field f42 = PopDBUtils.popFieldService("inheritThingVisibility", "inheritThingVisibility", "Inherit Thing " +
                "Visibility", rootGroup, "Thing Attachments", "java.lang.Boolean", 4L, true);
        PopDBUtils.popGroupField(rootGroup, f42, "true");
        Field f43 = PopDBUtils.popFieldService("maxImageWidth", "maxImageWidth", "Max Image Width", rootGroup, "Thing"
                + " Attachments", "java.lang.Integer", 4L, true);
        PopDBUtils.popGroupField(rootGroup, f43, "1000");
        Field f44 = PopDBUtils.popFieldService("maxImageHeight", "maxImageHeight", "Max Image Height", rootGroup,
                "Thing Attachments", "java.lang.Integer", 4L, true);
        PopDBUtils.popGroupField(rootGroup, f44, "1000");
        Field f45 = PopDBUtils.popFieldService("fileSystemPath", "fileSystemPath", "File System Path", rootGroup,
                "Thing Attachments", "java.lang.String", 4L, true);
        PopDBUtils.popGroupField(rootGroup, f45, "");
    }


    /**
     * Populate Spark connection
     *
     * @param rootGroup Root {@link Group} instance.
     */
    public static void populateAnalyticsConnection(Group rootGroup) {

        ConnectionType dbConnectionType = new ConnectionType();
        dbConnectionType.setGroup(rootGroup);
        dbConnectionType.setCode(MlConfiguration.ANALYTICS_CONNECTION_CODE);
        dbConnectionType.setDescription("Analytics Connection");
        dbConnectionType.setRequiredTestOnCreateEdit(false);

        List<Map<String, Object>> propertyDefinitions = new ArrayList<>();
        Map<String, Object> propertyDefinition;

        // Spark for analytics

        propertyDefinition = new HashMap<>();
        propertyDefinition.put("code", "masterHost");
        propertyDefinition.put("label", "Spark Master Host");
        propertyDefinition.put("type", "String");
        propertyDefinition.put("required", false);
        propertyDefinitions.add(propertyDefinition);

        propertyDefinition = new HashMap<>();
        propertyDefinition.put("code", "masterPort");
        propertyDefinition.put("label", "Spark Master Port");
        propertyDefinition.put("type", "Number");
        propertyDefinition.put("required", false);
        propertyDefinitions.add(propertyDefinition);

        propertyDefinition = new HashMap<>();
        propertyDefinition.put("code", "clusterMode");
        propertyDefinition.put("label", "Spark Cluster Mode");
        propertyDefinition.put("type", "String");
        propertyDefinition.put("required", false);
        propertyDefinitions.add(propertyDefinition);

        propertyDefinition = new HashMap<>();
        propertyDefinition.put("code", "responseTimeout");
        propertyDefinition.put("label", "Spark Response Timeout [s]");
        propertyDefinition.put("type", "String");
        propertyDefinition.put("required", false);
        propertyDefinitions.add(propertyDefinition);


        // Mongo for analytics

        propertyDefinition = new HashMap<>();
        propertyDefinition.put("code", "mongo.host");
        propertyDefinition.put("label", "Mongo Host");
        propertyDefinition.put("type", "String");
        propertyDefinition.put("required", true);
        propertyDefinitions.add(propertyDefinition);

        propertyDefinition = new HashMap<>();
        propertyDefinition.put("code", "mongo.port");
        propertyDefinition.put("label", "Mongo Port");
        propertyDefinition.put("type", "Number"); // is really an int
        propertyDefinition.put("required", true);
        propertyDefinitions.add(propertyDefinition);

        propertyDefinition = new HashMap<>();
        propertyDefinition.put("code", "mongo.dbname");
        propertyDefinition.put("label", "Mongo Database Name");
        propertyDefinition.put("type", "String"); // is really an int
        propertyDefinition.put("required", true);
        propertyDefinitions.add(propertyDefinition);

        propertyDefinition = new HashMap<>();
        propertyDefinition.put("code", "mongo.username");
        propertyDefinition.put("label", "Mongo Username");
        propertyDefinition.put("type", "String"); // is really an int
        propertyDefinition.put("required", true);
        propertyDefinitions.add(propertyDefinition);

        propertyDefinition = new HashMap<>();
        propertyDefinition.put("code", "password");
        propertyDefinition.put("label", "Mongo Password");
        propertyDefinition.put("type", "String"); // is really an int
        propertyDefinition.put("required", true);
        propertyDefinitions.add(propertyDefinition);

        propertyDefinition = new HashMap<>();
        propertyDefinition.put("code", "mongo.secure");
        propertyDefinition.put("label", "Mongo Secure");
        propertyDefinition.put("type", "Boolean");
        propertyDefinition.put("required", false);
        propertyDefinitions.add(propertyDefinition);

        // Analytics paths, etc.

        propertyDefinition = new HashMap<>();
        propertyDefinition.put("code", "extractions.path");
        propertyDefinition.put("label", "Path Extractions");
        propertyDefinition.put("type", "String");
        propertyDefinition.put("required", true);
        propertyDefinitions.add(propertyDefinition);

        propertyDefinition = new HashMap<>();
        propertyDefinition.put("code", "trainings.path");
        propertyDefinition.put("label", "Path Trainings");
        propertyDefinition.put("type", "String");
        propertyDefinition.put("required", true);
        propertyDefinitions.add(propertyDefinition);

        propertyDefinition = new HashMap<>();
        propertyDefinition.put("code", "predictions.path");
        propertyDefinition.put("label", "Path Predictions");
        propertyDefinition.put("type", "String");
        propertyDefinition.put("required", true);
        propertyDefinitions.add(propertyDefinition);

        propertyDefinition = new HashMap<>();
        propertyDefinition.put("code", "jars.path");
        propertyDefinition.put("label", "Path Jars");
        propertyDefinition.put("type", "String");
        propertyDefinition.put("required", true);
        propertyDefinitions.add(propertyDefinition);

        propertyDefinition = new HashMap<>();
        propertyDefinition.put("code", "responses.path");
        propertyDefinition.put("label", "Path Responses");
        propertyDefinition.put("type", "String");
        propertyDefinition.put("required", true);
        propertyDefinitions.add(propertyDefinition);

        propertyDefinition = new HashMap<>();
        propertyDefinition.put("code", "extraction.collection");
        propertyDefinition.put("label", "Target Collection");
        propertyDefinition.put("type", "String");
        propertyDefinition.put("required", true);
        propertyDefinitions.add(propertyDefinition);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            dbConnectionType.setPropertiesDefinitions(objectMapper.writeValueAsString(propertyDefinitions));
            ConnectionTypeService.getInstance().insert(dbConnectionType);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Populate Rest connection
     *
     * @param rootGroup Root {@link Group} instance.
     */
    public static void populateRestConnection(Group rootGroup) {
        //GENERIC REST SERVICES CONNECTION
        ConnectionType connectionType = new ConnectionType();
        connectionType.setGroup(rootGroup);
        connectionType.setCode("REST");
        connectionType.setDescription("REST Connection");
        connectionType.setRequiredTestOnCreateEdit(false);

        List<Map<String, Object>> propertyDefinitions = new ArrayList<>();

        propertyDefinitions.add(newPropertyDefinition("host", "Host", "String", true));
        propertyDefinitions.add(newPropertyDefinition("port", "Port", "Number", true));
        propertyDefinitions.add(newPropertyDefinition("contextpath", "Contextpath", "String", true));
        propertyDefinitions.add(newPropertyDefinition("apikey", "Apikey", "String", true));
        propertyDefinitions.add(newPropertyDefinition("secure", "Secure", "Boolean", false));

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            connectionType.setPropertiesDefinitions(objectMapper.writeValueAsString(propertyDefinitions));
            ConnectionTypeService.getInstance().insert(connectionType);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private static void populateKafkaConnection(Group rootGroup) {
        //GENERIC REST SERVICES CONNECTION
        ConnectionType connectionType = new ConnectionType();
        connectionType.setGroup(rootGroup);
        connectionType.setCode("KAFKA");
        connectionType.setDescription("KAFKA Broker");
        connectionType.setRequiredTestOnCreateEdit(false);

        List<Map<String, Object>> propertyDefinitions = new ArrayList<>();

        propertyDefinitions.add(newPropertyDefinition("zookeeper", "Zookeeper", "String", true));
        propertyDefinitions.add(newPropertyDefinition("server", "Server(s)", "String", true));

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            connectionType.setPropertiesDefinitions(objectMapper.writeValueAsString(propertyDefinitions));
            ConnectionTypeService.getInstance().insert(connectionType);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public static void populateGooglePubSubConnection(Group rootGroup) {
        ConnectionType connectionType = new ConnectionType();
        connectionType.setGroup(rootGroup);
        connectionType.setCode("GPubSub");
        connectionType.setDescription("Google Cloud API");
        connectionType.setRequiredTestOnCreateEdit(false);

        List<Map<String, Object>> propertyDefinitions = new ArrayList<>();
        propertyDefinitions.add(newPropertyDefinition("type", "Type", "String", true));
        propertyDefinitions.add(newPropertyDefinition("project_id", "Project Id", "String", true));
        propertyDefinitions.add(newPropertyDefinition("private_key_id", "Private Key Id", "String", true));
        propertyDefinitions.add(newPropertyDefinition("private_key", "Private Key", "String", true));
        propertyDefinitions.add(newPropertyDefinition("client_email", "Client Email", "String", true));
        propertyDefinitions.add(newPropertyDefinition("client_id", "Client Id", "String", true));
        propertyDefinitions.add(newPropertyDefinition("auth_uri", "Authentication URI", "String", true));
        propertyDefinitions.add(newPropertyDefinition("token_uri", "Token URI", "String", true));
        propertyDefinitions.add(newPropertyDefinition("auth_provider_x509_cert_url", "Authentication Provider X509 " +
                "Certificate URL", "String", true));
        propertyDefinitions.add(newPropertyDefinition("client_x509_cert_url", "Client X509 Certificate URL",
                "String", true));

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            connectionType.setPropertiesDefinitions(objectMapper.writeValueAsString(propertyDefinitions));
            ConnectionTypeService.getInstance().insert(connectionType);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public static void populatePasswordPoliciesFields(Group group) {
        Field p01 = PopDBUtils.popFieldService("passwordMinLength", "passwordMinLength", "Password Policy: Minimum Length",
            group, "Security Configuration", "java.lang.Integer", 3L, true);
        PopDBUtils.popGroupField(group, p01, "8");

        Field p02 = PopDBUtils.popFieldService("passwordMaxLength", "passwordMaxLength", "Password Policy: Maximum Length",
            group, "Security Configuration", "java.lang.Integer", 3L, true);
        PopDBUtils.popGroupField(group, p02, "128");

        Field p03 = PopDBUtils.popFieldService("passwordUppercaseRequired", "passwordUppercaseRequired", "Password Policy: One Uppercase Character Required",
            group, "Security Configuration", "java.lang.Boolean", 3L, true);
        PopDBUtils.popGroupField(group, p03, "true");

        Field p04 = PopDBUtils.popFieldService("passwordNumberRequired", "passwordNumberRequired", "Password Policy: One Numeric Character Required",
            group, "Security Configuration", "java.lang.Boolean", 3L, true);
        PopDBUtils.popGroupField(group, p04, "true");

        Field p05 = PopDBUtils.popFieldService("passwordSpecialCharRequired", "passwordSpecialCharRequired", "Password Policy: One Special Character Required",
            group, "Security Configuration", "java.lang.Boolean", 3L, true);
        PopDBUtils.popGroupField(group, p05, "true");

        Field p06 = PopDBUtils.popFieldService("passwordConsecutiveChar", "passwordConsecutiveChar", "Password Policy: Maximum Identical Consecutive Characters",
            group, "Security Configuration", "java.lang.Integer", 3L, true);
        PopDBUtils.popGroupField(group, p06, "2");

        Field p08 = PopDBUtils.popFieldService("passwordStrength", "passwordStrength", "Password Policy: Password Strength",
            group, "Security Configuration", "java.lang.String", 3L, true);
        PopDBUtils.popGroupField(group, p08, "moderate");

        Field p09 = PopDBUtils.popFieldService("passwordUseReservedWords", "passwordUseReservedWords", "Password Policy: Dictionary Enforced",
            group, "Security Configuration", "java.lang.Boolean", 3L, true);
        PopDBUtils.popGroupField(group, p09, "true");

        Field p10 = PopDBUtils.popFieldService("passwordReservedWords", "passwordReservedWords", "Password Policy: Custom Dictionary Words",
            group, "Security Configuration", "java.lang.String", 3L, true);
        PopDBUtils.popGroupField(group, p10, "root,mojix,vizix");

        Field p07 = PopDBUtils.popFieldService("passwordReusePrevious", "passwordReusePrevious", "Password Policy: Not Reusable Previous Passwords Quantity",
            group, "Security Configuration", "java.lang.Integer", 3L, true);
        PopDBUtils.popGroupField(group, p07, "4");

        Field p11 = PopDBUtils.popFieldService("passwordLoginChange", "passwordLoginChange", "Password Policy: Change Password On First/Next Login (Default)",
            group, "Security Configuration", "java.lang.Boolean", 3L, true);
        PopDBUtils.popGroupField(group, p11, "true");

        Field p12 = PopDBUtils.popFieldService("passwordExpirationDays", "passwordExpirationDays", "Password Policy: Expiration (days)",
            group, "Security Configuration", "java.lang.Integer", 3L, true);
        PopDBUtils.popGroupField(group, p12, "90");

        Field p13 = PopDBUtils.popFieldService("passwordExpirationNoticeDays", "passwordExpirationNoticeDays", "Password Policy: Expiration Notification (days)",
            group, "Security Configuration", "java.lang.Integer", 3L, true);
        PopDBUtils.popGroupField(group, p13, "3");

        Field p14 = PopDBUtils.popFieldService("passwordFailedAttempts", "passwordFailedAttempts", "Password Policy: Maximum Failed Attempts",
            group, "Security Configuration", "java.lang.Integer", 3L, true);
        PopDBUtils.popGroupField(group, p14, "3");

        Field p15 = PopDBUtils.popFieldService("passwordFailedLockTime", "passwordFailedLockTime", "Password Policy: Failed Lock (secs)",
            group, "Security Configuration", "java.lang.Integer", 3L, true);
        PopDBUtils.popGroupField(group, p15, "30");
    }
    /**
     * Main Task to Populate Data Base.
     *
     * @param args Arguments to set in command prompt.
     */
    public static void main(String args[]) {
        PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "create");
        System.getProperties().put("hibernate.cache.use_second_level_cache", "false");
        System.getProperties().put("hibernate.cache.use_query_cache", "false");
        Configuration.init(null);
        PopDBRequired popDBRequired = new PopDBRequired();
        Transaction transaction = GroupService.getGroupDAO().getSession().getTransaction();
        transaction.begin();
        popDBRequired.run();
        transaction.commit();
    }
}
