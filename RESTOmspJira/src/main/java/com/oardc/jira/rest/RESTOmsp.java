package com.oardc.jira.rest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.oardc.jira.model.ChartImage;
import com.oardc.jira.model.Engineer;
import com.oardc.jira.model.Velocity;
import com.oardc.jira.service.OMSPService;


@Path("/omsp")
public class RESTOmsp {
	
	/*
	 * 	URL Structure
	 *  /omsp/sprintVelocity/{sprintName}	: Get sprint engineers velocity
	 *  /omsp/sprintVelocityChart/{sprintName} : Get URL of chart for sprint engineers velocity
	 *  
	 *  /omsp/sprintEffort/{SprintName}	: get sprint effort list (total story-points) for the sprint
	 *  /omsp/sprintEffortChart/{sprintName} :get URL of chart for sprint effort list (total story-points) for the sprint
	 */
	
	@GET
	@Path("/sprintEffort/{sprintName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Engineer[] getEffortListBySprintName(@PathParam("sprintName") String sprintName) {
		OMSPService service = new OMSPService();
		Engineer[] result = new Engineer[0];
		HashMap<String, Double> response = service.getStoryPointListBySprintName(sprintName);
		if (response.isEmpty()) {
			return result;
		}
		Map<String,Integer> sortedMap = service.sortByValue(response);
		Iterator i = sortedMap.entrySet().iterator();
		int count = 0;
		result = new Engineer[sortedMap.size()];
		while (i.hasNext()) {
			Map.Entry<String, Double> me = (Map.Entry) i.next();
			String email = me.getKey().toString();
			double points = 0.0;
			try {
				points = Double.parseDouble(me.getValue().toString());
			}catch(NumberFormatException e){
			}
			result[count] = new Engineer(email.substring(0, email.indexOf("@")), email, points);
			count++;
		}
		return result;
	}
	
	@GET
	@Path("/sprintEffortChart/{sprintName}")
	@Produces(MediaType.APPLICATION_JSON)
	public ChartImage getChartEffortListBySprintName(@Context ServletContext context, @PathParam("sprintName") String sprintName) {
		OMSPService service = new OMSPService();
		return service.getChartStoryPointListBySprintName(context, sprintName);
	}
	
	@GET
	@Path("/sprintVelocity/{sprintName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Velocity[] getTeamVelocityBySprintName(@PathParam("sprintName") String sprintName) {
		OMSPService service = new OMSPService();
		Velocity[] result = new Velocity[0];
		HashMap<String, Double> response = service.getTeamVelocityBySprintName(sprintName);
		if (response.isEmpty()) {
			return result;
		}
		Map<String,Integer> sortedMap = service.sortByValue(response);
		Iterator i = sortedMap.entrySet().iterator();
		int count = 0;
		result = new Velocity[sortedMap.size()];
		while (i.hasNext()) {
			Map.Entry<String, Double> me = (Map.Entry) i.next();
			String name = me.getKey().toString();
			double points = 0.0;
			try {
				points = Double.parseDouble(me.getValue().toString());
			}catch(NumberFormatException e){
			}
			if (name.contains("@")) {
				name = name.substring(0, name.indexOf("@"));
			}
			result[count] = new Velocity(name, points);
			count++;
		}
		return result;
	}
	
	@GET
	@Path("/sprintVelocityChart/{sprintName}")
	@Produces(MediaType.APPLICATION_JSON)
	public ChartImage getChartTeamVelocityBySprintName(@Context ServletContext context, @PathParam("sprintName") String sprintName) {
		OMSPService service = new OMSPService();
		return service.getChartTeamVelocityBySprintName(context, sprintName);
	}
}

