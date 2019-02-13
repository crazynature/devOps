# Description:
#   Allows hubot to run commands for infra self service API
#	*sprint points - individual members points in a sprint
#	*sprint velocity - speed of points burndown per day in a sprint
# 
# Author:
#   Joseph Che

host = "http://infbjvm560.cn.oracle.com:8080/RESTOmspJira/service"
chartHost = "http://infbjvm560.cn.oracle.com:8080/chart"

default_msg = "This is what I have found:"

module.exports = (robot) ->

   # help message
   robot.respond /jira help/i, (res) ->
     res.send "How can I help you today?"
     res.send "I can perform  the following commands:"
     res.send "[1]   #{robot.name} chart sprint-points <sprint_name> : list sum of storypoints for every engineer in the sprint specified. Chart optional. e.g. #{robot.name} chart sprint-points OMSP_SprintX"
     res.send "[2]   #{robot.name} chart sprint-velocity <sprint_name> : list the team members completion velocity. Chart optional."
   
   robot.respond /sprint-points (.*)/i, (msg) ->
      sprintName = msg.match[1]
      if (sprintName.length < 1)
         msg.send "opps, you did not specify the <sprint_name>. Please try again"
         return
      msg.send "Hang on buddy, retrieving your data now..."
      make_headers = ->
         ret =
            'content-type': 'application/json'
      headers = make_headers()
      robot.http("#{host}/omsp/sprintEffort/#{sprintName}").headers(headers).get() (err, res, body) ->
         if err
            msg.send "I got an error :("
            #msg.send "#{err}"
            return
         msg.send "#{default_msg}"

         if res.statusCode is 200
            arr = JSON.parse body
            if null is arr
               msg.send "Opps, seems that the sprint name don't exist, or no developers are assigned to it. Please check it."
               return
            msg.send "Found #{arr.engineer.length} engineers in #{sprintName}:"
            for j in arr.engineer
               msg.send "#{j.name}: #{j.storyPoints}"
         else
            msg.send "Somethings wrong :("
            msg.send "Either the sprint do not exist or no engineers are assigned. Please check it."

   robot.respond /chart sprint-points (.*)/i, (msg) ->
      sprintName = msg.match[1]
      if (sprintName.length < 1)
         msg.send "opps, you did not specify the <sprint_name>. Please try again"
         return
      msg.send "Hang on buddy, retrieving your data now..."
      make_headers = ->
         ret =
            'content-type': 'application/json'
      headers = make_headers()
      robot.http("#{host}/omsp/sprintEffortChart/#{sprintName}").headers(headers).get() (err, res, body) ->
         if err
            msg.send "I got an error :("
            return
         msg.send "#{default_msg}"

         if res.statusCode is 200
            body = JSON.parse body
            msg.send "#{chartHost}/#{body.url}"
            return
         else
            msg.send "Somethings wrong :("
            msg.send "Either the sprint do not exist or no engineers are assigned. Please check it."

   robot.respond /sprint-velocity (.*)/i, (msg) ->
      sprintName = msg.match[1]
      if (sprintName.length < 1)
         msg.send "opps, you did not specify the <sprint_name>. Please try again"
         return
      msg.send "Hang on buddy, retrieving your data now..."
      make_headers = ->
         ret =
            'content-type': 'application/json'
      headers = make_headers()
      robot.http("#{host}/omsp/sprintVelocity/#{sprintName}").headers(headers).get() (err, res, body) ->
         if err
            msg.send "I got an error :("
            #msg.send "#{err}"
            return
         msg.send "#{default_msg}"

         if res.statusCode is 200
            arr = JSON.parse body
            if null is arr
               msg.send "Opps, seems that the sprint name don't exist, or no developers are assigned to it. Please check it."
               return
            msg.send "Calculated velocity for #{arr.velocity.length} engineers in #{sprintName}:"
            for j in arr.velocity
               msg.send "#{j.name}: #{j.velocity} points/day"
            msg.send "Note: This list do not include everyone if they have not completed any points."
         else
            msg.send "Somethings wrong :("
            msg.send "Either the sprint do not exist or no engineers are assigned. Please check it."

   robot.respond /chart sprint-velocity (.*)/i, (msg) ->
      sprintName = msg.match[1]
      if (sprintName.length < 1)
         msg.send "opps, you did not specify the <sprint_name>. Please try again"
         return
      msg.send "Hang on buddy, retrieving your data now..."
      make_headers = ->
         ret =
            'content-type': 'application/json'
      headers = make_headers()
      robot.http("#{host}/omsp/sprintVelocityChart/#{sprintName}").headers(headers).get() (err, res, body) ->
         if err
            msg.send "I got an error :("
            return
         msg.send "#{default_msg}"

         if res.statusCode is 200
            body = JSON.parse body
            msg.send "#{chartHost}/#{body.url}"
            return
         else
            msg.send "Somethings wrong :("
            msg.send "Either the sprint do not exist or no engineers are assigned. Please check it."

