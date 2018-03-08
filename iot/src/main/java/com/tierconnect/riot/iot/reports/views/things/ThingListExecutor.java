package com.tierconnect.riot.iot.reports.views.things;

import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.iot.reports.views.things.dto.ListResult;
import com.tierconnect.riot.iot.reports.views.things.dto.Parameters;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;
import org.apache.shiro.subject.Subject;


/**
 * Created by julio.rocha on 07-07-17.
 * Modified by achambi on 07-07-31.
 *
 * @author achambi
 * @author julio.rocha
 */
public class ThingListExecutor {
    private static Logger logger = Logger.getLogger(ThingListExecutor.class);
    private static final ThingListExecutor INSTANCE = new ThingListExecutor();

    private ThingListExecutor() {
    }

    public static ThingListExecutor getInstance() {
        return INSTANCE;
    }

    public ListResult list(Integer pageSize,
                           Integer pageNumber,
                           String order,
                           String where,
                           String extra,
                           String only,
                           String groupBy,
                           Long visibilityGroupId,
                           String upVisibility,
                           String downVisibility,
                           boolean treeView,
                           Subject subject,
                           User currentUser,
                           boolean returnFavorite,
                           boolean includeResults,
                           boolean includeTotal) {
        long timeStamp = System.currentTimeMillis();
        try {
            Parameters parameters = new Parameters(
                    pageSize,
                    pageNumber,
                    order,
                    where,
                    extra,
                    only,
                    groupBy,
                    visibilityGroupId,
                    upVisibility,
                    downVisibility,
                    treeView,
                    subject,
                    currentUser,
                    returnFavorite,
                    includeResults,
                    includeTotal);
            ThingList thingList;
            if (treeView) {
                thingList = new ThingListAsTree(parameters);
            } else {
                thingList = new ThingListAsTable(parameters);
            }
            return thingList.getList();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new UserException(e.getMessage(), e);
        } finally {
            logger.info("Done with getting things in  " + (System.currentTimeMillis() - timeStamp));
        }
    }
}
