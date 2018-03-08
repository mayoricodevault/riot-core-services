package com.tierconnect.riot.iot.popdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysema.query.NonUniqueResultException;
import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.servlet.security.ApiKeyToken;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.hibernate.Transaction;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.*;
import java.util.Date;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.math.NumberUtils.isNumber;


public class PopDBBase {
    private static final Logger logger = Logger.getLogger(PopDBBase.class);
    public String currentPopDB;
    private final Date popDBDate = new Date();

    public static void main(String args[]) throws Exception {
        logger.info("POPDBBASE is starting ... ");
        initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
        System.getProperties().put("hibernate.cache.use_second_level_cache", "false");
        System.getProperties().put("hibernate.cache.use_query_cache", "false");
        Configuration.init(null);
        testTransaction();

        PopDBBase popdb = new PopDBBase();
        LinkedHashMap<String, JSONObject> definitions = popdb.getPopDBDependencies();
        List<String> dependencies = new ArrayList<>(definitions.keySet());
        for (int i = dependencies.size() - 1; i >= 0; i--) {
            Transaction transaction = UserService.getUserDAO().getSession().getTransaction();
            transaction.begin();
            initShiroWithRoot();
            popdb.currentPopDB = dependencies.get(i);
            try {
                popdb.preProcess((JSONArray) definitions.get(popdb.currentPopDB).get("preProcess"));
                popdb.executeModules((JSONArray) definitions.get(popdb.currentPopDB).get("executeModules"));
                popdb.postProcess((JSONArray) definitions.get(popdb.currentPopDB).get("postProcess"));
                transaction.commit();
            } catch (UserException e) {
                logger.error(e.getMessage(), e);
                transaction.rollback();
            }
        }
        System.exit(0);
    }

