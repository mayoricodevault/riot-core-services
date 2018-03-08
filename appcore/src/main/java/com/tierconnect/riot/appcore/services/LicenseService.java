package com.tierconnect.riot.appcore.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.utils.AuthenticationUtils;
import com.tierconnect.riot.appcore.version.CodeVersion;
import com.tierconnect.riot.appcore.utils.VersionUtils;
import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by agutierrez on 4/23/15.
 */
public class LicenseService extends LicenseServiceBase {
    static Logger logger = Logger.getLogger(LicenseService.class);

    static private LicenseService INSTANCE = new LicenseService();
    public static boolean enableLicense = "false".equals(System.getProperty("2f53086555b7fbb940ce78616ff212e5")) ? false: true;
    private static final LoadingCache<Long, LicenseDetail> groupIdLicenseCache;

    private static final CacheLoader<Long, LicenseDetail> licenseLoader = new CacheLoader<Long, LicenseDetail>() {
        public LicenseDetail load(Long groupId) throws Exception {
            LicenseDetail licenseDetail = null;
            License license = getLicenseDAO().selectBy(QLicense.license.group.id.eq(groupId));
            if (license != null) {
                String licenseString = license.getLicenseString();
                TreeMap<String, Object> licenseAsMap = LicenseServiceUtils.licenseStringToMap(licenseString);
                licenseDetail = LicenseServiceUtils.licenseStringToLicenseDetail(licenseString);
                if (licenseAsMap == null || licenseDetail == null) {
                    logger.error("invalid license file format for group: " + license.getGroup().getName());
                    System.err.println("invalid license file format for group: " + license.getGroup().getName());
                    licenseDetail = null;
                }
                boolean valid = LicenseServiceUtils.verifySignature(licenseAsMap, LicenseServiceUtils.getDefaultPublicKey());
                if (!valid) {
                    logger.error("invalid license file for group: " + license.getGroup().getName());
                    System.err.println("invalid license file for group: " + license.getGroup().getName());
                    licenseDetail = null;
                }
            }
            return licenseDetail;
        }
    };

    static {
        groupIdLicenseCache = CacheBuilder.newBuilder().maximumSize(100).expireAfterWrite(60, TimeUnit.SECONDS).build(
                licenseLoader
        );
    }

    public static void clearCaches() {
        groupIdLicenseCache.invalidateAll();
    }

    public static LicenseService getInstance() {
        return INSTANCE;
    }

    public boolean hasValidActiveLicense(User user) {
        if (!enableLicense) {
            return true;
        }
        LicenseDetail licenseDetail = getLicenseDetail(getGroup(user), true);
        return licenseDetail != null && ((licenseDetail.getExpirationDate() == null || licenseDetail.getExpirationDate().after(new Date())) && isValidLicenseVersion(licenseDetail) );
    }

    public boolean isValidResource(User user, String resourceName) {
        return isValidResource(getGroup(user), resourceName);
    }

    public boolean isValidField(User user, String fieldName) {
        return isValidField(getGroup(user), fieldName);
    }


    public boolean isValidResource(Group group, String resourceName) {
        if (!enableLicense) {
            return true;
        }
        if (resourceName.equals("license")) {
            return true;
        }
        if (resourceName.equals("Tenants")) {
            return true;
        }
        boolean isLicenseDetailInheritance = LicenseService.getInstance().getLicenseDetailInheritance(group);
        LicenseDetail licenseDetail = getLicenseDetail(group, isLicenseDetailInheritance);
        if (licenseDetail == null
                || (licenseDetail.getExpirationDate() != null && licenseDetail.getExpirationDate().before(new Date()))
                || !(LicenseService.getInstance().isValidLicenseVersion(licenseDetail))) {
            return false;
        }
        return licenseDetail.hasResource(resourceName);
    }

    public boolean isValidField(Group group, String fieldName) {
        if (!enableLicense) {
            return true;
        }
        LicenseDetail licenseDetail = getLicenseDetail(group, true);
        return (licenseDetail != null) && licenseDetail.hasField(fieldName);
    }

    public LicenseDetail getLicenseDetail(Group group, boolean inherited) {
        if (inherited) {
            if (group.getTreeLevel() > 2) {
                return getLicenseDetail(group.getParentLevel2(), inherited);
            }
            LicenseDetail licenseDetail = getLicenseDetailFromCache(group);
            if (licenseDetail == null && group.getTreeLevel() == 2) {
                licenseDetail = getLicenseDetailFromCache(group.getParentLevel1());
            }
            return licenseDetail;
        } else {
            LicenseDetail licenseDetail = getLicenseDetailFromCache(group);
            return licenseDetail;
        }
    }

    private LicenseDetail getLicenseDetailFromCache(Group group) {
        Long groupId = group.getId();
        LicenseDetail licenseDetail = null;
        try {
            licenseDetail = groupIdLicenseCache.get(groupId);
        } catch (Exception ex) {

        }
        return licenseDetail;
    }

