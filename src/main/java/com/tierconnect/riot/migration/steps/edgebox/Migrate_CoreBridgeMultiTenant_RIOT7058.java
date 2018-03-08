package com.tierconnect.riot.migration.steps.edgebox;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.EdgeboxRule;
import com.tierconnect.riot.iot.entities.QEdgeboxRule;
import com.tierconnect.riot.iot.services.EdgeboxRuleService;
import com.tierconnect.riot.iot.services.EdgeboxRuleServiceBase;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.Pagination;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_CoreBridgeMultiTenant_RIOT7058 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_CoreBridgeMultiTenant_RIOT7058.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateBridges();
    }

    private void migrateBridges() {
        logger.info("Start Migrating coreBridge...");

        logger.info("Setting the group of coreBridge to all rules...");
        EdgeboxService edgeboxService = EdgeboxService.getInstance();
        Edgebox edgebox = edgeboxService.selectByCode("MCB");

        EdgeboxRuleService edgeboxRuleService = EdgeboxRuleServiceBase.getInstance();
        List<EdgeboxRule> rules = edgeboxRuleService.selectByEdgeboxId(edgebox.getId());
        for (EdgeboxRule rule : rules) {
            rule.setGroup(edgebox.getGroup());
        }

        logger.info("Assigning root as a group of coreBridge...");
        Group rootGroup = GroupService.getInstance().getRootGroup();
        edgebox.setGroup(rootGroup);

        logger.info("Rebuild Edgebox rules with new format for Esper Rules...");
        rebuildEsperRules();

        logger.info("End Migrating coreBridge");
    }

    private void rebuildEsperRules()
    {
        EdgeboxRuleService ruleService  = EdgeboxRuleServiceBase.getInstance();

        String extra = "";
        List<String> getExtraPropertyNames = new ArrayList<String>();
        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();

        Pagination pagination = new Pagination(1, -1);
        BooleanBuilder be = new BooleanBuilder();
        be = be.and( QueryUtils.buildSearch( QEdgeboxRule.edgeboxRule, "" ) );

        Long count = EdgeboxRuleService.getInstance().countList( be );

        for( EdgeboxRule edgeboxRule : ruleService.listPaginated( be, pagination, "" ) )
        {
            Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( edgeboxRule, extra, getExtraPropertyNames);
            edgeboxRule.setRule( tokenizerRule( edgeboxRule.getName(), edgeboxRule.getRule()));

            ruleService.update( edgeboxRule );

            list.add( publicMap );
        }
    }

    //rebuild esper rules
    //this function takes the rule and check if the rule is in the new format,
    //new format means, they are using the MessageEventType in the select clause
    //if not is in the new format, apply regular expression to get the thingType of the rule, and then
    //rebuild the rule with the new format
    //the new rule format has this structure:
    //   select * from messageEventType where udf('thingTypeCode') = 'your_thing_type_code') and ( your_original_condition )
    private String tokenizerRule(String name, String rule)
    {
        //System.out.println(rule);

        //because this function is auto-converting rules from previous format to the new one,
        //we are dealing with two thingTypes, the first in the from clause, and the second in the where clause
        String whereThingType = "";
        String fromThingType = "";
        StringBuilder condition = new StringBuilder( "" );

        rule = rule + " ";
        char car;
        boolean select = false;
        boolean asterisk = false;
        boolean from = false;
        boolean where = false;


        StringBuilder token = new StringBuilder("");

        StringBuilder strPattern = new StringBuilder( "" );
        strPattern.append( "udf" );    //udf//
        strPattern.append( "\\s*" );   //udf   //
        strPattern.append( "\\(" );    //udf   (//
        strPattern.append( "\\s*" );   //udf   (   //
        strPattern.append( "['\"]" );  //udf   (   "//
        strPattern.append( "thingTypeCode" );  //udf   (   "thingTypeCode//
        strPattern.append( "['\"]" );  //udf   (   "thingTypeCode"//
        strPattern.append( "\\s*" );   //udf   (   "thingTypeCode"  //
        strPattern.append( "\\)" );    //udf   (   "thingTypeCode"  )//
        strPattern.append( "\\s*" );   //udf   (   "thingTypeCode"  )   //
        strPattern.append( "=" );      //udf   (   "thingTypeCode"  )  =//
        strPattern.append( "\\s*" );   //udf   (   "thingTypeCode"  )  =  //
        strPattern.append( "['\"]" );  //udf   (   "thingTypeCode"  )  =  "//
        strPattern.append( "(.*?)" );  //udf   (   "thingTypeCode"  )  =  "assets//
        strPattern.append( "['\"]" );  //udf   (   "thingTypeCode"  )  =  "assets"//
        strPattern.append( "\\s*" );  //udf   (   "thingTypeCode"  )  =  "assets" //
        Pattern pattern = Pattern.compile( strPattern.toString() );
        Matcher matcher = pattern.matcher( rule );
        while (matcher.find()) {
            whereThingType = matcher.group(1);
        }

        for (int i=0; i < rule.length() ; i++ ) {
            car = rule.charAt( i );
            if ( car == '\n' || car == '\r' ||  car == ' ' || car == '\t' || i == rule.length()-1) {

                //validate every important token
                if ( token.toString().toLowerCase().equals( "select" ) && !select) {
                    select = true;
                    token = new StringBuilder("");
                }
                if ( token.toString().toLowerCase().equals( "*" ) && select ) {
                    asterisk = true;
                    token = new StringBuilder("");
                }
                if ( token.toString().toLowerCase().equals( "from" ) && asterisk ) {
                    from = true;
                    token = new StringBuilder("");
                }
                if ( token.toString().toLowerCase().equals( "where" ) && fromThingType.length() > 0 ) {
                    where = true;
                    token = new StringBuilder("");
                }
                //fromThingType, we have the default thingtype
                if (select && asterisk && from && fromThingType.length() == 0) {
                    fromThingType = token.toString();
                    token = new StringBuilder("");
                }

                if( token.length() > 0)
                {
                    if (select && asterisk && from && fromThingType.length() > 0 && where ) {
                        condition.append( token.toString() );
                        condition.append( car );
                    }
                    token = new StringBuilder("");
                }
                continue;
            }

            token = token.append( car );
        }

        StringBuilder newRule   = new StringBuilder( "" );

        if (!whereThingType.equals("") ) {
            //fromThingType = whereThingType;
            if ( fromThingType.equals( "messageEventType" )) {
                newRule.append( rule );
            } else {
                newRule.append( "select * from messageEventType where udf('thingTypeCode') = '" + whereThingType + "'" );
                if( condition.toString().length() > 0 )
                {
                    newRule.append( " and ( " + condition.toString() + " ) " );
                }
            }
        }
        else
        {
            newRule.append( "select * from messageEventType where udf('thingTypeCode') = '" + fromThingType + "'" );
            if( condition.toString().length() > 0 )
            {
                newRule.append( " and ( " + condition.toString() + " ) " );
            }
        }
        logger.info("converting EdgeboxRule " + name);
        logger.info("from: " + rule);
        logger.info("to: " + newRule.toString() );

        return newRule.toString();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
