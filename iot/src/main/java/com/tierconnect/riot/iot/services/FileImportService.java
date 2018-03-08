package com.tierconnect.riot.iot.services;

import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.AuthenticationUtils;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.utils.DateHelper;
import com.tierconnect.riot.commons.utils.DateTimeFormatterHelper;
import com.tierconnect.riot.iot.controllers.ThingTypeController;
import com.tierconnect.riot.iot.dao.util.FormulaUtil;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.fmc.utils.FMCConstants;
import com.tierconnect.riot.iot.utils.BackgroundProgressStatus;
import com.tierconnect.riot.iot.utils.Cache;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.servlet.exception.ForbiddenException;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.hibernate.FlushMode;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

/**
 * Created by pablo on 12/15/14.
 *
 * Service that handles the import of things, thing types from files
 */
public class FileImportService {
    public static enum Type {THING, THING_TYPE, THING_TYPE_PROPERTY, USER}


    static Logger logger = Logger.getLogger(FileImportService.class);
    private User user;
    private Cache cache;
    private ThingType thingType;
    private Group group;
    private boolean thingRunRules;

    public FileImportService(User user, ThingType thingType)
    {
        this(user);
        this.group = user.getGroup();
        this.thingType = thingType;
    }

    public FileImportService(User user, ThingType thingType, boolean thingRunRules)
    {
        this(user);
        this.group = user.getGroup();
        this.thingType = thingType;
        this.thingRunRules = thingRunRules;
    }

    public FileImportService(User user)
    {
        this.user = user;
        this.group = user.getGroup();
        this.cache = new Cache();
    }


    public String[] parse(String filePath, Type type, Long id) throws IOException
    {
        Reader in = new InputStreamReader(new FileInputStream(filePath), Charsets.UTF_8);

        return parse(in, type, id, filePath);
    }

    /**
     * Process a file. Read a file line by line. Parse a line and hand it to a line handler for processing
     * @param in file stream
     * @param type type file to parse. Determines what handler is going to be used
     * @return a string array indicating if each line was parsed successfully.
     * @throws IOException
     */
    public String[] parse(Reader in, Type type, Long id, String filePath) throws IOException
    {
        List<String> messages = new ArrayList<>();
        //get handler
        ImportHandler handler = getHandler(type);

        CSVParser records = CSVFormat.EXCEL.withHeader().withSkipHeaderRecord().parse(in);

        //get column length
        int columnLength = records.getHeaderMap().size();
        logger.info("column length " + columnLength);

        ImportExport importFile = ImportExportService.getInstance().get(id);

        if (importFile == null) {
            throw new UserException(String.format("ImportId[%d] not found.", id));
        }

        FlushMode flushMode = HibernateSessionFactory.getInstance().getCurrentSession().getFlushMode();
        HibernateSessionFactory.getInstance().getCurrentSession().setFlushMode(FlushMode.COMMIT);
        long errorCount = 0;


        User user = (User) SecurityUtils.getSubject().getPrincipal();

        importFile.setUserId(this.user.getId());
        importFile.setType(type.name());
        importFile.setProcessType("Import");
        Long dateTmp = (new Date()).getTime() / 1000;
        Long startTime = System.currentTimeMillis();
        importFile.setStartDate(new Date(dateTmp * 1000));
        String temporalName = importFile.getId() + "import";
        BackgroundProcess backgroundProcess = new BackgroundProcess();
        backgroundProcess.setIniDate(importFile.getStartDate());
        backgroundProcess.setCreatedByUser(this.user);
        backgroundProcess.setStatus(Constants.ADDED);
        backgroundProcess.setTypeProcess("import");
        backgroundProcess.setProgress(0);
        backgroundProcess.setFileName(filePath);
        String nameThread = FileExportService.class.getName() + "-RD-" + importFile.getId();
        backgroundProcess.setThreadName(nameThread);
        BackgroundProgressStatus.getInstance().setMapStatus(String.valueOf(importFile.getId()), "ADDED", 0, backgroundProcess, "import", user.getId(), 0, 0, backgroundProcess.getIniDate());
        BackgroundProcessService.getInstance().insertBackgroundProcess(backgroundProcess, importFile.getId(), "import", "importId");
        Double updateBackground = ConfigurationService.getAsInteger(this.user, "background_percentUpdate").doubleValue();
        List<CSVRecord> recordsList = records.getRecords();
        double totalList = recordsList.size();
        double delta = totalList * (updateBackground / 100.0);
        double div = 1.0;
        int progressRatio = (int) Math.rint(delta);
        if (progressRatio == 0) {
            div = progressRatio;
            progressRatio = 1;
        }
        int progressValue = progressRatio;

        int lineCount = 1;
        long successCount = 0;
        try {
            for (CSVRecord record : recordsList) {
                //validate size of columns
                if (record.size() == columnLength) {
                    String message = handler.parse(record);
                    messages.add("Line " + lineCount + ": " + message);
                    if (progressValue == lineCount) {
                        backgroundProcess.setStatus(Constants.IN_PROGRESS);
                        backgroundProcess.setProgress((int) (((progressValue / progressRatio) * updateBackground) / div));
                        String thingType = "";
                        switch (type) {
                            case THING_TYPE:
                                thingType = record.get("Code");
                                break;
                            case THING:
                            case THING_TYPE_PROPERTY:
                                thingType = record.get("Thing Type Code");
                                break;
                        }
                        if (backgroundProcess.getThingTypes() == null || !backgroundProcess.getThingTypes().contains(thingType)) {
                            if (backgroundProcess.getThingTypes() == null) {
                                backgroundProcess.setThingTypes(thingType);
                            } else {
                                backgroundProcess.setThingTypes(backgroundProcess.getThingTypes() + ", "
                                        + thingType);
                            }
                        }
                        progressValue = progressValue + progressRatio;
                        backgroundProcess = BackgroundProcessService.getInstance().update(backgroundProcess);
                        BackgroundProgressStatus.getInstance().setMapStatus(String.valueOf(importFile.getId()), backgroundProcess.getStatus(), backgroundProcess.getProgress(), backgroundProcess, "import", user.getId(), (int) totalList, lineCount, backgroundProcess.getIniDate());
                    }
                    lineCount++;

                    if (message.toLowerCase().contains("created") || message.toLowerCase().contains("updated") || message.toLowerCase().contains("updating") || message.toLowerCase().contains("inserting") || message.toLowerCase().contains("success")) {
                        successCount = successCount + 1;
                    } else {
                        errorCount = errorCount + 1;
                    }
                } else {
                    errorCount = errorCount + 1;
                    messages.add("Invalid column length. Expected " + columnLength +
                            " columns and got " + record.size() + ".");
                }
            }
        }catch (Exception e){
            errorCount = errorCount + 1;
            messages.add("Line "+lineCount+": It is not possible to import a record. This will be skipped.");
            lineCount++;
        }
        handler.stop(getGroupMqtt());
        importFile.setErrorRecord(errorCount);
        importFile.setSuccessRecord(successCount);
        importFile.setTotalRecord(Long.valueOf(lineCount - 1));
        Long dateTmpEnd = (new Date()).getTime() / 1000;
        importFile.setEndDate(new Date(dateTmpEnd * 1000));
        ImportExportService.getInstance().update(importFile);
        backgroundProcess.setProgress(100);
        Long endTime = System.currentTimeMillis();
        backgroundProcess.setEndDate(importFile.getEndDate());
        backgroundProcess.setStatus(Constants.COMPLETED);
        BackgroundProgressStatus.getInstance().setMapStatus(String.valueOf(importFile.getId()), backgroundProcess.getStatus(), backgroundProcess.getProgress(), backgroundProcess, "import", user.getId(), importFile.getTotalRecord().intValue(), importFile.getTotalRecord().intValue(), backgroundProcess.getIniDate());
        backgroundProcess.setTotalRecords(importFile.getTotalRecord());
        backgroundProcess.setTotalAffectedRecords(importFile.getSuccessRecord());
        backgroundProcess.setTotalOmittedRecords(importFile.getErrorRecord());
        if (backgroundProcess.getEndDate().getTime() - backgroundProcess.getIniDate().getTime() == 0) {
            backgroundProcess.setProcessTime(endTime - startTime);
        } else {
            backgroundProcess.setProcessTime(backgroundProcess.getEndDate().getTime() - backgroundProcess.getIniDate().getTime());
        }
        BackgroundProcessService.getInstance().update(backgroundProcess);
        BackgroundProgressStatus.getInstance().mapStatus.remove(temporalName);


        HibernateSessionFactory.getInstance().getCurrentSession().setFlushMode(flushMode);
        return messages.toArray(new String[messages.size()]);
    }

