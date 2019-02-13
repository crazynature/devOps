# Description:
#   Interact with your Jenkins CI server
#
# Dependencies:
#   None
#
# Configuration:
#   Auth should be in the "user:password" format.
#
# Commands:
#   hubot jenkins b <jobNumber> - builds the job specified by jobNumber. List jobs to get number.
#   hubot jenkins build <job> - builds the specified Jenkins job
#   hubot jenkins build <job>, <params> - builds the specified Jenkins job with parameters as key=value&key2=value2
#   hubot jenkins list <filter> - lists Jenkins jobs
#   hubot jenkins describe <job> - Describes the specified Jenkins job
#   hubot jenkins last <job> - Details about the last build for the specified Jenkins job
#   hubot jenkins server - shows the current Jenkins server it is connected to
#
# Author:
#   dougcole
#
# Updated by:
#   Joseph Che 

querystring = require 'querystring'

# Global Params
# auth - <username>:<password> : use a global identidy for chatbot access. Projects need to add the user in order for chatbot to access it.
# url - URL of the jenkins instance
auth = "oardc-omsp_sg@oracle.com:!QAZ2wsx"
url = "http://slcifaa.us.oracle.com:8114/jenkins/oardc-jenkins"
default_msg = "OK, here's what I have found."

# Holds a list of jobs, so we can trigger them with a number
# instead of the job's name. Gets populated on when calling
# list.
jobList = []

jenkinsBuildById = (msg) ->
  # Switch the index with the job name
  job = jobList[parseInt(msg.match[1]) - 1]

  if job
    msg.match[1] = job
    jenkinsBuild(msg)
  else
    msg.reply "I couldn't find that job. Try `jenkins list` to get a list."

jenkinsBuild = (msg, buildWithEmptyParameters) ->
    job = querystring.escape msg.match[1]
    params = msg.match[3]
    command = if buildWithEmptyParameters then "buildWithParameters" else "build"
    path = if params then "#{url}/job/#{job}/buildWithParameters?#{params}" else "#{url}/job/#{job}/#{command}"

    req = msg.http(path)

    authBuffer = new Buffer(auth).toString('base64')
    req.headers Authorization: "Basic #{authBuffer}"

    req.header('Content-Length', 0)
    req.post() (err, res, body) ->
        if err
          msg.reply "Jenkins says: #{err}"
        else if 200 <= res.statusCode < 400 # Or, not an error code.
          msg.reply "(#{res.statusCode}) Build started for #{job} #{url}/job/#{job}"
        else if 400 == res.statusCode
          jenkinsBuild(msg, true)
        else if 404 == res.statusCode
          msg.reply "Build not found, double check that it exists and is spelt correctly."
        else
          msg.reply "Jenkins says: Status #{res.statusCode} #{body}"

jenkinsDescribe = (msg) ->
    job = msg.match[1]

    path = "#{url}/job/#{job}/api/json"

    req = msg.http(path)

    authBuffer = new Buffer(auth).toString('base64')
    req.headers Authorization: "Basic #{authBuffer}"
    msg.send "#{default_msg}"
    req.header('Content-Length', 0)
    req.get() (err, res, body) ->
        if err
          msg.send "Jenkins says: #{err}"
        else
          response = ""
          try
            content = JSON.parse(body)
            response += "JOB: #{content.displayName}\n"
            response += "URL: #{content.url}\n"

            if content.description
              response += "DESCRIPTION: #{content.description}\n"

            response += "ENABLED: #{content.buildable}\n"
            response += "STATUS: #{content.color}\n"

            tmpReport = ""
            if content.healthReport.length > 0
              for report in content.healthReport
                tmpReport += "\n  #{report.description}"
            else
              tmpReport = " unknown"
            response += "HEALTH: #{tmpReport}\n"

            parameters = ""
            for item in content.actions
              if item.parameterDefinitions
                for param in item.parameterDefinitions
                  tmpDescription = if param.description then " - #{param.description} " else ""
                  tmpDefault = if param.defaultParameterValue then " (default=#{param.defaultParameterValue.value})" else ""
                  parameters += "\n  #{param.name}#{tmpDescription}#{tmpDefault}"

            if parameters != ""
              response += "PARAMETERS: #{parameters}\n"

            msg.send response

            if not content.lastBuild
              return

            path = "#{url}/job/#{job}/#{content.lastBuild.number}/api/json"
            req = msg.http(path)
            authBuffer = new Buffer(auth).toString('base64')
            req.headers Authorization: "Basic #{authBuffer}"

            req.header('Content-Length', 0)
            req.get() (err, res, body) ->
                if err
                  msg.send "Jenkins says: #{err}"
                else
                  response = ""
                  try
                    content = JSON.parse(body)
                    console.log(JSON.stringify(content, null, 4))
                    jobstatus = content.result || 'PENDING'
                    jobdate = new Date(content.timestamp);
                    response += "LAST JOB: #{jobstatus}, #{jobdate}\n"

                    msg.send response
                  catch error
                    msg.send error

          catch error
            msg.send error

