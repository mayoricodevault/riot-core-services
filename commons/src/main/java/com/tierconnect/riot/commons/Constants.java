package com.tierconnect.riot.commons;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by cfernandez
 * on 7/7/15.
 */
public class Constants {

    public static final String PREFIX_SYSTEM_COL = "vizix";
    public static final String DDD_EVENT_DIRECTION_IN = "in";
    public static final String DDD_EVENT_DIRECTION_OUT = "out";

    public static final String _ID = "_id";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String TIME = "time";
    public static final String PIN_LABEL = "pinLabel";
    public static final String HEAT = "heat";
    public static final String LABEL_NAME = "Name";
    public static final String SERIAL = "serial";
    public static final String SERIAL_NUMBER = "serialNumber";
    public static final String LOCATION = "location";
    public static final String THING_TYPE_CODE = "thingTypeCode";

    public static final String STEP_UPDATE_CACHE = "Updating thing cache";
    public static final String STEP_SAVE_THING_FIELDS = "Saving ThingFieldValues";

    public static final String THING_TYPE_DATA_TYPE = "DATA_TYPE";
    public static final String THING_TYPE_NATIVE_THING_TYPE = "NATIVE_THING_TYPE";

    public static final String UNKNOWN_ZONE_NAME = "Unknown";
    public static final String UNKNOWN_ZONE_CODE = "unknown";
    public static final String STARFLEX_DASHBOARD = "STARflex Dashboard";
    public static final String STARFLEX_MONITOR = "STARflex Monitor";
    public static final String STARFLEX_STATUS = "STARflex Status";
    public static final String STARFLEX_UNIQUE_TAGS = "STARflex Unique Tags";
    public static final String CONDITION_TYPE_GROUP_VIZIX_CEP = "VIZIX_CEP";
    public static final String CONDITION_TYPE_GROUP_VIZIX_FUNCTIONS = "VIZIX_FUNCTIONS";
    public static final String BRIDGE_TYPE = "BRIDGE_TYPE";
    public static final String ACTION_TYPE = "ACTION_TYPE";
    public static final String RULES_ACTION_TYPE = "RULES_ACTION";
    public static final String OPERATORS_TYPE = "OPERATOR";
    public static final String ACTION_STATUS_ACTIVE = "ACTIVE";
    public static final String ACTION_STATUS_DELETED = "DELETED";
    public static final String ADDED = "ADDED";
    public static final String TO_DELETE = "TO_DELETE";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELED = "CANCELED";
    public static final int CHUNK_AMOUNT = 4000;
    public static final String UPDATE_PROCESS = "UPDATE";
    public static final String DELETE_PROCESS = "DELETE";

    // RIOT-12852 Create&Update should populate source UDF with the value 'SERVICES'
    public static final String SOURCE_SERVICE = "SERVICES";
    public static final String UDF_SOURCE = "source";

    public static final String RELATIVE_DATE = "relativeDate";
    public static final String START_DATE = "startDate";
    public static final String END_DATE = "endDate";
    public static final String NOW_TIME = "NOW";

    public static final String EDGEBOX_CORE_TYPE = "core";
    public static final String EDGEBOX_EDGE_TYPE = "edge";
    public static final String EDGEBOX_GPS_TYPE = "GPS";
    public static final String EDGEBOX_STARFLEX_TYPE = "STARflex";
    public static final String EDGEBOX_FTP_TYPE = "FTP";
    public static final String APP2_MQTT_TOPIC = "/v1/data/APP2/#";
    public static final String EMAIL_CONFIGURATION_ERROR = "emailConfigurationError";
    public static final String EMAIL_SMTP_USER = "emailSmtpUser";
    public static final String EMAIL_RIOT_TEST = "riottest@tierconnect.com";

    public static final String REPORT_LOG_THRESHOLD = "reportLogThreshold";
    public static final String REPORT_LOG_ENABLE = "reportLogEnable";

