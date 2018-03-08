package com.tierconnect.riot.appcore.utils.validator;

import com.tierconnect.riot.appcore.entities.ConnectionType;

public interface ConnectionValidator {

    boolean testConnection(ConnectionType connectionType, String properties);
    int getStatus();
    String getCause();
}
