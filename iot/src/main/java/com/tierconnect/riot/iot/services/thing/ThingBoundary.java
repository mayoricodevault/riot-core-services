package com.tierconnect.riot.iot.services.thing;

import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.services.thing.control.ThingCreator;
import com.tierconnect.riot.iot.services.thing.control.ThingDeleter;
import com.tierconnect.riot.iot.services.thing.control.ThingUpdater;
import com.tierconnect.riot.iot.utils.Cache;
import org.apache.log4j.Logger;
import org.apache.shiro.subject.Subject;

import java.util.Date;
import java.util.Map;
import java.util.Stack;

/**
 * Created by julio.rocha on 02-08-17.
 */
public class ThingBoundary {
    private static Logger logger = Logger.getLogger(ThingBoundary.class);
    private static final ThingBoundary INSTANCE = new ThingBoundary();

    private ThingBoundary() {
    }

    public static ThingBoundary getInstance() {
        return INSTANCE;
    }

    public Map<String, Object> create(Stack<Long> recursivelyStack,
                                      String thingTypeCode,
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
        return ThingCreator.getInstance().create(
                recursivelyStack,
                thingTypeCode,
                groupHierarchyCode,
                name,
                serialNumber,
                parent,
                udfs,
                children,
                childrenUdf,
                executeTickle,
                validateVisibility,
                transactionDate,
                disableFMCLogic,
                createAndFlush,
                useDefaultValues,
                validations,
                facilityCode,
                fillSource,
                subject);
    }

    public Map<String, Object> update(Stack<Long> recursivelyStack
            , Thing thing
            , String thingTypeCode
            , String groupHierarchyCode
            , String name
            , String serialNumber
            , Object parent
            , Map<String, Object> udfs
            , Object children
            , Object childrenUdf
            , boolean executeTickle
            , boolean validateVisibility
            , Date transactionDate
            , boolean disableFMCLogic
            , Map<String, Boolean> validations
            , Cache cache
            , boolean updateAndFlush
            , boolean recursiveUpdate
            , String facilityCode
            , Subject subject
            , User currentUser
            , boolean fillSource) {
        return ThingUpdater.getInstance().update(recursivelyStack
                , thing
                , thingTypeCode
                , groupHierarchyCode
                , name, serialNumber
                , parent
                , udfs
                , children
                , childrenUdf
                , executeTickle
                , validateVisibility
                , transactionDate
                , disableFMCLogic
                , validations
                , cache
                , updateAndFlush
                , recursiveUpdate
                , facilityCode
                , subject, currentUser, fillSource);
    }


    public void delete(Stack<Long> recursivelyStack,
                       Thing thing,
                       boolean validateVisibility,
                       Date transactionDate,
                       boolean executeTickle,
                       User currentUser,
                       Subject subject,
                       User userLogged,
                       boolean fillSource,
                       boolean deleteMongoFlag,
                       boolean secure) {
        ThingDeleter.getInstance().delete(recursivelyStack, thing, validateVisibility,
                transactionDate, executeTickle, currentUser, subject, userLogged, fillSource, deleteMongoFlag, secure);
    }
}
