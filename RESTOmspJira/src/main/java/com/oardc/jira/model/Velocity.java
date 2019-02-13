package com.oardc.jira.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Velocity implements Serializable{

	private static final long serialVersionUID = -7981010853848453364L;
	private String name;
	private double velocity;
	
	public Velocity() {
		
	}
	
	public Velocity(String name, double velocity) {
		this.name = name;
		this.velocity = velocity;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getVelocity() {
		return velocity;
	}

	public void setVelocity(double velocity) {
		this.velocity = velocity;
	}
}
