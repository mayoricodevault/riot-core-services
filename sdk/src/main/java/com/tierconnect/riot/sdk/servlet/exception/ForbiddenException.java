package com.tierconnect.riot.sdk.servlet.exception;

import com.tierconnect.riot.sdk.dao.UserException;

public class ForbiddenException extends UserException{
	public ForbiddenException() {
		super();
	}

	public ForbiddenException(String message, Throwable cause,boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ForbiddenException(String message, Throwable cause) {
		super(message, cause);
	}

	public ForbiddenException(String message) {
		super(message);
	}

	public ForbiddenException(Throwable cause) {
		super(cause);
	}	
}
