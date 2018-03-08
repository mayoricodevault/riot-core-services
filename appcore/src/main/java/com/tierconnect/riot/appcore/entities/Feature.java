package com.tierconnect.riot.appcore.entities;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by agutierrez on 5/8/15.
 */
public class Feature {
    private final List<String> resources;
    private final List<String> fields;
    private final String description;

    public Feature(String description, List<String> resources, List<String> fields) {
        this.description = description;
        this.resources = (resources != null) ?  resources : new ArrayList<String>();
        this.fields = (fields != null) ? fields : new ArrayList<String>();
    }

    public List<String> getResources() {
        return resources;
    }

    public List<String> getFields() {
        return fields;
    }

    public String getDescription() {
        return description;
    }
}
