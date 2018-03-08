package com.tierconnect.riot.iot.utils.sql.dialect.resolver;

import com.tierconnect.riot.iot.utils.sql.dialect.SQLServer2008CustomDialect;
import com.tierconnect.riot.iot.utils.sql.dialect.SQLServer2012CustomDialect;
import org.hibernate.dialect.*;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Custom Dialect Resolver for hibernate 4.3.11.Final
 * <p>
 * Created by julio.rocha on 23-06-17.
 */
public class CustomDialectResolver implements DialectResolver {
    private static final CoreMessageLogger LOG = CoreLogging.messageLogger(CustomDialectResolver.class);

    /**
     * Singleton access
     */
    public static final CustomDialectResolver INSTANCE = new CustomDialectResolver();

    @Override
    public Dialect resolveDialect(DialectResolutionInfo info) {
        final String databaseName = info.getDatabaseName();

        if ("CUBRID".equalsIgnoreCase(databaseName)) {
            return new CUBRIDDialect();
        }

        if ("HSQL Database Engine".equals(databaseName)) {
            return new HSQLDialect();
        }

        if ("H2".equals(databaseName)) {
            return new H2Dialect();
        }

        if ("MySQL".equals(databaseName)) {
            final int majorVersion = info.getDatabaseMajorVersion();

            if (majorVersion >= 5) {
                return new MySQL5Dialect();
            }

            return new MySQLDialect();
        }

        if ("PostgreSQL".equals(databaseName)) {
            final int majorVersion = info.getDatabaseMajorVersion();
            final int minorVersion = info.getDatabaseMinorVersion();

            if (majorVersion == 9) {
                return new PostgreSQL9Dialect();
            }

            if (majorVersion == 8 && minorVersion >= 2) {
                return new PostgreSQL82Dialect();
            }

            return new PostgreSQL81Dialect();
        }

        if ("EnterpriseDB".equals(databaseName)) {
            return new PostgresPlusDialect();
        }

        if ("Apache Derby".equals(databaseName)) {
            final int majorVersion = info.getDatabaseMajorVersion();
            final int minorVersion = info.getDatabaseMinorVersion();

            if (majorVersion > 10 || (majorVersion == 10 && minorVersion >= 7)) {
                return new DerbyTenSevenDialect();
            } else if (majorVersion == 10 && minorVersion == 6) {
                return new DerbyTenSixDialect();
            } else if (majorVersion == 10 && minorVersion == 5) {
                return new DerbyTenFiveDialect();
            } else {
                return new DerbyDialect();
            }
        }

        if ("ingres".equalsIgnoreCase(databaseName)) {
            final int majorVersion = info.getDatabaseMajorVersion();
            final int minorVersion = info.getDatabaseMinorVersion();

            switch (majorVersion) {
                case 9:
                    if (minorVersion > 2) {
                        return new Ingres9Dialect();
                    }
                    return new IngresDialect();
                case 10:
                    return new Ingres10Dialect();
                default:
                    LOG.unknownIngresVersion(majorVersion);
            }
            return new IngresDialect();
        }

        if (databaseName.startsWith("Microsoft SQL Server")) {
            final int majorVersion = info.getDatabaseMajorVersion();
            switch (majorVersion) {
                case 8: {
                    return new SQLServerDialect();
                }
                case 9: {
                    return new SQLServer2005Dialect();
                }
                case 10: {
                    return new SQLServer2008CustomDialect();
                }
                case 11:
                case 12:
                case 13: {
                    return new SQLServer2012CustomDialect();
                }
                default: {
                    if (majorVersion < 8) {
                        LOG.warn("Unknown Microsoft SQL Server major version [" + majorVersion + "] " +
                                "using [" + SQLServerDialect.class + "] dialect");
                        return new SQLServerDialect();
                    } else {
                        // assume `majorVersion > 13`
                        LOG.warn("Unknown Microsoft SQL Server major version [" + majorVersion + "] " +
                                "using [" + SQLServer2012Dialect.class + "] dialect");
                        return new SQLServer2012CustomDialect();
                    }
                }
            }
        }

        if ("Sybase SQL Server".equals(databaseName) || "Adaptive Server Enterprise".equals(databaseName)) {
            return new SybaseASE15Dialect();
        }

        if (databaseName.startsWith("Adaptive Server Anywhere")) {
            return new SybaseAnywhereDialect();
        }

        if ("Informix Dynamic Server".equals(databaseName)) {
            return new InformixDialect();
        }

        if ("DB2 UDB for AS/400".equals(databaseName)) {
            return new DB2400Dialect();
        }

        if (databaseName.startsWith("DB2/")) {
            return new DB2Dialect();
        }

        if ("Oracle".equals(databaseName)) {
            final int majorVersion = info.getDatabaseMajorVersion();

            switch (majorVersion) {
                case 12:
                    // fall through
                case 11:
                    // fall through
                case 10:
                    return new Oracle10gDialect();
                case 9:
                    return new Oracle9iDialect();
                case 8:
                    return new Oracle8iDialect();
                default:
                    LOG.unknownOracleVersion(majorVersion);
            }
            return new Oracle8iDialect();
        }

        if ("HDB".equals(databaseName)) {
            // SAP recommends defaulting to column store.
            return new HANAColumnStoreDialect();
        }

        if (databaseName.startsWith("Firebird")) {
            return new FirebirdDialect();
        }

        return null;
    }
}
