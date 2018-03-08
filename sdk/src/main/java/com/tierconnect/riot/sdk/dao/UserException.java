package com.tierconnect.riot.sdk.dao;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

public class UserException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private int status = 400;

	public UserException() {
		super();
	}

	public UserException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public UserException(String message, Throwable cause) {
		super(message, cause);
	}

	public UserException(String message) {
		super(message);
	}

	public UserException(String message, int status) {
		super(message);
		this.status = status;
	}

	public UserException(String message, Throwable cause, int status) {
		super(message, cause);
		this.status = status;
	}

	private String encodeResponse (String message){
		String response;
		try {
			response = Jsoup.clean(message, Whitelist.basic());
		} catch (Exception e){
			response = message;
		}
		return response;
	}

	public UserException(Throwable cause) {
		super(cause);
	}

	@Override
	public String getMessage(){
		return encodeResponse(super.getMessage());
	}

	public int getStatus() {
		return status;
	}

}
