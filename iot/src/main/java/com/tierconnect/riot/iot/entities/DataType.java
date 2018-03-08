package com.tierconnect.riot.iot.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name = "dataType", indexes = {@Index(name = "IDX_datatype_code", columnList = "code")})
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class DataType extends DataTypeBase {

    public boolean isSequenceType() {
        return (this.getCode().equals("SEQUENCE"));
    }
}

