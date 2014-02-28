# -*- coding: utf-8 -*-

import os, stat, time
import common, hiseq_run
from fr.ens.transcriptome.aozan import Settings


def load_processed_run_ids(conf):
    """Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

    return common.load_processed_run_ids(conf[Settings.AOZAN_VAR_PATH_KEY] + '/sync.done')

def add_run_id_to_processed_run_ids(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run id: The run id
        conf: configuration dictionary
    """

    common.add_run_id_to_processed_run_ids(run_id, conf[Settings.AOZAN_VAR_PATH_KEY] + '/sync.done', conf)


def error(short_message, message, conf):
    """Error handling.

    Arguments:
        short_message: short description of the message
        message: message
        conf: configuration dictionary
    """

    common.error('[Aozan] synchronizer: ' + short_message, message, conf[Settings.AOZAN_VAR_PATH_KEY] + '/sync.lasterr', conf)


def partial_sync(run_id, last_sync, conf):
    """Partial synchronization of a run.

        Arguments:
                run_id: the run id
                conf: configuration dictionary
    """

    hiseq_data_path = hiseq_run.find_hiseq_run_path(run_id, conf)
    bcl_data_path = conf[Settings.BCL_DATA_PATH_KEY]
    final_output_path = bcl_data_path + '/' + run_id

    # Check if hiseq_data_path exists
    if hiseq_data_path == False:
        error('HiSeq run data not found', 'HiSeq data for run ' + run_id + 'not found in HiSeq directories (' + conf[Settings.HISEQ_DATA_PATH_KEY] + ')', conf)
        return False

    # Check if hiseq_data_path exists
    if not os.path.exists(hiseq_data_path):
        error("HiSeq directory does not exists", "HiSeq directory does not exists: " + hiseq_data_path, conf)
        return False

    # Check if bcl_data_path exists
    if not os.path.exists(bcl_data_path):
        error("Basecalling directory does not exists", "Basecalling directory does not exists: " + bcl_data_path, conf)
        return False

    # Check if final output path already exists
    if os.path.exists(final_output_path):
        error("Basecalling directory for run " + run_id + " already exists", "Basecalling directory for run " + run_id + " already exists: " + final_output_path, conf)
        return False

    input_path = hiseq_data_path + '/' + run_id
    output_path = bcl_data_path + '/' + run_id + '.tmp'
    
    # Create output path for run if not exists
    if not os.path.exists(output_path):
        os.mkdir(output_path)    

    input_path_du = common.du(input_path)
    output_path_du = common.du(output_path)
    output_path_df = common.df(bcl_data_path)
    du_factor = float(conf[Settings.SYNC_SPACE_FACTOR_KEY])
    space_needed = input_path_du * du_factor - output_path_du

    common.log("WARNING", "Sync step: input disk usage: " + str(input_path_du), conf)
    common.log("WARNING", "Sync step: output disk free: " + str(output_path_df), conf)
    common.log("WARNING", "Sync step: space needed: " + str(space_needed), conf)

    # Check if free space is available on
    if output_path_df < space_needed:
        error("Not enough disk space to perform synchronization for run " + run_id, "Not enough disk space to perform synchronization for run " + run_id +
              '.\n%.2f Gb' % (space_needed / 1024 / 1024 / 1024) + ' is needed (factor x' + str(du_factor) + ') on ' + bcl_data_path + '.', conf)
        return False

    # exclude CIF files ?
    if common.isTrue(Settings.SYNC_EXCLUDE_CIF_KEY, conf):
        exclude_files = ['*.cif', '*_pos.txt', '*.errorMap', '*.FWHMMap']
    else:
        exclude_files = []

    rsync_manifest_path = conf[Settings.TMP_PATH_KEY] + '/' + run_id + '.rsync.manifest'
    rsync_params = ''

    if last_sync:
        for exclude_file in exclude_files:
            rsync_params += " --exclude '" + exclude_file + "' "
    else:
        # Exclude files that will be rewritten severals times during the run
        exclude_files.extend(['*.bin', '*.txt', '*.xml'])
        cmd = 'cd ' + input_path + ' && find . -type f -mmin +' + conf[Settings.SYNC_CONTINUOUS_SYNC_MIN_AGE_FILES_KEY]
        for exclude_file in exclude_files:
            cmd += " -not -name '" + exclude_file + "' "
        cmd += ' > ' + rsync_manifest_path
        common.log("SEVERE", "exec: " + cmd, conf)
        if os.system(cmd) != 0:
            error("error while executing rsync for run " + run_id, 'Error while executing find.\nCommand line:\n' + cmd, conf)
            return False
        rsync_params = '--files-from=' + rsync_manifest_path

    # Copy data from hiseq path to bcl path
    cmd = 'rsync  -a ' + rsync_params + ' ' + input_path + '/ ' + output_path
    common.log("SEVERE", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("error while executing rsync for run " + run_id, 'Error while executing rsync.\nCommand line:\n' + cmd, conf)
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

    bcl_data_path = conf[Settings.BCL_DATA_PATH_KEY]
    reports_data_base_path = conf[Settings.REPORTS_DATA_PATH_KEY]
    tmp_base_path = conf[Settings.TMP_PATH_KEY]

    output_path = bcl_data_path + '/' + run_id
    reports_data_path = reports_data_base_path + '/' + run_id
    report_prefix = 'report_'
    hiseq_log_prefix = 'hiseq_log_'
    report_archive_file = report_prefix + run_id + '.tar.bz2'
    hiseq_log_archive_file = hiseq_log_prefix + run_id + '.tar.bz2'

    # Check if reports_data_path exists
    if not os.path.exists(reports_data_base_path):
        error("Report directory does not exists", "Report directory does not exists: " + reports_data_base_path, conf)
        return False

    # Check if reports archive exists
    if os.path.exists(reports_data_path + '/' + report_archive_file):
        error('Report archive already exists for run ' + run_id, 'Report archive already exists for run ' + run_id + ' : ' + report_archive_file, conf)
        return False

    # Check if hiseq log archive exists
    if os.path.exists(reports_data_path + '/' + hiseq_log_archive_file):
        error('Hiseq log archive already exists for run ' + run_id, 'Hiseq log archive already exists for run ' + run_id + ' : ' + hiseq_log_archive_file, conf)
        return False

    # Check if temporary directory exists
    if not os.path.exists(tmp_base_path):
        error("Temporary directory does not exists", "Temporary directory does not exists: " + tmp_base_path, conf)
        return False

    # Check if enough space to store reports
    if common.df(reports_data_base_path) < 10 * 1024 * 1024 * 1024:
        error("Not enough disk space to store aozan reports for run " + run_id, "Not enough disk space to store aozan reports for run " + run_id +
              '.\nNeed more than 10 Gb on ' + reports_data_base_path + '.', conf)
        return False

    # Do the synchronization
    if not partial_sync(run_id, True, conf):
        return False

    # Rename partial sync directory to final run BCL directory
    if os.path.exists(output_path + '.tmp'):
        os.rename(output_path + '.tmp', output_path)

    # Create if not exists archive directory for the run
    if not os.path.exists(reports_data_path):
        os.mkdir(reports_data_path)

    # Save quality control data
    tmp_path = tmp_base_path + '/' + run_id
    if os.path.exists(tmp_path):
        cmd = 'rm -rf ' + tmp_path
        common.log("SEVERE", "exec: " + cmd, conf)
        if os.system(cmd) != 0:
            error("error while removing existing temporary directory", 'Error while removing existing temporary directory.\nCommand line:\n' + cmd, conf)
            return False
    os.mkdir(tmp_path)
    cmd = 'cd ' + bcl_data_path + '/' + run_id + ' && ' + \
        'cp -rp InterOp RunInfo.xml runParameters.xml ' + tmp_path + ' && ' + \
        'cd ' + tmp_base_path + ' && ' + \
        'mv ' + run_id + ' ' + hiseq_log_prefix + run_id + ' && ' + \
        'tar cjf ' + reports_data_path + '/' + hiseq_log_archive_file + ' ' + hiseq_log_prefix + run_id + ' && ' + \
        'rm -rf ' + tmp_path + ' && rm -rf ' + hiseq_log_prefix + run_id
    common.log("SEVERE", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("error while saving Illumina quality control for run " + run_id, 'Error saving Illumina quality control.\nCommand line:\n' + cmd, conf)
        return False

    # Save html reports
    if os.path.exists(tmp_path):
        cmd = 'rm -rf ' + tmp_path
        common.log("SEVERE", "exec: " + cmd, conf)
        if os.system(cmd) != 0:
            error("error while removing existing temporary directory", 'Error while removing existing temporary directory.\nCommand line:\n' + cmd, conf)
            return False
    os.mkdir(tmp_path)
    cmd = 'cd ' + output_path + '/Data' + ' && ' + \
        'cp -rp Status_Files reports Status.htm ../First_Base_Report.htm ' + tmp_path + ' && ' + \
        'cd ' + tmp_base_path + ' && ' + \
        'mv ' + run_id + ' ' + report_prefix + run_id + ' && ' + \
        'tar cjf ' + reports_data_path + '/' + report_archive_file + ' ' + report_prefix + run_id + ' && ' + \
        'mv ' + report_prefix + run_id + ' ' + reports_data_path
        # 'cd ' + base_dir_path + ' && ' + \
        # 'cp -p ../First_Base_Report.htm ' + reports_data_path + '/' + run_id + '/ && ' + \
    common.log("SEVERE", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("error while saving Illumina html reports for run " + run_id, 'Error saving Illumina html reports.\nCommand line:\n' + cmd, conf)
        return False

    # Create index.hml file
    common.create_html_index_file(conf, reports_data_path + '/index.html', run_id, ['sync'])

    # Set read only archives files
    os.chmod(reports_data_path + '/' + report_archive_file, stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH)
    os.chmod(reports_data_path + '/' + hiseq_log_archive_file, stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH)


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
    if common.isExists(Settings.REPORTS_URL_KEY, conf):
        msg += '\n\nRun reports can be found at following location:\n  ' + conf[Settings.REPORTS_URL_KEY] + '/' + run_id

    msg += '\n\nFor this task %.2f GB has been used and %.2f GB still free.' % (du, df)

    common.send_msg('[Aozan] End of synchronization for run ' + run_id, msg, False, conf)
    common.log('INFO', 'sync step: success in ' + common.duration_to_human_readable(duration), conf)
    return True
