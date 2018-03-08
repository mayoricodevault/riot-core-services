package com.tierconnect.riot.iot.services;

/**
 * Created by rchirinos on 1/27/16.
 */

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.QReportDefinitionConfig;
import com.tierconnect.riot.iot.entities.ReportDefinition;
import com.tierconnect.riot.iot.entities.ReportDefinitionConfig;
import com.tierconnect.riot.iot.entities.ValidationBean;
import com.tierconnect.riot.iot.reports_integration.TableScriptExecution;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tierconnect.riot.commons.Constants.REPORT_TYPE_TABLE_CONNECTION;
import static com.tierconnect.riot.commons.Constants.REPORT_TYPE_TABLE_SCRIPT;


public class ReportDefinitionConfigService extends ReportDefinitionConfigServiceBase
{

    private static final Map<String, List<String>> reportConfigurationValid;
    private static final String ZONEBORDERS = "ZONEBORDERS";
    private static final String WEIGHTINVERTED = "WEIGHTINVERTED";
    private static final String SCRIPT = "SCRIPT";
    private static final String CONNECTION = "CONNECTION";
    private static final String PAGESIZE = "PAGESIZE";

    static {
        reportConfigurationValid = new HashMap<>(5);
        reportConfigurationValid.put(ZONEBORDERS, Arrays.asList(Constants.REPORT_TYPE_MAP, Constants.REPORT_TYPE_MAP_SUMMARY));
        reportConfigurationValid.put(WEIGHTINVERTED, Arrays.asList(Constants.REPORT_TYPE_MAP, Constants.REPORT_TYPE_MAP_SUMMARY));
        reportConfigurationValid.put(SCRIPT, Arrays.asList(Constants.REPORT_TYPE_TABLE_SCRIPT, Constants.REPORT_TYPE_TABLE_CONNECTION));
        reportConfigurationValid.put(CONNECTION, Collections.<String>singletonList(Constants.REPORT_TYPE_TABLE_CONNECTION));
        reportConfigurationValid.put(PAGESIZE, Arrays.asList(Constants.REPORT_TYPE_TABLE_DETAIL, Constants.REPORT_TYPE_TABLE_HISTORY,
                Constants.REPORT_TYPE_TABLE_CONNECTION, Constants.REPORT_TYPE_TABLE_SCRIPT, Constants.REPORT_TYPE_MAP,
                Constants.REPORT_TYPE_MAP_HISTORY));
    }


    /********************************************
     * Set properties
     * @param reportDefConf
     * @param reportDefConfMap
     * @param reportDefinition
     ********************************************/
    public void setProperties(ReportDefinitionConfig reportDefConf, Map<String, Object> reportDefConfMap, ReportDefinition reportDefinition) {
        reportDefConf.setKeyType((String) reportDefConfMap.get("keyType"));
        reportDefConf.setKeyValue((String) reportDefConfMap.get("keyValue"));
        reportDefConf.setReportDefinition(reportDefinition);
    }

    /************************************************
     * Valida Report Definition Config
     ***********************************************/
    public ValidationBean validaReportDefinitionConfig(Map<String, Object> reportDefConfMap, ReportDefinition reportDefinition) {
        ValidationBean validation = new ValidationBean();
        if (reportDefConfMap != null) {
            if (reportDefConfMap.get("keyType") == null || reportDefConfMap.get("keyValue") == null) {
                validation.setErrorDescription("Invalid Map configuration");
                return validation;
            }
            String keyType = reportDefConfMap.get("keyType").toString();
            String keyValue = reportDefConfMap.get("keyValue").toString();

            if (Objects.equals(PAGESIZE, keyType)) {
                if (!Utilities.isNumber(keyValue)) {
                    validation.setErrorDescription("Invalid 'Page size'.");
                    return validation;
                }
                BigDecimal valueInteger = new BigDecimal(keyValue);
                if (valueInteger.compareTo(BigDecimal.ONE) < 0 || valueInteger.compareTo(BigDecimal.valueOf(10000)) > 0) {
                    validation.setErrorDescription("Page Size must be between 1 and 10000.");
                    return validation;
                }
            }

            boolean isTableConnection = REPORT_TYPE_TABLE_CONNECTION.equals(reportDefinition.getReportType());
            boolean isTableScript = REPORT_TYPE_TABLE_SCRIPT.equals(reportDefinition.getReportType());
            if (!isTableConnection && !isTableScript) {//other reports
                return validation;
            }
            if (Objects.equals(SCRIPT, keyType)) {
                if (!StringUtils.isEmpty(keyValue)) {
                    if (isTableScript) {//validate commands
                        List<String> commands = new ArrayList<>();
                        String functionMinified = TableScriptExecution.minifyFunction(keyValue);
                        for (String data : this.getListForbiddenCommands()) {
                            if (TableScriptExecution.hasForbiddenSentences(functionMinified, data)) {
                                commands.add(data);
                            }
                        }
                        if (!commands.isEmpty()) {
                            validation.setErrorDescription("'Report Script' contains forbidden commands: " + String.join(",", commands));
                        }
                    }
                } else {
                    validation.setErrorDescription("'Report Script' is required and could not be empty");
                }
            } else if (isTableConnection && Objects.equals(CONNECTION, keyType)) {
                Connection connection;
                if (StringUtils.isEmpty(keyValue)
                        || (connection = ConnectionService.getInstance().get(Long.valueOf(keyValue))) == null
                        || !connection.getConnectionType().getCode().equals("DBConnection")) {
                    validation.setErrorDescription("'Table Connection contains an invalid datasource'");
                }
            }
        }
        return validation;
    }

