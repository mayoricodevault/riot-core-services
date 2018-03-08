package com.tierconnect.riot.iot.servlet;

import javax.servlet.ServletContext;

/**
 * Created by szymon on 11/01/15.
 */
public class BigDataBridgeConfigHelper {
    ServletContext sc;

    public BigDataBridgeConfigHelper(ServletContext sc)
    {
        this.sc = sc;
    }

    public String getBridgeCode()
    {
        return sc.getInitParameter( "bridge.code" );
    }

    public String getRestApikey()
    {
        return sc.getInitParameter( "rest.apikey" );
    }

    public String getRestContext()
    {
        return sc.getInitParameter( "rest.context" );
    }

    public String getRestHost()
    {
        return sc.getInitParameter( "rest.host" );
    }

    public String getRestPort()
    {
        return sc.getInitParameter( "rest.port" );
    }

    public String getThingTypeCode()
    {
        return sc.getInitParameter( "thingType.code" );
    }

}
