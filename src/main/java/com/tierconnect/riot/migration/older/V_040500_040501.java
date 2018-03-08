package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Created by fflores on 1/26/17.
 */
@Deprecated
public class V_040500_040501 implements MigrationStepOld {
    static Logger logger = Logger.getLogger(V_040500_040501.class);

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(40500);
    }

    @Override
    public int getToVersion() {
        return 40501;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateLdapParamater();
    }

    @Override
    public void migrateSQLAfter() throws Exception {
    }

    /**
     * migrate ldapValidateUserCreation parameter for LDAP/AD Authentication
     */
    private static void migrateLdapParamater() {
        Group rootGroup = GroupService.getInstance().getRootGroup();
        Field field = PopDBUtils.popFieldService("ldapUserIdentifier", "ldapUserIdentifier", "LDAP/AD User Identifier",
                rootGroup, "Security Configuration", "java.lang.String", 3L, true);
        PopDBUtils.popGroupField(rootGroup, field, "sAMAccountName");
    }

}
