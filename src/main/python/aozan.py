# -*- coding: utf-8 -*-

#
#                  Aozan development code
#
# This code may be freely distributed and modified under the
# terms of the GNU General Public License version 3 or later
# and CeCILL. This should be distributed with the code. If you
# do not have a copy, see:
#
#      http://www.gnu.org/licenses/gpl-3.0-standalone.html
#      http://www.cecill.info/licences/Licence_CeCILL_V2-en.html
#
# Copyright for this code is held jointly by the Genomic platform
# of the Institut de Biologie de l'École Normale Supérieure and
# the individual authors.
#
# For more information on the Aozan project and its aims,
# or to join the Aozan Google group, visit the home page at:
#
#      http://outils.genomique.biologie.ens.fr/aozan
#
#

'''
This script is Aozan main file, it executes the steps.
'''

import sys
import os
import traceback
from optparse import OptionParser

import common
import hiseq_run
import sync_run
import demux_run
import qc_run
import recompress_run
import estimate_space_needed
import detection_new_run
import detection_end_run

from java.util import Locale
from java.util import LinkedHashMap

from fr.ens.biologie.genomique.aozan import Globals
from fr.ens.biologie.genomique.aozan import Common
from fr.ens.biologie.genomique.aozan import AozanException
from fr.ens.biologie.genomique.eoulsan.util import SystemUtils
from fr.ens.biologie.genomique.aozan.Settings import DEMUX_STEP_KEY
from fr.ens.biologie.genomique.aozan.Settings import RECOMPRESS_STEP_KEY
from fr.ens.biologie.genomique.aozan.Settings import QC_STEP_KEY
from fr.ens.biologie.genomique.aozan.Settings import DEMUX_USE_HISEQ_OUTPUT_KEY
from fr.ens.biologie.genomique.aozan.Settings import DOCKER_URI_KEY
from fr.ens.biologie.genomique.aozan.Settings import AOZAN_LOG_LEVEL_KEY
from fr.ens.biologie.genomique.aozan.Settings import AOZAN_LOG_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import AOZAN_ENABLE_KEY
from fr.ens.biologie.genomique.aozan.Settings import SYNC_CONTINUOUS_SYNC_KEY
from fr.ens.biologie.genomique.aozan.Settings import AOZAN_VAR_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import LOCK_FILE_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import FASTQ_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import REPORTS_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import DOCKER_URI_KEY
from fr.ens.biologie.genomique.aozan.util import DockerConnection


def run_id_sorted_by_priority(run_ids, priority_ids):
    """Get a sorted.

    Arguments:
        run_ids: set of run ids to sort
        priority_ids: set of run ids in priority
    """
    if len(priority_ids) == 0:
        return list(run_ids)

    return list(priority_ids & run_ids) + list(run_ids - priority_ids)


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

    if os.path.exists(lock_file_path):
        os.unlink(lock_file_path)


def load_pid_in_lock_file(lock_file_path):
    """Load the pid in the lock file.

    Arguments:
        lock_file_path path of the pid file
    """

    f = open(lock_file_path, 'r')
    line = f.readline().strip()
    f.close()

    try:
        return int(line)
    except ValueError:
        return -1


def welcome(conf):
    """Welcome message.

    Arguments:
        conf: configuration object
    """
    global something_to_do
    if not something_to_do:
        common.log('INFO', 'Starting ' + Globals.WELCOME_MSG, conf)
        something_to_do = True

        # Add list step selected
        common.extract_steps_to_launch(True, conf)


something_to_do = False


