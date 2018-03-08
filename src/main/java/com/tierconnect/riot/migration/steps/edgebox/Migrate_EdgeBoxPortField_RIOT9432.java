package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.iot.dao.EdgeboxDAO;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 * This class is calling by reflection.
 */
@SuppressWarnings("unused")
public class Migrate_EdgeBoxPortField_RIOT9432 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_EdgeBoxPortField_RIOT9432.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migratePortByDefaultEdgeBox();
    }

    //todo ariel quiroz najar
    private void migratePortByDefaultEdgeBox() {
        EdgeboxDAO edgeboxDAO = EdgeboxService.getEdgeboxDAO();
        List<Edgebox> listEdgeBox = edgeboxDAO.selectAll();
        for (Edgebox edge : listEdgeBox) {
            String edgeCode = edge.getCode().toUpperCase();
            switch (edgeCode) {
                case "ALEB":
                    edge.setPort(9090L);
                    edge.setType("edge");
                    break;
                case "STAR":
                    edge.setPort(9091L);
                    break;
                case "STAR1":
                    edge.setPort(9092L);
                    break;
                case "ALEB2":
                    edge.setType("edge");
                    break;
                case "MCB":
                    edge.setType("core");
                    break;
                case "GPS":
                    edge.setType("edge");
                    break;
            }
            edgeboxDAO.update(edge);
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

}
