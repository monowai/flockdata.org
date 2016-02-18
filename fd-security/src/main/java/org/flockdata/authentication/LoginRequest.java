package org.flockdata.authentication;

import java.io.Serializable;

public class LoginRequest implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2443535860253992348L;
	
	private String username;
	private String password;

	public LoginRequest(String user, String pass) {
		this();
		this.username = user;
		this.password = pass;
	}

	public LoginRequest() {

	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String userName) {
		this.username = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
