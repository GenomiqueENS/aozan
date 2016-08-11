'''
Created on 28 oct. 2011

@author: jourdren
'''
import os.path, stat
import common, time
import demux_run, hiseq_run

from fr.ens.biologie.genomique.aozan import QC, Settings
from fr.ens.biologie.genomique.aozan import AozanException

from java.lang import Throwable

from fr.ens.biologie.genomique.aozan.Settings import FASTQ_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import AOZAN_VAR_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import REPORTS_URL_KEY
from fr.ens.biologie.genomique.aozan.Settings import QC_REPORT_SAVE_RAW_DATA_KEY
from fr.ens.biologie.genomique.aozan.Settings import REPORTS_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import QC_REPORT_STYLESHEET_KEY
from fr.ens.biologie.genomique.aozan.Settings import QC_REPORT_SAVE_REPORT_DATA_KEY
from fr.ens.biologie.genomique.aozan.Settings import TMP_PATH_KEY

DENY_FILE = 'qc.deny'
DONE_FILE = 'qc.done'
LASTERR_FILE = 'qc.lasterr'


def load_denied_run_ids(conf):
    """Load the list of the denied run ids.

    Arguments:
        conf: configuration dictionary
    """
    return common.load_run_ids(conf[AOZAN_VAR_PATH_KEY] + '/' + DENY_FILE)


def load_processed_run_ids(conf):
    """Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

    return common.load_run_ids(conf[AOZAN_VAR_PATH_KEY] + '/' + DONE_FILE)


def add_run_id_to_processed_run_ids(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run_id: The run id
        conf: configuration dictionary
    """

    common.add_run_id(run_id, conf[AOZAN_VAR_PATH_KEY] + '/' + DONE_FILE, conf)


def error(short_message, message, conf):
    """Error handling.

    Arguments:
        short_message: short description of the message
        message: message
        conf: configuration dictionary
    """

    common.error('[Aozan] qc: ' + short_message, message, conf[AOZAN_VAR_PATH_KEY] + '/' + LASTERR_FILE, conf)