jenkinsLast = (msg) ->
    job = msg.match[1]

    path = "#{url}/job/#{job}/lastBuild/api/json"

    req = msg.http(path)

    authBuffer = new Buffer(auth).toString('base64')
    req.headers Authorization: "Basic #{authBuffer}"
    msg.send "#{default_msg}"
    req.header('Content-Length', 0)
    req.get() (err, res, body) ->
        if err
          msg.send "Jenkins says: #{err}"
        else
          response = ""
          try
            content = JSON.parse(body)
            response += "NAME: #{content.fullDisplayName}\n"
            response += "URL: #{content.url}\n"

            if content.description
              response += "DESCRIPTION: #{content.description}\n"

            response += "BUILDING: #{content.building}\n"

            msg.send response

jenkinsHelp = (msg, robot) ->
   msg.send "Hi, I can help you with the following Jenkins commands."
   msg.send "[1]   #{robot.name} jenkins b <jobNumber> - builds the job specified by jobNumber. List jobs to get number."
   msg.send "[2]   #{robot.name} jenkins build <job> - builds the specified Jenkins job"
   msg.send "[3]   #{robot.name} jenkins build <job>, <params> - builds the specified Jenkins job with parameters as key=value&key2=value2"
   msg.send "[4]   #{robot.name} jenkins list <filter> - lists Jenkins jobs"
   msg.send "[5]   #{robot.name} jenkins describe <job> - Describes the specified Jenkins job"
   msg.send "[6]   #{robot.name} jenkins last <job> - Details about the last build for the specified Jenkins job"
   msg.send "[7]   #{robot.name} jenkins server - Shows which Jenkins server it is connected to"

jenkinsServer = (msg) ->
   msg.send "I am currently connected to:\n#{url}"

jenkinsList = (msg) ->
    filter = new RegExp(msg.match[2], 'i')
    req = msg.http("#{url}/api/json")

    authBuffer = new Buffer(auth).toString('base64')
    req.headers Authorization: "Basic #{authBuffer}"
    msg.send "#{default_msg}"
    req.get() (err, res, body) ->
        response = ""
        if err
          msg.send "Jenkins says: #{err}"
        else
          try
            #msg.send body
            content = JSON.parse(body)
            for job in content.jobs
              # Add the job to the jobList
              index = jobList.indexOf(job.name)
              if index == -1
                jobList.push(job.name)
                index = jobList.indexOf(job.name)

              state = if job.color == "red"
                        "FAIL"
                      else if job.color == "aborted"
                        "ABORTED"
                      else if job.color == "aborted_anime"
                        "CURRENTLY RUNNING"
                      else if job.color == "red_anime"
                        "CURRENTLY RUNNING"
                      else if job.color == "blue_anime"
                        "CURRENTLY RUNNING"
                      else "PASS"

              if (filter.test job.name) or (filter.test state)
                response += "[#{index + 1}] #{state} #{job.name}\n"
            msg.send response
          catch error
            msg.send error

module.exports = (robot) ->
  robot.respond /j(?:enkins)? build ([\w\.\-_ ]+)(, (.+))?/i, (msg) ->
    jenkinsBuild(msg, false)

  robot.respond /j(?:enkins)? b (\d+)/i, (msg) ->
    jenkinsBuildById(msg)

  robot.respond /j(?:enkins)? help/i, (msg) ->
    jenkinsHelp(msg, robot)

  robot.respond /j(?:enkins)? list( (.+))?/i, (msg) ->
    jenkinsList(msg)

  robot.respond /j(?:enkins)? describe (.*)/i, (msg) ->
    jenkinsDescribe(msg)

  robot.respond /j(?:enkins)? last (.*)/i, (msg) ->
    jenkinsLast(msg)

  robot.respond /j(?:enkins)? server/i, (msg) ->
    jenkinsServer(msg)

  robot.jenkins = {
    list: jenkinsList,
    server: jenkinsServer,
    build: jenkinsBuild,
    describe: jenkinsDescribe,
    help: jenkinsHelp,
    last: jenkinsLast
}
