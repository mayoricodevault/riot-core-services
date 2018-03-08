package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.QUser;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.entities.UserRole;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.appcore.utils.GeneralVisibilityUtils;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.iot.controllers.ThingTypeController;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.utils.BackgroundProgressStatus;
import com.tierconnect.riot.iot.utils.Cache;
import com.tierconnect.riot.iot.utils.VisibilityThingUtils;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by pablo on 1/6/15.
 *
 * Exports all entities to a CSV
 */
public class FileExportService {
    static Logger logger = Logger.getLogger(FileExportService.class);

    public static enum Type {THING, THING_TYPE, THING_TYPE_PROPERTY, REPORT, USER}
    private ThingType thingType;
    private List<ThingTypeField> thingTypeFields; //list that maintains order

    private List<String> headersExport;
    private List<List<String> > valuesExport;


    private Cache cache;

    public FileExportService(List<String> headers, List<List<String > > values) {
        this.headersExport = headers;
        this.valuesExport = values;
        this.cache = new Cache();
    }

    public FileExportService(ThingType thingType) {
        this.cache = new Cache();
        this.thingType = thingType;

        if(this.thingType != null)
        {
            this.thingTypeFields = new ArrayList<>(thingType.getThingTypeFields());

            Collections.sort(thingTypeFields, new Comparator<ThingTypeField>() {
                @Override
                public int compare(ThingTypeField o1, ThingTypeField o2) {
                    return (int)(o1.getId() - o2.getId());
                }
            });

            if (thingType.getParents() != null) {

            }
        }
    }

    public File export(Type type) throws IOException {
        return export(type, Charset.defaultCharset(), null);
    }

    public File export(Type type, Charset charset, Long id) throws IOException{
        logger.info("Starting export " + type);
        File file = File.createTempFile("export", ".csv");
        file.deleteOnExit();

//        FileWriter fw = new FileWriter(file);
        FileOutputStream fileStream = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(fileStream, charset);
        ExportHandler eh = getHandler(type, id);
        CSVPrinter printer = null;
        try
        {
            //set up the CSV printer
            printer = CSVFormat.EXCEL.withHeader(eh.headers()).print(writer);
            eh.process(printer);
        }
        finally
        {
            if (printer != null) {
                printer.flush();
                printer.close();
            }
        }

        return file;
    }

    private interface ExportHandler {
        public String[] headers();
        public void process(CSVPrinter p) throws IOException;
    }


