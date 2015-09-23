# -*- coding: utf-8 -*-
'''
Aozan main file.
Created on 25 oct. 2011

@author: Laurent Jourdren
'''

import sys, os, traceback
from optparse import OptionParser
import common, hiseq_run, sync_run, demux_run, qc_run
import estimate_space_needed
from java.util import Locale
# import first_base_report
import detection_new_run, detection_end_run
from java.util import LinkedHashMap
from fr.ens.transcriptome.aozan import Globals
from fr.ens.transcriptome.aozan import Common
from fr.ens.transcriptome.aozan import AozanException

from fr.ens.transcriptome.aozan.Settings import HISEQ_STEP_KEY
from fr.ens.transcriptome.aozan.Settings import FIRST_BASE_REPORT_STEP_KEY
from fr.ens.transcriptome.aozan.Settings import DEMUX_STEP_KEY
from fr.ens.transcriptome.aozan.Settings import QC_STEP_KEY
from fr.ens.transcriptome.aozan.Settings import DEMUX_USE_HISEQ_OUTPUT_KEY
from fr.ens.transcriptome.aozan.Settings import AOZAN_LOG_LEVEL_KEY
from fr.ens.transcriptome.aozan.Settings import AOZAN_LOG_PATH_KEY
from fr.ens.transcriptome.aozan.Settings import AOZAN_ENABLE_KEY
from fr.ens.transcriptome.aozan.Settings import SYNC_CONTINUOUS_SYNC_KEY
from fr.ens.transcriptome.aozan.Settings import AOZAN_VAR_PATH_KEY
from fr.ens.transcriptome.aozan.Settings import LOCK_FILE_KEY
from fr.ens.transcriptome.aozan.Settings import BCL_DATA_PATH_KEY
from fr.ens.transcriptome.aozan.Settings import FASTQ_DATA_PATH_KEY
from fr.ens.transcriptome.aozan.Settings import REPORTS_DATA_PATH_KEY

def create_lock_file(lock_file_path):
    """Create the lock file.

    Arguments:
        lock_file_path path of the lock file
    """

    f = open(lock_file_path, 'w')
    f.write(str(Common.getCurrentPid()))
    f.close()

def lock_file_exists(lock_file_path):
    """Check if the lock file exists for the execute pid

    Arguments:
        lock_file_path path of the lock file
    """

    if not os.path.exists(lock_file_path):
        return False

    if os.path.exists('/proc/%d' % (load_pid_in_lock_file(lock_file_path))):
        return True

    # PID from a dead processus, lock to delete
    delete_lock_file(lock_file_path)
    return False

def delete_lock_file(lock_file_path):
    """Delete the lock file.

    Arguments:
        lock_file_path path of the lock file
    """

    os.unlink(lock_file_path)


def load_pid_in_lock_file(lock_file_path):
    """Load the pid in the lock file.

    Arguments:
        lock_file_path path of the pid file
    """

    f = open(lock_file_path, 'r')
    pid = int(f.readline().strip())
    f.close()
    return pid


def welcome(conf):
    """Welcome message.

    Arguments:
        conf: configuration object    
    """
    global something_to_do
    if something_to_do == False:
        common.log('INFO', 'Start ' + Globals.WELCOME_MSG, conf)
        something_to_do = True
        
        # Add list step selected
        common.extract_steps_to_launch(True, conf)

something_to_do = False
is_error_message = True
is_not_error_message = False


def lock_step(lock_file_path, step_name, conf):
    """Lock a step.

    Arguments:
        lock_file_path: lock_file path
        step_name: step name
        conf: configuration object
    """
    
    # Check if parent directory of the lock file exists
    if not os.path.isdir(os.path.dirname(lock_file_path)):
        common.log('SEVERE', 'Parent directory of lock file does not exists. The lock file for ' + step_name + \
                   ' step has not been created: ' + lock_file_path, conf)
        return True

    # Return False if the run is currently processed
    if os.path.isfile(lock_file_path):
        return False

    # Create the lock file
    try:
        open(lock_file_path, 'w').close()
    except:
        common.log('SEVERE', 'The lock file cannot not been created (' + sys.exc_info()[0] + ') for ' + step_name + \
                   ': ' + lock_file_path, conf)
        return True

    return True 


def unlock_step(lock_file_path):
    """Unlock a step.

    Arguments:
        lock_file_path: lock_file path
    """

    # Remove lock file if exists
    if os.path.isfile(lock_file_path):
        os.remove(lock_file_path)

    return True


def lock_sync_step(conf, run_id):
    """Lock the sync step step.

    Arguments:
        conf: configuration object
        run_id: run_id
    """

    return lock_step(conf[BCL_DATA_PATH_KEY] + '/' + run_id + '.lock', 'sync', conf)

