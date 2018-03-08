package com.tierconnect.riot.appcore.entities;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.annotation.Generated;

@Entity

@Table(name="apc_field", indexes = {@Index(name = "IDX_field_name",  columnList="name")})
public class Field extends FieldBase
{

}

