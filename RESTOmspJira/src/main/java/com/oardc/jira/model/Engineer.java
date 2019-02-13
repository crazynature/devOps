package com.oardc.jira.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Engineer implements Serializable{

	private static final long serialVersionUID = -4505813959645432412L;
	
	private String name;
	private String email;
	private double totalStoryPoints;
	
	public Engineer() {
		
	}
	
	public Engineer(String name, String email, double storyPoints) {
		this.name = name;
		this.email = email;
		this.totalStoryPoints = storyPoints;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public double getStoryPoints() {
		return totalStoryPoints;
	}

	public void setStoryPoints(double storyPoints) {
		this.totalStoryPoints = storyPoints;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	

}
