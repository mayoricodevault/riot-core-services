package com.tierconnect.riot.iot.services;

import com.tierconnect.riot.iot.entities.*;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Created by pablo on 6/19/15.
 * <p/>
 * Translate properties from the client to mongo. For example group.id to groupId.
 * If non UDF properties add a .value to get the value from the DB. If the property is time series it adds value
 * at like value.name
 */
public class PropertyTranslator {

    private static Logger logger = Logger.getLogger(PropertyTranslator.class);
    //non UDF string names
    private static final List<String> NON_UDF_FIELDS = Collections.unmodifiableList(Arrays.asList("name", "serial", "group", "thingType", "parent"));
    private static final String DWELL_TIME = "dwellTime(";

    //dictionaries
    private Map<String, String> inToMongo;

    private boolean timeSeries = false;

    //thing Type selected to translate
    protected ThingType thingType;

    //thing type from the
    protected ThingType propertyThingType;

    private static final String FACILITYMAP_TIME = "zone.value.facilityMapTime";
    private static final String ZONEGROUP_TIME   = "zone.value.zoneGroupTime";
    private static final String ZONETYPE_TIME    = "zone.value.zoneTypeTime";
    private static final String FACILITYMAP_DWELL = "zone.value.facilityMapDwellTime";
    private static final String ZONEGROUP_DWELL   = "zone.value.zoneGroupDwellTime";
    private static final String ZONETYPE_DWELL    = "zone.value.zoneTypeDwellTime";

    public PropertyTranslator( ThingType thingType, boolean timeSeries )
    {
        this.timeSeries = timeSeries;
        this.thingType = thingType;

        inToMongo = new HashMap<>();
        inToMongo.put("serial", "serialNumber");
        inToMongo.put("id", "_id");
        inToMongo.put("Name", "name");


        // modified by terry (original)
        inToMongo.put("Zone", "Zone.value.name");
        inToMongo.put("Shift", "Shift.value.name");
        inToMongo.put("zone", "zone.value.id");
        inToMongo.put("zone.name", "zone.value.id");
        inToMongo.put("zone.name", "zone.value.id");
        inToMongo.put("zone.time", "zone.time");
        inToMongo.put("zoneLocalMap.id", "zone.value.facilityMap");
        //localMap.id for older configurations
        inToMongo.put("localMap.id", "zone.value.facilityMap");
        inToMongo.put("zoneGroup.id", "zone.value.zoneGroup");
        inToMongo.put("zoneCode.name", "zone.value.id");
        inToMongo.put("zoneProperty.id", "zone.value.id"); //special case for zoneProperty.id
        inToMongo.put("zoneType.id", "zone.value.id");
        inToMongo.put("zoneType.name", "zone.value.id");
        inToMongo.put("group.groupType.id", "groupTypeId");
//        inToMongo.put("logicalReader", "logicalReader.value.name");
        inToMongo.put("facilityCode", "facilityCode.value.name");

        //TODO: support other UDFs than zone ! SAME as DWELL
        inToMongo.put( "timeStamp(zone)", "zone.time" );


        // for FMC hardcoded, bad idea
        inToMongo.put("ScanZone", "ScanZone.value.id");
        inToMongo.put("ScanZone.name", "ScanZone.value.id");
        inToMongo.put("ScanZone.time", "ScanZone.time");
        inToMongo.put("ScanZoneCode.name", "ScanZone.value.id");
        inToMongo.put("ScanZoneLocalMap.id", "ScanZone.value.facilityMap");
        inToMongo.put("ScanZoneGroup.id", "ScanZone.value.zoneGroup");
        inToMongo.put("ScanZoneProperty.id", "ScanZone.value.id"); //special case for zoneProperty.id
        inToMongo.put("ScanZoneType.id", "ScanZone.value.id");
        inToMongo.put("ScanZoneType.name", "ScanZone.value.id");

        //TODO: support other UDFs than zone ! SAME as DWELL
        inToMongo.put( "timeStamp(ScanZone)", "ScanZone.time" );


        //shift
        inToMongo.put("shift", "shift.value.id");
    }

    /**
     * Contructor to add extra or change values
     *
     * @param thingType
     * @param toMongoDictionary
     */
    public PropertyTranslator(ThingType thingType, Map<String, String> toMongoDictionary) {
        this(thingType, true);
        inToMongo.putAll(toMongoDictionary);
    }

