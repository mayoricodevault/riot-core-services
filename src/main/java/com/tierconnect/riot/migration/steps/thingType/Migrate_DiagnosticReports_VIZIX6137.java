package com.tierconnect.riot.migration.steps.thingType;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.popdb.PopDBBaseUtils;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.MongoExecutionException;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import static com.tierconnect.riot.commons.Constants.REPORT_TYPE_TABLE_SCRIPT;
import static com.tierconnect.riot.commons.Constants.THING_TYPE_DATA_TYPE;

/**
 * Created by rchirinos on 07/07/2017
 */
public class Migrate_DiagnosticReports_VIZIX6137 implements MigrationStep {

    private static Logger logger = Logger.getLogger(Migrate_DiagnosticReports_VIZIX6137.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        //Update Thing Type of de Core Bridge
        updateFieldsInThingTypeTemplateCB();
        updateThingTypeCB();
        //Update Existent Table Script Reports
        //Check if mojix group exists or not. If it exists means that this folder can be added
        Group group = GroupService.getInstance().getByCode("mojix");
        if( group != null ) {
            Folder diagnostic = createFolderDiagnosticRoot();
            updateDiagnosticReport(diagnostic);
            createSummaryDiagnosticReport(diagnostic);
            organizeDiagnosticReports(diagnostic);
        }
        organizeTutorialReports();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    /**
     * Update fields in  Thing Type Template Core Bridge
     * @return ThingTypeTemplate object
     * @throws NonUniqueResultException
     */
    public ThingTypeTemplate updateFieldsInThingTypeTemplateCB() throws NonUniqueResultException {
        ThingTypeTemplate template = ThingTypeTemplateService.getInstance().getByName("CoreBridge");
        if (template != null ){
            ThingTypeFieldTemplateService tttInstance = ThingTypeFieldTemplateService.getInstance();
            ThingTypeFieldTemplate newPeriodTo = tttInstance.create(
                    "que_period_to", "que_period_to", "", "",
                    DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER),
                    THING_TYPE_DATA_TYPE, true, template, "");
            if (template.getThingTypeFieldTemplate() != null) {
                template.getThingTypeFieldTemplate().add(newPeriodTo);
                for (ThingTypeFieldTemplate ttFieldTemplate : template.getThingTypeFieldTemplate()) {
                    if (ttFieldTemplate.getName().equals("que_poll_idletime")) {
                        ttFieldTemplate.setName("que_idle_count");
                        ttFieldTemplate.setDescription("que_idle_count");
                        tttInstance.update(ttFieldTemplate);
                    }
                    if (ttFieldTemplate.getName().equals("que_size_pop")) {
                        ttFieldTemplate.setName("que_pop_count");
                        ttFieldTemplate.setDescription("que_pop_count");
                        tttInstance.update(ttFieldTemplate);
                    }
                }
                ThingTypeTemplateService.getInstance().update(template);
            }
        }
        return template;
    }

