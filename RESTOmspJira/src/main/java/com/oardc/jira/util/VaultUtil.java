package com.oardc.jira.util;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;

public class VaultUtil {
	
	public VaultUtil() {
		
	}
	
	public static Vault getVaultService() {
		try {
			final VaultConfig config = new VaultConfig()
					.address(Config.getProperty(Config.VAULT_HOST))
					.token(Config.getProperty(Config.VAULT_TOKEN))
					.build();
			final Vault vault = new Vault(config);
			return vault;
		} catch (VaultException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static String getSSOUsername() {
		try {
			String value = getVaultService().logical()
			        .read(Config.getProperty(Config.VAULT_USERNAME_KEY))
			        .getData().get("value");
			return value;
		} catch (VaultException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static String getSSOPassword() {
		try {
			String value = getVaultService().logical()
			        .read(Config.getProperty(Config.VAULT_PASSWORD_KEY))
			        .getData().get("value");
			return value;
		} catch (VaultException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	
	public static void main(String[] args) {
		System.out.println(getSSOUsername() + " " + getSSOPassword());
	}
	
	
}
