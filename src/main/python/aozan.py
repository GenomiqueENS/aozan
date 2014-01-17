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
import first_base_report
from java.util import LinkedHashMap
from fr.ens.transcriptome.aozan import Globals
from fr.ens.transcriptome.aozan import Common
from fr.ens.transcriptome.aozan import Settings

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
    """Create the lock file.

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

something_to_do = False
is_error_message = True
is_not_error_message = False

def discover_new_run(conf):
    """Discover new runs.

    Arguments:
        conf: configuration object  
    """

    #
    # Discover first base report
    #

    first_base_report_sent = first_base_report.load_processed_run_ids(conf)

    if conf[Settings.FIRST_BASE_REPORT_STEP_KEY].lower().strip() == 'true':
        for run_id in (first_base_report.get_available_run_ids(conf) - first_base_report_sent):
            welcome(conf)
            common.log('INFO', 'First base report ' + run_id, conf)
            first_base_report.send_report(run_id, conf)
            first_base_report.add_run_id_to_processed_run_ids(run_id, conf)
            first_base_report_sent.add(run_id)

            # Verify space needed during the first base report
            estimate_space_needed.estimate(run_id, conf)

    #
    # Discover hiseq run done
    #

    hiseq_run_ids_done = hiseq_run.load_processed_run_ids(conf)

    if conf[Settings.HISEQ_STEP_KEY].lower().strip() == 'true':
        for run_id in (hiseq_run.get_available_run_ids(conf) - hiseq_run_ids_done):
            welcome(conf)
            common.log('INFO', 'Discover ' + run_id, conf)
            hiseq_run.send_mail_if_recent_run(run_id, 12 * 3600, conf)
            hiseq_run.add_run_id_to_processed_run_ids(run_id, conf)
            hiseq_run_ids_done.add(run_id)

    return hiseq_run_ids_done

def launch_steps(conf):
    """Launch steps.

    Arguments:
        conf: configuration object  
    """

    # Discover new runs
    hiseq_run_ids_done = discover_new_run(conf)

    # Load run do not process
    hiseq_run_ids_do_not_process = hiseq_run.load_deny_run_ids(conf)

    #
    # Sync hiseq and storage
    #

    sync_run_ids_done = sync_run.load_processed_run_ids(conf)

    # Get the list of run available on HiSeq output
    if conf[Settings.SYNC_STEP_KEY].lower().strip() == 'true':
        for run_id in (hiseq_run_ids_done - sync_run_ids_done - hiseq_run_ids_do_not_process):
            welcome(conf)
            common.log('INFO', 'Synchronize ' + run_id, conf)
            if sync_run.sync(run_id, conf):
                sync_run.add_run_id_to_processed_run_ids(run_id, conf)
                sync_run_ids_done.add(run_id)
            else:
                return


    # Check if new run appears while sync step
    if len(discover_new_run(conf) - sync_run_ids_done - hiseq_run.load_deny_run_ids(conf)) > 0:
        launch_steps(conf)
        return

    #
    # Demultiplexing
    #

    if conf[Settings.DEMUX_USE_HISEQ_OUTPUT_KEY].lower().strip() == 'true':
        sync_run_ids_done = hiseq_run_ids_done
        
    demux_run_ids_done = demux_run.load_processed_run_ids(conf)
    
    if conf[Settings.DEMUX_STEP_KEY].lower().strip() == 'true':
        for run_id in (sync_run_ids_done - demux_run_ids_done):
            welcome(conf)
            common.log('INFO', 'Demux ' + run_id, conf)
            if demux_run.demux(run_id, conf):
                demux_run.add_run_id_to_processed_run_ids(run_id, conf)
                demux_run_ids_done.add(run_id)
            else:
                return

    # Check if new run appears while demux step
    if len(discover_new_run(conf) - sync_run_ids_done - hiseq_run.load_deny_run_ids(conf)) > 0:
        launch_steps(conf)
        return

    #
    # Quality control
    #

    qc_run_ids_done = qc_run.load_processed_run_ids(conf)
    
    if conf[Settings.QC_STEP_KEY].lower().strip() == 'true':
        for run_id in (demux_run_ids_done - qc_run_ids_done):
            welcome(conf)
            common.log('INFO', 'Quality control ' + run_id, conf)
            if qc_run.qc(run_id, conf):
                qc_run.add_run_id_to_processed_run_ids(run_id, conf)
                qc_run_ids_done.add(run_id)
            else:
                return

    # Check if new run appears while quality control step
    if len(discover_new_run(conf) - sync_run_ids_done - hiseq_run.load_deny_run_ids(conf)) > 0:
        launch_steps(conf)
        return

    #
    # Partial synchronization
    #

    working_run_ids = hiseq_run.get_working_run_ids(conf)

    if conf[Settings.SYNC_CONTINUOUS_SYNC_KEY].lower().strip() == 'true':
        for run_id in (working_run_ids - sync_run_ids_done - hiseq_run_ids_do_not_process):
            welcome(conf)
            common.log('INFO', 'Partial synchronization of ' + run_id, conf)
            if not sync_run.partial_sync(run_id, False, conf):
                return


def aozan_main():
    """Aozan main method.

    Arguments:
        conf: configuration object  
    """

    # Define command line parser
    parser = OptionParser(usage='usage: ' + Globals.APP_NAME_LOWER_CASE + '.sh [options] conf_file')
    parser.add_option('-q', '--quiet', action='store_true', dest='quiet',
                default=False, help='quiet')

    # Parse command line arguments
    (options, args) = parser.parse_args()

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
    if conf[Settings.AOZAN_ENABLE_KEY].lower().strip() == 'false':
        sys.exit(0)

    # Init logger
    Common.initLogger(conf[Settings.AOZAN_LOG_PATH_KEY], conf[Settings.AOZAN_LOG_LEVEL_KEY])

    # Check critical free space available
    hiseq_run.send_mail_if_critical_free_space_available(conf)

    lock_file_path = conf[Settings.LOCK_FILE_KEY]

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
        except:
                # Get exception info
                exception_msg = str(sys.exc_info()[0]) + ' (' + str(sys.exc_info()[1]) + ')'
                traceback_msg = traceback.format_exc(sys.exc_info()[2]).replace('\n', ' ')

                # Log the exception
                common.log('SEVERE', 'Exception: ' + exception_msg, conf)
                common.log('WARNING', traceback_msg, conf)

                # Send a mail with the exception
                common.send_msg("[Aozan] Exception: " + exception_msg, traceback_msg, is_not_error_message, conf)
    else:
        if not options.quiet:
            print "A lock file exists."
        if not os.path.exists('/proc/%d' % (load_pid_in_lock_file(lock_file_path))):
            common.error('[Aozan] A lock file exists', 'A lock file exist at ' + conf[Settings.LOCK_FILE_KEY] + 
                         ". Please investigate last error and then remove the lock file.", is_error_message, conf[Settings.AOZAN_VAR_PATH_KEY] + '/aozan.lasterr', conf)


# Launch Aozan main
if __name__ == "__main__":
    aozan_main()

