package com.tierconnect.riot.iot.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.dao.*;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.entities.exceptions.MLModelException;
import com.tierconnect.riot.appcore.utils.MlConfiguration;
import com.tierconnect.riot.iot.utils.rest.RestCallException;
import com.tierconnect.riot.iot.utils.rest.RestClient;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.time.LocalDate;
import java.util.*;

/**
 * This class provides all the application services of the analytics module.
 * The services can be grouped in the following categories:
 * <p>
 * - business models services
 * - extractions services
 * - trainings services
 * - predictions services
 * <p>
 * All of them provide services to create and retrieve the respective entities
 * (i.e. business models, extractions, trainings and predictions). It also provides
 * some other related services like upload a jar file.
 *
 * @author Pablo Caballero
 * @author Alfredo Villalba
 * @author Ariel Iporre
 */
public class MlModelService {

    private static Logger logger = Logger.getLogger(MlModelService.class);

    private static final String HOST_SPARK_JOB_SERVER = MlConfiguration.property("spark.host");
    private static final String PORT_SPARK_JOB_SERVER = MlConfiguration.property("spark.port");


    //=========================================================================
    //
    //            B U S I N E S S   M O D E L S   S E R V I C E S
    //
    //=========================================================================

    /**
     * Creates a new business model. It requires the related JAR file (the model itself) to be previously
     * uploaded to the riot-core-services (see method {@link #uploadJarToCoreServices(String, InputStream)}).
     * To achieve its goal, this method performs two tasks. It first uploads to the Spark Job Server
     * the JAR file that was previously uploaded to riot-core-service. Once this is done, this method
     * inserts the new business model into the database. In order to successfully create a business model,
     * both the uploading of the JAR to the Spark Job Server and the insertion into the database must succeed.
     *
     * @param name  name of the business model
     * @param desc  description of the business model
     * @param jarId indentifier of the previously uploaded JAR
     * @return the ID of the created business model
     * @throws MLModelException unable to upload or integrate JAR file
     */
    public Long createBusinessModel(String name, String desc, String jarId) throws MLModelException {

        TemporalJar tempJar = TemporalJar.getTemporalJarById(jarId);
        //todo: refactor change upload spark job server
        //tempJar.uploadToSparkJobServer();
        MlBusinessModel.Builder builder = new MlBusinessModel.Builder(name, desc, tempJar.appName(), tempJar.jarOriginalFileName());
        for (Map<String, Object> p : tempJar.modelInputs()) {
            builder.predictor((String) p.get("name"), (String) p.get("type"));
        }
        MlBusinessModel bModel = builder.build();
        new MlBusinessModelDAO().insert(bModel);
        return bModel.getId();
    }


    /**
     * Provides the service to upload a JAR file to riot-core-services.
     *
     * @param jarFileName    JAR file name
     * @param jarInputStream input stream containing the JAR file
     * @return a unique identifier of the uploaded JAR file
     * @throws MLModelException unable to write JAR file into disk
     */
    // TODO: original jar filename jarFileName parameter is probably not necessarily at all
    public String uploadJarToCoreServices(String jarFileName, InputStream jarInputStream) throws MLModelException {
        TemporalJar tempJar = TemporalJar.createTemporalJarFromStream(jarFileName, jarInputStream);
        return tempJar.id();
    }


    /**
     * Returns a list of business models.
     *
     * @param groupId
     * @return
     */
    public List<MlBusinessModel> businessModels(Long groupId) {

        List<MlBusinessModel> businessModels = new ArrayList<>();

        //filter group
        if (groupId != null) {
            //todo should see child right?
            Group group = GroupService.getInstance().get(groupId);

            Map<String, Object> map = new HashMap<>();
            map.put("group", group);

            List<MlBusinessModelTenant> modelTenants = new MlBusinessModelTenantDAO().selectAllBy(map);
            for (MlBusinessModelTenant businessModelTenant : modelTenants) {
                businessModels.add(businessModelTenant.getBusinessModel());
            }
        }
        // no filter group
        else {
            businessModels = new MlBusinessModelDAO().selectAll();
        }

        return businessModels;
    }


