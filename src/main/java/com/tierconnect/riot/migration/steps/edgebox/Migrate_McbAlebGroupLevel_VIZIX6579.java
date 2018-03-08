package com.tierconnect.riot.migration.steps.edgebox;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.QEdgebox;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by dbascope on 07/13/17
 */
public class Migrate_McbAlebGroupLevel_VIZIX6579 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_McbAlebGroupLevel_VIZIX6579.class);
    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        changeMcbGroupLevel();
        changeAlebGroupLevel();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    private void changeMcbGroupLevel() throws NonUniqueResultException {
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QEdgebox.edgebox.type.eq("core"));
        List<Group> companyGroup = GroupService.getInstance().getByLevel(2);
        if (companyGroup.size() > 0) {
            for (Edgebox edgebox : EdgeboxService.getEdgeboxDAO().selectAllBy(be)) {
                if (edgebox.getGroup().equals(GroupService.getInstance().getRootGroup())) {
                    edgebox.setGroup(companyGroup.get(0));
                }
                if (edgebox.getName().contains("*")) {
                    edgebox.setName(edgebox.getName().replace("*", "").trim());
                }
                EdgeboxService.getInstance().update(edgebox);
            }
        }
    }

    private void changeAlebGroupLevel() throws NonUniqueResultException {
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QEdgebox.edgebox.type.eq("edge"));
        be = be.and(QEdgebox.edgebox.group.eq(GroupService.getInstance().getRootGroup()));
        List<Group> companyGroup = GroupService.getInstance().getByLevel(3);
        if (companyGroup.size() > 0) {
            for (Edgebox edgebox : EdgeboxService.getEdgeboxDAO().selectAllBy(be)) {
                edgebox.setGroup(companyGroup.get(0));
                EdgeboxService.getInstance().update(edgebox);
            }
        }
    }
}