def lock_demux_step(conf, run_id):
    """Lock the demux step step.

    Arguments:
        conf: configuration object
        run_id: run_id
    """

    return lock_step(conf[FASTQ_DATA_PATH_KEY] + '/' + run_id + '.lock', 'demux', conf)

def lock_qc_step(conf, run_id):
    """Lock the qc step step.

    Arguments:
        conf: configuration object
        run_id: run_id
    """

    return lock_step(conf[REPORTS_DATA_PATH_KEY] + '/qc_' + run_id + '.lock', 'qc', conf)

def lock_partial_sync_step(conf, run_id):
    """Lock the partial sync step.

    Arguments:
        conf: configuration object
        run_id: run_id
    """

    return lock_sync_step(conf, run_id)

def unlock_sync_step(conf, run_id):
    """Unlock the sync step.

    Arguments:
        conf: configuration object
        run_id: run_id
    """

    return unlock_step(conf[BCL_DATA_PATH_KEY] + '/' + run_id + '.lock')

def unlock_demux_step(conf, run_id):
    """Unlock the demux step.

    Arguments:
        conf: configuration object
        run_id: run_id
    """

    return unlock_step(conf[FASTQ_DATA_PATH_KEY] + '/' + run_id + '.lock')

def unlock_qc_step(conf, run_id):
    """Unlock the qc step.

    Arguments:
        conf: configuration object
        run_id: run_id
    """

    return unlock_step(conf[REPORTS_DATA_PATH_KEY] + '/qc_' + run_id + '.lock')

def unlock_partial_sync_step(conf, run_id):
    """Unlock the partial sync step.

    Arguments:
        conf: configuration object
        run_id: run_id
    """

    return unlock_sync_step(conf, run_id)


def discover_new_run_TOREMOVE(conf):
    """Discover new runs.

    Arguments:
        conf: configuration object  
    """

    #
    # Discover new run
    #
# 
#     first_base_report_sent = first_base_report.load_processed_run_ids(conf)
# 
#     if common.is_conf_value_equals_true(FIRST_BASE_REPORT_STEP_KEY, conf):
#         for run_id in (first_base_report.get_available_run_ids(conf) - first_base_report_sent):
#             welcome(conf)
#             common.log('INFO', 'First base report ' + run_id, conf)
#             first_base_report.send_report(run_id, conf)
#             first_base_report.add_run_id_to_processed_run_ids(run_id, conf)
#             first_base_report_sent.add(run_id)
# 
#             # Verify space needed during the first base report
#             estimate_space_needed.estimate(run_id, conf)
# 
#     #
#     # Discover hiseq run done
#     #
# 
#     hiseq_run_ids_done = hiseq_run.load_processed_run_ids(conf)
# 
#     if common.is_conf_value_equals_true(HISEQ_STEP_KEY, conf):
#         for run_id in (hiseq_run.get_available_run_ids(conf) - hiseq_run_ids_done):
#             welcome(conf)
#             common.log('INFO', 'Discover ' + run_id, conf)
#             
#             if hiseq_run.create_run_summary_reports(run_id, conf):
#                 hiseq_run.send_mail_if_recent_run(run_id, 12 * 3600, conf)
#                 hiseq_run.add_run_id_to_processed_run_ids(run_id, conf)
#                 hiseq_run_ids_done.add(run_id)
#             else:
#                 raise Exception('Create run summary report for new discovery run ' + run_id)
# 
#     return hiseq_run_ids_done