    /**
     * Update Thing Type "coreBridge" in order to update new name of UDFs
     */
    public void updateThingTypeCB() {
        ThingType thingType = ThingTypeService.getInstance().getByCodeAndGroup("coreBridge", GroupService.getInstance().getRootGroup());
        if( thingType != null ) {
            ThingTypeFieldService ttfInstance = ThingTypeFieldService.getInstance();
            ThingTypeField ttField = ttfInstance.insertThingTypeField(
                    thingType, "que_period_to", "", "", THING_TYPE_DATA_TYPE,
                    DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER), true, "",null);
            thingType.getThingTypeFields().add(ttField);
            for(ThingTypeField tTypeField : thingType.getThingTypeFields()) {
                if (tTypeField.getName().equals("que_poll_idletime")) {
                    tTypeField.setName("que_idle_count");
                    ttfInstance.update(tTypeField);
                }
                if (tTypeField.getName().equals("que_size_pop")) {
                    tTypeField.setName("que_pop_count");
                    ttfInstance.update(tTypeField);
                }
            }
            ThingTypeService.getInstance().update(thingType);
        }
    }


    /**
     * Update Diagnostic Reports
     * @param  folderDiagnostic Folder of diagnostic
     * @return Report Definition
     */
    public ReportDefinition updateDiagnosticReport(Folder folderDiagnostic) {
        ReportDefinition rep = null;
        try{
            //Change Name of the Report
            ReportDefinitionService repDefInstance =  ReportDefinitionService.getInstance();
            PopDBBaseUtils popdbUtil = new PopDBBaseUtils("CoreTenant", new Date());
            rep = ReportDefinitionService.getInstance().getByNameAndType("ViZix TimeSeries Statistic CoreBridge", REPORT_TYPE_TABLE_SCRIPT);
            if (rep!=null ){
                rep.setName("ViZix Queues Detailed");
                rep.setDescription("ViZix Queues Detailed");
                rep.setFolder(folderDiagnostic);
                //Filters
                for(ReportFilter reportFilter : rep.getReportFilter()){
                    if(reportFilter.getPropertyName().equals("thingType.id")) {
                        reportFilter.setDisplayOrder(2F);
                    }
                    if(reportFilter.getPropertyName().equals("relativeDate")) {
                        reportFilter.setDisplayOrder(3F);
                    }
                    if(reportFilter.getPropertyName().equals("startDate")) {
                        reportFilter.setDisplayOrder(4F);
                    }
                    if(reportFilter.getPropertyName().equals("endDate")) {
                        reportFilter.setDisplayOrder(5F);
                    }
                }
                rep.getReportFilter().add(
                        ReportFilterService.getInstance().createReportFilter(
                                "Tenant Group","group.id", "1", "<", null, true, null, rep ));
                rep.getReportFilter().add(
                        ReportFilterService.getInstance().createReportFilter(
                                "Serial Number","serial", "6", "~", null, true, null, rep ));

                //Script
                String script = popdbUtil.readTableScript("ReportScript-StatisticCoreBridge.js");
                rep.getReportDefConfigItem("SCRIPT").setKeyValue(script);
                repDefInstance.update(rep);
                //Update the script
                popdbUtil.insertTableScript("ReportScript-StatisticCoreBridge.js","ViZix Queues Detailed");
            }
        } catch(Exception e) {
            logger.error("Error populating ReportScript-StatisticCoreBridge.js", e);
        }
        return rep;
    }

    /**
     * Create Folder Diagnostic
     * @return Folder object
     */
    public Folder createFolderDiagnosticRoot() {
        Folder diagnostic = new Folder();
        diagnostic.setName("ViZix Diagnostics");
        diagnostic.setCode("vizix_diagnostics");
        diagnostic.setCreationDate(new Date());
        diagnostic.setGroup(GroupService.getInstance().getRootGroup());
        diagnostic.setLastModificationDate(new Date());
        diagnostic.setSequence(1L);
        diagnostic.setTypeElement("report");
        return FolderService.getInstance().insert(diagnostic);
    }

    /**
     * Create Folder Tutorial
     * @return Folder object
     */
    public Folder createFolderTutorialRoot() {
        Folder tutorial = new Folder();
        tutorial.setName("ViZix Tutorias");
        tutorial.setCode("vizix_tutorials");
        tutorial.setCreationDate(new Date());
        tutorial.setGroup(GroupService.getInstance().getRootGroup());
        tutorial.setLastModificationDate(new Date());
        tutorial.setSequence(2L);
        tutorial.setTypeElement("report");
        return FolderService.getInstance().insert(tutorial);
    }

    /**
     * This method organize all Table Scripts in two folders: Diagnostic and Tutorial
     * @param folderDiagnostic Diagnostic Folder
     */
    public void organizeDiagnosticReports(Folder folderDiagnostic) {
        BooleanBuilder bb = new BooleanBuilder();
        bb.and(QReportDefinition.reportDefinition.name.eq("ViZix TimeSeries Order")).or(
                QReportDefinition.reportDefinition.name.eq("ViZix TimeSeries Order F")).or(
                QReportDefinition.reportDefinition.name.eq("ViZix TimeSeries Order S")).or(
                QReportDefinition.reportDefinition.name.eq("ViZix TimeSeries Upsert CB")).or(
                QReportDefinition.reportDefinition.name.eq("ViZix TimeSeries Upsert E")).or(
                QReportDefinition.reportDefinition.name.eq("ViZix TimeSeries Upsert EB")).or(
                QReportDefinition.reportDefinition.name.eq("ViZix TimeSeries Upsert FE")).or(
                QReportDefinition.reportDefinition.name.eq("ViZix TimeSeries Upsert IvE")).or(
                QReportDefinition.reportDefinition.name.eq("ViZix TimeSeries Upsert S#")).or(
                QReportDefinition.reportDefinition.name.eq("ViZix Index Statistics"));
        List<ReportDefinition>lstReportDefinition= ReportDefinitionService.getInstance().listPaginated(bb, null, null);
        if ( (lstReportDefinition != null) && (!lstReportDefinition.isEmpty())) {
            for(ReportDefinition repDef: lstReportDefinition ) {
                //Diagnostic
                if(repDef.getName().equalsIgnoreCase("ViZix TimeSeries Order")) {
                    repDef.setName("ViZix Out-of-Order");
                    repDef.setDescription("ViZix Out-of-Order");
                    repDef.setFolder(folderDiagnostic);
                } else if(repDef.getName().equalsIgnoreCase("ViZix TimeSeries Order F")) {
                    repDef.setName("ViZix Out-of-Order by Filter");
                    repDef.setDescription("ViZix Out-of-Order by Filter");
                    repDef.setFolder(folderDiagnostic);
                } else if(repDef.getName().equalsIgnoreCase("ViZix TimeSeries Order S")) {
                    repDef.setName("ViZix Out-of-Order by Starflex");
                    repDef.setDescription("ViZix Out-of-Order by Starflex");
                    repDef.setFolder(folderDiagnostic);
                } else if(repDef.getName().equalsIgnoreCase("ViZix TimeSeries Upsert CB")) {
                    repDef.setName("ViZix Upserts CB by Source - I");
                    repDef.setDescription("ViZix Upserts CB by Source - I");
                    repDef.setFolder(folderDiagnostic);
                } else if(repDef.getName().equalsIgnoreCase("ViZix TimeSeries Upsert E")) {
                    repDef.setName("ViZix Upserts CB by Source - E");
                    repDef.setDescription("ViZix Upserts CB by Source - E");
                    repDef.setFolder(folderDiagnostic);
                } else if(repDef.getName().equalsIgnoreCase("ViZix TimeSeries Upsert EB")) {
                    repDef.setName("ViZix Upserts EB - I");
                    repDef.setDescription("ViZix Upserts EB - I");
                    repDef.setFolder(folderDiagnostic);
                } else if(repDef.getName().equalsIgnoreCase("ViZix TimeSeries Upsert FE")) {
                    repDef.setName("ViZix Upserts by SF Filter - E");
                    repDef.setDescription("ViZix Upserts by SF Filter - E");
                    repDef.setFolder(folderDiagnostic);
                } else if(repDef.getName().equalsIgnoreCase("ViZix TimeSeries Upsert IvE")) {
                    repDef.setName("ViZix Upserts E vs EB vs CB");
                    repDef.setDescription("ViZix Upserts E vs EB vs CB");
                    repDef.setFolder(folderDiagnostic);
                } else if(repDef.getName().equalsIgnoreCase("ViZix TimeSeries Upsert S#")) {
                    repDef.setName("ViZix Upserts by StarFlex");
                    repDef.setDescription("ViZix Upserts by StarFlex");
                    repDef.setFolder(folderDiagnostic);
                } else if(repDef.getName().equalsIgnoreCase("ViZix Index Statistics")) {
                    repDef.setName("ViZix Indexes");
                    repDef.setDescription("ViZix Indexes");
                    repDef.setFolder(folderDiagnostic);
                }
                ReportDefinitionService.getInstance().update(repDef);
            }
        }
    }

    /**
     * Organize Tutorial Reports
     */
    public void organizeTutorialReports() {
        BooleanBuilder bb = new BooleanBuilder();
        bb.and(QReportDefinition.reportDefinition.name.eq("Mongo Filters Query (Now)")).or(
                QReportDefinition.reportDefinition.name.eq("Mongo Filters Query (Custom)")).or(
                QReportDefinition.reportDefinition.name.eq("Mongo Example #1a")).or(
                QReportDefinition.reportDefinition.name.eq("Mongo Example #1b")).or(
                QReportDefinition.reportDefinition.name.eq("Mongo Example #1c")).or(
                QReportDefinition.reportDefinition.name.eq("Mongo Example #2")).or(
                QReportDefinition.reportDefinition.name.eq("Mongo TimeZone Test")).or(
                QReportDefinition.reportDefinition.name.eq("SQL Report Example"));
        List<ReportDefinition>lstReport= ReportDefinitionService.getInstance().listPaginated(bb, null, null);
        if ( (lstReport != null) && (!lstReport.isEmpty())) {
            Folder folderTutorial = createFolderTutorialRoot();
            for(ReportDefinition repDef: lstReport ) {
                if( repDef.getName().equalsIgnoreCase("Mongo Filters Query (Now)")) {
                    repDef.setDescription("Mongo Filters Query (Now)");
                    repDef.setFolder(folderTutorial);
                } else if(repDef.getName().equalsIgnoreCase("Mongo Filters Query (Custom)")) {
                    repDef.setDescription("Mongo Filters Query (Custom)");
                    repDef.setFolder(folderTutorial);
                } else if(repDef.getName().equalsIgnoreCase("Mongo Example #1a")) {
                    repDef.setDescription("Mongo Example #1a");
                    repDef.setFolder(folderTutorial);
                } else if(repDef.getName().equalsIgnoreCase("Mongo Example #1b")) {
                    repDef.setDescription("Mongo Example #1b");
                    repDef.setFolder(folderTutorial);
                } else if(repDef.getName().equalsIgnoreCase("Mongo Example #1c")) {
                    repDef.setDescription("Mongo Example #1c");
                    repDef.setFolder(folderTutorial);
                } else if(repDef.getName().equalsIgnoreCase("Mongo Example #2")) {
                    repDef.setDescription("Mongo Example #2");
                    repDef.setFolder(folderTutorial);
                } else if(repDef.getName().equalsIgnoreCase("Mongo TimeZone Test")) {
                    repDef.setDescription("Mongo TimeZone Test");
                    repDef.setFolder(folderTutorial);
                } else if(repDef.getName().equalsIgnoreCase("SQL Report Example")) {
                    repDef.setDescription("SQL Report Example, connection with external relational database.");
                    repDef.setFolder(folderTutorial);
                }
                ReportDefinitionService.getInstance().update(repDef);
            }
        }

    }

    /**
     * Create Summary DIagnostic REport
     * @param folderDiagnostic Folder of Diagnostic
     * @throws NonUniqueResultException exception
     * @throws IOException exception
     * @throws MongoExecutionException exception
     */
    public void createSummaryDiagnosticReport(Folder folderDiagnostic) throws NonUniqueResultException, IOException, MongoExecutionException {
        Group group = GroupService.getInstance().getByCode("mojix");
        if( group != null) {
            ReportDefinition repSummary = new ReportDefinition();
            repSummary.setName("ViZix Queues Summary");
            repSummary.setDescription("ViZix Queues Summary");
            repSummary.setFolder(folderDiagnostic);
            repSummary.setBulkEdit(false);
            repSummary.setCreatedByUser(UserService.getInstance().getRootUser());
            repSummary.setDelete(false);
            repSummary.setDismiss(false);
            repSummary.setGroup(GroupService.getInstance().getByCode("mojix"));
            repSummary.setHeatmap(false);
            repSummary.setIsMobile(false);
            repSummary.setReportType("mongo");
            repSummary.setTypeOrder(3L);
            repSummary.setRfidPrint(false);
            repSummary.setRunOnLoad(true);
            repSummary.setGroup(group);
            repSummary.setChartFunction("[{\"type\":\"columnCharts\",\"title\":\"Column Charts\",\"subtype\":\"basic\",\"lib\":\"hc\",\"label\":\"@CHART_COLUMNCHARTS_LABEL_COLUMN_CHART\",\"disabled\":false,\"routeImg\":\"images/charts/column_charts/basic.svg\",\"activeStatus\":true,\"pinDefault\":true,\"name\":\"test\"}]");
            ReportDefinitionService.getInstance().insert(repSummary);
            repSummary = ReportDefinitionService.getInstance().getByNameAndType("ViZix Queues Summary","mongo");

            ReportFilter groupFilter = new ReportFilter();
            groupFilter.setReportDefinition(repSummary);
            groupFilter.setDisplayOrder(1F);
            groupFilter.setEditable(true);
            groupFilter.setLabel("Tenant Group");
            groupFilter.setOperator("<");
            groupFilter.setPropertyName("group.id");
            ReportFilterService.getInstance().insert(groupFilter);

            ReportDefinitionConfig repConfig = new ReportDefinitionConfig();
            repConfig.setReportDefinition(repSummary);
            repConfig.setKeyType("SCRIPT");
            PopDBBaseUtils popdbUtil = new PopDBBaseUtils("CoreTenant", new Date());
            String script = popdbUtil.readTableScript("ViZixDiagnostics/SummaryCoreBridgeStats.js");
            repConfig.setKeyValue(script);
            popdbUtil.insertTableScript("ViZixDiagnostics/SummaryCoreBridgeStats.js","ViZix Queues Summary");
            ReportDefinitionConfigService.getInstance().insert(repConfig);
        }

    }

}
