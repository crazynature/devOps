package com.oardc.jira.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Project implements Serializable{

	private static final long serialVersionUID = 206334539260151431L;
	
	private String name;
	private String url;
	private int id;
	private String type;
	
	public Project() {}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public String toString() {
		return name + " " + type + " " + id + " " + url;
	}

}
