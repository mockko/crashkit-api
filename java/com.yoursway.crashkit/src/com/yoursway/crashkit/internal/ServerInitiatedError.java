package com.yoursway.crashkit.internal;

import java.io.IOException;

public class ServerInitiatedError extends IOException {

	private static final long serialVersionUID = 1L;
	private final String response;
	
	public ServerInitiatedError(String response) {
		super(response);
		this.response = response;
	}
	
	public boolean is(String response) {
		return this.response.equalsIgnoreCase(response);
	}
	
}
