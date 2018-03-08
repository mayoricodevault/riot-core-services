package com.tierconnect.riot.iot.services;

import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.entities.QUser;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.iot.dao.util.FormulaUtil;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.ServerException;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.servlet.security.ApiKeyToken;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.*;

/**
 * Created by rchirinos on 3/11/16.
 */
public class UpdateChildrenFormulaTrigger implements Runnable
{
    private static Logger logger = Logger.getLogger( UpdateChildrenFormulaTrigger.class );

    private Stack<Long> recursivelyStack;
    private Long thingId;
    private Map<String, Object> udf;
    private String userApiKey;
    private Long userId;

    public UpdateChildrenFormulaTrigger(
            Stack<Long> recursivelyStack
            , Long thingId
            , Map<String, Object> udf
            , String userApiKey
            , Long userId)
    {
        this.thingId = thingId;
        this.udf = udf;
        this.userApiKey = userApiKey;
        this.userId = userId;
        this.recursivelyStack = recursivelyStack;
    }

    /**
     * This method executes the logic of the Job
     *
     */
    @Override
    public void run()
    {
        logger.info("Start Thread");
//        synchronized(userId)
//        {
        Transaction transaction = null;
//            while (!Thread.currentThread().isInterrupted())
//            {
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        transaction = session.getTransaction();
        transaction.begin();
        try {

            RiotShiroRealm.initCaches();
            ApiKeyToken token = new ApiKeyToken(UserService.getInstance().getUserDAO().selectBy(QUser.user.id.eq(userId)).getApiKey());
            PermissionsUtils.loginUser(token);

            executeUpdateChildren(recursivelyStack, thingId, udf, userId);
            transaction.commit();

        } catch (Exception ex) {
            HibernateDAOUtils.rollback(transaction);
        }

    }

