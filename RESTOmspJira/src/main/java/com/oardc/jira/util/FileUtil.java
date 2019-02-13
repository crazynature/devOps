package com.oardc.jira.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.apache.commons.io.FileUtils;


public class FileUtil {
	
	public static void createDirectoryIfNotExists(String target) throws IOException{
		//first check if dir exist
		Path initialDir = Paths.get(Config.getProperty(Config.CHART_DIRECTORY));
		if (Files.notExists(initialDir)) {
			Files.createDirectories(initialDir);
		}
		
		Path path = Paths.get(target);
		if (Files.notExists(path)) {
			if ("true".equalsIgnoreCase(Config.getProperty(Config.AUTO_CLEAR_CHARTS))){
				Path dir = Paths.get(Config.getProperty(Config.CHART_DIRECTORY));
				cleanDirectory(dir.toFile());
				System.out.println("auto clear charts enabled. Cleared old charts.");
			}
			System.out.println("Initial directory not found. Created directory.");
			Files.createDirectories(path);
		}else {
			System.out.println("Initial directory exists.");
		}
	}
	
	public static String generateFolderName() {
		Date date = new Date();
		LocalDate localdate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		String name = localdate.getMonthValue() + "-" + localdate.getYear();
		return name;
	}
	
	public static void cleanDirectory(File directory) {
		try {
			FileUtils.cleanDirectory(directory);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		try {
			createDirectoryIfNotExists("tmp2/temp2");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
