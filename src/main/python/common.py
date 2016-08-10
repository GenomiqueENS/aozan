# -*- coding: utf-8 -*-

'''
Created on 25 oct. 2011

@author: Laurent Jourdren
'''

import hiseq_run, sync_run, demux_run
import smtplib, os.path, time, sys
import fcntl, time, errno
import mimetypes
from email.utils import formatdate
from glob import glob
from xml.etree.ElementTree import ElementTree

from java.io import File
from java.lang import Runtime
from java.util import LinkedHashMap
from java.util.logging import Level
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.mime.audio import MIMEAudio
from email.mime.base import MIMEBase
from email.mime.image import MIMEImage
from email import encoders

from fr.ens.biologie.genomique.aozan import Common
from fr.ens.biologie.genomique.aozan import Globals
from fr.ens.biologie.genomique.aozan import Settings
from fr.ens.biologie.genomique.aozan import AozanException
from fr.ens.biologie.genomique.aozan.util import FileUtils
from fr.ens.biologie.genomique.aozan.illumina import RunInfo

from fr.ens.biologie.genomique.aozan.Settings import AOZAN_DEBUG_KEY
from fr.ens.biologie.genomique.aozan.Settings import SEND_MAIL_KEY
from fr.ens.biologie.genomique.aozan.Settings import SMTP_SERVER_KEY
from fr.ens.biologie.genomique.aozan.Settings import MAIL_ERROR_TO_KEY
from fr.ens.biologie.genomique.aozan.Settings import MAIL_FOOTER_KEY
from fr.ens.biologie.genomique.aozan.Settings import MAIL_FROM_KEY
from fr.ens.biologie.genomique.aozan.Settings import MAIL_HEADER_KEY
from fr.ens.biologie.genomique.aozan.Settings import MAIL_TO_KEY
from fr.ens.biologie.genomique.aozan.Settings import SYNC_STEP_KEY
from fr.ens.biologie.genomique.aozan.Settings import INDEX_HTML_TEMPLATE_KEY
from fr.ens.biologie.genomique.aozan.Settings import DEMUX_USE_HISEQ_OUTPUT_KEY
from fr.ens.biologie.genomique.aozan.Settings import REPORTS_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import AOZAN_LOG_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import AOZAN_VAR_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import FASTQ_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import TMP_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_SAMPLESHEETS_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_COMPRESSION_KEY
from fr.ens.biologie.genomique.aozan.Settings import BCL2FASTQ_USE_DOCKER_KEY
from fr.ens.biologie.genomique.aozan.Settings import QC_CONF_FASTQSCREEN_BLAST_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import QC_CONF_FASTQSCREEN_BLAST_ENABLE_KEY

from fr.ens.biologie.genomique.aozan.util import StringUtils

PRIORITY_FILE = 'runs.priority'
LOCK_MAX_ATTEMPTS = 100
LOCK_SLEEP_DURATION = 0.1

def load_prioritized_run_ids(conf):
    """Load the list of the prioritized run ids.

    Arguments:
        conf: configuration dictionary
    """
    return load_run_ids(conf[AOZAN_VAR_PATH_KEY] + '/' + PRIORITY_FILE)


def df(path):
    """Get the free space on a partition.

    Arguments:
        path: file on the partition
    """
    # s = os.statvfs('/')
    # return (s.f_bavail * s.f_frsize)
    if os.path.exists(path):
        return long(File(path).getFreeSpace())

    return 0L


def du(path):
    """Get the disk usage of a directory.

    Arguments:
        path: path of the directory
    """

    if not os.path.exists(path):
        return 0L

    cmd = 'du -b --max-depth=0 ' + path
    child_stdin, child_stdout = os.popen2(cmd)
    lines = child_stdout.readlines()
    child_stdin.close()
    child_stdout.close()

    return long(lines[0].split('\t')[0])


def is_conf_value_equals_true(settings_key, conf):
    """Check a property exists in configuration object and value equals 'true'

    Arguments:
        settings_key: key in configuration for the property
        conf: configuration dictionary
        return boolean
    """
    return is_conf_value_defined(settings_key, 'true', conf)


def is_conf_key_exists(settings_key, conf):
    """Check a property exists in configuration object

    Arguments:
        settings_key: key in configuration for the property
        conf: configuration dictionary
        return boolean
    """

    return is_conf_value_defined(settings_key, None, conf)


def is_dir_exists(settings_key, conf):
    """Check a directory path corresponding to a property in configuration exists

    Arguments:
        settings_key: key in configuration for the property
        conf: configuration dictionary
        return boolean
    """

    exist = is_conf_key_exists(settings_key, conf)

    if not exist:
        return False

    path = conf[settings_key].strip()
    return os.path.exists(path) and os.path.isdir(path)


def is_file_exists(settings_key, conf):
    """Check a file path corresponding to a property in configuration exists

    Arguments:
        settings_key: key in configuration for the property
        conf: configuration dictionary
        return boolean
    """
    exist = is_conf_key_exists(settings_key, conf)

    if not exist:
        return False

    path = conf[settings_key].strip()
    return os.path.exists(path) and os.path.isfile(path)


def is_conf_value_defined(settings_key, expected_value, conf):
    """Check a property exists in configuration object with a specific expected_value (if it's different None or empty

    Arguments:
        settings_key: key in configuration for the property
        expected_value: expected_value of property wanted
        conf: configuration dictionary
        return boolean
    """

    # Get value
    try:
        value = conf[settings_key]
    except KeyError:
        return False

    # Test if value is defined
    if value is None:
        return False

    # Trim value
    value = value.lower().strip()

    # Test if value is empty
    if len(value) == 0:
        return False

    # Test if value must be compared to expected_value
    if expected_value is None:
        return True

    # Trim and lower expected value
    expected_value = expected_value.lower().strip()

    return value == expected_value


def list_existing_files(path, files_array):
    """Return string with existing files from array

    Arguments:
        path: path to directory
        files_array: all files to check
    """

    s = ''
    for filename in files_array:
        if os.path.exists(path + '/' + filename):
            s = filename + ' ' + s

    if s == '':
        return None

    return s + ' '