def qc(run_id, conf):
    """Proceed to quality control of a run.

    Arguments:
        run_id: The run id
        conf: configuration dictionary
    """

    start_time = time.time()

    input_run_data_path = common.get_input_run_data_path(run_id, conf)

    if input_run_data_path is None:
        return False

    fastq_input_dir = conf[FASTQ_DATA_PATH_KEY] + '/' + run_id
    reports_data_base_path = conf[REPORTS_DATA_PATH_KEY]
    reports_data_path = reports_data_base_path + '/' + run_id
    qc_output_dir = reports_data_path + '/qc_' + run_id
    tmp_extension = '.tmp'

    common.log('INFO', 'QC step: start', conf)

    # Check if input run data data exists
    if input_run_data_path is None:
        error("Basecalling data directory does not exists", "Basecalling data directory does not exists.", conf)
        return False

    # Check if input root fastq root data exists
    if not common.is_dir_exists(FASTQ_DATA_PATH_KEY, conf):
        error("Fastq data directory does not exists",
              "Fastq data directory does not exists: " + conf[FASTQ_DATA_PATH_KEY], conf)
        return False

    # Create if not exists report directory for the run
    if not os.path.exists(reports_data_path):
        os.mkdir(reports_data_path)

    # Check if temporary directory exists
    if not common.is_dir_exists(TMP_PATH_KEY, conf):
        error("Temporary directory does not exists", "Temporary directory does not exists: " + conf[TMP_PATH_KEY], conf)
        return False

    # Check if the output directory already exists
    if os.path.exists(qc_output_dir):
        error("quality control report directory already exists for run " + run_id,
              'The quality control report directory already exists for run ' + run_id + ': ' + qc_output_dir, conf)
        return False

    # Check if the output directory already exists
    if os.path.exists(reports_data_path + '/qc_' + run_id + '.tar.bz2'):
        error("quality control report archive already exists for run " + run_id,
              'The quality control report archive already exists for run ' + run_id + ': ' +
              reports_data_path + '/qc_' + run_id + '.tar.bz2', conf)
        return False

    # Check if enough free space is available
    if common.df(conf[REPORTS_DATA_PATH_KEY]) < 1 * 1024 * 1024 * 1024:
        error("Not enough disk space to store aozan quality control for run " + run_id,
              "Not enough disk space to store aozan reports for run " + run_id +
              '.\nNeed more than 10 Gb on ' + conf[REPORTS_DATA_PATH_KEY] + '.', conf)
        return False

    # Create temporary temporary directory
    qc_output_dir += tmp_extension
    if not os.path.exists(qc_output_dir):
        os.mkdir(qc_output_dir)

    try:

        # Initialize the QC object
        qc = QC(Settings(conf), input_run_data_path, fastq_input_dir, qc_output_dir, conf[TMP_PATH_KEY], run_id)

        # Compute the report
        report = qc.computeReport()
    except AozanException, exp:
        error("error while computing qc report for run " + run_id + ".", common.exception_msg(exp, conf), conf)
        return False
    except Throwable, exp:
        error("error while computing qc report for run " + run_id + ".", common.exception_msg(exp, conf), conf)
        return False

    # Remove qc data if not demand
    if common.is_conf_value_defined(QC_REPORT_SAVE_RAW_DATA_KEY, 'false', conf):
        try:
            os.remove(qc_output_dir + '/data-' + run_id + '.txt')
            # qc.writeRawData(report, qc_output_dir + '/data-' + run_id + '.txt')
        except AozanException, exp:
            error("error while removing qc raw data for run " + run_id + ".", exp.getMessage(), conf)
            return False

    # Write the XML report
    if common.is_conf_value_equals_true(QC_REPORT_SAVE_REPORT_DATA_KEY, conf):
        try:
            qc.writeXMLReport(report, qc_output_dir + '/' + run_id + '.xml')
        except AozanException, exp:
            error("error while computing qc report XML for run " + run_id + ".", common.exception_msg(exp, conf), conf)
            return False
        except Throwable, exp:
            error("error while computing qc report XML for run " + run_id + ".", common.exception_msg(exp, conf), conf)
            return False

    # Remove tmp extension of temporary qc directory
    os.rename(qc_output_dir, qc_output_dir[:-len(tmp_extension)])
    qc_output_dir = qc_output_dir[:-len(tmp_extension)]

    # Write the HTML report
    html_report_file = qc_output_dir + '/' + run_id + '.html'
    try:
        if not common.is_conf_key_exists(QC_REPORT_STYLESHEET_KEY, conf):
            qc.writeReport(report, None, html_report_file)
        else:
            qc.writeReport(report, conf[QC_REPORT_STYLESHEET_KEY], html_report_file)
    except AozanException, exp:
        error("error while computing qc report HTML for run " + run_id + ".", common.exception_msg(exp, conf), conf)
        return False
    except Throwable, exp:
        error("error while computing qc report HTML for run " + run_id + ".", common.exception_msg(exp, conf), conf)
        return False

    # Check if the report has been generated
    if not os.path.exists(html_report_file):
        error("error while computing qc report for run " + run_id + ".", "No html report generated", conf)
        return False

    # Archive the reports
    cmd = 'cd ' + reports_data_path + '  && ' + \
          'tar cjf qc_' + run_id + '.tar.bz2 qc_' + run_id
    common.log("INFO", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("error while saving the qc archive file for " + run_id,
              'Error while saving the  qc archive file.\nCommand line:\n' + cmd, conf)
        return False

    # Set read only basecall stats archives files
    os.chmod(reports_data_path + '/qc_' + run_id + '.tar.bz2', stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH)

    # Check if the report has been generated
    if not os.path.exists(html_report_file):
        error("error while computing qc report for run " + run_id + ".", "No html report generated", conf)
        return False

    # The output directory must be read only
    cmd = 'chmod -R ugo-w ' + qc_output_dir
    common.log("INFO", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("error while setting read only the output qc directory for run " + run_id,
              'Error while setting read only the output qc directory.\nCommand line:\n' + cmd, conf)
        return False

    # Create index.hml file
    sessions = [Settings.HISEQ_STEP_KEY, Settings.DEMUX_STEP_KEY, Settings.QC_STEP_KEY]
    common.create_html_index_file(conf, run_id, sessions)

    df_in_bytes = common.df(qc_output_dir)
    du_in_bytes = common.du(qc_output_dir)
    df = df_in_bytes / (1024 * 1024 * 1024)
    du = du_in_bytes / (1024 * 1024)

    common.log("WARNING", "QC step: output disk free after qc: " + str(df_in_bytes), conf)
    common.log("WARNING", "QC step: space used by qc: " + str(du_in_bytes), conf)

    duration = time.time() - start_time

    msg = 'End of quality control for run ' + run_id + '.' + \
          '\nJob finished at ' + common.time_to_human_readable(time.time()) + \
          ' with no error in ' + common.duration_to_human_readable(duration) + '. ' + \
          'You will find attached to this message the quality control report.\n\n' + \
          'QC files for this run ' + \
          'can be found in the following directory:\n  ' + qc_output_dir

    # Add path to report if reports.url exists
    if common.is_conf_key_exists(REPORTS_URL_KEY, conf):
        msg += '\n\nRun reports can be found at following location:\n  ' + conf[REPORTS_URL_KEY] + '/' + run_id

    msg += '\n\nFor this task %.2f MB has been used and %.2f GB still free.' % (du, df)

    common.send_msg_with_attachment('[Aozan] End of quality control for run ' + run_id + ' on ' +
                                    common.get_instrument_name(run_id, conf), msg, html_report_file, conf)
    common.log('INFO', 'QC step: success in ' + common.duration_to_human_readable(duration), conf)
    return True
