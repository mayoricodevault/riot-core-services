package com.tierconnect.riot.iot.entities;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table (name = "ThingParentHistory")
@XmlRootElement(name = "ThingParentHistory")
public class ThingParentHistory extends ThingParentHistoryBase {
}