    private Group getGroup(User user) {
        return user.getActiveGroup();
    }

    public static boolean isEnableLicense() {
        return enableLicense;
    }

    public static void setEnableLicense(boolean enableLicense) {
        LicenseService.enableLicense = enableLicense;
    }

    public static boolean isValidLicenseVersion(LicenseDetail licenseDetail) {
        boolean result = true;
        String[] versionParts = licenseDetail.getVersion().split("\\.");
        if (!versionParts[0].toLowerCase().equals("x")) {
            int[] v = VersionUtils.getAppVersionInt(CodeVersion.getInstance().getCodeVersion());
            if (!Integer.valueOf(versionParts[0]).equals(v[0])) {
                return false;
            }
            if (!versionParts[1].toLowerCase().equals("x")) {
                if (!Integer.valueOf(versionParts[1]).equals(v[1])) {
                    return false;
                }
            }
            if (versionParts.length > 2 && v.length >2){
                if (!versionParts[2].toLowerCase().equals("x")) {
                    if (!Integer.valueOf(versionParts[2]).equals(v[2])) {
                        return false;
                    }
                }
            }
        }
        return result;
    }

    public void logLicenses() {
        GroupService groupService = GroupServiceBase.getInstance();
        Group rootGroup = groupService.getRootGroup();
        LicenseDetail licenseDetail = getLicenseDetail(rootGroup, false);
        logger.warn("LICENSE INFO");
        logger.warn("============");
        log(rootGroup, licenseDetail);
        List<Group> companies = groupService.getGroupDAO().selectAllBy(QGroup.group.parent.eq(rootGroup));
        for (Group group : companies) {
            licenseDetail = getLicenseDetail(group, false);
            log(group, licenseDetail);
        }
    }

    private void log(Group group, LicenseDetail licenseDetail) {
        if(licenseDetail != null ){
            Date expirationDate = licenseDetail.getExpirationDate();
            String expirationDateFormatted;
            if(expirationDate != null){
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
                expirationDateFormatted = sdf.format(expirationDate);
            }else{
                expirationDateFormatted = "No expiration";
            }
            logger.warn("LICENSE -> Group: " + group.getHierarchyName(true) + ", Version: " + licenseDetail.getVersion() + ", Expiration Date: " + expirationDateFormatted);
        }else{
            logger.warn("LICENSE -> Group: " + group.getHierarchyName(true) + ", NO LICENSE DETAIL FOR GROUP");
        }
    }

    /**
     * This method set a 'Native Authentication' for root Group if license has not LDAP Auth feature.
     */
    public void setNativeAuthenticationToRootGroup() {
        try {
            Group rootGroup = GroupService.getInstance().getRootGroup();
            if (rootGroup == null) {
                logger.warn("Was not possible update authentication mode. [rootGroup] is null");
                return;
            }
            List<String> licenseFeatures = LicenseService.getInstance().getLicenseDetail(rootGroup, false).getFeatures();
            if (licenseFeatures == null) {
                logger.warn("Was not possible update authentication mode. [licenseFeatures] is null");
                return;
            }
            Field field = FieldService.getInstance().selectByGroupAndName(rootGroup, AuthenticationUtils.AUTHENTICATION_MODE);
            if (field == null) {
                logger.warn("Was not possible update authentication mode. [" + AuthenticationUtils.AUTHENTICATION_MODE + "] field not found.");
                return;
            }
            GroupField groupField = GroupFieldService.getInstance().selectByGroupField(rootGroup, field);
            if (groupField == null) {
                logger.warn("Was not possible update authentication mode. [" + AuthenticationUtils.AUTHENTICATION_MODE + "] value not found.");
                return;
            }

            if ((groupField.getValue().equals(AuthenticationUtils.LDAP_AD_AUTHENTICATION)) &&
                    !licenseFeatures.contains(LicenseDetail.LDAP_AUTH_ROOT_GROUP)) {
                groupField.setValue(AuthenticationUtils.NATIVE_AUTHENTICATION);
                GroupFieldService.getInstance().update(groupField);
                logger.warn("Authentication mode was changed to 'Native' Authentication");
            }
        } catch (Exception e) {
            logger.warn("Was not possible update authentication mode. " + e.getMessage());
        }
    }

    public boolean getLicenseDetailInheritance(Group group) {
        boolean response = true;
        if (group.getTreeLevel() > 2) {
            return getLicenseDetailInheritance(group.getParentLevel2());
        }
        LicenseDetail licenseDetail = getLicenseDetailFromCache(group);
        if (licenseDetail != null && group.getTreeLevel() == 2){
            return response;
        }
        licenseDetail = getLicenseDetailFromCache(group.getParentLevel1());
        if (licenseDetail != null && licenseDetail.getApplicableGroupLevel().contains(1L) &&
                (group.getTreeLevel() == 1 || group.getTreeLevel() == 2) ){
            response = licenseDetail.isTenantLicenseInheritance();
        }
        return response;
    }

}