def get_input_run_data_path(run_id, conf):
    """Return the path to input run data according to hiseq and synchronization step parameters

    Arguments:
        run_id: run id
        conf: configuration dictionary
    """

    path = None

    # Case: input run data in bcl path
    if sync_run.is_sync_step_enable(conf):
        path = conf[BCL_DATA_PATH_KEY]

    # Case without synchronization
    # Set a bcl path
    if is_conf_value_defined(SYNC_STEP_KEY, 'false', conf) and is_conf_value_defined(DEMUX_USE_HISEQ_OUTPUT_KEY,
                                                                                     'false', conf):
        path = conf[BCL_DATA_PATH_KEY]

    # Case without synchronization and use the hiseq outut path
    # Check if must use the direct output of the HiSeq
    if is_conf_value_defined(SYNC_STEP_KEY, 'false', conf) and is_conf_value_equals_true(DEMUX_USE_HISEQ_OUTPUT_KEY,
                                                                                         conf):
        # Retrieve the path of run data directory on HiSeq
        path = hiseq_run.find_hiseq_run_path(run_id, conf)

    if path is None or path is False or not os.path.exists(path):
        error("Sequencer data directory does not exists.", "Sequencer data data directory does not exists.",
              get_last_error_file(conf), conf)
        return None

    return path + '/' + run_id


def send_msg(subject, message, is_error, conf):
    """Send a message to the user about the data extraction.

    Arguments:
        subject: subject of message
        message: text mail
        is_error: true if it is a error message
        conf: configuration object
    """

    send_mail = is_conf_value_equals_true(SEND_MAIL_KEY, conf)
    smtp_server = conf[SMTP_SERVER_KEY]

    # Specific receiver for error message
    if is_error:
        mail_to = conf[MAIL_ERROR_TO_KEY]

        # Mail error not define
        if mail_to is None or mail_to == '':
            mail_to = conf[MAIL_TO_KEY]
    else:
        mail_to = conf[MAIL_TO_KEY]

    mail_from = conf[MAIL_FROM_KEY]
    mail_cc = None
    mail_bcc = None

    if mail_to is not None:
        if type(mail_to) == str or type(mail_to) == unicode:
            mail_to = [mail_to]

    if mail_cc is not None:
        if type(mail_cc) == str or type(mail_cc) == unicode:
            mail_cc = [mail_cc]

    if mail_bcc is not None:
        if type(mail_bcc) == str or type(mail_bcc) == unicode:
            mail_bcc = [mail_bcc]

    # Create object msg
    msg = create_msg(mail_from, mail_to, mail_cc, mail_bcc, subject, message, conf)

    # Now send or store the message
    composed = msg.as_string()

    if send_mail:
        server = smtplib.SMTP(smtp_server)
        dests = []
        dests.extend(mail_to)
        if mail_cc is not None:
            dests.extend(mail_cc)
        if mail_bcc is not None:
            dests.extend(mail_bcc)
        server.sendmail(mail_from, dests, composed)
        server.quit()
    else:
        print '-------------'
        print composed
        print '-------------'


def send_msg_with_attachment(subject, message, attachment_file, conf):
    """Send a message to the user about the data extraction."""

    send_mail = is_conf_value_equals_true(SEND_MAIL_KEY, conf)
    smtp_server = conf[SMTP_SERVER_KEY]
    mail_to = conf[MAIL_TO_KEY]
    mail_from = conf[MAIL_FROM_KEY]
    mail_cc = None
    mail_bcc = None

    if mail_to is not None:
        if type(mail_to) == str or type(mail_to) == unicode:
            mail_to = [mail_to]

    if mail_cc is not None:
        if type(mail_cc) == str or type(mail_cc) == unicode:
            mail_cc = [mail_cc]

    if mail_bcc is not None:
        if type(mail_bcc) == str or type(mail_bcc) == unicode:
            mail_bcc = [mail_bcc]

    # Create object msg
    msg = create_msg(mail_from, mail_to, mail_cc, mail_bcc, subject, message, conf)

    ctype, encoding = mimetypes.guess_type(attachment_file)

    if ctype is None or encoding is not None:
        # No guess could be made, or the file is encoded (compressed), so
        # use a generic bag-of-bits type.
        ctype = 'application/octet-stream'

    maintype, subtype = ctype.split('/', 1)
    if maintype == 'text':
        fp = open(attachment_file, 'r')
        # Note: we should handle calculating the charset
        part2 = MIMEText(fp.read(), _subtype=subtype)
        fp.close()
    elif maintype == 'image':
        fp = open(attachment_file, 'rb')
        part2 = MIMEImage(fp.read(), _subtype=subtype)
        fp.close()
    elif maintype == 'audio':
        fp = open(attachment_file, 'rb')
        part2 = MIMEAudio(fp.read(), _subtype=subtype)
        fp.close()
    else:
        fp = open(attachment_file, 'rb')
        part2 = MIMEBase(maintype, subtype)
        part2.set_payload(fp.read())
        fp.close()
        # Encode the payload using Base64
        encoders.encode_base64(part2)

    # Set the filename parameter
    part2.add_header('Content-Disposition', 'attachment', filename=os.path.basename(attachment_file))
    msg.attach(part2)

    # Now send or store the message
    composed = msg.as_string()

    if send_mail:
        server = smtplib.SMTP(smtp_server)
        dests = []
        dests.extend(mail_to)
        if mail_cc is not None:
            dests.extend(mail_cc)
        if mail_bcc is not None:
            dests.extend(mail_bcc)
        server.sendmail(mail_from, dests, composed)
        server.quit()
    else:
        print '-------------'
        print composed
        print '-------------'


def create_msg(mail_from, mail_to, mail_cc, mail_bcc, subject, message, conf):
    """Create a message to send.

    Arguments:
        mail_from : senders
        mail_to : receivers
        mail_cc : other receivers
        mail_bcc : masked receivers
        subject: subject of message
        message: text mail
        conf: configuration object
    """

    COMMASPACE = ', '
    message = conf[MAIL_HEADER_KEY].replace('\\n', '\n') + message + conf[MAIL_FOOTER_KEY].replace('\\n', '\n')
    message = message.replace('\n', '\r\n')
    message = unicode(message)

    msg = MIMEMultipart()
    msg['From'] = mail_from

    if mail_to is not None:
        msg['To'] = COMMASPACE.join(mail_to)

    if mail_cc is not None:
        msg['Cc'] = COMMASPACE.join(mail_cc)

    if mail_bcc is not None:
        msg['Bcc'] = COMMASPACE.join(mail_bcc)

    msg['Subject'] = subject
    msg['Date'] = formatdate()

    # Not seen
    msg.preamble = message.encode('ascii', 'ignore')

    # The message
    part1 = MIMEText(message, 'plain', 'utf-8')
    msg.attach(part1)

    return msg


