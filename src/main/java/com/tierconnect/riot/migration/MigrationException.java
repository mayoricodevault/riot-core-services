package com.tierconnect.riot.migration;

public class MigrationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public MigrationException() {
		super();
	}

	public MigrationException(String message, Throwable cause,
                              boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public MigrationException(String message, Throwable cause) {
		super(message, cause);
	}

	public MigrationException(String message) {
		super(message);
	}

	public MigrationException(Throwable cause) {
		super(cause);
	}	

}
