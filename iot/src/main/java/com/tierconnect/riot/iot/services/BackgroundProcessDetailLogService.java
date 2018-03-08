package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.iot.entities.BackgroundProcessDetailLog;
import com.tierconnect.riot.iot.entities.BackgroundProcessEntity;
import com.tierconnect.riot.iot.entities.QBackgroundProcessDetailLog;
import com.tierconnect.riot.iot.entities.QBackgroundProcessEntity;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : rchirinos
 * @date : 12/23/16 1:49 PM
 * @version:
 */
public class BackgroundProcessDetailLogService extends BackgroundProcessDetailLogServiceBase {

    static Logger logger = Logger.getLogger(BackgroundProcessDetailLogService.class);

    /**
     * @return list of things pending to delete
     */
    public List<BackgroundProcessDetailLog> getThingsPendingToDelete() {
        BooleanBuilder be = new BooleanBuilder();
        be.and(QBackgroundProcessDetailLog.backgroundProcessDetailLog.backgroundProcessDetail.isNull());
        return BackgroundProcessDetailLogService.getInstance().listPaginated(be, null, null);
    }

    /**
     * @return the register of the thing in log table given it's id
     */
    public BackgroundProcessDetailLog getThingPendingToDelete(Long thingId) {
        BooleanBuilder be = new BooleanBuilder();
        be.and(QBackgroundProcessDetailLog.backgroundProcessDetailLog.backgroundProcessDetail.isNull());
        be.and(QBackgroundProcessDetailLog.backgroundProcessDetailLog.thingId.eq(thingId));
        List<BackgroundProcessDetailLog> backgroundProcessDetailLogs = BackgroundProcessDetailLogService
                .getInstance().listPaginated(be, null, null);
        return (backgroundProcessDetailLogs != null && backgroundProcessDetailLogs.size() > 0) ?
                backgroundProcessDetailLogs.get(0) : null;
    }

    /**
     * @param thingId
     */
    public void deleteOne(Long thingId) {
        BackgroundProcessDetailLog thingPendingToDelete = getThingPendingToDelete(thingId);
        if (thingPendingToDelete != null) {
            delete(thingPendingToDelete);
        }
    }

    /**
     * Relies that all the entities associated to it has been deleted
     *
     * @param backgroundProcessDetailLog
     */
    @Override
    public void delete(BackgroundProcessDetailLog backgroundProcessDetailLog) {
        getBackgroundProcessDetailLogDAO().delete(backgroundProcessDetailLog);
    }

    public void deleteBackgroundProcessDetailLog(long backgroundProcessDetailId){
        BooleanBuilder be = new BooleanBuilder();

        be = be.and(QBackgroundProcessDetailLog.backgroundProcessDetailLog.backgroundProcessDetail.id.eq(backgroundProcessDetailId));

        List<BackgroundProcessDetailLog> backgroundProcessDetailLog = BackgroundProcessDetailLogService.getInstance().listPaginated(be, null, null);
        for(BackgroundProcessDetailLog elem : backgroundProcessDetailLog){
            getBackgroundProcessDetailLogDAO().delete( elem );

        }
    }

}
