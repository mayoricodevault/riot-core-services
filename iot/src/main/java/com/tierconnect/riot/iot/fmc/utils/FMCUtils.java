package com.tierconnect.riot.iot.fmc.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.commons.utils.CoordinateUtils;
import com.tierconnect.riot.iot.controllers.ThingController;
import com.tierconnect.riot.iot.dao.ZoneDAO;
import com.tierconnect.riot.iot.dao.mongo.SapMongoDAO;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.BrokerClientHelper;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.iot.services.ZoneService;
import com.tierconnect.riot.sdk.dao.MongoExecutionException;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
/**
 * Created by agutierrez on 3/17/15.
 */
public class FMCUtils {
    public static final String EVENT_TYPE_DELETE = "Delete";
    public static final String EVENT_TYPE_DISASSOCIATE = "Disassociate";
    public static final String EVENT_TYPE_ASSOCIATE = "Associate";
    public static final String EVENT_TYPE_CHANGE_STATUS = "ChangeStatus";
    public static final String EVENT_TYPE_SAP_SYNC = "SapSync";

    static Logger logger = Logger.getLogger(FMCUtils.class);

    //SAP returned Information
    public static final String SAP_USER_STATUS = "UserStatus";
    public static final String SAP_SYSTEM_STATUS = "SystemStatus";
    public static final String SAP_ASSET_NUM = "AssetNum";
    public static final String SAP_VALID_FROM_DATE = "ValidFromDate";
    public static final String SAP_VALID_TO_DATE = "ValidToDate";
    public static final String SAP_SERIAL_NUM = "SerialNum";
    public static final String SAP_MATERIAL_NUM = "MaterialNum";
    public static final String SAP_CATEGORY_CODE = "CategoryCode";
    public static final String SAP_OWNER = "Owner";
    public static final String SAP_ADMINISTRATOR = "Administrator";
    public static final String SAP_CURRENT_LOCATION = "CurrentLocation";
    public static final String SAP_MAIN_PLANT = "MainPlant";
    public static final String SAP_DESCRIPTION = "Description";

    //Real State and information Sent to SAP, we have to include in this Real State also the serial number of the Asset
    public static final String EVENT_TYPE = "Event";
    public static final String EVENT_TAG_ID = "TagID";

    public static final String EVENT_TIME = "lastEventTime";
    public static final String EVENT_USER = "User";
    public static final String EVENT_PLANT = "Plant";

    //if it is synchronized with SAP
    public static final String SAP_SYNC_STATUS = "SAPSync";

    public enum SAP_SYNC {
        AVAILABLE_RETRIES("SapSyncAvailableRetries"),
        RETRIES("SapSyncRetries"),

        EVENT("SAPSyncEvent"),
        TIME("SAPSyncTime"),
        USER("SAPSyncUser"),
        TAG_ID("SAPSyncTagID"),
        PLANT("SAPSyncPlant"),
        SCAN_ZONE("SAPSyncScanZone"),

        ERROR_MESSAGE("SAPSyncErrorMessage"),
        INPUT_MESSAGE("SAPSyncInput"),
        OUTPUT_MESSAGE("SAPSyncOutput"),
        ;
        private String value;

