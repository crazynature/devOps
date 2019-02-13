package com.oardc.jira.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class Config {
	public final static String JIRA_HOST = "jira.host";
	public final static String VAULT_HOST = "vault.host";
	public final static String VAULT_TOKEN = "vault.token";
	public final static String VAULT_USERNAME_KEY = "vault.secret.sso_username";
	public final static String VAULT_PASSWORD_KEY = "vault.secret.sso_password";
	public final static String CHART_DIRECTORY = "jira.chart.directory";
	public final static String AUTO_CLEAR_CHARTS = "jira.chart.autoclean";
	
	public static String getProperty(String key) {
		Properties prop = new Properties();
		InputStream input = null;
		String resourceName = "config.properties"; 
		ClassLoader loader = Thread.currentThread().getContextClassLoader();

		try {
			input = loader.getResourceAsStream(resourceName);
			// load a properties file
			prop.load(input);
			return prop.getProperty(key);
		} catch (IOException io) {
			io.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
		return null;
	}
}