    /**
     * Get list of Groups Mqtt
     * @return List of Groups
     */
    public List<Long> getGroupMqtt() {
        List<Long> groupMqtt = null;
        if(this.group != null) {
            groupMqtt = GroupService.getInstance().getMqttGroups(this.group);
        } else {
            groupMqtt = GroupService.getInstance().getMqttGroups(GroupService.getInstance().getRootGroup());
        }
        return groupMqtt;
    }


    /**
     * Helper method that instantiates the appropriate handler for a file
     * @param type type of format to handle
     * @return the appropriate handler for the file format
     */
    private ImportHandler getHandler(Type type)
    {
        ImportHandler ih = null;
        switch (type) {
            case THING:
                ih = new ThingHandler(user, thingType, ThingService.getInstance(), cache, thingRunRules);
                break;
            case THING_TYPE:
                ih = new ThingTypeHandler(ThingTypeService.getInstance(), cache, ThingTypeMapService.getInstance());
                break;
            case THING_TYPE_PROPERTY:
                ih = new ThingTypePropertyHandler(ThingTypeService.getInstance(), cache);
                break;
            case USER:
                if (!PermissionsUtils.buildSearch(SecurityUtils.getSubject(), new HashMap<String, Boolean>(), "user:i|user:u")) {
                    throw new UserException("Permissions error: User does not have access to create or update users.");
                }
                ih = new UserHandler();
                break;
        }

        logger.info("Handler is " + ih);

        return ih;
    }

    /**
     * Interface that handles the processing of a line from a file being parsed
     */
    private interface ImportHandler {
		//parse one line of a file
        public String parse(CSVRecord row);

		//called then the handler stops;
		public void stop(List<Long> groupMqtt);
    }

    /**
     * handler to process a line of a thing import file
     */
    private static class ThingHandler implements ImportHandler
    {
        private User user;
        private ThingService ts;
        private Cache cache;
        private ThingType thingType;
        private List<TickleItem> thingsToTickle;
        private boolean runRules;

        public ThingHandler(User user, ThingType thingType, ThingService ts, Cache cache)
        {
            this.user = user;
            this.ts = ts;
            this.thingType = thingType;
            this.cache = cache;
            thingsToTickle = new ArrayList<>();
        }

        public ThingHandler(User user, ThingType thingType, ThingService ts, Cache cache, boolean runRules)
        {
            this.user = user;
            this.ts = ts;
            this.thingType = thingType;
            this.cache = cache;
            thingsToTickle = new ArrayList<>();
            this.runRules = runRules;
        }

        /**************************************************************************************
         * @method parseNew
         * @description this method does the insert or update of the thing. If the thing is not valid
         *              it returns the corresponding message.
         * @param row  CSV Record with the thing values and thing field properties
         * @return
        /**************************************************************************************/
        @Override
        public String parse(CSVRecord row)
        {
            String output = "";
            String serial = "";
            try
            {
                String thingTypeCode = thingType.getThingTypeCode();
                if(thingTypeCode != null && thingTypeCode.equals(row.get("Thing Type Code")))
                {
                    cache.getThingType(thingTypeCode);
                    // if (null == serial || serial.isEmpty())
                    //    throw new UserException("Invalid or empty serial number");
                    String parentSerial = row.isSet("Parent") ? row.get("Parent") : "";
                    Thing parent = findParent(parentSerial);
                    Date storeData =  new Date();

                    if(!StringUtils.isEmpty(thingType.getSerialFormula())){
                        Map<String, Object> udfs = new HashMap<>();
                        for(Map.Entry<String, String> item : row.toMap().entrySet()){
                            if(item.getKey().equals("Thing Type Code")){
                                udfs.put("thingTypeCode", item.getValue());
                            }else if(item.getKey().equals("Thing Name")){
                                udfs.put("name", item.getValue());
                            }else if(item.getKey().equals("Thing Serial")){
                                udfs.put("serialNumber", item.getValue());
                            }else if(item.getKey().equals("ownerGroup")){
                                udfs.put("group", item.getValue());
                            }else {
                                udfs.put(item.getKey(), item.getValue());
                            }
                        }

                        serial = FormulaUtil.getFormulaValues(udfs, null, thingType.getSerialFormula()).toString();
                    }else{
                        serial = row.get("Thing Serial").trim();
                    }

                    Thing thing = ThingService.getInstance().getBySerialNumberWithFields( serial, thingType );
                    output = thing == null ? "A new thing created": "Thing updated";
                    output += " with serial " + serial;
                    String hierarchyNameGroup = row.isSet("ownerGroup") ? row.get("ownerGroup").trim() : null;
                    Group group;
                    if (hierarchyNameGroup != null)
                        group = cache.getGroup(hierarchyNameGroup);
                    else
                        group = thingType.getGroup();
                    boolean includeRoot = group.getName().equals("root");

                    Map <String, Boolean> validations = new HashMap<>();
                    validations.put("thingType", false);
                    validations.put("group", true);
                    validations.put("thing.exists", false);
                    validations.put("thing.serial", false);
                    validations.put("thing.parent", false);
                    validations.put("thing.children", false);
                    validations.put("thing.udfs", true);
                    validations.put("thing.uFavoriteAndRecent", false);

                    Subject currentUser = SecurityUtils.getSubject();
                    User user = ((User) currentUser.getPrincipal());
                    boolean enableFMCLogic = ConfigurationService.getAsBoolean(user, "fmcSapEnableSapSyncOnImport");

                    Map<String, Object> result;
                    if (thing == null)
                    {
                        if (!PermissionsUtils.buildSearch(SecurityUtils.getSubject(), new HashMap<String, Boolean>(), "_thingType_"+thingType.getId()+":i")) {
                            return "ERROR: User does not have permissions to create new Things, with thingType: " + thingType.getName();
                        }
                        Stack<Long> recursivelyStack = new Stack<>();
                        //Create a new Thing
                        result = ThingsService.getInstance().create(
                                recursivelyStack,
                                thingType.getThingTypeCode(),
                                group.getHierarchyName(includeRoot),
                                row.isSet( "Thing Name" ) ? row.get( "Thing Name" ) : serial,
                                serial,
                                this.getMapParent(parent),
                                this.getMapOfUdfs( thingType, row , storeData ),
                                null, null,
                                false, true , storeData, !enableFMCLogic, false, true,null, null, true, null);
                        output = "A new thing created";
                    }
                    else
                    {
                        if (!PermissionsUtils.buildSearch(SecurityUtils.getSubject(), new HashMap<String, Boolean>(), "_thingType_"+thingType.getId()+":u")) {
                            return "ERROR: User does not have permissions to update Things, with thingType: " + thingType.getName();
                        }
                        //Update Thing
                        Stack<Long> recursivelyStack = new Stack<>();
                        result = ThingService.getInstance().update(
                                recursivelyStack,
                                thing
                                , thingType.getThingTypeCode()
                                , group.getHierarchyName(includeRoot)
                                , row.isSet( "Thing Name" ) ? row.get( "Thing Name" ) : serial
                                , serial
                                , parent
                                , this.getMapOfUdfs( thingType, row , storeData )
                                , null
                                , null
                                , false, true, storeData, !enableFMCLogic, validations, cache, false, true, user, true);
                        output = "Thing updated";
                    }
                    output += " with serial " + ((Map) result.get("thing")).get("serial");
                    TickleItem tickleItem = getTickleItem(serial, thingType.getThingTypeCode(),
                            storeData, (Map<String, Object>) result.get("fields"),
                            GroupService.getInstance().getMqttGroups(thingType.getGroup()));
                    thingsToTickle.add(tickleItem);
                }
                else
                {
                    output = "Thing Type Code does not match the code selected for import";
                }
            }
            catch (NonUniqueResultException | IllegalArgumentException | UserException e)
            {
                logger.error(e);
                output = "ERROR: " + e.getMessage();
            }

            Subject currentUser = SecurityUtils.getSubject();
            currentUser.getSession().touch();
            logger.info(output);
            return output;
        }

