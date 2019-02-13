package com.oardc.jira.service;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletContext;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;

import com.oardc.jira.model.ChartImage;
import com.oardc.jira.model.Project;
import com.oardc.jira.model.Sprint;
import com.oardc.jira.util.DateUtil;
import com.oardc.jira.util.FileUtil;
import com.oardc.jira.util.JIRAHttpConnection;
import com.oardc.jira.util.Config;
import com.oardc.jira.util.VaultUtil;

import HTTPClient.HTTPResponse;
import HTTPClient.ModuleException;
import HTTPClient.NVPair;
import HTTPClient.ParseException;
import HTTPClient.URI;

public class OMSPService {

	public HashMap<String, Double> getStoryPointListBySprintId(int sprintId) {
		HashMap<String, Double> map = new HashMap<String, Double>();
		String allIssues = getIssuesBySprintId(sprintId);
		try {
			JSONObject obj = new JSONObject(allIssues);
			JSONArray array = obj.getJSONArray("issues");
			for (int i = 0; i < array.length(); i++) {
				JSONObject issue = array.getJSONObject(i);
				String name = issue.getJSONObject("fields").getJSONObject("assignee").get("name").toString();
				if (!map.containsKey(name)) {
					map.put(name, 0.0);
				}
				double balance = ((Double) map.get(name)).doubleValue();
				double points = 0.0;
				String pointField = issue.getJSONObject("fields").get("customfield_10010").toString();
				if (null != pointField && !"null".equalsIgnoreCase(pointField)) {
					try {
						points = Double.parseDouble(pointField);
					} catch (NumberFormatException e) {
						// try convert to integer
						int temp = Integer.parseInt(pointField);
						points = temp;
					}
				}
				map.put(name, balance + points);
			}
			return map;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return new HashMap<String, Double>();
	}
	
	public ChartImage getChartStoryPointListBySprintName(ServletContext context, String sprintName) {
		final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		final String xLabel = "Engineer";
		final String yLabel = "Total Story Points";
		final String title = "Story Points for " + sprintName;
		
		HashMap<String, Double> response = getStoryPointListBySprintName(sprintName);
		if (response.isEmpty()) {
			return null;
		}
		Map<String,Integer> sortedMap = sortByValue(response);
		Iterator i = sortedMap.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<String, Double> me = (Map.Entry) i.next();
			String email = me.getKey().toString();
			if (email.contains("@")) {
				email = email.substring(0, email.indexOf("@")).replace('.', ' ');
			}
			double points = 0.0;
			try {
				points = Double.parseDouble(me.getValue().toString());
			}catch(NumberFormatException e){
			}
			dataset.addValue(points, email, "");
		}
		JFreeChart barChart = ChartFactory.createBarChart(title, xLabel, yLabel, dataset, PlotOrientation.VERTICAL, true, false, false);
		BarRenderer ren = new BarRenderer();
		ren.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
		ren.setDefaultSeriesVisible(true);
		ren.setShadowVisible(false);
		ren.setDrawBarOutline(true);
		ren.setSeriesOutlinePaint(0, Color.WHITE);
		ren.setBarPainter(new StandardBarPainter());
		ren.setDefaultItemLabelsVisible(true);
		barChart.getCategoryPlot().setRenderer(ren);
		int width = 640;
		int height = 480;
		UUID uuid = UUID.randomUUID();
		try {
			String foldername = Config.getProperty(Config.CHART_DIRECTORY) + "/" + FileUtil.generateFolderName();
			FileUtil.createDirectoryIfNotExists(foldername);
			File BarChart = new File(foldername + "/storypoints_" + sprintName + "_" + uuid + ".jpg");
			ChartImage chartImage = new ChartImage();
			chartImage.setFileName(BarChart.getName());
			chartImage.setUrl(FileUtil.generateFolderName() + "/" + BarChart.getName());
			ChartUtils.saveChartAsJPEG(BarChart, barChart, width, height);
			return chartImage;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public HashMap<String, Double> getStoryPointListBySprintName(String sprintName) {
		if (null == sprintName) {
			return null;
		}
		if (null == getSprintByName(sprintName)) {
			return new HashMap<String, Double>();
		}
		return getStoryPointListBySprintId(getSprintByName(sprintName).getId());
	}
	
	public ChartImage getChartTeamVelocityBySprintName(ServletContext context, String sprintName) {
		final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		final String xLabel = "Engineer";
		final String yLabel = "Velocity (Avg points per day)";
		final String title = "Team Velocity for " + sprintName;
		
		HashMap<String, Double> response = getTeamVelocityBySprintName(sprintName);
		if (response.isEmpty()) {
			return null;
		} 
		Map<String,Integer> sortedMap = sortByValue(response);
		Iterator i = sortedMap.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<String, Double> me = (Map.Entry) i.next();
			String email = me.getKey().toString();
			if (email.contains("@")) {
				email = email.substring(0, email.indexOf("@")).replace('.', ' ');
			}
			double points = 0.0;
			try {
				points = Double.parseDouble(me.getValue().toString());
			}catch(NumberFormatException e){
			}
			dataset.addValue(points, email, "");
		}
		JFreeChart barChart = ChartFactory.createBarChart(title, xLabel, yLabel, dataset, PlotOrientation.VERTICAL, true, false, false);
		BarRenderer ren = new BarRenderer();
		ren.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
		ren.setDefaultSeriesVisible(true);
		ren.setShadowVisible(false);
		ren.setDrawBarOutline(true);
		ren.setSeriesOutlinePaint(0, Color.WHITE);
		ren.setBarPainter(new StandardBarPainter());
		ren.setDefaultItemLabelsVisible(true);
		barChart.getCategoryPlot().setRenderer(ren);
		int width = 640;
		int height = 480;
		UUID uuid = UUID.randomUUID();
		try {
			String foldername = Config.getProperty(Config.CHART_DIRECTORY) + "/" + FileUtil.generateFolderName();
			FileUtil.createDirectoryIfNotExists(foldername);
			File BarChart = new File(foldername + "/teamvelocity_" + sprintName + "_" + uuid + ".jpg");
			ChartImage chartImage = new ChartImage();
			chartImage.setFileName(BarChart.getName());
			chartImage.setUrl(FileUtil.generateFolderName() + "/" + BarChart.getName());
			ChartUtils.saveChartAsJPEG(BarChart, barChart, width, height);
			return chartImage;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public HashMap<String, Double> getTeamVelocityBySprintId(int sprintId){
		String totalCommited = "Total Commited";
		String totalCompleted = "Team Velocity";
		HashMap<String, Double> commitedMap = new HashMap<String, Double>();
		HashMap<String, Double> completedMap = new HashMap<String, Double>();
		commitedMap.put(totalCommited, 0.0);
		completedMap.put(totalCompleted, 0.0);
		String allIssues = getIssuesBySprintId(sprintId);
		try {
			JSONObject obj = new JSONObject(allIssues);
			JSONArray array = obj.getJSONArray("issues");
			for (int i = 0; i < array.length(); i++) {
				JSONObject issue = array.getJSONObject(i);
				String assignee = issue.getJSONObject("fields").getJSONObject("assignee").get("name").toString();
				String pointField = issue.getJSONObject("fields").get("customfield_10010").toString();
				String statusField = issue.getJSONObject("fields").getJSONObject("status").getJSONObject("statusCategory").get("name").toString();
				double totalCommitmentPoints = ((Double) commitedMap.get(totalCommited)).doubleValue();
				double totalCompletedPoints = ((Double) completedMap.get(totalCompleted)).doubleValue();
				double nameCommited = 0.0;
				
				if (null != assignee) {
					if (commitedMap.containsKey(assignee)) {
						nameCommited = ((Double) commitedMap.get(assignee)).doubleValue();
					}
					double nameCompleted = 0.0;
					if (completedMap.containsKey(assignee)) {
						nameCompleted = ((Double) completedMap.get(assignee)).doubleValue();
					}
					double points = 0.0;
					if (null != pointField && !"null".equalsIgnoreCase(pointField)) {
						try {
							points = Double.parseDouble(pointField);
						}catch(NumberFormatException e) {
							//try convert to integer
							int temp = Integer.parseInt(pointField);
							points = temp;
						}
					}
					if (null != statusField) {
						if (statusField.equalsIgnoreCase("In Progress")) {
							commitedMap.put(assignee, nameCommited + points);
						}else if (statusField.equalsIgnoreCase("To Do")) {
							commitedMap.put(assignee, nameCommited + points);
						}else if (statusField.equalsIgnoreCase("Done")) {
							completedMap.put(assignee, nameCompleted + points);
							completedMap.put(totalCompleted, totalCompletedPoints + points);
						}
						commitedMap.put(totalCommited, totalCommitmentPoints + points);
					}
				}
			}
			Sprint s = getSprintBySprintId(sprintId);
			double activeDays = DateUtil.getWorkingDaysBetweenTwoDates(s.getStartDate(), new Date());;
			if (new Date().getTime() > s.getEndDate().getTime()) {
				activeDays = DateUtil.getWorkingDaysBetweenTwoDates(s.getStartDate(), s.getEndDate());
			}
			HashMap<String, Double> velocityMap = new HashMap<String, Double>();
			Iterator i = completedMap.entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry<String, Double> completedEntry = (Map.Entry) i.next();
				String name = completedEntry.getKey().toString();
				if (completedEntry.getValue() == 0.0 || activeDays == 0.0) {
					velocityMap.put(name, 0.0);
				}else {
					double roundedD = Math.round(((completedEntry.getValue()/activeDays) * 100.0 )) / 100.0 ;
					velocityMap.put(name, roundedD);
				}
			}
			return velocityMap;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	public HashMap<String, Double> getTeamVelocityBySprintName(String sprintName) {
		if (null == sprintName) {
			return null;
		}
		return getTeamVelocityBySprintId(getSprintByName(sprintName).getId());
	}

	/*
	 * Sprint
	 */

	public String getIssuesBySprintId(int sprintId) {
		try {
			JIRAHttpConnection conn = new JIRAHttpConnection(new URI(Config.getProperty(Config.JIRA_HOST)),
					VaultUtil.getSSOUsername(), VaultUtil.getSSOPassword().toCharArray(), "JIRA");

			NVPair[] headers = { new NVPair("fields", "customfield_10010,name,status,assignee"),
					new NVPair("maxResults", "100"), new NVPair("Content-Type", "application/json") };
			HTTPResponse response = conn.Get("/jira/rest/agile/latest/sprint/" + sprintId + "/issue", headers);

			int statusCode = response.getStatusCode();
			if (statusCode == 200) {
				BufferedReader in = new BufferedReader(new InputStreamReader(response.getInputStream()));
				String inputline;
				StringBuffer results = new StringBuffer();
				while ((inputline = in.readLine()) != null) {
					results.append(inputline);
				}
				in.close();
				return results.toString();
			} else {
				String responseData = new String(response.getData());
				System.out.println("Action failed. Status code " + statusCode + " was returned with response data: "
						+ responseData);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ModuleException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getIssuesBySprintName(String sprintName) {
		if (null == sprintName) {
			return null;
		}
		return getIssuesBySprintId(getSprintByName(sprintName).getId());
	}

	public String getAllSprints() {
		try {
			JIRAHttpConnection conn = new JIRAHttpConnection(new URI(Config.getProperty(Config.JIRA_HOST)),
					VaultUtil.getSSOUsername(), VaultUtil.getSSOPassword().toCharArray(), "JIRA");

			NVPair[] headers = { new NVPair("Content-Type", "application/json") };
			HTTPResponse response = conn.Get("/jira/rest/agile/1.0/board/154082/sprint", headers);
			int statusCode = response.getStatusCode();
			if (statusCode == 200) {
				BufferedReader in = new BufferedReader(new InputStreamReader(response.getInputStream()));
				String inputline;
				StringBuffer results = new StringBuffer();
				while ((inputline = in.readLine()) != null) {
					results.append(inputline);
				}
				in.close();
				return results.toString();
			} else {
				String responseData = new String(response.getData());
				System.out.println("Action failed. Status code " + statusCode + " was returned with response data: "
						+ responseData);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ModuleException e) {
			e.printStackTrace();
		}
		return null;
	}

	public Sprint getSprintByName(String name) {
		try {
			JIRAHttpConnection conn = new JIRAHttpConnection(new URI(Config.getProperty(Config.JIRA_HOST)),
					VaultUtil.getSSOUsername(), VaultUtil.getSSOPassword().toCharArray(), "JIRA");

			NVPair[] headers = { new NVPair("Content-Type", "application/json") };
			HTTPResponse response = conn.Get("/jira/rest/agile/1.0/board/154082/sprint", headers);
			int statusCode = response.getStatusCode();
			if (statusCode == 200) {
				BufferedReader in = new BufferedReader(new InputStreamReader(response.getInputStream()));
				String inputline;
				StringBuffer results = new StringBuffer();
				while ((inputline = in.readLine()) != null) {
					results.append(inputline);
				}
				in.close();
				Sprint sprint = new Sprint();
				try {
					JSONObject obj = new JSONObject(results.toString());
					JSONArray array = obj.getJSONArray("values");
					for (int i = 0; i < array.length(); i++) {
						JSONObject jsonSprint = array.getJSONObject(i);
						if (name.equalsIgnoreCase(jsonSprint.get("name").toString())) {
							sprint.setId(jsonSprint.getInt("id"));
							sprint.setName(jsonSprint.getString("name"));
							sprint.setState(jsonSprint.getString("state"));
							sprint.setUrl(jsonSprint.getString("self"));
							DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
							Date startDate = df.parse(jsonSprint.getString("startDate"));
							Date endDate = df.parse(jsonSprint.getString("endDate"));
							sprint.setStartDate(startDate);
							sprint.setEndDate(endDate);
							return sprint;
						}
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (java.text.ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				String responseData = new String(response.getData());
				System.out.println("Action failed. Status code " + statusCode + " was returned with response data: "
						+ responseData);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ModuleException e) {
			e.printStackTrace();
		}
		return null;
	}

	public Sprint getSprintBySprintId(int id) {
		try {
			JIRAHttpConnection conn = new JIRAHttpConnection(new URI(Config.getProperty(Config.JIRA_HOST)),
					VaultUtil.getSSOUsername(), VaultUtil.getSSOPassword().toCharArray(), "JIRA");

			NVPair[] headers = { new NVPair("Content-Type", "application/json") };
			HTTPResponse response = conn.Get("/jira/rest/agile/1.0/sprint/" + id, headers);
			int statusCode = response.getStatusCode();
			if (statusCode == 200) {
				BufferedReader in = new BufferedReader(new InputStreamReader(response.getInputStream()));
				String inputline;
				StringBuffer results = new StringBuffer();
				while ((inputline = in.readLine()) != null) {
					results.append(inputline);
				}
				in.close();
				Sprint sprint = new Sprint();
				try {
					JSONObject obj = new JSONObject(results.toString());
					sprint.setId(obj.getInt("id"));
					sprint.setName(obj.getString("name"));
					sprint.setState(obj.getString("state"));
					sprint.setUrl(obj.getString("self"));
					DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
					Date startDate = df.parse(obj.getString("startDate"));
					Date endDate = df.parse(obj.getString("endDate"));
					sprint.setStartDate(startDate);
					sprint.setEndDate(endDate);
					return sprint;
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (java.text.ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				String responseData = new String(response.getData());
				System.out.println("Action failed. Status code " + statusCode + " was returned with response data: "
						+ responseData);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ModuleException e) {
			e.printStackTrace();
		}
		return null;

	}

	public Sprint getActiveSprint() {
		try {
			JIRAHttpConnection conn = new JIRAHttpConnection(new URI(Config.getProperty(Config.JIRA_HOST)),
					VaultUtil.getSSOUsername(), VaultUtil.getSSOPassword().toCharArray(), "JIRA");

			NVPair[] headers = { new NVPair("Content-Type", "application/json"), new NVPair("state", "active") };
			HTTPResponse response = conn.Get("/jira/rest/agile/1.0/board/154082/sprint", headers);
			int statusCode = response.getStatusCode();
			if (statusCode == 200) {
				BufferedReader in = new BufferedReader(new InputStreamReader(response.getInputStream()));
				String inputline;
				StringBuffer results = new StringBuffer();
				while ((inputline = in.readLine()) != null) {
					results.append(inputline);
				}
				in.close();
				Sprint sprint = new Sprint();
				try {
					JSONObject obj = new JSONObject(results.toString());
					JSONArray array = obj.getJSONArray("values");
					for (int i = 0; i < array.length(); i++) {
						JSONObject jsonSprint = array.getJSONObject(i);
						sprint.setId(jsonSprint.getInt("id"));
						sprint.setName(jsonSprint.getString("name"));
						sprint.setState(jsonSprint.getString("state"));
						sprint.setUrl(jsonSprint.getString("self"));
						DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
						Date startDate = df.parse(jsonSprint.getString("startDate"));
						Date endDate = df.parse(jsonSprint.getString("endDate"));
						sprint.setStartDate(startDate);
						sprint.setEndDate(endDate);
					}
					return sprint;
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (java.text.ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				String responseData = new String(response.getData());
				System.out.println("Action failed. Status code " + statusCode + " was returned with response data: "
						+ responseData);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ModuleException e) {
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * Project
	 */

	public Project getProjectByName(String name) {
		try {
			JIRAHttpConnection conn = new JIRAHttpConnection(new URI(Config.getProperty(Config.JIRA_HOST)),
					VaultUtil.getSSOUsername(), VaultUtil.getSSOPassword().toCharArray(), "JIRA");

			NVPair[] headers = { new NVPair("name", name), new NVPair("Content-Type", "application/json") };
			HTTPResponse response = conn.Get("/jira/rest/agile/latest/board", headers);
			int statusCode = response.getStatusCode();
			if (statusCode == 200) {
				BufferedReader in = new BufferedReader(new InputStreamReader(response.getInputStream()));
				String inputline;
				StringBuffer results = new StringBuffer();
				while ((inputline = in.readLine()) != null) {
					results.append(inputline);
				}
				in.close();
				Project project = new Project();
				try {
					JSONObject obj = new JSONObject(results.toString());
					JSONArray array = obj.getJSONArray("values");
					for (int i = 0; i < array.length(); i++) {
						JSONObject json = array.getJSONObject(i);
						project.setId(json.getInt("id"));
						project.setName(json.getString("name"));
						project.setUrl(json.getString("self"));
						project.setType(json.getString("type"));
						return project;
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				String responseData = new String(response.getData());
				System.out.println("Action failed. Status code " + statusCode + " was returned with response data: "
						+ responseData);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ModuleException e) {
			e.printStackTrace();
		}
		return null;
	}

	public Map<String, Integer> sortByValue(HashMap<String, Double> hashMap) {
		List list = new LinkedList(hashMap.entrySet());
		Collections.sort(list, new Comparator() {

			@Override
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o2)).getValue()).compareTo(((Map.Entry) (o1)).getValue());
			}
		});

		Map result = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	public static void main(String[] args) {
		OMSPService test = new OMSPService();
		System.out.println(test.getTeamVelocityBySprintName("OMSP_Sprint14"));
//		 System.out.println(test.getIssuesBySprintName("OMSP_Sprint14"));
//		test.getChartStoryPointListBySprintName("OMSP_Sprint15");
		
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//		Date ed = new Date();
//		try {
//			Date d = sdf.parse("2018-01-03");
//			System.out.println(DateUtil.getWorkingDaysBetweenTwoDates(ed, d));
//		} catch (java.text.ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

}
