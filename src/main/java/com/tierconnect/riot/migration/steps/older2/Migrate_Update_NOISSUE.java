package com.tierconnect.riot.migration.steps.older2;

import com.tierconnect.riot.appcore.dao.CassandraUtils;
import com.tierconnect.riot.appcore.dao.RoleResourceDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.iot.services.ThingTypeTemplateService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_Update_NOISSUE implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_Update_NOISSUE.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateResources();
        migrateFields();
        migrateTemplates();
        updateExitReportTable();
    }

    private void migrateResources() {
        ResourceService resourceService = ResourceService.getInstance();
        QResource qResource = QResource.resource;
        RoleResourceService roleResourceService = RoleResourceServiceBase.getInstance();
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        //New Resources
        Resource moduleControl = ResourceServiceBase.getResourceDAO().selectBy("name", "Control");
        Role rootRole = RoleService.getInstance().getRootRole();
        if (resourceService.countList(qResource.name.eq("license")) == 0) {
            Resource re = new Resource();
            re.setAcceptedAttributes("riuda");
            re.setGroup(rootGroup);
            re.setLabel("License");
            re.setName("license");
            re.setDescription("License");
            re.setFqname("com.tierconnect.riot.appcore.entities.License");
            re.setParent(moduleControl);
          //  re.setModule(moduleControl.getName());
            re.setTreeLevel(2);
            resourceService.insert(re);
            roleResourceService.insert(rootRole, re, re.getAcceptedAttributes());

            ResourceService.getInstance().insert(Resource.getPropertyResource(rootGroup, re, "generator", "Generate " +
                    "License", "Generate License"));
        }
        QRoleResource qRoleResource = QRoleResource.roleResource;
        RoleResourceDAO roleResourceDAO = RoleResourceServiceBase.getRoleResourceDAO();
        if (resourceService.countList(qResource.name.eq("thingTypeTemplate")) == 0) {
            Resource re = resourceService.insert(Resource.getClassResource(rootGroup, ThingTypeTemplate.class,
                    moduleControl));
            List<Role> roles = roleResourceDAO.getQuery().where(qRoleResource.resource.name.eq("thingType")).distinct
                    ().list(qRoleResource.role);
            for (Role role : roles) {
                roleResourceService.insert(role, re, "r");
            }
        }
        if (resourceService.countList(qResource.name.eq("thingTypeFieldTemplate")) == 0) {
            Resource re = resourceService.insert(Resource.getClassResource(rootGroup, ThingTypeFieldTemplate.class,
                    moduleControl));
            List<Role> roles = roleResourceDAO.getQuery().where(qRoleResource.resource.name.eq("thingType")).distinct
                    ().list(qRoleResource.role);
            for (Role role : roles) {
                roleResourceService.insert(role, re, "r");
            }
        }
        if (resourceService.countList(qResource.name.eq("dataType")) == 0) {
            Resource re = resourceService.insert(Resource.getClassResource(rootGroup, DataType.class, moduleControl));
            List<Role> roles = roleResourceDAO.getQuery().where(qRoleResource.resource.name.eq("thingType")).distinct
                    ().list(qRoleResource.role);
            for (Role role : roles) {
                roleResourceService.insert(role, re, "r");
            }
        }
    }

    private void migrateFields() {
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        PopDBUtils.migrateFieldService("shiftZoneValidationEnabled", "shiftZoneValidationEnabled", "Shift-Zone " +
                "Validation Enabled", rootGroup, "Job Scheduling", "java.lang.Boolean", null, true, "true");
        PopDBUtils.migrateFieldService("genetecVideoLinksVisible", "genetecVideoLinksVisible", "Genetec Video Links " +
                "Visible", rootGroup, "Integration", "java.lang.Boolean", 3L, false, "false");

        PopDBUtils.migrateFieldService("i18NDirectory", "i18NDirectory", "I18N Directory", rootGroup, "Look & Feel",
                "java.lang.String", 1L, true, "");
    }

    private void migrateTemplates() {
        //Populate DataType
        PopDBRequiredIOT.populateDataType();

        //Populate thingType templates
        PopDBRequiredIOT.populateThingTypeOldTemplates(GroupService.getInstance().getRootGroup());
        for (ThingTypeField thingTypeField : ThingTypeFieldService.getThingTypeFieldDAO().selectAll()) {
            thingTypeField.setTypeParent("DATA_TYPE");
            ThingTypeFieldService.getInstance().update(thingTypeField);
        }

        //Populate Thing Type ZPL
        Group facility = GroupService.getInstance().getRootGroup();
        PopDBRequiredIOT.populateThingTypeZPL(facility);

        //Update all old thingTypes with Custom Templates
        for (ThingType thingType : ThingTypeService.getInstance().getAllThingTypes()) {
            thingType.setThingTypeTemplate(ThingTypeTemplateService.getInstance().get(1L));
            ThingTypeService.getInstance().update(thingType);
        }

    }

    public static void updateExitReportTable() {
        logger.info("Updating exit_report table...");
        logger.info("ALTER TABLE exit_report ADD zone varchar;");
        try {
            CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra" +
                    ".keyspace"));
            CassandraUtils.getSession().execute(
                    CassandraUtils.getSession().prepare("ALTER TABLE exit_report ADD zone varchar;").bind());
            CassandraUtils.getSession().execute(
                    CassandraUtils.getSession().prepare("CREATE INDEX ON exit_report( zone );").bind());
            CassandraUtils.shutdown();
        } catch (Exception ex) {
            logger.warn("ALTER TABLE exit_report ADD zone varchar; skipped, zone column already exists");
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