    /*Automatic delete index*/
    public static final String INDEX_MIN_DAYS_TO_MAINTAIN = "indexMinDaysToMaintain";
    public static final String INDEX_CLEANUP_SCHEDULE = "indexCleanupSchedule";
    public static final String INDEX_STATISTIC_SCHEDULE = "indexStatisticSchedule";
    public static final String INDEX_MIN_COUNT_TO_MAINTAIN = "indexMinCountToMaintain";
    /*Automatic delete index*/

    public static final int REPORT_LOG_MAX_ARRAY_HISTORY_LOG = -50000;


    public static final String BRIDGE_ERROR_STATUS_TIMEOUT = "bridgeErrorStatusTimeout";


    public static final List<String> NON_UDFS = Collections.unmodifiableList(
            Arrays.asList("serialNumber", "name",
                    "groupId", "groupCode", "groupName",
                    "thingTypeId", "thingTypeCode", "thingTypeName",
                    "time", "modifiedTime", "createdTime",
                    "sqn", "specName",
                    "id", "_id", "lastValue", "timeSeries"));

    public static final List<String> META_FIELDS = Collections.unmodifiableList(
            Arrays.asList("source", "outOfOrder", "tsCoreIn", "tsEdgeIn", "filter", "seqNum"));


    //Reports
    public static final String REGEX_CASE_INSENSITIVE = "i";
    public static final String REGEX_MULTI_MATCH = "m";
    public static final String REGEX_EXTENDED = "e";
    public static final String REGEX_MULTILINE = "s";
    public static final String REGEX_CASE_SENSITIVE = "";
    public static final String TEMP_NAME_REPORT = "tmpExportRep";
    public static final String DWELLTIME = "dwellTime";
    public static final String ZONE_PROPERTY_ID = "Property.id";
    public static final String ZONE_TYPE_NAME = "Type.name";
    public static final String POPUP_COMMON_FIELDS_NAME = "thing.headers";

    //Report Operators
    public static final String OP_CONTAINS = "~";
    public static final String OP_LESS_THAN = "<";
    public static final String OP_GREATHER_THAN = ">";
    public static final String OP_NOT_EQUALS = "!=";
    public static final String OP_GREATHER_THAN_EQUALS = ">=";
    public static final String OP_LESS_THAN_EQUALS = "<=";
    public static final String OP_IS_EMPTY = "isEmpty";
    public static final String OP_IS_NOT_EMPTY = "isNotEmpty";
    public static final String OP_EQUALS = "=";
    public static final String OP_DOUBLE_EQUALS = "==";

    public static final List<String> OP_TYPES = Collections.unmodifiableList(
            Arrays.asList(OP_CONTAINS, OP_LESS_THAN, OP_GREATHER_THAN,
                    OP_NOT_EQUALS, OP_GREATHER_THAN_EQUALS, OP_LESS_THAN_EQUALS,
                    OP_IS_EMPTY, OP_IS_NOT_EMPTY, OP_EQUALS, OP_DOUBLE_EQUALS)
    );

    public final static String GROUP_PROPERTY_ID = "group.id";
    public final static String GROUP_PROPERTY_NAME = "group.name";
    public final static String THING_TYPE_PROPERTY_ID = "thingType.id";
    public final static String THING_TYPE_PROPERTY_NAME = "thingType.name";
    public final static String GROUP_TRANSLATE_ID = "groupId";
    public final static String GROUP_TRANSLATE_NAME = "groupName";
    public final static String GROUP_TYPE_TRANSLATE_NAME = "groupTypeName";
    public final static String THING_TYPE_TRANSLATE_ID = "thingTypeId";
    public final static String THING_TYPE_TRANSLATE_NAME = "thingTypeName";
    public final static String GROUP_TS_TRANSLATE = "value.groupId";
    public final static String THING_TS_TYPE_TRANSLATE = "value.thingTypeId";
    public final static String PATH_TTUDF_SERIAL_NUMBER = ".value.serialNumber";
    public final static String PATH_TTUDF_VALUE = ".value";

