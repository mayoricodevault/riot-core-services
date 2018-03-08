package com.tierconnect.riot.appcore.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.BooleanPath;
import com.tierconnect.riot.appcore.entities.QGroup;
import com.tierconnect.riot.appcore.services.GroupService;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.Path;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.path.EntityPathBase;
import com.tierconnect.riot.sdk.dao.UserException;

public class QueryUtils {
	
	static final Logger logger = Logger.getLogger( QueryUtils.class );
	
	public static OrderSpecifier[] getOrderFields(EntityPathBase base, String orderString){
		if(StringUtils.isBlank(orderString)){
			return null;
		}
		
		String tokens[] = orderString.split(",",-1);
		OrderSpecifier orderFields[] = new OrderSpecifier[tokens.length];
		
		for(int i=0;i<tokens.length;i++){
			orderFields[i] = getOrderField(base,tokens[i]);
		}

		return orderFields;
	}

	public static OrderSpecifier getOrderField(EntityPathBase base, String orderString){
		if(orderString.endsWith(":asc") == false && orderString.endsWith(":desc") == false){
			throw new UserException("orderString should finish with \":asc\" or \":desc\"");
		}

		int sortSeparatorIndex = orderString.indexOf(":");
		String pathString = orderString.substring(0,sortSeparatorIndex);
		try {
			Object field = getField(base, pathString);

			String ascDesc = orderString.endsWith("desc") ? "desc" : "asc"; 
			return (OrderSpecifier) field.getClass().getMethod(ascDesc).invoke(field);
		}catch (Exception e){
			throw new UserException("Invalid orderString:"+orderString, e);
		}
	}
	
	public static Path getField(EntityPathBase base,String pathString){
		try {
			Object orderField = base;
			for(String token : pathString.split("\\.",-1)){
				String field = token;
				
				orderField = orderField.getClass().getField(field).get(orderField);
			}
			return (Path) orderField;
		}catch (Exception e){
			throw new UserException("Invalid field:"+pathString, e);
		}
	}
	
	public static Map splitWithEscaping(String str, char... _separators){
        Map result = new HashMap();
		List<String> tokens = new LinkedList<>();
		StringBuffer buffer = new StringBuffer();
        char separator = '\0';
		for(int i=0;i<str.length();i++){
            char c = str.charAt(i);

			if(c == '\\'){
				if(i == str.length() - 1){
					throw new UserException("Invalid escape String at " + i);
				}
				char c2 = str.charAt(i+1);
				if(c2 == 'N'){//Null
					if(buffer.length() > 0){
						buffer.append("\\N");
					}else{
						buffer = null;
					}					
				}else{
					buffer.append(c2);
				}
				i += 1;//Escaping character
			}else {
                boolean separatorFound = false;
                for (int i1 = 0; i1 < _separators.length; i1++) {
                    if(c == _separators[i1]){
                        separator = _separators[i1];
                        separatorFound = true;
                    }
                }
				if(separatorFound){
					if(buffer == null){
						tokens.add(null);  //This if branch never gets executed
					}else{
						tokens.add(buffer.toString());
					}
					buffer = new StringBuffer();
				}else{
					buffer.append(c);
				}
			}
		}
		if(buffer == null){
			tokens.add(null);  //This if branch never gets executed
		}else if(buffer.length() > 0){
			tokens.add(buffer.toString());
		}
		result.put("separator", separator);
        result.put("tokens", tokens.toArray(new String[tokens.size()]));
        return result;
	}

    //With escaping
    public static int getIndexOf(String s, char c) {
        for (int i=0; i< s.length(); i++) {
            if (s.charAt(i) == c) {
                if (i == 0 || s.charAt(i-1) != '\\') {
                    return  i;
                }
            }
        }
        return -1;
    }

