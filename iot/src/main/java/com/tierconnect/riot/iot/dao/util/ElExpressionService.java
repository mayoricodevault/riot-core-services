package com.tierconnect.riot.iot.dao.util;

import com.mongodb.BasicDBObject;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingField;
import com.tierconnect.riot.sdk.dao.UserException;
import de.odysseus.el.ExpressionFactoryImpl;
import de.odysseus.el.tree.TreeStore;
import de.odysseus.el.tree.impl.Builder;
import de.odysseus.el.tree.impl.Cache;
import de.odysseus.el.util.SimpleContext;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Created by fflores on 2/24/16.
 */
public class ElExpressionService{
    private static Logger logger = Logger.getLogger( ElExpressionService.class );

    static
    {
        logger.setLevel( Level.DEBUG );
    }

    ExpressionFactory factory;

    SimpleContext context;

//    public static void main( String[] args ) throws ClientProtocolException, URISyntaxException, IOException
//    {
//        ConnectionParameters.setConnectionParameters( "localhost", 8080, "/riot-core-services", "root" );
//
//        ThingTypeService.loadThingTypes();
//        ZoneService.loadZones();
//
//        ThingWrapper tm = new ThingWrapper();
//
//        Thing t = new Thing();
//
//        t.setId( 1 );
//        t.setThingTypeId( 1 );
//        t.setSerialNumber( "AE12345" );
//        t.setName( "Fork Lift #1" );
//        t.setName( "1" );
//
//        tm.setThing( t );
//
//        tm.setThingMessage( new ThingMessage( "default_rfid_thingtype", tm.getThing().getSerialNumber() ) );
//
//        ElExpressionService ees = new ElExpressionService();
//        ees.initialize( tm );
//
//        StringBuffer in = new StringBuffer();
//        in.append( "\n" );
//        in.append( "serialNumber=${serialNumber}\n" );
//        in.append( "name=${name}\n" );
//        in.append( "name+1=${name+1}\n" );
//        in.append( "parent.color=${parent.color}\n" );
//        //in.append( "childByThingTypeCode.default_rfid_tag.serialNumber=${childByThingTypeCode.default_rfid_tag.serialNumber}\n" );
//
//        String out = ees.evaluate( in.toString() );
//
//        System.out.println( "in='" + in + "'" );
//        System.out.println( "out='" + out + "'" );
//    }