    /**
     * A temporal jar is an uploaded jar to core services through the method {@link #uploadJarToCoreServices(String, InputStream)}.
     * It is temporal because it is supposed to be stored for a short period of time. In fact, it should be removed
     * after having created a business model with this jar, or after some while of having created any business model with
     * it.
     * <p>
     * TODO: implement mechanisms to actually remove temporal jars - for now they are persistent jars
     */
    private static class TemporalJar {

        private String id;
        private String tempJarFullFileName;
        private String metadataFullFileName;
        private Map<String, Object> metadata;
        private String appName;
        private String jarOriginalFileName; // TODO: this attribute should be removed? here for compatibility with the DB which contains a jar column for this, but it's never used


        private TemporalJar(String id) {
            this.id = id;
            tempJarFullFileName = MlConfiguration.property("jars.path") + "/" + id.toString() + ".jar";
            metadataFullFileName = MlConfiguration.property("jars.path") + "/" + id.toString() + ".json";
            appName = "app-" + id;
        }


        /**
         * Creates a temporal jar from an input stream which should contain the original jar file. Normally,
         * this input stream will be received through an endpoint, but it could come from somewhere else.
         *
         * @param jarOriginalFileName filename of the original jar
         * @param jarStream           input stream containing the jar file
         * @return a temporal jar
         * @throws MLModelException
         */
        public static TemporalJar createTemporalJarFromStream(String jarOriginalFileName, InputStream jarStream) throws MLModelException {

            try {

                TemporalJar tempJar = new TemporalJar(UUID.randomUUID().toString());
                tempJar.jarOriginalFileName = jarOriginalFileName; // TODO: remove this? see comment on attribute declaration
                FileOutputStream fop = new FileOutputStream(new File(tempJar.tempJarFullFileName));
                fop.write(IOUtils.toByteArray(jarStream));
                fop.flush();
                fop.close();
                tempJar.generateMetadata();
                return tempJar;

            } catch (IOException e) {
                throw new MLModelException("Unable to write jar file into the disk", e);
            }
        }

        /**
         * Returns a temporal jar that has been created previously, assuming the temporal
         * jar still exists. A temporal jar exits if the jar and its metadata are still
         * stored in disk.
         *
         * @param id ID of the temporal jar
         * @return the temporal jar
         * @throws MLModelException
         */
        public static TemporalJar getTemporalJarById(String id) throws MLModelException {
            TemporalJar tempJar = new TemporalJar(id);
            tempJar.readMetadata();
            return tempJar;
        }


        /**
         * Uploads a temporal jar to the spark job server.
         */
        public void uploadToSparkJobServer() throws MLModelException {

            try {
                RestClient client = RestClient.instance();
                Response rh = new Response();
                logger.info("uploading JAR to SJS...");
                client.post(
                        new URI("http://" + HOST_SPARK_JOB_SERVER + ":" + PORT_SPARK_JOB_SERVER + "/jars/" + appName),
                        tempJarFullFileName,
                        rh
                );
                logger.info("response from JSJ: " + rh.getResponse());
                logger.info("upload JAR to SJS finished");

            } catch (RestCallException | URISyntaxException e) {
                throw new MLModelException("Unable to upload jar to spark job server", e);
            }
        }


        public String id() {
            return id;
        }

        public List<Map<String, Object>> modelInputs() {return (List<Map<String, Object>>) metadata.get("modelInputs"); }

        public String appName() {
            return appName;
        }

        // TODO: removed this method? see comment on jarOriginalFileName attribute
        public String jarOriginalFileName() {
            return (String) metadata.get("jarOriginalFileName");
        }


