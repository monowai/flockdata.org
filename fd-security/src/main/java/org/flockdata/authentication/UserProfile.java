package org.flockdata.authentication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserProfile implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8908570723211178151L;

	private String userId;
	private String userName;
	private String userEmail;
	private String status;
	private String company;
	private List<String> userRoles = new ArrayList<>();
	private String apiKey;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void addUserRole(String role) {
		userRoles.add(role);
	}

	public Object[] getUserRoles() {
		return this.userRoles.toArray();
	}

	@Override
	public String toString() {
		return "UserProfile [userId=" + userId + ", userName=" + userName
				+ ", userEmail=" + userEmail + ", status=" + status
				+ ", userRoles=" + userRoles + ", apiKey=" + apiKey + "]";
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	
}
