package com.tierconnect.riot.appcore.entities;

/**
 * Created by agutierrez on 12/15/2014.
 */
public enum ResourceType {
    CLASS(1),
    THING_TYPE_CLASS(2),
    //CUSTOM_OBJECT_TYPE_CLASS(3),
    PROPERTY(4),
    //TODO REMOVE THIS
    METHOD(5),
    MODULE(6),
    //MENU(7),
    REPORT_DEFINITION(8),
    DATA_ENTRY(9);
    int id;

    ResourceType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
