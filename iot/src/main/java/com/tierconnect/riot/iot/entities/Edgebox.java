package com.tierconnect.riot.iot.entities;

import javax.annotation.Generated;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * Created by julio.rocha on 16-05-17.
 */
@Entity
@Generated("com.tierconnect.riot.appgen.service.GenModel")
@Table(name = "Edgebox", indexes = {@Index(name = "IDX_edgebox_code", columnList = "code", unique = true)})
public class Edgebox extends EdgeboxBase
{

}
