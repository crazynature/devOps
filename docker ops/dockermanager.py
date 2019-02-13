#!/usr/bin/python
# -*- coding: UTF-8 -*-
import sys
import getopt

from common.log import *
from common.mail import Mail
from common.utils import *
from metadata.metadata_repo import MetadataRepository
from dockerdriver import DockerDriver

class DockerManager:
	def docker_provisioning(self,para_att):
		driver = DockerDriver()
		
		# Setup request info
		if not driver.set_service_request_info(para_att[0].lower(), para_att[1].lower(), para_att[2].lower(), para_att[3].lower(), para_att[4].lower(), para_att[5]):
			return [False, driver.errmsg, driver.servicedata]

		
		# Provision Docker service
		if not driver.provision_docker_service():
			return [False, driver.errmsg, driver.servicedata]
			
		return [True, 'ok', driver.servicedata]


		
	def docker_retire_service(self,para_att):
		driver = DockerDriver()    
		

		
		# Load Docker service info
		if not driver.load_docker_service(para_att[0], para_att[1].lower()) :
			return [False, driver.errmsg]
		  
		# Retire Docker service
		if not driver.retire_docker_service():
			return [False, driver.errmsg]
		
		return [True, 'ok']
		

		
if __name__ == "__main__": 
	docker_manager = DockerManager()
	try:
		opts, args = getopt.getopt(sys.argv[1:], 'prd', ['provision', 'retire', 'delete'])
	except getopt.error, msg:
		print msg
		sys.exit(2)
	print args
	for o, a in opts:
		# Provision Docker Service
		if o in ("-p", "--provision"):
			ret =docker_manager.docker_provisioning(args)
			mail = Mail()
			mail.mail_dockerservice(args[2].lower(), args[3].lower(), ret)
			
			if not ret[0]:
				logging.error(ret[1])
				sys.exit(1)
		
		# Retire Docker Service, run after delete service by shell
		elif o in ("-r", "--retire"):
			ret = docker_manager.docker_retire_service(args)
			mail = Mail()
			mail.mail_retire_dockerservice(args[0], args[1].lower(), ret)
			
			if not ret[0]:
				logging.error(ret[1])
				sys.exit(3)
					