    /**
     * Method to get the list of forbidden commands
     */
    private List<String> getListForbiddenCommands()
    {
        List<String> forbiddenMongoCommands = new ArrayList<>();
        forbiddenMongoCommands.add("bulkWrite");
        forbiddenMongoCommands.add("deleteOne");
        forbiddenMongoCommands.add("deleteMany");
        forbiddenMongoCommands.add("drop");
        forbiddenMongoCommands.add("dropIndex");
        forbiddenMongoCommands.add("dropIndexes");
        forbiddenMongoCommands.add("findAndModify");
        forbiddenMongoCommands.add("findOneAndDelete");
        forbiddenMongoCommands.add("findOneAndReplace");
        forbiddenMongoCommands.add("findOneAndUpdate");
        forbiddenMongoCommands.add("insert");
        forbiddenMongoCommands.add("insertOne");
        forbiddenMongoCommands.add("insertMany");
        forbiddenMongoCommands.add("replaceOne");
        forbiddenMongoCommands.add("remove");
        forbiddenMongoCommands.add("renameCollection");
        forbiddenMongoCommands.add("save");
        forbiddenMongoCommands.add("update");
        forbiddenMongoCommands.add("updateOne");
        forbiddenMongoCommands.add("updateMany");

        //Collection Methods
        forbiddenMongoCommands.add("copyTo");
        forbiddenMongoCommands.add("createIndex");
        forbiddenMongoCommands.add("ensureIndex");

        //Database Methods
        forbiddenMongoCommands.add("runCommand");
        forbiddenMongoCommands.add("cloneCollection");
        forbiddenMongoCommands.add("cloneDatabase");
        forbiddenMongoCommands.add("copyDatabase");
        forbiddenMongoCommands.add("createCollection");
        forbiddenMongoCommands.add("dropDatabase");
        forbiddenMongoCommands.add("fsyncLock");
        forbiddenMongoCommands.add("fsyncUnlock");
        forbiddenMongoCommands.add("getSiblingDB");

        //User Management
        forbiddenMongoCommands.add("createUser");
        forbiddenMongoCommands.add("updateUser");
        forbiddenMongoCommands.add("changeUserPassword");
        forbiddenMongoCommands.add("removeUser");
        forbiddenMongoCommands.add("dropAllUsers");
        forbiddenMongoCommands.add("dropUser");
        forbiddenMongoCommands.add("grantRolesToUser");
        forbiddenMongoCommands.add("revokeRolesFromUser");
        forbiddenMongoCommands.add("getUser");
        forbiddenMongoCommands.add("getUsers");

        //Role Management
        forbiddenMongoCommands.add("createRole");
        forbiddenMongoCommands.add("updateRole");
        forbiddenMongoCommands.add("dropRole");
        forbiddenMongoCommands.add("dropAllRoles");
        forbiddenMongoCommands.add("grantPrivilegesToRole");
        forbiddenMongoCommands.add("revokePrivilegesFromRole");
        forbiddenMongoCommands.add("grantRolesToRole");
        forbiddenMongoCommands.add("revokeRolesFromRole");
        forbiddenMongoCommands.add("getRole");
        forbiddenMongoCommands.add("getRoles");
        return forbiddenMongoCommands;
    }