    public void initialize(Map<String,Object> values, Thing thing)
    {
        try {
            TreeStore store = new TreeStore(new Builder(), new Cache(5000));
            factory = new ExpressionFactoryImpl(store);
//            java.util.Properties properties = new java.util.Properties();
//            properties.put("javax.el.cacheSize", "5000");
//            factory = new ExpressionFactoryImpl(properties);
            context = new SimpleContext();
            context.setELResolver(new ThingResolver(thing));
            context.setVariable("tenantCode", factory.createValueExpression(thing!=null?thing.getGroup().getTenantGroup().getCode():"", String.class));
            if (values != null){
                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    context.setVariable(entry.getKey(), factory.createValueExpression(entry.getValue(), Object.class));
                }
            }
            context.setFunction("long", "toHexString", java.lang.Long.class.getMethod("toHexString", long.class));
            context.setFunction("String", "leftPad", StringUtils.class.getMethod("leftPad", String.class, int.class, String.class));
            context.setFunction("String", "upperCase", StringUtils.class.getMethod("upperCase", String.class));
            context.setFunction("","count", com.tierconnect.riot.iot.dao.util.ThingResolver.class.getMethod("count",String.class,String.class));
            context.setFunction("String", "upperCase", StringUtils.class.getMethod("upperCase", String.class));

            context.setFunction("boolean", "contains", StringUtils.class.getMethod("contains", String.class,String.class));
            context.setFunction("boolean", "equals", StringUtils.class.getMethod("equals", String.class, String.class));
            context.setFunction("", "indexOf", StringUtils.class.getMethod("indexOf", String.class, String.class, int.class));
            context.setFunction("boolean", "isEmpty", StringUtils.class.getMethod("isEmpty", String.class));
            context.setFunction("String", "replace", StringUtils.class.getMethod("replace",String.class,String.class,String.class));
            context.setFunction("String", "substring", StringUtils.class.getMethod("substring",String.class,int.class,int.class));
            context.setFunction("String", "length", StringUtils.class.getMethod("length",String.class));
            context.setFunction("String", "lowerCase", StringUtils.class.getMethod("lowerCase", String.class));
            context.setFunction("String", "trim", StringUtils.class.getMethod("trim", String.class));
            context.setFunction("","decoderRing001", com.tierconnect.riot.iot.dao.util.ThingResolver.class.getMethod("decoderRing001",String.class));
            context.setFunction("","decoderRing002", com.tierconnect.riot.iot.dao.util.ThingResolver.class.getMethod("decoderRing002",String.class));
            context.setFunction("","currentUser", com.tierconnect.riot.iot.dao.util.ThingResolver.class.getMethod("currentUser",String.class));
            context.setFunction("","epcDecode", com.tierconnect.riot.iot.dao.util.ThingResolver.class.getMethod("epcDecode",String.class, String.class));
            context.setFunction("","countParentChild", com.tierconnect.riot.iot.dao.util.ThingResolver.class.getMethod("countParentChild",long.class,String.class,String.class));
            context.setFunction("","countThingTypeUDF", com.tierconnect.riot.iot.dao.util.ThingResolver.class.getMethod("countThingTypeUDF",long.class,String.class,String.class,String.class));
        } catch (NoSuchMethodException e) {
            logger.error(e);
            throw new UserException("There is an error evaluating functions in formula expression", e);
        } catch (Exception e) {
            logger.error("Error initializing Expression. ", e);
            throw new UserException("Error initializing Expression.", e);
        }
    }

    public Object evaluate(String expression,boolean validateProperties)
    {
        Object result = "";
        try {
            ValueExpression e;
            e = factory.createValueExpression( context,expression,Object.class);
            Object aux = e.getValue(context);
            if (aux != null) {
                result = aux;
                if ((aux instanceof BasicDBObject) || (aux instanceof Map)) {
                    if (((Map) aux).containsKey("value")) result = ((Map) aux).get("value");
                    if (((Map) aux).containsKey("name")) result = ((Map) aux).get("name");
                }
                if (aux instanceof ThingField) {
                    result =  ((ThingField) aux).getValue();
                }else if (aux instanceof Double || aux instanceof Float || aux instanceof BigDecimal) {
                    result = roundedValue(aux, 5);
                }
            }
        } catch( de.odysseus.el.tree.TreeBuilderException el ) {
            // when there is a parsing error
            logger.error(el);
            // a general message is returned because it cannot be technical for users
            throw new UserException("There is a parse error in formula: " + expression, el);
        } catch (javax.el.PropertyNotFoundException er){
            // when UDFs used in formula don't exist in thingTypeFields, because they don't have values
            if (validateProperties) {
                throw er;
            } else {
                return null;
            }
        } catch (javax.el.ELException ex) {
            // when parameters for function do not match
            logger.error(ex);
            throw new UserException(ex.getMessage(), ex);
        }
        return result;
    }

    public Object roundedValue(Object value, Integer maxDecimals){
        return roundedValue(value, maxDecimals, 1);
    }

    public Object roundedValue(Object value, Integer maxDecimals, Integer minDecimals){
        NumberFormat formatter = NumberFormat.getInstance(Locale.US);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        formatter.setGroupingUsed(false);
        formatter.setMaximumFractionDigits(maxDecimals);
        formatter.setMinimumFractionDigits(minDecimals);
        if( value instanceof Float ){
            return new Float(formatter.format(value));
        } else if( value instanceof Double ){
            return new Double(formatter.format(value));
        } else if( value instanceof BigDecimal ){
            return new BigDecimal(formatter.format(value));
        } else if( value instanceof Integer ){
            return new Integer((new Float(formatter.format(value))).intValue());
        } else if( value instanceof Long ){
            return new Long((new Float(formatter.format(value))).intValue());
        } else {
            return value;
        }
    }

}