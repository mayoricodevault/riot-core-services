package com.tierconnect.riot.migration;

/**
 * Created by cvertiz on 4/4/17.
 */
public class MigrationStep implements Comparable<MigrationStep>{

    private Integer order;
    private String path;
    private Object stepInstance;
    private Class clazz;
    private String hash;
    private String description;

    public MigrationStep(Integer order, String path, String description, Object stepInstance) {
        this.order = order;
        this.path = path;
        this.stepInstance = stepInstance;
        this.description = description;
        this.clazz = stepInstance.getClass();
    }

    public Integer getOrder() {
        return order;
    }

    public String getPath() {
        return path;
    }

    public Object getStepInstance() {
        return stepInstance;
    }

    public Class getClazz() {
        return clazz;
    }

    public String getHash() {
        return hash;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public int compareTo(MigrationStep o) {
        return getOrder().compareTo(o.getOrder());
    }
}
