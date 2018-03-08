package com.tierconnect.riot.appcore.controllers;

import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.LicenseService;
import com.tierconnect.riot.appcore.services.LicenseServiceUtils;
import com.tierconnect.riot.appcore.services.TokenService;
import com.tierconnect.riot.appcore.servlet.TokenCacheHelper;
import com.tierconnect.riot.appcore.utils.EntityVisibility;
import com.tierconnect.riot.appcore.utils.GeneralVisibilityUtils;
import com.tierconnect.riot.appcore.utils.NetUtils;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.Subject;

import javax.annotation.Generated;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.KeyPair;
import java.text.SimpleDateFormat;
import java.util.*;

@Path("/license")
@Api("/license")
@Generated("com.tierconnect.riot.appgen.service.GenController")
public class LicenseController extends LicenseControllerBase {
    static Logger logger = Logger.getLogger(LicenseController.class);

    @GET
    @Path("/licenseDetail/feature")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = {"license:r"})
    @ApiOperation(position = 1, value = "Select the list of all features")
    public Response selectLicenses() {
        Map<String,Object> mapResponse = new HashMap<String,Object>();
        mapResponse.put( "total", LicenseDetail.getGlobalFeatures().size() );
        mapResponse.put( "results", LicenseDetail.getGlobalFeatures() );
        return RestUtils.sendOkResponse( mapResponse );

    }

    @GET
    @Path("/licenseDetail/module")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = {"license:r"})
    @ApiOperation(position = 2, value = "Select the list of all modules")
    public Response selectModules() {
        Map<String, Module> globalModules = LicenseDetail.getGlobalModules();
        List<String> modules = new ArrayList<>(globalModules.keySet());
        Map<String,Object> mapResponse = new HashMap<String,Object>();
        mapResponse.put("total", modules.size());
        mapResponse.put( "results", modules );
        return RestUtils.sendOkResponse( mapResponse );

    }

    @GET
    @Path("/licenseDetail/option")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = {"license:r"})
    @ApiOperation(position = 2, value = "list options for  the list of all modules")
    public Response selectOptions() {
        Map result = new HashMap();
        result.put("vendor", Arrays.asList("Mojix", "Alien"));
        result.put("customer", Arrays.asList("Aramco", "Baku", "Dupont", "FMC", "NetApp", "Sharaf", "Coderoad"));
        result.put("product", Arrays.asList("ViZix", "RIoT"));
        //result.put("licenseType", Arrays.asList("Customer Evaluation", "Customer Pilot", "Customer Pre-Production", "Customer Production", "Customer Test Environment", "Development License", "Sales Demonstration", "Mojix Test Enviroment"));
        result.put("licenseType", Arrays.asList("EVAL", "PROD", "TEST"));
        result.put("version", Arrays.asList("2.4.x"));
        return RestUtils.sendOkResponse(result);
    }

    @GET
    @Path("/licenseDetail/group/{groupId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = {"license:r"})
    @ApiOperation(position = 3, value = "Select a License Detail for a Group")
    public Response selectLicenses(@PathParam("groupId") Long groupId, @QueryParam("inherited") @DefaultValue("true") String inheritedParam, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project) {
        Boolean inherited = Boolean.valueOf(inheritedParam);
        Group group = GroupService.getInstance().get(groupId);
        if (group == null) {
            return RestUtils.sendBadResponse(String.format("GroupId[%d] not found", groupId));
        }
        // get inherited license
        String inheritedMsg = null;
        LicenseService licenseService = LicenseService.getInstance();
        boolean isTenantLicenseInheritance = LicenseService.getInstance().getLicenseDetailInheritance(group);
        // get licenseDetail
        LicenseDetail licenseDetail = null;
        if (inherited){
            LicenseDetail licenseDetailUser = licenseService.getLicenseDetail(group, false);
            licenseDetail = licenseService.getLicenseDetail(group, isTenantLicenseInheritance);
            if (isTenantLicenseInheritance && licenseDetailUser == null && licenseDetail != null){
                inheritedMsg = "This Tenant Group does not yet have a license loaded, and is inheriting default root license.";
            }
        } else {
            licenseDetail = licenseService.getLicenseDetail(group, inherited);
        }
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(License.class.getCanonicalName(), null);
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QLicense.license, visibilityGroup, null, null);

        Map licenseMap = LicenseServiceUtils.licenseDetailToMap(licenseDetail);
        if (licenseMap == null) {
            licenseMap = new HashMap();
        }
        GroupService groupService = GroupService.getInstance();

        if (!(licenseDetail != null && groupService.isGroupInsideTree(groupService.get(licenseDetail.getGroupId()), visibilityGroup ))) {
            licenseMap.remove("key");
        }

        if (licenseDetail != null) {
            licenseMap.put("group", (new GroupController()).selectGroups(licenseDetail.getGroupId(), "groupType",  null, extend, project, false).getEntity());
            if (inheritedMsg != null){
                licenseMap.put("inheritedMsg",inheritedMsg);
            }
        }
        return RestUtils.sendOkResponse(licenseMap);
    }

    @PATCH
    @Path("/licenseDetail/group/{groupId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    @RequiresPermissions(value = {"license:u", "license:i"}, logical = Logical.OR)
    @ApiOperation(position = 4, value = "Updates the License for a Group")
    public Response updateLicense(@PathParam("groupId") Long groupId, String licenseString) {

        //1. Validate if the group exists on database.
        Group group = GroupService.getInstance().get(groupId);
        if (group == null) {
            return RestUtils.sendBadResponse(String.format("GroupId[%d] not found", groupId));
        }
        //2. Valid if it is possible to convert the license from String to Map.
        Map<String, Object> map = LicenseServiceUtils.licenseStringToMap(licenseString);
        if (map == null) {
            return RestUtils.sendBadResponse(String.format("Invalid license format"));
        }

        // 3. Limit visibility based on user's group and the object's group (group based authorization)
        License aux = new License();
        aux.setGroup(group);
        GeneralVisibilityUtils.limitVisibilityUpdate(getEntityVisibility(), aux, aux.getGroup());

        // 4. Verify if the license key is valid.
        LicenseService licenseService = LicenseService.getInstance();
        TreeMap<String, Object> licenseAsMap = new TreeMap<String, Object>(map);
        boolean validLicense = LicenseServiceUtils.verifySignature(licenseAsMap, LicenseServiceUtils.getDefaultPublicKey());
        if (!validLicense) {
            return RestUtils.sendBadResponse("Invalid license");
        }

        // 5. Verify the License is applicable to group.
        LicenseDetail licenseDetail = LicenseServiceUtils.licenseStringToLicenseDetail(licenseString);
        if (!licenseDetail.getApplicableGroupLevel().contains(new Long(group.getTreeLevel()))) {
            return RestUtils.sendBadResponse("License cannot be applied to this Group, verify Applicable Group Level value ");
        }

        // 6. Verify the license is valid to version current Vizix.
        if (!LicenseService.isValidLicenseVersion(licenseDetail)) {
            return RestUtils.sendBadResponse("License cannot be applied to this Version");
        }

        // 7. The licenses only are valids to  level groups 2 and root, others levels are not valid.
        LicenseDetail rootLicenseDetail = null;
        if (group.getTreeLevel() == 2) {
            License rootLicense = LicenseService.getInstance().getLicenseDAO().selectBy(QLicense.license.group.eq
                    (group.getParentLevel1()));
            if (rootLicense == null) {
                return RestUtils.sendBadResponse("License cannot be applied, you need first to apply a license to " +
                        "root Group");
            }
            rootLicenseDetail = licenseService.getLicenseDetail(group.getParentLevel1(), false);
        } else if (group.getTreeLevel() > 2) {
            return RestUtils.sendBadResponse("License cannot be applied, license only supported for Root and Tenant " +
                    "Groups");
        }

        // 8. Verify max number of users in the group level.
        licenseDetail.setGroupId(groupId);
        if (licenseDetail.getMaxNumberOfUsers() != null) {
            Long count = UserController.count(licenseDetail);
            if (count > licenseDetail.getMaxNumberOfUsers()) {
                return RestUtils.sendBadResponse("License cannot be applied, you have exceed the maximum number of " +
                        "Users");
            }
        }

        // 9. Verify max number of groups in the group level.
        if (group.getTreeLevel() == 1) {
            if (licenseDetail.getMaxLevel2Groups() != null) {
                Long count = GroupService.getInstance().countAllActiveTenants();
                if (count > licenseDetail.getMaxLevel2Groups()) {
                    return RestUtils.sendBadResponse("License cannot be applied, you have exceed the maximum number " +
                            "of " +
                            "Tenant Groups");
                }
            }
        }

        // 10 . Verify max number of subgroups in the group level.
        if (licenseDetail.getMaxLevel3Groups() != null) {
            Long count = GroupController.countSubTenants(licenseDetail);
            if (count > licenseDetail.getMaxLevel3Groups()) {
                return RestUtils.sendBadResponse("License cannot be applied, you have exceed the maximum number of " +
                        "Sub Tenant Groups");
            }
        }
        // 11. Verify max thing types number.
        if (licenseDetail.getMaxThingTypes() != null) {
            try {
                Long count = (Long) Class.forName("com.tierconnect.riot.iot.controllers.ThingTypeController")
                        .getMethod("count", LicenseDetail.class).invoke(null, licenseDetail);
                if (count > licenseDetail.getMaxThingTypes()) {
                    return RestUtils.sendBadResponse("License cannot be applied, you have exceed the maximum number " +
                            "of Thing Types");
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        // 12. Verify max things number.
        if (licenseDetail.getMaxThings() != null) {
            try {
                Long count = (Long) Class.forName("com.tierconnect.riot.iot.controllers.ThingController").getMethod
                        ("count", LicenseDetail.class).invoke(null, licenseDetail);
                if (count > licenseDetail.getMaxThings()) {
                    return RestUtils.sendBadResponse("License cannot be applied, you have exceed the maximum number " +
                            "of Things");
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        /*
        //validate that tenant license is lower or equal than root license
        if (rootLicenseDetail != null) {
            if (!rootLicenseDetail.hasMoreorEqualPermissionsThan(licenseDetail)) {
                return RestUtils.sendBadResponse("License cannot be applied because has more power than rootLicense");
            }
        }
        */

        // 13. Delete the license and insert new license to group.
        License license = LicenseService.getInstance().getLicenseDAO().selectBy(QLicense.license.group.eq(group));
        if (license != null) {
            LicenseService.getInstance().delete(license);
        }
        licenseAsMap.put("groupId", groupId);

        license = new License();
        license.setGroup(group);
        license.setInstallTime(new Date());
        license.setLicenseString(LicenseServiceUtils.licenseMapToString(licenseAsMap));

        LicenseService.getInstance().insert(license);

        QToken qToken = QToken.token;
        Subject currentUser = SecurityUtils.getSubject();
        User user = ((User) currentUser.getPrincipal());

        List<Token> tokenObjects = TokenService.getInstance().getTokenDAO()
                .selectAllBy(qToken.user.eq(user).and(qToken.tokenActive.eq(true)));
        for (Token tokenObject: tokenObjects) {
            tokenObject.setTokenActive(false);
            tokenObject.setTokenExpirationTime(new Date());
            TokenCacheHelper.invalidate(tokenObject.getTokenString());
        }

        LicenseService.clearCaches();

        // Setting Native Authentication if license have not "LDAP Auth - 'root' Group" feature
        LicenseService.getInstance().setNativeAuthenticationToRootGroup();

        return RestUtils.sendOkResponse(LicenseServiceUtils.licenseStringToMap(license.getLicenseString()));
    }

    @PATCH
    @Path("/generate")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = {"license:u", "license:i", "license:r"}, logical = Logical.AND)
    @ApiOperation(position = 5, value = "Creates a License")
    public Response generateLicense(Map<String, Object> params, @Context HttpServletRequest request) {
        String certificateBase64 = (String) params.get("certificateBase64");
        String certificatePassword = (String) params.get("certificatePassword");
        byte[] certificate = Base64.decodeBase64(certificateBase64);
        Map aux = (Map) params.get("license");
        if (aux == null) {
            return RestUtils.sendBadResponse(String.format("Missing license"));
        }
        TreeMap<String, Object> map = new TreeMap<>(aux);
        if (map.isEmpty() || !map.containsKey("vendor") || !map.containsKey("product")) {
            return RestUtils.sendBadResponse(String.format("Invalid license"));
        }

        boolean correctKeyPair = false;
        KeyPair keyPair = null;

        if (!correctKeyPair) {
            keyPair = LicenseServiceUtils.loadPem(certificate, certificatePassword);
            if (keyPair != null) {
                correctKeyPair = true;
            } else {
                logger.error("It is not a valid PEM file");
            }
        }

        if (!correctKeyPair) {
            try {
                keyPair = (KeyPair) LicenseServiceUtils.fromBytes(certificate);
                if (keyPair != null) {
                    correctKeyPair = true;
                }
            } catch (Exception e) {
                logger.error("It is not a KeyPair file");
            }
        }


        if (!correctKeyPair) {
            return RestUtils.sendBadResponse(String.format("Invalid certificate file, we support only formats: PEM file and Java KeyPair serialized"));
        }

        if (keyPair.getPublic() == null) {
            logger.error("Your pem file needs to include both public and private key");
        }

        String base64PublicKey = LicenseServiceUtils.getPublicKeyAsB64String(keyPair.getPublic());
        String base64DefaultPublicKey = LicenseServiceUtils.getPublicKeyAsB64String(LicenseServiceUtils.getDefaultPublicKey());
        if (!base64DefaultPublicKey.equals(base64PublicKey)) {
            logger.error("The ViZix private license key is not valid. to register add public key: " + base64PublicKey);
            return RestUtils.sendBadResponse("The ViZix private license key is not valid. Please select a valid ViZix private license key and try again.");
        }

        try {
            User user = (User) SecurityUtils.getSubject().getPrincipal();
            Date date = new Date();
            map.put("creationDate", date);
            map.put("serialNumber", new SimpleDateFormat("YYYYMMddhhmmss").format(date));
            String signature = LicenseServiceUtils.generateSignature(map, keyPair.getPrivate());
            map.put("key", signature);
            logger.warn("****************************************************");
            logger.warn("****************************************************");
            logger.warn("A License has been generated");
            logger.warn(" User: " + user.getUsername());
            logger.warn(" Date: " + new SimpleDateFormat("YYYY/MM/dd hh:mm:ss ZZZ").format(date));
            logger.warn(" User IP: " + NetUtils.getClientIpAddress(request));
            logger.warn(" License File: " + map.get("product")+"_License-"+map.get("vendor")+"-"+map.get("customer")+"-"+map.get("serialNumber")+"-"+map.get("licenseType")+".key");
            logger.warn(" License: " + LicenseServiceUtils.licenseMapToString(map));
            logger.warn("****************************************************");
            logger.warn("****************************************************");

        } catch (Exception e) {
            return RestUtils.sendBadResponse(String.format("Error signing the license"));
        }

        return RestUtils.sendOkResponse(map);
    }
}