        public TickleItem getTickleItem(String serialNumber, String thingTypeCode, Date transactionDate,
                                        Map<String, Object> fields, List<Long> groupMqtt)
        {
            TickleItem tickleItem = new TickleItem();
            tickleItem.setSerialNumber(serialNumber);
            tickleItem.setThingTypeCode(thingTypeCode);
            tickleItem.setTransactionDate(transactionDate);
            tickleItem.setGroupMqtt(groupMqtt);

            if( (fields != null ) && (!fields.isEmpty()) ){
                for (String fieldName : fields.keySet())
                {
                    TickleFieldItem field = new TickleFieldItem();
                    field.setName(fieldName);
                    if (fields.get(fieldName) != null){
                        field.setValue(fields.get(fieldName).toString());
                    }else {
                        field.setValue("");
                    }
                    tickleItem.addField(field);
                }
            }
            return tickleItem;
        }


        //after all lines parsed, tickle new things
        @Override
        public void stop(List<Long> groupMqtt)
        {
            String kafkaEnabledValue = Configuration.getProperty("kafka.enabled");
            boolean kafkaEnabled = kafkaEnabledValue != null ? Boolean.parseBoolean(kafkaEnabledValue) : false;
            if(kafkaEnabled){
                performThingTickleKafka();
            } else{
                boolean successfullyCalled = performThingTickle();

                //TODO remove when coreBridge had been fixed !thingType.getThingTypeCode().contains(FMCConstants.FMC)
                if (successfullyCalled && !thingType.getThingTypeCode().contains(FMCConstants.FMC)) {
                    performThingFieldTickle();
                }
            }
        }

        public boolean performThingTickle()
        {
            Long reloadAllThingsThreshold = ConfigurationService.getAsLong(user, "reloadAllThingsThreshold");
            List<Long> groupMqtt = GroupService.getInstance().getMqttGroups(thingType.getGroup());
            if (reloadAllThingsThreshold == null)
            {
                logger.error("Cannot call the message to reload CoreBridge cache because " +
                        "the things cache reload threshold specified is invalid.");
                return false;
            }

            logger.info("reloadAllThingsThreshold=" + reloadAllThingsThreshold);

            if (thingsToTickle.size() > reloadAllThingsThreshold ){
                BrokerClientHelper.sendRefreshThingMessage(false, groupMqtt);
            }
            else
            {
                int i=1;
                for (TickleItem tickleItem : thingsToTickle)
                {
                    logger.info("Sending to refresh single thing message " + i + ": " + tickleItem.getSerialNumber());
                    BrokerClientHelper.sendRefreshSingleThingMessage(tickleItem.getThingTypeCode(), tickleItem.getSerialNumber(),
                            false, groupMqtt);
                    i++;
                }
            }
            return true;
        }

        public void performThingTickleKafka(){
            for(TickleItem tickleItem : thingsToTickle){
                BrokerClientHelper.initThingFieldTickleKafka(tickleItem.getThingTypeCode(),runRules);
                for (TickleFieldItem tickleFieldItem : tickleItem.getFields())
                {
                    BrokerClientHelper.setThingField(
                            tickleItem.getSerialNumber(),
                            tickleItem.getTransactionDate().getTime(),
                            tickleFieldItem.getName(),
                            tickleFieldItem.getValue());
                }
                BrokerClientHelper.sendThingFieldTickle();
            }
        }

        public void performThingFieldTickle()
        {
            boolean sendThingFieldTickle = ConfigurationService.getAsBoolean(user, "sendThingFieldTickle");
            logger.info("sendThingFieldTickle=" + sendThingFieldTickle);

            // overwriting "sendThingFieldTickle" in the configuration with the value of the form if it is true
            if (runRules){
                sendThingFieldTickle = true;
            }

            if (sendThingFieldTickle)
            {
                int i = 1;
                for (TickleItem tickleItem : thingsToTickle)
                {
                    logger.info("Sending thingField tickle " + i + ": " + tickleItem.getSerialNumber());
                    if (tickleItem.getFields().size() != 0)
                    {
                        if (i == 1) {
                            // Beginning thingFiled tickle
                            BrokerClientHelper.initThingFieldTickle(tickleItem.getThingTypeCode(), tickleItem.getGroupMqtt());
                        }

                        // Building message with UDFs
                        String value;
                        for (TickleFieldItem tickleFieldItem : tickleItem.getFields())
                        {
                            value = "\"" + tickleFieldItem.getValue() + "\"";
                            BrokerClientHelper.setThingField(tickleItem.getSerialNumber(),
                                    tickleItem.getTransactionDate().getTime(), tickleFieldItem.getName(), value);
                        }
                        i++;
                    }
                }
                // sending the message
                if (!thingsToTickle.isEmpty()) {
                    BrokerClientHelper.sendThingFieldTickle();
                }
            }
        }

        private Thing findParent(String parentSerial) throws NonUniqueResultException {
            Thing parent = null;
            //associate with parent
            if(StringUtils.isNotEmpty(parentSerial)) {
                parent = ThingService.getInstance().getBySerialNumber(parentSerial);
                if(parent == null)
                {
                    throw new UserException("Unable to get parent thing with serial " + parentSerial);
                }

                //validate that the parent thing type is valid for the thing type parents
                boolean foundType = false;

                for (ThingType parentThingType: thingType.getParents()) {
                    if (parentThingType.getId().equals( parent.getThingType().getId() )) {
                        foundType = true;
                        break;
                    }
                }

                if(!foundType) {
                    throw new UserException("The parent's thing type "+ parent.getThingType().getName() +
                                    " does not match any of " + thingType.getName() + "'s parents");
                }
            }

            return parent;

        }

        /**
         * Method to get Udf's so as to send to Thing Service
         */
        public Map<String, Object> getMapOfUdfs(ThingType thingType, CSVRecord row, Date storeData) {
            Map<String, Object> udfs = new HashMap<>();
            Map<String, Object> value = null;

            for (ThingTypeField thingTypeField : thingType.getThingTypeFields()) {
                try {
                    if (row.isMapped(thingTypeField.getName())
                            && !Utilities.isEmptyOrNull(row.get(thingTypeField.getName()))) {
                        value = new HashMap<>();
                        Object rowValue = Utilities.removeSpaces(row.get(thingTypeField.getName()));
                        if (ThingTypeField.Type.isDateOrTimestamp(thingTypeField.getDataType().getId())) {
                            String valueString = Utilities.removeSpaces(row.get(thingTypeField.getName()));
                            Date valueDate = DateHelper.getDateAndDetermineFormat(valueString);
                            if (valueDate != null) {
                                rowValue = valueDate;
                                if (ThingTypeField.Type.isTimestamp(thingTypeField.getDataType().getId())) {
                                    rowValue = valueDate.getTime();
                                }
                            }
                        }
                        value.put("value", rowValue);
                        value.put("time", storeData.getTime());
                        udfs.put(thingTypeField.getName(), value);
                    }
                } catch (Exception e) {
                    logger.error(e);
                    throw new UserException(e.getMessage());
                }
            }
            return udfs;
        }