    private String translate(String in, boolean addValue) {
        in = in.replace(" ", "");
        String newIn = StringUtils.substringBefore(in, ",");
        String out   = null;
        if ((newIn != null) && (newIn.contains("timeStamp("))) {
            out = in.substring(in.indexOf("(")+1, in.length()-1)+".time";
        } else {
            out = inToMongo.get(newIn);
        }
        //nothing in dictionary
        if (StringUtils.isEmpty(out)) {
            //try to translate
            out = doTranslation(newIn, addValue);
        }

        if (thingType != null && propertyThingType != null) {
            //case rfid is child of asset
            if (propertyThingType.isChild(thingType.getId())) {
                out = "parent." + out;
            }

            //check if we have think type udfs
            ThingTypeField thingTypeField = thingType.getMatchingThingTypeUdf(propertyThingType.getId());
            //logger.info("THING TYPE FIELD " + thingTypeField);
            if (thingTypeField != null) {
                out = thingTypeField.getName() + ".value." + out;
            } else {
                //thingTypes can have multiple parents, so this is tricky since multiple parents can have
                //matching thing type udfs. Need a way to select one parent????
                //todo POSSIBLE BUG!!!!!
                for (ThingType parentType : thingType.getParents()) {
                    ThingTypeField parentThingTypeField =
                            parentType.getMatchingThingTypeUdf(propertyThingType.getId());

                    if (parentThingTypeField != null) {
                        out = "parent." + parentThingTypeField.getName() + ".value." + out;
                        break;
                    }
                }
            }

            //reverse UDF thing type. translate to list of children
            ThingTypeField thingTypeFieldRevers = propertyThingType.getMatchingThingTypeUdf(thingType.getId());
            if (thingTypeFieldRevers != null) {
                out = propertyThingType.getCode() + "_children." + out;
            } else {
                //thingTypes can have multiple parents, so this is tricky since multiple parents can have
                //matching thing type udfs. Need a way to select one parent????
                //todo POSSIBLE BUG!!!!!
                for (ThingType parentType : propertyThingType.getParents()) {
                    ThingTypeField parentThingTypeField =
                            parentType.getMatchingThingTypeUdf(thingType.getId());

                    if (parentThingTypeField != null) {
                        out = parentType.getCode() + "_children." + out;
                        break;
                    }
                }
            }

        }

        return out;
    }

    public String translateFromString(String in) {
        return translate(in, true);
    }

    public boolean isDwell(String property) {
        return StringUtils.remove(property, " ").startsWith(DWELL_TIME);
    }


    private String doTranslation(String in, boolean addValue) {
        String[] tokens = StringUtils.split(in, ".");
        //init same value
        String out = in;

        //has a dot notation like thingType.id. Translate to thingTypeId
        if (tokens.length > 1) {
            if (NON_UDF_FIELDS.contains(tokens[0])) {
                //do camel case
                for (int i = 1; i < tokens.length; i++) {
                    tokens[i] = StringUtils.capitalize(tokens[i]);
                }
                out = StringUtils.join(tokens);
            }

        }
        //UDF value and it does not have a dot.
        else if (!NON_UDF_FIELDS.contains(in) && addValue) {
            //a dwell time property. get time
            //todo not necesary
            if (isDwell(in)) {
                in = stripDwell(in);
                out = getDwellTimeProperty(in);
            }                
            else {  //add ".value"
                out = in.concat(".value");
            }
        }
        return out;
    }

    /**
     * Check if the property is dwell or not , if yes, then return the mongo path
     * @param in
     * @return
     */
    public String getDwellTimeProperty(String in) {
        String out = null;
        if (!timeSeries) {
            if (in.equals("facilityMap")) {
                out = FACILITYMAP_TIME;
            } else if (in.equals("zoneGroup")) {
                out = ZONEGROUP_TIME;
            } else if (in.equals("zoneType")) {
                out = ZONETYPE_TIME;
            } else {
                out = in.concat(".time");
            }
        } else {
            if (in.equals("facilityMap")) {
                out = FACILITYMAP_DWELL;
            } else if (in.equals("zoneGroup")) {
                out = ZONEGROUP_DWELL;
            } else if (in.equals("zoneType")) {
                out = ZONETYPE_DWELL;
            } else {
                out = in.concat(".dwellTime");
            }
        }
        return out;
    }

    private String stripDwell(String in) {
        String out = StringUtils.removeStart(StringUtils.remove(in, " "), DWELL_TIME);
        out = StringUtils.removeEnd(out, ")");
        out = StringUtils.trim(out);

        return out;
    }
}
