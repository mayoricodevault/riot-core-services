package com.tierconnect.riot.iot.services.thing.control;

import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.services.thing.entity.CrudParameters;
import com.tierconnect.riot.iot.utils.Cache;
import org.apache.shiro.subject.Subject;

import java.util.Date;
import java.util.Map;

/**
 * Created by julio.rocha on 02-08-17.
 */
public class ThingParameterExtractor {
    private static final ThingParameterExtractor INSTANCE = new ThingParameterExtractor();

    private ThingParameterExtractor() {
    }

    public static ThingParameterExtractor getInstance() {
        return INSTANCE;
    }

    public CrudParameters extract(String thingTypeCode,
                                  String groupHierarchyCode,
                                  String name,
                                  String serialNumber,
                                  Map<String, Object> parent,
                                  Map<String, Object> udfs,
                                  Object children,
                                  Object childrenUdf,
                                  boolean executeTickle,
                                  boolean validateVisibility,
                                  Date transactionDate,
                                  boolean disableFMCLogic,
                                  boolean createAndFlush,
                                  Boolean useDefaultValues,
                                  Map<String, Boolean> validations,
                                  String facilityCode,
                                  boolean fillSource,
                                  Subject subject) {
        return InsertParameterExtractor.getInstance().extract(thingTypeCode, groupHierarchyCode,
                name, serialNumber, parent, udfs, children, childrenUdf, executeTickle, validateVisibility,
                transactionDate, disableFMCLogic, createAndFlush, useDefaultValues, validations,
                facilityCode, fillSource, subject);
    }

    public CrudParameters extract(Thing thing,
                                  boolean validateVisibility,
                                  Date transactionDate,
                                  boolean executeTickle,
                                  User userExecutor,
                                  Subject subject,
                                  User userLogged,
                                  boolean fillSource,
                                  boolean deleteMongoFlag,
                                  boolean secure) {
        return DeleteParameterExtractor.getInstance().extract(thing, validateVisibility,
                transactionDate, executeTickle, userExecutor, subject, userLogged, fillSource, deleteMongoFlag, secure);
    }

    public CrudParameters extract(Thing thing,
                                  String thingTypeCode,
                                  String groupHierarchyCode,
                                  String name,
                                  String serialNumber,
                                  Object parent,
                                  Map<String, Object> udfs,
                                  Object children,
                                  Object childrenUdf,
                                  boolean executeTickle,
                                  boolean validateVisibility,
                                  Date transactionDate, boolean disableFMCLogic,
                                  Map<String, Boolean> validations,
                                  Cache cache,
                                  boolean updateAndFlush,
                                  boolean recursiveUpdate, String facilityCode,
                                  Subject subject,
                                  User currentUser,
                                  boolean fillSource) {
        return UpdateParameterExtractor.getInstance()
                .extract(thing, thingTypeCode, groupHierarchyCode, name, serialNumber, parent, udfs, children, childrenUdf,
                        executeTickle, validateVisibility, transactionDate, disableFMCLogic, validations, cache, updateAndFlush,
                        recursiveUpdate, facilityCode, currentUser, subject, fillSource);
    }
}
