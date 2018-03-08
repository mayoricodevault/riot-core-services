package com.tierconnect.riot.iot.popdb;

import com.tierconnect.riot.iot.entities.Zone;
import com.tierconnect.riot.iot.services.ZoneService;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;

import java.util.Arrays;
import java.util.List;

/**
 * Created by fflores on 5/19/2015.
 */
public class FixDBUpdateZonesSaturn {

    static Logger logger = Logger.getLogger( FixDBUpdateZonesSaturn.class );

    public static void main(String args[]) {
        System.out.println("Starting FixDBUpdateZonesSaturn... ");
        FixDBUpdateZonesSaturn.initMysqlJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "validate");
        System.getProperties().put("hibernate.connection.url", "jdbc:mysql://localhost:3306/riot_main");
        System.getProperties().put("hibernate.connection.username", "root");
        System.getProperties().put("hibernate.connection.password", "control123!");
        System.getProperties().put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        Transaction zoneTransaction = ZoneService.getZoneDAO().getSession().getTransaction();
        zoneTransaction.begin();
        changeCode();
        zoneTransaction.commit();
        System.out.println("FixDBUpdateZonesSaturn has been finished.");
    }

    public static void initMysqlJDBCDrivers() {
        //explicitly load the mysql and mssql drivers otherwise popdb would fail
        try {
            Class.forName("org.gjt.mm.mysql.Driver");
            logger.info(String.format("registering mysql jdbc driver"));
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            logger.info(String.format("registering sqlserver jdbc driver"));
        } catch (Exception ex) {
            System.out.println(Arrays.asList(ex.getStackTrace()));
        }
    }

    public static void changeCode() {
        System.out.println("Starting changeCode...");
        List<Zone> zones= ZoneService.getZoneDAO().selectAll();
        int i = 0;
        for (Zone zone : zones)
        {
            if (null == zone.getCode())
            {
                zone.setCode(zone.getName());
                ZoneService.getZoneDAO().update(zone);
                i++;
            }
        }
        System.out.println("zones modified >>> " + i);
    }

}
