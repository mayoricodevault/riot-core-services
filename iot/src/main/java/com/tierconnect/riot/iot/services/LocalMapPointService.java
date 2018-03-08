package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.iot.entities.LocalMap;
import com.tierconnect.riot.iot.entities.LocalMapPoint;
import com.tierconnect.riot.iot.entities.QLocalMapPoint;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.dao.UserException;

import java.util.List;

/**
 * Created by rsejas on 3/26/17.
 */
public class LocalMapPointService extends LocalMapPointServiceBase {
    public List<LocalMapPoint> getLocalMapPointByMap(LocalMap localMap) {
        if (localMap == null) {
            throw new UserException("zone is null");
        }
        String where = "localMap.id=" + localMap.getId().intValue();
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        booleanBuilder = booleanBuilder.and(QueryUtils.buildSearch(QLocalMapPoint.localMapPoint, where));
        Pagination pagination = new Pagination(1, -1);
        List<LocalMapPoint> localMapPointList = LocalMapPointService.getInstance().listPaginated(booleanBuilder, pagination, null);
        return localMapPointList;
    }
}
