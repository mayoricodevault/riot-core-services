package com.tierconnect.riot.iot.popdb;

import com.tierconnect.riot.appcore.dao.CassandraUtils;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.GroupTypeService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.Charsets;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.hibernate.Transaction;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PopDBUpdate {
    private static final Logger logger = Logger.getLogger(PopDBRiot.class);

    private static String OS = System.getProperty("os.name").toLowerCase();

    public static void main(String args[]) throws Exception {



        PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
        CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra.keyspace"));
        PopDBUpdate popdb = new PopDBUpdate();

        Transaction transaction = GroupService.getGroupDAO().getSession().getTransaction();
        transaction.begin();
        PopDBIOTUtils.initShiroWithRoot();

        popdb.run();

        transaction.commit();

        System.out.println("CSV Dir = " + System.getProperty("files.dir"));

        System.exit(0);
    }

    public void run() {
        createData();
    }

    private void createData()  {
        String dir = System.getProperty("files.dir");



        if(!dir.endsWith("\\") && !dir.endsWith("/") )
            dir += isWindows()?"\\":"/";


        List<String> messages = new ArrayList<>();


        try {
            FileImportService fis = new FileImportService((User) SecurityUtils.getSubject().getPrincipal());

            //Thing type
            FileImportService.Type type = FileImportService.Type.valueOf("THING_TYPE");

            if (new File(dir + "thingTypes.csv").exists()) {
                InputStream inputStream = new FileInputStream(dir + "thingTypes.csv");
                String[] result = fis.parse(new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)), type,null, "thingTypes.csv");

                messages.add("Thing type imported :" + Arrays.toString(result));
            } else
                messages.add("File " + dir + "thingTypes.csv NOT FOUND");

            //Thing type properties
            type = FileImportService.Type.valueOf("THING_TYPE_PROPERTY");

            if (new File(dir + "thingTypeProperties.csv").exists()) {
                InputStream inputStream = new FileInputStream(dir + "thingTypeProperties.csv");
                String[] result = fis.parse(new BufferedReader(new InputStreamReader(inputStream,StandardCharsets.UTF_8)), type,null, "thingTypeProperties.csv");

                messages.add("Thing type properties imported :");
                messages.addAll(Arrays.asList(result));
            } else
                messages.add("File " + dir + "thingTypeProperties.csv NOT FOUND");

            //Thing

            ThingType thingType = ThingTypeService.getInstance().getByCode("Workstations");
            fis = new FileImportService((User) SecurityUtils.getSubject().getPrincipal(), thingType);
            type = FileImportService.Type.valueOf("THING");
            if (new File(dir + "things.csv").exists()) {
                InputStream inputStream = new FileInputStream(dir + "things.csv");
                String[] result = fis.parse(new BufferedReader(new InputStreamReader(inputStream, Charsets.UTF_8)), type,null, "things.csv");

                messages.add("Things imported :");
                messages.addAll(Arrays.asList(result));
            } else
                messages.add("File " + dir + "things.csv NOT FOUND");

            //Assign camera id to zone
            if (new File(dir + "cameraZoneNames.csv").exists()) {
                InputStream inputStream = new FileInputStream(dir + "cameraZoneNames.csv");

                CSVParser records = CSVFormat.EXCEL.withHeader().withSkipHeaderRecord().parse(new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)));
                int columnLength = records.getHeaderMap().size();
                for (CSVRecord record : records) {

                    if (record.size() == columnLength) {
                        String zoneName = record.get("Zone name");
                        List<String> cameras = new ArrayList<>();
                        for(int i = 1; i<= records.getHeaderMap().size(); i++){
                            try{
                                String cam = record.get("Camera " + i);
                                if (cam!=null && !cam.isEmpty())
                                    cameras.add(cam);
                            }catch(Exception e){break;}
                        }

                        List<Zone> zones = ZoneService.getZonesByName(zoneName);
                        if (zones.size() > 0) {
                            Zone zone = zones.get(0);
                            ZoneProperty zoneProperty = ZoneService.getInstance().getZonePropertyByName(zone, "camera");

                            if (zoneProperty != null) {
                                String cameraIds = "";


                                for(String cam : cameras)
                                    //(cameraIds.isEmpty()?"":",") adds comma at the beginning of each item except fisrt one
                                    cameraIds += (cameraIds.isEmpty()?"":",") + cam;

                                if (cameraIds.length()> 0) {
                                    PopDBIOTUtils.popZonePropertyValue(cameraIds, zone, zoneProperty.getId());
                                    messages.add("Zone  " + zoneName + " : added cameras " + cameraIds);
                                }else
                                    messages.add("Zone  " + zoneName + " : has no cameras ");
                            } else {
                                messages.add("ZoneProperty NOT FOUND : " + " camera");
                            }
                        } else {
                            messages.add("Zone NOT FOUND : " + zoneName);
                        }
                    } else {
                        messages.add("ERROR: Invalid column length. Expected " + columnLength +
                                " columns and got " + record.size() + ".");
                    }
                }
            } else
                messages.add("File " + dir + "cameraZoneNames.csv NOT FOUND");
            //Create Workstation Assignment Report

            List<ReportDefinition> rd = ReportDefinitionService.getInstance().getZonesByName("Workstation Assignment");

            if(rd.size() == 0){
                User rootUser = UserService.getInstance().getRootUser();
                Group rootGroup = GroupService.getInstance().getRootGroup();
                GroupType tenantGroupType = GroupTypeService.getInstance().getTenantGroupType();

                ReportDefinition reportDefinition = new ReportDefinition();
                reportDefinition.setName("Workstation Assignment");
                reportDefinition.setGroup(rootGroup);
                reportDefinition.setPinLabel("2");
                reportDefinition.setReportType("table");
                reportDefinition.setDefaultTypeIcon("pin");
                reportDefinition.setCreatedByUser(rootUser);
                reportDefinition.setEditInline(true);
                reportDefinition.setRunOnLoad(true);
                reportDefinition.setIsMobile(Boolean.FALSE);
                reportDefinition.setIsMobileDataEntry(Boolean.FALSE);
                reportDefinition = ReportDefinitionService.getInstance().insert(reportDefinition);

                String[] labels = {"Serial", "Monitor ID", "Operator"};
                String[] propertyNames2 = {"serial", "window_id", "operator"};
                String[] propertyOrders2 = {"1", "2", "3"};
                Boolean[] propertyEditable = {false, false, true};
                Long[] propertyTypeIds2 = {thingType.getId(), thingType.getId(), thingType.getId()};

                for (int it = 0; it < Array.getLength(labels); it++) {
                    ReportProperty reportProperty = createReportProperty(labels[it], propertyNames2[it],
                            propertyOrders2[it], propertyTypeIds2[it], propertyEditable[it],
                            reportDefinition);
                    ReportPropertyService.getInstance().insert(reportProperty);
                }

                String[] labelsFilter2 = {"", "Monitor ID", "Operator"};
                String[] propertyNamesFilter2 = {"thingType.id", "window_id", "operator"};
                String[] propertyOrdersFilter2 = {"1", "1", "1"};
                String[] operatorFilter2 = {"=", "=", "="};
                String[] value2 = {thingType.getId().toString(),"", ""};
                Long[] propertyIds = {null, thingType.getThingTypeFieldByName("window_id").getId(),
                        thingType.getThingTypeFieldByName("operator").getId()};
                Long[] thingTypeIds2 = {null, thingType.getId(), thingType.getId()};

                Boolean[] isEditable2 = {false, true, true};


                for (int it = 0; it < Array.getLength(labelsFilter2); it++) {
                    ReportFilter reportFilter = createReportFilter(labelsFilter2[it],
                            propertyNamesFilter2[it], propertyOrdersFilter2[it],
                            isEditable2[it], propertyIds[it],thingTypeIds2[it], operatorFilter2[it], value2[it], reportDefinition);
                    ReportFilterService.getInstance().insert(reportFilter);
                }

                messages.add("Report  " + reportDefinition.getName() +
                        " aded");
            }else
                messages.add("Report  Workstation Assignment already exists");


        } catch (Exception e) {
            e.printStackTrace();
            messages.add("ERROR :" + e.getMessage());
        }finally {
            System.out.println("\n\n\n******************** ******************** ********************");
            System.out.println("******************** POPDB UPDATE SUMMARY ********************");
            System.out.println("******************** ******************** ********************\n\n\n");
            for (String message : messages) {
                System.out.println(message);
            }
        }

    }

    private ReportProperty createReportProperty(String label, String propertyName, String propertyOrder,
                                                Long propertyTypeId, boolean isEditable,
                                                ReportDefinition reportDefinition) {
        ReportProperty reportProperty = new ReportProperty();
        reportProperty.setLabel(label);
        reportProperty.setPropertyName(propertyName);
        reportProperty.setDisplayOrder(Float.parseFloat(propertyOrder));
        reportProperty.setThingType( ThingTypeService.getInstance().get( propertyTypeId ));
        reportProperty.setThingTypeField( ThingTypeFieldService.getInstance().getThingTypeFieldByName( propertyName ).get( 0 ) );
        reportProperty.setEditInline(isEditable);
        reportProperty.setReportDefinition(reportDefinition);
        return reportProperty;
    }

    private ReportFilter createReportFilter(String label, String propertyName, String propertyOrder,
                                            Boolean isEditable,Long propertyId,Long thingTypeId,
                                            String operatorFilter, String value,
                                            ReportDefinition reportDefinition) {
        ReportFilter reportFilter = new ReportFilter();
        reportFilter.setLabel(label);
        reportFilter.setPropertyName(propertyName);
        reportFilter.setDisplayOrder(Float.parseFloat(propertyOrder));
        reportFilter.setOperator(operatorFilter);
        reportFilter.setValue(value);
        reportFilter.setEditable(isEditable);
        reportFilter.setReportDefinition(reportDefinition);
        reportFilter.setThingType( ThingTypeService.getInstance().get(thingTypeId ) );
        reportFilter.setThingTypeField( ThingTypeFieldService.getInstance().get( propertyId ) );
        return reportFilter;
    }

    public static boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

}
