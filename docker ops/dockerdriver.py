#!/usr/bin/python
# -*- coding: UTF-8 -*-
import getopt
import time
import re
import os
import ConfigParser
import requests
import json
import shutil
import socket
import sys
from metadata.metadata_repo import MetadataRepository
from common.utils import *
from common.log import *
class DockerDriver:
    def __init__(self):
	# the manager address
        self.manager = 'http://cdcjp64.cn.oracle.com:4243/v1.24'
        self.http_result = None
        self.serviceId = None
        self.header = {"Content-Type": "application/json"}
        self.containerId =None
        self.errmsg = ''
        self.owner = None
        self.parameterfile = None
        self.retirefile = None
        self.servicedata = {}
        self.metadata_repo = MetadataRepository()  

    def set_service_request_info(self, servicetype, serviceversion, owner, manager, tenant, project):
	# input the user info
        self.owner = owner
        self.servicedata['servicetype'] = servicetype
        self.servicedata['serviceversion'] = serviceversion
        self.servicedata['owner'] = owner
        self.servicedata['manager'] = manager
        self.servicedata['tenant'] = tenant
        self.servicedata['project'] = project
        self.servicedata['startdate'] = date_now()
        self.servicedata['expiredate'] = date_dayslater(30)
        # self.servicedata['servicetype'] = servicetype
        # self.servicedata['serviceversion'] = serviceversion
		# create the service
        if self.create_service():
            return True
        else:
            self.errmsg = "Exit, please check to this app's json file, %s not created successed."%self.servicedata['serviceid']
            return False

    def provision_docker_service(self):
        self.servicedata['status'] = 'running'
        
        if not self.metadata_repo.provision_dockerservice(self.servicedata):
            self.errmsg = 'Add metadata for docker service %s failed' % self.servicedata['serviceid']
            return False
        
        return True
        
            
    def load_docker_service(self, serviceid, owner):
        self.servicedata = self.metadata_repo.get_dockerservice_info(serviceid)
        if not self.servicedata :
            self.errmsg = 'Load Docker Service %s info failed' % serviceid
            return False
            
        if self.servicedata['owner'] != owner:
            self.errmsg = 'Docker Service %s is owned by %s, could not used by %s' % (serviceid, self.servicedata['owner'], owner)
            return False
       
        result = self.delete_service(self.servicedata['serviceid'])
        if not result[0]:
            self.errmsg = result[1]
            return False
        return True
        
    def retire_docker_service(self):

        if not self.metadata_repo.retire_dockerservice(self.servicedata['serviceid']):
            self.errmsg = 'Retire Docker Service %s metadata info failed' % self.servicedata['serviceid']
            return False
            
        return True

        
    def create_service(self):
	# create serId
        currentTime = str(int(round(time.time() * 1000)))
        if self.servicedata['servicetype'] == 'db':
            self.servicedata['serviceid'] = self.servicedata['servicetype']+self.servicedata['serviceversion']+currentTime
	# copy the post file base on service type and version
            if self.servicedata['serviceversion'] is "11204":
                data = json.load(open('./swarm/sshdb11g.json','r'))
            else:
                data = json.load(open('./swarm/sshdb12c.json','r'))
            data['Name']=self.servicedata['serviceid']
            data["TaskTemplate"]["ContainerSpec"]["Image"]='oardcdevops-docker-local.dockerhub-den.oraclecorp.com/database'+self.servicedata['serviceversion']+':ee-default'
        elif self.servicedata['servicetype'] == "wls":
            
            self.servicedata['serviceid'] = self.servicedata['servicetype']+self.servicedata['serviceversion']+currentTime

            data = json.load(open('./swarm/sshwls.json','r'))
            data['Name']=self.servicedata['serviceid']
            data["TaskTemplate"]["ContainerSpec"]["Image"]='oardcdevops-docker-local.dockerhub-den.oraclecorp.com/weblogic'+self.servicedata['serviceversion']+':dev'
	#check HTTP response
        url='%s/services/create'%self.manager
        header = {"Content-Type": "application/json"}
        response = requests.post(url, data=json.dumps(data), headers=header)
        print 'Show HTTP return result'
        print '******'
        self.http_result =str(response.status_code)
        print response.status_code
        print response.text
        print '*******'
        print "\"HTTP\": \""+self.http_result+" \""
        self.serviceId = response.json()['ID'] 
        if self.check_if_post_success():
            return self.servicedata['serviceid']        
        else:   
            print 'Create service %s failed.'%self.servicedata['serviceid']
        
    def check_if_post_success(self):
        if(self.http_result == '200' or  self.http_result == '201'):
            print "POST is successed, Next check the task status."
            time.sleep(10)
            url = '%s/tasks?filters={%%22service%%22:{%%22%s%%22:true}}'%(self.manager,self.serviceId)
            response = requests.get(url)
            data = response.text

            if data.startswith('['):
                data = data[1:-2]
            try:
                data = json.loads(data)
                state= data["Status"]["State"]
                nodeId=data["NodeID"]
                self.containerId = data["Status"]["ContainerStatus"]["ContainerID"]
            except ValueError, e:
                print '************************'
                print data
                print '************************'
                print 'Request rejected'
                return False
            url = '%s/nodes/%s'%(self.manager,nodeId)
            response = requests.get(url)
            data = response.json()
            self.servicedata['host'] = data["Description"]["Hostname"]
            url = '%s/services/%s'%(self.manager,self.serviceId)
            response = requests.get(url)
            data = response.json()
            self.servicedata['sshport'] = data["Endpoint"]["Ports"][0]["PublishedPort"]
            self.servicedata['serviceport'] = data["Endpoint"]["Ports"][1]["PublishedPort"]
            print "*********************"
            print "APP SVRID: %s , Task stat: %s , APP_NODE: %s , HOST_PORT: sshport: %s serviceport: %s"%(self.servicedata['serviceid'],state,self.servicedata['host'],self.servicedata['sshport'],self.servicedata['serviceport'])
            print self.servicedata['host']
            print self.servicedata['serviceport']
            wtime = 10
            while 'running'  not in state:
                print "The Deployment state is not RUNNING state, the max Waitting time is 5 mins for this. Now time is %d seconds"%wtime
                if(wtime >= 300):
                    print "The deployment hardware time is over %d , please check hardware configuration file."%wtime
                    return False
                time.sleep(10)
                wtime +=10
                url = '%s/tasks?filters={%%22service%%22:{%%22%s%%22:true}}'%(self.manager,self.serviceId)
                response = requests.get(url)
                
                data = json.load(response.json())
                state= data["Status"]["State"]
            
            if 'wls' in self.servicedata['serviceid']:
                return self.get_continer_serid_from_api_wls()
            elif  self.servicedata['serviceid'].startswith('db'):
                return self.get_continer_serid_from_api_db()
            else:
                print "Exit, please check to this app's json file, %s not created successed."%self.servicedata['serviceid']
                return False
            
    def get_continer_serid_from_api_wls(self):
        wtime = 30
        time.sleep(60)
        while (wtime <= 300):
            url = 'http://%s:%s/console/login/LoginForm.jsp'%(self.servicedata['host'],self.servicedata['serviceport'])
            response = requests.get(url)
            
            if 'Oracle WebLogic Server Administration Console' in response.text:
                url='http://%s:4243/v1.24/containers/%s/exec'%(self.servicedata['host'],self.containerId)
                header = {"Content-Type": "application/json"}
                post_file ='./swarm/createexecwls.json'
                json_data = json.load(open(post_file,'r'))
                response = requests.post(url, data=json.dumps(json_data), headers=header)
                exceData = response.json()
                exceId = exceData['Id']       
                url='http://%s:4243/v1.24/exec/%s/start'%(self.servicedata['host'],exceId)
                post_file = './swarm/startexec.json'
                json_data = json.load(open(post_file,'r'))
                response = requests.post(url, data=json.dumps(json_data), headers=header)
                time.sleep(5)    
                if self.check_port(self.servicedata['host'],self.servicedata['sshport']):
                    print "Check sshwls admin server whether Running"
                    print "Weblogic AdminServer is RUNNING, Good luck. Compelted"
                    self.servicedata['serviceid'] = self.servicedata['serviceid']
                    self.servicedata['host'] = self.servicedata['host']
                    self.servicedata['serviceport'] = self.servicedata['serviceport']
                    self.servicedata['serviceuser'] = 'weblogic'
                    self.servicedata['servicepwd'] = 'Welcome1'
                    self.servicedata['sshuser'] = 'oracle'
                    self.servicedata['sshpwd'] = 'oracle'
                    self.servicedata['sshrootpwd'] = '111111'
                    self.servicedata['sshport'] = str(self.servicedata['sshport'])
                    print "The %s service is created. End script"%self.servicedata['serviceid']
                    return True
                else:
                    print "SSH %s is failed."%self.servicedata['sshport']
                    return False               
            print "Now has checked for %d seconds, please waitting for max 300s."%wtime
            time.sleep(30)
            wtime += 30
            print wtime                
        print "Over %d seconds, please chech this task which tashname is %s"%(wtime,self.servicedata['serviceid'])
        return  False
        
    def get_continer_serid_from_api_db(self):
        header = {"Content-Type": "application/json"}
        wtime=30
        max_time = 600;
        while wtime <=max_time :
            url = 'http://%s:4243/v1.24/containers/%s/logs?stdout=1'%(self.servicedata['host'],self.containerId)
            response = requests.get(url)
            if "DATABASE IS READY TO USE!" in response.text:
                print "DATABASE IS READY TO USE!"
                if self.servicedata['serviceversion'].startswith('11'):
                    url = 'http://%s:4243/v1.24/containers/%s/exec'%(self.servicedata['host'],self.containerId)
                    post_file = './swarm/createexecdb11g.json'
                    json_data = json.load(open(post_file,'r'))
                    response = requests.post(url, data=json.dumps(json_data), headers=header)
                else:
                    url ='http://%s:4243/v1.24/containers/%s/exec'%(self.servicedata['host'],self.containerId)
                    post_file = './swarm/createexecdb12c.json'
                    json_data = json.load(open(post_file,'r'))
                    response = requests.post(url, data=json.dumps(json_data), headers=header)
                exceData = response.json()
                exceId = exceData["Id"]
                url='http://%s:4243/v1.24/exec/%s/start'%(self.servicedata['host'],exceId)
                post_file = './swarm/startexec.json'
                json_data = json.load(open(post_file,'r'))
                response = requests.post(url, data=json.dumps(json_data), headers=header)
                time.sleep(5)
                if self.check_port(self.servicedata['host'],self.servicedata['sshport']):
                    self.servicedata['host'] = self.servicedata['host']
                    self.servicedata['serviceport'] = self.servicedata['serviceport']
                    self.servicedata['serviceuser'] = 'sys'
                    self.servicedata['servicepwd'] = 'Welcome1'
                    self.servicedata['sshuser'] = 'oracle'
                    self.servicedata['sshpwd'] = 'oracle'
                    self.servicedata['sshrootpwd'] = '111111'
                    self.servicedata['sshport'] = str(self.servicedata['sshport'])
                    print "SSH %s is open."%self.servicedata['sshport']      
                    print "The %s service is created. End script"%self.servicedata['serviceid']
                    return True
            else:
                print "The DATABASE is not ready, please waitting max 5 - 10 mins.Now is %d seconds."%wtime
                time.sleep(30)
                wtime+=30
                    
        print "Exit, please check system log, %s not created successed."%self.servicedata['serviceid']   
        print response.text
        return False
    
        
    def check_port(self,address,port):
        s=socket.socket(socket.AF_INET,socket.SOCK_STREAM)
        s.settimeout(3)
        try:
                s.connect((address,port))
                print "Check ssh port %s OK" %port
                return True
        except Exception,e:
                print "Check ssh port %s failed:%s" %(port,e)
                return False


    def delete_service(self,serId):
        if serId != None:
            self.servicedata['serviceid'] = serId
        master = 'cdcjp64.cn.oracle.com'
        url= 'http://%s:4243/services' % (master)
        out = requests.get(url)
        out = out.text
        print self.servicedata['serviceid']
        if self.servicedata['serviceid'] not in out:
            errmsg =  "\'Can not find the swarm service ID: "+self.servicedata['serviceid']+", failed. The %s is not exist\'"%self.servicedata['serviceid']
            print errmsg
            return [False,errmsg]
        else:
            wtime = 0
            while wtime <= 60:
                url= 'http://%s:4243/services/%s' % (master, self.servicedata['serviceid'])
                requests.delete(url)
                url = 'http://%s:4243/services' % (master)
                out = requests.get(url)
                if self.servicedata['serviceid'] not in out:
                    print  "\'The swarm service %s is removed. Exit for Succecced.\'"%self.servicedata['serviceid']
                    return [True,'Misson complete']
                else:
                    print "Please waitting for one minates to removed the service, now is " + wtime + " seconds."
                    wtime += 10
                    time.sleep(10)



