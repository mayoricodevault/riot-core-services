package com.tierconnect.riot.migration.steps.group;

import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupField;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.FieldService;
import com.tierconnect.riot.appcore.services.GroupFieldService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_InsertAndUpdateLDAPFields_RIOT11927 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_InsertAndUpdateLDAPFields_RIOT11927.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        insertLDAPFields();
        updateLDAPFields();
    }

    /**
     * Insert data for LDAP
     */
    private void insertLDAPFields() {
        Group rootGroup = GroupService.getInstance().getRootGroup();
        // Authentication values begin
        Field f50 = PopDBUtils.popFieldService("authenticationMode", "authenticationMode", "Authentication Mode",
                rootGroup, "Security Configuration", "java.lang.String", 3L, false);
        PopDBUtils.popFieldWithParentService("ldapAdAuthentication", "ldapAdAuthentication", "LDAP/AD",
                rootGroup, "Security Configuration", "java.lang.String", 3L, false, f50);
        PopDBUtils.popFieldWithParentService("nativeAuthentication", "nativeAuthentication", "Native",
                rootGroup, "Security Configuration", "java.lang.String", 3L, false, f50);
        PopDBUtils.popFieldService("ldapAdConnection", "ldapAdConnection", "LDAP/AD Connection", rootGroup, "Security" +
                " Configuration", "java.lang.String", 3L, true);
        // Authentication values end.
    }

    /**
     * Update and delete LDAP Fields
     */
    private void updateLDAPFields() {
        Field nativeField = FieldService.getInstance().selectByName("native");
        Field ldapField = FieldService.getInstance().selectByName("ldap");
        Field ldapConnectionField = FieldService.getInstance().selectByName("ldapConnection");
        Field ldapPasswordRequiredField = FieldService.getInstance().selectByName("passwordRequired");
        List<Group> groupList = GroupService.getGroupDAO().selectAll();
        for (Group aGroup : groupList) {
            GroupField nativeGFValue = GroupFieldService.getInstance().selectByGroupField(aGroup, nativeField);
            if (nativeGFValue != null) {
                logger.debug("Group [" + aGroup.getName() + "] has native GroupField");
                GroupField ldapGFValue = GroupFieldService.getInstance().selectByGroupField(aGroup, ldapField);
                GroupField ldapConnectionGFValue = GroupFieldService.getInstance().selectByGroupField(aGroup,
                        ldapConnectionField);
                // updating LDAP values
                if ((ldapGFValue != null) && (ldapConnectionGFValue != null)) {
                    Field newLDAPAuthenticationMode = FieldService.getInstance().selectByName("authenticationMode");
                    Field newLDAPADConnection = FieldService.getInstance().selectByName("ldapAdConnection");
                    Boolean isNative = Boolean.valueOf(nativeGFValue.getValue());
                    if (isNative) {
                        PopDBUtils.popGroupField(aGroup, newLDAPAuthenticationMode, "nativeAuthentication");
                        PopDBUtils.popGroupField(aGroup, newLDAPADConnection, "");
                    } else {
                        PopDBUtils.popGroupField(aGroup, newLDAPAuthenticationMode, "ldapAdAuthentication");
                        PopDBUtils.popGroupField(aGroup, newLDAPADConnection, ldapConnectionGFValue.getValue());
                    }
                } else {
                    logger.warn("Is not possible to migrate LDAP values for group [" + aGroup.getName() + "].");
                }
                // Deleting old LDAP Values (groupfield)
                GroupFieldService.getInstance().delete(nativeGFValue);
                if (ldapGFValue != null) {
                    GroupFieldService.getInstance().delete(ldapGFValue);
                }
                if (ldapConnectionGFValue != null) {
                    GroupFieldService.getInstance().delete(ldapConnectionGFValue);
                }
                if (ldapPasswordRequiredField != null) {
                    GroupField ldapPasswordRequiredGFValue = GroupFieldService.getInstance().selectByGroupField
                            (aGroup, ldapPasswordRequiredField);
                    if (ldapPasswordRequiredGFValue != null) {
                        GroupFieldService.getInstance().delete(ldapPasswordRequiredGFValue);
                    }
                }
            } else {
                logger.debug("Group [" + aGroup.getName() + "] has NOT native GroupField");
            }
        }
        // Deleting old LDAP Values (apc_field)
        if (nativeField != null) {
            FieldService.getInstance().delete(nativeField);
        }
        if (ldapField != null) {
            FieldService.getInstance().delete(ldapField);
        }
        if (ldapConnectionField != null) {
            FieldService.getInstance().delete(ldapConnectionField);
        }
        if (ldapPasswordRequiredField != null) {
            FieldService.getInstance().delete(ldapPasswordRequiredField);
        }

    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