def get_last_error_file(conf):
    """Return path to the file which saves last error throws in common operation.

    Arguments:
        conf: configuration dictionary
    """

    if is_dir_exists(AOZAN_VAR_PATH_KEY, conf):
        return conf[AOZAN_VAR_PATH_KEY].strip() + '/aozan.lasterr'

    return False


def error(short_message, message, last_error_file_path, conf):
    """Error handling.

    Arguments:
        short_message: short description of the message
        message: message
        conf: configuration dictionary
    """

    # No write in last error file, directory does not exists
    if last_error_file_path is False:
        return

    new_error = ''
    if short_message is not None:
        new_error = short_message
    if message is not None:
        new_error += ' ' + message

    if len(new_error) == 0:
        new_error = 'Error without description'

    new_error.replace('\n', ' ')
    log('SEVERE', new_error.replace('\n', ' '), conf)

    if os.path.exists(last_error_file_path):
        f = open(last_error_file_path, 'r')

        # Extract content file and convert list in string
        last_error = ''.join(f.readlines())
        f.close()

        # Comparison text error
        if not new_error == last_error:
            # Send mail if new error
            send_msg(short_message, message, True, conf)
    else:
        send_msg(short_message, message, True, conf)

    f = open(last_error_file_path, 'w')
    f.write(new_error)
    f.close()


def log(level, message, conf):
    """Log message.

    Arguments:
        level: log level
        message: message to log
        conf: configuration dictionary
    """

    logger = Common.getLogger()
    logger.log(Level.parse(level), message)


def exception_msg(exp, conf):
    """Create error message when an AozanException is thrown.

    Arguments:
        exp: exception
        conf: configuration dictionary
    """

    if is_conf_value_equals_true(AOZAN_DEBUG_KEY, conf):
        return str(exp.getClass().getName()) + ': ' + str(exp.getMessage()) + '\n' + StringUtils.stackTraceToString(exp)
    else:
        return str(exp.getMessage())


def duration_to_human_readable(ms_since_epoch):
    """Convert a number of seconds in human readable string.

    Arguments:
        ms_since_epoch: the number of seconds since epoch
    """

    hours = int(ms_since_epoch / 3600)
    hours_rest = ms_since_epoch % 3600
    minutes = int(hours_rest / 60)
    minutes_rest = ms_since_epoch % 60
    seconds = int(minutes_rest)

    return "%02d:%02d:%02d" % (hours, minutes, seconds)


def time_to_human_readable(ms_since_epoch):
    """Convert a number of seconds since epoch in a human readable string.

    Arguments:
        ms_since_epoch: the number of seconds
    """

    return time.strftime("%a %b %d %H:%M:%S %Z %Y", time.localtime(ms_since_epoch))


def load_run_ids(file_path):
    """Load a list of run ids.

    Arguments:
        file_path: file path
    """

    result = set()

    if not os.path.exists(file_path):
        return result

    f = open(file_path, 'r')

    for l in f:
        run_id = l[:-1]
        if len(run_id) == 0:
            continue
        # Extract first field
        result.add(run_id.strip())

    f.close()

    return result


def add_run_id(run_id, file_path, conf):
    """Add a run id to a list of the run ids.

    Arguments:
        run_id: The run id
        file_path: path of the file
        conf: configuration dictionary
    """

    f = open(file_path, 'a')
    log('INFO','Add ' + run_id + ' on ' + get_instrument_name(run_id, conf) + ' to ' + os.path.basename(file_path), conf)
    attempt = 0
    while attempt <= LOCK_MAX_ATTEMPTS:
        try:
            fcntl.flock(f, fcntl.LOCK_EX | fcntl.LOCK_NB)
            f.write(run_id + '\n')
            f.close()
            fcntl.flock(f, fcntl.LOCK_UN)
            return
        except IOError as e:
            # raise on unrelated IOErrors
            if e.errno != errno.EAGAIN:
                raise Exception("Can't write " + run_id + " to " + file_path + ".")
            else:
                attemps = attemps + 1
                time.sleep(LOCK_SLEEP_DURATION)
    raise Exception("Lock timeout: Can't write " + run_id + " to " + file_path + ".")
        

def get_report_run_data_path(run_id, conf):
    """ Build report run data path from run id

    Arguments:
        run_id: The run id
        conf: configuration dictionary
    Return:
        path to report_run_data
    """

    return conf[REPORTS_DATA_PATH_KEY] + '/' + run_id


def get_run_parameters_path(run_id, conf):
    """ Get path to run parameters related to the run_id. With HiSeq name is
           runParameters.xml, with NextSeq name is RunParameters.xml.
    Arguments:
        run_id: run id
        conf: configuration dictionary

    Returns:
        path to the run parameters file related to the run_id
    """

    run_dir_path = hiseq_run.find_hiseq_run_path(run_id, conf)

    if run_dir_path is False:
        run_dir_path = conf[BCL_DATA_PATH_KEY]

    # Find file at the root of directory sequencer data
    res = glob(run_dir_path + '/' + run_id + "/*Parameters.xml")

    # Check result path
    for run_parameter_path in res:
        if os.path.basename(run_parameter_path).lower() == "runparameters.xml":
            return run_parameter_path

    raise Exception('Unable to find the run parameters for run ' + run_id)


def create_html_index_file(conf, run_id, sections):
    """Create an index.html file that contains links to all the generated reports.

    Arguments:
        conf: configuration dictionary
        run_id: The run id
        sections: The list of section to write, use step key from configuration
    """

    """ Since version RTA after 1.17, Illumina stop the generation of the Status and reports files"""

    report_path = get_report_run_data_path(run_id, conf)
    output_file_path = report_path + "/index.html"

    # Retrieve BufferedReader on index html template
    template_path = conf[INDEX_HTML_TEMPLATE_KEY]
    if template_path != None and template_path != '' and os.path.exists(template_path):
        f_in = open(template_path, 'r')
        text = ''.join(f_in.readlines())
        lines = text.split('\n');
        f_in.close()

    else:
        # Use default template save in aozan jar file
        jar_is = Globals.getResourceAsStream(Globals.INDEX_HTML_TEMPLATE_FILENAME)
        lines = FileUtils.readFileByLines(jar_is)

    # TODO check all path in session write in link tag and add final file if exist
    # NOT necessary to test step sync if Settings.SYNC_STEP_KEY in sections
    # and os.path.exists(report_path + '/report_' + run_id):
    if os.path.exists(report_path + '/report_' + run_id):
        sections.append('optional2')

        if os.path.exists(report_path + '/report_' + run_id + '/Status.htm'):
            sections.append('optional1')

    write_lines = True
    result = ''

    for line in lines:

        if line.startswith('<!--START_SECTION'):
            tokens = line.split(' ')
            #  Extract step name in start section
            section_name = tokens[1]
            # Extraction version
            version = tokens[2]

            write_lines = is_section_to_add_in_report(sections, section_name,
                                                      version, run_id, conf)

        elif line.startswith('<!--END_SECTION'):
            write_lines = True

        elif write_lines is True:
            if '${RUN_ID}' in line:
                result += line.replace('${RUN_ID}', run_id) + '\n'
            elif '${VERSION}' in line:
                result += line.replace('${VERSION}', Globals.APP_VERSION_STRING) + '\n'
            else:
                result += line + '\n'

    f_out = open(output_file_path, 'w')
    f_out.write(result)
    f_out.close()


