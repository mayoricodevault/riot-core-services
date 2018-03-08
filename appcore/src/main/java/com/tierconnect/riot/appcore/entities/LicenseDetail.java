package com.tierconnect.riot.appcore.entities;

import com.tierconnect.riot.sdk.dao.UserException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Arrays.asList;

/**
 * Created by agutierrez on 4/29/15.
 */
public class LicenseDetail {
    private static ConcurrentHashMap<String, Feature> globalFeatures = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Module> globalModules = new ConcurrentHashMap<>();
    private static List<String> allResources = new CopyOnWriteArrayList<>();
    private static List<String> allFields = new CopyOnWriteArrayList<>();

    public static final String INTEGRATION_MD_7_BAKU = "Integration_MD7_Baku";

    public static final String INTEGRATION_GENETEC_BAKU = "Integration_Genetec_Baku";

    public static final String INTEGRATION_SAP_FMC = "Integration_SAP_FMC";

    public static final String SERVICES_LICENSE_GENERATOR = "Services_LicenseGenerator";
    public static final String LDAP_AUTH_ROOT_GROUP = "LDAP_Auth_root_Group";


    public static final String ESPER_RULES_PLUGIN = "Esper Rules Plugin";

    public static final String MACHINE_LEARNING = "Machine_Learning";

    public static final String RETAIL_REPLENISHMENT = "Retail - Replenishment";
    public static final String RETAIL_HOT_REPLENISHMENT = "Retail - Hot Replenishment";
    public static final String RETAIL_SEL_THRU_REPLENISHMENT = "Retail - Sell Thru Replenishment";
    public static final String RETAIL_DRESSING_ROOM = "Retail - Dressing Room";

    static {

        globalFeatures.put(ESPER_RULES_PLUGIN,new Feature("Esper Rules Plugin", asList("esperRulesPlugin"), null));
        globalFeatures.put(INTEGRATION_GENETEC_BAKU,new Feature("Integration - Genetec (Baku)", null, asList("genetecServer", "genetecPort", "genetecVideoLinksVisible")));
        globalFeatures.put(INTEGRATION_MD_7_BAKU,new Feature("Integration - MD7 (Baku)", null, asList("ipAddress")));
        globalFeatures.put(INTEGRATION_SAP_FMC,new Feature("Integration - SAP (FMC)", null, asList("fmcSapUrl", "fmcSapUsername", "fmcSapPassword", "fmcSapNumberOfRetries", "fmcSapWaitSecondsToRetry","fmcSapEnableSapSyncOnImport")));
        globalFeatures.put(SERVICES_LICENSE_GENERATOR,new Feature("Services - License Generator", asList("license_generator"), null));
        globalFeatures.put(MACHINE_LEARNING, new Feature("Analytics - Machine Learning", asList("mlExtraction", "mlModel", "mlPrediction", "mlBusinessModel"), null));
        //globalFeatures.put(LDAP_AUTH_ROOT_GROUP,new Feature("LDAP Auth - 'root' Group", asList("ldapRootGroup"), null));
        globalFeatures.put(RETAIL_REPLENISHMENT, new Feature("Mojix Retail - Replenishment Tile", asList("Mojix Retail App", "Replenishment"), null));
        globalFeatures.put(RETAIL_HOT_REPLENISHMENT, new Feature("Mojix Retail - Hot Replenishment Tile", asList("Mojix Retail App", "HotReplenishment"), null));
        globalFeatures.put(RETAIL_SEL_THRU_REPLENISHMENT, new Feature("Mojix Retail - Sell Thru Replenishment Tile", asList("Mojix Retail App", "SellThruReplenishment"), null));
        globalFeatures.put(RETAIL_DRESSING_ROOM, new Feature("Mojix Retail - Dressing Room Tile", asList("Mojix Retail App", "DressingRoom"), null));


        globalModules.put("Analytics",new Module(Collections.<String>singletonList("Analytics"), null));
        globalModules.put("Blockchain", new Module(asList("Blockchain", "smartContractParty", "smartContractDefinition", "smartcontract"), null));
        globalModules.put("Gateway", new Module(asList("Gateway", Resource.RESOURCE_BRIDGES_RULES_NAME, "Flows", "edgebox", "edgeboxRule"), null));
        globalModules.put("Model", new Module(asList("Model","mapMaker", "zone", "zoneGroup", "zonePoint", "zoneType", "localMap", "logicalReader"), null));
        globalModules.put("Services", new Module(asList("Services", /*TODO create*/"importExport", "connection", "connectionType", "healthAndStatus", "logs"), null));
        globalModules.put("Tenants", new Module(asList("Tenants", "user", "user_editRoamingGroup", "user_passwordExpirationPolicy", "groupType", "group", "field", "role", "resource", "shift", "license"), null));
        globalModules.put("Things", new Module(asList("Things", "thing", "thing_editOwn", "thingType", "thingTypeTemplate", "thingTypeFieldTemplate", "dataType"), null));
        // next lines were commented on 2017-03-02, please delete them if there are not error related with Monitor, Zones or reports
//        globalModules.put("Monitor",new Module(asList("Monitor", "healthAndStatus", "logs"), null));
//        globalModules.put("Zones",new Module(asList("Zones","zone", "zoneGroup", "zonePoint", "zoneType", "localMap", "logicalReader"), null));
//        globalModules.put("Reports",new Module(asList("Reports","reportDefinition", "reportEntryOption", "reportDefinition_editOwn", "reportDefinition_emailRecipients", "reportDefinition_assignThing", "reportDefinition_unAssignThing", "reportDefinition_inlineEdit"), null));
        List<String> allResourcesAux = new ArrayList<>();
        List<String> allFieldsAux = new ArrayList<>();
        for (Map.Entry<String, Feature> entry: globalFeatures.entrySet()) {
            Feature feature = entry.getValue();
            for (String resource: feature.getResources()) {
                allResourcesAux.add(resource);
            }
            for (String field: feature.getFields()) {
                allFieldsAux.add(field);
            }
        }
        for (Map.Entry<String, Module> entry: globalModules.entrySet()) {
            Module feature = entry.getValue();
            for (String resource: feature.getResources()) {
                allResourcesAux.add(resource);
            }
            for (String field: feature.getFields()) {
                allFieldsAux.add(field);
            }
        }
        allResources.addAll(allResourcesAux);
        allFields.addAll(allFieldsAux);

    }