    /***************************************
     * Update Report Definition Config
     ***************************************/
    public void updateReportDefinitionConfig(
            ReportDefinition reportDefinition,
            List<Map<String, Object>> reportDefConfBy) throws UserException
    {
        List<ReportDefinitionConfig> reportDefConfList = new ArrayList<>();

        if(reportDefConfBy != null) {
            // remove old configuration
            reportDefConfBy = removeOldConfiguration(reportDefinition, reportDefConfBy);

            // validate configuration
            Map<String, Object> newReportConfigMap = validateConfigMap(reportDefinition, reportDefConfBy);

            // get old configuration
            Map<String, ReportDefinitionConfig> oldReportConfigMap = reportDefinition.getReportDefinitionConfig()
                    .stream().collect(Collectors.toMap(ReportDefinitionConfig::getKeyType, Function.identity()));
            reportDefinition.getReportDefinitionConfig().clear();

            // remove configuration
            for (Map.Entry<String, ReportDefinitionConfig> oldConfig : oldReportConfigMap.entrySet()) {
                if (!newReportConfigMap.containsKey(oldConfig.getKey())) {
                    ReportDefinitionConfigService.getInstance().delete(oldConfig.getValue());
                }
            }

            // update configuration
            for (Map<String, Object> reportDefConfByMap : reportDefConfBy) {
                ReportDefinitionConfig reportConfDef;

                if (!oldReportConfigMap.containsKey(reportDefConfByMap.get("keyType"))) {
                    reportConfDef = new ReportDefinitionConfig();
                } else {
                    reportConfDef = oldReportConfigMap.get(reportDefConfByMap.get("keyType"));
                }
                reportConfDef.setKeyType((String) reportDefConfByMap.get("keyType"));
                reportConfDef.setKeyValue((String) reportDefConfByMap.get("keyValue"));
                reportConfDef.setReportDefinition(reportDefinition);
                reportDefConfList.add(reportConfDef);
            }
        }
        reportDefinition.setReportDefinitionConfig(reportDefConfList);
    }

    private List<Map<String, Object>> removeOldConfiguration(ReportDefinition reportDefinition,
                                                             List<Map<String, Object>> reportDefConfBy) {

        List<Map<String, Object>> newReportDefConfBy = new ArrayList<>();
        for (Map<String, Object> reportDefConfByMap : reportDefConfBy) {
            if (reportConfigurationValid.containsKey(reportDefConfByMap.get("keyType"))
                    && reportConfigurationValid.get(reportDefConfByMap.get("keyType")).contains(reportDefinition.getReportType())) {
                newReportDefConfBy.add(reportDefConfByMap);
            }
        }
        return newReportDefConfBy;
    }

    private Map<String, Object> validateConfigMap(ReportDefinition reportDefinition,
                                                  List<Map<String, Object>> reportDefConfBy) {
        // new report Configuration
        Map<String, Object> newReportConfigMap = new HashMap<>();

        for (Map<String, Object> reportDefConfByMap : reportDefConfBy) {
            ValidationBean validation = ReportDefinitionConfigService.getInstance()
                    .validaReportDefinitionConfig(reportDefConfByMap, reportDefinition);
            if (validation.isError()) {
                throw new UserException(validation.getErrorDescription());
            }
            newReportConfigMap.put((String) reportDefConfByMap.get("keyType"), reportDefConfByMap);
        }
        return newReportConfigMap;
    }

    @Override public void validateInsert(ReportDefinitionConfig reportDefinitionConfig) {
        super.validateInsert(reportDefinitionConfig);
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QReportDefinitionConfig.reportDefinitionConfig.keyType.eq(reportDefinitionConfig.getKeyType()));
        be = be.and(QReportDefinitionConfig.reportDefinitionConfig.reportDefinition.eq(reportDefinitionConfig.getReportDefinition()));
        if (reportDefinitionConfig.getId() == null && getReportDefinitionConfigDAO().selectBy(be) != null) {
            throw new UserException("Report definition config already exists.");
        }
    }

    @Override public void validateUpdate(ReportDefinitionConfig reportDefinitionConfig) {
        super.validateUpdate(reportDefinitionConfig);
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QReportDefinitionConfig.reportDefinitionConfig.keyType.eq(reportDefinitionConfig.getKeyType()));
        be = be.and(QReportDefinitionConfig.reportDefinitionConfig.reportDefinition.eq(reportDefinitionConfig.getReportDefinition()));
        if (reportDefinitionConfig.getId() == null && getReportDefinitionConfigDAO().selectBy(be) != null) {
            throw new UserException("Report definition config already exists.");
        }
    }
}

