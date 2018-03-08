package com.tierconnect.riot.iot.popdb;

import com.tierconnect.riot.appcore.dao.ResourceDAO;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QResource;
import com.tierconnect.riot.appcore.entities.Resource;
import com.tierconnect.riot.appcore.entities.ResourceType;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.ResourceServiceBase;
import com.tierconnect.riot.iot.entities.*;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;

import static com.tierconnect.riot.appcore.entities.Resource.*;
import static com.tierconnect.riot.iot.popdb.PopDBRequiredIOT.*;

public class PopulateResourcesIOT{

    static Logger logger = Logger.getLogger(PopulateResourcesIOT.class);

    public static HashSet<Resource> populatePopDBRequiredIOT(Group group){


        HashSet<Resource> resources = new HashSet<>();

        logger.info("***************** BEGIN POPULATE RESOURCES PART 2 *******************");
        ResourceService resourceService = ResourceService.getInstance();
        ResourceDAO resourceDAO = ResourceServiceBase.getResourceDAO();


        //***********************************************************//
        //******************  MAIN MODULE THINGS ********************//
        //***********************************************************//
        Resource moduleThings = resourceDAO.selectBy(QResource.resource.name.eq("Things"));
        resources.add(moduleThings);

        //****** MODULE: Things ******************//
        Resource thingResource = resourceService.insert(getClassResource(group, Thing.class, moduleThings));
        thingResource.setAcceptedAttributes("r");
        Resource thingPropResource = getPropertyResource(group,
                                                         thingResource,
                                                         "editOwn",
                                                         "Always allow a user to edit his own thing",
                                                         "Allows a user to see/edit thing objects " +
                                                         "created by him that are in an upper " +
                                                         "level");

        resources.add(resourceService.insert(thingResource));
        resources.add(resourceService.insert(thingPropResource));
        resources.add(resourceService.insert(getClassResource(group, DataType.class, moduleThings)));

        //****** MODULE: Thing Types *************//
        resources.add(resourceService.insert(getClassResource(group, ThingType.class, moduleThings)));
        resources.add(resourceService.insert(getClassResource(group, ThingTypeTemplate.class, moduleThings)));
        resources.add(resourceService.insert(getClassResource(group, ThingTypeFieldTemplate.class, moduleThings)));

        //*******************************UI ARMA DE MANERA PERSONALIZADA****************************************//
        Resource moduleThingTypes = resourceService.insert(getModuleResource(group, "Thing Types", "Thing Types"));
        resources.add(moduleThingTypes);


        //***********************************************************//
        //**************** MAIN MODULE TENANT ***********************//
        //***********************************************************//

        Resource moduleTenants = resourceDAO.selectBy(QResource.resource.name.eq("Tenants"));
        resources.add(moduleTenants);

        //****** MODULE: Shifts ******************//
        resources.add(resourceService.insert(getClassResource(group, Shift.class, moduleTenants)));


        //***********************************************************//
        //****************** MAIN MODULE SERVICES *******************//
        //***********************************************************//

        Resource moduleServices = resourceDAO.selectBy(QResource.resource.name.eq("Services"));
        resources.add(moduleServices);


        //****** MODULE: Import & Export ******************//
        Resource importResource = resourceService.insert(Resource.getClassResource(group,
                ImportExport.class,
                moduleServices));
        importResource.setAcceptedAttributes("x");
        importResource.setDescription("Class level resource for ImportExport ");
        resources.add(importResource);

        //****** MODULE: Health & Status ****************//
        // At the moment it is not possible to use HealthAndStatus as resource, please enable next line when task to use
        //HealthAndStatus as resource is completed
//        resources.add(resourceService.insert(getClassResource(group, HealthAndStatus.class, moduleServices)));
        Resource moduleHealthAndStatus = Resource.getModuleResource(group, "Health & Status","healthAndStatus",moduleServices);
        moduleHealthAndStatus.setAcceptedAttributes("x");
        resources.add(resourceService.insert(moduleHealthAndStatus));

        //****** MODULE: Logs ***************************//
        Resource moduleLogs = Resource.getModuleResource(group, "Logs", "logs",moduleServices);
        moduleLogs.setAcceptedAttributes("x");
        resources.add(resourceService.insert(moduleLogs));


        //***********************************************************//
        //******************** MAIN MODULE MODEL ********************//
        //***********************************************************//

        Resource moduleModel = resourceDAO.selectBy(QResource.resource.name.eq("Model"));
        resources.add(moduleModel);
        Resource moduleMapMaker = Resource.getModuleResource(group, "Map Maker", "mapMaker",moduleModel);
        moduleMapMaker.setAcceptedAttributes("x");
        resources.add(resourceService.insert(moduleMapMaker));

        //****** MODULE: Zones ***************************//
        resources.add(resourceService.insert(Resource.getClassResource(group, Zone.class, moduleModel)));

        //****** MODULE: Zone Types   **************************//
        resources.add(resourceService.insert(Resource.getClassResource(group, ZoneType.class, moduleModel)));

        //****** MODULE: Zone Groups ***************************//
        resources.add(resourceService.insert(Resource.getClassResource(group, ZoneGroup.class, moduleModel)));

        //****** MODULE: Zone Point  **************************//
        resources.add(resourceService.insert(Resource.getClassResource(group, ZonePoint.class, moduleModel)));

        //****** MODULE: Logical Readers  *********************//
        resources.add(resourceService.insert(Resource.getClassResource(group, LocalMap.class, moduleModel)));
        resources.add(resourceService.insert(Resource.getClassResource(group, LogicalReader.class, moduleModel)));


        //***********************************************************//
        //******************** MAIN MODULE MODEL ********************//
        //***********************************************************//
        Resource moduleGateway = resourceDAO.selectBy(QResource.resource.name.eq("Gateway"));
        resources.add(moduleGateway);
        resources.add(resourceService.insert(getClassResource(group, EdgeboxRule.class, moduleGateway)));
        resources.add(resourceService.insert(getClassResource(group, Parameters.class, moduleGateway)));

        //****** MODULE: MODULE BRIDGES & RULES  *********************//
        Resource bridgesAndRules = resourceService.insert(getModuleResource(group,
                                                                            "Bridges & Rules",
                RESOURCE_BRIDGES_RULES_NAME,
                                                                            moduleGateway));
        resources.add(bridgesAndRules);

        //****** MODULE: CLASS EDGE BRIDGE  *********************//
        resources.add(resourceService.insert(getClassResource(group, Edgebox.class, bridgesAndRules)));

        //****** MODULE: MODULE SCHEDULED RULE  *********************//
        Resource scheduledRule = resourceService.insert(getModuleResource(group,
                "Scheduled Rule",
                RESOURCE_SCHEDULED_RULE_NAME,
                moduleGateway));
        resources.add(scheduledRule);
        resources.add(resourceService.insert(getClassResource(group, ScheduledRule.class, scheduledRule)));

        //****** MODULE: MODULE FLOWS  *********************//
        Resource moduleFlows = Resource.getModuleResource(group, "Flows", "Flows",moduleGateway);
        resources.add(resourceService.insert(moduleFlows));
        Resource moduleStreamApp = Resource.getModuleResource(group, "Stream App", "streamApp",moduleGateway);
        resources.add(resourceService.insert(moduleStreamApp));


        //***********************************************************//
        //******************** MAIN MODULE REPORTS ******************//
        //***********************************************************//

        //****** MODULE: CLASS ANALITICS  *********************//
        Resource moduleAnalytics = resourceDAO.selectBy(QResource.resource.name.eq("Analytics"));
        resources.add(moduleAnalytics);

        ///******RESOURCE REPORTS ****************************//
        Resource reports = resourceService.insert(getModuleResource(group,
                "Reports",
                "Reports",
                moduleAnalytics));
        resources.add(reports);

        Resource reportDefinitionResource = resourceService.insert(getClassResource(group,
                                                                                    ReportDefinition.class,
                                                                                    moduleAnalytics));
        reportDefinitionResource.setAcceptedAttributes(new LinkedHashSet<>(Arrays.asList("r",
                                                                                         "i",
                                                                                         "u",
                                                                                         "d",
                                                                                         "a",
                                                                                         "x",
                                                                                         "p")));
        resources.add(reportDefinitionResource);

        resources.add(resourceService.insert(Resource.getClassResource(group, MlExtraction.class,
                moduleAnalytics)));
        resources.add(resourceService.insert(Resource.getClassResource(group, MlModel.class,
                moduleAnalytics)));
        resources.add(resourceService.insert(Resource.getClassResource(group, MlPrediction.class,
                moduleAnalytics)));
        resources.add(resourceService.insert(Resource.getClassResource(group, MlBusinessModel.class,
                moduleAnalytics)));

        //****** MODULE: PROPERTY Edit own report  *********************//
        Resource rEditOwn = resourceService.insert(getPropertyResource(group,
                                                                       reportDefinitionResource,
                                                                       "editOwn",
                                                                       thing_editOwn_label,
                                                                       thing_editOwn_description));
        rEditOwn.setLabel("Edit own report");
        rEditOwn.setParent(reportDefinitionResource);
        resources.add(rEditOwn);

        //****** MODULE: PROPERTY Set email recipients *****************//
        Resource emailRecipients = resourceService.insert(getPropertyResource(group,
                                                                              reportDefinitionResource,
                                                                              "emailRecipients",
                                                                              reportDefinition_emailRecipients_label,
                                                                              reportDefinition_emailRecipients_description));
        emailRecipients.setLabel("Set email recipients");
        emailRecipients.setAcceptedAttributes("ru");
        emailRecipients.setParent(reportDefinitionResource);
        resources.add(emailRecipients);

        Resource tableScriptEdition = resourceService.insert(Resource.getPropertyResource(group,
                reportDefinitionResource,
                "editTableScript",
                reportDefinition_editTableScript_label,
                reportDefinition_editTableScript_description));
        tableScriptEdition.setLabel("Edit Table Script");
        tableScriptEdition.setAcceptedAttributes("u");
        tableScriptEdition.setParent(reportDefinitionResource);
        resources.add(tableScriptEdition);


        //***********************************************************//
        //*************** MAIN MODULE REPORTS INSTANCES *************//
        //***********************************************************//
        Resource moduleReportInstances = resourceService.insert(getModuleResource(group,
                                                                                  Resource.REPORT_INSTANCES_MODULE,
                                                                                  "Report Instances"));
        resources.add(moduleReportInstances);


        //****** MODULE: PROPERTY Thing Property *****************//
        Resource rdInlineEdit = resourceService.insert(getPropertyResource(group,
                                                                           reportDefinitionResource,
                                                                           "inlineEdit",
                                                                           reportDefinition_inlineEdit_label,
                                                                           reportDefinition_inlineEdit_description));
        rdInlineEdit.setLabel("Thing Property");
        resources.add(rdInlineEdit);

        //****** MODULE: PROPERTY Thing Associate & Disassociate *****************//
        Resource rdAssignUnAssignThing = resourceService.insert(getPropertyResource(group,
                                                                                    reportDefinitionResource,
                                                                                    "assignUnAssignThing",
                                                                                    reportDefinition_assignUnAssignThing_label,
                                                                                    reportDefinition_assignUnAssignThing_description));
        rdAssignUnAssignThing.setLabel("Thing Associate & Disassociate");
        resources.add(rdAssignUnAssignThing);


        Resource inLineEdit = resourceService.insert(getModuleResource(group,
                                                                       "Inline Edit",
                                                                       "reportDefinition_inlineEditGroup",
                                                                       reportDefinitionResource.getParent()));
        resources.add(inLineEdit);

        rdAssignUnAssignThing.setParent(inLineEdit);
        rdInlineEdit.setParent(inLineEdit);
        //****************************** Populate Folders **********************
        Resource moduleFolder = resourceDAO.selectBy(QResource.resource.name.eq("Folder"));
        resources.add(moduleFolder);
        Resource moduleReportFolder = Resource.getModuleResource(group, "Report Folder", "reportFolder",moduleFolder);
        moduleReportFolder.setAcceptedAttributes("uid");
        resources.add(resourceService.insert(moduleReportFolder));


        // region Module Retail

        //****************************************************//
        //*************** MODULE RETAIL *************//
        //****************************************************//

        populateRetailAppResources(group, resources);

        // endregion

        //****************************************************//
        //*************** MAIN MODULE BLOCKCHAIN *************//
        //****************************************************//
        Resource moduleBlockchain = resourceService.insert(Resource.getModuleResource(group, "Blockchain", "Blockchain"));
        resources.add(moduleBlockchain);

        // Smart Contract Definition Resource
        Resource smartContractDefResource = Resource.getClassResource(group, SmartContractDefinition.class, moduleBlockchain);
        smartContractDefResource.setTreeLevel(2);
        resources.add(resourceService.insert(smartContractDefResource));

        // Smart Contract Party Resource
        Resource smartContractPartyResource = Resource.getClassResource(group, SmartContractParty.class, moduleBlockchain);
        smartContractPartyResource.setTreeLevel(2);
        resources.add(resourceService.insert(smartContractPartyResource));

        // Smart Contract Config Resource
        Resource smartContractConfigResource = Resource.getClassResource(group, SmartContractConfig.class, moduleBlockchain);
        smartContractConfigResource.setTreeLevel(2);
        resources.add(resourceService.insert(smartContractConfigResource));


        Resource smartContractResource = new Resource(group, "smartcontract", "r");
        smartContractResource.setParent(moduleBlockchain);
        smartContractResource.setTreeLevel(2);
        smartContractResource.setLabel(smartContractResource.getName());
        smartContractResource.setFqname(smartContractResource.getName());
        smartContractResource.setDescription("smart contract things");
        smartContractResource.setType(ResourceType.CLASS.getId());
        resources.add(resourceService.insert(smartContractResource));

        logger.info("***************** END POPULATE RESOURCES PART 2 *******************");
        return resources;
    }

