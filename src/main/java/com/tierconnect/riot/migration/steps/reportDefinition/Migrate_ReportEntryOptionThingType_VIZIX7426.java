package com.tierconnect.riot.migration.steps.reportDefinition;

import com.google.common.collect.Lists;
import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.ReportDefinitionService;
import com.tierconnect.riot.iot.services.ReportEntryOptionService;

import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.List;

import static com.tierconnect.riot.iot.services.ThingTypeService.*;

/**
 * Created by rchirinos.
 * on 29/08/2017.
 * Modified by achambi.
 * on 04/09/2017.
 * Class called per reflection.
 */
@SuppressWarnings("unused")
public class Migrate_ReportEntryOptionThingType_VIZIX7426 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_ReportEntryOptionThingType_VIZIX7426.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateThingTypeDataEntry();
    }

    private void migrateThingTypeDataEntry() {
        List<String> reportTypes = Lists.newArrayList("table", "mongo");
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QReportDefinition.reportDefinition.reportType.in(reportTypes));
        List<ReportDefinition> lstReportDefinition =
                ReportDefinitionService.getInstance().listPaginated(be, null, null);
        if ((lstReportDefinition != null) && (!lstReportDefinition.isEmpty())) {
            for (ReportDefinition reportDefinition : lstReportDefinition) {
                if ((reportDefinition.getReportEntryOption() != null) && (!reportDefinition.getReportEntryOption().isEmpty())) {
                    for (ReportEntryOption entryOption : reportDefinition.getReportEntryOption()) {
                        ThingType thingType = null;
                        if (entryOption.getNewOption()) {
                            thingType = getThingTypeConfiguredForNew(entryOption.getReportEntryOptionProperties());
                            if (thingType != null) {
                                changeReportPropertyThingIdReport(
                                        entryOption.getReportEntryOptionProperties(),
                                        thingType.getId());
                                entryOption.setThingType(thingType);
                                ReportEntryOptionService.getInstance().update(entryOption);
                            }
                        }
                        if (thingType == null) {
                            entryOption.setNewOption(false);
                            entryOption.setEditOption(true);
                            ReportEntryOptionService.getInstance().update(entryOption);
                        }
                    }
                }
            }
        }
    }

    /**
     * Set Thing Type of the data entry
     *
     * @param properties  List of Report entry properties
     * @param thingTypeId thing type pivot.
     */
    private void changeReportPropertyThingIdReport(List<ReportEntryOptionProperty> properties, Long thingTypeId) {
        List<Long> thingTypesIds = getInstance().getThingTypeIdsOfPathsByThingTypeId(thingTypeId);
        if (thingTypesIds.indexOf(thingTypeId) == -1) {
            thingTypesIds.add(thingTypeId);
        }
        if ((properties != null) && (!properties.isEmpty())) {
            for (ReportEntryOptionProperty optionProperty : properties) {
                Long thingTypeIdOptionProp = optionProperty.getThingTypeIdReport();
                if (thingTypeIdOptionProp != null &&
                        !thingTypesIds.contains(thingTypeIdOptionProp)) {
                    optionProperty.setThingTypeIdReport(0L);
                }
            }
        }
    }

    /**
     * Get Thing Type of the data entry
     *
     * @param entryOptionProperties List of Report entry properties
     * @return Thing Type Object
     */
    private ThingType getThingTypeConfiguredForNew(List<ReportEntryOptionProperty> entryOptionProperties) {
        ThingType response = null;
        if ((entryOptionProperties != null) && (!entryOptionProperties.isEmpty())) {
            for (ReportEntryOptionProperty optionProperty : entryOptionProperties) {
                if ((optionProperty.getThingTypeIdReport() != null) &&
                        (optionProperty.getThingTypeIdReport().compareTo(0L) > 0)) {
                    response = getInstance().get(optionProperty.getThingTypeIdReport());
                    break;
                }
            }
        }
        return response;
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
    }

}