def is_section_to_add_in_report(sections, section_name, version, run_id, conf):
    """
    Check if the section is required in report

    Arguments:
        sections: The list of section to write, use step key from configuration
        section_name: section name checked
        run_id: The run id
        conf: configuration dictionary

    True if test pass
    """

    # Check the current section name is included in sections required
    if section_name not in sections:
        return False

    # Check the section name related to a step name required or it is an optional section
    if not (is_conf_value_equals_true(section_name, conf) or section_name.startswith('optional')):
        return False

    # Check if version on the section
    if version == 'all':
        return True

    if version.startswith('rta'):
        rta_major_version = get_rta_major_version(run_id, conf)
        if version.endswith(str(rta_major_version)):
            return True
        else:
            return False

    # Section added in report html
    return True


def check_configuration(conf, configuration_file_path):
    """ Check if path useful exists

    Arguments:
        conf: configuration dictionary
        configuration_file_path: path of the configuration file

    Returns:
        True if the configuration is valid
    """

    steps_to_launch = extract_steps_to_launch(False, conf)

    msg = ''

    # # Path common on all steps

    # Check log path
    if not is_file_exists(AOZAN_LOG_PATH_KEY, conf):
        msg += '\n\t* Aozan log file path does not exists : ' + conf[AOZAN_LOG_PATH_KEY]

    # Check if temporary directory exists
    if not is_dir_exists(TMP_PATH_KEY, conf):
        msg += '\n\t* Temporary directory does not exists: ' + conf[TMP_PATH_KEY]

    if not is_dir_exists(REPORTS_DATA_PATH_KEY, conf):
        msg += '\n\t* Report run data directory does not exists: ' + conf[REPORTS_DATA_PATH_KEY]

    # # Step First_base_report and HiSeq
    if Settings.HISEQ_STEP_KEY in steps_to_launch:
        # Check if hiseq_data_path exists
        for hiseq_output_path in hiseq_run.get_hiseq_data_paths(conf):
            if not os.path.exists(hiseq_output_path):
                msg += '\n\t* Sequencer output directory does not exists: ' + hiseq_output_path

    # # For step SYNC
    if Settings.SYNC_STEP_KEY in steps_to_launch:
        # Check if bcl_data_path exists
        if not is_dir_exists(BCL_DATA_PATH_KEY, conf):
            msg += '\n\t* Basecalling directory does not exists: ' + conf[BCL_DATA_PATH_KEY]

    # # For step DEMUX
    if Settings.DEMUX_STEP_KEY in steps_to_launch:
        # Check if bcl2fastq samplesheet path exists
        if not is_dir_exists(BCL2FASTQ_SAMPLESHEETS_PATH_KEY, conf):
            msg += '\n\t* Bcl2fastq sample sheets directory does not exists: ' + conf[BCL2FASTQ_SAMPLESHEETS_PATH_KEY]

        # Check if root input fastq data directory exists
        if not is_dir_exists(FASTQ_DATA_PATH_KEY, conf):
            msg += '\n\t* Fastq data directory does not exists: ' + conf[FASTQ_DATA_PATH_KEY]

        # Check if bcl2fastq samplesheet path exists
        if is_conf_value_equals_true(BCL2FASTQ_USE_DOCKER_KEY, conf):
            pass
        else:
            if not is_dir_exists(BCL2FASTQ_PATH_KEY, conf):
                msg += '\n\t* bcl2fastq path directory does not exists: ' + conf[BCL2FASTQ_PATH_KEY]

        # Check compression type: three values None, gzip (default) bzip2
        if not is_fastq_compression_format_valid(conf):
            msg += '\n\t* Invalid FASTQ compression format: ' + conf[BCL2FASTQ_COMPRESSION_KEY]

    # # For step QC
    if Settings.QC_STEP_KEY in steps_to_launch:
        # Check path to blast if step enable
        if is_conf_value_equals_true(QC_CONF_FASTQSCREEN_BLAST_ENABLE_KEY, conf) and not is_file_exists(
                QC_CONF_FASTQSCREEN_BLAST_PATH_KEY, conf):
            msg += '\n\t* Blast enabled but blast file path does not exists: ' + conf[
                QC_CONF_FASTQSCREEN_BLAST_PATH_KEY]

    if len(msg) > 0:
        msg = 'Error(s) found in Aozan configuration file (' + os.path.abspath(configuration_file_path) + '):' + msg
        error("[Aozan] check configuration: error(s) in configuration file.", msg, get_last_error_file(conf), conf)
        return False

    return True


def extract_steps_to_launch(update_logger, conf):
    """ List steps to launch

    Arguments:
        conf: configuration dictionary
        update_logger: boolean to add data in logger

    Returns:
        list steps name
    """

    steps = []
    for key in [Settings.FIRST_BASE_REPORT_STEP_KEY, Settings.HISEQ_STEP_KEY, Settings.SYNC_STEP_KEY,
                Settings.DEMUX_STEP_KEY, Settings.QC_STEP_KEY]:

        if is_conf_value_equals_true(key, conf):
            steps.append(key)

    # Add data in logger
    if update_logger:
        txt = ", ".join(steps)
        log("CONFIG", "Aozan steps enabled: " + txt, conf)

    # Return list steps setting
    return steps


