package com.tierconnect.riot.appcore.entities;

import javax.annotation.Generated;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Created by julio.rocha on 18-08-17.
 */
@Entity
@Generated("com.tierconnect.riot.appgen.service.GenModel")
@Table(name = "Recent", indexes = {
        @Index(name = "IDX_recent_userTypeElid", columnList = "user_id,typeElement,elementId", unique = true)})
public class Recent extends RecentBase {
    @Version
    private long version;
}