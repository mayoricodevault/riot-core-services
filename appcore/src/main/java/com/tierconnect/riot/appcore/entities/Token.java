package com.tierconnect.riot.appcore.entities;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.annotation.Generated;

@Entity
@Table(name="Token", indexes = {@Index(name = "IDX_token_tokenExpirationTime",  columnList="tokenExpirationTime")})
public class Token extends TokenBase
{

}

