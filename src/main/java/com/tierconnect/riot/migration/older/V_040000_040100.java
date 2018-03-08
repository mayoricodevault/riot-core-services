package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.DBHelper;
import org.apache.log4j.Logger;

import com.tierconnect.riot.iot.dao.EdgeboxDAO;

import java.util.Arrays;
import java.util.List;

/**
 * Created by fflores on 1/7/2016.
 * Modify by achambi on 11/02/2016.
 */
@Deprecated
public class V_040000_040100 implements MigrationStepOld {

    static Logger logger = Logger.getLogger(V_040000_040100.class);

    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(40000);
    }

    @Override
    public int getToVersion() {
        return 40100;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        DBHelper dbHelper = new DBHelper();
        String databaseType = dbHelper.getDataBaseType();
        dbHelper.executeSQLFile("sql/" + databaseType + "/V040000_to_040100.sql");
    }

    @Override
    public void migrateHibernate() throws Exception {
        migratePortByDefaultEdgeBox();
    }

    @Override
    public void migrateSQLAfter() throws Exception {
    }


    //todo ariel quiroz najar
    private void migratePortByDefaultEdgeBox(){
        EdgeboxDAO edgeboxDAO= EdgeboxService.getEdgeboxDAO();
        List<Edgebox>listEdgebox=edgeboxDAO.selectAll();
        for (Edgebox edge:listEdgebox){
            if (edge.getCode().equalsIgnoreCase("ALEB")){
                edge.setPort(9090L);
                edge.setType("edge");
            }

            if (edge.getCode().equalsIgnoreCase("STAR")){
                edge.setPort(9091L);
            }

            if (edge.getCode().equalsIgnoreCase("STAR1")){
                edge.setPort(9092L);
            }
            if (edge.getCode().equalsIgnoreCase("ALEB2")){
                edge.setType("edge");
            }
            if (edge.getCode().equalsIgnoreCase("MCB")){
                edge.setType("core");
            }
            if (edge.getCode().equalsIgnoreCase("GPS")){
                edge.setType("edge");
            }
            edgeboxDAO.update(edge);
        }
    }
}