    String vendor;
    String customer;
    String product;
    String licenseType;
    String serialNumber;
    String version;
    String description;
    Date expirationDate;
    Date creationDate;
    List<String> features = new ArrayList<>();
    List<String> modules = new ArrayList<>();
    Long maxNumberOfUsers;
    Long maxConcurrentUsers;
    Long maxLevel2Groups;
    Long maxLevel3Groups;
    Long maxThingTypes;
    Long maxThings;
    String serverIp;
    String clientIp;
    List<Long> applicableGroupLevel;
    boolean tenantLicenseInheritance;

    String key;
    Long groupId;

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getLicenseType() {
        return licenseType;
    }

    public void setLicenseType(String licenseType) {
        this.licenseType = licenseType;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public List<String> getFeatures() {
        return features != null ? features: new ArrayList<String>();
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public List<String> getModules() {
        return modules != null ? modules : new ArrayList<String>();
    }

    public void setModules(List<String> modules) {
        this.modules = modules;
    }

    public Long getMaxNumberOfUsers() {
        return maxNumberOfUsers;
    }

    public void setMaxNumberOfUsers(Long maxNumberOfUsers) {
        this.maxNumberOfUsers = maxNumberOfUsers;
    }

    public Long getMaxConcurrentUsers() {
        return maxConcurrentUsers;
    }

    public void setMaxConcurrentUsers(Long maxConcurrentUsers) {
        this.maxConcurrentUsers = maxConcurrentUsers;
    }

    public Long getMaxLevel2Groups() {
        return maxLevel2Groups;
    }

    public void setMaxLevel2Groups(Long maxLevel2Groups) {
        this.maxLevel2Groups = maxLevel2Groups;
    }

    public Long getMaxLevel3Groups() {
        return maxLevel3Groups;
    }

    public void setMaxLevel3Groups(Long maxLevel3Groups) {
        this.maxLevel3Groups = maxLevel3Groups;
    }

    public Long getMaxThingTypes() {
        return maxThingTypes;
    }

    public void setMaxThingTypes(Long maxThingTypes) {
        this.maxThingTypes = maxThingTypes;
    }

    public Long getMaxThings() {
        return maxThings;
    }

    public void setMaxThings(Long maxThings) {
        this.maxThings = maxThings;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public List<Long> getApplicableGroupLevel() {
        return applicableGroupLevel;
    }

    public void setApplicableGroupLevel(List<Long> applicableGroupLevel) {
        this.applicableGroupLevel = applicableGroupLevel;
    }

    public boolean isTenantLicenseInheritance() {
        return tenantLicenseInheritance;
    }

    public void setTenantLicenseInheritance(boolean tenantLicenseInheritance) {
        this.tenantLicenseInheritance = tenantLicenseInheritance;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public boolean hasResource(String name) {
        if (!allResources.contains(name)) {
            return  true;
        }
        for (String featureName : this.getFeatures()) {
            Feature feature = globalFeatures.get(featureName);
            if (feature != null && feature.getResources().contains(name)) {
                return true;
            }
        }
        for (String moduleName : this.getModules()) {
            Module module = globalModules.get(moduleName);
            if (module != null && module.getResources().contains(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasField(String name) {
        if (!allFields.contains(name)) {
            return  true;
        }
        for (String featureName : this.getFeatures()) {
            if (globalFeatures.get(featureName).getFields().contains(name)) {
                return true;
            }
        }
        for (String moduleName : this.getModules()) {
            if (globalModules.get(moduleName) == null) {
                throw new UserException("License for this group is invalid.");
            }
            if (globalModules.get(moduleName).getFields().contains(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasFeature(String name) {
        return features != null && features.contains(name);
    }

    public boolean hasModule(String module) {
        return modules != null && modules.contains(module);
    }

    public static ConcurrentHashMap<String, Feature> getGlobalFeatures() {
        return globalFeatures;
    }

    public static void setGlobalFeatures(ConcurrentHashMap<String, Feature> globalFeatures) {
        LicenseDetail.globalFeatures = globalFeatures;
    }

    public static ConcurrentHashMap<String, Module> getGlobalModules() {
        return globalModules;
    }

    public static void setGlobalModules(ConcurrentHashMap<String, Module> globalModules) {
        LicenseDetail.globalModules = globalModules;
    }
}
