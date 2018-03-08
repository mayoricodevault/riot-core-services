package com.tierconnect.riot.iot.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : rchirinos
 * @date : 9/14/16 3:56 PM
 * @version:
 */
@Entity
@Table(name = "parameters", indexes = { @Index(name = "IDX_PARAMETERS_CATEGORY", columnList = "code") })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Parameters extends ParametersBase{

    public Parameters () {

    }
    /**
     * Constructor
     * @param category
     * @param code
     * @param appResourceCode
     */
    public Parameters(String category, String code, String appResourceCode, String value) {
        this.category = category;
        this.code = code;
        this.appResourceCode = appResourceCode;
        this.value = value;
    }
}