def launch_steps(conf):
    """Launch steps.

    Arguments:
        conf: configuration object  
    """
    
    # Discover new runs
    hiseq_run_ids_done = detection_new_run.discover_new_run(conf)
    #print 'DEBUG launchStep run done '+ str(hiseq_run_ids_done)
    
    # Load run do not process
    hiseq_run_ids_do_not_process = hiseq_run.load_deny_run_ids(conf)
    #print 'DEBUG launchStep run deny '+ str(hiseq_run_ids_do_not_process)
    #
    # Sync hiseq and storage
    #

    sync_run_ids_done = sync_run.load_processed_run_ids(conf)
    #print 'DEBUG launchStep sync done '+ str(sync_run_ids_done)

    # Get the list of run available on HiSeq output
    if sync_run.is_sync_step_enable(conf):
        
        try:      
            for run_id in (hiseq_run_ids_done - sync_run_ids_done - hiseq_run_ids_do_not_process):
                
                #print 'DEBUG sync launch on '+ str(run_id)
                
                if lock_sync_step(conf, run_id):
                    welcome(conf)
                    common.log('INFO', 'Synchronize ' + run_id, conf)
                    if sync_run.sync(run_id, conf):
                        sync_run.add_run_id_to_processed_run_ids(run_id, conf)
                        sync_run_ids_done.add(run_id)
                        unlock_sync_step(conf, run_id)
                    else:
                        unlock_sync_step(conf, run_id)
                        return
                else:
                    common.log('INFO', 'Synchronize ' + run_id + ' is locked.', conf)

        except:
            exception_msg = str(sys.exc_info()[0]) + ' (' + str(sys.exc_info()[1]) + ')'
            traceback_msg = traceback.format_exc(sys.exc_info()[2])

            sync_run.error("Fail synchronization for run " + run_id + ", catch exception " + exception_msg,
                           "Fail synchronization for run " + run_id + ", catch exception " + exception_msg + "\n Stacktrace : \n" + traceback_msg, conf)


    # Check if new run appears while sync step
    if  sync_run.is_sync_step_enable(conf) and len(detection_new_run.discover_new_run(conf) - sync_run_ids_done - hiseq_run.load_deny_run_ids(conf)) > 0:
        #print 'DEBUG New run discovery relaunch steps at the start'
        launch_steps(conf)
        return

    #
    # Demultiplexing
    #

    if common.is_conf_value_equals_true(DEMUX_USE_HISEQ_OUTPUT_KEY, conf):
        sync_run_ids_done = hiseq_run_ids_done
        
    demux_run_ids_done = demux_run.load_processed_run_ids(conf)
    #print 'DEBUG launchStep demux done '+ str(demux_run_ids_done)
    #print 'DEBUG runs to demux '+ str(sync_run_ids_done - demux_run_ids_done)
    
    if common.is_conf_value_equals_true(DEMUX_STEP_KEY, conf):
        try:
            for run_id in (sync_run_ids_done - demux_run_ids_done):
                
                #print 'DEBUG demux launch on ' + str(run_id)
                
                if lock_demux_step(conf, run_id):
                    welcome(conf)
                    common.log('INFO', 'Demux ' + run_id, conf)
                    if demux_run.demux(run_id, conf):
                        demux_run.add_run_id_to_processed_run_ids(run_id, conf)
                        demux_run_ids_done.add(run_id)
                        unlock_demux_step(conf, run_id)
                    else:
                        unlock_demux_step(conf, run_id)
                        return
                else:
                    common.log('INFO', 'Demux ' + run_id + ' is locked.', conf)

        except:
            exception_msg = str(sys.exc_info()[0]) + ' (' + str(sys.exc_info()[1]) + ')'
            traceback_msg = traceback.format_exc(sys.exc_info()[2])

            demux_run.error("Fail demultiplexing for run " + run_id + ", catch exception " + exception_msg,
                            "Fail demultiplexing for run " + run_id + ", catch exception " + exception_msg + "\n Stacktrace : \n" + traceback_msg, conf)


    # Check if new run appears while demux step
    if common.is_conf_value_equals_true(DEMUX_STEP_KEY, conf) and len(detection_new_run.discover_new_run(conf) - sync_run_ids_done - hiseq_run.load_deny_run_ids(conf)) > 0:
        #print 'DEBUG New run discovery relaunch steps at the start'
        launch_steps(conf)
        return

    #
    # Quality control
    #

    qc_run_ids_done = qc_run.load_processed_run_ids(conf)
    #print 'DEBUG launchStep qc done '+ str(qc_run_ids_done)
    #print 'DEBUG runs to qc '+ str(demux_run_ids_done - qc_run_ids_done)
    
    if common.is_conf_value_equals_true(QC_STEP_KEY, conf):
        
        try:
            for run_id in (demux_run_ids_done - qc_run_ids_done):
                #print 'DEBUG: check type on run id ', type(run_id), '|'+run_id+'|', len(run_id)
                #print 'DEBUG qc launch on ' + str(run_id)
                if lock_qc_step(conf, run_id):
                    welcome(conf)
                    common.log('INFO', 'Quality control ' + run_id, conf)
                    if qc_run.qc(run_id, conf):
                        qc_run.add_run_id_to_processed_run_ids(run_id, conf)
                        qc_run_ids_done.add(run_id)
                        unlock_qc_step(conf, run_id)
                    else:
                        unlock_qc_step(conf, run_id)
                        return
                else:
                    common.log('INFO', 'Quality control ' + run_id + ' is locked.', conf)

        except:
            exception_msg = str(sys.exc_info()[0]) + ' (' + str(sys.exc_info()[1]) + ')'
            traceback_msg = traceback.format_exc(sys.exc_info()[2])
            qc_run.error("Fail quality control for run " + run_id + ", catch exception " + exception_msg,
                           "Fail quality control for run " + run_id + ", catch exception " + exception_msg + "\n Stacktrace : \n" + traceback_msg, conf)


    # Check if new run appears while quality control step
    if common.is_conf_value_equals_true(QC_STEP_KEY, conf) and len(detection_new_run.discover_new_run(conf) - sync_run_ids_done - hiseq_run.load_deny_run_ids(conf)) > 0:
        #print 'DEBUG New run discovery relaunch steps at the start'
        # TODO
        launch_steps(conf)
        return

    #
    # Partial synchronization
    #

    working_run_ids = hiseq_run.get_working_run_ids(conf)

    if common.is_conf_value_equals_true(SYNC_CONTINUOUS_SYNC_KEY, conf):
        for run_id in (working_run_ids - sync_run_ids_done - hiseq_run_ids_do_not_process):
            if lock_partial_sync_step(conf, run_id):
                welcome(conf)
                common.log('INFO', 'Partial synchronization of ' + run_id, conf)
                if not sync_run.partial_sync(run_id, False, conf):
                    unlock_partial_sync_step(conf, run_id)
                    return
                unlock_partial_sync_step(conf, run_id)
            else:
                common.log('INFO', 'Partial synchronization of ' + run_id + ' is locked.', conf)

