package com.tierconnect.riot.iot.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.job.JobUtils;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.MongoExecutionException;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.quartz.*;

import java.io.IOException;
import java.util.*;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;


/**
 * Created by cfernandez
 * 2/3/2015.
 */
public class ShiftZoneRevalidationJob implements Job
{
    static Logger logger = Logger.getLogger(ShiftZoneRevalidationJob.class);

    Map<String, Map> zoneNames;

    public void init(){
        logger.debug("Initializing Shift Zone re-validation Job....");
        schedule();
    }

    public static void schedule()
    {
        if (Boolean.TRUE.equals(validateSchedule()))
        {
            logger.info("scheduling ShiftZoneRevalidationJob...");
            JobDetail job = newJob(ShiftZoneRevalidationJob.class)
                    .withIdentity("ShiftZoneRevalidationJob", "ShiftZoneRevalidationJob")
                    .build();

            String cronExpression = getSchedule();
            Trigger trigger = newTrigger()
                    .withIdentity("triggerJob_ShiftZoneRevalidationJob", "ShiftZoneRevalidationJob")
                    .withSchedule(cronSchedule(cronExpression))
                    .build();

            try {
                JobUtils.getScheduler().scheduleJob(job, trigger);
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        }
    }

    public static Boolean validateSchedule(){
        Boolean result = Boolean.FALSE;
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        try
        {
            transaction.begin();

            result = ConfigurationService.getAsBoolean(GroupService.getInstance().getRootGroup(), "shiftZoneValidationEnabled");
            logger.debug("[Shift zone re-validation job] Job enabled: " + result);

            transaction.commit();
        }
        catch (Exception e){
            logger.error("[Shift zone re-validation job] Cannot read enabled/disabled configuration");
            HibernateDAOUtils.rollback(transaction);
        }
        return result;
    }

    public static String getSchedule(){
        String cron = "0 0/5 * * * ?";

        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        try
        {
            transaction.begin();

            String schedule = ConfigurationService.getAsString(GroupService.getInstance().getRootGroup(), "shiftZoneValidation");
            cron = parseSchedule(schedule);
            logger.debug("[Shift re-validation job] Cron expression defined: " + cron);

            transaction.commit();
        }
        catch (Exception e){
            logger.error("[Shift re-validation job] Cannot read schedule configuration");
            HibernateDAOUtils.rollback(transaction);
        }

        return cron;

    }

    private  static String parseSchedule(String schedule) {
        String cronExpression = "0 0 0/1 * * ?";
        int minutes = Integer.parseInt(schedule);
        int module = minutes % 60;
        if (module == 0)
        {
            int hour = minutes / 60;
            if (hour < 24){
                cronExpression = "0 0 0/" + hour + " * * ?";
            }else {
                logger.info("[Shift re-validation job] Invalid Job frequency defined, using default time: 60 min");
            }
        }
        else{
            if (minutes < 60 && minutes > 4){
                cronExpression = "0 0/" + minutes + " * * * ?";
            }else{
                logger.info("[Shift re-validation job] Invalid Job frequency defined, using default time: 60 min");
            }
        }
        return cronExpression;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        logger.info("[Shift re-validation job] " + "[" + java.util.Calendar.getInstance().getTime() + "] Start Running");

        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        try
        {
            transaction.begin();

            //Get Values
            EdgeboxService edgeboxService = EdgeboxService.getInstance();
            List<Edgebox> lstEdgeBoxCore = edgeboxService.getByType(Constants.EDGEBOX_CORE_TYPE);
            if( (lstEdgeBoxCore!=null) && (!lstEdgeBoxCore.isEmpty())) {
                for(Edgebox edgebox : lstEdgeBoxCore) {
                    String edgeboxConfig = edgebox.getConfiguration();

                    ObjectMapper mapper = new ObjectMapper();
                    Map<?, ?> map = mapper.readValue(edgeboxConfig, Map.class);
                    Map<?, ?> shiftZoneRule = (Map<?, ?>) map.get("shiftZoneRule");

                    // getting zone violation properties from configuration
                    String shiftProperty = (String) shiftZoneRule.get("shiftProperty");
                    String zoneViolationFlag = (String) shiftZoneRule.get("zoneViolationFlagProperty");
                    String zoneViolationStatus = (String) shiftZoneRule.get("zoneViolationStatusProperty");

                    if (shiftProperty == null){
                        logger.info("[Shift re-validation job]: shiftProperty was not defined in edgebox configuration");
                    }

                    if (zoneViolationFlag == null){
                        logger.info("[Shift re-validation job]: zoneViolationFlagProperty was not defined in edgebox configuration");
                    }

                    if (zoneViolationStatus == null){
                        logger.info("[Shift re-validation job]: zoneViolationStatusProperty  was not defined in edgebox configuration");
                    }

                    if((shiftProperty!=null) && (zoneViolationFlag!=null) && (zoneViolationStatus!=null)) {
                        processShiftZoneRevalidation(shiftProperty, zoneViolationFlag, zoneViolationStatus);
                    }
                    //TODO: Next implementation is going to have a Thing Type filter by Core Bridge, now just one core will execute the process
                    break;
                }
            }

            transaction.commit();
        }
        catch (Exception e){
            logger.error("[Shift re-validation job] error=" + e.getMessage());
            HibernateDAOUtils.rollback(transaction);
        }

        logger.info("[Shift re-validation job]" + "[" + java.util.Calendar.getInstance().getTime() + "] End Running");
    }

    /**
     * Method to process the logic of Shift Zone Re validation
     * @param shiftProperty Thing Type Field Name shift
     * @param zoneViolationFlag Zone Violation Flag
     * @param zoneViolationStatus Zone Violation Status
     * @throws MongoExecutionException
     * @throws IOException
     */
    private void processShiftZoneRevalidation(String shiftProperty, String zoneViolationFlag, String zoneViolationStatus)
            throws MongoExecutionException, IOException {
        ThingService thingService = ThingService.getInstance();
        ShiftService shiftService = ShiftService.getInstance();
        List<Thing> things = thingService.selectAllThings();
        logger.info("[Shift re-validation job] Thing to process: " + things.size());

        // Get all zones and shift relations
        ShiftZoneService shiftZoneService = ShiftZoneService.getInstance();
        zoneNames = shiftZoneService.selectAllShiftZone();

        for (Thing thing : things)
        {
            logger.debug("[Shift re-validation job] processing : thing=" + thing.getName() + ", serial=" + thing.getSerial());

            if (availableRequiredFields(thing, shiftProperty, zoneViolationFlag, zoneViolationStatus))
            {
                logger.debug("[Shift re-validation job] all required files available");

                ThingTypeField fieldShift = thing.getThingTypeField(shiftProperty);
                ThingTypeField zoneField = thing.getThingTypeField("zone");

                String shiftIds = getThingFieldValue(thing, fieldShift);
                String zoneName = getThingFieldValue(thing, zoneField);

                // If is not a valid zone continue with the next thing
                if ( zoneName != null && !"".equals(zoneName.trim()) )
                {
                    boolean isAllowed;
                    boolean propertyValue = false; //if the thing is allowed the priority value is false
                    java.util.Calendar calendar = new GregorianCalendar();

                    // If the zone is unknown set false the flag and status
                    if ( "Unknown".equals( getZoneName(zoneName) ) )
                    {
                        logger.info("[Shift re-validation job] 'Unknown' zone, setting flag and status to 'false'");
                        setZoneViolationValue(false, zoneViolationFlag, thing);
                        setZoneViolationValue(false, zoneViolationStatus, thing);
                    } else if ( shiftIds != null )
                    {
                        List<Shift> shifts = getShifts(shiftIds, zoneName, thing.getGroup());
                        logger.info("[Shift re-validation job] CurrentZone=" + getZoneName( zoneName ) + ", shifts available=" + Arrays.toString(shifts.toArray()));
                        if ( !shifts.isEmpty() ) {
                            isAllowed = shiftService.IsAllowed(shifts, calendar);
                            if (!isAllowed) {
                                logger.info("[Shift re-validation job] Illegal thing=" + thing.getSerial() + " for zone=" + getZoneName(zoneName) + ", setting " + zoneViolationFlag + "=true");
                                propertyValue = true;

                                // updating zoneViolationFlag field (this field is updated just when the property value is true)
                                setZoneViolationValue(true, zoneViolationFlag, thing);
                            }
                            // updating zoneViolationStatus Field (this field is updated all the time)
                            setZoneViolationValue(propertyValue, zoneViolationStatus, thing);
                        } else
                        {
                            logger.info("[Shift re-validation job] CurrentZone=" + getZoneName( zoneName ) + " no shift assigned to this zone, setting flags and status to 'true'");
                            setZoneViolationValue(true, zoneViolationFlag, thing);
                            setZoneViolationValue(true, zoneViolationStatus, thing);
                        }
                    }else // If there not exists an assigned shift set true the flag and status
                    {
                        logger.info("[Shift re-validation job] CurrentZone=" + getZoneName( zoneName ) + " Shift=null, setting flags and status to 'true'");
                        setZoneViolationValue(true, zoneViolationFlag, thing);
                        setZoneViolationValue(true, zoneViolationStatus, thing);
                    }

                    logger.info("[Shift re-validation job] Validating thing with serial=" + thing.getSerial() + " and shiftsIds assigned=" + shiftIds);
                }
            }
        }
    }

    private void setZoneViolationValue( boolean propertyValue, String zoneViolation, Thing thing ) throws MongoExecutionException {
        Map<String, Object> mapFlag = new HashMap<>();
        Map<String, Object> tempTf = new HashMap<>(  );
        ThingTypeField thingTypeField = ThingTypeFieldService.getInstance().getThingTypeFieldByName(zoneViolation).get(0);
        tempTf.put( "thingTypeFieldId", thingTypeField.getId());
        tempTf.put("time", new Date());
        tempTf.put( "value", propertyValue);
        mapFlag.put(zoneViolation, tempTf);
        Date timeAndModifiedTime = new Date();
        ThingMongoService.getInstance().updateThing(thing,null, mapFlag, new Date(), timeAndModifiedTime);
    }

    public String getThingFieldValue(Thing thing, ThingTypeField thingTypeField)
    {
        String value = null;
        String key = thingTypeField.getName() + ".value";
        String where = "_id=" + thing.getId();

        List<String> fields = new ArrayList<>();
        fields.add(key);

        Map<String,Object> fieldValues = ThingMongoDAO.getInstance().getThingUdfValues(where, null, fields, null);
        List<Map<String, Object>> udfValuesList = (List<Map<String, Object>>) fieldValues.get("results");
        if (udfValuesList != null && udfValuesList.size() > 0) {
            value = udfValuesList.get(0).get(key)!=null?String.valueOf(udfValuesList.get(0).get(key)):null;
        }
        return value;
    }

    private String getShiftIds( String shiftsString ) throws IOException {
        String shiftIds1 = "";
        StringBuilder shiftIdsString = new StringBuilder();
        HashMap<String,Object> shiftIdsMap = new ObjectMapper().readValue(shiftsString, HashMap.class);
        for(Map.Entry<String, Object> entry : shiftIdsMap.entrySet()) {
            String key = entry.getKey();
            if ( "id".equals( key )) {
                String value = entry.getValue().toString();
                shiftIdsString.append(value);
                shiftIdsString.append(",");
            }
        }
        shiftIds1 = shiftIdsString.length() > 0 ? shiftIdsString.substring(0, shiftIdsString.length() - 1): "";
        return shiftIds1;
    }

    private String getZoneName( String zoneString ) throws IOException {
        // Parsing to Map zoneName String
        String zoneName = "";
        HashMap<String,Object> zoneNameMap = new ObjectMapper().readValue(zoneString, HashMap.class);
        for(Map.Entry<String, Object> entry : zoneNameMap.entrySet()) {
            String key = entry.getKey();
            if ( "name".equals( key )) {
                zoneName = entry.getValue().toString();
            }
        }
        return zoneName;
    }

    /**
     * Get shift objects from shift Ids
     * @param shiftString String param with shift ids e.g. "1,2,3,4,5"
     * @return List of shifts
     */
    private List<Shift> getShifts(String shiftString, String zoneString, Group group) throws IOException {
        List<Shift> shifts = new ArrayList<>();
        String shiftIds = getShiftIds(shiftString);
        if (shiftIds==null || "".equals(shiftIds)){
            return shifts;
        }

        String[] splitIds = shiftIds.split(",");

        String zoneName = getZoneName( zoneString );

        List<String> shiftIdsList = Arrays.asList(splitIds);

        ShiftService shiftService = ShiftService.getInstance();
        for (String shiftId : shiftIdsList)
        {
            Long id = Long.parseLong( shiftId );
            Shift shift = shiftService.get( id );
            if (shift.getActive()) {
                if (zoneNames.get(shift.getCode()) != null) {
                    Map<String, String> mapZoneName = (Map<String, String>) zoneNames.get(shift.getCode());
                    if (mapZoneName.get(zoneName) != null) {
                        shifts.add(shift);
                    }
                }
            }
        }
        return shifts;
    }

    /**
     * Check if fields shift, zone, zoneViolationFlag and zoneViolationStatus are not null
     * @param thing thing
     * @return boolean
     */
    public static boolean availableRequiredFields(Thing thing,String shiftProperty,
                                                  String zoneViolationFlag, String zoneViolationStatus){
        if (shiftProperty == null || "".equals(shiftProperty.trim())){
            return false;
        }

        if (zoneViolationFlag == null || "".equals(zoneViolationFlag.trim())){
            return false;
        }

        if (zoneViolationStatus == null || "".equals(zoneViolationStatus.trim())){
            return false;
        }

        ThingTypeField fieldZoneViolationFlag = thing.getThingTypeField(zoneViolationFlag);
        ThingTypeField fieldZoneViolationStatus = thing.getThingTypeField(zoneViolationStatus);
        ThingTypeField fieldShift = thing.getThingTypeField(shiftProperty);
        ThingTypeField zoneField = thing.getThingTypeField("zone");

        if (fieldZoneViolationFlag != null && fieldZoneViolationStatus != null && fieldShift != null && zoneField != null){
            return true;
        }
        return false;
    }

    /**
     * this method is called by reflexion from GroupConfiguration
     */
    public static void reschedule() {
        logger.info("rescheduling ShiftZoneRevalidationJob...");
        long startTime = System.currentTimeMillis() + 5000L;
        String keyJob   = "reShiftZoneRevalidationJob";
        String groupJob = "ShiftZoneRevalidationJobReschedule";
        JobUtils.killJob(keyJob, groupJob);
        JobDetail job = newJob(RescheduleShiftZoneRevalidationJob.class)
                .withIdentity(keyJob, groupJob)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("triggerJob_ShiftZoneRevalidationJob", "ShiftZoneRevalidationJobReschedule")
                .startAt(new Date(startTime))
                .build();

        try {
            JobUtils.getScheduler().scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    /**
     * this method is called by reflexion from GroupConfiguration
     */
    public static void stop() {
        logger.info("stopping ShiftZoneRevalidationJob...");
        try {
            JobUtils.getScheduler().deleteJob(new JobKey("ShiftZoneRevalidationJob", "ShiftZoneRevalidationJob"));
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    /**
     * this method is called by reflexion from GroupConfiguration
     */
    public static boolean checkExistJob ()
    {
        boolean result = false;
        try {
            result = JobUtils.getScheduler().checkExists(new JobKey("ShiftZoneRevalidationJob", "ShiftZoneRevalidationJob"));
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static class RescheduleShiftZoneRevalidationJob implements Job
    {

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            String key = context.getJobDetail().getKey().getName();
            try
            {
                try {
                    JobUtils.getScheduler().deleteJob(new JobKey("ShiftZoneRevalidationJob", "ShiftZoneRevalidationJob"));
                } catch (SchedulerException e) {
                    e.printStackTrace();
                }
                schedule();
            }
            catch (Exception e){
                logger.error("[Shift re-validation job] " + key + " failed", e);
            }
        }
    }

}
