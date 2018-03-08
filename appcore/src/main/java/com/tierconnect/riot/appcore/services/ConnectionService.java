package com.tierconnect.riot.appcore.services;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.mysema.query.types.expr.BooleanExpression;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QConnection;
import com.tierconnect.riot.appcore.utils.validator.*;
import com.tierconnect.riot.sdk.dao.UserException;
import javax.annotation.Generated;
import java.util.List;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class ConnectionService extends ConnectionServiceBase {

    public Connection getByCode(String code) {
        HibernateQuery query = getConnectionDAO().getQuery();
        return query.where(QConnection.connection.code.eq(code))
                .uniqueResult(QConnection.connection);

    }

    @Override
    public Connection insert(Connection connection) {
        if (existsCodeConnection(connection.getCode())) {
            throw new UserException(String.format("Code '[%s]' already exist", connection.getCode()));
        }
        Long id = getConnectionDAO().insert(connection);
        connection.setId(id);
        return connection;
    }

    @Override
    public Connection update(Connection connection) {
        if (existsCodeConnection(connection.getCode(), connection.getId())) {
            throw new UserException(String.format("Code '[%s]' already exist", connection.getCode()));
        }
        getConnectionDAO().update(connection);
        updateFavorite(connection);
        return connection;
    }

    public boolean existsCodeConnection(String codeConnection) {
        return existsCodeConnection(codeConnection, null);
    }

    private boolean existsCodeConnection(String codeConnection, Long excludeId) {
        BooleanExpression predicate = QConnection.connection.code.eq(codeConnection);
        if (excludeId != null) {
            predicate = predicate.and(QConnection.connection.id.ne(excludeId));
        }
        return getConnectionDAO().getQuery().where(predicate).exists();

    }

    public Connection getByCodeAndGroup(String code, Group group) {
        HibernateQuery query = getConnectionDAO().getQuery();
        return query.where(QConnection.connection.code.eq(code).and(QConnection.connection.group.eq(group)))
                .uniqueResult(QConnection.connection);
    }

    public List<Connection> getByTypeAndGroup(ConnectionType connectionType, Group group) {
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QConnection.connection.connectionType.eq(connectionType));
        be = be.and(QConnection.connection.group.eq(group));
        return getConnectionDAO().selectAllBy(be);
    }

    public void testConnection(Connection connection) {
        String requiredMesasge = connection.requiredFieldsMessage();
        if (requiredMesasge != null) {
            throw new UserException(requiredMesasge, 400);
        }
        ConnectionValidator connectionValidator = null;
        switch (connection.getConnectionType().getCode()) {
            case "ANALYTICS":
                connectionValidator = new AnalyticsValidator();
                break;
            case "REST":
                connectionValidator = new RestValidator();
                break;
            case "MQTT":
                connectionValidator = new MqttValidator();
                break;
            case "KAFKA":
                connectionValidator = new KafkaValidator();
                break;
            case "FTP":
                connectionValidator = new FtpValidator();
                break;
            case "ldap":
                connectionValidator = new LdapValidator();
                break;
            case "MONGO":
                connectionValidator = new MongoValidator();
                break;
            case  "DBConnection":
                connectionValidator = new ExternalRelationalDBValidator();
                break;
            case "SQL":
                connectionValidator = new InternalSQLConnectionValidator();
                break;
            case "GPubSub":
                connectionValidator = new GooglePubSubValidator();
                break;
            default:
            throw new UserException(connection.getConnectionType().getCode()+" Connetion Type is not being tested");
        }
        if (connectionValidator != null) {
            if (!connectionValidator.testConnection(connection.getConnectionType(), connection.getProperties())) {
                throw new UserException(connectionValidator.getCause(), connectionValidator.getStatus());
            }
        }
    }
}