    // reports
    public static final String REPORT_TYPE_TABLE_DETAIL = "table";
    public static final String REPORT_TYPE_TABLE_SUMMARY = "tableSummary";
    public static final String REPORT_TYPE_MAP_SUMMARY = "mapSummary";
    public static final String REPORT_TYPE_MAP_SUMMARY_BY_ZONE = "mapSummaryByZone";
    public static final String REPORT_TYPE_TABLE_HISTORY = "tableTimeSeries";
    public static final String REPORT_TYPE_MAP_HISTORY = "mapHistory";
    public static final String REPORT_TYPE_MAP = "map";
    public static final String REPORT_TYPE_TABLE_SCRIPT = "mongo";
    public static final String REPORT_TYPE_TABLE_CONNECTION = "tableConnection";
    public static final String BULK_PROCESS_REPORT_MODULE_NAME = "reports";
    public static final String DWELLTIME_CHANGED = "DTHC";
    public static final String DWELLTIME_BLINKED = "DTBK";
    public static final List<String> REPORT_TYPES = Collections.unmodifiableList(
            Arrays.asList(REPORT_TYPE_MAP, REPORT_TYPE_MAP_SUMMARY, REPORT_TYPE_TABLE_DETAIL,
                    REPORT_TYPE_TABLE_HISTORY, REPORT_TYPE_TABLE_SUMMARY, REPORT_TYPE_TABLE_SCRIPT, REPORT_TYPE_TABLE_CONNECTION)
    );

    //Summary Last Known
    public static final String LAST_KNOWN_CHANGED = ".changed";
    public static final String LAST_KNOWN_BLINKED = ".blinked";

    // collections

    public static final String COLLECTION_THINGS = "things";
    public static final String COLLECTION_EXIT_REPORT = "exit_report";
    public static final String COLLECTION_THINGSNAPSHOT = "thingSnapshots";
    public static final String COLLECTION_THINGSNAPSHOT_IDS = "thingSnapshotIds";

    //
    public static final String CHILDREN = "children";
    public static final String PARENT = "parent";

    // records
    public static final String THING_FIELD_TYPE_MAP = "thingFieldTypeMap";
    public static final String THING_HEADERS = "thingHeaders";
    public static final String THING_PROPERTIES = "thingProperties";
    public static final String THING_CREATED = "thingCreated";

    public static final String SENT_EMAIL_REPORT = "SEND REPORT BY EMAIL";
    public static final String EXPORT_REPORT = "EXPORT REPORT";
    public static final String UI_REPORT = "REPORT";

    // topic separators
    public static final String DEFAULT_SEPARATOR = "/";
    public static final String KAFKA_SEPARATOR = "___";

    // accumulators Summary Reports
    public static final String SUM = "SUM";
    public static final String AVG = "AVG";
    public static final String MAX = "MAX";
    public static final String MIN = "MIN";

    public static final String COUNT = "count";
    public static final String OTHER = "[other]";
    public static final String TOTAL = "Total";
    public static final String OTHER_Y = "otherY";
    public static final String PROPERTY_EMPTY = "[Empty]";

    //Thing Types STARflex
    public static final String TT_STARflex_CONFIG_CODE = "STR_400";
    public static final String TT_STARflex_CONFIG_NAME = "STARflex Config";
    public static final String TT_STARflex_STATUS_CODE = "starflex_status_code";
    public static final String TT_STARflex_STATUS_NAME = "STARflex Status";
    public static final String TT_STARflex_CODE = "flextag_code";
    public static final String TT_STARflex_NAME = "STARflex";

    public static final String THING_STARflex_CONFIG_ACRONYM = "SF_";
    public static final String THING_STARflex_STATUS_ACRONYM = "SFS_";
    public static final String THING_STARflex_ACRONYM = "SFT_";

    public static final String STARFLEX_TAG_BRIDGES_CODE_ACRONYM = "STAR_";
    public final static String STARFLEX_TAG_BRIDGES_NAME_ACRONYM = "StarflexTag_";