def lock_step(lock_file_path, step_name, conf):
    """Lock a step.

    Arguments:
        lock_file_path: lock_file path
        step_name: step name
        conf: configuration object
    """

    # Check if parent directory of the lock file exists
    if not os.path.isdir(os.path.dirname(lock_file_path)):
        common.log('SEVERE', 'Parent directory of lock file does not exist. The lock file for ' + step_name +
                   ' step has not been created: ' + lock_file_path, conf)
        return True

    # Return False if the run is currently processed
    if os.path.isfile(lock_file_path):
        return False

    # Create the lock file
    try:
        open(lock_file_path, 'w').close()
    except:
        common.log('SEVERE', 'The lock file cannot be created (' + sys.exc_info()[0] + ') for ' + step_name +
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
    """Lock the sync step.

    Arguments:
        conf: configuration object
        run_id: run_id
    """

    return lock_step(conf[BCL_DATA_PATH_KEY] + '/' + run_id + '.lock', 'sync', conf)


def lock_demux_step(conf, run_id):
    """Lock the demux step.

    Arguments:
        conf: configuration object
        run_id: run_id
    """

    return lock_step(conf[FASTQ_DATA_PATH_KEY] + '/' + run_id + '.lock', 'demux', conf)


def lock_recompress_step(conf, run_id):
    """Lock the recompress step.

    Arguments:
        conf: configuration object
        run_id: run_id
    """

    return lock_step(conf[FASTQ_DATA_PATH_KEY] + '/' + run_id + '.lock', 'recompress', conf)


def lock_qc_step(conf, run_id):
    """Lock the qc step.

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


def unlock_recompress_step(conf, run_id):
    """Unlock the recompress step.

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


def exception_error(step, desc, conf):
    """Handle an exception.

    Arguments:
        step: error step
        desc: description of the section error
        conf: configuration object
        run_id: run_id
    """
    exp_info = sys.exc_info()
    exception_msg = str(exp_info[0]) + ' (' + str(exp_info[1]) + ')'
    traceback_msg = traceback.format_exc(exp_info[2])

    short_message = desc + ', catch exception ' + exception_msg
    message = desc + ', catch exception ' + exception_msg + \
              '\n Stacktrace: \n' + traceback_msg

    if step == 'sync':
        sync_run.error(short_message, message, conf)
    elif step == 'demux':
        demux_run.error(short_message, message, conf)
    elif step == 'qc':
        qc_run.error(short_message, message, conf)
    else:
        # Log the exception
        common.log('SEVERE', 'Exception: ' + exception_msg, conf)
        common.log('WARNING', traceback_msg.replace('\n', ' '), conf)

        # Send a mail with the exception
        common.send_msg("[Aozan] Exception: " + exception_msg, traceback_msg,
                        True, conf)


def launch_steps(conf):
    """Launch steps.

    Arguments:
        conf: configuration object
    Returns:
        a boolean. True if launch_steps() is a success.
    """

    # Load run do not process
    hiseq_run_ids_do_not_process = hiseq_run.load_deny_run_ids(conf)

    # Discover new runs
    detection_new_run.discover_new_runs(hiseq_run_ids_do_not_process, conf)
    hiseq_run_ids_done = detection_end_run.discover_finished_runs(hiseq_run_ids_do_not_process, conf)


    #
    # Sync hiseq and storage
    #

    sync_run_ids_done = sync_run.load_processed_run_ids(conf)

    # Load run with higher priority
    prioritized_run_ids = common.load_prioritized_run_ids(conf)

    # Get the list of run available on HiSeq output
    if sync_run.is_sync_step_enable(conf):

        try:
            sync_denied_run_ids = sync_run.load_denied_run_ids(conf)
            for run_id in run_id_sorted_by_priority(
                                            hiseq_run_ids_done - sync_run_ids_done - hiseq_run_ids_do_not_process - sync_denied_run_ids,
                                            prioritized_run_ids):

                # print 'DEBUG sync launch on '+ str(run_id)

                if lock_sync_step(conf, run_id):
                    welcome(conf)
                    common.log('INFO', 'Synchronizing ' + run_id, conf)
                    if sync_run.sync(run_id, conf):
                        sync_run.add_run_id_to_processed_run_ids(run_id, conf)
                        sync_run_ids_done.add(run_id)
                        unlock_sync_step(conf, run_id)
                    else:
                        unlock_sync_step(conf, run_id)
                        return False
                else:
                    common.log('INFO', 'Synchronizing ' + run_id + ' is locked.', conf)

        except:
            exception_error('sync' , 'Failed synchronization for run ' + run_id, conf)

    #
    # Demultiplexing
    #

    if common.is_conf_value_equals_true(DEMUX_USE_HISEQ_OUTPUT_KEY, conf):
        sync_run_ids_done = hiseq_run_ids_done

    demux_run_ids_done = demux_run.load_processed_run_ids(conf)
    # print 'DEBUG launchStep demux done '+ str(demux_run_ids_done)
    # print 'DEBUG runs to demux '+ str(sync_run_ids_done - demux_run_ids_done)

    if common.is_conf_value_equals_true(DEMUX_STEP_KEY, conf):
        try:
            demux_denied_run_ids = demux_run.load_denied_run_ids(conf)
            for run_id in run_id_sorted_by_priority(sync_run_ids_done - demux_run_ids_done - demux_denied_run_ids,
                                                    prioritized_run_ids):

                # print 'DEBUG demux launch on ' + str(run_id)

                if lock_demux_step(conf, run_id):
                    welcome(conf)
                    common.log('INFO', 'Demultiplexing ' + run_id, conf)
                    if demux_run.demux(run_id, conf):
                        demux_run.add_run_id_to_processed_run_ids(run_id, conf)
                        demux_run_ids_done.add(run_id)
                        unlock_demux_step(conf, run_id)
                    else:
                        unlock_demux_step(conf, run_id)
                        return False
                else:
                    common.log('INFO', 'Demultiplexing ' + run_id + ' is locked.', conf)

        except:
            exception_error('demux', 'Failed demultiplexing for run ' + run_id, conf)

    #
    # Recompression
    #

    recompress_run_ids_done = recompress_run.load_processed_run_ids(conf)

    if common.is_conf_value_equals_true(RECOMPRESS_STEP_KEY, conf):

        try:
            recompress_denied_run_ids = recompress_run.load_denied_run_ids(conf)
            for run_id in run_id_sorted_by_priority(demux_run_ids_done - recompress_run_ids_done - recompress_denied_run_ids,
                                                    prioritized_run_ids):
                if lock_recompress_step(conf, run_id):
                    welcome(conf)
                    common.log('INFO', 'Recompressing ' + run_id, conf)
                    if recompress_run.recompress(run_id, conf):
                        recompress_run.add_run_id_to_processed_run_ids(run_id, conf)
                        recompress_run_ids_done.add(run_id)
                        unlock_recompress_step(conf, run_id)
                    else:
                        unlock_recompress_step(conf, run_id)
                        return False
                else:
                    common.log('INFO', 'Recompressing ' + run_id + ' is locked.', conf)

        except:
            exception_msg = str(sys.exc_info()[0]) + ' (' + str(sys.exc_info()[1]) + ')'
            traceback_msg = traceback.format_exc(sys.exc_info()[2])
            recompress_run.error("Failed recompression for run " + run_id + ", catch exception " + exception_msg,
                         "Failed recompression for run " + run_id + ", catch exception " + exception_msg + "\n Stacktrace : \n" + traceback_msg,
                         conf)


    #
    # Quality control
    #

    if not common.is_conf_value_equals_true(RECOMPRESS_STEP_KEY, conf):
        recompress_run_ids_done = demux_run_ids_done

    qc_run_ids_done = qc_run.load_processed_run_ids(conf)

    if common.is_conf_value_equals_true(QC_STEP_KEY, conf):

        try:
            qc_denied_run_ids = qc_run.load_denied_run_ids(conf)
            for run_id in run_id_sorted_by_priority(recompress_run_ids_done - qc_run_ids_done - qc_denied_run_ids,
                                                    prioritized_run_ids):
                if lock_qc_step(conf, run_id):
                    welcome(conf)
                    common.log('INFO', 'Quality control ' + run_id, conf)
                    if qc_run.qc(run_id, conf):
                        qc_run.add_run_id_to_processed_run_ids(run_id, conf)
                        qc_run_ids_done.add(run_id)
                        unlock_qc_step(conf, run_id)
                    else:
                        unlock_qc_step(conf, run_id)
                        return False
                else:
                    common.log('INFO', 'Quality control ' + run_id + ' is locked.', conf)

        except:
            exception_error('qc', 'Failed quality control for run ' + run_id, conf)

    #
    # Partial synchronization
    #

    working_run_ids = hiseq_run.get_working_run_ids(conf)

    if common.is_conf_value_equals_true(SYNC_CONTINUOUS_SYNC_KEY, conf):
        for run_id in (working_run_ids - sync_run_ids_done - hiseq_run_ids_do_not_process):
            if lock_partial_sync_step(conf, run_id):
                welcome(conf)
                common.log('INFO', 'Partial synchronizing ' + run_id, conf)
                if not sync_run.partial_sync(run_id, False, conf):
                    unlock_partial_sync_step(conf, run_id)
                    return False
                unlock_partial_sync_step(conf, run_id)
            else:
                common.log('INFO', 'Partial synchronizing ' + run_id + ' is locked.', conf)

    # Close Docker connections
    DockerConnection.getInstance(conf[DOCKER_URI_KEY]).closeConnections()

    # Everything is OK
    return True


def aozan_main():
    """Aozan main method.
    """

    # Define command line parser
    parser = OptionParser(usage='usage: ' + Globals.APP_NAME_LOWER_CASE + '.sh [options] conf_file')
    parser.add_option('-q', '--quiet', action='store_true', dest='quiet',
                      default=False, help='quiet')
    parser.add_option('-v', '--version', action='store_true', dest='version', help='Aozan version')
    parser.add_option('-e', '--exit-code', action='store_true', dest='exit_code',
                      help='Returns non zero exit code if a step fails')
    parser.add_option('-c', '--conf', action='store_true', dest='conf',
                      help='Default Aozan configuration, loads before configuration file.')

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

    # Check if OS is Linux
    if not SystemUtils.isLinux():
        sys.stderr.write('ERROR: Aozan can not be executed. Operating system is not Linux\n')
        sys.exit(1)

    # Check if configuration file exists
    conf_file = args[0]
    if not os.path.isfile(conf_file):
        sys.stderr.write('ERROR: Aozan can not be executed. Configuration file is missing: ' + \
                         conf_file + '\n')
        sys.exit(1)

    # Load Aozan conf file
    common.load_conf(conf, conf_file)

    # End of Aozan if aozan is not enable
    if common.is_conf_value_defined(AOZAN_ENABLE_KEY, 'false', conf):
        sys.exit(0)

    # Init logger
    try:
        Common.initLogger(conf[AOZAN_LOG_PATH_KEY], conf[AOZAN_LOG_LEVEL_KEY])
    except AozanException, exp:
        common.exception_msg(exp, conf)

    # Check main path file in configuration
    if not common.check_configuration(conf, conf_file):
        common.log('SEVERE',
                   'Aozan can not be executed. Configuration is invalid or missing, some useful directories ' +
                   'may be inaccessible. ',
                   conf)
        sys.exit(1)

    # check if global program set is available in PATH
    global_program_set = {"bash", "du", "touch", "chmod", "cp", "mv", "rm", "find", "tar"}
    for program in global_program_set:
        if not common.exists_in_path(program):
            common.log('SEVERE',
                  "Can't find all needed commands in PATH env var. Unable to find: " + program + " command.", conf)
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
            result = launch_steps(conf)

            # Remove lock file
            delete_lock_file(lock_file_path)

            # TODO remove *.lasterr files

            if something_to_do:
                common.log('INFO', 'Ending Aozan', conf)

                # Cancel logger, in case not be cancel properly
                Common.cancelLogger()

            if not result and options.exit_code:
                sys.exit(1)

        except:
            exception_error(None, '', conf)

            common.log('INFO', 'Ending Aozan', conf)

            # Remove lock file
            delete_lock_file(lock_file_path)

            # Cancel logger, in case not be cancel properly
            Common.cancelLogger()
            sys.exit(1)
    else:
        if not options.quiet:
            sys.stderr.write('ERROR: Aozan can not be executed. A lock file exists.\n')
        if not os.path.exists('/proc/%d' % (load_pid_in_lock_file(lock_file_path))):
            common.error('[Aozan] A lock file exists', 'A lock file exists at ' + conf[LOCK_FILE_KEY] +
                         ". Please investigate last error and then remove the lock file.", True,
                         conf[AOZAN_VAR_PATH_KEY] + '/aozan.lasterr', conf)


# Launch Aozan main
if __name__ == "__main__":
    aozan_main()