        /**********************************************
         *  Method to get parent map so as to send to Thing Service
         **********************************************/
        public Map<String, Object> getMapParent(Thing parent)
        {
            Map<String, Object> response = null ;
            if(parent!=null)
            {
                response = new HashMap<String, Object>();
                response.put( "serialNumber", parent.getSerial() );
                response.put( "thingTypeCode", parent.getThingType().getThingTypeCode() );
            }

            return response;
        }
    }

    /**
     * handler to process a line of a thing type import file
     */
    private static class ThingTypeHandler implements ImportHandler
    {
        private ThingTypeService tts;
        private Cache cache;
        private ThingTypeMapService ttms;
        private Long modifiedTime;

        public ThingTypeHandler(ThingTypeService tts, Cache cache, ThingTypeMapService ttms) {
            this.tts = tts;
            this.cache = cache;
            this.ttms = ttms;
            this.modifiedTime = System.currentTimeMillis();
        }

        @Override
        public String parse(CSVRecord row) {
            Subject subject = SecurityUtils.getSubject();
            final User currentUser = (User) subject.getPrincipal();
            String output = "";
            try {
                String thingTypeCode = row.get("Code");
                String name = row.get("Name").trim();
                String template = row.isSet("Thing Type Template") ? row.get("Thing Type Template") : null;
                boolean isParent = row.isSet("isParent") ? Boolean.valueOf(row.get("isParent")) : false;
                boolean isAutoCreate = row.isSet("Auto Created") ? Boolean.valueOf(row.get("Auto Created")) : false;

                if (StringUtils.isNotEmpty(thingTypeCode) && StringUtils.isNotEmpty(name)) {
                    String parent = row.isSet("Parent") ? row.get("Parent") : "";
                    String ownerGroup = row.isSet("Owner Group") ? row.get("Owner Group") : "";
                    ThingType tt = cache.getThingTypeByCode(thingTypeCode, false);
                    Group group = cache.getGroup(ownerGroup);
                    if (!parent.isEmpty() && !ownerGroup.isEmpty()){
                        validate(tt, parent, group);
                    }
                    if (tt != null) {
                        Set<Long> ttIds = currentUser.getThingTypeResources();
                        if (ttIds.size() > 0 && ttIds.contains(tt.getId())) {
                            if (!PermissionsUtils.buildSearch(SecurityUtils.getSubject(), new
                                HashMap<String, Boolean>(), "thingType:u")) {
                                return "ERROR: User does not have permissions to update Thing Types";
                            }
                            tt.setName(name);
                            // insert or delete associated parents
                            List<ThingType> parentThingTypesToDelete = new LinkedList<>();
                            Map<String, List<ThingTypeMap>> parents = getParents(tt, parent);
                            if (parents.get("delete") != null && !parents.get("delete").isEmpty())
                                for (ThingTypeMap thingTypeMap : parents.get("delete")) {
                                    if (thingTypeMap.getParent().getChildrenTypeMaps() == null) {
                                        thingTypeMap.getParent().setChildrenTypeMaps(new
                                            HashSet<ThingTypeMap>());
                                    }
                                    thingTypeMap.getParent().getChildrenTypeMaps().remove
                                        (thingTypeMap);
                                    tt.getParentTypeMaps().remove(thingTypeMap);
                                    ttms.delete(thingTypeMap);
                                    parentThingTypesToDelete.add(thingTypeMap.getParent());
                                }
                            if (parents.get("insert") != null && !parents.get("insert").isEmpty())
                                for (ThingTypeMap thingTypeMap : parents.get("insert")) {
                                    ThingTypeMap newRelation = ttms.insert(thingTypeMap);
                                    if (newRelation.getParent().getChildrenTypeMaps() == null) {
                                        newRelation.getParent().setChildrenTypeMaps(new
                                            HashSet<ThingTypeMap>());
                                    }
                                    newRelation.getParent().getChildrenTypeMaps().add(thingTypeMap);
                                    tt.getParentTypeMaps().add(newRelation);
                                }
                            // update thingTypeTemplate
                            if (template != null && !template.isEmpty())
                                tt.setThingTypeTemplate(cache.getThingTypeTemplate(template));
                            tt.setIsParent(isParent);
                            tt.setAutoCreate(isAutoCreate);
                            tt.setModifiedTime(modifiedTime);
                            tts.update(tt);
                            output = "Updated thing type with code " + thingTypeCode;
                            tts.associate(tt, null, null, parentThingTypesToDelete, null);
                        } else {
                            output = "Insufficient permissions on Thing Type " + thingTypeCode;
                        }
                    } else {
                        if (!PermissionsUtils.buildSearch(SecurityUtils.getSubject(), new
                            HashMap<String, Boolean>(), "thingType:i")) {
                            return "ERROR: User does not have permissions to create new Thing Types";
                        }
                        if (!UserService.getInstance().getRootUser().equals(currentUser)&&
                                !group.equals(currentUser.getActiveGroup().getParentLevel2())){
                            throw new UserException("User does not have permissions on Tenant Group"+group.getCode());
                        }
                        ThingType thingType = new ThingType();
                        thingType.setName(name);
                        thingType.setGroup(group);
                        thingType.setThingTypeCode(thingTypeCode);
                        // set thingTypeTemplate
                        if (template == null || template.isEmpty())
                            thingType.setThingTypeTemplate( cache.getThingTypeTemplate("Custom Thing Type") );
                        else
                            thingType.setThingTypeTemplate(cache.getThingTypeTemplate(template));
                        thingType.setIsParent(isParent);
                        thingType.setAutoCreate(isAutoCreate);
                        thingType.setModifiedTime(modifiedTime);
                        if (thingType.getParentTypeMaps() == null) {
                            thingType.setParentTypeMaps(new HashSet<ThingTypeMap>());
                        }
                        LicenseDetail licenseDetail = LicenseService.getInstance().getLicenseDetail(currentUser.getActiveGroup(), true);
                        Long maxNumberOfThingTypes = licenseDetail.getMaxThingTypes();
                        Long countAll = null;
                        if (maxNumberOfThingTypes != null){
                             countAll = ThingTypeController.count(licenseDetail);
                        }
                        if (maxNumberOfThingTypes == null || countAll < maxNumberOfThingTypes) {
                            //persist new
                            tts.insert(thingType);
                            // persist associated parents
                            Map<String, List<ThingTypeMap>> parents = getParents(thingType, parent);
                            if (parents.get("insert") != null && !parents.get("insert").isEmpty())
                                for (ThingTypeMap thingTypeMap : parents.get("insert")) {
                                    if (thingTypeMap.getParent().getChildrenTypeMaps() == null) {
                                        thingTypeMap.getParent().setChildrenTypeMaps(new HashSet<ThingTypeMap>());
                                    }
                                    ttms.insert(thingTypeMap);
                                    thingTypeMap.getParent().getChildrenTypeMaps().add(thingTypeMap);
                                    thingType.getParentTypeMaps().add(thingTypeMap);
                                }
                            output = "Created a new thing type with code " + thingTypeCode;
                            tts.associate(thingType, null);
                        }else{
                            output = "You cannot insert more thing types because you reach the limit of your license";
                        }
                    }
                }
                else
                {
                    output = "Thing Type " + (StringUtils.isEmpty(thingTypeCode) ? "Code" : "Name") + " cannot be empty.";
                }
            } catch (UserException | IllegalArgumentException  e) {
                output = e.getMessage();
            }
            logger.info(output);
            return output;
        }

