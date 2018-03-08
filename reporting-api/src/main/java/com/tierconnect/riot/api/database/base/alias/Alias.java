package com.tierconnect.riot.api.database.base.alias;

import java.util.Objects;

import static com.tierconnect.riot.api.assertions.Assertions.voidNotNull;

/**
 * Created by vealaro on 12/6/16.
 */
public class Alias {
    private String property;
    private String label;
    private String function;
    private boolean exclude;

    public Alias(String property, boolean exclude) {
        this(property, property, null, exclude);
    }

    public Alias(String property, String label) {
        this(property, label, null, false);
    }

    public Alias(String property, String label, String function, boolean exclude) {
        voidNotNull("property", property);
        this.property = property;
        this.label = label;
        this.function = function;
        this.exclude = exclude;
    }

    public String getProperty() {
        return property;
    }

    public String getLabel() {
        return label;
    }

    public String getFunction() {
        return function;
    }

    public boolean isExclude() {
        return exclude;
    }

    public static Alias create(String property, String label, String function) {
        return new Alias(property, label, function, false);
    }

    public static Alias create(String property, String label) {
        return new Alias(property, label);
    }

    public static Alias create(String property) {
        return new Alias(property, property);
    }

    public static Alias exclude(String property) {
        return new Alias(property, true);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Alias)) return false;
        Alias alias = (Alias) o;
        return isExclude() == alias.isExclude() &&
                Objects.equals(getProperty(), alias.getProperty()) &&
                Objects.equals(getLabel(), alias.getLabel()) &&
                Objects.equals(getFunction(), alias.getFunction());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getProperty(), getLabel(), getFunction(), isExclude());
    }
}
