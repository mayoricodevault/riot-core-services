package com.tierconnect.riot.appcore.entities;

import javax.annotation.Generated;
import javax.persistence.Entity;

@Entity
public class ConnectionType extends ConnectionTypeBase
{
    public enum Type {
        TYPE_LDAP_ACTIVE_DIRECTORY       ("LDAP_ACTIVE_DIRECTORY");

        public String value;
        Type(String value){
            this.value = value;
        }

        public static Type getTypeByValue (String value){
            Type result = null;
            for (Type type : Type.values()){
                if (type.value.equals(value))
                    result = type;
            }
            return  result;
        }
    }

}