		@Override public void stop(List<Long> groupMqtt)
		{
            // calling thingTypeTickle to refresh coreBridge cache
            BrokerClientHelper.sendRefreshThingTypeMessage(false, groupMqtt);
		}

		/**
         * Get associated parents to add and delete
         * @param thingType
         * @param parents
         * @return
         */
        public Map<String, List<ThingTypeMap>> getParents (ThingType thingType, String parents)
        {
            logger.info("getting parents " + parents);
            Map<String, List<ThingTypeMap>> result = new HashMap<String, List<ThingTypeMap>>();
            List<ThingTypeMap> ttmInsert = new ArrayList<ThingTypeMap>();
            List<ThingTypeMap> ttmDelete = new ArrayList<ThingTypeMap>();

            //ThingType thingType = thingTypeCache.get(thingTypeCode);
            Set<ThingTypeMap> thingTypeMaps = thingType.getParentTypeMaps();

            String[] parentArray = new String[]{};
            if (parents != null && StringUtils.isNotEmpty(parents))
            {
                parentArray = parents.split(",");
            }

            // thingTypeMaps to insert
            if (parentArray.length>0)
            {
                for (int i=0;parentArray.length>i;i++)
                {
                    boolean foundThingType = false;
                    try
                    {
                        ThingType thingTypeParent = ThingTypeService.getInstance().getByCode(parentArray[i]);
                        if (thingTypeParent == null)
                            throw new UserException("Unable to get parent thing type with code " + parentArray[i]);
                        if (thingTypeMaps != null && !thingTypeMaps.isEmpty())
                        {
                            for (ThingTypeMap thingTypeMap : thingTypeMaps)
                            {
                                if (thingTypeMap.getParent().getId().compareTo(thingTypeParent.getId()) == 0)
                                {
                                    foundThingType = true;
                                    break;
                                }
                            }
                        }
                        if (!foundThingType)
                        {
                            ThingTypeMap thingTypeMap = new ThingTypeMap();
                            thingTypeMap.setParent(thingTypeParent);
                            thingTypeMap.setChild(thingType);
//                            thingTypeParent.getChildrenTypeMaps().add(thingTypeMap);
                            ttmInsert.add(thingTypeMap);
                        }
                    }
                    catch (NonUniqueResultException e)
                    {
                        throw new UserException("Unable to get thing type with code " + parentArray[i] + ". Duplicate code.", e);
                    }
                }
            }

            // thingTypeMaps to delete
            if (thingTypeMaps != null && !thingTypeMaps.isEmpty())
            {
                for (ThingTypeMap ttmParent : thingTypeMaps)
                {
                    boolean foundThingType = false;
                    for (int i = 0; parentArray.length > i; i++)
                    {
                        try
                        {
                            ThingType thingTypeParent = ThingTypeService.getInstance().getByCode(parentArray[i]);
                            if (thingTypeParent == null)
                                throw new UserException("Unable to get parent thing type with code " + parentArray[i]);
                            if (ttmParent.getParent().getId().compareTo(thingTypeParent.getId()) == 0)
                            {
                                foundThingType = true;
                                break;
                            }
                        }
                        catch (NonUniqueResultException e)
                        {
                            throw new UserException("Unable to get thing type with code "+parentArray[i]+". Duplicate code.", e);
                        }
                    }
                    if (!foundThingType)
                    {
                        ttmDelete.add(ttmParent);
                    }
                }
            }
            result.put("insert", ttmInsert);
            result.put("delete", ttmDelete);
            return result;
        }

        public void validate(ThingType thingType, String parents, Group group)
        {
            logger.info("validate thing type " + thingType);
            if (thingType != null)
            {
                String[] parentArray = new String[]{};
                if (parents != null && StringUtils.isNotEmpty(parents))
                {
                    parentArray = parents.split(",");
                }

                // validate child and parent must be different
                if (parentArray.length > 0)
                {
                    for (String parentCode : parentArray)
                    {
                        if (parentCode.equals(thingType.getThingTypeCode()))
                            throw new UserException( "Thing Type '" + thingType.getThingTypeCode() +
                                    "' cannot be a parent of the same Thing Type.");
                    }
                }

                if (group != null)
                {
                    if (parentArray.length > 0) {
                        // validate child and parents have to be in the same Tenant Group
                        List<String> nullParents = new ArrayList<String>();
                        for (String parentCode : parentArray) {
                            try
                            {
                                ThingType thingParent = ThingTypeService.getInstance().getByCode(parentCode);
                                if  (thingParent == null)
                                    nullParents.add(parentCode);
                                if (thingParent != null && group.getId().compareTo(thingParent.getGroup().getId()) != 0)
                                    throw new UserException("To associate two Thing Types, both have to be in " +
                                            "the same Tenant Group, please review '" + thingType.getThingTypeCode() +
                                            "' and '" + thingParent.getThingTypeCode() + "' association.");
                            }
                            catch (NonUniqueResultException e)
                            {
                                throw new UserException("Unable to get thing type with code " + parentCode +
                                        ". Duplicate code.", e);
                            }
                        }
                        // validate all parents must be different from null
                        if (!nullParents.isEmpty()){
                            StringBuilder sb = new StringBuilder();
                            for (String nullParent : nullParents) {
                                sb.append(nullParent);
                                sb.append(",");
                            }
                            throw new UserException("Parent Thing Type(s) '" + sb.toString().substring(0,
                                    sb.toString().length()-1) + "' do(es) not exist.");
                        }
                    }
                }
                // validate parents
                List<ThingType> thingTypeParents = thingType.getParents();
                if (thingTypeParents != null && thingTypeParents.size()>0)
                {
                    List<ThingType> thingTypeParentsDif = new ArrayList<ThingType>();
                    for (ThingType thingTypeParent : thingTypeParents)
                    {
                        for (int i=0; parentArray.length > i;i++)
                        {
                            if (thingTypeParent.getThingTypeCode().equals(parentArray[i]))
                                thingTypeParentsDif.add(thingTypeParent);
                        }
                    }
                    thingTypeParents.removeAll(thingTypeParentsDif);
                    // validate eliminated thing types as parents
                    List<Thing> things = ThingService.getInstance().selectByThingType(thingType.getId());
                    for (ThingType thingTypeParent : thingTypeParents)
                    {
                        boolean foundThing = false;
                        List<Thing> parentsThing = ThingService.getInstance().selectByThingType(thingTypeParent.getId());
                        for (Thing parent : parentsThing)
                        {
                            for (Thing thing : things)
                            {
                                if (thing.getParent() != null && thing.getParent().getId().compareTo(parent.getId()) == 0)
                                {
                                    foundThing = true;
                                    break;
                                }
                            }
                            if(foundThing) {
                                throw new UserException("Thing type parent " + thingTypeParent.getThingTypeCode() +
                                        " cannot be eliminated because there are things based in this parent-child schema.");
                            }
                        }
                    }
                }

                // validate if thingType is already parent of another Thing Type
                List<ThingTypeMap> thingTypeMaps = ThingTypeMapService.getInstance().getThingTypeMapByParentId(thingType.getId());
                if (parentArray.length > 0 && thingTypeMaps != null && !thingTypeMaps.isEmpty())
                {
                    throw new UserException("Thing Type(s) '" + parents + "' cannot be parent of '" +
                            thingType.getThingTypeCode() + "', because '" + thingType.getThingTypeCode() +
                            "' is already parent of another Thing Type.");
                }
                // validate if parents are already children of another Thing Types
                if (parentArray != null&& parentArray.length > 0) {
                    for (String parentCode : parentArray)
                    {
                        try
                        {
                            ThingType thingChild = ThingTypeService.getInstance().getByCode(parentCode);
                            List<ThingTypeMap> thingChildTypeMap = ThingTypeMapService.getInstance().
                                    getThingTypeMapByChildId( thingChild.getId() );
                            if (parentArray.length > 0 && thingChildTypeMap != null && !thingChildTypeMap.isEmpty()) {
                                throw new UserException("Thing Type '" + thingType.getThingTypeCode() +
                                        "' cannot be a child of '" + parentCode + "', because '" + parentCode +
                                        "' is already a child of another Thing Type.");
                            }
                        }
                        catch (NonUniqueResultException e)
                        {
                            throw new UserException("Unable to get thing type with code " + parentCode +
                                    ". Duplicate code.", e);
                        }
                    }
                }
            }
        }

    }

