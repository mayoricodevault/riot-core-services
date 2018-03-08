package com.tierconnect.riot.appcore.controllers;

import org.apache.log4j.Logger;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;

/**
 * Created by agutierrez on 02-06-14.
 */
public class RiotPermission extends WildcardPermission implements Permission {
    static Logger logger = Logger.getLogger(RiotPermission.class);

    public RiotPermission(String name) {
        super(name);
    }

//    @Override
//    public boolean implies(Permission p) {
//        boolean result = super.implies(p);
//        if (!result) {
//            logger.error("AGG: returning false for:" + p);
//        }
//        return result;
//    }
}