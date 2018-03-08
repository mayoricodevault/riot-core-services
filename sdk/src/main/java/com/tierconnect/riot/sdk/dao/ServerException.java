package com.tierconnect.riot.sdk.dao;

public class ServerException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ServerException() {
		super();
	}

	public ServerException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ServerException(String message, Throwable cause) {
		super(message, cause);
	}

	public ServerException(String message) {
		super(message);
	}

	public ServerException(Throwable cause) {
		super(cause);
	}

}
