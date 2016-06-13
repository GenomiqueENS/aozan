# -*- coding: utf-8 -*-

import os, time
import common, hiseq_run

from fr.ens.biologie.genomique.aozan.Settings import AOZAN_VAR_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import HISEQ_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import SYNC_STEP_KEY
from fr.ens.biologie.genomique.aozan.Settings import SYNC_SPACE_FACTOR_KEY
from fr.ens.biologie.genomique.aozan.Settings import SYNC_EXCLUDE_CIF_KEY
from fr.ens.biologie.genomique.aozan.Settings import SYNC_CONTINUOUS_SYNC_MIN_AGE_FILES_KEY
from fr.ens.biologie.genomique.aozan.Settings import TMP_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import REPORTS_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import REPORTS_URL_KEY


def is_sync_step_enable(conf):
    """Check if all parameters useful for synchronization step are defined

    Arguments:
        conf: configuration dictionary
    """

    # Synchronization step: True
    if common.is_conf_value_equals_true(SYNC_STEP_KEY, conf):

        # Check bcl path must be defined
        if common.is_conf_key_exists(BCL_DATA_PATH_KEY, conf):
            bcl_path = conf[BCL_DATA_PATH_KEY]

            # Check bcl not same hiseq output path
            for path in hiseq_run.get_hiseq_data_paths(conf):
                if path == bcl_path:
                    error('error configuration.',
                          'Basecalling path and hiseq output data path are the same: ' + bcl_path, conf)
                    return False

            return True

    return False


def load_processed_run_ids(conf):
    """Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

    return common.load_processed_run_ids(conf[AOZAN_VAR_PATH_KEY] + '/sync.done')


def add_run_id_to_processed_run_ids(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run_id: The run id
        conf: configuration dictionary
    """

    common.add_run_id_to_processed_run_ids(run_id, conf[AOZAN_VAR_PATH_KEY] + '/sync.done', conf)


def error(short_message, message, conf):
    """Error handling.

    Arguments:
        short_message: short description of the message
        message: message
        conf: configuration dictionary
    """

    common.error('[Aozan] synchronizer: ' + short_message, message, conf[AOZAN_VAR_PATH_KEY] + '/sync.lasterr', conf)


def get_exclude_files_list(run_id, conf):
    """ Build list of excluded extension file from sequencer type.

    Arguments:
        run_id: the run id
        conf: configuration dictionary

    Return:
        excluded extension files list
    """

    # Check if excluded file is required
    if not common.is_conf_value_equals_true(SYNC_EXCLUDE_CIF_KEY, conf):
        return []

    # TODO exclude *bcl.bgzf.bci  with RTA 2 ?
    return ['*.cif', '*_pos.txt', '*.errorMap', '*.FWHMMap']


