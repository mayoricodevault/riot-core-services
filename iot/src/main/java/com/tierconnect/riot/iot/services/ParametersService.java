package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.entities.QParameters;
import com.tierconnect.riot.iot.entities.ValidationBean;
import com.tierconnect.riot.sdk.dao.UserException;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : rchirinos
 * @date : 9/14/16 2:53 PM
 * @version:
 */
public class ParametersService  extends ParametersServiceBase {

    public static final String CATEGORY_BRIDGE_TYPE = "BRIDGE_TYPE";
    public static final String CODE_EDGE = "edge";
    public static final String CODE_CORE = "core";

    /**
     * Validate the creation of a specific parameter
     * @param parameters
     * @return
     */
    public ValidationBean validateCreate(Parameters parameters )
    {
        ValidationBean val = new ValidationBean();
        if ((parameters !=null) &&
                ((parameters.getCategory() !=null) && (!parameters.getCategory().trim().isEmpty())) &&
                ((parameters.getCode() !=null) && (!parameters.getCode().trim().isEmpty())) ) {
//            if (getParametersDAO().existsParameter(parameters.getCategory().trim(), parameters.getCode().trim())) {
//                val = new ValidationBean();
//                val.setError(true);
//                val.setErrorDescription("Category "+parameters.getCategory()+" and Code "+parameters.getCode()+" already exist in Parameters.");
//            }
        } else {
            val.setErrorDescription("Category and Code  cannot be null or empty.");
        }
        return val;
    }

    /**
     * Insert a new Parameter
     * @param parameters
     * @return
     */
    @Override
    public Parameters insert( Parameters parameters )
    {
        ValidationBean valida = validateCreate(parameters);
        if(valida.isError()) {
            throw new UserException(valida.getErrorDescription());
        }
        Parameters parameter = getByCategoryAndCode(parameters.getCategory(), parameters.getCode());
        if (parameter != null) {
            getParametersDAO().update(parameter);
        } else {
            Long id = getParametersDAO().insert( parameters );
            parameters.setId( id );
        }
        return parameters;
    }

    /**
     * Get
     * @param category
     * @param code
     * @return
     */
    public Parameters getByCategoryAndCode(String category, String code) {
        Parameters result = null;
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QParameters.parameters.category.eq(category));
        be = be.and(QParameters.parameters.code.eq(code));
        List<Parameters> lstParameters = ParametersService.getInstance().listPaginated(be,null, null);
        if((lstParameters != null) && (lstParameters.size() >0)) {
            if (lstParameters.size()>=2) {
                throw new UserException("Duplicate values for '"+ category+"' and '"+code+"'");
            }
            result = lstParameters.get(0);
        }
        return result;
    }

}