def is_fastq_compression_format_valid(conf):
    """ Check compression format fastq for bcl2fastq
        Three possible : None, gzip, bzip2, other exist aozan

    Arguments:
        conf: configuration dictionary

    Returns:
        True if the FASTQ compression format is valid
    """

    # Get the compression format defined by user
    if not is_conf_key_exists(BCL2FASTQ_COMPRESSION_KEY, conf):
        compression = 'none'
    else:
        compression = conf[BCL2FASTQ_COMPRESSION_KEY].lower().strip()

    # Check for compression alias
    if len(compression) == 0:
        compression = 'none'
    elif compression == 'gz' or compression == '.gz':
        compression = 'gzip'
    elif compression == 'bz2' or compression == '.bz2':
        compression = 'bzip2'

    conf[BCL2FASTQ_COMPRESSION_KEY] = compression

    # Check if compression format is allowed
    if compression == 'none' or compression == 'gzip' or compression == 'bzip2':
        return True

    return False


def get_rta_major_version(run_id, conf):
    """ Identify the RTA version used for the run from runParameter.xml file, from the version of RTA software
    Arguments:
        run_id: run id
        conf: configuration dictionary

    Returns:
        1 or 2
    Raises:
        Exception if the version of RTA is unknown/unsupported
    """

    if run_id is None:
        return None

    # Extract RTA version
    rtaversion = extract_rtaversion(run_id, conf)

    if rtaversion is not None:

        if rtaversion.startswith("1."):
            return 1

        if rtaversion.startswith("2."):
            return 2

    # TODO throw exception
    raise Exception('Unknown RTA version (' + rtaversion + ') for run ' + run_id)


def extract_rtaversion(run_id, conf):
    """ Extract RTA version from runParameter.xml file
    Arguments:
        run_id: run id
        conf: configuration dictionary

    Returns:
        RTA version of the run or None
    """
    # Find file at the root of directory sequencer data
    run_parameter_file = get_run_parameters_path(run_id, conf)

    if run_parameter_file is not None:
        # Extract element
        rtaversion_element = extract_elements_by_tag_name_from_xml(run_parameter_file, "RTAVersion")

        # Extract version
        if len(rtaversion_element) == 0:
            raise Exception('Unable to get RTA version in ' + run_parameter_file + ' for run ' + run_id)

        # Extract major element version
        return rtaversion_element[0].text.strip()

    raise Exception('Unable to locate run parameter file to get RTA version for run ' + run_id)


def get_instrument_name(run_id, conf):
    """ Identify the sequencer name
    Arguments:
        run_id: run id
        conf: configuration dictionary

    Return:
        A string with the instrument name
    """

    instrument_serial_number = run_id.split('_')[1]
    key = 'sequencer.name.' + instrument_serial_number

    if key in conf:
        return conf[key]
    else:
        return get_instrument_model(run_id, conf)


def get_instrument_model(run_id, conf):
    """ Identify the sequencer model from runParameter.xml file
    Arguments:
        run_id: run id
        conf: configuration dictionary

    Return:
        A string with the instrument name
    """

    run_parameter_file = get_run_parameters_path(run_id, conf)

    tree = ElementTree()
    tree.parse(run_parameter_file)

    application_tags = list(tree.iter('ApplicationName'))
    scanner_tags = list(tree.iter('ScannerID'))
    instrument_tags = list(tree.iter('InstrumentID'))

    if len(application_tags) == 0:
        return 'Unknown instrument'

    sequencer_model = application_tags[0].text.split(' ')[0]

    if len(scanner_tags) > 0:
        sequencer_model += ' ' + scanner_tags[0].text
    elif len(instrument_tags) > 0:
        sequencer_model += ' ' + instrument_tags[0].text

    return sequencer_model




def extract_elements_by_tag_name_from_xml(xml_path, tag_name):
    """ Extract an element from xml file path by tag name
    Arguments:
        xml_path: path to the xml file
        tag_name: tag name on element extracted

    Returns:
        list elements founded
    """

    # Check file exits and tag name not empty
    if os.path.exists(xml_path) and os.path.isfile(xml_path) and len(tag_name) > 0:
        # Read file
        tree = ElementTree()
        tree.parse(xml_path)

        # Find tag from name
        res = tree.findall(tag_name)

        # Check result found
        if len(res) == 0:
            # Search in children nodes
            res = tree.findall('*/' + tag_name)

        # Return result
        return res

    # Default return empty list
    return []


def print_default_configuration():
    """ Print default parameter in configuration.

    """
    conf = LinkedHashMap()

    # Load default configuration
    set_default_conf(conf)

    # Convert in test
    return '\n'.join(str(x + '=' + conf[x]) for x in conf)