        /**
         * Generates the metadata of this temporal jar. The metadata is extracted from the jar itself by
         * instantiating a precise class and invoking a precise method. If the jar does not contain such
         * a class or method, it will throw an exception, failing in this way the integration of the jar.
         * In other words, the jar must be compatible in order to be used as a temporal jar, which will be
         * used to create a businnes model.
         *
         * @throws MLModelException unable to integrate jar file or unable to write metadata into the disk
         */
        private void generateMetadata() throws MLModelException {

            try {

                // Load and instantiate class ModelConfiguration
                URL url = new File(tempJarFullFileName).toURI().toURL();
                URLClassLoader classLoader = new URLClassLoader(new URL[]{url}, this.getClass().getClassLoader());
                Class classModelConf = Class.forName("com.tierconnect.riot.ml.config.ModelConfiguration", true, classLoader);
                Object objModelConf = classModelConf.newInstance();

                // Get model inputs with the help of the ModelConfiguration class
                List<Map<String, Object>> modelInputsForMetadata = new ArrayList<>();
                Map<String, Map<String, Object>> modelInputs;
                modelInputs = (Map<String, Map<String, Object>>) classModelConf.getDeclaredMethod("extractionFields").invoke(objModelConf);
                for (String inputName : modelInputs.keySet()) {
                    Map<String, Object> modelInput = modelInputs.get(inputName);
                    modelInput.put("name", inputName);
                    modelInputsForMetadata.add(modelInput);
                    logger.info("model input:" + inputName + " type: " + modelInputs.get(inputName));
                }

                // Create and save metadata
                metadata = new HashMap<>();
                metadata.put("modelInputs", modelInputsForMetadata);
                metadata.put("jarOriginalFileName", jarOriginalFileName); // TODO: remove this? see comment of attribute declaration
                new ObjectMapper().writeValue(new File(metadataFullFileName), metadata);

            } catch (MalformedURLException | InstantiationException | InvocationTargetException |
                    NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
                throw new MLModelException("Unable to integrate jar file", e);

            } catch (IOException e) {
                throw new MLModelException("Unable to write jar metadata into the disk", e);
            }
        }


        /**
         * Reads the metadata linked to this temporal jar from the disk.
         *
         * @throws MLModelException
         */
        private void readMetadata() throws MLModelException {
            try {
                metadata = new ObjectMapper().readValue(
                        new File(metadataFullFileName),
                        new TypeReference<Map<String, Object>>() {
                        });
            } catch (IOException e) {
                throw new MLModelException("Unable to read jar metadata from the disk", e);
            }
        }


    }


    public static class Response implements RestClient.ResponseHandler {
        private String response;

        @Override
        public void success(InputStream is) throws IOException, RestCallException {
            response = IOUtils.toString(is);
            // todo handle something else here?
        }

        @Override
        public void error(InputStream is) throws IOException, RestCallException {
            response = IOUtils.toString(is);
            // todo handle something else here?
            return;
        }

        public String getResponse() {
            return response;
        }

    }


    //=========================================================================
    //
    //                E X T R A C T I O N S   S E R V I C E S
    //
    //=========================================================================


    /**
     * Extracts features from the database using a business model.
     *
     * @param businessModelId business model
     * @param groupId         tenant id
     * @param name            name of the model - needs to be unique
     * @param start           start date of data extraction
     * @param end             end date of data extraction
     * @param predictors      mapping of the predictors to the fields on the database
     * @return id of model created
     * @throws MLModelException
     */
    public Long extract(
            Long businessModelId, Long groupId, String name, String comments,
            LocalDate start, LocalDate end, List<Predictor> predictors) throws MLModelException {

        logger.info("Extraction action:  (" + businessModelId + "," + groupId + "," + name + "," +
                comments + "," + start + "," + end +  "," +  predictors.toString() + ")");

        MlBusinessModel mlBusinessModel = new MlBusinessModelDAO().selectById(businessModelId);
        Group group = GroupService.getInstance().get(groupId);
        List<MlExtractionPredictor> mlPredictors = Predictor.createSeveral(predictors);

        MlExtraction extraction = mlBusinessModel.extraction(group, start, end, name, comments, mlPredictors);
//        extraction.start(new JobServerService());
        logger.info("=======================>>>> Start extraction!!!!!!!!!!!");
        extraction.start(new HiddenServerService());
        new MlExtractionDAO().insert(extraction);

        return extraction.getId();
    }


