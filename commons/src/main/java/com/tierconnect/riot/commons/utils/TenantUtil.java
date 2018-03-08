package com.tierconnect.riot.commons.utils;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;

/**
 * TenantUtil class.
 *
 * @author jantezana
 * @version 2017/04/19
 */
public final class TenantUtil {
    public static final String ROOT = ">root";

    /**
     * Gets the tenant code.
     *
     * @param hierarchyName the hierarchy name
     * @return the tenant code
     */
    public static String getTenantCode(final String hierarchyName) {
        Preconditions.checkNotNull(hierarchyName, "The hierarchyName is null");
        String tenantCode;
        if (StringUtils.startsWith(hierarchyName, ROOT)) {
            tenantCode = null;
        } else {
            tenantCode = hierarchyName.split(">")[1];
        }

        return tenantCode;
    }
}