def load_conf(conf, conf_file_path):
    """Load configuration file"""

    # Converting table between old and new keys
    converting_table_key = {}
    converting_table_key['casava.design.format'] = Settings.BCL2FASTQ_SAMPLESHEET_FORMAT_KEY
    converting_table_key['casava.samplesheet.format'] = Settings.BCL2FASTQ_SAMPLESHEET_FORMAT_KEY
    converting_table_key['casava.design.prefix.filename'] = Settings.BCL2FASTQ_SAMPLESHEET_PREFIX_FILENAME_KEY
    converting_table_key['casava.samplesheet.prefix.filename'] = Settings.BCL2FASTQ_SAMPLESHEET_PREFIX_FILENAME_KEY
    converting_table_key['casava.designs.path'] = Settings.BCL2FASTQ_SAMPLESHEETS_PATH_KEY
    converting_table_key['casava.samplesheets.path'] = Settings.BCL2FASTQ_SAMPLESHEETS_PATH_KEY

    converting_table_key['casava.adapter.fasta.file.path'] = Settings. BCL2FASTQ_ADAPTER_FASTA_FILE_PATH_KEY
    converting_table_key['casava.additionnal.arguments'] = Settings.BCL2FASTQ_ADDITIONNAL_ARGUMENTS_KEY
    converting_table_key['casava.compression'] = Settings.BCL2FASTQ_COMPRESSION_KEY
    converting_table_key['casava.compression.level'] = Settings.BCL2FASTQ_COMPRESSION_LEVEL_KEY
    converting_table_key['casava.fastq.cluster.count'] = Settings.BCL2FASTQ_FASTQ_CLUSTER_COUNT_KEY
    converting_table_key['casava.mismatches'] = Settings.BCL2FASTQ_MISMATCHES_KEY
    converting_table_key['casava.path'] = Settings.BCL2FASTQ_PATH_KEY

    converting_table_key['casava.threads'] = Settings.BCL2FASTQ_THREADS_KEY
    converting_table_key['casava.with.failed.reads'] = Settings.BCL2FASTQ_WITH_FAILED_READS_KEY
    converting_table_key['casava.design.generator.command'] = Settings.BCL2FASTQ_SAMPLESHEET_GENERATOR_COMMAND_KEY
    converting_table_key['casava.samplesheet.generator.command'] = Settings.BCL2FASTQ_SAMPLESHEET_GENERATOR_COMMAND_KEY
    converting_table_key['demux.use.docker.enable'] = Settings.BCL2FASTQ_USE_DOCKER_KEY

    converting_table_key['qc.conf.fastqc.threads'] = Settings.QC_CONF_THREADS_KEY
    converting_table_key['qc.conf.blast.arguments'] = Settings.QC_CONF_FASTQSCREEN_BLAST_ARGUMENTS_KEY
    converting_table_key['qc.conf.blast.db.path'] = Settings.QC_CONF_FASTQSCREEN_BLAST_DB_PATH_KEY
    converting_table_key['qc.conf.blast.path'] = Settings.QC_CONF_FASTQSCREEN_BLAST_PATH_KEY
    converting_table_key['qc.conf.blast.version.expected'] = Settings.QC_CONF_FASTQSCREEN_BLAST_VERSION_EXPECTED_KEY
    converting_table_key['qc.conf.step.blast.enable'] = Settings.QC_CONF_FASTQSCREEN_BLAST_ENABLE_KEY

    converting_table_key['qc.conf.ignore.paired.mode'] = Settings.QC_CONF_FASTQSCREEN_MAPPING_IGNORE_PAIRED_MODE_KEY
    converting_table_key['qc.conf.max.reads.parsed'] = Settings.QC_CONF_FASTQSCREEN_FASTQ_MAX_READS_PARSED_KEY
    converting_table_key['qc.conf.reads.pf.used'] = Settings.QC_CONF_FASTQSCREEN_FASTQ_READS_PF_USED_KEY
    converting_table_key['qc.conf.skip.control.lane'] = Settings.QC_CONF_FASTQSCREEN_MAPPING_SKIP_CONTROL_LANE_KEY

    converting_table_key['qc.conf.genome.alias.path'] = Settings.QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_ALIAS_PATH_KEY
    converting_table_key['qc.conf.settings.genomes'] = Settings.QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_KEY
    converting_table_key[
        'qc.conf.settings.genomes.desc.path'] = Settings.QC_CONF_FASTQSCREEN_SETTINGS_GENOMES_DESC_PATH_KEY
    converting_table_key[
        'qc.conf.settings.mappers.indexes.path'] = Settings.QC_CONF_FASTQSCREEN_SETTINGS_MAPPERS_INDEXES_PATH_KEY


    # Converting table between old and new test names
    test_name_converting_table = {}
    test_name_converting_table['globalpercentq30'] = 'global.globalpercentq30'
    test_name_converting_table['globalrawclustersmean'] = 'global.globalrawclustersmean'
    test_name_converting_table['globalrawclusterscount'] = 'global.globalrawclusterscount'
    test_name_converting_table['globalsamplescount'] = 'global.globalsamplescount'
    test_name_converting_table['globalbasecount'] = 'global.globalbasecount'
    test_name_converting_table['globalrawclustersphixsd'] = 'global.globalrawclustersphixsd'
    test_name_converting_table['globalpfclustersmediane'] = 'global.globalpfclustersmediane'
    test_name_converting_table['globalrawclustersphixmean'] = 'global.globalrawclustersphixmean'
    test_name_converting_table['globalprcpfclusters'] = 'global.globalprcpfclusters'
    test_name_converting_table['globalerrorrate'] = 'global.globalerrorrate'
    test_name_converting_table['globalprojectscount'] = 'global.globalprojectscount'
    test_name_converting_table['globalpfclusterssd'] = 'global.globalpfclusterssd'
    test_name_converting_table['globalprcundeterminedcluster'] = 'global.globalprcundeterminedcluster'
    test_name_converting_table['globaldensitycluster'] = 'global.globaldensitycluster'
    test_name_converting_table['globalrawclustersphix'] = 'global.globalrawclustersphix'
    test_name_converting_table['globalrawclustersmediane'] = 'global.globalrawclustersmediane'
    test_name_converting_table['globalpercentalignal'] = 'global.globalpercentalignal'
    test_name_converting_table['globalcyclescount'] = 'global.globalcyclescount'
    test_name_converting_table['globalrawclusterssd'] = 'global.globalrawclusterssd'
    test_name_converting_table['globallanescount'] = 'global.globallanescount'
    test_name_converting_table['nonindexedglobalbasecount'] = 'global.nonindexedglobalbasecount'
    test_name_converting_table['globalrawclustersphixmediane'] = 'global.globalrawclustersphixmediane'
    test_name_converting_table['globalpfclustersmean'] = 'global.globalpfclustersmean'
    test_name_converting_table['globalpfclusterscount'] = 'global.globalpfclusterscount'
    test_name_converting_table['errorrate75cycle'] = 'lane.errorrate75cycle'
    test_name_converting_table['clusterdensity'] = 'lane.clusterdensity'
    test_name_converting_table['pfclusters'] = 'lane.pfclusters'
    test_name_converting_table['errorrate35cycle'] = 'lane.errorrate35cycle'
    test_name_converting_table['percentintensitycycle20'] = 'lane.percentintensitycycle20'
    test_name_converting_table['firstcycleintensity'] = 'lane.firstcycleintensity'
    test_name_converting_table['phasingprephasing'] = 'lane.phasingprephasing'
    test_name_converting_table['errorrate'] = 'lane.errorrate'
    test_name_converting_table['rawclusters'] = 'lane.rawclusters'
    test_name_converting_table['percentalign'] = 'lane.percentalign'
    test_name_converting_table['errorrate100cycle'] = 'lane.errorrate100cycle'
    test_name_converting_table['pfclusterspercent'] = 'lane.pfclusterspercent'
    test_name_converting_table['lanepercentq30'] = 'lane.lanepercentq30'
    test_name_converting_table['rawclusterphix'] = 'lane.rawclusterphix'
    test_name_converting_table['lanesrunproject'] = 'project.lanesrunproject'
    test_name_converting_table['rawclustermaxproject'] = 'project.rawclustermaxproject'
    test_name_converting_table['genomesproject'] = 'project.genomesproject'
    test_name_converting_table['recoverablerawclusterpercent'] = 'project.recoverablerawclusterpercent'
    test_name_converting_table['pfclustersumproject'] = 'project.pfclustersumproject'
    test_name_converting_table['rawclustersumproject'] = 'project.rawclustersumproject'
    test_name_converting_table['linkprojectreport'] = 'project.linkprojectreport'
    test_name_converting_table['recoverablepfclusterpercent'] = 'project.recoverablepfclusterpercent'
    test_name_converting_table['isindexedproject'] = 'project.isindexedproject'
    test_name_converting_table['samplesexceededcontaminationthreshold'] = 'project.samplesexceededcontaminationthreshold'
    test_name_converting_table['pfclusterminproject'] = 'project.pfclusterminproject'
    test_name_converting_table['rawclusterminproject'] = 'project.rawclusterminproject'
    test_name_converting_table['pfclustermaxproject'] = 'project.pfclustermaxproject'
    test_name_converting_table['samplecountproject'] = 'project.samplecountproject'
    test_name_converting_table['fsqmapped'] = 'sample.fsqmapped'
    test_name_converting_table['sequencelengthdistribution'] = 'sample.sequencelengthdistribution'
    test_name_converting_table['linkreport'] = 'sample.linkreport'
    test_name_converting_table['percentinlanesample'] = 'sample.percentinlanesample'
    test_name_converting_table['percentpfsample'] = 'sample.percentpfsample'
    test_name_converting_table['perbasequalityscores'] = 'sample.perbasequalityscores'
    test_name_converting_table['pertilesequencequality'] = 'sample.pertilesequencequality'
    test_name_converting_table['linkreportrecoverycluster'] = 'sample.linkreportrecoverycluster'
    test_name_converting_table['percentq30'] = 'sample.percentq30'
    test_name_converting_table['meanqualityscorepf'] = 'sample.meanqualityscorepf'
    test_name_converting_table['persequencequalityscores'] = 'sample.persequencequalityscores'
    test_name_converting_table['recoverablerawclusterssamples'] = 'sample.recoverablerawclusterssamples'
    test_name_converting_table['recoverablerawclusterssamplespercent'] = 'sample.recoverablerawclusterssamplespercent'
    test_name_converting_table['basicstats'] = 'sample.basicstats'
    test_name_converting_table['recoverablepfclusterssamplespercent'] = 'sample.recoverablepfclusterssamplespercent'
    test_name_converting_table['badtiles'] = 'sample.badtiles'
    test_name_converting_table['adaptercontent'] = 'sample.adaptercontent'
    test_name_converting_table['duplicationlevel'] = 'sample.duplicationlevel'
    test_name_converting_table['persequencegccontent'] = 'sample.persequencegccontent'
    test_name_converting_table['overrepresentedseqs'] = 'sample.overrepresentedseqs'
    test_name_converting_table['pfclusterssamples'] = 'sample.pfclusterssamples'
    test_name_converting_table['recoverablepfclusterssamples'] = 'sample.recoverablepfclusterssamples'
    test_name_converting_table['ncontent'] = 'sample.ncontent'
    test_name_converting_table['hitnolibraries'] = 'sample.hitnolibraries'
    test_name_converting_table['rawclusterssamples'] = 'sample.rawclusterssamples'
    test_name_converting_table['kmercontent'] = 'sample.kmercontent'
    test_name_converting_table['perbasesequencecontent'] = 'sample.perbasesequencecontent'
    test_name_converting_table['lanesrunsample'] = 'samplestats.lanesrunsample'
    test_name_converting_table['samplestatsfsqmapped'] = 'samplestats.samplestatsfsqmapped'
    test_name_converting_table['samplestatpfclustersum'] = 'samplestats.samplestatpfclustersum'
    test_name_converting_table['genomessample'] = 'samplestats.genomessample'
    test_name_converting_table['meanqualityscorepfsamplestats'] = 'samplestats.meanqualityscorepfsamplestats'
    test_name_converting_table['samplestatsrecoverablerawclusterpercent'] = 'samplestats.samplestatsrecoverablerawclusterpercent'
    test_name_converting_table['samplestatspercentq30basepf'] = 'samplestats.samplestatspercentq30basepf'
    test_name_converting_table['linksamplereport'] = 'samplestats.linksamplereport'
    test_name_converting_table['samplestathitnolibrariessum'] = 'samplestats.samplestathitnolibrariessum'
    test_name_converting_table['samplestatrawclustersum'] = 'samplestats.samplestatrawclustersum'
    test_name_converting_table['percentsampleinproject'] = 'samplestats.percentsampleinproject'
    test_name_converting_table['isindexedsample'] = 'samplestats.isindexedsample'
    test_name_converting_table['samplestatspassingfilterclusterpercent'] = 'samplestats.samplestatspassingfilterclusterpercent'
    test_name_converting_table['samplestatcountsample'] = 'samplestats.samplestatcountsample'
    test_name_converting_table['samplestatsrecoverablepfclusterpercent'] = 'samplestats.samplestatsrecoverablepfclusterpercent'
    test_name_converting_table['samplestatexceededcontaminationthreshold'] = 'samplestats.samplestatexceededcontaminationthreshold'
    test_name_converting_table['samplestatsrawclustermin'] = 'samplestats.samplestatsrawclustermin'
    test_name_converting_table['samplestatspfclustermax'] = 'samplestats.samplestatspfclustermax'
    test_name_converting_table['samplestatspfclustermin'] = 'samplestats.samplestatspfclustermin'
    test_name_converting_table['samplestatsrecoveryrawcluster'] = 'samplestats.samplestatsrecoveryrawcluster'
    test_name_converting_table['samplestatsrawclustermax'] = 'samplestats.samplestatsrawclustermax'
    test_name_converting_table['percentsampleinrun'] = 'samplestats.percentsampleinrun'
    test_name_converting_table['samplestatsrecoverypfcluster'] = 'samplestats.samplestatsrecoverypfcluster'

    f = open(conf_file_path, 'r')

    for l in f:
        s = l[:-1].strip()
        if len(s) == 0 or l[0] == '#':
            continue

        # Search other configuration file
        if s.startswith('include'):
            # Path to the other configuration file
            other_configuration_path = s.split('=')[1].strip()

            if len(other_configuration_path) == 0:
                continue

            # Check file exists
            if not (os.path.exists(other_configuration_path) or os.path.isfile(other_configuration_path)):
                sys.exit(1)

            # Check different path
            if other_configuration_path == conf_file_path:
                continue

            # Load other configuration file,
            load_conf(conf, other_configuration_path)

        else:
            fields = s.split('=')
            key = fields[0].strip()
            value = fields[1].strip()

            if len(fields) == 2:

                # Check if needed to converting key
                if key in converting_table_key:
                    key = converting_table_key[key]

                # Converting old test names to new ones
                if key.startswith('qc.test.'):
                    for k in test_name_converting_table.keys():
                        prefix = 'qc.test.' + k + '.'
                        if key.startswith(prefix):
                            key = 'qc.test.' + test_name_converting_table[k] + key[len(prefix)-1:]
                            break

                conf[key] = value

    f.close()

    # Save configuration file path
    conf[Settings.AOZAN_CONF_FILE_PATH] = conf_file_path

    # Save completed configuration file
    f = open(conf[TMP_PATH_KEY] + '/full_aozan.conf', 'w')
    f.write('\n'.join(str(x + '=' + str(conf[x])) for x in conf))
    f.flush()
    f.close()

    return conf