    public void executeUpdateChildren(Stack<Long> recursivelyStack, Long thingId, Map<String, Object> udf, Long userId )
    {
        try {
            //Get List of fields to update
            //list of Native Children  and children UDF, just in a unique list
            User currentUser = UserService.getInstance().get(userId);

            Map<ThingType, List<ThingTypeField>> lstThingTypeField = getListChildrenFields(thingId);
            Map<ThingType, List<ThingTypeField>> finalFields = getFieldsToUpdate(lstThingTypeField, thingId, udf);
            Map<String, Object> udfsToUpdate = getThingTypeFieldsByUdf(lstThingTypeField, finalFields, udf);
            Map<String, Object> mapResponse = new HashMap<>();

            //Iterate final fields
            for (Map.Entry<ThingType, List<ThingTypeField>> entry : finalFields.entrySet()) {
                ThingType thingType = entry.getKey();
                List<ThingTypeField> lstTTFieldValue = entry.getValue();

                String where = "thingTypeCode='" + thingType.getCode() + "'&groupCode='" + thingType.getGroup().getCode() + "'";
                String only = "_id,thingTypeCode,name,serialNumber";

                mapResponse = ThingService.getInstance().processListThings(
                        10000 //pageSize
                        , 1     //pageNumber
                        , null  //order
                        , where //where
                        , null  //extra
                        , only  //only
                        , null  //groupBy
                        , null //visibilityGroupId
                        , null //upVisibility
                        , null //downVisibility
                        , false /*treeView*/
                        , currentUser
                        , false);

                List<Map<String, Object>> result = (List<Map<String, Object>>) mapResponse.get("results");
                if (result != null && result.size() > 0) {
                    for (Object things : result) {
                        Date storageDate = new Date();
                        Map<String, Object> thingMap = (Map<String, Object>) things;

                        ThingService.getInstance().update(
                                recursivelyStack
                                , Long.parseLong(thingMap.get("_id").toString())
                                , (String) thingMap.get("thingTypeCode")
                                , null //group
                                , null //name
                                , (String) thingMap.get("serialNumber")
                                , null //parent
                                , null//udfsToUpdate//this.udf
                                , null //children
                                , null //childrenUdf
                                , true //executeTickle
                                , true //validateVisibility
                                , storageDate //transactionDate
                                , false
                                , currentUser //disableFMCLogic
                                , true);

                        logger.info("Thread Updated: " + thingMap.get("_id"));
                    }
                }
            }



        } catch (Exception ex) {
            if (ex instanceof UserException) {
                logger.warn(ex.getMessage());
            } else if (ex instanceof ServerException) {
                logger.error(ex.getMessage());
            } else {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    /**
     * This method returns all list of Native Children  and children UDF, just in a unique list
     * @param thingId
     * @return
     */
    public static Map<ThingType, List<ThingTypeField>> getListChildrenFields(Long thingId)
    {
        Thing thing = ThingService.getInstance().get(thingId);
        //Get List of native children thingTypes
        List<ThingType> lstThingType = thing.getThingType().getChildren();

        //Get List of children UDFs thingTypes
        List<ThingType> lstThingTypeUdf = ThingTypeService.getInstance().getChildrenUdfThingTypeCode(thing.getId());

        //Get List of thingTypeFields with expressions in native children and children UDFs
        Map<ThingType, List<ThingTypeField>>  lstThingTypeField = FormulaUtil.getListOfFieldsWithExpression(lstThingType);
        Map<ThingType, List<ThingTypeField>>  lstThingTypeFieldIdUdf = FormulaUtil.getListOfFieldsWithExpression(lstThingTypeUdf);

        //Get a unique list of thing type fields
        lstThingTypeField.putAll(lstThingTypeFieldIdUdf);

        return lstThingTypeField;
    }
    /**
     * This method get a list of thingTypeFields Filters only things with the value of the father: "${parent."
     * @param lstThingTypeField
     * @param thingId
     * @param udf udf's of the parent thing
     * @return
     */
    public static Map<ThingType, List<ThingTypeField>> getFieldsToUpdate(
            Map<ThingType, List<ThingTypeField>> lstThingTypeField
            , Long thingId
            , Map<String, Object> udf)
    {
        Map<ThingType, List<ThingTypeField>>  finalFields = new HashMap<>();
        if(lstThingTypeField!=null && lstThingTypeField.size()>0)
        {
            //Filter only things with the value of the father: "${parent."
            for (Map.Entry<ThingType, List<ThingTypeField>> entry : lstThingTypeField.entrySet())
            {
                ThingType thingType = entry.getKey();
                List<ThingTypeField> lstTTFieldValue = entry.getValue();
                List<ThingTypeField> lstNewTTFieldValue = new ArrayList<>();
                for(ThingTypeField thingTypeField: lstTTFieldValue)
                {
                    if(thingTypeField.getDefaultValue().contains("${parent."))
                    {
                        lstNewTTFieldValue.add(thingTypeField);
                    }
                }
                if(lstNewTTFieldValue!=null && lstNewTTFieldValue.size()>0)
                {
                    finalFields.put(thingType,lstNewTTFieldValue);
                }
            }

        }
        return finalFields;
    }

    /**
     * This method filters only thing type field with have the same udfValues
     * @param finalFields
     * @return
     */
    public static Map<String, Object> getThingTypeFieldsByUdf(
            Map<ThingType, List<ThingTypeField>> lstThingTypeField
            , Map<ThingType, List<ThingTypeField>>  finalFields
            , Map<String, Object> udf)
    {
        Map<String, Object> result = new HashMap<>();
        if(finalFields!=null && finalFields.size()>0)
        {
            for (Map.Entry<ThingType, List<ThingTypeField>> entry : lstThingTypeField.entrySet())
            {
                ThingType thingType = entry.getKey();
                List<ThingTypeField> lstTTFieldValue = entry.getValue();

                if(lstTTFieldValue!=null && lstTTFieldValue.size()>0)
                {
                    Map<String, Object> values = FormulaUtil.getMapValues(udf);
                    for(ThingTypeField thingTypeField: lstTTFieldValue)
                    {
                        for (Map.Entry<String, Object> entryData : values.entrySet())
                        {
                            //Check Expression Children equals udfValue
                            if(thingTypeField.getDefaultValue().contains("${parent."+entryData.getKey()))
                            {
                                Map<String, Object> value = new HashMap<>();
                                value.put("value", entryData.getValue());
                                result.put(thingTypeField.getName(),value);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

}