        private SAP_SYNC(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public static final String DISASSOCIATE_CODE = "02";
    public static final String ASSOCIATE_CODE = "01";
    public static final String DELETE_CODE = "03";

    public static int getMaxNumberOfRetries(User user) {
        Long aux = ConfigurationService.getAsLong(user, "fmcSapNumberOfRetries");
        return (aux != null) ? aux.intValue() : 5;
    }

    public static long getMinimumTimeSecondsToWaitBetweenRetries(User user) {
        Long aux = ConfigurationService.getAsLong(user, "fmcSapWaitSecondsToRetry");
        return (aux != null) ? aux.intValue()*1000 : 5000L;
    }

    public static void fmcHandleUpdateUdfs(Thing thing , Map<String, Object> udfs, Date storeDate, User currentUser) {
        Map scanZone = (Map) udfs.get(FMCConstants.FMC_SCAN_ZONE);
        Map tagZone = (Map) udfs.get("zone");
        if (scanZone != null) {
            fmcHandleUpdateAssetScanZone(thing, thing.getThingTypeField(FMCConstants.FMC_SCAN_ZONE), (String) scanZone.get("value"), storeDate, currentUser, "" + new Date().getTime(), 1, true);
        } else if (tagZone != null) {
            fmcHandleUpdateTagZone(thing, thing.getThingTypeField("zone"), (String) tagZone.get("value"), storeDate);
        }
    }

    public static void fmcHandleUpdateTagZone(Thing tag, ThingTypeField tagThingTypeField, String valueTag, Date storeDate) {
        if (tag == null || tagThingTypeField == null || valueTag == null) {
            return;
        }
        Thing parent = tag.getParent();
        if (parent == null) {
            return;
        }
        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();
        long startTime = (new Date()).getTime();
        String transactionCode = "" + startTime;
        long operationId = 1L;
        String tagThingTypeCode = tag.getThingType().getThingTypeCode();
        String parentThingTypeCode = parent.getThingType().getThingTypeCode();
        if (tagThingTypeCode.contains(FMCConstants.FMC) && tagThingTypeCode.contains(FMCConstants.TAG) && tagThingTypeField.getName().toLowerCase().equals("zone") &&  parentThingTypeCode.contains(FMCConstants.FMC) && parentThingTypeCode.contains(FMCConstants.ASSET)) {
            logger.warn("---fmcHandleUpdateTagZone---");
            Zone zone;
            ZoneDAO zoneDAO = ZoneService.getInstance().getZoneDAO();
            try {
                zone = zoneDAO.selectBy(QZone.zone.id.eq(Long.valueOf(valueTag)));
            } catch (NumberFormatException ex) {
                zone = zoneDAO.selectBy(QZone.zone.code.eq(valueTag));
            }
            ThingTypeField thingTypeField = parent.getThingType().getThingTypeFieldByName(FMCConstants.FMC_SCAN_ZONE);
            String value = zone.getCode();
            Map<String, Object> storeDateMapThing = new HashMap<>();
            ThingService thingService = ThingService.getInstance();
            setUdf(parent, storeDateMapThing, FMCConstants.FMC_SCAN_ZONE, value);
            setUdf(parent, storeDateMapThing, EVENT_PLANT, zone.getGroup().getCode());
            Stack<Long> recursivelyStack = new Stack<>();
            thingService.update(recursivelyStack, parent, null, storeDateMapThing, null, null, false, true, storeDate, true, currentUser, true);
            BrokerClientHelper.sendRefreshSingleThingMessage(parent.getThingType().getThingTypeCode(), parent.getSerial(),
                    true, GroupService.getInstance().getMqttGroups(parent.getGroup()));
            FMCUtils.fmcHandleUpdateAssetScanZone( parent, thingTypeField, value, storeDate, currentUser, transactionCode, operationId, false);
        }
    }

    public static void fmcHandleUpdateAssetScanZone(Thing thing, ThingTypeField thingTypeField, String value, Date storeDate, User currentUser, String tCode, long operationId, boolean updateChild) {
        QThing qThing = QThing.thing;
        ZoneService zoneService = ZoneService.getInstance();
        //Thing thing = thingTypeField.getThing();
        Stack<Long> recursivelyStack = new Stack<>();
        String thingTypeCode = thing.getThingType().getThingTypeCode();
        if (thingTypeCode.contains(FMCConstants.FMC) && thingTypeCode.contains(FMCConstants.ASSET) && thingTypeField.getName().equals(FMCConstants.FMC_SCAN_ZONE)) {
            logger.warn("---fmcHandleUpdateAssetScanZone---");
            ThingService thingService = ThingService.getInstance();
            Map<String, Object> storeDateMapThing = new HashMap<>();
            Map<String, Object> storeDateMapChild = new HashMap<>();
            for (SAP_SYNC field : SAP_SYNC.values()) {
                setUdf(thing, storeDateMapThing, field.toString(), null);
            }
            setUdf(thing, storeDateMapThing, EVENT_TYPE, EVENT_TYPE_CHANGE_STATUS);
            setUdf(thing, storeDateMapThing, EVENT_TIME, storeDate.getTime() + "");
            setUdf(thing, storeDateMapThing, EVENT_USER, currentUser.getUsername());
            try {
                setUdf(thing, storeDateMapThing, FMCConstants.FMC_STATUS, value.substring(value.indexOf("_") + 1).toUpperCase());
            } catch (Exception ex) {
                logger.error(ex.getMessage());
            }
            List<Zone> zones = zoneService.getZoneDAO().selectAllBy(QZone.zone.code.eq(value));
            List<Thing> children = ThingService.getThingDAO().selectAllBy(qThing.parent.eq(thing).and(qThing.thingType.thingTypeCode.contains(FMCConstants.TAG)));
            {
                for (Thing child : children) {
                    setUdf(thing, storeDateMapThing, EVENT_TAG_ID, child.getSerial());
                    if (zones.size() == 1) { //TODO this should be filtered by tenant of the thing
                        Zone zone = zones.get(0);
                        setUdf(thing, storeDateMapThing, EVENT_PLANT, zone.getGroup().getCode());
                        if (updateChild) {
                            //setUdf(child, storeDateMapChild, "lastDetectTime", storeDate.getTime() + "");
                            //setUdf(child, storeDateMapChild, "lastLocateTime", storeDate.getTime() + "");

                            setUdf(child,storeDateMapChild, "zone", zone.getCode());
                            /*String[] centroid = getCalculatedLocationsCenters(zone)git t;
                            if (centroid.length == 2) {
                                setUdf(child, storeDateMapChild, "location", centroid[0]);
                            }
                            if (centroid.length == 2) {
                                setUdf(child, storeDateMapChild, "locationXYZ", centroid[1]);
                            }*/
                            Map<String,Object> parentMap = new HashMap<>();
                            parentMap.put("serialNumber", thing.getSerial());
                            parentMap.put("thingTypeCode", thing.getThingType().getThingTypeCode());

                            BrokerClientHelper.sendRefreshSingleThingMessage(child.getThingType().getThingTypeCode(),
                                    child.getSerial(), true, GroupService.getInstance().getMqttGroups(child.getGroup()));
                        }
                        try {
                            SapMongoDAO.getInstance().insertSap("statusChange", 0, thing.getId(), child.getSerial(),
                                    currentUser.getId(), zone.getId(), storeDate.getTime(), tCode, operationId, thing.getSerial());
                        } catch (MongoExecutionException e) {
                            e.printStackTrace();
                        }
//                        FMCSyncStatusChangeJob.reschedule(0, thing.getId(), child.getSerial(), currentUser.getId(), zone.getId(), storeDate.getTime(), tCode, operationId);
                    }
                }
            }
            thingService.update(recursivelyStack, thing, null, storeDateMapThing, null, null, false, true, storeDate, true, currentUser, true);
            BrokerClientHelper.sendRefreshSingleThingMessage(thing.getThingType().getThingTypeCode(), thing.getSerial(),
                    true, GroupService.getInstance().getMqttGroups(thing.getGroup()));
        }
    }

    public static void fmcHandleAssignTag(Thing parent, Thing child, Group group, Date storeDate, User currentUser) {
        fmcHandleAssignTag( parent,  child,  group,  storeDate,  currentUser,  new Date().getTime()+"", 1L);
    }

    public static void fmcHandleAssignTag(Thing parent, Thing child, Group group, Date storeDate, User currentUser,  String tCode, long operationId) {
        Thing thing = parent;
        String parentThingTypeCode = parent.getThingType().getThingTypeCode();
        String childThingTypeCode = child.getThingType().getThingTypeCode();
        if (parentThingTypeCode.contains(FMCConstants.FMC) && parentThingTypeCode.contains(FMCConstants.ASSET)
                && childThingTypeCode.contains(FMCConstants.FMC) && childThingTypeCode.contains(FMCConstants.TAG)) {
            logger.warn("---fmcHandleAssignTag---");
            ThingService thingService = ThingService.getInstance();
            Map<String, Object> storeDateMap = new HashMap<>();
            for (SAP_SYNC field : SAP_SYNC.values()) {
                setUdf(thing, storeDateMap, field.toString(), null);
            }
            setUdf(thing, storeDateMap, EVENT_TYPE, EVENT_TYPE_ASSOCIATE);
            setUdf(thing, storeDateMap, EVENT_TIME, storeDate.getTime() + "");
            setUdf(thing, storeDateMap, EVENT_TAG_ID, child.getSerial());
            setUdf(thing, storeDateMap, EVENT_USER, currentUser.getUsername());
            if (group == null) {
                group = getGroupForPlantFMC(thing, child);
            }
            //setUdf(thing, storeDateMap, EVENT_PLANT, group.getCode());
            Stack<Long> recursivelyStack = new Stack<>();
            thingService.update(recursivelyStack, thing, null, storeDateMap, null, null, false, true, storeDate, true, currentUser, true);
            BrokerClientHelper.sendRefreshSingleThingMessage(thing.getThingType().getThingTypeCode(), thing.getSerial(),
                    true, GroupService.getInstance().getMqttGroups(thing.getGroup()));
            try {
                SapMongoDAO.getInstance().insertSap("assign", 0, thing.getId(), child.getSerial(),
                        currentUser.getId(), group != null ? group.getId(): 0L, storeDate.getTime(), tCode, operationId, thing.getSerial());
            } catch (MongoExecutionException e) {
                e.printStackTrace();
            }
//            FMCSyncAssignJob.reschedule(0, parent.getId(), child.getSerial(), currentUser.getId(), group != null ? group.getId(): 0L, storeDate.getTime(), tCode, operationId);
        }
    }

    public static Group getGroupForPlantFMC(Thing thing, Thing child) {
        Group group=null;
        ZoneService zoneService = ZoneService.getInstance();
        ThingMongoDAO thingMongoDAO = ThingMongoDAO.getInstance();

        String where = "_id=" + child.getId();
        List<String> fields = new ArrayList<>();
        fields.add("zone.value");
        List fieldValues = (List) thingMongoDAO.getThingUdfValues(where, null, fields, null).get("results");
        if (fieldValues != null && fieldValues.size() > 0 && ((Map)fieldValues.get(0)).get("zone.value")!=null) {
              Map map = (Map) fieldValues.get(0);
              Long id = (Long) ((Map) map.get("zone.value")).get("id");
              Zone zone = zoneService.get(id);
              group = zone.getGroup();
        }

        if (group == null) {
            where = "_id=" + thing.getId();
            fields = new ArrayList<>();
            fields.add("ScanZone.value");
            fieldValues = (List) thingMongoDAO.getThingUdfValues(where, null, fields, null).get("results");
            if (fieldValues != null && fieldValues.size() > 0 && ((Map)fieldValues.get(0)).get("ScanZone.value")!=null) {
                Map map  = (Map) fieldValues.get(0);
                Long id = (Long) ((Map) map.get("ScanZone.value")).get("id");
                Zone zone = zoneService.get(id);
                group = zone.getGroup();
            }
        }
        return group;
    }

    public static void fmcHandleUnAssignTag(Thing parent, Thing child, Group group, Date storeDate, User currentUser) {
        fmcHandleUnAssignTag(parent, child, group, storeDate, currentUser, new Date().getTime() + "", 1L);
    }

    public static void fmcHandleUnAssignTag(Thing parent, Thing child, Group group, Date storeDate, User currentUser,  String tCode, long operationId) {
        Thing thing = parent;
        String parentThingTypeCode = parent.getThingType().getThingTypeCode();
        String childThingTypeCode = child.getThingType().getThingTypeCode();
        if (parentThingTypeCode.contains(FMCConstants.FMC) && parentThingTypeCode.contains(FMCConstants.ASSET)
                && childThingTypeCode.contains(FMCConstants.FMC) && childThingTypeCode.contains(FMCConstants.TAG)) {
            logger.warn("---fmcHandleUnAssignTag---");
            ThingService thingService = ThingService.getInstance();
            Map<String, Object> storeDateMap = new HashMap<>();
            Map<String, Object> storeDatePlusTenMap = new HashMap<>();

            for (SAP_SYNC field : SAP_SYNC.values()) {
                setUdf(thing, storeDateMap, field.toString(), null);
            }
            setUdf(thing, storeDateMap, EVENT_TYPE, EVENT_TYPE_DISASSOCIATE);
            setUdf(thing, storeDateMap, EVENT_TIME, storeDate.getTime() + "");
            setUdf(thing, storeDateMap, EVENT_TAG_ID, child.getSerial());
            setUdf(thing, storeDateMap, EVENT_USER, currentUser.getUsername());
            if (group == null) {
                group = getGroupForPlantFMC(thing, child);
            }
            //setUdf(thing, storeDateMap, EVENT_PLANT, group.getCode());
            List<Long> groupMqtt = GroupService.getInstance().getMqttGroups(thing.getGroup());
            Stack<Long> recursivelyStack = new Stack<>();
            thingService.update(recursivelyStack, thing, null, storeDateMap, null, null, false, true, storeDate, true, currentUser, true);
            BrokerClientHelper.sendRefreshSingleThingMessage(thing.getThingType().getThingTypeCode(), thing.getSerial(),
                    true, groupMqtt);

            Date storeDatePlusTen = new Date(storeDate.getTime() + 10);
            recursivelyStack = new Stack<>();
            setUdf(thing, storeDatePlusTenMap, EVENT_TAG_ID, "");
            thingService.update(recursivelyStack, thing, null, storeDatePlusTenMap, null, null, false, true, storeDatePlusTen, true, currentUser, true);
            BrokerClientHelper.sendRefreshSingleThingMessage(thing.getThingType().getThingTypeCode(), thing.getSerial(),
                    true, groupMqtt);
            try {
                SapMongoDAO.getInstance().insertSap("unAssign", 0, thing.getId(), child.getSerial(),
                        currentUser.getId(), group != null ? group.getId(): 0L, storeDate.getTime(), tCode, operationId, thing.getSerial());
            } catch (MongoExecutionException e) {
                e.printStackTrace();
            }
//            FMCSyncUnAssignJob.reschedule(0, parent.getId(), child.getSerial(), currentUser.getId(), group != null ? group.getId(): 0L, storeDate.getTime(), tCode, operationId);
        }
    }

    public static void fmcHandleDeleteAsset(Thing parent, Group group, Date storeDate, User currentUser) {
        fmcHandleDeleteAsset(parent, group, storeDate, currentUser, new Date().getTime() + "", 1L);
    }

    public static void fmcHandleDeleteAsset(Thing parent, Group group, Date storeDate, User currentUser,  String tCode, long operationId) {
        String parentThingTypeCode = parent.getThingType().getThingTypeCode();
        if (parentThingTypeCode.contains(FMCConstants.FMC) && parentThingTypeCode.contains(FMCConstants.ASSET)) {
            try {
                SapMongoDAO.getInstance().insertSap("delete", 0, 0L, parent.getSerial(),
                        currentUser.getId(), group.getId(), storeDate.getTime(), tCode, operationId, parent.getSerial());
            } catch (MongoExecutionException e) {
                e.printStackTrace();
            }
//            FMCSyncDeleteJob.reschedule(0, parent.getSerial(), currentUser.getId(), group.getId(), storeDate.getTime(), tCode, operationId);
        }
    }


    public static void fmcHandleSyncTag(Thing thing, Map<String, Object> storeDateMap, Map result) {
        setUdf2(thing, storeDateMap, SAP_USER_STATUS, (String) result.get("UserStatus"));
        setUdf2(thing, storeDateMap, SAP_SYSTEM_STATUS, (String) result.get("SystemStatus"));
        setUdf2(thing, storeDateMap, SAP_ASSET_NUM, (String) result.get("AssetNum"));
        setUdf2(thing, storeDateMap, SAP_VALID_FROM_DATE, (String) result.get("ValidFromDate"));
        setUdf2(thing, storeDateMap, SAP_VALID_TO_DATE, (String) result.get("ValidToDate"));
        setUdf2(thing, storeDateMap, SAP_SERIAL_NUM, (String) result.get("SerialNum"));
        setUdf2(thing, storeDateMap, SAP_MATERIAL_NUM, (String) result.get("MaterialNum"));
        setUdf2(thing, storeDateMap, SAP_CATEGORY_CODE, (String) result.get("CategoryCode"));
        setUdf2(thing, storeDateMap, SAP_OWNER, (String) result.get("Owner"));
        setUdf2(thing, storeDateMap, SAP_ADMINISTRATOR, (String) result.get("Administrator"));
        setUdf2(thing, storeDateMap, SAP_CURRENT_LOCATION, (String) result.get("CurrentLocation"));
        setUdf2(thing, storeDateMap, SAP_MAIN_PLANT, (String) result.get("MainPlant"));
        setUdf2(thing, storeDateMap, SAP_DESCRIPTION, (String) result.get(SAP_DESCRIPTION));
    }

    public static String[] getCalculatedLocationsCenters(Zone zone) {
        String[] result = new String[]{"",""};
        try {
            Set<ZonePoint> zonePoints = zone.getZonePoints();
            List<double[]> points = new ArrayList<>();
            for (ZonePoint zonePoint : zonePoints) {
                points.add(new double[]{zonePoint.getX(), zonePoint.getY()});
            }
            double[] center = calculateCentroid(points);
            result[0]  = center[0] + ";" + center[1] + ";0.0";
            LocalMap localMap = zone.getLocalMap();
            if (localMap != null) {
                double lonOrigin = localMap.getLonOrigin() != null ? localMap.getLonOrigin() : 0;
                double latOrigin = localMap.getLatOrigin() != null ? localMap.getLatOrigin() : 0;
                double altOrigin = localMap.getAltOrigin() != null ? localMap.getAltOrigin() : 0;
                double declination = localMap.getDeclination() != null ? localMap.getDeclination() : 0;
                String imageUnit = localMap.getImageUnit();
                CoordinateUtils cu = new CoordinateUtils(lonOrigin, latOrigin, altOrigin, declination, imageUnit);
                double[] xyz = cu.lonlat2xy(center[0], center[1], 0);
                result[1] = xyz[0] + ";" + xyz[1] + ";" + +xyz[2];
            }
        } catch (Exception ex) {
            logger.error (ex.getMessage(), ex);
        }
        return result;
    }


    static public double[] calculateCentroid( List<double[]> points )
    {
        double xsum = 0;
        double ysum = 0;
        for( int i = 0; i < points.size(); i++ )
        {
            xsum += points.get( i )[0];
            ysum += points.get( i )[1];
        }
        return new double[] { xsum / points.size(), ysum / points.size() };
    }


    static CloseableHttpClient client;

    static {
        client = getHttpClient();
    }

    private static CloseableHttpClient getHttpClient() {
        SSLContextBuilder builder = new SSLContextBuilder();
        try {
            builder.loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            });
            //builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        SSLConnectionSocketFactory sslsf = null;
        try {
            sslsf = new SSLConnectionSocketFactory(builder.build(), SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return HttpClients.custom().setSSLSocketFactory(sslsf).build();
    }

    public static Map sendMessageToSAP(Map message, User user, String tCode, long operationId) {
        String sapUri = ConfigurationService.getAsString(user, "fmcSapUrl");
        String sapUsername = ConfigurationService.getAsString(user, "fmcSapUsername");
        String sapPassword = ConfigurationService.getAsString(user, "fmcSapPassword");
        File logFile = ThingController.getLogFile(user, tCode);
        return sendMessageToSAP(message, sapUri, sapUsername, sapPassword, logFile, tCode, operationId);

    }

    public static Map sendMessageToSAP(Map message, String url, String username, String password, File logFile, String tCode, long operationId)  {
        ObjectMapper objectMapper = new ObjectMapper();
//        String hardCodedResponse = "{\"RFIDEquipmentDetails_MT\":{\"Description\":\"GV 2 1/16-10K MAN STDD\",\"UserStatus\":\"0006\",\"SystemStatus\":\"AVLB\",\"AssetNum\":null,\"ValidFromDate\":\"2012-02-16\",\"ValidToDate\":\"9999-12-31\",\"SerialNum\":\"000000000000501887\",\"MaterialNum\":\"90-130-316\",\"CategoryCode\":\"S\",\"Owner\":null,\"Administrator\":null,\"CurrentLocation\":null,\"MaintPlant\":\"7370\",\"ServiceCallStatus\":\"0\"}}";
////        String hardCodedResponse = "{\"RFIDEquipmentDetails_MT\":{\"Description\":\"GV 4 1/16-15K HYD STDD\",\"UserStatus\":\"0006\",\"SystemStatus\":\"AVLB\",\"AssetNum\":\"000006024918\",\"ValidFromDate\":\"2013-11-22\",\"ValidToDate\":\"9999-12-31\",\"SerialNum\":\"2006-01-276T\",\"MaterialNum\":\"P150640\",\"CategoryCode\":\"R\",\"Owner\":null,\"Administrator\":null,\"CurrentLocation\":null,\"MaintPlant\":\"7320\",\"ServiceCallStatus\":\"0\"}}\n";
//        int i = 10;
//        if (i == 10) {
//            try {
//                Map<String, Object> hardCodedResult = objectMapper.readValue(hardCodedResponse, HashMap.class);
//                return hardCodedResult;
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
        long startTime = System.currentTimeMillis();
        Map finalResult = null;
        boolean error = false;
        String errorMessage = null;
        try {
            if (logFile != null) {
                logger.warn("writing log file: " + logFile.getAbsolutePath());
                FileUtils.writeStringToFile(logFile, "TransactionCode: " + tCode + "\r\n", true);
                FileUtils.writeStringToFile(logFile, "OperationId:" + operationId + "\r\n", true);
                FileUtils.writeStringToFile(logFile, "Sap User: " + username + "\r\n", true);
            }
            if (url == null) {
                throw new UserException("invalid url for sap synchronization ");
            }
            HttpPost httpPost = new HttpPost(url);
            String jsonBody = "{}";
            if (message != null) {
                jsonBody = objectMapper.writeValueAsString(message);
            }


            logger.warn("Sending message to SAP, transactionCode: " + tCode + ", operation: " + operationId + ", input: " + jsonBody);
            final RequestConfig params = RequestConfig.custom().setConnectTimeout(10000).setSocketTimeout(10000).build();
            httpPost.setConfig(params);
            httpPost.setEntity(new StringEntity(jsonBody));

            HttpUriRequest request = httpPost;
            request.addHeader("content-type", "application/json");
            String encoding = Base64.encodeBase64String((username + ":" + password).getBytes("UTF-8"));
            request.setHeader("Authorization", "Basic " + encoding);

            if (logFile != null) {
                FileUtils.writeStringToFile(logFile, "Input:\r\n", true);
                FileUtils.writeStringToFile(logFile, jsonBody + "\r\n", true);
            }

            CloseableHttpClient client = FMCUtils.client;
            HttpResponse httpResponse = client.execute(request);

            HttpEntity entity = httpResponse.getEntity();
            StringBuffer responseStringBuffer = new StringBuffer();
            if (entity != null && entity.getContent() != null) {
                BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    responseStringBuffer.append(inputLine);
                }
                in.close();
            }
            String responseString = responseStringBuffer.toString();
            int statusCode = httpResponse.getStatusLine().getStatusCode();

            logger.warn("Receiving message from SAP, transactionCode: " + tCode+ ", operation: " + operationId + ", output: "+ responseString);

            if (logFile != null) {
                FileUtils.writeStringToFile(logFile, "Output: Status " + statusCode + " Body" + "\r\n", true);
                FileUtils.writeStringToFile(logFile, responseString + "\r\n", true);
            }

            if (StringUtils.isNotBlank(responseString)) {
                Map<String, Object> mapResult = new HashMap<String, Object>();
                try {
                    mapResult = objectMapper.readValue(responseString, HashMap.class);
                    if (statusCode == 401 || statusCode == 403) {
                        error = true;
                        errorMessage = "Invalid Credentials to connect to SAP";
                    } else if (statusCode == 200 || statusCode == 201) {
                        String error_ = (String) mapResult.get("error");
                        if (StringUtils.isNotEmpty(error_)) {
                            error = true;
                            errorMessage = error_;
                        }
                        Map aux = (Map) mapResult.get("RFIDEquipmentDetails_MT");
                        if (aux != null) {
                            error_ = (String) aux.get("ErrorMessage");
                            if (StringUtils.isNotEmpty(error_)) {
                                error = true;
                                errorMessage = error_;
                            }
                        }
                    } else {
                        error = true;
                    }
                    finalResult = mapResult;
                } catch (Exception ex) {
                    error = true;
                    errorMessage = ex.getMessage();
                }
            }  else {
                error = true;
                errorMessage = "Empty Response from SAP";
            }
        } catch (Exception ex) {
            logger.warn("Error Sending message to SAP, transactionCode: " + tCode+ ", operation: " + operationId + ", exception: "+ (StringUtils.isNotEmpty(ex.getMessage())? ex.getMessage() : ex.getClass().getSimpleName()));
            try {
                if (logFile != null) {
                    FileUtils.writeStringToFile(logFile, "Status: exception \r\n", true);
                    FileUtils.writeStringToFile(logFile, ExceptionUtils.getStackTrace(ex) + "\r\n", true);
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
            if (ex instanceof java.net.UnknownHostException || ex instanceof UserException || ex instanceof IllegalStateException) {
                throw new UserException(ex.getMessage());
            } else {
                throw new RuntimeException(StringUtils.isNotEmpty(ex.getMessage())? ex.getMessage(): ex.getClass().getSimpleName());
            }
        } finally {
            try {
                if (logFile != null) {
                    FileUtils.writeStringToFile(logFile, "Status: " + (error ? " error " + errorMessage : " success" + "\r\n"), true);
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
            long finalTime = System.currentTimeMillis();
            try {
                if (logFile != null) {
                    FileUtils.writeStringToFile(logFile, "Time: start " + startTime + " end " + finalTime + " elapsed " + (finalTime - startTime) + "\r\n", true);
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
        return finalResult;
    }

    public static String getDate(long timeStamp) {
        DateTimeFormatter isoDateFormat = ISODateTimeFormat.dateTime();
        String isoDateStr = isoDateFormat.print(new DateTime(timeStamp));
        return isoDateStr;
    }

    public static void setUdf(Thing thing, Map<String, Object> udfs, String name, Object value) {
        if (thing != null && thing.getThingTypeField(name) != null) {
            Map<String, Object> udf = new HashMap<>();
            udf.put("value", value);
            udfs.put(name, udf);
        }
    }
    public static void setUdf2(Thing thing, Map<String, Object> udfs, String name, Object valueN) {
        if (thing.getThingTypeField(name) != null) {
            if (StringUtils.isNotEmpty(valueN == null ? "" : valueN.toString()) && !valueN.toString().toLowerCase().equals("null")) {
                Map<String, Object> udf = new HashMap<>();
                udf.put("value", valueN);
                udfs.put(name, udf);
            }
        }
    }

    public static void fmcHandleSapSendMessage(String event, Map message, Map messageDetail, Thing parent, User user, Date sapSyncStoreDate, Date handHeldEventDate, int nRetries, String tCode, long operationId, long sapId) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();
        ThingService thingService = ThingService.getInstance();
        Map<String, Object> udfsSapSyncStoreDate = new HashMap<>();
        Map<String, Object> udfsHandHeldEventDate = new HashMap<>();

        try {

            setUdf(parent, udfsSapSyncStoreDate, EVENT_TYPE, EVENT_TYPE_SAP_SYNC);
            setUdf(parent, udfsSapSyncStoreDate, EVENT_TIME, handHeldEventDate.getTime() + "");
            setUdf(parent, udfsSapSyncStoreDate, SAP_SYNC.EVENT.toString(), event);
            setUdf(parent, udfsSapSyncStoreDate, SAP_SYNC.TAG_ID.toString(), (String) messageDetail.get("TagID"));
            setUdf(parent, udfsSapSyncStoreDate, SAP_SYNC.PLANT.toString(), (String) messageDetail.get("Plant"));
            setUdf(parent, udfsSapSyncStoreDate, SAP_SYNC.SCAN_ZONE.toString(), (String) messageDetail.get("ScanZone"));
            setUdf(parent, udfsSapSyncStoreDate, SAP_SYNC.USER.toString(), (String) messageDetail.get("User"));
            setUdf(parent, udfsSapSyncStoreDate, SAP_SYNC.INPUT_MESSAGE.toString(), objectMapper.writeValueAsString(message));

            setUdf(parent, udfsSapSyncStoreDate, SAP_SYNC.AVAILABLE_RETRIES.toString(), (getMaxNumberOfRetries(user) - (nRetries + 1)));
            setUdf(parent, udfsSapSyncStoreDate, SAP_SYNC.RETRIES.toString(), (nRetries + 1));
            setUdf(parent, udfsSapSyncStoreDate, SAP_SYNC.TIME.toString(), "" + sapSyncStoreDate.getTime());

            if (parent != null) {
                Stack<Long> recursivelyStack = new Stack<>();
                thingService.update(recursivelyStack, parent, null, udfsSapSyncStoreDate, null, null, false, true, sapSyncStoreDate, true, currentUser, true);
                BrokerClientHelper.sendRefreshSingleThingMessage(parent.getThingType().getThingTypeCode(), parent.getSerial(),
                        true, GroupService.getInstance().getMqttGroups(parent.getGroup()));
            }

            setUdf(parent, udfsHandHeldEventDate, SAP_SYNC.EVENT.toString(), event);
            setUdf(parent, udfsHandHeldEventDate, SAP_SYNC.TAG_ID.toString(), (String) messageDetail.get("TagID"));
            setUdf(parent, udfsHandHeldEventDate, SAP_SYNC.PLANT.toString(), (String) messageDetail.get("Plant"));
            setUdf(parent, udfsHandHeldEventDate, SAP_SYNC.SCAN_ZONE.toString(), (String) messageDetail.get("ScanZone"));
            setUdf(parent, udfsHandHeldEventDate, SAP_SYNC.USER.toString(), (String) messageDetail.get("User"));
            setUdf(parent, udfsHandHeldEventDate, SAP_SYNC.INPUT_MESSAGE.toString(), objectMapper.writeValueAsString(message));

            setUdf(parent, udfsHandHeldEventDate, SAP_SYNC.AVAILABLE_RETRIES.toString(),  (getMaxNumberOfRetries(user) - (nRetries + 1)));
            setUdf(parent, udfsHandHeldEventDate, SAP_SYNC.RETRIES.toString(), (nRetries + 1));
            setUdf(parent, udfsHandHeldEventDate, SAP_SYNC.TIME.toString(), sapSyncStoreDate.getTime());

            if (parent != null) {
                Stack<Long> recursivelyStack = new Stack<>();
                thingService.update(recursivelyStack, parent, null, udfsHandHeldEventDate, null, null, false, true, handHeldEventDate, true, currentUser, true);
                BrokerClientHelper.sendRefreshSingleThingMessage(parent.getThingType().getThingTypeCode(), parent.getSerial(),
                        true, GroupService.getInstance().getMqttGroups(parent.getGroup()));
            }
            udfsSapSyncStoreDate = new HashMap<>();
            udfsHandHeldEventDate = new HashMap<>();

            Map result = sendMessageToSAP(message, user, tCode, operationId);
            boolean error = true;
            if (result != null && result.get("RFIDEquipmentDetails_MT") != null) {
                Map responseDetail = (Map) result.get("RFIDEquipmentDetails_MT");
                setUdf(parent, udfsSapSyncStoreDate, SAP_SYNC.OUTPUT_MESSAGE.toString(), objectMapper.writeValueAsString(message));
                setUdf(parent, udfsHandHeldEventDate, SAP_SYNC.OUTPUT_MESSAGE.toString(), objectMapper.writeValueAsString(message));
                if ("0".equals(((Map)result.get("RFIDEquipmentDetails_MT")).get("ServiceCallStatus"))) {
                    error = false;
                }

                setUdf(parent, udfsSapSyncStoreDate, SAP_SYNC_STATUS, (!error ? "true" : "false"));
                setUdf(parent, udfsHandHeldEventDate, SAP_SYNC_STATUS, (!error ? "true" : "false"));
                if (error) {
                    setUdf(parent, udfsSapSyncStoreDate, SAP_SYNC.ERROR_MESSAGE.toString(), (String) responseDetail.get("ErrorMessage"));
                    setUdf(parent, udfsHandHeldEventDate, SAP_SYNC.ERROR_MESSAGE.toString(), (String) responseDetail.get("ErrorMessage"));
                    SapMongoDAO.getInstance().updateSapOKorError(sapId, result, "error");
                } else {
                    setUdf(parent, udfsSapSyncStoreDate, SAP_SYNC.ERROR_MESSAGE.toString(), "");
                    setUdf(parent, udfsHandHeldEventDate, SAP_SYNC.ERROR_MESSAGE.toString(), "");
                    fmcHandleSyncTag(parent, udfsSapSyncStoreDate, (Map) result.get("RFIDEquipmentDetails_MT"));
                    SapMongoDAO.getInstance().updateSapOKorError(sapId, result, "success");
                }
            } else {
                SapMongoDAO.getInstance().updateSapOKorError(sapId, result, "error");
                throw new RuntimeException("No Message from SAP");
            }
        } catch (Exception ex) {
            setUdf(parent, udfsSapSyncStoreDate, SAP_SYNC_STATUS, "false");
            setUdf(parent, udfsSapSyncStoreDate, SAP_SYNC.ERROR_MESSAGE.toString(), ex.getMessage());
            setUdf(parent, udfsSapSyncStoreDate, SAP_SYNC.OUTPUT_MESSAGE.toString(), "");
            setUdf(parent, udfsHandHeldEventDate, SAP_SYNC_STATUS, "false");
            setUdf(parent, udfsHandHeldEventDate, SAP_SYNC.ERROR_MESSAGE.toString(), ex.getMessage());
            setUdf(parent, udfsHandHeldEventDate, SAP_SYNC.OUTPUT_MESSAGE.toString(), "");
            Map <String, Object> result = new HashMap<>();
            result.put("Error message", ex.getMessage());
            SapMongoDAO.getInstance().updateSapOKorError(sapId, result, "error");
            throw ex;
        } finally {
            if (parent != null) {
                thingService.update(new Stack<Long>(), parent, null, udfsSapSyncStoreDate, null, null, false, true, sapSyncStoreDate, true, currentUser, true);
                thingService.update(new Stack<Long>(), parent, null, udfsHandHeldEventDate, null, null, false, true, handHeldEventDate, true, currentUser, true);
                BrokerClientHelper.sendRefreshSingleThingMessage(parent.getThingType().getThingTypeCode(), parent.getSerial(),
                        true, GroupService.getInstance().getMqttGroups(parent.getGroup()));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        File logFile = File.createTempFile("restFile", ".json");
        String tCode="transaction";

        //String url = "http://kbg1pis0.kongsberg.fmcweb.com:50000/AdvantcoRESTAdapter/RESTServlet?channel=RFID_Equipment_REST_Sync_S_CC&service=RFID_Pilot";
        String url = "https://www.pcwebshop.co.uk/";
        String username = "RFIDAPPLUSER";
        String password = "rfid4FMC";
        try {
            Map message = new HashMap<>();
            Map messageDetail = new HashMap<>();
            message.put("RFIDEquipmentTag_MT", messageDetail);
            messageDetail.put("TagID", "T001");
            messageDetail.put("EquipmentNum", "E001");
            messageDetail.put("DateTime", getDate(new Date().getTime()));
            messageDetail.put("Plant", "");
            messageDetail.put("User", "adminp");
            messageDetail.put("Action", ASSOCIATE_CODE);
            sendMessageToSAP(message, url, username, password, logFile, tCode, 1L);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            Map message = new HashMap<>();
            Map messageDetail = new HashMap<>();
            message.put("RFIDEquipmentTag_MT", messageDetail);
            messageDetail.put("TagID", "T001");
            messageDetail.put("EquipmentNum", "E001");
            messageDetail.put("DateTime", getDate(new Date().getTime()));
            messageDetail.put("Plant", "");
            messageDetail.put("User", "adminp");
            messageDetail.put("Action", DISASSOCIATE_CODE);
            sendMessageToSAP(message, url, username, password, logFile, tCode, 2L);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            Map message = new HashMap<>();
            Map messageDetail = new HashMap<>();
            message.put("RFIDEquipmentStatus_MT", messageDetail);
            messageDetail.put("TagID", "T001");
            messageDetail.put("EquipmentNum", "E001");
            messageDetail.put("DateTime", getDate(new Date().getTime()));
            messageDetail.put("Plant", "7395");
            messageDetail.put("User", "adminp");
            messageDetail.put("ScanZone", "7395_LocalRDRep");
            sendMessageToSAP(message, url, username, password, logFile, tCode, 3L);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
