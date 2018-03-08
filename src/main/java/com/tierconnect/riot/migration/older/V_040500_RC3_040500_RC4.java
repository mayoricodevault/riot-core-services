package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.iot.popdb.PopDBML;
import com.mysema.query.types.expr.BooleanExpression;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.EdgeboxRule;
import com.tierconnect.riot.iot.entities.QEdgeboxRule;
import com.tierconnect.riot.iot.services.EdgeboxRuleService;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Created by fflores on 12/7/16.
 */
@Deprecated
public class V_040500_RC3_040500_RC4 implements MigrationStepOld {
    static Logger logger = Logger.getLogger(V_040500_RC3_040500_RC4.class);

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(4050003);
    }

    @Override
    public int getToVersion() {
        return 4050004;
    }

    @Override
    public void migrateSQLBefore() throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        fillSortOrderFiledForEdgeboxRules();
        PopDBML popDBML = new PopDBML();
        popDBML.addResources();
    }

    @Override
    public void migrateSQLAfter() throws Exception {
    }

    /**
     * Fills the field sortOrder of each edgeboxrule by group
     * */
    public void fillSortOrderFiledForEdgeboxRules() {
        //all registers
        final List<Edgebox> edgeboxes = EdgeboxService.getInstance()
                .listPaginated(null, null);
        final String orderString = "name:asc";
        for(Edgebox eb : edgeboxes){
            BooleanExpression be = QEdgeboxRule.edgeboxRule.edgebox.id.eq(eb.getId());
            //all registers
            List<EdgeboxRule> edgeboxRules = EdgeboxRuleService.getInstance()
                    .listPaginated(be, null, orderString);
            int orderSequence = 0;
            for(EdgeboxRule er : edgeboxRules){
                er.setSortOrder(orderSequence);
                EdgeboxRuleService.getInstance().update(er);
                orderSequence++;
            }
        }
    }
}
