package com.tierconnect.riot.iot.bridgeAgent;

/**
 * Created by cfernandez
 * on 8/21/17.
 * Define all available bridgeTypes
 */
public enum BridgeType {
    CORE("core"),
    ALE("edge"),
    STAR_FLEX("STARflex"),
    FTP("FTP"),
    GPS("GPS"),
    SARP("SARP"),
    MONGO_INJECTOR("Mongo_Injector");

    /**
     * The type represents the internal name of the bridge type as it is stored in the database (table: edgebox)
     */
    private String type;

    BridgeType(String type){
        this.type = type;
    }

    public String getType(){
        return type;
    }
}
