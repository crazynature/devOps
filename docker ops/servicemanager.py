#!/usr/bin/python
# -*- coding: UTF-8 -*-
import datetime
import errno
import json
import os
import re
import subprocess

from chef_wrapper import ChefWrapper
from clonetemplate import Template
from common.log import Logger
from common.mail import Mail
from common.slack_sender import SlackSender
from managevm import VmManager
from provisioningvm import VmProvisioning
from serviceproducer import ServiceProducer
from vmsnapshot import Snapshot
from winauisoprepare import WinAuIsoPrepare
from zabbix_wrapper import ZabbixWrapper
from dockermanager import DockerManager

class ServiceManager:
    def __init__(self):
        self.logger = Logger()
        self.operation = None

    def __del__(self):
        pass  # logging.info(self.serviceid + ' finished.')

    def service_parser(self, operation, parameters):
        s_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
        # Logging service start
        self.operation = operation
        self.logger.info('Common', 'ServiceManager', 'service_parser',
                         'START: Service request \"%s\" with parameter %s.' %
                         (self.operation, json.dumps(parameters)))

        try:
            # VM
            if operation == 'vmprovisioning':
                self.vm_provisioning(parameters)
            elif operation == 'vmupdate':
                self.vm_update(parameters)
            elif operation == 'vmretire':
                self.vm_retire(parameters)
            elif operation == 'vmextend':
                self.vm_extend(parameters)
            elif operation == 'vmtransfer':
                self.vm_transfer(parameters)
            elif operation == 'vmstart':
                self.vm_start(parameters)

            # Template
            elif operation == 'templatecreate':
                self.template_create(parameters)
            elif operation == 'templateretire':
                self.template_retire(parameters)

            # Snapshot
            elif operation == 'snapshotcreate':
                self.snapshot_create(parameters)
            elif operation == 'snapshotretire':
                self.snapshot_retire(parameters)
            elif operation == 'snapshotrefresh':
                self.snapshot_refresh(parameters)

            # Chef
            elif operation == 'chef_nd_bootstrap':
                self.__chef_nd_bootstrap(parameters)
            elif operation == 'chef_sw_install':
                self.__chef_sw_install(parameters)
            elif operation == 'chef_add_monitor':
                self.__chef_add_monitor(parameters)
            
            # docker
            elif operation == 'dockerservice_create':
                self.__dockerservice_create(parameters)
            elif operation == 'dockerservice_delete':
                self.__dockerservice_delete(parameter)
            
            # Zabbix
            elif operation == 'zabbix_sync_status':
                self.__zabbix_sync_status(parameters)
            elif operation == 'zabbix_check_monitor':
                self.__zabbix_check_monitor(parameters)

            # Others
            else:
                raise Exception(errno.EINVAL, "Unknown operation: %s !" % operation)

        except Exception as e:
            e_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
            self.logger.exception('Common', 'ServiceManager', 'service_parser', str(e))
            slack_sender = SlackSender()
            slack_sender.send_message(str(e), parameters['owner'], s_dt, e_dt,
                                      'Common', 'ServiceManager', 'service_parser')

        self.logger.info('Common', 'ServiceManager', 'service_parser',
                         'END  : Service request \"%s\" with parameter %s.' %
                         (self.operation, json.dumps(parameters)))
        return

    ####################################################################################################
    def vm_provisioning(self, parameters):
        s_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
        result = True
        stage_name = 'vm_provisioning()'
        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Start to Provisioning VM for \"%s\"' % parameters['owner'])

        chef_wrapper = None
        service_producer = None
        vm_name = ''

        try:
            ##################################################
            # Stage : Initialize
            stage_name = 'Initialize'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            service_producer = ServiceProducer()
            chef_wrapper = ChefWrapper(sid=parameters['serviceid'], op=self.operation, stage=stage_name)
            winauiso_prep = WinAuIsoPrepare(sid=parameters['serviceid'], op=self.operation, stage=stage_name)
            vm_provisioning = VmProvisioning(parameters['serviceid'], self.operation)
            if not vm_provisioning.load_driver():
                raise Exception(vm_provisioning.errormsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Pre-Provisioning VM
            stage_name = 'Pre-Provisioning VM'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            win_iso_info = None
            if re.match('^windows.*$', parameters['template'].lower()):
                winauiso_prep.set_stage(stage_name)
                win_iso_info = winauiso_prep.build_winiso(parameters)
                if not win_iso_info:
                    raise Exception(winauiso_prep.mail_msg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Provisioning VM
            stage_name = 'Provisioning VM'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            vm_provisioning.stage = stage_name
            vm_provisioning.setup_provisioning_parameter(parameters, win_iso_info)
            if not vm_provisioning.provisioning_vm():
                raise Exception(vm_provisioning.errormsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Chef Node Bootstrap
            stage_name = 'Chef Node Bootstrap'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            vm_name = vm_provisioning.get_vm_name()
            chef_wrapper.stage(stage_name)
            if 0 != chef_wrapper.load(vm_name, "", "", "", ""):
                raise Exception(chef_wrapper.mail_errmsg, {})

            if 0 != chef_wrapper.bootstrap(os.environ['OARDC_PT_ENVIRONMENT'], ["role[InfraVM]"], ""):
                raise Exception(chef_wrapper.mail_errmsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Verification
            stage_name = 'Verification'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            vm_provisioning.stage = stage_name
            verified = vm_provisioning.provisioning_vm_verify()
            # Send Verify mail to owner
            vm_provisioning.provisioning_vm_mail(verified)
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

        except Exception as e:
            e_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
            result = False

            # Error log
            self.logger.exception(parameters['serviceid'], self.operation, stage_name, e.args[0])

            # Mail
            mail = Mail()
            mail.mail_provisioning_fail(parameters, e.args[0])

            # Slack
            slack_sender = SlackSender()
            slack_sender.send_message(e.args[0], parameters['owner'], s_dt, e_dt,
                                      parameters['serviceid'], self.operation, stage_name)
        finally:
            if vm_name:
                service_producer.send_message(parameters['serviceid'], operation='zabbix_sync_status',
                                              message={'serviceid': parameters['serviceid'],
                                                       'owner': parameters['owner'],
                                                       'hostname': vm_name,
                                                       'action': 'audit'})
            if chef_wrapper:
                chef_wrapper.unload()

        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Finish to Provisioning VM for \"%s\"' % parameters['owner'])

        return result

    ####################################################################################################
    def vm_update(self, parameters):
        s_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
        result = True
        stage_name = ''
        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Start to Updating VM for \"%s\"' % parameters['owner'])

        service_producer = None
        chef_wrapper = None
        vm_manager = None

        try:
            ##################################################
            # Stage : Initialize
            stage_name = 'Initialize'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            service_producer = ServiceProducer()
            winauiso_prep = WinAuIsoPrepare(sid=parameters['serviceid'], op=self.operation, stage=stage_name)
            vm_manager = VmManager(parameters['serviceid'], self.operation)
            vm_provisioning = VmProvisioning(parameters['serviceid'], self.operation)
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Pre-Check
            stage_name = 'Pre-Check'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            vm_manager.stage = stage_name
            if not vm_manager.vm_checkupdate(parameters['hostname'], parameters['owner']):
                raise Exception(vm_manager.driver.errmsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Disable the VM host in Zabbix
            stage_name = 'Disable old VM host in Zabbix'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            service_producer.send_message(parameters['serviceid'], operation='zabbix_sync_status',
                                          message={'serviceid': parameters['serviceid'],
                                                   'owner': parameters['owner'],
                                                   'hostname': parameters['hostname'],
                                                   'action': 'shutdown'})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Delete Node and Client from Chef
            stage_name = 'Delete Node and Client from Chef'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            cmd_1_proc = subprocess.Popen(
                '/usr/bin/knife node bulk delete \"^%s.*\" -y >/dev/null 2>&1' % parameters['hostname'],
                stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
            cmd_2_proc = subprocess.Popen(
                '/usr/bin/knife client bulk delete \"^%s.*\" -y >/dev/null 2>&1' % parameters['hostname'],
                stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)

            if 0 != cmd_1_proc.returncode or 0 != cmd_2_proc.returncode:
                self.logger.error(
                    parameters['serviceid'], self.operation, stage_name,
                    'Failed(n:%d,c:%d) to delete node & client information of \"%s\" from Chef Server !' %
                    (cmd_1_proc.returncode, cmd_2_proc.returncode, parameters['hostname']))

            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Pre-Updating VM
            stage_name = 'Pre-Updating VM'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            win_iso_info = None
            if re.match('^windows.*$', parameters['template'].lower()):
                winauiso_prep.set_stage(stage_name)
                win_iso_info = winauiso_prep.build_winiso(parameters)
                if not win_iso_info:
                    raise Exception(winauiso_prep.mail_msg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Provisioning VM
            stage_name = 'Provisioning VM'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            vm_provisioning.stage = stage_name
            vm_provisioning.setup_provisioning_parameter(parameters, win_iso_info)
            if not vm_provisioning.load_driver():
                raise Exception(vm_provisioning.errormsg, {})
            if not vm_provisioning.provisioning_vm():
                # Mail to admin if need action
                if vm_provisioning.refresh and vm_provisioning.error > 0:
                    mailadmin = Mail()
                    mailadmin.mail_fail_needaction(parameters['owner'], 'VM refresh', vm_provisioning.errormsg)

                raise Exception(vm_provisioning.errormsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Retired old VM host in Zabbix
            stage_name = 'Retired old VM host in Zabbix'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            service_producer.send_message(parameters['serviceid'], operation='zabbix_sync_status',
                                          message={'serviceid': parameters['serviceid'],
                                                   'owner': parameters['owner'],
                                                   'hostname': parameters['hostname'],
                                                   'action': 'retired'})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Chef Node Bootstrap
            stage_name = 'Chef Node Bootstrap'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            vm_name = vm_provisioning.get_vm_name()
            chef_wrapper.stage(stage_name)
            rc = chef_wrapper.load(vm_name, "", "", "", "", os.environ['OARDC_PT_ENVIRONMENT'], ["role[InfraVM]"], "")
            if 0 != rc:
                raise Exception(chef_wrapper.mail_errmsg, {})

            rc = chef_wrapper.bootstrap()
            if 0 != rc:
                raise Exception(chef_wrapper.mail_errmsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Verification
            stage_name = 'Verification'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            vm_provisioning.stage = stage_name
            verified = vm_provisioning.provisioning_vm_verify()
            # Send Verify mail to owner
            vm_provisioning.provisioning_vm_mail(verified)
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

        except Exception as e:
            e_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
            result = False

            if ('Initialize' != stage_name and 'Pre-Check' != stage_name) and vm_manager:
                vm_manager.stage = 'Exception handling'
                vm_manager.vm_setup_status(parameters['hostname'], parameters['owner'], 'running')

            # Error log
            self.logger.exception(parameters['serviceid'], self.operation, stage_name, e.args[0])

            # Mail
            mail = Mail()
            mail.mail_provisioning_fail(parameters, e.args[0])

            # Slack
            slack_sender = SlackSender()
            slack_sender.send_message(e.args[0], parameters['owner'], s_dt, e_dt,
                                      parameters['serviceid'], self.operation, stage_name)
        finally:
            service_producer.send_message(parameters['serviceid'], operation='zabbix_sync_status',
                                          message={'serviceid': parameters['serviceid'],
                                                   'owner': parameters['owner'],
                                                   'hostname': parameters['hostname'],
                                                   'action': 'audit'})
            if chef_wrapper:
                chef_wrapper.unload()

        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Finish to Updating VM for \"%s\"' % parameters['owner'])

        return result

    ####################################################################################################
    def vm_retire(self, parameters):
        s_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
        result = True
        stage_name = ''
        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Start to Retire VM for \"%s\"' % parameters['owner'])

        service_producer = None

        try:
            ##################################################
            # Stage : Initialize
            stage_name = 'Initialize'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            service_producer = ServiceProducer()
            vm_manager = VmManager(parameters['serviceid'], self.operation)
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Retire VM
            stage_name = 'Retire VM'
            vm_manager.stage = stage_name
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            if not vm_manager.vm_retire(parameters['hostname'], parameters['owner']):
                raise Exception(vm_manager.driver.errmsg, {})

            mail = Mail()
            mail.mail_retire(parameters['hostname'], parameters['owner'], True, '')
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

        except Exception as e:
            e_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
            result = False

            # Error log
            self.logger.exception(parameters['serviceid'], self.operation, stage_name, e.args[0])

            # Mail
            mail = Mail()
            mail.mail_retire(parameters['hostname'], parameters['owner'], False, e.args[0])

            # Slack
            slack_sender = SlackSender()
            slack_sender.send_message(e.args[0], parameters['owner'], s_dt, e_dt,
                                      parameters['serviceid'], self.operation, stage_name)
        finally:
            service_producer.send_message(parameters['serviceid'], operation='zabbix_sync_status',
                                          message={'serviceid': parameters['serviceid'],
                                                   'owner': parameters['owner'],
                                                   'hostname': parameters['hostname'],
                                                   'action': 'audit'})

        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Finish to Retire VM for \"%s\"' % parameters['owner'])

        return result

    ####################################################################################################
    def vm_extend(self, parameters):
        s_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
        result = True
        stage_name = ''
        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Start to Extend VM life cycle for \"%s\"' % parameters['owner'])

        try:
            ##################################################
            # Stage : Initialize
            stage_name = 'Initialize'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            vm_manager = VmManager(parameters['serviceid'], self.operation)
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Extend VM
            stage_name = 'Extend VM'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            vm_manager.stage = stage_name
            if not vm_manager.vm_extend(parameters['hostname'], parameters['expiredate'], parameters['owner']):
                raise Exception(vm_manager.driver.errmsg, {})

            mail = Mail()
            mail.mail_extend(parameters['hostname'], parameters['owner'], parameters['expiredate'], True, '',
                             vm_manager.driver.vminfo)
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')
        except Exception as e:
            e_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
            result = False

            # Error log
            self.logger.exception(parameters['serviceid'], self.operation, stage_name, e.args[0])

            # Mail
            mail = Mail()
            mail.mail_extend(parameters['hostname'], parameters['owner'], parameters['expiredate'], False, e.args[0],
                             None)

            # Slack
            slack_sender = SlackSender()
            slack_sender.send_message(e.args[0], parameters['owner'], s_dt, e_dt,
                                      parameters['serviceid'], self.operation, stage_name)
        finally:
            pass

        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Finish to Extend VM life cycle for \"%s\"' % parameters['owner'])

        return result

    ####################################################################################################
    def vm_transfer(self, parameters):
        s_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
        result = True
        stage_name = ''
        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Start to Transfer VM for \"%s\"' % parameters['owner'])

        try:
            ##################################################
            # Stage : Initialize
            stage_name = 'Initialize'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            vm_manager = VmManager(parameters['serviceid'], self.operation)
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Transfer VM
            stage_name = 'Transfer VM'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            vm_manager.stage = stage_name
            trans_result = vm_manager.vm_transfer(parameters['hostname'], parameters['owner'],
                                                  parameters['toowner'], parameters['tomanager'],
                                                  parameters['totenant'])
            # Mail reuslt
            mail = Mail()
            mail.mail_transfer_vm(parameters['hostname'], parameters['owner'], parameters['toowner'],
                                  trans_result, vm_manager.driver.errmsg,
                                  vm_manager.driver.vminfo, vm_manager.snapshots)

            if not trans_result:
                raise Exception(vm_manager.driver.errmsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

        except Exception as e:
            e_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
            result = False

            # Error log
            self.logger.exception(parameters['serviceid'], self.operation, stage_name, e.args[0])

            # Slack
            slack_sender = SlackSender()
            slack_sender.send_message(e.args[0], parameters['owner'], s_dt, e_dt,
                                      parameters['serviceid'], self.operation, stage_name)
        finally:
            pass

        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Finish to Transfer VM for \"%s\"' % parameters['owner'])

        return result

    ####################################################################################################
    def vm_start(self, parameters):
        s_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
        result = True
        stage_name = 'Initialize'
        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Start to Start VM for \"%s\"' % parameters['owner'])

        try:
            ##################################################
            # Stage : Initialize
            stage_name = 'Initialize'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            vm_manager = VmManager(parameters['serviceid'], self.operation)
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Start VM
            stage_name = 'Start VM'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            vm_manager.stage = stage_name
            stt_result = vm_manager.vm_startup(parameters['hostname'], parameters['owner'])

            # Mail reuslt
            mail = Mail()
            mail.mail_startup(parameters['hostname'], parameters['owner'], stt_result, vm_manager.driver.errmsg)

            if not stt_result:
                raise Exception(vm_manager.driver.errmsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

        except Exception as e:
            e_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
            result = False

            # Error log
            self.logger.exception(parameters['serviceid'], self.operation, stage_name, e.args[0])

            # Slack
            slack_sender = SlackSender()
            slack_sender.send_message(e.args[0], parameters['owner'], s_dt, e_dt,
                                      parameters['serviceid'], self.operation, stage_name)
        finally:
            pass

        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Finish to Start VM for \"%s\"' % parameters['owner'])

        return result

    ####################################################################################################
    def template_retire(self, parameters):
        s_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
        result = True
        stage_name = ''
        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Start to retire template %s for \"%s\"' % (parameters['name'], parameters['owner']))

        try:
            ##################################################
            # Stage : Initialize
            stage_name = 'Initialize'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            template = Template(parameters['serviceid'], self.operation)
            template.stage = stage_name
            if not template.load_driver():
                raise Exception(template.errmsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Retire
            stage_name = 'Retire VM Template'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            template.setup_retire_template_parameter(parameters)
            template.stage = stage_name
            if not template.retire_template():
                raise Exception(template.errmsg, {})
            # Mail to owner
            mail = Mail()
            mail.mail_template_retire(parameters['owner'], parameters['name'], True, '')
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

        except Exception as e:
            e_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
            result = False

            # Error log
            self.logger.exception(parameters['serviceid'], self.operation, stage_name, e.args[0])

            # Mail
            failmail = Mail()
            failmail.mail_template_retire(parameters['owner'], parameters['name'], False, e.args[0])

            # Slack
            slack_sender = SlackSender()
            slack_sender.send_message(e.args[0], parameters['owner'], s_dt, e_dt,
                                      parameters['serviceid'], self.operation, stage_name)
        finally:
            pass

        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Finish to retire template %s for \"%s\"' % (parameters['name'], parameters['owner']))

        return result

    ####################################################################################################
    def template_create(self, parameters):
        s_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
        result = True
        stage_name = ''
        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Start to create template for \"%s\"' % parameters['owner'])

        service_producer = None
        vm_manager = None

        try:
            ##################################################
            # Stage : Initialize
            stage_name = 'Initialize'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            vm_manager = VmManager(parameters['serviceid'], self.operation)
            service_producer = ServiceProducer()
            template = Template(parameters['serviceid'], self.operation)
            template.stage = stage_name
            if not template.load_driver():
                raise Exception(template.errmsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Pre-Check
            stage_name = 'Pre-Check'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            template.setup_clone_template_parameter(parameters)
            template.stage = stage_name
            if not template.precheck_clone_template():
                raise Exception(template.errmsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Disable VM host in Zabbix
            stage_name = 'Disable VM host in Zabbix'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            service_producer.send_message(parameters['serviceid'], operation='zabbix_sync_status',
                                          message={'serviceid': parameters['serviceid'],
                                                   'owner': parameters['owner'],
                                                   'hostname': parameters['hostname'],
                                                   'action': 'shutdown'})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Clone VM Template
            stage_name = 'Clone VM Template'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            template.stage = stage_name
            if template.clone_template():
                # Mail to owner
                mail = Mail()
                mail.mail_clonetemplate(template.host, template.owner, True, '',
                                        template.tmpinfo, template.vminfo)
            else:
                raise Exception(template.errmsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Enable VM host in Zabbix
            stage_name = 'Enable VM host in Zabbix'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            service_producer.send_message(parameters['serviceid'], operation='zabbix_sync_status',
                                          message={'serviceid': parameters['serviceid'],
                                                   'owner': parameters['owner'],
                                                   'hostname': parameters['hostname'],
                                                   'action': 'running'})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Verification
            stage_name = 'Verification'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            template.stage = stage_name
            if not template.verify_clone_template():
                raise Exception(template.errmsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

        except Exception as e:
            e_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
            result = False

            if ('Initialize' != stage_name and 'Pre-Check' != stage_name) and vm_manager:
                vm_manager.stage = 'Exception handling'
                vm_manager.vm_setup_status(parameters['hostname'], parameters['owner'], 'running')

            # Error log
            self.logger.exception(parameters['serviceid'], self.operation, stage_name, e.args[0])

            # Mail
            failmail = Mail()
            failmail.mail_clonetemplate(parameters['hostname'], parameters['owner'], False, e.args[0],
                                        None, None)

            # Slack
            slack_sender = SlackSender()
            slack_sender.send_message(e.args[0], parameters['owner'], s_dt, e_dt,
                                      parameters['serviceid'], self.operation, stage_name)
        finally:
            service_producer.send_message(parameters['serviceid'], operation='zabbix_sync_status',
                                          message={'serviceid': parameters['serviceid'],
                                                   'owner': parameters['owner'],
                                                   'hostname': parameters['hostname'],
                                                   'action': 'audit'})

        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Finish to create template for \"%s\"' % parameters['owner'])

        return result

    ####################################################################################################
    def snapshot_create(self, parameters):
        s_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
        result = True
        stage_name = ''
        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Start to Create VM Snapshot for \"%s\"' % parameters['owner'])

        service_producer = None
        vm_manager = None
        chef_wrapper = None

        try:
            ##################################################
            # Stage : Initialize
            stage_name = 'Initialize'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            vm_manager = VmManager(parameters['serviceid'], self.operation)
            service_producer = ServiceProducer()
            chef_wrapper = ChefWrapper(sid=parameters['serviceid'], op=self.operation, stage=stage_name)
            snapshot = Snapshot(parameters['serviceid'], self.operation)
            snapshot.stage = stage_name
            if not snapshot.load_driver():
                raise Exception(snapshot.errmsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Pre-Check
            stage_name = 'Pre-Check'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            snapshot.stage = stage_name
            snapshot.setup_create_snapshot_parameter(parameters)
            if not snapshot.precheck_create_snapshot():
                raise Exception(snapshot.errmsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Disable VM host in Zabbix
            stage_name = 'Disable VM host in Zabbix'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            service_producer.send_message(parameters['serviceid'], operation='zabbix_sync_status',
                                          message={'serviceid': parameters['serviceid'],
                                                   'owner': parameters['owner'],
                                                   'hostname': parameters['hostname'],
                                                   'action': 'shutdown'})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Create Chef Node Snapshot
            stage_name = 'Create Chef Node Snapshot'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            rc = chef_wrapper.load(parameters['hostname'], "", "", "", "")
            if rc != 0:
                raise Exception(chef_wrapper.mail_errmsg, {})
            rc = chef_wrapper.snapshot()
            if rc != 0:
                raise Exception(chef_wrapper.mail_errmsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Create snapshot
            stage_name = 'Create VM Snapshot'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            snapshot.stage = stage_name
            if snapshot.create_snapshot():
                mail = Mail()
                mail.mail_createsnapshot(parameters['hostname'], parameters['owner'], True, '',
                                         snapshot.vminfo, snapshot.snapshot)
            else:
                raise Exception(snapshot.errmsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Enable VM host in Zabbix
            stage_name = 'Enable VM host in Zabbix'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            service_producer.send_message(parameters['serviceid'], operation='zabbix_sync_status',
                                          message={'serviceid': parameters['serviceid'],
                                                   'owner': parameters['owner'],
                                                   'hostname': parameters['hostname'],
                                                   'action': 'running'})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Verification
            stage_name = 'Verification'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            snapshot.stage = stage_name
            verify_result = snapshot.verify_snapshot()
            if not verify_result:
                raise Exception(snapshot.errmsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

        except Exception as e:
            e_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
            result = False

            if ('Initialize' != stage_name and 'Pre-Check' != stage_name) and vm_manager:
                vm_manager.stage = 'Exception handling'
                vm_manager.vm_setup_status(parameters['hostname'], parameters['owner'], 'running')

            # Error log
            self.logger.exception(parameters['serviceid'], self.operation, stage_name, e.args[0])

            # Mail
            mail = Mail()
            mail.mail_createsnapshot(parameters['hostname'], parameters['owner'], False, e.args[0],
                                     None, None)

            # Slack
            slack_sender = SlackSender()
            slack_sender.send_message(e.args[0], parameters['owner'], s_dt, e_dt,
                                      parameters['serviceid'], self.operation, stage_name)
        finally:
            service_producer.send_message(parameters['serviceid'], operation='zabbix_sync_status',
                                          message={'serviceid': parameters['serviceid'],
                                                   'owner': parameters['owner'],
                                                   'hostname': parameters['hostname'],
                                                   'action': 'audit'})
            if chef_wrapper:
                chef_wrapper.unload()

        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Finish to Create VM Snapshot for \"%s\"' % parameters['owner'])
        return result

    ####################################################################################################
    def snapshot_retire(self, parameters):
        s_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
        result = True
        stage_name = ''
        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Start to Retire VM Snapshot for \"%s\"' % parameters['owner'])

        try:
            ##################################################
            # Stage : Initialize
            stage_name = 'Initialize'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            snapshot = Snapshot(parameters['serviceid'], self.operation)
            snapshot.stage_name = stage_name
            if not snapshot.load_driver():
                raise Exception(snapshot.errmsg, {})

            # Setup retire parameter
            snapshot.setup_retire_snapshot_parameter(parameters)
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Retire VM Snapshot
            stage_name = 'Retire VM Snapshot'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            snapshot.stage_name = stage_name
            if snapshot.retire_snapshot():
                mail = Mail()
                mail.mail_retiresnapshot(parameters['owner'], parameters['name'], True, '')
            else:
                raise Exception(snapshot.errmsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

        except Exception as e:
            e_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
            result = False

            # Error log
            self.logger.exception(parameters['serviceid'], self.operation, stage_name, e.args[0])

            # Mail
            mail = Mail()
            mail.mail_retiresnapshot(parameters['owner'], parameters['name'], False, e.args[0])

            # Slack
            slack_sender = SlackSender()
            slack_sender.send_message(e.args[0], parameters['owner'], s_dt, e_dt,
                                      parameters['serviceid'], self.operation, stage_name)
        finally:
            pass

        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Finish to Retire VM Snapshot for \"%s\"' % parameters['owner'])
        return result

    ####################################################################################################
    def snapshot_refresh(self, parameters):
        s_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
        result = True
        stage_name = ''
        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Start to Refresh VM Snapshot for \"%s\"' % parameters['owner'])

        service_producer = None
        vm_manager = None
        chef_wrapper = None

        try:
            ##################################################
            # Stage : Initialize
            stage_name = 'Initialize'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            vm_manager = VmManager(parameters['serviceid'], self.operation)
            service_producer = ServiceProducer()
            chef_wrapper = ChefWrapper(sid=parameters['serviceid'], op=self.operation, stage=stage_name)
            snapshot = Snapshot(parameters['serviceid'], self.operation)
            snapshot.stage = stage_name
            if not snapshot.load_driver():
                raise Exception(snapshot.errmsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Pre-Check
            stage_name = 'Pre-Check'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            snapshot.stage = stage_name
            snapshot.setup_refresh_snapshot_parameter(parameters)
            if not snapshot.precheck_refresh_snapshot():
                raise Exception(snapshot.errmsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Disable the VM host in Zabbix
            stage_name = 'Disable old VM host in Zabbix'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            service_producer.send_message(parameters['serviceid'], operation='zabbix_sync_status',
                                          message={'serviceid': parameters['serviceid'],
                                                   'owner': parameters['owner'],
                                                   'hostname': parameters['hostname'],
                                                   'action': 'shutdown'})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Delete Node and Client from Chef
            stage_name = 'Delete Node and Client from Chef'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            cmd_1_proc = subprocess.Popen(
                '/usr/bin/knife node bulk delete \"^%s.*\" -y >/dev/null 2>&1' % parameters['hostname'],
                stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
            cmd_2_proc = subprocess.Popen(
                '/usr/bin/knife client bulk delete \"^%s.*\" -y >/dev/null 2>&1' % parameters['hostname'],
                stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)

            if 0 != cmd_1_proc.returncode or 0 != cmd_2_proc.returncode:
                self.logger.error(
                    parameters['serviceid'], self.operation, stage_name,
                    'Failed(n:%d,c:%d) to delete node & client information of \"%s\" from Chef Server !' %
                    (cmd_1_proc.returncode, cmd_2_proc.returncode, parameters['hostname']))

            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Refresh
            stage_name = 'Refresh VM with Snapshot'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            snapshot.stage = stage_name
            if not snapshot.refresh_snapshot():
                raise Exception(snapshot.errmsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Retired old VM host in Zabbix
            stage_name = 'Retired old VM host in Zabbix'
            service_producer.send_message(parameters['serviceid'], operation='zabbix_sync_status',
                                          message={'serviceid': parameters['serviceid'],
                                                   'owner': parameters['owner'],
                                                   'hostname': parameters['hostname'],
                                                   'action': 'retired'})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Refresh Chef Node Snapshot
            stage_name = 'Refresh Chef Node Snapshot'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            rc = chef_wrapper.load(parameters['hostname'], "", "", "", "")
            if rc != 0:
                raise Exception(chef_wrapper.mail_errmsg, {})

            rc = chef_wrapper.refresh()
            if rc != 0:
                raise Exception(chef_wrapper.mail_errmsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Verification
            stage_name = 'Verification'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            snapshot.stage = stage_name
            if not snapshot.verify_snapshot():
                raise Exception(snapshot.errmsg, {})

            mail = Mail()
            mail.mail_refershsnapshot(parameters['hostname'], parameters['owner'], parameters['name'], True, '')

            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

        except Exception as e:
            e_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
            result = False

            if ('Initialize' != stage_name and 'Pre-Check' != stage_name) and vm_manager:
                vm_manager.stage = 'Exception handling'
                vm_manager.vm_setup_status(parameters['hostname'], parameters['owner'], 'running')

            # Error
            self.logger.exception(parameters['serviceid'], self.operation, stage_name, e.args[0])

            # Mail
            mail = Mail()
            mail.mail_refershsnapshot(parameters['hostname'], parameters['owner'], parameters['name'], False, e.args[0])

            # Slack
            slack_sender = SlackSender()
            slack_sender.send_message(e.args[0], parameters['owner'], s_dt, e_dt,
                                      parameters['serviceid'], self.operation, stage_name)
        finally:
            service_producer.send_message(parameters['serviceid'], operation='zabbix_sync_status',
                                          message={'serviceid': parameters['serviceid'],
                                                   'owner': parameters['owner'],
                                                   'hostname': parameters['hostname'],
                                                   'action': 'audit'})
            if chef_wrapper:
                chef_wrapper.unload()

        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Finish to Refresh VM Snapshot for \"%s\"' % parameters['owner'])
        return result

    ####################################################################################################
    def __chef_nd_bootstrap(self, parameters):
        s_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
        result = True
        stage_name = ''
        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Start to Chef Node Bootstrap \"%s\"' % parameters['hostname'])

        chef_wrapper = None

        try:
            ##################################################
            # Stage : Initialize
            stage_name = 'Initialize'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            chef_wrapper = ChefWrapper(sid=parameters['serviceid'], op=self.operation, stage=stage_name)
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

            ##################################################
            # Stage : Chef Node Bootstrap
            stage_name = 'Chef Node Bootstrap'
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'START')
            rc = chef_wrapper.load(parameters['hostname'], "", "", "", "")
            if rc != 0:
                raise Exception(chef_wrapper.mail_errmsg, {})

            rc = chef_wrapper.bootstrap(os.environ['OARDC_PT_ENVIRONMENT'], ["role[InfraVM]"], "")
            if rc != 0:
                raise Exception(chef_wrapper.mail_errmsg, {})
            self.logger.info(parameters['serviceid'], self.operation, stage_name, 'END')

        except Exception as e:
            e_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
            result = False
            self.logger.exception(parameters['serviceid'], self.operation, stage_name, e.args[0])
            # Mail
            # Slack
            slack_sender = SlackSender()
            slack_sender.send_message(e.args[0], parameters['owner'], s_dt, e_dt,
                                      parameters['serviceid'], self.operation, stage_name)

        if chef_wrapper:
            chef_wrapper.unload()

        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Finish to add monitor into \"%s\"' % parameters['hostname'])
        return result

    ####################################################################################################
    def __chef_sw_install(self, parameters):
        s_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
        result = True
        stage_name = 'Chef SW Install'
        chef_wrapper = None
        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Start to add monitor into \"%s\"' % parameters['hostname'])
        try:
            ##################################################
            chef_wrapper = ChefWrapper(sid=parameters['serviceid'], op=self.operation, stage=stage_name)
            rc = chef_wrapper.load(parameters['hostname'], "", "", "", "", False, False)
            if rc != 0:
                raise Exception(chef_wrapper.mail_errmsg, {})

            rc = chef_wrapper.swinstall(parameters['package'], parameters['version'], "")
            if rc != 0:
                raise Exception(chef_wrapper.mail_errmsg, {})

        except Exception as e:
            e_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
            result = False
            self.logger.exception(parameters['serviceid'], self.operation, stage_name, e.args[0])
            # Mail
            # Slack
            slack_sender = SlackSender()
            slack_sender.send_message(e.args[0], parameters['owner'], s_dt, e_dt,
                                      parameters['serviceid'], self.operation, stage_name)

        if chef_wrapper:
            chef_wrapper.unload()

        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Finish to add monitor into \"%s\"' % parameters['hostname'])
        return result

    ####################################################################################################
    def __chef_add_monitor(self, parameters):
        s_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
        result = True
        stage_name = 'Chef Add Monitor'
        chef_wrapper = None
        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Start to add monitor into \"%s\"' % parameters['hostname'])
        try:
            ##################################################
            chef_wrapper = ChefWrapper(sid=parameters['serviceid'], op=self.operation, stage=stage_name)
            rc = chef_wrapper.load(parameters['hostname'], "", "", "", "", False, False)
            if rc != 0:
                raise Exception(chef_wrapper.mail_errmsg, {})

            rc = chef_wrapper.addmonitor(parameters['environment'], parameters['runlist'], "")
            if rc != 0:
                raise Exception(chef_wrapper.mail_errmsg, {})

        except Exception as e:
            e_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
            result = False
            self.logger.exception(parameters['serviceid'], self.operation, stage_name, e.args[0])
            # Mail
            # Slack
            slack_sender = SlackSender()
            slack_sender.send_message(e.args[0], parameters['owner'], s_dt, e_dt,
                                      parameters['serviceid'], self.operation, stage_name)

        if chef_wrapper:
            chef_wrapper.unload()

        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Finish to add monitor into \"%s\"' % parameters['hostname'])
        return result

    ####################################################################################################
    def __zabbix_sync_status(self, parameters):
        s_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
        result = True
        stage_name = 'Zabbix Sync Status'
        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Start to sync status of \"%s\" by action %s ' %
                         (parameters['hostname'], parameters['action']))
        try:
            ##################################################
            zabbix_wrapper = ZabbixWrapper(sid=parameters['serviceid'], op=self.operation, stage=stage_name)
            rc = zabbix_wrapper.sync_status(parameters['hostname'], parameters['action'])
            if 0 != rc:
                raise Exception('Failed(%d) to sync status of \"%s\" by action %s ' %
                                (rc, parameters['hostname'], parameters['action']), {})
        except Exception as e:
            e_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
            result = False
            self.logger.exception(parameters['serviceid'], self.operation, stage_name, e.args[0])
            # Mail
            # Slack
            slack_sender = SlackSender()
            slack_sender.send_message(e.args[0], parameters['owner'], s_dt, e_dt,
                                      parameters['serviceid'], self.operation, stage_name)

        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Finish to sync status of \"%s\" by action %s ' %
                         (parameters['hostname'], parameters['action']))
        return result

    ####################################################################################################
    def __zabbix_check_monitor(self, parameters):
        s_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
        result = True
        stage_name = 'Zabbix Check Monitor'
        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Start to check monitor in \"%d\" day(s)' % parameters['days'])

        try:
            ##################################################
            zabbix_wrapper = ZabbixWrapper(sid=parameters['serviceid'], op=parameters['op'], stage=stage_name)
            rc = zabbix_wrapper.check_monitor(parameters['days'])
            if 0 != rc:
                raise Exception('Failed to check monitor in \"%d\" day(s)' % parameters['days'], {})
        except Exception as e:
            e_dt = datetime.datetime.now().strftime("%Y/%m/%dT%H:%M:%S.%f")
            result = False
            self.logger.exception(parameters['serviceid'], self.operation, stage_name, e.args[0])
            # Mail
            # Slack
            slack_sender = SlackSender()
            slack_sender.send_message(e.args[0], parameters['owner'], s_dt, e_dt,
                                      parameters['serviceid'], self.operation, stage_name)

        self.logger.info(parameters['serviceid'], self.operation, stage_name,
                         'Finish to check monitor in \"%d\" day(s)' % parameters['days'])
        return result
        
        
    ##################################################################################################
    
    def __dockerservice_create(self,parameters):
        docker_manager = DockerManager()
        docker_info = [parameters['servicetype'],parameters['serviceversion'],parameters['owner'],parameters['manager'],parameters['tenant'],parameters['project']]
        ret = docker_manager.docker_provisioning(docker_info)
        mail = Mail()
        mail.mail_dockerservice(parameters['owner'].lower(), parameters['manager'].lower(), ret)
            
        if not ret[0]:
            logging.error(ret[1])
            sys.exit(1)
    
    
    
    def __dockerservice_delete(self,parameters):
        docker_manager = DockerManager()
        docker_info = [parameters['serviceid'], parameters['owner']]
        ret = docker_manager.docker_retire_service(docker_info)
        mail = Mail()
        mail.mail_retire_dockerservice(parameters['serviceid'], parameters['owner'].lower(), ret)
            
        if not ret[0]:
            logging.error(ret[1])
            sys.exit(3)
# -EOF-
