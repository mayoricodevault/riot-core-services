package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.iot.dao.MlBusinessModelDAO;
import com.tierconnect.riot.iot.entities.MlBusinessModel;
import com.tierconnect.riot.iot.entities.QMlBusinessModel;

import javax.annotation.Generated;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class MlBusinessModelService extends MlBusinessModelServiceBase 
{
    public static MlBusinessModel getByName(String name) {
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QMlBusinessModel.mlBusinessModel.name.eq(name));
        MlBusinessModelDAO mlBusinessModelDAO = new MlBusinessModelDAO();
        return mlBusinessModelDAO.selectBy(be);
    }


}