    //With escaping
    public static int getIndexOfEndParenthesis(String s, int start) {
        if (s.charAt(start) != '(') {
            throw new RuntimeException("Error start parenthesis not found");
        }
        int n=1;
        for (int i=start+1; i< s.length(); i++) {
            if (s.charAt(i) == '(') {
                if (s.charAt(i-1) != '\\') { //for Escaping
                    n++;
                }
            }
            if (s.charAt(i) == ')') {
                if (s.charAt(i-1) != '\\') { //for Escaping
                    n--;
                }
            }
            if (n == 0) {
                return i;
            }
        }
        return -1;
    }

    public static BooleanBuilder buildSearch(EntityPathBase base, String searchString){
        if (StringUtils.isBlank(searchString)) {
            return new BooleanBuilder();
        }
        //1st Priority Parenthesis
        int startParenthesisIndex = getIndexOf(searchString, '(');
        if (startParenthesisIndex != -1) {
            //a&(
            //(
            int endParenthesisIndex =  getIndexOfEndParenthesis(searchString, startParenthesisIndex);
            if (endParenthesisIndex == -1) {
                throw new RuntimeException("Matching closing parenthesis was not found.");
            }
            boolean negatedParenthesis=startParenthesisIndex>0 && searchString.charAt(startParenthesisIndex-1) == '!';

            BooleanBuilder center = buildSearch(base, searchString.substring(startParenthesisIndex + 1, endParenthesisIndex));
            if (negatedParenthesis) {
                center = center.not();
            }

            if (startParenthesisIndex >= 2) {
                BooleanBuilder left = buildSearch(base, searchString.substring(0, startParenthesisIndex - (negatedParenthesis?2:1)));
                int aux = startParenthesisIndex - (negatedParenthesis?2:1);
                char operator = searchString.substring(aux, aux+1).charAt(0);
                center = andOrExpressions(left, center, operator);
            }
            //)
            //)|a
            if (endParenthesisIndex < searchString.length()-2) {
                BooleanBuilder right = buildSearch(base, searchString.substring(endParenthesisIndex + 2, searchString.length()));
                char operator = searchString.substring(endParenthesisIndex +1, endParenthesisIndex +2).charAt(0);
                center = andOrExpressions(center, right, operator);
            }
            return center;
        }
        //2nd Priority And
        {
            int startAndIndex = getIndexOf(searchString, '&');
            if (startAndIndex != -1) {
                BooleanBuilder left = buildSearch(base, searchString.substring(0, startAndIndex));
                BooleanBuilder right = buildSearch(base, searchString.substring(startAndIndex + 1, searchString.length()));
                return andOrExpressions(left, right, '&');
            }
        }

        //3er Priority Or
        {
            int startOrIndex = getIndexOf(searchString, '|');
            if (startOrIndex != -1) {
                BooleanBuilder left = buildSearch(base, searchString.substring(0, startOrIndex));
                BooleanBuilder right = buildSearch(base, searchString.substring(startOrIndex + 1, searchString.length()));
                return andOrExpressions(left, right, '|');
            }
        }
        return new BooleanBuilder(getCondition(base, searchString));
    }

    private static BooleanBuilder andOrExpressions(BooleanBuilder left, BooleanBuilder right, char operator) {
        BooleanBuilder result = new BooleanBuilder();
        if (operator == '&') {
            result = left.and(right);
        }
        if (operator == '|') {
            result = left.or(right);
        }
        return result;
    }


