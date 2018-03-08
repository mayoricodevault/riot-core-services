package com.tierconnect.riot.sdk.dao;

public class ReportRecordException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ReportRecordException() {
		super();
	}

	public ReportRecordException(String message, Throwable cause,
								 boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ReportRecordException(String message, Throwable cause) {
		super(message, cause);
	}

	public ReportRecordException(String message) {
		super(message);
	}

	public ReportRecordException(Throwable cause) {
		super(cause);
	}	

}