def partial_sync(run_id, last_sync, conf):
    """Partial synchronization of a run.

        Arguments:
                run_id: the run id
                last_sync: last synchronization
                conf: configuration dictionary
    """

    hiseq_data_path = hiseq_run.find_hiseq_run_path(run_id, conf)
    bcl_data_path = conf[BCL_DATA_PATH_KEY]
    final_output_path = bcl_data_path + '/' + run_id

    # Check if hiseq_data_path exists
    if hiseq_data_path is False:
        error('Sequencer run data not found',
              'Sequencer data for run ' + run_id + 'not found in sequencer directories (' + conf[HISEQ_DATA_PATH_KEY] + ')',
              conf)
        return False

    # Check if hiseq_data_path exists
    if not os.path.exists(hiseq_data_path):
        error("Sequencer directory does not exists", "Sequencer directory does not exists: " + hiseq_data_path, conf)
        return False

    # Check if bcl_data_path exists
    if not os.path.exists(bcl_data_path):
        error("Basecalling directory does not exists", "Basecalling directory does not exists: " + bcl_data_path, conf)
        return False

    # Check if final output path already exists
    if os.path.exists(final_output_path):
        error("Basecalling directory for run " + run_id + " already exists",
              "Basecalling directory for run " + run_id + " already exists: " + final_output_path, conf)
        return False

    input_path = hiseq_data_path + '/' + run_id
    output_path = bcl_data_path + '/' + run_id + '.tmp'

    # Create output path for run if not exists
    if not os.path.exists(output_path):
        os.mkdir(output_path)

    input_path_du = common.du(input_path)
    output_path_du = common.du(output_path)
    output_path_df = common.df(bcl_data_path)
    du_factor = float(conf[SYNC_SPACE_FACTOR_KEY])
    space_needed = input_path_du * du_factor - output_path_du

    common.log("WARNING", "Sync step: input disk usage: " + str(input_path_du), conf)
    common.log("WARNING", "Sync step: output disk free: " + str(output_path_df), conf)
    common.log("WARNING", "Sync step: space needed: " + str(space_needed), conf)

    # Check if free space is available on
    if output_path_df < space_needed:
        error("Not enough disk space to perform synchronization for run " + run_id,
              "Not enough disk space to perform synchronization for run " + run_id +
              '.\n%.2f Gb' % (space_needed / 1024 / 1024 / 1024) + ' is needed (factor x' + str(
                  du_factor) + ') on ' + bcl_data_path + '.', conf)
        return False

        # exclude CIF files ?
    #     if common.is_conf_value_equals_true(SYNC_EXCLUDE_CIF_KEY, conf):
    #         exclude_files = ['*.cif', '*_pos.txt', '*.errorMap', '*.FWHMMap']
    #     else:
    #         exclude_files = []

    # Extract exclude file from sequencer type and configuration
    exclude_files = get_exclude_files_list(run_id, conf)

    rsync_manifest_path = conf[TMP_PATH_KEY] + '/' + run_id + '.rsync.manifest'
    rsync_params = ''

    if last_sync:
        for exclude_file in exclude_files:
            rsync_params += " --exclude '" + exclude_file + "' "
    else:
        # Exclude files that will be rewritten severals times during the run
        exclude_files.extend(['*.bin', '*.txt', '*.xml'])
        cmd = 'cd ' + input_path + ' && find . -type f -mmin +' + conf[SYNC_CONTINUOUS_SYNC_MIN_AGE_FILES_KEY]
        for exclude_file in exclude_files:
            cmd += " -not -name '" + exclude_file + "' "
        cmd += ' > ' + rsync_manifest_path
        common.log("INFO", "exec: " + cmd, conf)
        if os.system(cmd) != 0:
            error("error while executing rsync for run " + run_id, 'Error while executing find.\nCommand line:\n' + cmd,
                  conf)
            return False
        rsync_params = '--files-from=' + rsync_manifest_path

    # Copy data from hiseq path to bcl path
    cmd = 'rsync  -a --no-owner --no-group ' + rsync_params + ' ' + input_path + '/ ' + output_path
    common.log("INFO", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("error while executing rsync for run " + run_id, 'Error while executing rsync.\nCommand line:\n' + cmd,
              conf)
        return False

    if not last_sync:
        os.remove(rsync_manifest_path)

    return True


def sync(run_id, conf):
    """Synchronize a run.

    Arguments:
        run_id: the run id
        conf: configuration dictionary
    """

    start_time = time.time()
    common.log('INFO', 'Sync step: start', conf)

    bcl_data_path = conf[BCL_DATA_PATH_KEY]
    reports_data_base_path = conf[REPORTS_DATA_PATH_KEY]
    output_path = bcl_data_path + '/' + run_id

    # Check if reports_data_path exists
    if not os.path.exists(reports_data_base_path):
        error("Report directory does not exists", "Report directory does not exists: " + reports_data_base_path, conf)
        return False

    # Check if enough space to store reports
    if common.df(reports_data_base_path) < 10 * 1024 * 1024 * 1024:
        error("Not enough disk space to store aozan reports for run " + run_id,
              "Not enough disk space to store aozan reports for run " + run_id +
              '.\nNeed more than 10 Gb on ' + reports_data_base_path + '.', conf)
        return False

    # Do the synchronization
    if not partial_sync(run_id, True, conf):
        return False

    # Rename partial sync directory to final run BCL directory
    if os.path.exists(output_path + '.tmp'):
        os.rename(output_path + '.tmp', output_path)

    # Check used and free space
    df_in_bytes = common.df(bcl_data_path)
    du_in_bytes = common.du(output_path)
    df = df_in_bytes / (1024 * 1024 * 1024)
    du = du_in_bytes / (1024 * 1024 * 1024)

    common.log("WARNING", "Sync step: output disk free after sync: " + str(df_in_bytes), conf)
    common.log("WARNING", "Sync step: space used by sync: " + str(du_in_bytes), conf)

    duration = time.time() - start_time

    msg = 'End of synchronization for run ' + run_id + '.\n' + \
          'Job finished at ' + common.time_to_human_readable(time.time()) + \
          ' with no error in ' + common.duration_to_human_readable(duration) + '.\n\n' + \
          'Run output files (without .cif files) can be found in the following directory:\n  ' + output_path

    # Add path to report if reports.url exists
    if common.is_conf_key_exists(REPORTS_URL_KEY, conf):
        msg += '\n\nRun reports can be found at following location:\n  ' + conf[REPORTS_URL_KEY] + '/' + run_id

    msg += '\n\nFor this task %.2f GB has been used and %.2f GB still free.' % (du, df)

    common.send_msg('[Aozan] End of synchronization for run ' + run_id + ' on ' +
                    common.get_instrument_name(run_id, conf), msg, False, conf)
    common.log('INFO', 'sync step: success in ' + common.duration_to_human_readable(duration), conf)
    return True