    /**
     * handler to process a line of a thing type property import file
     */
    private static class ThingTypePropertyHandler implements ImportHandler
    {
        private ThingTypeService ttService;
        private ThingTypeFieldService ttfService;
        private Cache cache;

        public ThingTypePropertyHandler(ThingTypeService tts, Cache cache) {
            this.ttService = tts;
            this.ttfService = ThingTypeFieldService.getInstance();
            this.cache = cache;
        }

        @Override
        public String parse(CSVRecord row) {
            String output = "";
            try {
                ThingType tt = validatedThingType(row.get("Thing Type Code"));
                String name = row.get("Thing Type Property");
                String unit = row.get("Unit");
                String type = row.get("Type");
                String dataTypeThingTypeCode = row.get("Type Thing Type Code");
                String symbol = row.get("Symbol");
                String timeSeries = row.get("Time Series");
                String defaultValue = row.get("Default Value");
                ThingTypeField thingTypeField = tt.getThingTypeFieldByName(name);
                if (validateFields(name, dataTypeThingTypeCode, type, timeSeries) &&
                        validatePermissions(tt, thingTypeField)) {
                    output = "Updating thing type field " + name + "...";
                    if (thingTypeField == null) {
                        output = "Inserting thing type field " + name + " to thing type with code " +
                                tt.getCode() + "...";
                        thingTypeField = new ThingTypeField();
                        thingTypeField.setThingType(tt);
                        thingTypeField.setName(name);
                        //TODO: fast fix
                        if (tt.getThingTypeFields() == null) {
                            tt.setThingTypeFields(new HashSet<ThingTypeField>());
                        }
                    }
                    populate(thingTypeField, unit, type, dataTypeThingTypeCode, symbol, timeSeries,
                            defaultValue);
                    List<Map<String, Object>> thingTypeList = new ArrayList<>();
                    Map<String, Object> thingType = new HashMap<>();
                    thingType.putAll(thingTypeField.publicMap());
                    thingTypeList.add(thingType);
                    ttService.updateFields(thingTypeList, tt, false);
                    List<ThingTypeField> thingTypeFieldUDFList = new LinkedList<>();
                    if (thingTypeField.isThingTypeUDF()) {
                        thingTypeFieldUDFList.add(thingTypeField);
                    }
                    ttService.associate(tt, thingTypeFieldUDFList);
                }
            } catch (UserException | IllegalArgumentException  e) {
                output = e.getMessage();
            }
            logger.info(output);
            return output;
        }

        @Override
        public void stop(List<Long> groupMqtt) {
            // calling thingType tickle to refresh coreBridge cache
            BrokerClientHelper.sendRefreshThingTypeMessage(false, groupMqtt);
        }

		/**
         * helper method that sets the values and validates.
         */
        private void populate(ThingTypeField ttf, String unit, String type, String typeThingTypeCode,  String symbol,
                              String timeSeries, String defaultValue) throws IllegalArgumentException {
            // TODO fflores: there isn't a source column in csv to set multiple
            ttf.setMultiple(false);

            if (StringUtils.isNotEmpty(unit)) {
                ttf.setUnit(unit);
            }
            if (StringUtils.isNotEmpty(symbol)) {
                ttf.setSymbol(symbol);
            }
            if (StringUtils.isNotEmpty(timeSeries)) {
                ttf.setTimeSeries(Boolean.parseBoolean(timeSeries.toLowerCase()));
            }

            // set type
            Long typeId = null;
            if (StringUtils.isEmpty(type) || !StringUtils.isNumeric(type)) {
                throw new IllegalArgumentException("Property Type value is empty or not numeric");
            } else {
                typeId = Long.parseLong(type);
                ThingTypeField.Type typeAsEnum = ThingTypeField.Type.getTypeByValue(typeId);
                if(typeAsEnum == null) {
                    throw new IllegalArgumentException("Invalid Property Type value " + type);
                }
                ttf.setDataType(DataTypeService.getInstance().get(typeId));
            }

            // set typeParent
            if (typeId == ThingTypeField.Type.TYPE_THING_TYPE.value) {
                ttf.setTypeParent(ThingTypeField.TypeParent.TYPE_PARENT_NATIVE_THING_TYPE.value);
            } else {
                ttf.setTypeParent(ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value);
            }

            // set dataTypeThingType
            Long typeThingTypeId = null;
            if (!Utilities.isEmptyOrNull(typeThingTypeCode)) {
                ThingType thingType = cache.getThingTypeByCode(typeThingTypeCode, true);
                if (thingType != null) {
                    typeThingTypeId = thingType.getId();
                    ttf.setDataTypeThingTypeId(typeThingTypeId);
                }
            }

            // set defaultValue
            ttf.setDefaultValue(null);
            if (!Utilities.isEmptyOrNull(defaultValue)) {
                // validation for default value: shift, logicalReader, zone, group, thingType
                ThingTypeField.Type typeAsEnum = null;
                if (ttf.getDataType() != null) {
                    try {
                        typeAsEnum = ThingTypeField.Type.getTypeByValue(ttf.getDataType().getId());
                    } catch (NumberFormatException e) {
                        logger.error(e);
                        throw new IllegalArgumentException("Type is invalid for property '" + ttf.getName() + "'", e);
                    }
                }
                if (typeAsEnum != null) {
                    switch (typeAsEnum) {
                        case TYPE_SHIFT:
                            Shift shift = cache.getShiftByCode(defaultValue);
                            ttf.setDefaultValue(shift.getCode());
                            break;
                        case TYPE_GROUP:
                            Group group = cache.getGroupByCode(defaultValue);
                            ttf.setDefaultValue(group.getCode());
                            break;
                        case TYPE_ZONE:
                            Zone zone = cache.getZoneByCode(defaultValue);
                            ttf.setDefaultValue(zone.getCode());
                            break;
                        case TYPE_LOGICAL_READER:
                            LogicalReader logReader = cache.getLogicalReaderByCode(defaultValue);
                            ttf.setDefaultValue(logReader.getCode());
                            break;
                        case TYPE_THING_TYPE:
                            Thing thing = cache.getThingBySerialAndThingType(defaultValue,typeThingTypeId);
                            ttf.setDefaultValue(thing.getSerial());
                            break;
                        case TYPE_NUMBER:
                            if (!Utilities.isNumber(defaultValue)){
                                throw new IllegalArgumentException("Default value: '"+ defaultValue + "' is invalid for property '" + ttf.getName() + "'");
                            }
                            ttf.setDefaultValue(defaultValue);
                            break;
                        case TYPE_DATE:
                        case TYPE_TIMESTAMP:
                            try {
                                Date date = null;
                                if (DateHelper.isTimeStampMillis(defaultValue)) {
                                    date = new Date(Long.parseLong(defaultValue));
                                } else if (DateHelper.isTimeStampSecs(defaultValue)) {
                                    date = new Date(Long.parseLong(defaultValue) * 1000);
                                } else {
                                    date = DateTimeFormatterHelper.parseDateTextAndDetermineFormat(Utilities.removeSpaces(defaultValue));
                                }
                                ttf.setDefaultValue(String.valueOf(date.getTime()));
                            } catch (Exception e) {
                                throw new IllegalArgumentException("Default value is invalid:" + e.getMessage());
                            }
                            break;
                        default:
                            ttf.setDefaultValue(defaultValue);
                            break;
                    }
                }
            }
        }