    public final static String STARFLEX_MONGO_CONN_NAME = "MONGO";
    public final static String STARFLEX_MQTT_CONN_NAME = "MQTT";

    public final static String STARFLEX_MONGO_CONN_ACRONYM = "MONGO_";
    public final static String STARFLEX_MQTT_CONN_ACRONYM = "MQTT_";

    public final static String STARFLEX_MAIN_BRIDGE_CODE = "STAR";
    public final static String STARFLEX_MAIN_GROUP_CODE = "SF";

    public final static String TENANT_COPANY = "tenant";
    public final static String EDGEBOX_CODE_MCB = "MCB";

    public static final int PARENT_GROUP_LEVEL_MQTT = 2;

    public static final String MQTT_CONNECTION = "MQTT";
    public static final String KAFKA_CONNECTION = "KAFKA";


    // Kafka Stream Configuration.
    public static final int DEFAULT_LOW_THROUGHPUT_LINGER_MS = 0;
    public static final int DEFAULT_HIGH_THROUGHPUT_LINGER_MS = 5;
    public static final int DEFAULT_LOW_THROUGHPUT_BATCHSIZE = 16384;
    public static final int DEFAULT_HIGH_THROUGHPUT_BATCHSIZE = 65536;

    // Spec name
    public static final String SPEC_NAME_SWARM = "SWR";
    public static final String SPEC_NAME_MYSPECNAME = "MYSPECNAME";

    //Default Time Zone
    public static final String DEFAULT_TIME_ZONE = "EST5EDT";
    public static final String DEFAULT_DATE_FORMAT = "MM/DD/YYYY hh:mm:ss A";
    public static final String TIME_ZONE_CONFIG = "timeZoneConfiguration";
    public static final String DATE_FORMAT_CONFIG = "dateFormatConfiguration";

    //
    public static final String CATEGORY_THING_TYPE_TEMPLATE_CUSTOM = "CUSTOM";
    public static final String CATEGORY_THING_TYPE_TEMPLATE_SENSORS = "SENSORS";
    public static final String CATEGORY_THING_TYPE_TEMPLATE_M2M = "M2M";
    public static final String CATEGORY_THING_TYPE_TEMPLATE_BLOCKCHAIN = "BLOCKCHAIN";
    // Templates Mojix Retail App
    public static final String CATEGORY_THING_TYPE_TEMPLATE_MOJIX_RETAIL_APP_BASE = "MOJIX_RETAIL_APP_BASE";
    public static final String CATEGORY_THING_TYPE_TEMPLATE_MOJIX_RETAIL_APP_SYNC = "MOJIX_RETAIL_APP_SYNC";
    public static final String CATEGORY_THING_TYPE_TEMPLATE_MOJIX_RETAIL_APP_CONFIG = "MOJIX_RETAIL_APP_CONFIG";

    //Connection properties that must be integers
    public static final String CONNECTION_MASTER_PORT = "masterPort";
    public static final String CONNECTION_RESPONSE_TIMEOUT = "responseTimeout";
    public static final String CONNECTION_MONGO_PORT = "mongo.port";
    public static final List<String> CONNECTION_INTEGER_PROPERTIES = Collections.unmodifiableList(
            Arrays.asList(CONNECTION_MASTER_PORT, CONNECTION_RESPONSE_TIMEOUT, CONNECTION_MONGO_PORT)
    );

    public enum FileOperation {
        OVERRIDE("overwrite"),
        RENAME("rename"),
        SKIP("skip");

        public String value;

        FileOperation(String value) {
            this.value = value;
        }
    }

    public enum ActionHTTPConstants {
        EXECUTION_TYPE_METHOD("method"),
        EXECUTION_TYPE_URL("url"),
        EXECUTION_TYPE_HEADERS("headers"),
        EXECUTION_TYPE_TIMEOUT("timeout"),
        EXECUTION_TYPE_BASIC_AUTH("basicAuth"),
        EXECUTION_TYPE_BASIC_AUTH_USERNAME("username"),
        EXECUTION_TYPE_BASIC_AUTH_PASSWORD("password"),
        EXECUTION_TYPE("executionType"),
        EXECUTION_TYPE_REST("REST"),
        EXECUTION_TYPE_FORM("FORM");