    /**
     * Helper method that instantiates the appropriate handler for a file
     * @param type type of format to handle
     * @return the appropriate handler for the file format
     */
    private ExportHandler getHandler(Type type, Long id)
    {
            ExportHandler ih = null;
            Subject subject = SecurityUtils.getSubject();
            final User currentUser = (User) subject.getPrincipal();
            final String typeName = type.name();

            switch (type) {
                case THING:
                    ih = new ExportHandler() {
                        @Override
                        public String[] headers() {
                            //get static column names and then the variable thing type field column names
                            List<String> headers = new ArrayList<>();
                            headers.add("Thing Type Code");
                            headers.add("Thing Serial");
                            headers.add("Thing Name");
                            headers.add("ownerGroup");

                            for (ThingTypeField thingTypeField : thingTypeFields) {
                                headers.add(thingTypeField.getName());
                            }

                            //add parent header if thing type has parents
                            if (thingType.getParents() != null && thingType.getParents().size() > 0) {
                                headers.add("Parent");
                            }
                            return headers.toArray(new String[headers.size()]);
                        }

                        @Override
                        public void process(CSVPrinter p) throws IOException {


                            ImportExport exportFile = ImportExportService.getInstance().get(id);
                            if (exportFile == null) {
                                throw new UserException(String.format("ExportId[%d] not found.", id));
                            }
                            String temporalName = exportFile.getId() + "export";

                            exportFile.setUserId(currentUser.getId());
                            exportFile.setType(typeName);
                            exportFile.setProcessType("Export");
                            Long startTime = System.currentTimeMillis();
                            Long dateTmp = (new Date()).getTime()/1000;
                            exportFile.setStartDate(new Date(dateTmp*1000));

                            String filter = "thingTypeCode,serialNumber,name,groupId";
                            String[] dataThing = filter.split(",");
                            ArrayList arrayListData = new ArrayList(Arrays.asList(dataThing));

                            List<String> filterFields = new ArrayList<String>();
                            filterFields.addAll(arrayListData);

                            Set<String> timestampDateFields = new HashSet<>();
                            for (ThingTypeField thingTypeField : thingTypeFields) {
                                if (thingTypeField.getDataType().getTypeParent().equals(ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value)
                                        && thingTypeField.getDataType().getType().equals(
                                        ThingTypeField.TypeParentSubGroup.TYPE_PARENT_DATA_TYPE_STANDARD_DATA.value)) {
                                    filterFields.add(thingTypeField.getName() + ".value");
                                    if (ThingTypeField.Type.isDateOrTimestamp(thingTypeField.getDataType().getId())) {
                                        timestampDateFields.add(thingTypeField.getName() + ".value");
                                    }
                                } else if (thingTypeField.getDataType().getTypeParent().equals(ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value)
                                        && thingTypeField.getDataType().getType().equals(
                                        ThingTypeField.TypeParentSubGroup.TYPE_PARENT_DATA_TYPE_NATIVE_OBJECT.value)) {
                                    filterFields.add(thingTypeField.getName() + ".value.code");
                                } else if (thingTypeField.getDataType().getTypeParent().equals(ThingTypeField.TypeParent.TYPE_PARENT_NATIVE_THING_TYPE.value)) {
                                    filterFields.add(thingTypeField.getName() + ".value.serialNumber");
                                }
                            }
                            if (thingType.getParents() != null && thingType.getParents().size() > 0) {
                                filterFields.add("parent.serialNumber");
                            }

                            Long visibilityGroupId = currentUser.getActiveGroup().getId();
                            Map<Long, List<ThingType>> groups = VisibilityThingUtils.calculateThingsVisibility(visibilityGroupId);
                            if (groups == null) {
                                throw new UserException("Group of the user is not valid.");
                            }
                            List<Long> groupIds = FileExportService.getGroupIds(groups, "false", "true");
                            StringBuilder where = new StringBuilder();
                            where.append("thingTypeCode=").append(thingType.getThingTypeCode());
                            if (groupIds != null && !groupIds.isEmpty()) {
                                where.append("&groupId$in[");
                                String groupIdsString = "";
                                for (Long groupId : groupIds) {
                                    groupIdsString += groupId + ",";
                                }
                                where.append(groupIdsString.substring(0, groupIdsString.length() - 1));
                                where.append("]");
                            }
                            Map<String, Object> results = ThingMongoDAO.getInstance().getThingUdfValues(where.toString(), null, filterFields, null);
                            exportFile.setTotalRecord(Long.parseLong( results.get("total").toString()));
                            exportFile.setSuccessRecord(Long.parseLong( results.get("total").toString()));
                            BackgroundProcess backgroundProcess = new BackgroundProcess();
                            backgroundProcess.setIniDate(exportFile.getStartDate());
                            backgroundProcess.setCreatedByUser(currentUser);
                            backgroundProcess.setStatus(Constants.ADDED);
                            backgroundProcess.setTypeProcess("export");
                            backgroundProcess.setProgress(0);
                            String nameThread = FileExportService.class.getName() + "-RD-" + exportFile.getId();
                            backgroundProcess.setThreadName(nameThread);
                            BackgroundProgressStatus.getInstance().setMapStatus(String.valueOf(exportFile.getId()), "ADDED",0, backgroundProcess,"export",currentUser.getId(),0,0,backgroundProcess.getIniDate());
                            backgroundProcess.setTotalRecords(exportFile.getTotalRecord());
                            backgroundProcess.setTotalOmittedRecords(0L);
                            double updateBackground = ConfigurationService.getAsInteger(currentUser, "background_percentUpdate").doubleValue();
                            backgroundProcess = BackgroundProcessService.getInstance().insert(backgroundProcess);
                            BackgroundProcessEntity backgroundProcessEntity = new BackgroundProcessEntity();
                            backgroundProcessEntity.setBackgroundProcess(backgroundProcess);
                            backgroundProcessEntity.setColumnValue(exportFile.getId().toString());
                            backgroundProcessEntity.setModuleName("export");
                            backgroundProcessEntity.setColumnName("exportId");
                            BackgroundProcessEntityService.getInstance().insert(backgroundProcessEntity);
                            double delta = backgroundProcess.getTotalRecords()* (updateBackground/100.0);
                            double div = 1.0;
                            int progressRatio = (int) Math.rint(delta);
                            if (progressRatio == 0) {
                                div = progressRatio;
                                progressRatio = 1;
                            }
                            int progressValue = progressRatio;
                            DateFormatAndTimeZone dateFormatAndTimeZone = UserService.getInstance().getDateFormatAndTimeZone(currentUser);
                            logger.info("USER [" + currentUser.getUsername() + "] REGIONAL SETTING " + dateFormatAndTimeZone);
                            if (results != null) {
                                for (Object data : (List) results.get("results")) {
                                    List<Object> fieldValues = new ArrayList<>();
                                    Map<String, Object> dataMap = (Map<String, Object>) data;
                                    int i = 0;
                                    for (String filterField : filterFields) {
                                        boolean addFilterField = true;
                                        for (String timeStampField : timestampDateFields) {
                                            if (filterField.equals(timeStampField)) {
                                                Long timeSt = null;
                                                if (!(dataMap.get(filterField) instanceof Long)) {
                                                    if (dataMap.get(filterField) instanceof String && !((String) dataMap.get(filterField)).isEmpty()) {
                                                        timeSt = Long.parseLong((String) dataMap.get(filterField));
                                                    } else if (dataMap.get(filterField) instanceof Date) {
                                                        timeSt = ((Date) dataMap.get(filterField)).getTime();
                                                    }
                                                } else {
                                                    timeSt = (Long) dataMap.get(filterField);
                                                }
                                                if (timeSt != null) {
                                                    Timestamp ts = new Timestamp(timeSt);
                                                    String tsAsString = dateFormatAndTimeZone.getISODateTimeFormatWithoutTimeZone(ts);
                                                    fieldValues.add(tsAsString);
                                                    addFilterField = false;
                                                    break;
                                                }

                                            } else if (dataMap.get(filterField) instanceof Date) {
                                                Date date = (Date) dataMap.get(filterField);
                                                String dateFormat = dateFormatAndTimeZone.getISODateTimeFormatWithoutTimeZone(date);
                                                fieldValues.add(dateFormat);
                                                addFilterField = false;
                                                break;
                                            }
                                        }
                                        if (filterField.equals("groupId")) {
                                            // get hierarchy name group
                                            String hierarchyNameGroup = cache.getHierarchyNameGroupById((Long) dataMap.get(filterField));
                                            fieldValues.add(hierarchyNameGroup);
                                        } else if (addFilterField) {
                                            if (dataMap.get(filterField) instanceof Double) {
                                                DecimalFormat decimalFormat = new DecimalFormat("#.##############");
                                                fieldValues.add(decimalFormat.format(dataMap.get(filterField)));
                                            } else {
                                                fieldValues.add(dataMap.get(filterField));
                                            }
                                        }
                                    }
                                    p.printRecord(fieldValues);

                                    if (progressValue == i) {
                                        backgroundProcess.setStatus(Constants.IN_PROGRESS);
                                        backgroundProcess.setProgress((int) (((progressValue / progressRatio)*updateBackground)/div));
                                        progressValue = progressValue + progressRatio;
                                        backgroundProcess = BackgroundProcessService.getInstance().update(backgroundProcess);
                                        BackgroundProgressStatus.getInstance().setMapStatus(String.valueOf(exportFile.getId()), backgroundProcess.getStatus(),backgroundProcess.getProgress(), backgroundProcess,"export",currentUser.getId(), backgroundProcess.getTotalRecords().intValue(),i,backgroundProcess.getIniDate());
                                    }
                                    i = i + 1;
                                }
                            }
                            Long endTime = System.currentTimeMillis();
                            Long dateTmpEnd = (new Date()).getTime()/1000;
                            exportFile.setEndDate(new Date(dateTmpEnd*1000));
                            ImportExportService.getInstance().update(exportFile);
                            backgroundProcess.setTotalRecords(Long.valueOf(exportFile.getTotalRecord()));
                            backgroundProcess.setProgress(100);
                            backgroundProcess.setEndDate(exportFile.getEndDate());
                            backgroundProcess.setStatus(Constants.COMPLETED);
                            BackgroundProgressStatus.getInstance().setMapStatus(String.valueOf(exportFile.getId()), backgroundProcess.getStatus(),backgroundProcess.getProgress(), backgroundProcess,"export",currentUser.getId(),backgroundProcess.getTotalRecords().intValue(),backgroundProcess.getTotalRecords().intValue(),backgroundProcess.getIniDate());
                            backgroundProcess.setTotalAffectedRecords(exportFile.getSuccessRecord());
                            if (backgroundProcess.getEndDate().getTime()-backgroundProcess.getIniDate().getTime() == 0){
                                backgroundProcess.setProcessTime(endTime - startTime);
                            }else {
                                backgroundProcess.setProcessTime(backgroundProcess.getEndDate().getTime() - backgroundProcess.getIniDate().getTime());
                            } BackgroundProcessService.getInstance().update(backgroundProcess);
                            BackgroundProgressStatus.getInstance().mapStatus.remove(temporalName);
                        }
                    };
                    break;
                case THING_TYPE:
                    ih = new ExportHandler() {
                        @Override
                        public String[] headers() {
                            List<ThingType> thingTypes = ThingTypeService.getInstance().listPaginated(null, null);
                            //get static column names and then the variable column names
                            List<String> headers = new ArrayList<>();
                            headers.add("Name");
                            headers.add("Owner Group");
                            headers.add("Code");
                            headers.add("isParent");
                            headers.add("Auto Created");

                            //add parent header if thing type has parents
                            Set<Long> ttIds = currentUser.getThingTypeResources();
                            for (ThingType thingType : thingTypes) {
                                if (thingType.getParents() != null
                                    && thingType.getParents().size() > 0
                                    && ttIds.contains(thingType.getId())) {
                                    headers.add("Parent");
                                    break;
                                }
                            }
                            headers.add("Thing Type Template");
                            return headers.toArray(new String[headers.size()]);
                        }

                        @Override
                        public void process(CSVPrinter p) throws IOException {
                            ImportExport exportFile = ImportExportService.getInstance().get(id);
                            if (exportFile == null) {
                                throw new UserException(String.format("ExportId[%d] not found.", id));
                            }
                            String temporalName = exportFile.getId() + "export";
                            exportFile.setUserId(currentUser.getId());
                            exportFile.setType(typeName);
                            exportFile.setProcessType("Export");
                            Long startTime = System.currentTimeMillis();
                            Long dateTmp = (new Date()).getTime()/1000;
                            exportFile.setStartDate(new Date(dateTmp*1000));
                            BackgroundProcess backgroundProcess = new BackgroundProcess();
                            backgroundProcess.setIniDate(exportFile.getStartDate());
                            backgroundProcess.setCreatedByUser(currentUser);
                            backgroundProcess.setStatus(Constants.ADDED);
                            backgroundProcess.setTypeProcess("export");
                            backgroundProcess.setProgress(0);
                            String nameThread = FileExportService.class.getName() + "-RD-" + exportFile.getId();
                            backgroundProcess.setThreadName(nameThread);
                            ThingTypeController thingTypeController = new ThingTypeController();
                            BooleanBuilder be = new BooleanBuilder();
                            be = be.and(GeneralVisibilityUtils.limitVisibilitySelectAll(thingTypeController.getEntityVisibility(), QThingType.thingType, VisibilityUtils.getVisibilityGroup(ThingType.class.getCanonicalName(), null), "false", ""));
                            be = be.and( QueryUtils.buildSearch( QThingType.thingType, ""));
                            Set<Long> ttIds = currentUser.getThingTypeResources();
                            if (ttIds.size() > 0) {
                                be = be.and(QThingType.thingType.id.in(ttIds));
                            }
                            List<ThingType> thingTypes = ThingTypeService.getInstance().listPaginated(be,null, null);
                            backgroundProcess.setTotalRecords(Long.valueOf(thingTypes.size()));
                            backgroundProcess.setTotalOmittedRecords(0L);
                            BackgroundProgressStatus.getInstance().setMapStatus(String.valueOf(exportFile.getId()), "ADDED",0, backgroundProcess,"export",currentUser.getId(),0,0,backgroundProcess.getIniDate());
                            double updateBackground = ConfigurationService.getAsInteger(currentUser, "background_percentUpdate").doubleValue();
                            backgroundProcess = BackgroundProcessService.getInstance().insert(backgroundProcess);
                            BackgroundProcessEntity backgroundProcessEntity = new BackgroundProcessEntity();
                            backgroundProcessEntity.setBackgroundProcess(backgroundProcess);
                            backgroundProcessEntity.setColumnValue(exportFile.getId().toString());
                            backgroundProcessEntity.setModuleName("export");
                            backgroundProcessEntity.setColumnName("exportId");
                            BackgroundProcessEntityService.getInstance().insert(backgroundProcessEntity);
                            double div = 1.0;
                            double delta = Double.valueOf(thingTypes.size()) * (updateBackground/100.0);
                            int progressRatio = (int) Math.rint(delta);
                            if (progressRatio == 0) {
                                div = progressRatio;
                                progressRatio = 1;
                            }
                            int progressValue = progressRatio;

                            // validate if the report has the parent column
                            boolean hasParentColumn = false;
                            for (ThingType thingType : thingTypes) {
                                if (thingType.getParents() != null && thingType.getParents().size() > 0) {
                                    hasParentColumn = true;
                                    break;
                                }
                            }

                            for (int i = 0; i < thingTypes.size(); i++) {
                                ThingType thingType = thingTypes.get(i);
                                List<String> values = new ArrayList<>();
                                values.add(thingType.getName());
                                values.add(thingType.getGroup().getHierarchyName(thingType.getGroup().getName().equals("root")));
                                values.add(thingType.getThingTypeCode());
                                values.add(String.valueOf(thingType.isIsParent()));
                                values.add(String.valueOf(thingType.isAutoCreate()));

                                //add parent column if thing type has parents
                                if (hasParentColumn) {
                                    if (thingType.getParents() != null && thingType.getParents().size() > 0) {
                                        StringBuilder parentSb = new StringBuilder();
                                        for (ThingType parent : thingType.getParents()) {
                                            parentSb.append(parent.getThingTypeCode());
                                            parentSb.append(",");
                                        }
                                        values.add(parentSb.toString().substring(0, parentSb.toString().length() - 1));
                                    } else {
                                        values.add("");
                                    }
                                }
                                if (null != thingType.getThingTypeTemplate()) {
                                    values.add(thingType.getThingTypeTemplate().getName());
                                } else {
                                    values.add("");
                                }
                                p.printRecord(values);

                                if (progressValue == i) {
                                    backgroundProcess.setStatus(Constants.IN_PROGRESS);
                                    backgroundProcess.setProgress((int) (((progressValue / progressRatio)*updateBackground)/div));
                                    progressValue = progressValue + progressRatio;
                                    backgroundProcess = BackgroundProcessService.getInstance().update(backgroundProcess);
                                    BackgroundProgressStatus.getInstance().setMapStatus(String.valueOf(exportFile.getId()), backgroundProcess.getStatus(),backgroundProcess.getProgress(), backgroundProcess,"export", currentUser.getId(),backgroundProcess.getTotalRecords().intValue(),i,backgroundProcess.getIniDate());
                                }
                            }
                            Long endTime = System.currentTimeMillis();
                            Long dateTmpEnd = (new Date()).getTime()/1000;
                            exportFile.setEndDate(new Date(dateTmpEnd*1000));
                            exportFile.setTotalRecord((long) thingTypes.size());
                            exportFile.setSuccessRecord((long) thingTypes.size());
                            ImportExportService.getInstance().update(exportFile);
                            backgroundProcess.setProgress(100);
                            backgroundProcess.setEndDate(exportFile.getEndDate());
                            backgroundProcess.setStatus(Constants.COMPLETED);
                            BackgroundProgressStatus.getInstance().setMapStatus(String.valueOf(exportFile.getId()), backgroundProcess.getStatus(),backgroundProcess.getProgress(), backgroundProcess,"export",currentUser.getId(),backgroundProcess.getTotalRecords().intValue(),backgroundProcess.getTotalRecords().intValue(),backgroundProcess.getIniDate());
                            backgroundProcess.setTotalAffectedRecords(exportFile.getSuccessRecord());
                            if (backgroundProcess.getEndDate().getTime()-backgroundProcess.getIniDate().getTime() == 0){
                                backgroundProcess.setProcessTime(endTime - startTime);
                            }else {
                                backgroundProcess.setProcessTime(backgroundProcess.getEndDate().getTime() - backgroundProcess.getIniDate().getTime());
                            }
                            BackgroundProcessService.getInstance().update(backgroundProcess);
                            BackgroundProgressStatus.getInstance().mapStatus.remove(temporalName);
                        }
                    };
                    break;
                case THING_TYPE_PROPERTY:
                    ih = new ExportHandler() {
                        @Override
                        public String[] headers() {
                            return new String[]{"Thing Type Code", "Thing Type Property", "Unit",
                                    "Type", "Type Thing Type Code", "Symbol", "Time Series", "Default Value"};
                        }

                        @Override
                        public void process(CSVPrinter p) throws IOException {

                            ImportExport exportFile = ImportExportService.getInstance().get(id);
                            if (exportFile == null) {
                                throw new UserException(String.format("ExportId[%d] not found.", id));
                            }
                            DateFormatAndTimeZone dateFormatAndTimeZone = UserService.getInstance().getDateFormatAndTimeZone(currentUser);
                            logger.info("USER [" + currentUser.getUsername() + "] REGIONAL SETTING " + dateFormatAndTimeZone);
                            String temporalName = exportFile.getId() + "export";

                            exportFile.setUserId(currentUser.getId());
                            exportFile.setType(typeName);
                            exportFile.setProcessType("Export");
                            Long startTime = System.currentTimeMillis();
                            Long dateTmp = (new Date()).getTime()/1000;
                            exportFile.setStartDate(new Date(dateTmp*1000));

                            BackgroundProcess backgroundProcess = new BackgroundProcess();
                            backgroundProcess.setIniDate(exportFile.getStartDate());
                            backgroundProcess.setCreatedByUser(currentUser);
                            backgroundProcess.setStatus(Constants.ADDED);
                            backgroundProcess.setTypeProcess("export");
                            backgroundProcess.setProgress(0);
                            String nameThread = FileExportService.class.getName() + "-RD-" + exportFile.getId();
                            backgroundProcess.setThreadName(nameThread);
                            BackgroundProgressStatus.getInstance().setMapStatus(String.valueOf(exportFile.getId()), "ADDED",0, backgroundProcess,"export",currentUser.getId(),0,0,backgroundProcess.getIniDate());
                            double updateBackground = ConfigurationService.getAsInteger(currentUser, "background_percentUpdate").doubleValue();
                            backgroundProcess = BackgroundProcessService.getInstance().insert(backgroundProcess);
                            BackgroundProcessEntity backgroundProcessEntity = new BackgroundProcessEntity();
                            backgroundProcessEntity.setBackgroundProcess(backgroundProcess);
                            backgroundProcessEntity.setColumnValue(exportFile.getId().toString());
                            backgroundProcessEntity.setModuleName("export");
                            backgroundProcessEntity.setColumnName("exportId");
                            BackgroundProcessEntityService.getInstance().insert(backgroundProcessEntity);
                            ThingTypeController thingTypeController = new ThingTypeController();
                            BooleanBuilder be = new BooleanBuilder();
                            be = be.and(GeneralVisibilityUtils.limitVisibilitySelectAll(thingTypeController.getEntityVisibility(), QThingType.thingType, VisibilityUtils.getVisibilityGroup(ThingType.class.getCanonicalName(),null), "false", ""));
                            be = be.and( QueryUtils.buildSearch( QThingType.thingType, ""));
                            Set<Long> ttIds = currentUser.getThingTypeResources();
                            if (ttIds.size() > 0) {
                                be = be.and(QThingType.thingType.id.in(ttIds));
                            }
                            List<ThingType> thingTypes = ThingTypeService.getInstance().listPaginated(be,null, null);
                            BooleanBuilder beAnd = new BooleanBuilder();
                            if (thingTypes.size() > 0) {
                                beAnd = beAnd.and(QThingTypeField.thingTypeField.thingType.in(thingTypes));
                            }
                            List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance().listPaginated(beAnd, null,null);
                            int totalFields = thingTypeFields.size();
                            double div = 1.0;
                            double delta = Double.valueOf(totalFields) * (updateBackground/100.0);
                            int progressRatio = (int) Math.rint(delta);
                            if (progressRatio == 0) {
                                div = progressRatio;
                                progressRatio = 1;
                            }
                            int progressValue = progressRatio;
                            int i = 0;

                            for (ThingType thingType : thingTypes) {
                                for (ThingTypeField thingTypeField : thingType.getThingTypeFields()) {
                                    List<Object> values = new ArrayList<>();
                                    values.add(thingType.getThingTypeCode());
                                    values.add(thingTypeField.getName());
                                    values.add(thingTypeField.getUnit());
                                    values.add(thingTypeField.getDataType().getId());
                                    // set Data Type Thing Type
                                    String dataTypeThingTypeCode = null;
                                    if (null != thingTypeField.getDataTypeThingTypeId()) {
                                        ThingType dataTypeThingType = cache.getThingTypeById(thingTypeField.getDataTypeThingTypeId());
                                        if (null != dataTypeThingType)
                                            dataTypeThingTypeCode = dataTypeThingType.getThingTypeCode();
                                    }
                                    values.add(dataTypeThingTypeCode);
                                    values.add(thingTypeField.getSymbol());
                                    values.add(thingTypeField.getTimeSeries());
                                    String defaultValue = thingTypeField.getDefaultValue();
                                    //Apply ISO date format to thing type's default value
                                    Long defaultValueLong;
                                    if (Utilities.isNumber(defaultValue) && (ThingTypeFieldService.isDateTimeStampType(thingTypeField.getDataType()))){
                                        defaultValueLong = Long.parseLong(defaultValue);
                                        defaultValue = dateFormatAndTimeZone.getISODateTimeFormatWithoutTimeZone(defaultValueLong);
                                    }
                                    values.add(defaultValue);
                                    p.printRecord(values);
                                    if (progressValue == i) {
                                        backgroundProcess.setStatus(Constants.IN_PROGRESS);
                                        backgroundProcess.setProgress((int) (((progressValue / progressRatio)*updateBackground)/div));
                                        progressValue = progressValue + progressRatio;
                                        backgroundProcess = BackgroundProcessService.getInstance().update(backgroundProcess);
                                        BackgroundProgressStatus.getInstance().setMapStatus(String.valueOf(exportFile.getId()), backgroundProcess.getStatus(),backgroundProcess.getProgress(), backgroundProcess,"export",currentUser.getId(),totalFields,i,backgroundProcess.getIniDate());
                                    }
                                    i++;
                                }
                            }
                            exportFile.setTotalRecord(Long.valueOf(totalFields));
                            exportFile.setSuccessRecord(Long.valueOf(totalFields));
                            Long endTime = System.currentTimeMillis();
                            Long dateTmpEnd = (new Date()).getTime()/1000;
                            exportFile.setEndDate(new Date(dateTmpEnd*1000));
                            ImportExportService.getInstance().update(exportFile);
                            backgroundProcess.setTotalRecords(Long.valueOf(exportFile.getTotalRecord()));
                            backgroundProcess.setTotalOmittedRecords(0L);
                            backgroundProcess.setProgress(100);
                            backgroundProcess.setEndDate(exportFile.getEndDate());
                            backgroundProcess.setStatus(Constants.COMPLETED);
                            BackgroundProgressStatus.getInstance().setMapStatus(String.valueOf(exportFile.getId()), backgroundProcess.getStatus(),backgroundProcess.getProgress(), backgroundProcess,"export",currentUser.getId(),totalFields,totalFields,backgroundProcess.getIniDate());
                            backgroundProcess.setTotalAffectedRecords(exportFile.getSuccessRecord());
                            if (backgroundProcess.getEndDate().getTime()-backgroundProcess.getIniDate().getTime() == 0){
                                backgroundProcess.setProcessTime(endTime - startTime);
                            }else {
                                backgroundProcess.setProcessTime(backgroundProcess.getEndDate().getTime() - backgroundProcess.getIniDate().getTime());
                            }
                            BackgroundProcessService.getInstance().update(backgroundProcess);
                            BackgroundProgressStatus.getInstance().mapStatus.remove(temporalName);

                        }
                    };
                    break;
                case REPORT:
                    ih = new ExportHandler() {
                        @Override
                        public String[] headers() {
                            String headersArray[] = new String[headersExport.size()];
                            Object headersObj[] = headersExport.toArray();
                            for (int it = 0; it < headersArray.length; it++) {
                                headersArray[it] = headersObj[it].toString();
                            }
                            return headersArray;
                        }

                        @Override
                        public void process(CSVPrinter p) throws IOException {
                            for (List<String> list : valuesExport) {
                                List<Object> values = new ArrayList<>();
                                for (String value : list) {
                                    values.add(value);
                                }
                                p.printRecord(values);
                            }

                        }
                    };
                    break;
                case USER:

                    if (!PermissionsUtils.buildSearch(SecurityUtils.getSubject(), new HashMap<String, Boolean>(), "user:r")) {
                        throw new UserException("Permissions error: User does not have access to read users.");
                    }
                    ih = new ExportHandler() {
                        @Override
                        public String[] headers() {
                            return new String[]{"Username", "Home Group", "Roaming Group", "Roles",
                                    "First name", "Last name", "Email", "Password","Time Zone","Date Format"};
                        }

                        @Override
                        public void process(CSVPrinter p) throws IOException {
                            ImportExport exportFile = ImportExportService.getInstance().get(id);
                            if (exportFile == null) {
                                throw new UserException(String.format("ExportId[%d] not found.", id));
                            }
                            String temporalName = exportFile.getId() + "export";
                            exportFile.setUserId(currentUser.getId());
                            exportFile.setType(typeName);
                            exportFile.setProcessType("Export");
                            Long startTime = System.currentTimeMillis();
                            Long dateTmp = (new Date()).getTime()/1000;
                            exportFile.setStartDate(new Date(dateTmp*1000));
                            BackgroundProcess backgroundProcess = new BackgroundProcess();
                            backgroundProcess.setIniDate(exportFile.getStartDate());
                            backgroundProcess.setCreatedByUser(currentUser);
                            backgroundProcess.setStatus(Constants.ADDED);
                            backgroundProcess.setTypeProcess("export");
                            backgroundProcess.setProgress(0);
                            String nameThread = FileExportService.class.getName() + "-RD-" + exportFile.getId();
                            backgroundProcess.setThreadName(nameThread);
                            BackgroundProgressStatus.getInstance().setMapStatus(String.valueOf(exportFile.getId()), "ADDED",0, backgroundProcess,"export",currentUser.getId(),0,0,backgroundProcess.getIniDate());
                            Boolean excludeRoot = false;
                            User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
                            String where = "group<" + currentUser.getActiveGroup().getId();
                            BooleanBuilder be = new BooleanBuilder();

                            exportFile.setUserId(currentUser.getId());
                            if (excludeRoot) {
                                be = be.and(QUser.user.username.ne("root"));
                            }
                            be = be.and(QueryUtils.buildSearch(QUser.user, where));
                            List<User> userList = UserService.getInstance().listPaginated(be, null, null);
                            backgroundProcess.setTotalRecords(Long.valueOf(userList.size()));
                            backgroundProcess.setTotalOmittedRecords(0L);
                            double updateBackground = ConfigurationService.getAsInteger(currentUser, "background_percentUpdate").doubleValue();
                            backgroundProcess = BackgroundProcessService.getInstance().insertBackgroundProcess(backgroundProcess, exportFile.getId(), "export", "exportId");
                            double delta = Double.valueOf(userList.size()) * (updateBackground/100.0);
                            double div = 1.0;
                            int progressRatio = (int) Math.rint(delta);
                            if (progressRatio == 0) {
                                div = progressRatio;
                                progressRatio = 1;
                            }
                            int progressValue = progressRatio;

                            for (int i = 0; i < userList.size(); i++) {
                                User user = userList.get(i);
                                List<Object> values = new ArrayList<>();
                                values.add(user.getUsername());
                                values.add(user.getGroup().getHierarchyName(false).replaceAll("\"", ""));
                                values.add((user.getRoamingGroup() != null) ? user.getRoamingGroup().getHierarchyName() : "");
                                List<String> roleList = new ArrayList<>();
                                for (UserRole userRole : user.getUserRoles()) {
                                    roleList.add(userRole.getRole().getName());
                                }
                                values.add(String.join(";", roleList));
                                values.add(user.getFirstName());
                                values.add(user.getLastName());
                                values.add(user.getEmail());
                                values.add(""); // Password is exported as an empty value
                                values.add(user.getTimeZone());
                                values.add(user.getDateFormat());
                                p.printRecord(values);
                                if (progressValue == i) {
                                    backgroundProcess.setStatus(Constants.IN_PROGRESS);
                                    backgroundProcess.setProgress((int) (((progressValue / progressRatio)*updateBackground)/div));
                                    progressValue = progressValue + progressRatio;
                                    backgroundProcess = BackgroundProcessService.getInstance().update(backgroundProcess);
                                    BackgroundProgressStatus.getInstance().setMapStatus(String.valueOf(exportFile.getId()), backgroundProcess.getStatus(),backgroundProcess.getProgress(), backgroundProcess,"export",currentUser.getId(),userList.size(),i,backgroundProcess.getIniDate());
                                }
                            }
                            Long endTime = System.currentTimeMillis();
                            Long dateTmpEnd = (new Date()).getTime()/1000;
                            exportFile.setEndDate(new Date(dateTmpEnd*1000));
                            exportFile.setTotalRecord((long) userList.size());
                            exportFile.setSuccessRecord((long) userList.size());
                            ImportExportService.getInstance().update(exportFile);
                            backgroundProcess.setProgress(100);
                            backgroundProcess.setEndDate(exportFile.getEndDate());
                            backgroundProcess.setStatus(Constants.COMPLETED);
                            BackgroundProgressStatus.getInstance().setMapStatus(String.valueOf(exportFile.getId()), backgroundProcess.getStatus(),backgroundProcess.getProgress(), backgroundProcess,"export",currentUser.getId(),userList.size(),userList.size(),backgroundProcess.getIniDate());
                            backgroundProcess.setTotalAffectedRecords(exportFile.getSuccessRecord());
                            if (backgroundProcess.getEndDate().getTime()-backgroundProcess.getIniDate().getTime() == 0){
                                backgroundProcess.setProcessTime(endTime - startTime);
                            }else {
                                backgroundProcess.setProcessTime(backgroundProcess.getEndDate().getTime() - backgroundProcess.getIniDate().getTime());
                            }  BackgroundProcessService.getInstance().update(backgroundProcess);
                            BackgroundProgressStatus.getInstance().mapStatus.remove(temporalName);
                        }
                    };
                    break;
            }

        logger.info("Handler is " + ih);
        return ih;
    }