	private static BooleanBuilder getCondition(EntityPathBase base,String conditionString){
		Map result = splitWithEscaping(conditionString, '=', '~', '<', '>'); //operator ^ removed because no implenentation
        String[] operands = (String[]) result.get("tokens");
        char operator  = (Character) result.get("separator");

		if(operands.length == 0) {
			throw new UserException("Invalid Condition: " + conditionString);
		}

        String operand0 = operands[0];
		String operand1 = null;
		if (operand0.equals("serial")) {
			operand1 = operands.length >=2 ? (operands[1]).toUpperCase() : "";
		} else {
			operand1 = operands.length >=2 ? (operands[1]): "";
		}
        Object field = getField(base, operand0);

        if(operands.length != 2 ) {
            if(!field.getClass().equals(com.mysema.query.types.path.StringPath.class)){
                throw new UserException("Invalid Condition: " + conditionString);
            }
        }

		Object value = null;
        if(operand1 != null){
            if (operand1.equals("null")) {
                value = null;
            } else if(field.getClass().equals(com.mysema.query.types.path.NumberPath.class)){
				value = Double.parseDouble(operand1);
			} else if(field.getClass().equals(com.mysema.query.types.path.StringPath.class)){
				value = operand1;
			} else if (field.getClass().equals(QGroup.class)){
                value = Long.parseLong(operand1);
            } else if (field.getClass().equals(BooleanPath.class)) {
                value = Boolean.parseBoolean(operand1);
            }
            else{
				throw new UserException("Expected Number or String condition");
			}
		}

		BooleanBuilder expr;
		try {
			Method method = null;
			if(value != null){
                if (operator == '=') {
                    if(field.getClass().equals(com.mysema.query.types.path.StringPath.class) && value.equals("")){
                        Method method1 = field.getClass().getMethod("eq", Object.class);
                        Method method2 = field.getClass().getMethod("isNull");
                        expr = new BooleanBuilder((Predicate) method1.invoke(field, value)).or((BooleanExpression) method2.invoke(field));
                        return expr;
                    } else {
                        method = field.getClass().getMethod("eq", Object.class);
                    }
                } else if (operator == '~') {
                    method = field.getClass().getMethod("like", String.class);
                } else if (operator == '<') {
                    if (field.getClass().equals(QGroup.class)) {
                        BooleanBuilder b = new BooleanBuilder(VisibilityUtils.limitVisibilityPredicate(GroupService.getInstance().get((Long) value), (QGroup) field, false, true));
                        return b;
                    } else {
                        method = field.getClass().getMethod("lt", Number.class);
                    }
                } else if (operator == '>') {
                    method = field.getClass().getMethod("gt", Number.class);
                } else {
                    throw new UserException("Operator "+operator+ " not supported");
                }
                expr = new BooleanBuilder((Predicate) method.invoke(field, value));
			}else{
				method = field.getClass().getMethod("isNull");
				expr = new BooleanBuilder((Predicate) method.invoke(field));
			}
		} catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
			throw new UserException("Invalid conditionString: " + conditionString, e);
		}
		