    private static void initJDBCDrivers() {
        try {
            Class.forName("org.gjt.mm.mysql.Driver");
            logger.info("registering mysql jdbc driver");
        } catch (Exception ex) {
            //empty
        }
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            logger.info("registering sqlServer jdbc driver");
        } catch (Exception ex) {
            //empty
        }
    }

    private static void testTransaction() {
        if (!Boolean.parseBoolean(getConfProperty("popdb.erase"))) {
            try {
                Connection connection = DriverManager.getConnection(
                    getConfProperty("hibernate.connection.url"),
                    getConfProperty("hibernate.connection.username"),
                    getConfProperty("hibernate.connection.password"));
                DatabaseMetaData md = connection.getMetaData();
                ResultSet rs = md.getTables(null, null, "%", null);
                if (!rs.next()) {
                    connection.close();
                    throw new SQLException();
                }
                connection.close();
            } catch (SQLException e) {
                logger.error("No database found, please run with 'clean' option.");
                System.exit(0);
            }
        }
    }

    private static void initShiroWithRoot() throws UnknownHostException {
        Factory<SecurityManager> factory = new IniSecurityManagerFactory("classpath:shiro_riot.ini");
        SecurityManager securityManager = factory.getInstance();
        SecurityUtils.setSecurityManager(securityManager);
        RiotShiroRealm.initCaches();
        Subject currentUser = SecurityUtils.getSubject();
        ApiKeyToken token = new ApiKeyToken(UserService.getInstance().getRootUser().getApiKey());
        currentUser.login(token);

        MongoDAOUtil.setupMongoPopDb(Configuration.getProperty("mongo.primary"),
                Configuration.getProperty("mongo.secondary"),
                Configuration.getProperty("mongo.replicaset"),
                Boolean.valueOf(Configuration.getProperty("mongo.ssl")),
                Configuration.getProperty("mongo.username"),
                Configuration.getProperty("mongo.password"),
                Configuration.getProperty("mongo.authdb"),
                Configuration.getProperty("mongo.db"),
                Configuration.getProperty("mongo.controlReadPreference"),
                Configuration.getProperty("mongo.reportsReadPreference"),
                Boolean.valueOf(Configuration.getProperty("mongo.sharding")),
                (isNotBlank(Configuration.getProperty("mongo.connectiontimeout"))
                        && isNumber(Configuration.getProperty("mongo.connectiontimeout"))) ?
                        Integer.parseInt(Configuration.getProperty("mongo.connectiontimeout")) : null,
                (isNotBlank(Configuration.getProperty("mongo.maxpoolsize"))
                        && isNumber(Configuration.getProperty("mongo.maxpoolsize"))) ?
                        Integer.parseInt(Configuration.getProperty("mongo.maxpoolsize")) : null);
    }

    private static String getConfProperty(String propertyName) {
        if (System.getProperty(propertyName) != null) {
            return System.getProperty(propertyName);
        }
        return Configuration.getProperty(propertyName);
    }

    public LinkedHashMap<String, JSONObject> getPopDBDependencies()
        throws IOException, ParseException, URISyntaxException {
        String popDBName = getConfProperty("popdb.option");
        LinkedHashMap<String, JSONObject> dependencies = new LinkedHashMap<>();

        String path = "/app/WEB-INF/classes/popDB/";

        do {
            InputStream is;
            try {
                logger.info("Getting resources from: " + path + "popDB/" + popDBName + "/definition.json");
                is = new FileInputStream(path + "popDB/" + popDBName + "/definition.json");
            } catch (FileNotFoundException e) {
                path = FilenameUtils.getFullPath(
                    PopDBBase.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
                    + "../resources/main/";
                try {
                    logger.info("Not found, getting resources from:" + path + "popDB/" + popDBName + "/definition.json");
                    is = new FileInputStream(path + "popDB/" + popDBName + "/definition.json");
                } catch (FileNotFoundException e1) {
                    logger.info("Not found, getting resources from classpath.");
                    is = PopDBBase.class.getClassLoader().getResourceAsStream("popDB/" + popDBName +
                        "/definition.json");
                }
            }
            if (is != null) {
                String text = IOUtils.toString(is, "UTF-8");
                JSONParser parser = new JSONParser();
                JSONObject definitionFiles = (JSONObject) parser.parse(text);
                dependencies.put(popDBName, definitionFiles);
                popDBName = definitionFiles.get("dependsOn").toString();
                is.close();
            } else {
                popDBName = null;
            }
        } while (StringUtils.isNotBlank(popDBName));
        if (dependencies.isEmpty()){
            logger.warn("No popDB found, only minimal resources were created.");
        }
        return dependencies;
    }

    private void preProcess(JSONArray pre){
    }

    private void postProcess(JSONArray post){
    }

    public void executeModules(JSONArray definitionFiles) throws UserException {
        logger.info("------- Populating " + currentPopDB + " -------");
        JSONParser parser = new JSONParser();
        for (Object definitionFile : definitionFiles) {
            try {
                JSONObject definition = (JSONObject) parser.parse(definitionFile.toString());
                logger.info("**** Populating " + definition.get("module") + " from definition file: " +
                        definition.get("definitionFile") + " ****");
                populateModule("popDB/" + currentPopDB + "/" + definition.get("definitionFile").toString());
                logger.info("**** Done ****");
            } catch (ParseException e) {
                logger.warn("An error occurred while parsing definition file for module "
                        + definitionFile.toString() + ", module skipped.", e);
            }
        }
        logger.info("------- " + currentPopDB + " finished -------");
    }

    private void populateModule(String fileName) {
        try {
            InputStream is = PopDBBase.class.getClassLoader().getResourceAsStream(fileName);
            if (is != null) {
                String text = IOUtils.toString(is, "UTF-8");
                JSONParser parser = new JSONParser();
                JSONObject definitionObjects = (JSONObject) parser.parse(text);
                preExecute((JSONArray) definitionObjects.get("preProcess"));
                executeModule((JSONArray) definitionObjects.get("executeModules"));
                postExecute((JSONArray) definitionObjects.get("postProcess"));
                is.close();
            } else {
                logger.warn("Definition file " + fileName + " was not found, module skipped.");
            }
        } catch (Exception e) {
            logger.error("An Error was occurred while processing definition file " + fileName, e);
        }
    }

    private void preExecute(JSONArray pre){
    }

    private void postExecute(JSONArray post){
    }

    private void executeModule(JSONArray definitionObjects) {
        PopDBBaseUtils ut = new PopDBBaseUtils(currentPopDB, popDBDate);
        JSONParser parser = new JSONParser();
        int moduleCount = 0;
        int instanceCount = 0;
        int skipCount = 0;
        for (Object definitionObject : definitionObjects) {
            try {
                JSONObject definition = (JSONObject) parser.parse(replaceConfiguration(definitionObject.toString()));
                String importPathService = ut.getPathForClass(definition.get("innerClass").toString(), "services");
                String importPathEntities = ut.getPathForClass(definition.get("innerClass").toString(), "entities");

                ObjectMapper mapper = new ObjectMapper();
                String className = definition.get("innerClass").toString().equals("Thing")
                        ? "Things" : definition.get("innerClass").toString();
                JSONArray instances = (JSONArray) definition.get("instances");
                if (StringUtils.equals(className, "CustomThing")) {
                    for (Object inst : instances) {
                        try {
                            ut.createThing((JSONObject) inst);
                            logger.info(className + " instance" + getInstanceLog((JSONObject) inst)
                                + " successfully created.");
                            instanceCount++;
                        } catch (Exception e) {
                            if (e instanceof UserException || e.getCause() instanceof UserException
                                || e.getCause() instanceof NonUniqueResultException) {
                                logger.warn(
                                    className + " instance" + getInstanceLog((JSONObject) inst)
                                        + " already exists.");
                                skipCount++;
                            } else {
                                logger.warn("An Error occurred while processing " + className + " instance"
                                    + getInstanceLog((JSONObject) inst) + ".", e);
                            }
                        }
                    }
                } else {
                    Class<?> srv = Class.forName(importPathService + className + "Service");
                    Class<?> cls = Class.forName(importPathEntities + definition.get("innerClass").toString());
                    Method methodInstance = srv.newInstance().getClass().getMethod("insert", cls);
                    for (Object inst : instances) {
                        try {
                            sanitizeObject((JSONObject) inst);
                            Object entity = mapper.readValue(jsonToString((JSONObject) inst), cls);
                            methodInstance.invoke(srv.newInstance(), entity);
                            logger.info(className + " instance" + getInstanceLog((JSONObject) inst)
                                + " successfully created.");
                            instanceCount++;
                        } catch (Exception e) {
                            if (e instanceof UserException || e.getCause() instanceof UserException
                                || e.getCause() instanceof NonUniqueResultException) {
                                logger.warn(
                                    className + " instance" + getInstanceLog((JSONObject) inst) + " already exists.");
                                skipCount++;
                            } else {
                                logger.warn("An Error occurred while processing " + className + ""
                                    + " instance"
                                    + getInstanceLog((JSONObject) inst) + ".", e);
                            }
                        }
                    }
                    moduleCount++;
                }
            } catch (Exception e) {
                throw new UserException(e);
            }
        }
        logger.info("Processed " + moduleCount + " modules and " + instanceCount + " instances ("
            + skipCount +" skipped)");
    }

    private String getInstanceLog(JSONObject inst) {
       if (inst.get("name") != null) {
            return " [" + inst.get("name") + "]";
        } else if (inst.get("code") != null) {
            return " [" + inst.get("code") + "]";
        } else {
            return "";
        }
    }

    private void sanitizeObject(JSONObject definition) throws Exception {
        PopDBBaseUtils ut = new PopDBBaseUtils(currentPopDB, popDBDate);
        for (Object key : definition.keySet()) {
            if (definition.get(key) != null
                    && definition.get(key) instanceof JSONObject
                    && (((JSONObject) definition.get(key)).containsKey("innerClass")
                    || ((JSONObject) definition.get(key)).containsKey("innerMethod"))) {
                if (((JSONObject) definition.get(key)).get("params") instanceof JSONArray) {
                    JSONArray args = (JSONArray) ((JSONObject) definition.get(key)).get("params");
                    Class<?>[] paramClass = new Class[args.size()];
                    Object[] methodParams = new Object[args.size()];
                    int pos = 0;
                    for (Object arg1 : args) {
                        JSONObject arg = (JSONObject) arg1;
                        if (!arg.get("innerClass").toString().equals("String")) {
                            String importPathEntities = ut.getPathForClass(arg.get("innerClass").toString(), "entities");
                            paramClass[pos] = Class.forName(importPathEntities + arg.get("innerClass"));
                            methodParams[pos] = getObjectByField(arg.get("innerClass").toString(),
                                    arg.get("getter").toString(),
                                    arg.get("params").toString());
                        } else {
                            paramClass[pos] = String.class;
                            methodParams[pos] = arg.get("params");
                        }
                        pos++;
                    }

                    if (((JSONObject) definition.get(key)).containsKey("innerClass")) {
                        int level = ((JSONObject) definition.get(key)).get("classLevel") != null ?
                                Integer.parseInt(((JSONObject) definition.get(key)).get("classLevel").toString()) : 1;
                        definition.put(key,
                                getMapByFields(((JSONObject) definition.get(key)).get("innerClass").toString(),
                                        ((JSONObject) definition.get(key)).get("getter").toString(),
                                        paramClass, methodParams, level));
                    } else {
                        if (((JSONObject) definition.get(key)).containsKey("ignore")
                                && Boolean.parseBoolean(((JSONObject) definition.get(key)).get("ignore").toString())){
                            callMethodByFields(((JSONObject) definition.get(key)).get("innerMethod").toString(),
                                    paramClass, methodParams);
                            definition.remove(key);
                        } else {
                            definition.put(key,
                                    callMethodByFields(((JSONObject) definition.get(key)).get("innerMethod").toString(),
                                            paramClass, methodParams));
                        }
                    }
                } else {
                    if (((JSONObject) definition.get(key)).containsKey("innerClass")) {
                        int level = ((JSONObject) definition.get(key)).get("classLevel") != null ?
                                Integer.parseInt(((JSONObject) definition.get(key)).get("classLevel").toString()) : 1;
                        definition.put(key,
                                getMapByField(((JSONObject) definition.get(key)).get("innerClass").toString(),
                                        ((JSONObject) definition.get(key)).get("getter").toString(),
                                        ((JSONObject) definition.get(key)).get("params").toString(), level));
                    } else {
                        if (((JSONObject) definition.get(key)).containsKey("ignore")
                                && Boolean.parseBoolean(((JSONObject) definition.get(key)).get("ignore").toString())){
                            callMethodByField(((JSONObject) definition.get(key)).get("innerMethod").toString(),
                                    ((JSONObject) definition.get(key)).get("params"));
                            definition.remove(key);
                        } else {
                            definition.put(key,
                                    callMethodByField(((JSONObject) definition.get(key)).get("innerMethod").toString(),
                                            ((JSONObject) definition.get(key)).get("params")));
                        }
                    }
                }
            }
        }
    }

    private String jsonToString(JSONObject definition) {
        String instString = definition.toString().replace("newDate", String.valueOf(popDBDate.getTime()));
        instString = instString.replaceAll("xNominal", "xnominal");
        instString = instString.replaceAll("yNominal", "ynominal");
        instString = instString.replaceAll("RFIDprint", "rfidprint");
        return instString;
    }

    private static String replaceConfiguration(String instString){
        int pos = 0;
        int posA;
        do {
            posA = instString.indexOf("${", pos);
            if (posA > -1) {
                int posB = instString.indexOf("}", posA);
                String config = instString.substring(posA + 2, posB);
                try {
                    instString = instString.replaceAll("\\$\\{" + config + "}", getConfProperty(config));
                } catch (Exception ignore){
                }
                pos = posB;
            }
        } while (posA > -1);
        return instString;
    }

    private Object getObjectByField(String innerClass, String getter, String field) throws Exception {
        PopDBBaseUtils ut = new PopDBBaseUtils(currentPopDB, popDBDate);
        String importPathServices = ut.getPathForClass(innerClass, "services");

        Class<?> srv = Class.forName(importPathServices + innerClass + "Service");

        Method methodInstance = srv.newInstance().getClass().getMethod(getter, String.class);

        return methodInstance.invoke(srv.newInstance(), field);
    }

    private Object getMapByField(String innerClass, String getter, String field, int level) throws Exception {
        try {
            PopDBBaseUtils ut = new PopDBBaseUtils(currentPopDB, popDBDate);
            return ut.getReferencedPublicMap(innerClass, getObjectByField(innerClass, getter, field), level);
        } catch (NullPointerException e) {
            return null;
        }
    }

    private Object getMapByFields(String innerClass, String getter, Class<?>[] paramClass, Object[] methodParams, int level)
            throws Exception {
        try {
            PopDBBaseUtils ut = new PopDBBaseUtils(currentPopDB, popDBDate);
            String importPathServices = ut.getPathForClass(innerClass, "services");

            Class<?> srv = Class.forName(importPathServices + innerClass + "Service");

            Object reference;
            if (paramClass.length > 0) {
                Method methodInstance = srv.newInstance().getClass().getMethod(getter, paramClass);
                reference = methodInstance.invoke(srv.newInstance(), methodParams);
            } else {
                Method methodInstance = srv.newInstance().getClass().getMethod(getter);
                reference = methodInstance.invoke(srv.newInstance());
            }
            if (reference instanceof ArrayList) {
                reference = ((ArrayList) reference).get(0);
            }

            return ut.getReferencedPublicMap(innerClass, reference, level);
        } catch (NullPointerException e) {
            return null;
        }
    }

    private Object callMethodByField(String methodName, Object field) throws Exception {
        PopDBBaseUtils ut = new PopDBBaseUtils(currentPopDB, popDBDate);
        try {
            Method method = ut.getClass().getMethod(methodName, Object.class);
            return method.invoke(ut, field);
        } catch (NullPointerException e) {
            return null;
        }
    }

    private Object callMethodByFields(String methodName, Class<?>[] paramClass, Object[] methodParams)
            throws Exception {
        PopDBBaseUtils ut = new PopDBBaseUtils(currentPopDB, popDBDate);
        try {
            Method method = ut.getClass().getMethod(methodName, paramClass);
            return method.invoke(ut, methodParams);
        } catch (NullPointerException e) {
            return null;
        }
    }
}