        /**
         * Validate the thing type code requested, and return the corresponding Thing Type if it is allowed to current user
         *
         * @param thingTypeCode
         * @return
         * @throws UserException
         */
        public ThingType validatedThingType(String thingTypeCode) throws UserException {
            if(StringUtils.isEmpty(thingTypeCode)){
                throw new UserException("Invalid Thing Type Code");
            }
            final User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
            Set<Long> ttIds = currentUser.getThingTypeResources();
            ThingType tt = cache.getThingType(thingTypeCode);
            if (ttIds.size() == 0 || !ttIds.contains(tt.getId())) {
                throw new UserException("Insufficient permissions on Thing Type " + tt.getCode());
            }
            return tt;
        }

        /**
         * Validate Permissions for the current user is allowed to modify thingField if it exist, or validate
         * permissions on Thing Type if Thing Type Field not exist
         *
         * @param ttf
         * @param tt
         * @return
         * @throws UserException
         */
        public boolean validatePermissions(ThingType tt, ThingTypeField ttf) throws UserException {
            final User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
            if (ttf != null){
                if( !ttf.getThingType().getGroup().equals(currentUser.getActiveGroup())
                        && !UserService.getInstance().getRootUser().equals(currentUser)) {
                    throw new UserException("User does not have permissions on Tenant Group " +
                            ttf.getThingType().getGroup().getCode());
                }
            }else if ( !tt.getGroup().equals(currentUser.getActiveGroup())
                    && !UserService.getInstance().getRootUser().equals(currentUser)) {
                throw new UserException("User does not have permissions on Tenant Group " + tt.getGroup().getCode());
            }
            return true;
        }

        /**
         *
         * @return
         * @throws UserException
         */
        public boolean validateFields(String name, String dataTypeThingTypeCode, String type, String timeSeries)
                throws UserException {
            if (!isValidName(name)) {
                throw new UserException("Invalid property name");
            }
            if (!isValidDataTypeThingTypeCode(dataTypeThingTypeCode, Long.parseLong(type))) {
                throw new UserException("Type Thing Type Code cannot be null");
            }
            if (!isValidTimeSeries(Long.parseLong(type), timeSeries)) {
                throw new UserException("Time Series value in Property Thing Type UDF '" + name + "' should be true");
            }
            return true;
        }

        /**
         *
         * @param name
         * @return true when the param name is not empty
         */
        public boolean isValidName(String name) {
            return !StringUtils.isEmpty(name);
        }

        /**
         *
         * @param dataTypeThingTypeCode
         * @param type
         * @return true when the thing type is different to 27 or if it is, the data type thing type code doesn't have to be empty
         */
        public boolean isValidDataTypeThingTypeCode(String dataTypeThingTypeCode, Long type) {
            return !(type ==(ThingTypeField.Type.TYPE_THING_TYPE.value)) || (type ==(ThingTypeField.Type.TYPE_THING_TYPE.value) && dataTypeThingTypeCode != null && dataTypeThingTypeCode.length()>0);
        }

        /**
         *
         * @param timeSeries
         * @return true only when the param timeSeries is TRUE (as boolean)
         */
        public boolean isValidTimeSeries(Long type, String timeSeries) {
            if (type == ThingTypeField.Type.TYPE_THING_TYPE.value) {
                return timeSeries != null && Boolean.parseBoolean(timeSeries.toLowerCase());
            }
            return true;
        }

    }

    /**
     * handler to process a line of a user import file
     */
    private static class UserHandler implements ImportHandler {
        private String homeGroupStr;
        private String roamingGroupStr;
        private String username;
        private String roleListStr;
        private String firstName;
        private String lastName;
        private String email;
        private String password;
        private Group group;
        private Group roamingGroup;
        private String flagString = "--Not Required--";
        private String timeZone;
        private String dateFormat;

        @Override
        public String parse(CSVRecord row) {
            User user = null;
            Boolean isNewUser = false;
            String usernameMessage = "";
            String output;
            try {
                if (!row.isSet("Username") || !row.isSet("Home Group")) {
                    return "ERROR: 'Username' and 'Home Group' are required columns.";
                }
                this.username = row.get("Username");
                this.homeGroupStr = row.get("Home Group");
                ValidationBean validationBean = new ValidationBean();
                validateUserGroup(validationBean, this.homeGroupStr.trim());
                usernameMessage = this.username.isEmpty()?"":" Username [" + this.username + "] ";
                if (validationBean.isError()) {
                    return "ERROR: " + usernameMessage + validationBean.getErrorDescription();
                }
                user = UserService.getInstance().selectByUsernameGroup(username, this.group.getId());
                isNewUser = (user == null);
                validationBean = validateUserRow(row, isNewUser);
                if (validationBean.isError()) {
                    return "ERROR: " + usernameMessage + validationBean.getErrorDescription();
                }

                // Creating userMap
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("group.id", this.group.getId());
                if (!this.roamingGroupStr.equals(flagString)) {
                    userMap.put("roamingGroup.id", (this.roamingGroup != null) ? this.roamingGroup.getId() : null);
                }
                userMap.put("username", this.username);
                if (!this.firstName.equals(flagString)) {
                    userMap.put("firstName", this.firstName);
                }
                if (!this.lastName.equals(flagString)) {
                    userMap.put("lastName", this.lastName);
                }
                if (!this.email.equals(flagString)) {
                    userMap.put("email", this.email);
                }
                if (!this.timeZone.equals(flagString)){
                    userMap.put("timeZone", this.timeZone);
                }

                if (!this.dateFormat.equals(flagString)){
                    userMap.put("dateFormat", this.dateFormat);
                }
                List<String> rolesNotDetected = new ArrayList<>();
                validationBean = validateRootGroup(isNewUser);
                if (validationBean.isError()) {
                    return "ERROR: " + usernameMessage + validationBean.getErrorDescription();
                }
                if (isNewUser) {
                    if (!PermissionsUtils.buildSearch(SecurityUtils.getSubject(), new HashMap<String, Boolean>(), "user:i")) {
                        return "ERROR: " + usernameMessage + " User does not have permissions to create new users";
                    }
                    if (validationBean.isError()) {
                        return "ERROR: " + usernameMessage + validationBean.getErrorDescription();
                    }
                    if (!this.password.equals(flagString)) {
                        userMap.put("password", this.password);
                    }
                    if (ConfigurationService.getAsBoolean(this.group, "passwordLoginChange")) {
                        userMap.put("forcePasswordChange", true);
                    } else {
                        userMap.put("forcePasswordChange", false);
                    }
                    user = UserService.getInstance().createUser(userMap);
                    rolesNotDetected = UserService.getInstance().addRolesToUser(this.roleListStr, user);
                    if (!userMap.get("password").toString().isEmpty() && AuthenticationUtils.LDAP_AD_AUTHENTICATION.
                            equals(ConfigurationService.getAsString(user.getActiveGroup(), AuthenticationUtils.AUTHENTICATION_MODE)) ){
                        output = "SUCCESS: " + usernameMessage + "User created. Password change will not have effect while AD/LDAP is active.";
                    } else {
                        output = "SUCCESS: " + usernameMessage + "User created";
                    }
                } else {
                    if (!PermissionsUtils.buildSearch(SecurityUtils.getSubject(), new HashMap<String, Boolean>(), "user:u")) {
                        return "ERROR: " + usernameMessage + " User does not have permissions to update users";
                    }
                    if (!this.password.isEmpty() && !this.password.equals(flagString)) {
                        userMap.put("password", this.password);
                    }
                    if (ConfigurationService.getAsBoolean(this.group, "passwordLoginChange")) {
                        userMap.put("forcePasswordChange", true);
                    } else {
                        userMap.put("forcePasswordChange", false);
                    }
                    Map<String, Object> userUpdated = UserService.getInstance().updateUser(user.getId(), userMap, true);
                    if (userUpdated.containsKey("error")) {
                        output = userUpdated.get("message").toString();
                    } else {
                        output = "SUCCESS: " + usernameMessage + "User updated";
                    }
                    if (!this.roleListStr.equals(flagString)) {
                        List<UserRole> userRoleList = UserRoleService.getInstance().listByUser(user);
                        List<UserRole> rolesToDelete = UserService.getInstance().getRolesToDelete(userRoleList, this.roleListStr);
                        UserService.getInstance().removeUserRoles(rolesToDelete);
                        this.roleListStr = UserService.getInstance().getRolesToAdd(userRoleList, this.roleListStr);
                        rolesNotDetected = UserService.getInstance().addRolesToUser(this.roleListStr, user);
                    }
                }
                if (!rolesNotDetected.isEmpty()) {
                    output += " Roles " + rolesNotDetected.toString() + " were not detected.";
                }
            } catch(ForbiddenException e) {
                String action = "create";
                if ((user != null) && (isNewUser)) {
                    UserService.getInstance().delete(user);
                } else {
                    action = "update";
                }
                output = "ERROR: " + usernameMessage + "You do not have permissions to " + action + " this user.";
            } catch (UserException e) {
                output = "ERROR: " + e.getMessage();
            }
            return output;
        }

