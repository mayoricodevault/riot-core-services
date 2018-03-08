package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.iot.entities.BackgroundProcess;
import com.tierconnect.riot.iot.entities.BackgroundProcessEntity;
import com.tierconnect.riot.iot.entities.QBackgroundProcess;
import com.tierconnect.riot.iot.entities.QBackgroundProcessEntity;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : aruiz
 * @date : 2/9/17 3:06 PM
 * @version:
 */
public class BackgroundProcessEntityService extends BackgroundProcessEntityServiceBase {

    public BackgroundProcessEntity getModuleInformation(BackgroundProcess backgroundProcess, String module){
        BackgroundProcessEntity backgroundProcessEntity;
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QBackgroundProcessEntity.backgroundProcessEntity.moduleName.eq(module));
        be = be.and(QBackgroundProcessEntity.backgroundProcessEntity.backgroundProcess.eq(backgroundProcess));

        backgroundProcessEntity = getBackgroundProcessEntityDAO().selectBy(be);

        return backgroundProcessEntity;
    }

    public void deleteBackgroundProcessEntity(long backgroundProcessId){
        BooleanBuilder be = new BooleanBuilder();

        be = be.and(QBackgroundProcessEntity.backgroundProcessEntity.backgroundProcess.id.eq(backgroundProcessId));

        List<BackgroundProcessEntity> backgroundProcessEntity = BackgroundProcessEntityService.getInstance().listPaginated(be, null, null);
        for(BackgroundProcessEntity elem : backgroundProcessEntity){
            getBackgroundProcessEntityDAO().delete( elem );

        }
    }

}
