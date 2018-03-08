package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.iot.dao.EdgeboxDAO;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_RegularizeTypeEdgeBox_RIOT10641 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_RegularizeTypeEdgeBox_RIOT10641.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        regularizeTypeEdgeBox();
    }

    /**
     * Method to regularize types of edgeboxes
     */
    private void regularizeTypeEdgeBox() {
        EdgeboxDAO edgeboxDAO = EdgeboxService.getEdgeboxDAO();
        List<Edgebox> listEdgebox = edgeboxDAO.selectAll();
        for (Edgebox edge : listEdgebox) {
            boolean setEdgeType = false;
            if (isCoreBridge(edge.getConfiguration())) {
                if (!StringUtils.equals(edge.getType(), "core")) {
                    edge.setType("core");
                    setEdgeType = true;
                }
            } else {
                if (!StringUtils.equals(edge.getType(), "edge")) {
                    edge.setType("edge");
                    setEdgeType = true;
                }
            }
            if (setEdgeType) {
                edgeboxDAO.update(edge);
            }
        }
    }

    /**
     * @param configuration the configuration attribute of edgebox table
     * @return true if it is a core bridge configuration
     */
    public boolean isCoreBridge(String configuration) {
        return (!StringUtils.isBlank(configuration) && configuration.toLowerCase().contains("pointinzonerule"));
        //specific of core bridges
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
