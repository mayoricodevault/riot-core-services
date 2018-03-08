package com.tierconnect.riot.migration.older;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Created by fflores on 9/29/16.
 */
@Deprecated
public class V_040305_040400 implements MigrationStepOld {
    static Logger logger = Logger.getLogger(V_040305_040400.class);

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(40305);
    }

    @Override
    public int getToVersion() {
        return 40400;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateLdapParamater();
        migrateAleBridgeTemplateConfiguration();
        migrateAleBridgeConfiguration();
        migratePropertyName();
        migrateGPIOThingType();
    }

    @Override
    public void migrateSQLAfter() throws Exception {
    }

    /**
     * migrate ldapValidateUserCreation parameter for LDAP/AD Authentication
     */
    private static void migrateLdapParamater() {
        Group rootGroup = GroupService.getInstance().getRootGroup();
        Field field = PopDBUtils.popFieldService("ldapValidateUserCreation", "ldapValidateUserCreation", "LDAP/AD Validate User Creation",
                rootGroup, "Security Configuration", "java.lang.Boolean", 3L, true);
        PopDBUtils.popGroupField(rootGroup, field, "false");
    }

    /**
     * migrateAleBridgeTemplateConfiguration
     * Update the "edge" parameters
     */
    private static void migrateAleBridgeTemplateConfiguration(){
        String bridgeType = "BRIDGE_TYPE";
        Parameters parameters =  ParametersService.getInstance().getByCategoryAndCode(bridgeType, "edge");
        String value = parameters.getValue();
        String valueToAdd = "";
        if (!value.contains("socketTimeout")){
            valueToAdd = ",\"socketTimeout\":60000";
        }
        if (!value.contains("dynamicTimeoutRate")){
            valueToAdd = valueToAdd + ",\"dynamicTimeoutRate\":0";
        }
        if (!value.contains("send500ErrorOnTimeout")){
            valueToAdd = valueToAdd + ",\"send500ErrorOnTimeout\":{\"active\":0}";
        }
        valueToAdd = valueToAdd + "}";
        String replaceParameters = value.substring(0,value.length()-1) + valueToAdd;
        parameters.setValue(replaceParameters);
        ParametersService.getInstance().update(parameters);
    }

    /**
     * migrateAleBridgeConfiguration
     * migrate the Ale Bridge Configuration with new udfs template
     */
    private static void migrateAleBridgeConfiguration(){
        logger.info("Migrating Ale Bridge configuration...");
        EdgeboxService edgeboxService  = EdgeboxService.getInstance();
        List<Edgebox> edgebox = edgeboxService.selectAll();
         for (Edgebox edge: edgebox){
             if (StringUtils.equals(edge.getType(), "edge")){
                 setAleBridgeConfiguration(edgeboxService, edge,"socketTimeout", 60000, false);
                 setAleBridgeConfiguration(edgeboxService, edge,"dynamicTimeoutRate", 0, false);
                 setAleBridgeConfiguration(edgeboxService, edge,"send500ErrorOnTimeout", 0, true);
             }
         }
    }

    public static void migratePropertyName(){
        migrateReportFiler();
        migrateReportProperty();
        migrateReportRule();
        migrateReportGroupBy();
    }

    public static void migrateReportFiler(){
        List<ReportFilter> reportFilters = ReportFilterService.getInstance().getFiltersByPropertyName("localMap.id");
        for (ReportFilter reportFilter: reportFilters) {
            reportFilter.setPropertyName("zoneLocalMap.id");
        }
    }

    public static void migrateReportProperty(){
        List<ReportProperty> reportProperties = ReportPropertyService.getInstance().getPropertiesByPropertyName("localMap.id");
        for (ReportProperty reportProperty: reportProperties) {
            reportProperty.setPropertyName("zoneLocalMap.id");
        }
    }

    public static void migrateReportRule(){
        List<ReportRule> reportRules = ReportRuleService.getInstance().getRuleByPropertyName("localMap.id");
        for (ReportRule reportRule: reportRules) {
            reportRule.setPropertyName("zoneLocalMap.id");
        }
    }

    public static void migrateReportGroupBy(){
        List<ReportGroupBy> reportGroupBys = ReportGroupByService.getInstance().getGroupByPropertyName("localMap.id");
        for (ReportGroupBy reportGroupBy: reportGroupBys) {
            reportGroupBy.setPropertyName("zoneLocalMap.id");
        }
    }

    /**
     *
     * @param edgeboxService  edgebox Service
     * @param edgebox edgebox
     * @param udfConfiguration udf Configuration
     * @param udfValue udf Value
     * @param isBooleanValue Boolean Value
     * Update the new udfs ale bridge configuration
     */
    public static void setAleBridgeConfiguration(EdgeboxService edgeboxService, Edgebox edgebox , String udfConfiguration, int udfValue, boolean isBooleanValue){
        String edgeboxConfig = edgebox.getConfiguration();
        if (!edgeboxConfig.contains(udfConfiguration)) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                if (StringUtils.isNotEmpty(edgeboxConfig)){
                    JsonNode rootNode = mapper.readTree(edgeboxConfig);
                    if (isBooleanValue){
                        ObjectNode node = mapper.createObjectNode();
                        node.put("active", 0);
                        ((ObjectNode) rootNode).put(udfConfiguration, node);
                    } else {
                        ((ObjectNode) rootNode).put(udfConfiguration, udfValue);
                    }
                    edgeboxConfig = rootNode.toString();
                    edgebox.setConfiguration(edgeboxConfig);
                    edgeboxService.update(edgebox);
                    logger.info(udfConfiguration + " default configuration has been added to aleBridge configuration");
                } else {
                    logger.info(edgebox.getCode() + " without Edgebox Config");
                }

            } catch (Exception e) {
                logger.error("Error when update AleBridge Configuration", e);
            }
        }
    }

    /**
     * Delete GPIO Thing Type
     */
    public static void migrateGPIOThingType (){
        ThingTypeTemplate thingTypeTemplateGPIO = getThingTypeTemplateGPIO();
        if (thingTypeTemplateGPIO != null){
            ThingTypeTemplateService.getInstance().delete(thingTypeTemplateGPIO);
        }
    }

    /**
     * get GPIO Thing Type Template
     * @return
     */
    public static ThingTypeTemplate getThingTypeTemplateGPIO (){
        HibernateQuery query = ThingTypeTemplateService.getThingTypeTemplateDAO().getQuery();
        return query.where(QThingTypeTemplate.thingTypeTemplate.name.eq("GPIO"))
                .uniqueResult(QThingTypeTemplate.thingTypeTemplate);
    }
}