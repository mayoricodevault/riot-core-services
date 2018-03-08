package com.tierconnect.riot.utils;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

public class AddHeaderRequestFilter implements ClientRequestFilter {

	private final String headerName;
	private final String headerValue;

	public AddHeaderRequestFilter(String headerName, String headerValue) {
		this.headerName = headerName;
		this.headerValue = headerValue;
	}

	@Override
	public void filter(ClientRequestContext requestContext) throws IOException {
		requestContext.getHeaders().add(headerName, headerValue);
	}

}