        /**
         * Validate Home Group and Roaming Group is different to root
         * @return validationBean
         * @param isNewUser
         */
        private ValidationBean validateRootGroup(Boolean isNewUser) {
            ValidationBean validationBean = new ValidationBean();
            if (isNewUser && (this.group != null) && (this.group.equals(GroupService.getInstance().getRootGroup()))) {
                validationBean.setErrorDescription(" Is not possible to create user with 'root' as Home Group.");
            }
            String action = "update";
            if (isNewUser) {
                action = "create";
            }
            if ((this.roamingGroup != null) && (this.roamingGroup.equals(GroupService.getInstance().getRootGroup()))) {
                validationBean.setErrorDescription(" Is not possible to " + action + " a user with 'root' as Roaming Group.");
            }
            return validationBean;
        }

        @Override
        public void stop(List<Long> groupMqtt) {

        }

        /**
         * Validate User's row
         */
        private ValidationBean validateUserRow(CSVRecord row, Boolean isNewUser) {
            ValidationBean validationBean = new ValidationBean();
            try {
                if (row.isSet("Roles")) {
                    this.roleListStr = row.get("Roles");
                } else {
                    if (isNewUser) {
                        validationBean.setErrorDescription("Error parsing row. 'Roles' column is required to create new users");
                        return validationBean;
                    }
                    this.roleListStr = flagString;
                }
                if (row.isSet("Roaming Group")) {
                    this.roamingGroupStr = row.get("Roaming Group");
                } else {
                    this.roamingGroupStr = this.flagString;
                }
                if (row.isSet("First name")) {
                    this.firstName = row.get("First name");
                } else {
                    this.firstName = this.flagString;
                }
                if (row.isSet("Last name")) {
                    this.lastName = row.get("Last name");
                } else {
                    this.lastName = this.flagString;
                }
                if (row.isSet("Email")) {
                    this.email = row.get("Email");
                } else {
                    this.email = this.flagString;
                }
                if (row.isSet("Password")) {
                    this.password = row.get("Password");
                } else {
                    this.password = this.flagString;
                }
                if (row.isSet("Time Zone")){
                    this.timeZone = row.get("Time Zone");
                }else {
                    this.timeZone = this.flagString;
                }
                if (row.isSet("Date Format")){
                    this.dateFormat = row.get("Date Format");
                }else{
                    this.dateFormat = this.flagString;
                }
            } catch (Exception e) {
                validationBean.setErrorDescription("Unknown error was detected parsing row. " + e.getMessage());
                return validationBean;
            }

            validateUserRoamingGroup(validationBean, this.roamingGroupStr.trim());
            if (!validationBean.isError() && !this.roleListStr.equals(flagString)) {
                validateUserRoles(validationBean, roleListStr.trim(), this.group);
            }
            return validationBean;
        }

        /**
         * Validate User Roles
         */
        private void validateUserRoles(ValidationBean validationBean, String roleList, Group group) {
            if (roleList.isEmpty()) {
                validationBean.setErrorDescription("Role is required.");
                return;
            }
            String list[] = roleList.split(";");
            for (String roleName:list) {
                if (roleName.trim().isEmpty()) {
                    validationBean.setErrorDescription("Role's Name is required.");
                }
            }
            List<String> invalidRoles = new ArrayList<>();
            for (String roleName:list) {
                String groupList[] = group.getHierarchyName(false).split(">");
                Boolean roleIsValid = false;
                for (String groupStr:groupList) {
                    try {
                        if (groupStr.isEmpty()) {
                            continue;
                        }
                        List<Role> roleList1;
                        if (groupStr.equals("root")) {
                            roleList1 = RoleService.getInstance().getByName(roleName, null);
                        } else {
                            Group group1 = GroupService.getInstance().getByCode(groupStr);
                            roleList1 = RoleService.getInstance().getByName(roleName, group1);
                        }
                        if (!roleList1.isEmpty()) {
                            roleIsValid = true;
                            break;
                        }
                    } catch (NonUniqueResultException e) {
                        validationBean.setErrorDescription("Group [" + groupStr + "] is not an unique group");
                        return;
                    }
                }
                if (!roleIsValid) {
                    invalidRoles.add(roleName);
                }
            }
            if (!invalidRoles.isEmpty()) {
                validationBean.setErrorDescription("Role " + invalidRoles.toString() + " not found, for any Group [" + this.group.getHierarchyName(false) + "].");
            }
        }

        /**
         * Validate User Group
         */
        private void validateUserGroup(ValidationBean validationBean, String groupStr) {
            if (groupStr.isEmpty()) {
                validationBean.setErrorDescription("Home Group is required.");
                return;
            }
            try {
                this.group = GroupService.getInstance().getByHierarchyCode(groupStr);
                if (this.group == null) {
                    validationBean.setErrorDescription("Home Group [" + groupStr + "] not found.");
                }
            } catch (NonUniqueResultException e) {
                validationBean.setErrorDescription("Home Group is non unique.");
            }
        }

        /**
         * Validate User Roaming Group
         */
        private void validateUserRoamingGroup(ValidationBean validationBean, String roamingGroupStr) {
            try {
                if (roamingGroupStr.isEmpty() || this.roamingGroupStr.equals(flagString)) {
                    this.roamingGroup = null;
                    return;
                }
                this.roamingGroup = GroupService.getInstance().getByHierarchyCode(roamingGroupStr);
                if (this.roamingGroup == null) {
                    validationBean.setErrorDescription("Roaming Group [" + roamingGroupStr + "] not found.");
                }
            } catch (NonUniqueResultException e) {
                validationBean.setErrorDescription("Roaming Group [" + roamingGroupStr + "] is non unique.");
            }
        }


    }
}