		return expr;
	}

	private static Object getFielsByGet( Object base, String fieldsString )
	{
		Object result = base;		
		for( String token : fieldsString.split( "\\.", -1 ) )
		{
			if( base == null || StringUtils.isBlank(token) )
			{
				break;
			}

			try 
			{
				String str = "get" + StringUtils.capitalize( token );
				logger.debug( "str=" + str );
				result = result.getClass().getMethod(str).invoke(result);
			} 
			catch (Exception e) 
			{
				throw new UserException( "Invalid fieldString: " + token + " e=" + e, e );
			}
		}
		return result;
	}

	private static Object customIndividualMap( Object entity, String fieldsString )
	{
		Object obj = null;
		try 
		{
			Object field = getFielsByGet( entity, fieldsString );
			if( field != null )
			{
				//It can be null, like parent
				//logger.debug( "fieldString=" + fieldsString + " field=" + field );
				if( field instanceof Collection )
				{
					//logger.debug( "IS A COLLECTION" );
					List<Map<String, Object>> list = new LinkedList<>();
					for( Iterator i = ((Iterable) field).iterator(); i.hasNext(); )
					{
						Object o = i.next();
						//logger.debug( "o=" + o );
						list.add( (Map<String, Object>) o.getClass().getMethod("publicMap").invoke(o) );
					}
					obj = list;
				}
				else
				{
					//logger.debug( "NOT A COLLECTION" );
					obj =  field.getClass().getMethod("publicMap").invoke(field);
				}
			}
			else
			{
				obj = new HashMap<>();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new UserException( "Invalid FieldString: " + fieldsString + " e=" + e , e);
		}
		
		return obj;
	}

	/**
	 * @param entity
	 * @param properties - a cvs separated list of "extra" properties
	 * @return
	 */
	public static Map<String,Object> mapWithExtraFieldsNested( Object entity, Map<String,Object> publicMap,  String properties, List<String> controllerExtraProperties )
	{
		Map<String, Object> extraProperties = new HashMap<>();


        if (StringUtils.isNotBlank(properties)){
            for (String str : properties.split(",", -1)) {
                String[] propertyArray = str.trim().split("\\.", -1);
                parseTree(extraProperties, propertyArray);
            }
        }

        publicMap.putAll(buildMapWithExtraFields(entity, extraProperties, controllerExtraProperties));

		return publicMap;
	}

    private static Map<String, Object> buildMapWithExtraFields(Object entity, Map<String, Object> extraProperties, List<String> controllerExtraProperties) {


        Map<String, Object> baseMap;

        try {
            baseMap = (Map<String, Object>) entity.getClass().getMethod("publicMap").invoke(entity);
        } catch (Exception e) {
            throw new UserException("base does not have a publicMap method " + entity.getClass().getName(), e);
        }

        if (baseMap != null && baseMap.containsKey("serial")) {
            baseMap.put("serial", StringUtils.upperCase((String) baseMap.get("serial")));
        }

        for (Map.Entry<String, Object> field : extraProperties.entrySet()) {

            if (!controllerExtraProperties.contains(field.getKey())) {
                if (!((Map)field.getValue()).entrySet().isEmpty()) {

                    Object subEntities = getFielsByGet(entity, field.getKey());

                    if (subEntities instanceof Collection) {
                        List<Map<String,Object>> propertiesList = new ArrayList<>();
                        for (Object subEntity : (Collection) subEntities) {
                            propertiesList.add(buildMapWithExtraFields(subEntity, (Map)field.getValue(), controllerExtraProperties));
                        }
                        baseMap.put(field.getKey(), propertiesList);
                    } else {
                        Object subEntity = subEntities;
                        baseMap.put(field.getKey(), buildMapWithExtraFields(subEntity, (Map)field.getValue(), controllerExtraProperties));
                    }
                }else{
                    baseMap.put(field.getKey(), customIndividualMap(entity, field.getKey()));
                }
            } else {
                //this will be filled by addToPublicMap method
                continue;
            }
        }

        return baseMap;
    }


    /**
     * This method removes from map all property EXCEPT what is in 'only' and 'extra'.
     * <p/>
     * This is a quick and dirty implementation to implement functionality.
     * This should be implemented at the lowest possible level in the stack.
     *
     * @param map
     * @param only  a csv list of property names
     * @param extra - a csv list of property names
     *              <p/>
     *              TODO: handle nested properties
     */
    public static void filterProjectionNested(Map<String, Object> map, String only, String extra) {
        if (only == null || "".equals(only)) {
            return;
        }

        Map<String, Object> extraProperties = new HashMap<>();
        Map<String, Object> onlyProperties = new HashMap<>();

        if (StringUtils.isNotBlank(extra)){
            for (String str : extra.split(",", -1)) {
                String[] propertyArray = str.trim().split("\\.", -1);
                parseTree(extraProperties, propertyArray);
            }
        }

        if (StringUtils.isNotBlank(only)){
            for (String str : only.split(",", -1)) {
                String[] propertyArray = str.trim().split("\\.", -1);
                parseTree(onlyProperties, propertyArray);
            }
        }



        filterMap(map, onlyProperties, extraProperties);

    }

    private static void filterMap(Map<String, Object> map, Map<String, Object> onlyProperties, Map<String, Object> extraProperties){
        Iterator<String> i = map.keySet().iterator();
        while(i.hasNext()) {
            String key = i.next();
            if(extraProperties != null && !extraProperties.keySet().contains(key) && onlyProperties != null && !onlyProperties.keySet().contains(key)){
                if(!key.equals("id") || filterId(onlyProperties,extraProperties)){
                    i.remove();
                }
            }else if(onlyProperties != null && map.get(key) instanceof List){
                for(Map<String, Object> element : (List<Map<String,Object>>) map.get(key)){
                    filterMap((Map)element,
                            (Map)onlyProperties.get(key),
                            (Map)extraProperties.get(key));
                }
            }else if(onlyProperties != null && map.get(key) instanceof Map){
                filterMap((Map)map.get(key),
                        (Map)onlyProperties.get(key),
                        (Map)extraProperties.get(key));
            }
        }
    }

    private static boolean filterId(Map<String, Object> onlyProperties, Map<String, Object> extraProperties){

        int fieldCounter = 0;
        for(Map.Entry<String,Object> element : onlyProperties.entrySet()){
            if(!extraProperties.containsKey(element.getKey()) && !element.getKey().equals("id")){
                fieldCounter ++;
            }
        }

        return fieldCounter > 0 ;

    }


    private static void parseTree(Map<String, Object> propertiesTree, String[] propertiesArray){

        if(propertiesArray.length == 1){
            propertiesTree.put(propertiesArray[0], new HashMap<String, Object>());
        }else{
            if(!propertiesTree.containsKey(propertiesArray[0])){
                propertiesTree.put(propertiesArray[0], new HashMap<String, Object>());
            }
            parseTree((Map<String,Object>)propertiesTree.get(propertiesArray[0]), Arrays.copyOfRange(propertiesArray, 1, propertiesArray.length));
        }
    }

    /**
     * @param entity
     * @param properties - a cvs separated list of "extra" properties
     * @return
     */
    @Deprecated
    public static Map<String,Object> mapWithExtraFields( Object entity, String properties, List<String> extraProperties )
    {
        Map<String, Object> baseMap;
        try
        {
            baseMap = (Map<String, Object>) entity.getClass().getMethod( "publicMap" ).invoke( entity );
        }
        catch( Exception e )
        {
            throw new UserException( "base does not have a publicMap method " + entity.getClass().getName(), e);
        }

        if(baseMap != null && baseMap.containsKey("serial")){
            baseMap.put("serial", StringUtils.upperCase((String)baseMap.get("serial")));
        }

        if( StringUtils.isNotBlank( properties ) )
        {
            for( String property : properties.split( ",", -1 ) )
            {
                if (extraProperties.contains(property)) {
                    //this will be filled by addToPublicMap method
                    continue;
                } else {
                    //logger.debug( "entity=" + entity + " property=" + property );
                    baseMap.put( property, customIndividualMap( entity, property ) );
                }
            }
        }

        return baseMap;
    }

    /**
     * This method removes from map all property EXCEPT what is in 'only' and 'extra'.
     * <p/>
     * This is a quick and dirty implementation to implement functionality.
     * This should be implemented at the lowest possible level in the stack.
     *
     * @param map
     * @param only  a csv list of property names
     * @param extra - a csv list of property names
     *              <p/>
     *              TODO: handle nested properties
     */
    @Deprecated
    public static void filterOnly(Map<String, Object> map, String only, String extra) {
        if (only == null || "".equals(only))
            return;

        Set<String> onlyProperties = new HashSet<String>();

        for (String str : only.split(",")) {
            onlyProperties.add(str.trim());
        }

        Set<String> extraProperties = new HashSet<String>();

        if (extra != null) {
            for (String str : extra.split(",")) {
                extraProperties.add(str.trim());
            }
        }

        for (Iterator<String> i = map.keySet().iterator(); i.hasNext(); ) {
            String key = i.next();
            if (!(onlyProperties.contains(key) || extraProperties.contains(key))) {
                i.remove();
            }
        }
    }

}