def set_default_conf(conf):
    # Global
    conf[Settings.AOZAN_ENABLE_KEY] = 'True'
    conf[Settings.SEND_MAIL_KEY] = 'False'
    conf[Settings.AOZAN_LOG_LEVEL_KEY] = str(Globals.LOG_LEVEL)
    conf[Settings.FIRST_BASE_REPORT_STEP_KEY] = 'True'
    conf[Settings.HISEQ_STEP_KEY] = 'True'
    conf[Settings.SYNC_STEP_KEY] = 'True'
    conf[Settings.DEMUX_STEP_KEY] = 'True'
    conf[Settings.QC_STEP_KEY] = 'False'

    # Lock file
    conf[Settings.LOCK_FILE_KEY] = '/var/lock/aozan.lock'

    # Synchronization
    conf[Settings.SYNC_EXCLUDE_CIF_KEY] = 'True'
    conf[Settings.SYNC_CONTINUOUS_SYNC_KEY] = 'False'
    conf[Settings.SYNC_CONTINUOUS_SYNC_MIN_AGE_FILES_KEY] = '15'

    # Bcl2fastq
    conf[Settings.DEMUX_USE_HISEQ_OUTPUT_KEY] = 'False'
    conf[Settings.BCL2FASTQ_SAMPLESHEET_FORMAT_KEY] = 'xls'
    conf[Settings.BCL2FASTQ_SAMPLESHEET_PREFIX_FILENAME_KEY] = 'samplesheet'
    conf[Settings.BCL2FASTQ_PATH_KEY] = '/usr/local/bcl2fastq'
    conf[Settings.BCL2FASTQ_COMPRESSION_KEY] = 'gzip'
    conf[Settings.BCL2FASTQ_FASTQ_CLUSTER_COUNT_KEY] = '0'
    conf[Settings.BCL2FASTQ_COMPRESSION_KEY] = '9'
    conf[Settings.BCL2FASTQ_MISMATCHES_KEY] = '0'
    conf[Settings.BCL2FASTQ_THREADS_KEY] = str(Runtime.getRuntime().availableProcessors())
    conf[Settings.BCL2FASTQ_ADAPTER_FASTA_FILE_PATH_KEY] = ''
    conf[Settings.BCL2FASTQ_WITH_FAILED_READS_KEY] = 'True'
    conf[Settings.BCL2FASTQ_ADDITIONNAL_ARGUMENTS_KEY] = ''

    # New options since Aozan version 2.0 and managment of NextSeq
    conf[Settings.BCL2FASTQ_USE_DOCKER_KEY] = 'false'

    # Data path
    conf[Settings.TMP_PATH_KEY] = '/tmp'
    conf[Settings.INDEX_SEQUENCES_KEY] = ''
    conf[Settings.INDEX_HTML_TEMPLATE_KEY] = ''
    conf[Settings.REPORTS_URL_KEY] = ''
    conf[Settings.QC_REPORT_STYLESHEET_KEY] = ''

    # Space needed
    conf[Settings.HISEQ_WARNING_MIN_SPACE_KEY] = str(3 * 1024 * 1024 * 1024 * 1024)
    conf[Settings.HISEQ_CRITICAL_MIN_SPACE_KEY] = str(1 * 1024 * 1024 * 1024 * 1024)
    conf[Settings.SYNC_SPACE_FACTOR_KEY] = str(0.2)
    conf[Settings.DEMUX_SPACE_FACTOR_KEY] = str(0.7)
    # Value for step estimated space needed during first base report
    # estimation factor for fastq_space_per_lane_per_cycle (cmd du -b)
    conf[Settings.FASTQ_SPACE_FACTOR_KEY] = str(224000000)
    # estimation factor for bcl_space_per_lane_per_cycle (cmd du -b)
    conf[Settings.BCL_SPACE_FACTOR_KEY] = str(416000000)
    # estimation factor for hiseq_space_per_lane_per_cycle (cmd du -b)
    conf[Settings.HISEQ_SPACE_FACTOR_KEY] = str(3180000000)

    # Mail configuration
    conf[Settings.MAIL_HEADER_KEY] = 'THIS IS AN AUTOMATED MESSAGE.\\n\\n'
    conf[Settings.MAIL_FOOTER_KEY] = '\\n\\nThe Aozan team.\\n'

    # Collectors configuration
    # ReadCollector
    conf[Settings.QC_CONF_READ_XML_COLLECTOR_USED_KEY] = 'false'
    # TileMetricsCollector
    conf[Settings.QC_CONF_CLUSTER_DENSITY_RATIO_KEY] = str(0.3472222)
    # TemporaryPartialFastqCollector
    conf[Settings.QC_CONF_FASTQSCREEN_FASTQ_READS_PF_USED_KEY] = str(200000)
    # Use only the first X reads pf in fastq file
    conf[Settings.QC_CONF_FASTQSCREEN_FASTQ_MAX_READS_PARSED_KEY] = str(30000000)
    # Configuration FastqscreenCollector, parameters for mapping
    # no detection contamination for control lane
    conf[Settings.QC_CONF_FASTQSCREEN_MAPPING_SKIP_CONTROL_LANE_KEY] = 'true'
    # run paired : no paired mapping
    conf[Settings.QC_CONF_FASTQSCREEN_MAPPING_IGNORE_PAIRED_MODE_KEY] = 'true'
    # Default mapper is bowtie
    conf[Settings.QC_CONF_FASTQSCREEN_MAPPER_KEY] = 'bowtie'
