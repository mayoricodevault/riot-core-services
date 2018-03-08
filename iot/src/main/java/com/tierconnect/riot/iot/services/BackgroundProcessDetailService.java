package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.iot.entities.BackgroundProcessDetail;
import com.tierconnect.riot.iot.entities.QBackgroundProcessDetail;

import javax.annotation.Generated;
import java.util.List;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class BackgroundProcessDetailService extends BackgroundProcessDetailServiceBase 
{
    public List<BackgroundProcessDetail> getBackgroundProcessDetail(long backgroundProcessId){
        BooleanBuilder be = new BooleanBuilder();

        be = be.and(QBackgroundProcessDetail.backgroundProcessDetail.backgroundProcess.id.eq(backgroundProcessId));

        List <BackgroundProcessDetail> backgroundProcessDetailList = BackgroundProcessDetailService.getInstance().listPaginated(be, null, null);

        return backgroundProcessDetailList;
    }

    public void deleteBackgroundProcessDetail(long backgroundProcessDetailId){
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QBackgroundProcessDetail.backgroundProcessDetail.id.eq(backgroundProcessDetailId));

        List<BackgroundProcessDetail> backGroundProcessDetail = BackgroundProcessDetailService.getInstance().listPaginated(be, null, null);
        for(BackgroundProcessDetail elem : backGroundProcessDetail){
            getBackgroundProcessDetailDAO().delete( elem );

        }

    }


}

