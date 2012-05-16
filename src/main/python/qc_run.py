'''
Created on 28 oct. 2011

@author: jourdren
'''
import os.path, stat
import common, time
from fr.ens.transcriptome.aozan import QC
from fr.ens.transcriptome.aozan import AozanException

def load_processed_run_ids(conf):
    """Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

    return common.load_processed_run_ids(conf['aozan.var.path'] + '/qc.done')

def add_run_id_to_processed_run_ids(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run id: The run id
        conf: configuration dictionary
    """

    common.add_run_id_to_processed_run_ids(run_id, conf['aozan.var.path'] + '/qc.done', conf)


def error(short_message, message, conf):
    """Error handling.

    Arguments:
        short_message: short description of the message
        message: message
        conf: configuration dictionary
    """

    common.error('[Aozan] qc: ' + short_message, message, conf['aozan.var.path'] + '/qc.lasterr', conf)


def qc(run_id, conf):
    """Proceed to quality control of a run.

    Arguments:
        run id: The run id
        conf: configuration dictionary
    """

    start_time = time.time()

    fastq_input_dir = conf['fastq.data.path'] + '/' + run_id
    bcl_input_dir = conf['bcl.data.path'] + '/' + run_id
    reports_data_base_path = conf['reports.data.path']
    reports_data_path = reports_data_base_path + '/' + run_id
    qc_output_dir = reports_data_path + '/qc_' + run_id

    # Check if input root bcl data exists
    if not os.path.exists(conf['bcl.data.path']):
        error("Basecalling data directory does not exists", "Basecalling data directory does not exists: " + conf['bcl.data.path'], conf)
        return False

    # Check if input root fastq root data exists
    if not os.path.exists(conf['fastq.data.path']):
        error("Fastq data directory does not exists", "Fastq data directory does not exists: " + conf['fastq.data.path'], conf)
        return False

    # Check if reports path directory exists
    if not os.path.exists(reports_data_path):
        error("Report directory does not exists", "Report directory does not exists: " + reports_data_path, conf)
        return False

    # Check if temporary directory exists
    if not os.path.exists(conf['tmp.path']):
        error("Temporary directory does not exists", "Temporary directory does not exists: " + conf['tmp.path'], conf)
        return False

    # Check if the output directory already exists
    if os.path.exists(qc_output_dir):
        error("quality control report directory already exists for run " + run_id,
              'The quality control report directory already exists for run ' + run_id + ': ' + qc_output_dir, conf)
        return False

    # Check if the output directory already exists
    if os.path.exists(reports_data_path + '/qc_' + run_id + '.tar.bz2'):
        error("quality control report archive already exists for run " + run_id,
              'The quality control report archive already exists for run ' + run_id + ': ' + reports_data_path + '/qc_' + run_id + '.tar.bz2', conf)
        return False

    # Check if enough free space is available
    if common.df(conf['reports.data.path']) < 1 * 1024 * 1024 * 1024:
        error("Not enough disk space to store aozan quality control for run " + run_id, "Not enough disk space to store aozan reports for run " + run_id +
              '.\nNeed more than 10 Gb on ' + conf['qc.data.path'] + '.', conf)
        return False

    # Initialize the QC object
    qc = QC(conf, conf['tmp.path'])

    # Compute the report
    try:
        report = qc.computeReport(bcl_input_dir, fastq_input_dir, qc_output_dir, run_id)
    except AozanException, exp:
        error("error while computing qc report for run " + run_id + ".", exp.getMessage(), conf)
        return False


    # Write the XML report
    if conf['qc.report.save.report.data'].lower().strip() == 'true':
        try:
            qc.writeXMLReport(report, qc_output_dir + '/' + run_id + '.xml')
        except AozanException, exp:
            error("error while computing qc report for run " + run_id + ".", exp.getMessage(), conf)
            return False

    # Write the HTML report
    html_report_file = qc_output_dir + '/' + run_id + '.html'
    try:
        if conf['qc.report.stylesheet'] == '':
            qc.writeReport(report, None, html_report_file)
        else:
            qc.writeReport(report, conf['qc.report.stylesheet'], html_report_file)
    except AozanException, exp:
        error("error while computing qc report for run " + run_id + ".", exp.getMessage(), conf)
        return False

    # Write qc data
    if conf['qc.report.save.raw.data'].lower().strip() == 'true':
        try:
            qc.writeRawData(report, qc_output_dir + '/data-' + run_id + '.txt')
        except AozanException, exp:
            error("error while computing qc raw data for run " + run_id + ".", exp.getMessage(), conf)
            return False

    # Archive the reports
    cmd = 'cd ' + reports_data_path + '  && ' + \
       'tar cjf qc_' + run_id + '.tar.bz2 qc_' + run_id
    common.log("DEBUG", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("error while saving the qc archive file for " + run_id, 'Error while saving the  qc archive file.\nCommand line:\n' + cmd, conf)
        return False

    # Set read only basecall stats archives files
    os.chmod(reports_data_path + '/qc_' + run_id + '.tar.bz2', stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH)

    # Check if the report has been generated
    if not os.path.exists(html_report_file):
        error("error while computing qc report for run " + run_id + ".", "No html report generated", conf)
        return False



    # The output directory must be read only
    cmd = 'chmod -R ugo-w ' + qc_output_dir
    common.log("DEBUG", "exec: " + cmd, conf)
    if os.system(cmd) != 0:
        error("error while setting read only the output qc directory for run " + run_id, 'Error while setting read only the output qc directory.\nCommand line:\n' + cmd, conf)
        return False

    df_in_bytes = common.df(qc_output_dir)
    du_in_bytes = common.du(qc_output_dir)
    df = df_in_bytes / (1024 * 1024 * 1024)
    du = du_in_bytes / (1024 * 1024 * 1024)

    common.log("DEBUG", "QC step: output disk free after qc: " + str(df_in_bytes), conf)
    common.log("DEBUG", "QC step: space used by qc: " + str(du_in_bytes), conf)

    duration = time.time() - start_time

    msg = 'End of quality control for run ' + run_id + '.' + \
        '\nJob finished at ' + common.time_to_human_readable(time.time()) + \
        ' with no error in ' + common.duration_to_human_readable(duration) + '. ' + \
        'You will find attached to this message the quality control report.\n\n' + \
        'QC files for this run ' + \
        'can be found in the following directory:\n  ' + qc_output_dir

    # Add path to report if reports.url exists
    if conf['reports.url'] != None and conf['reports.url'] != '':
        msg += '\n\nRun reports can be found at following location:\n  ' + conf['reports.url'] + '/' + run_id

    msg += '\n\nFor this task %.2f GB has been used and %.2f GB still free.' % (du, df)


    common.send_msg_with_attachment('[Aozan] End of quality control for run ' + run_id, msg, html_report_file, conf)
    common.log('INFO', 'QC step: success in ' + common.duration_to_human_readable(duration), conf)
    return True