        public String value;

        ActionHTTPConstants(String value) {
            this.value = value;
        }
    }

    // Mongo
    public static final String MONGO_CONTROL_READ_PREFERENCE = "primary";
    public static final String MONGO_REPORTS_READ_PREFERENCE = "primary";

    //Messages
    public enum AutoIndexMessages {
        DELETE_PROCESS("Indexing: This field defines each when the deletion process is executed automatically."),
        GENERATE_STATICS("Indexing: This field defines each when the process of generating statistics is executed automatically."),
        INDEX_ANALYZE("Indexing: Minimum number of days to analyze an index."),
        INDEX_MIN_EXECUTIONS("Indexing: Minimum number of executions before an Index is deleted.");

        AutoIndexMessages(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        private String message;

    }

    // Scheduled Rule APP2
    public static final String SCHEDULED_RULE_DATA = "/v1/data/";
    public static final long SCHEDULED_RULE_MESSAGE_SIZE = 50;
    public static final String BRIDGE_CODE = "bridgeCode";

    //Password policies
    public static final int PASSWORD_MIN_LENGTH = 1;
    public static final int PASSWORD_MAX_LENGTH = 128;
    public static final String PASSWORD_STATUS_PENDING = "PENDING";
    public static final String PASSWORD_STATUS_ACTIVE = "ACTIVE";
    public static final String PASSWORD_STATUS_INACTIVE = "INACTIVE";

    // constans of snapshots
    public static final String SNAPSHOT_TIMESERIES = "timeSeries";
    public static final String SNAPSHOT_LASTVALUE = "lastValue";
    public static final String SNAPSHOT_CREATEDTIME = "createdTime";
    public static final String SNAPSHOT_VALUE = "value";
    public static final String SNAPSHOT_TIME = "time";
    public static final String SNAPSHOT_ID = "_id";

    // Rule conditions
    public static final String CONDITION_TYPE = "CONDITION_TYPE";
    public static final String CONDITION_TYPE_JS = "JS";
    public static final String CONDITION_TYPE_CEP = "CEP";
    public static final String CONDITION_TYPE_ALWAYS_TRUE = "ALWAYS_TRUE";
    public static final String CONDITION_TYPE_SCHEDULED = "SCHEDULED";

    // Rule actions
    public static final String ACTION_MQTT_SUBSCRIBER = "MQTTPushSubscriber";
    public static final String ACTION_THING_PROPERTY_SETTER = "ThingPropertySetter";
    public static final String ACTION_JAVASCRIPT_ACTION = "ThingPropertySetterJSSubscriber";
    public static final String ACTION_WRITE_TO_MFR = "MFRReaderGPIOSubscriber";
    public static final String ACTION_TURN_OFF_MFR = "MFRTurnOffGPIOSubscriber";
    public static final String ACTION_WRITE_TO_ALIEN = "AlienReaderGPIOSubscriber";
    public static final String ACTION_SEND_EMAIL = "EmailSubscriber";
    public static final String ACTION_FLOW_HTTP = "com.tierconnect.riot.bridges.rules.actions.RestEndpointSubscriber";
    public static final String ACTION_BIGDATA_TABLE = "ReportGeneratorSubscriber";
    public static final String ACTION_SEND_DB_QUERY = "ExternalDataBaseSubscriber";
    public static final String ACTION_TCP_ACTION = "TCPAction";
    public static final String ACTION_MULTIPLE_ACTIONS = "com.tierconnect.riot.bridges.rules.actions.SuperSubscriber";
    public static final String ACTION_GOOGLE_PUBSUB_PUBLISH = "GooglePubSubPublish";

    // page size group
    public static final String GROUP_PAGE_SIZE = "pageSize";

    public static final String TYPE_ICON_PATH = "resources/iconset/";
    public static final String TYPE_ICON_PREFIX = "mojix-retail";
}

