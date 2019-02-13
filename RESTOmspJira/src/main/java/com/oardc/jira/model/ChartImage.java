package com.oardc.jira.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ChartImage  implements Serializable{
	
	private static final long serialVersionUID = -3702548004230563387L;
	
	private String url;
	private String fileName;
	
	public ChartImage() {
		
	}
	
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

}