    public static List<Long> getGroupIds(
            Map<Long, List<ThingType>> groups
            , String upVisibility
            , String downVisibility) {
        List<Long> lstGroupIds = new ArrayList<>();
        if ((upVisibility != null && upVisibility.equals("false")) &&
                (downVisibility != null && downVisibility.equals("false"))) {
            //Show only things of the group
            Iterator entries = groups.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry thisEntry = (Map.Entry) entries.next();
                Long key = (Long) thisEntry.getKey();
                lstGroupIds.add(key);
            }
        } else if ((upVisibility != null && upVisibility.equals("false")) &&
                (downVisibility != null && (downVisibility.equals("") || downVisibility.equals("true")))) {
            for (Long groupId : groups.keySet()) {
                //Get Children
                lstGroupIds = GroupService.getInstance().getListGroupIdsChildren(groupId);
            }

        } else if ((downVisibility != null && downVisibility.equals("false")) &&
                (upVisibility != null && (upVisibility.equals("") || upVisibility.equals("true")))) {
            Iterator entries = groups.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry thisEntry = (Map.Entry) entries.next();
                Long key = (Long) thisEntry.getKey();
                lstGroupIds.add(key);
            }
        } else {
            //By default, children are added to the list
            for (Long groupId : groups.keySet()) {
                //Get Children
                lstGroupIds = GroupService.getInstance().getListGroupIdsChildren(groupId);
            }
        }
        return lstGroupIds;
    }

}