    /**
     * Returns a certain number of rows from an extraction (the data itself). Each row is returned
     * as an instance of DataRow (see {@link DataRow}). It is important to notice that each row has an identifier
     * besides the values for the respective extracted features.
     *
     * @param extractionId identifier of the extraction
     * @param offsetRow    starting row
     * @param numRows      number of rows
     * @return list of rows
     * @throws MLModelException
     */
    public List<DataRow> getDataRowsExtraction(Long extractionId, Long offsetRow, Long numRows)
            throws MLModelException {

        try {
            List<DataRow> res = new ArrayList<>();

            String uuid = new MlExtractionDAO().selectById(extractionId).getUuid();
            String file = MlConfiguration.property("extractions.path") + "/" + uuid + ".csv";
            BufferedReader fin = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));  // todo define buffer size properly
            List<String> headers = Arrays.asList(fin.readLine().split(","));

            int c = -1;
            String line;
            while (((line = fin.readLine()) != null) && (c < (offsetRow + numRows - 1))) {
                c++;
                if ((c >= offsetRow) && (c <= (offsetRow + numRows - 1))) {
                    res.add(new DataRow((long) c + 1, headers, Arrays.asList(line.split(","))));
                }
            }

            fin.close();
            return res;
        } catch (IOException e) {
            throw new MLModelException(e);
        }

    }


    /**
     * Returns a list of extractions.
     *
     * @param groupId         group ID to which extractions belongs
     * @param bussinesModelId businnes model ID that was used to perform extractions
     * @param completed       finished or not finished extractions
     * @return
     */
    public List<MlExtraction> extractions(Long groupId, Long bussinesModelId, Boolean completed) {

        Map<String, Object> map;
        map = new HashMap<>();

        // Filter by group
        if (groupId != null) {
            //todo should see child right?
            Group group = GroupService.getInstance().get(groupId);
            map.put("group", group);
        }

        // Filter by business model
        if (bussinesModelId != null) {
            MlBusinessModel mlBusinessModel = new MlBusinessModelDAO().selectById(bussinesModelId);
            map.put("businessModel", mlBusinessModel);
        }

        // Filter by status
        if (completed != null && completed) {
            map.put("status", "FINISHED");
        }

        return new MlExtractionDAO().selectAllBy(map);
    }


    //=========================================================================
    //
    //                   T R A I N I N G   S E R V I C E S
    //
    //=========================================================================

    /**
     * Trains a model.
     *
     * @param groupId      group ID
     * @param extractionId extraction ID
     * @param name         name of the trained model
     * @param comments     comments of the trained model
     * @return training ID
     * @throws MLModelException
     */
    public Long train(
            Long groupId,
            Long extractionId,
            String name,
            String comments) throws MLModelException {

        Group group = GroupService.getInstance().get(groupId);
        MlExtraction extraction = new MlExtractionDAO().selectById(extractionId);
        MlAlgorithm algorithm = new MlAlgorithm(extraction);

        MlModel model = algorithm.train(new HiddenServerService(), name, comments, group);
        new MlModelDAO().insert(model);
        return model.getId();
    }


    /**
     * Returns a list of (trained) models.
     *
     * @param groupId
     * @param businessModelId
     * @param completed
     * @return
     */
    public List<MlModel> models(Long groupId, Long businessModelId, Boolean completed) {
        Map<String, Object> map;
        map = new HashMap<>();

        // Filter by group
        if (groupId != null) {
            //todo should see child right?
            Group group = GroupService.getInstance().get(groupId);
            map.put("group", group);
        }

        // Filter by business model
        if (businessModelId != null) {
            MlBusinessModel mlBusinessModel = new MlBusinessModelDAO().selectById(businessModelId);
            map.put("businessModel", mlBusinessModel);
        }

        // Filter by status
        if (completed != null && completed) {
            map.put("status", "FINISHED");
        }

        return new MlModelDAO().selectAllBy(map);
    }


    //=========================================================================
    //
    //                P R E D I C T I O N S   S E R V I C E S
    //
    //=========================================================================


    /**
     * Performs predictions using a (trained) model.
     * <p>
     * The parameters that the model requires (params) must be provided as a map containing
     * the following entries:
     * <p>
     * - startDate, endDate  OR year, month, weekOfTheMonth, dayOfTheMonth, dayOfTheWeek
     * - predictors having the format [pname1|pvalue11;pvalue12...,pname2|pvalue21;pvalue22;....,....,pnamek|pvaluek1;pvaluek2;...]
     * <p>
     * where pnamei is the name of the i-ieme predictor, and pvalueij is the j-ieme value for predictor pnamei
     *
     * @param params see previous description of the method
     * @return prediction in map format
     */
    public Map<String, Object> predict(Long modelId, Map<String, String> params) throws MLModelException {

        MlModel model = new MlModelDAO().selectById(modelId);
        Map<String, Object> response = model.predict(params, new HiddenServerService());
        return response;
    }


    /**
     * Performs predictions and stores the result of predictions in a persistent way.
     *
     * @param params
     * @return
     * @throws MLModelException
     */
    public Long createPersistentPrediction(Map<String, String> params) throws MLModelException {

        try {

            // Perform prediction (TODO: we could improve this by keeping stored the non persistent prediction)
            MlModel model = new MlModelDAO().selectById(Long.parseLong(params.get("modelId")));
            Map<String, Object> response = model.predict(params, new HiddenServerService());

            // Save prediction data into a file
            String uuid = UUID.randomUUID().toString();
            String filename = MlConfiguration.property("predictions.path") + "/" + uuid + ".json";
            new ObjectMapper().writeValue(new File(filename), response);

            // Create and insert prediction into the database
            MlPrediction prediction =
                    new MlPrediction.Builder(model, model.getGroup(), params.get("name"), params.get("comments"), uuid)
                            .predictor("startDate", params.get("startDate"))
                            .predictor("endDate", params.get("endDate"))
                            .predictorsFromEncodedString(params.get("predictors"))
                            .build();

            return new MlPredictionDAO().insert(prediction);

        } catch (IOException e) {
            throw new MLModelException("Unable to save data prediction", e);
        }

    }


    /**
     * Returns a list of persistent predictions.
     *
     * @param groupId
     * @param modelId
     * @return
     */
    public List<MlPrediction> getPersistentPredictions(Long groupId, Long modelId) {

        Map<String, Object> map = new HashMap<>();
        if (groupId != null) {
            map.put("group", GroupService.getInstance().get(groupId));
        }
        if (modelId != null) {
            map.put("trainedModel", new MlModelDAO().selectById(modelId));
        }
        return new MlPredictionDAO().selectAllBy(map);
    }


    /**
     * Returns a persistent prediction.
     *
     * @param id
     * @return
     */
    public MlPrediction getPersistentPrediction(Long id) {
        return new MlPredictionDAO().selectById(id);
    }


    /**
     * Returns data from a persistent prediction.
     *
     * @param dataId
     * @return
     * @throws MLModelException
     */
    public Map<String, Object> getDataPersistentPrediction(Long dataId) throws MLModelException {

        try {

            MlPrediction prediction = new MlPredictionDAO().selectById(dataId); // prediction and data prediction share same id
            String filename = MlConfiguration.property("predictions.path") + "/" + prediction.getUuid() + ".json";

            Map<String, Object> data = new ObjectMapper().readValue(
                    new File(filename),
                    new TypeReference<Map<String, Object>>() {
                    });

            return data;

        } catch (IOException e) {
            throw new MLModelException("Unable to load data prediction file", e);
        }


    }


    //-----------------------------------------------------
    // Expected responses from spark cluster
    //----------------------------------------------------


    public String handleResponse(String responseId, String response) throws MLModelException {

        try {
            if (isExpectedResponse(responseId)) {
                writeResponseIntoDisk(responseId + ".json", response);
            }
        } catch (IOException e) {
            throw new MLModelException("Unable to persist response with id " + responseId, e);
        }
        return responseId;
    }

    // TODO: implement something more sophisticated to avoid receiving not expected messages and storing them
    private boolean isExpectedResponse(String responseId) {
        return true;
    }

    private void writeResponseIntoDisk(String filename, String response) throws IOException {
        Writer fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(MlConfiguration.property("responses.path") + "/" + filename), "UTF-8"));
        IOUtils.write(response, fileWriter);
        fileWriter.close();
    }


    //-----------------------------------------------------
    // Common nested classes
    //----------------------------------------------------


    /**
     * This class models a row of data which is composed of names of columns (headers)
     * and their respective values. A row also has a special value which is its identifier.
     * Tha latter is associated to the column named "id".
     */
    public static class DataRow {

        private Map<String, Object> row = new LinkedHashMap<>();

        public DataRow(Long id, List<String> columnNames, List<String> values) {
            row.put("id", id);
            for (int i = 0; i < columnNames.size(); i++) {
                row.put(columnNames.get(i), values.get(i));
            }
        }

        public Long getId() {
            return (Long) row.get("id");
        }

        public Map<String, Object> asMap() {
            return row;
        }

        public Set<String> getColumnNames() {
            return row.keySet();
        }

        public Collection<Object> getValues() {
            return row.values();
        }

        public Object getValue(String columnName) {
            return row.get(columnName);
        }
    }


    //helper class to for predictor parameter
    //todo maybe use a simple array.
    public static class Predictor {
        private String featureName;
        private Long thingTypeId;
        private Long algorithmPredictorId;
        private String propertyName;

        private MlBusinessModelPredictorDAO mlBusinessModelPredictorDAO = new MlBusinessModelPredictorDAO();

        // the map and the object Property traslator should be in model predictor class. However, there are some
        // difficulties including the with Appgen.class since it couldn't refer the
        // "com.tierconnect.riot.iot.reports.mongodb.*" package.

        private static Map<String, String> map;

        static {
            map = new HashMap<>();
            map.put("serial", "serialNumber");
            map.put("id", "_id");
            map.put("Name", "name");
            map.put("zone", "zone.value.name");
            map.put("zoneLocalMap.id", "zone.value.facilityMap");
            map.put("localMap.id", "zone.value.facilityMap");
            map.put("zoneGroup.id", "zone.value.zoneGroup");
            map.put("zoneCode.name", "zone.value.id");
            map.put("zoneProperty.id", "zone.value.id"); //special case for zoneProperty.id
            map.put("zoneType.id", "zone.value.id");
            map.put("zoneType.name", "zone.value.id");
            map.put("group.groupType.id", "groupTypeId");
            map.put("logicalReader", "logicalReader.value.name");
            map.put("facilityCode", "facilityCode.value.name");
        }


        public Predictor(Long thingTypeId, Long algorithmPredictorId, String propertyName, String featureName) {
            this.thingTypeId = thingTypeId;
            this.algorithmPredictorId = algorithmPredictorId;
            this.propertyName = propertyName;
            this.featureName = featureName;
        }
        // JUST TO DEBUG
        public String toString(){
            return "{ " + thingTypeId + ", " + algorithmPredictorId + ", " + propertyName+ " }";
        }
        public MlExtractionPredictor create() {
            ThingType thingType = ThingTypeService.getInstance().get(thingTypeId);

            MlBusinessModelPredictor mlBusinessModelPredictor =
                    mlBusinessModelPredictorDAO.selectById(algorithmPredictorId);

            PropertyTranslator propertyTranslator = new PropertyTranslator(thingType, map);
            String propertyPath = propertyTranslator.translateFromString(propertyName);
            propertyPath = "value.".concat(propertyPath);

            return new MlExtractionPredictor(
                    mlBusinessModelPredictor,
                    thingType,
                    propertyName,
                    propertyPath,
                    featureName);
        }

        public static List<MlExtractionPredictor> createSeveral(List<Predictor> predictors) {

            List<MlExtractionPredictor> mlPredictors = new ArrayList<>();
            for (Predictor predictor : predictors) {
                mlPredictors.add(predictor.create());
            }

            return mlPredictors;
        }

    }

}
