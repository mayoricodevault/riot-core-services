package com.tierconnect.riot.migration.steps.user;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.Resource;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.entities.UserPassword;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.UserPasswordService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.utils.HashUtils;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dbascope on 07/07/2017
 */
public class Migrate_DefaultPasswords_VIZIX7545 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_DefaultPasswords_VIZIX7545.class);


    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        Map<String, String> defaults = new HashMap<>();
        defaults.put("root", "root");
        defaults.put("tenant", "mojix");
        defaults.put("samuel", "SM");
        defaults.put("paul", "C");
        defaults.put("adminc", "mojix");
        defaults.put("adminp", "SM");
        defaults.put("employee", "SM");
        for (String userName : defaults.keySet()) {
            Group group = GroupService.getInstance().getByCode(defaults.get(userName));
            if (group != null) {
                User user = UserService.getInstance().getByUsername(userName, group);
                if (user != null && user.getHashedPassword().equals(HashUtils.hashSHA256(userName))) {
                    UserPassword userPassword = new UserPassword();
                    userPassword.setUser(user);
                    userPassword.setStatus(Constants.PASSWORD_STATUS_PENDING);
                    userPassword.setHashedPassword(user.getHashedPassword());
                    UserPasswordService.getInstance().insert(userPassword);
                }
            }
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

}