def aozan_main():
    """Aozan main method.

    Arguments:
        conf: configuration object  
    """

    # Define command line parser
    parser = OptionParser(usage='usage: ' + Globals.APP_NAME_LOWER_CASE + '.sh [options] conf_file')
    parser.add_option('-q', '--quiet', action='store_true', dest='quiet',
                default=False, help='quiet')
    parser.add_option('-v', '--version', action='store_true', dest='version', help='Aozan version')
    parser.add_option('-c', '--conf', action='store_true', dest='conf', help='Default Aozan configuration Aozan, load before configuration file.')

    # Parse command line arguments
    (options, args) = parser.parse_args()
    
    # Print Aozan current version
    if options.version:
        print Globals.WELCOME_MSG
        sys.exit(0)
    
    #  Print default configuration option
    if options.conf:
        print common.print_default_configuration()
        sys.exit(0)
        
    # If no argument print usage
    if len(args) < 1:
        parser.print_help()
        sys.exit(1)

    # Create configuration object
    conf = LinkedHashMap()

    # Set the default value in the configuration object
    common.set_default_conf(conf)

    # Use default (US) locale
    Locale.setDefault(Globals.DEFAULT_LOCALE)

    # Load Aozan conf file
    common.load_conf(conf, args[0])

    # End of Aozan if aozan is not enable
    if common.is_conf_value_defined(AOZAN_ENABLE_KEY, 'false', conf):
        sys.exit(0)

    # Init logger
    try:
        Common.initLogger(conf[AOZAN_LOG_PATH_KEY], conf[AOZAN_LOG_LEVEL_KEY])
    except AozanException, exp:
        common.exception_msg(exp, conf)
        
    # Check main path file in configuration
    if not common.check_configuration(conf, args[0]):
        common.log('SEVERE', 'Aozan can not be executed, configuration invalid or useful directories inaccessible. ', conf)
        sys.exit(1)

        
    # Check critical free space available
    hiseq_run.send_mail_if_critical_free_space_available(conf)

    lock_file_path = conf[LOCK_FILE_KEY]

    # Run only if there is no lock
    # if not os.path.exists(lock_file_path):
    if not lock_file_exists(lock_file_path):
        try:
            # Create lock file
            create_lock_file(lock_file_path)
            
            # Launch steps
            launch_steps(conf)

            # Remove lock file
            delete_lock_file(lock_file_path)

            # TODO remove *.lasterr files

            if something_to_do:
                common.log('INFO', 'End of Aozan', conf)
                    
                # Cancel logger, in case not be cancel properly 
                Common.cancelLogger()
        except:
                # Get exception info
                exception_msg = str(sys.exc_info()[0]) + ' (' + str(sys.exc_info()[1]) + ')'
                traceback_msg = traceback.format_exc(sys.exc_info()[2]).replace('\n', ' ')

                # Log the exception
                common.log('SEVERE', 'Exception: ' + exception_msg, conf)
                common.log('WARNING', traceback_msg, conf)

                # Send a mail with the exception
                common.send_msg("[Aozan] Exception: " + exception_msg, traceback_msg, is_not_error_message, conf)
                
                common.log('INFO', 'End of Aozan', conf)
                
                # Remove lock file
                delete_lock_file(lock_file_path)
               
                # Cancel logger, in case not be cancel properly 
                Common.cancelLogger()
    else:
        if not options.quiet:
            print "A lock file exists."
        if not os.path.exists('/proc/%d' % (load_pid_in_lock_file(lock_file_path))):
            common.error('[Aozan] A lock file exists', 'A lock file exist at ' + conf[LOCK_FILE_KEY] + 
                         ". Please investigate last error and then remove the lock file.", is_error_message, conf[AOZAN_VAR_PATH_KEY] + '/aozan.lasterr', conf)


# Launch Aozan main
if __name__ == "__main__":
    aozan_main()