    public static void populateRetailAppResources(Group group, HashSet<Resource> resources) {
        logger.info("***************** START POPULATE RETAIL RESOURCES *******************");
        ResourceService resourceService = ResourceService.getInstance();
        Resource moduleRetail = resourceService.insert(Resource.getModuleResource(group, "Mojix Retail App", "Retail"));
        resources.add(moduleRetail);

        //****** MODULE: Replenishment ***************************//
        Resource moduleReplenishment = Resource.getModuleResource(group, "Replenishment Tile", "Replenishment",moduleRetail);
        moduleReplenishment.setAcceptedAttributes("x");
        moduleReplenishment.setDescription("Replenishment Tile");
        resources.add(resourceService.insert(moduleReplenishment));

        //****** MODULE: Hot Replenishment ***************************//
        Resource moduleHotReplenishment = Resource.getModuleResource(group, "Hot Replenishment Tile", "HotReplenishment",moduleRetail);
        moduleHotReplenishment.setAcceptedAttributes("x");
        moduleHotReplenishment.setDescription("Hot Replenishment Tile");
        resources.add(resourceService.insert(moduleHotReplenishment));

        //****** MODULE: Sell Thru Replenishment ***************************//
        Resource moduleSellThruReplenishment = Resource.getModuleResource(group, "Sell Thru Replenishment Tile", "SellThruReplenishment",moduleRetail);
        moduleSellThruReplenishment.setAcceptedAttributes("x");
        moduleSellThruReplenishment.setDescription("Sell Thru Replenishment Tile");
        resources.add(resourceService.insert(moduleSellThruReplenishment));

        //****** MODULE: Dressing Room ***************************//
        Resource moduleDressingRoom = Resource.getModuleResource(group, "Dressing Room Tile", "DressingRoom",moduleRetail);
        moduleDressingRoom.setAcceptedAttributes("x");
        moduleDressingRoom.setDescription("Dressing Room Tile");
        resources.add(resourceService.insert(moduleDressingRoom));

        logger.info("***************** END POPULATE RETAIL RESOURCES *******************");
    }
}
