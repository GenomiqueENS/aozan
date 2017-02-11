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
This script contains all common functions in all Aozan python scripts
'''


import smtplib
import os.path
import time
import sys
import os
import stat
import mimetypes
from email.utils import formatdate
from glob import glob
from xml.etree.ElementTree import ElementTree
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.mime.audio import MIMEAudio
from email.mime.base import MIMEBase
from email.mime.image import MIMEImage
from email import encoders
from pipes import quote

import hiseq_run
import sync_run

from java.io import File
from java.lang import Runtime
from java.util import LinkedHashMap
from java.util.logging import Level

from fr.ens.biologie.genomique.aozan import Common
from fr.ens.biologie.genomique.aozan import Globals
from fr.ens.biologie.genomique.aozan import Settings
from fr.ens.biologie.genomique.aozan import AozanException
from fr.ens.biologie.genomique.aozan.util import FileUtils
from fr.ens.biologie.genomique.aozan.illumina import RunInfo
from java.nio.channels import FileChannel
from java.nio.channels import FileLock
from java.io import File;
from java.io import RandomAccessFile;

from fr.ens.biologie.genomique.aozan.Settings import AOZAN_DEBUG_KEY
from fr.ens.biologie.genomique.aozan.Settings import SEND_MAIL_KEY
from fr.ens.biologie.genomique.aozan.Settings import SMTP_SERVER_KEY
from fr.ens.biologie.genomique.aozan.Settings import SMTP_PORT_KEY
from fr.ens.biologie.genomique.aozan.Settings import SMTP_USE_STARTTLS_KEY
from fr.ens.biologie.genomique.aozan.Settings import SMTP_USE_SSL_KEY
from fr.ens.biologie.genomique.aozan.Settings import SMTP_LOGIN_KEY
from fr.ens.biologie.genomique.aozan.Settings import SMTP_PASSWORD_KEY
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
from fr.ens.biologie.genomique.aozan.Settings import QC_CONF_FASTQC_BLAST_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import QC_CONF_FASTQC_BLAST_ENABLE_KEY
from fr.ens.biologie.genomique.aozan.Settings import QC_CONF_FASTQC_BLAST_DB_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import HISEQ_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import READ_ONLY_OUTPUT_FILES_KEY
from fr.ens.biologie.genomique.aozan.util import StringUtils

PRIORITY_FILE = 'runs.priority'



FIRST_BASE_DONE_FILE = 'first_base_report.done'
FIRST_BASE_REPORT_FILE = 'First_Base_Report.htm'

HISEQ_DENY_FILE = 'hiseq.deny'
HISEQ_DONE_FILE = 'hiseq.done'

SYNC_DENY_FILE = 'sync.deny'
SYNC_DONE_FILE = 'sync.done'
SYNC_LASTERR_FILE = 'sync.lasterr'

DEMUX_DENY_FILE = 'demux.deny'
DEMUX_DONE_FILE = 'demux.done'
DEMUX_LASTERR_FILE = 'demux.lasterr'

RECOMPRESS_DENY_FILE = 'recompress.deny'
RECOMPRESS_DONE_FILE = 'recompress.done'
RECOMPRESS_LASTERR_FILE = 'recompress.lasterr'

QC_DENY_FILE = 'qc.deny'
QC_DONE_FILE = 'qc.done'
QC_LASTERR_FILE = 'qc.lasterr'

BCL2FASTQ2_VERSION = "2.18.0.12"
MAX_DELAY_TO_SEND_TERMINATED_RUN_EMAIL = 12 * 3600

def exists_in_path(program):
    """check if a program exists in PATH environnement variable

    Arguments:
        program: program name
    """
    path_env_var = os.environ['PATH']
    for path in path_env_var.split(':'):
        if os.path.exists(path + "/" + program):
            return True
    return False

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

    cmd = 'du -b --max-depth=0 ' + quote(path)
    child_stdin, child_stdout = os.popen2(cmd)
    lines = child_stdout.readlines()
    child_stdin.close()
    child_stdout.close()

    return long(lines[0].split('\t')[0])


def chmod(path, conf):
    """Change the rights of a file.

    Arguments:
        path: path of the file
        conf: Aozan configuration
    """

    if is_conf_value_equals_true(READ_ONLY_OUTPUT_FILES_KEY, conf):
        try:
            os.chmod(path, stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH)
        except OSError:
            return False

    return True


def chmod_files_in_dir(path, pattern, conf):
    """Change the rights of files in a directory.

    Arguments:
        path: path of the directory
        motif: file motif
        conf: Aozan configuration
    """

    if is_conf_value_equals_true(READ_ONLY_OUTPUT_FILES_KEY, conf):

        for root, dirs, files in os.walk(path):

            for f in files:
                if pattern is None or pattern == "" or pattern in f:
                    if not chmod(root + '/' + f, conf):
                        return False

    return True


def is_file_readable(path):
    """Test if a file is readable.

    Arguments:
        path: path of the directory
    Return:
        boolean: true if the file is readable
    """

    if not os.path.isfile(path):
        return False

    try:
        fp = open(path, 'r')
        fp.close()

    except:
        return False

    return True


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
        error("Sequencer data directory does not exist.", "Sequencer data data directory does not exist.",
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
        server = _connect_smtp_server(conf)
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


def send_msg_with_attachment(subject, message, attachment_file, is_error, conf):
    """Send a message to the user about the data extraction."""

    send_mail = is_conf_value_equals_true(SEND_MAIL_KEY, conf)
    mail_from = conf[MAIL_FROM_KEY]
    mail_cc = None
    mail_bcc = None

    # Specific receiver for error message
    if is_error:
        mail_to = conf[MAIL_ERROR_TO_KEY]

        # Mail error not define
        if mail_to is None or mail_to == '':
            mail_to = conf[MAIL_TO_KEY]
    else:
        mail_to = conf[MAIL_TO_KEY]

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
        server = _connect_smtp_server(conf)
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


def _connect_smtp_server(conf):
    """Configure the connection to the SMTP server.

    Arguments:
        conf: configuration object
    """

    smtp_server = conf[SMTP_SERVER_KEY]

    # Define default port
    if is_conf_value_equals_true(SMTP_USE_SSL_KEY, conf):
        smtp_port = 465
    else:
        smtp_port = 25

    # Change the port if required
    if is_conf_key_exists(SMTP_PORT_KEY, conf):
        smtp_port = int(conf[SMTP_PORT_KEY])

    # Connect to the server using SSL or not
    if is_conf_value_equals_true(SMTP_USE_SSL_KEY, conf):
        server = smtplib.SMTP_SSL(smtp_server, smtp_port)
    else:
        server = smtplib.SMTP(smtp_server, smtp_port)

    # Enable StartTLS
    if is_conf_value_equals_true(SMTP_USE_STARTTLS_KEY, conf):
        server.starttls()

    # Use a login and a password if required
    if is_conf_key_exists(SMTP_LOGIN_KEY, conf) and is_conf_key_exists(SMTP_PASSWORD_KEY, conf):
        server.login(conf[SMTP_LOGIN_KEY], conf[SMTP_PASSWORD_KEY])

    return server


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

    # No write in last error file, directory does not exist
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
    log('INFO', 'Adding ' + run_id + ' on ' + get_instrument_name(run_id, conf) + ' to ' + os.path.basename(file_path),
        conf)

    f = File(file_path);

    # Open file
    raf = RandomAccessFile(f, 'rws')

    # Creating lock
    channel = raf.getChannel()
    lock = channel.lock()

    # Set position at the end of the file
    raf.seek(f.length())

    # Add the run_id at the end of the file
    raf.writeBytes(run_id.strip() + '\n')

    # Release locks
    if lock:
        lock.release()

    # Close file
    channel.close()
    raf.close()


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
            result += line.replace('${RUN_ID}', run_id). \
                           replace('${APP_NAME}', Globals.APP_NAME). \
                           replace('${VERSION}', Globals.APP_VERSION_STRING). \
                           replace('${WEBSITE}', Globals.WEBSITE_URL) + '\n'

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


def _check_conf_key(conf, msg, key_name):

    if not key_name in conf:
        msg += '\n\t* Configuration setting is not set: ' + key_name
        return False

    return True

def _check_conf_dir(conf, msg, key_name, desc):

    if not _check_conf_key(conf, msg, key_name):
        return False

    if not is_dir_exists(key_name, conf):
        msg += '\n\t* ' + desc + ' directory does not exist: ' + conf[key_name]
        return False

    return True

def _check_conf_file(conf, msg, key_name, desc):

    if not _check_conf_key(conf, msg, key_name):
        return False

    if not is_file_exists(key_name, conf):
        msg += '\n\t* ' + desc + ' file does not exist: ' + conf[key_name]
        return False

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

    # Path common on all steps

    # Check log path
    _check_conf_file(conf, msg, AOZAN_LOG_PATH_KEY, 'Aozan log file')

    # Check if temporary directory exists
    _check_conf_dir(conf, msg, TMP_PATH_KEY, 'Temporary')

    # Check run data directory
    _check_conf_dir(conf, msg, REPORTS_DATA_PATH_KEY, 'Report run data')

    # Step First_base_report and HiSeq
    if Settings.HISEQ_STEP_KEY in steps_to_launch:

        if _check_conf_key(conf, msg, HISEQ_DATA_PATH_KEY):
            # Check if hiseq_data_path exists
            for hiseq_output_path in hiseq_run.get_hiseq_data_paths(conf):
                if not os.path.isdir(hiseq_output_path):
                    msg += '\n\t* Sequencer output directory does not exist: ' + hiseq_output_path

    # For step SYNC
    if Settings.SYNC_STEP_KEY in steps_to_launch:

        # Check if bcl_data_path exists
        _check_conf_dir(conf, msg, BCL_DATA_PATH_KEY, 'Basecalling')

    # For step DEMUX
    if Settings.DEMUX_STEP_KEY in steps_to_launch:

        # Check if bcl2fastq samplesheet path exists
        _check_conf_dir(conf, msg, BCL2FASTQ_SAMPLESHEETS_PATH_KEY, 'Bcl2fastq samplesheets')

        # Check if root input fastq data directory exists
        _check_conf_dir(conf, msg, FASTQ_DATA_PATH_KEY, 'Fastq data')

        # Check if bcl2fastq samplesheet path exists
        if is_conf_value_equals_true(BCL2FASTQ_USE_DOCKER_KEY, conf):
            pass
        else:
            _check_conf_file(conf, msg, BCL2FASTQ_PATH_KEY, 'bcl2fastq executable')

        # Check compression type: three values None, gzip (default) bzip2
        if not is_fastq_compression_format_valid(conf):
            msg += '\n\t* Invalid FASTQ compression format: ' + conf[BCL2FASTQ_COMPRESSION_KEY]

    # # For step QC
    if Settings.QC_STEP_KEY in steps_to_launch:
        # Check path to blast if step enable
        if is_conf_value_equals_true(QC_CONF_FASTQC_BLAST_ENABLE_KEY, conf):
            _check_conf_file(conf, msg, QC_CONF_FASTQC_BLAST_PATH_KEY, 'Blast executable')
            _check_conf_dir(conf, msg, QC_CONF_FASTQC_BLAST_DB_PATH_KEY, 'Blast database')


    if len(msg) > 0:
        msg = 'Error(s) found in Aozan configuration file (' + os.path.abspath(configuration_file_path) + '):' + msg
        error("[Aozan] Check configuration: error(s) in configuration file.", msg, get_last_error_file(conf), conf)
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
                Settings.DEMUX_STEP_KEY, Settings.RECOMPRESS_STEP_KEY, Settings.QC_STEP_KEY]:

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

    converting_table_key['casava.adapter.fasta.file.path'] = Settings.BCL2FASTQ_ADAPTER_FASTA_FILE_PATH_KEY
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

    converting_table_key['qc.conf.blast.arguments'] = Settings.QC_CONF_FASTQC_BLAST_ARGUMENTS_KEY
    converting_table_key['qc.conf.blast.db.path'] = Settings.QC_CONF_FASTQC_BLAST_DB_PATH_KEY
    converting_table_key['qc.conf.blast.path'] = Settings.QC_CONF_FASTQC_BLAST_PATH_KEY
    converting_table_key['qc.conf.step.blast.enable'] = Settings.QC_CONF_FASTQC_BLAST_ENABLE_KEY
    converting_table_key['qc.conf.fastqscreen.blast.arguments'] = Settings.QC_CONF_FASTQC_BLAST_ARGUMENTS_KEY
    converting_table_key['qc.conf.fastqscreen.blast.db.path'] = Settings.QC_CONF_FASTQC_BLAST_DB_PATH_KEY
    converting_table_key['qc.conf.fastqscreen.blast.path'] = Settings.QC_CONF_FASTQC_BLAST_PATH_KEY
    converting_table_key['qc.conf.fastqscreen.blast.enable'] = Settings.QC_CONF_FASTQC_BLAST_ENABLE_KEY

    converting_table_key['qc.conf.fastqscreen.settings.genomes'] = Settings.QC_CONF_FASTQSCREEN_GENOMES_PATH_KEY
    converting_table_key['qc.conf.fastqscreen.settings.genomes.alias.path'] = Settings.QC_CONF_FASTQSCREEN_GENOMES_ALIAS_PATH_KEY
    converting_table_key['qc.conf.fastqscreen.settings.genomes.desc.path'] = Settings.QC_CONF_FASTQSCREEN_GENOMES_DESC_PATH_KEY
    converting_table_key['qc.conf.fastqscreen.settings.mappers.indexes.path'] = Settings.QC_CONF_FASTQSCREEN_MAPPERS_INDEXES_PATH_KEY
    converting_table_key['qc.conf.fastqscreen.mapper.argument'] = Settings.QC_CONF_FASTQSCREEN_MAPPER_ARGUMENTS_KEY
    converting_table_key['qc.conf.fastqscreen.mapping.ignore.paired.mode'] = Settings.QC_CONF_FASTQSCREEN_MAPPING_IGNORE_PAIRED_END_MODE_KEY
    converting_table_key['qc.conf.fastqscreen.percent.contamination.threshold'] = Settings.QC_CONF_FASTQSCREEN_PERCENT_PROJECT_CONTAMINATION_THRESHOLD_KEY

    converting_table_key['qc.conf.ignore.paired.mode'] = Settings.QC_CONF_FASTQSCREEN_MAPPING_IGNORE_PAIRED_END_MODE_KEY
    converting_table_key['qc.conf.max.reads.parsed'] = Settings.QC_CONF_FASTQSCREEN_FASTQ_MAX_READS_PARSED_KEY
    converting_table_key['qc.conf.reads.pf.used'] = Settings.QC_CONF_FASTQSCREEN_FASTQ_READS_PF_USED_KEY
    converting_table_key['qc.conf.skip.control.lane'] = Settings.QC_CONF_FASTQSCREEN_MAPPING_SKIP_CONTROL_LANE_KEY

    converting_table_key['qc.conf.genome.alias.path'] = Settings.QC_CONF_FASTQSCREEN_GENOMES_ALIAS_PATH_KEY
    converting_table_key['qc.conf.settings.genomes'] = Settings.QC_CONF_FASTQSCREEN_GENOMES_PATH_KEY
    converting_table_key[
        'qc.conf.settings.genomes.desc.path'] = Settings.QC_CONF_FASTQSCREEN_GENOMES_DESC_PATH_KEY
    converting_table_key[
        'qc.conf.settings.mappers.indexes.path'] = Settings.QC_CONF_FASTQSCREEN_MAPPERS_INDEXES_PATH_KEY

    # Converting table between old and new test names
    test_name_converting_table = {}
    test_name_converting_table[ 'globalbasecount' ] = 'global.base.count'
    test_name_converting_table[ 'globalcyclescount' ] = 'global.cycle.count'
    test_name_converting_table[ 'globaldensitycluster' ] = 'global.cluster.density'
    test_name_converting_table[ 'globallanescount' ] = 'global.lane.count'
    test_name_converting_table[ 'nonindexedglobalbasecount' ] = 'global.non.indexed.base.count'
    test_name_converting_table[ 'globalpercentalignal' ] = 'global.phix.align.percent'
    test_name_converting_table[ 'globalerrorrate' ] = 'global.error.rate'
    test_name_converting_table[ 'globalpercentq30' ] = 'global.q30.percent'
    test_name_converting_table[ 'globalpfclusterscount' ] = 'global.pf.cluster.count'
    test_name_converting_table[ 'globalpfclustersmean' ] = 'global.mean.pf.cluster.count'
    test_name_converting_table[ 'globalpfclustersmediane' ] = 'global.median.pf.cluster.count'
    test_name_converting_table[ 'globalprcpfclusters' ] = 'global.pf.cluster.percent'
    test_name_converting_table[ 'globalpfclusterssd' ] = 'global.pf.cluster.sd'
    test_name_converting_table[ 'globalprojectscount' ] = 'global.project.count'
    test_name_converting_table[ 'globalrawclusterscount' ] = 'global.raw.cluster.count'
    test_name_converting_table[ 'globalrawclustersmean' ] = 'global.mean.raw.cluster.count'
    test_name_converting_table[ 'globalrawclustersmediane' ] = 'global.median.raw.cluster.count'
    test_name_converting_table[ 'globalrawclustersphix' ] = 'global.phix.raw.cluster.count'
    test_name_converting_table[ 'globalrawclustersphixmean' ] = 'global.mean.phix.raw.cluster.count'
    test_name_converting_table[ 'globalrawclustersphixmediane' ] = 'global.median.phix.raw.cluster.count'
    test_name_converting_table[ 'globalrawclustersphixsd' ] = 'global.phix.raw.cluster.sd'
    test_name_converting_table[ 'globalrawclusterssd' ] = 'global.raw.cluster.sd'
    test_name_converting_table[ 'globalsamplescount' ] = 'global.sample.count'
    test_name_converting_table[ 'globalprcundeterminedcluster' ] = 'global.undetermined.cluster.percent'
    test_name_converting_table[ 'clusterdensity' ] = 'lane.cluster.density'
    test_name_converting_table[ 'errorrate100cycle' ] = 'lane.100.cycle.error.rate'
    test_name_converting_table[ 'errorrate35cycle' ] = 'lane.35.cycle.error.rate'
    test_name_converting_table[ 'errorrate75cycle' ] = 'lane.75.cycle.error.rate'
    test_name_converting_table[ 'errorrate' ] = 'lane.error.rate'
    test_name_converting_table[ 'firstcycleintensity' ] = 'lane.first.cycle.intensity'
    test_name_converting_table[ 'percentalign' ] = 'lane.phix.align.percent'
    test_name_converting_table[ 'percentintensitycycle20' ] = 'lane.cycle.20.intensity.percent'
    test_name_converting_table[ 'lanepercentq30' ] = 'lane.q30.percent'
    test_name_converting_table[ 'pfclusters' ] = 'lane.pf.cluster.count'
    test_name_converting_table[ 'pfclusterspercent' ] = 'lane.pf.cluster.percent'
    test_name_converting_table[ 'phasingprephasing' ] = 'lane.phasing.prephasing.percent'
    test_name_converting_table[ 'rawclusters' ] = 'lane.raw.cluster.count'
    test_name_converting_table[ 'rawclusterphix' ] = 'lane.phix.raw.cluster.count'
    test_name_converting_table[ 'genomesproject' ] = 'project.genome.names'
    test_name_converting_table[ 'isindexedproject' ] = 'project.is.indexed'
    test_name_converting_table[ 'lanesrunproject' ] = 'project.lane.count'
    test_name_converting_table[ 'linkprojectreport' ] = 'project.fastqscreen.report'
    test_name_converting_table[ 'recoverablepfclusterpercent' ] = 'project.recoverable.pf.cluster.percent'
    test_name_converting_table[ 'recoverablerawclusterpercent' ] = 'project.recoverable.raw.cluster.percent'
    test_name_converting_table[ 'pfclustermaxproject' ] = 'project.max.pf.cluster.count'
    test_name_converting_table[ 'pfclusterminproject' ] = 'project.min.pf.cluster.count'
    test_name_converting_table[ 'pfclustersumproject' ] = 'project.pf.cluster.count'
    test_name_converting_table[ 'rawclustermaxproject' ] = 'project.max.raw.cluster.count'
    test_name_converting_table[ 'rawclusterminproject' ] = 'project.min.raw.cluster.count'
    test_name_converting_table[ 'rawclustersumproject' ] = 'project.raw.cluster.count'
    test_name_converting_table[ 'samplecountproject' ] = 'project.sample.count'
    test_name_converting_table[ 'samplesexceededcontaminationthreshold' ] = 'project.fastqscreen.sample.overcontamination.count'
    test_name_converting_table[ 'adaptercontent' ] = 'sample.fastqc.adapter.content'
    test_name_converting_table[ 'badtiles' ] = 'sample.fastqc.bad.tiles'
    test_name_converting_table[ 'basicstats' ] = 'sample.fastqc.basic.stats'
    test_name_converting_table[ 'duplicationlevel' ] = 'sample.fastqc.duplication.level'
    test_name_converting_table[ 'fsqmapped' ] = 'sample.fastqscreen.mapped.percent'
    test_name_converting_table[ 'hitnolibraries' ] = 'sample.fastqscreen.mapped.except.ref.genome.percent'
    test_name_converting_table[ 'kmercontent' ] = 'sample.fastqc.kmer.content'
    test_name_converting_table[ 'linkreport' ] = 'sample.fastqscreen.report'
    test_name_converting_table[ 'meanqualityscorepf' ] = 'sample.base.pf.mean.quality.score'
    test_name_converting_table[ 'ncontent' ] = 'sample.fastqc.n.content'
    test_name_converting_table[ 'overrepresentedseqs' ] = 'sample.fastqc.overrepresented.sequences'
    test_name_converting_table[ 'perbasequalityscores' ] = 'sample.fastqc.per.base.quality.scores'
    test_name_converting_table[ 'perbasesequencecontent' ] = 'sample.fastqc.per.base.sequence.content'
    test_name_converting_table[ 'percentinlanesample' ] = 'sample.in.lane.percent'
    test_name_converting_table[ 'percentpfsample' ] = 'sample.pf.percent'
    test_name_converting_table[ 'percentq30' ] = 'sample.q30.percent'
    test_name_converting_table[ 'persequencegccontent' ] = 'sample.fastqc.per.sequence.gc.content'
    test_name_converting_table[ 'persequencequalityscores' ] = 'sample.fastqc.per.sequence.quality.scores'
    test_name_converting_table[ 'pertilesequencequality' ] = 'sample.fastqc.per.tile.sequence.quality'
    test_name_converting_table[ 'pfclusterssamples' ] = 'sample.pf.cluster.count'
    test_name_converting_table[ 'rawclusterssamples' ] = 'sample.raw.cluster.count'
    test_name_converting_table[ 'linkreportrecoverycluster' ] = 'sample.cluster.recovery.report'
    test_name_converting_table[ 'recoverablepfclusterssamplespercent' ] = 'sample.recoverable.pf.cluster.percent'
    test_name_converting_table[ 'recoverablepfclusterssamples' ] = 'sample.recoverable.pf.cluster.count'
    test_name_converting_table[ 'recoverablerawclusterssamplespercent' ] = 'sample.recoverable.raw.cluster.percent'
    test_name_converting_table[ 'recoverablerawclusterssamples' ] = 'sample.recoverable.raw.cluster.count'
    test_name_converting_table[ 'sequencelengthdistribution' ] = 'sample.fastqc.sequence.length.distribution'
    test_name_converting_table[ 'samplestatsfsqmapped' ] = 'pooledsample.fastqscreen.mapped.percent'
    test_name_converting_table[ 'genomessample' ] = 'pooledsample.genome.names'
    test_name_converting_table[ 'samplestathitnolibrariessum' ] = 'pooledsample.fastqscreen.mapped.except.ref.percent'
    test_name_converting_table[ 'isindexedsample' ] = 'pooledsample.is.indexed'
    test_name_converting_table[ 'lanesrunsample' ] = 'pooledsample.lane.count'
    test_name_converting_table[ 'linksamplereport' ] = 'pooledsample.fastqscreen.report'
    test_name_converting_table[ 'meanqualityscorepfsamplestats' ] = 'pooledsample.pf.base.mean.quality.score'
    test_name_converting_table[ 'samplestatspassingfilterclusterpercent' ] = 'pooledsample.pf.percent'
    test_name_converting_table[ 'samplestatspercentq30basepf' ] = 'pooledsample.q30.percent'
    test_name_converting_table[ 'samplestatsrecoverablepfclusterpercent' ] = 'pooledsample.recoverable.pf.cluster.percent'
    test_name_converting_table[ 'samplestatsrecoverablerawclusterpercent' ] = 'pooledsample.recoverable.raw.cluster.percent'
    test_name_converting_table[ 'percentsampleinproject' ] = 'pooledsample.in.project.percent'
    test_name_converting_table[ 'percentsampleinrun' ] = 'pooledsample.in.run.percent'
    test_name_converting_table[ 'samplestatspfclustermax' ] = 'pooledsample.max.pf.cluster.count'
    test_name_converting_table[ 'samplestatspfclustermin' ] = 'pooledsample.min.pf.cluster.count'
    test_name_converting_table[ 'samplestatpfclustersum' ] = 'pooledsample.pf.cluster.count'
    test_name_converting_table[ 'samplestatsrawclustermax' ] = 'pooledsample.max.raw.cluster.count'
    test_name_converting_table[ 'samplestatsrawclustermin' ] = 'pooledsample.min.raw.cluster.count'
    test_name_converting_table[ 'samplestatrawclustersum' ] = 'pooledsample.raw.cluster.count'
    test_name_converting_table[ 'samplestatsrecoverypfcluster' ] = 'pooledsample.pf.cluster.recovery.count'
    test_name_converting_table[ 'samplestatsrecoveryrawcluster' ] = 'pooledsample.raw.cluster.recovery.count'
    test_name_converting_table[ 'samplestatcountsample' ] = 'pooledsample.sample.count'
    test_name_converting_table[ 'samplestatexceededcontaminationthreshold' ] = 'pooledsample.fastqscreen.sample.overcontamination.count'

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

            if len(fields) == 1:
                value = ''
            else :
                value = fields[1].strip()

            if len(fields) == 2:

                # Check if needed to converting key
                if key in converting_table_key:
                    key = converting_table_key[key]

                # Converting old test names to new ones
                if key.startswith('qc.test.'):
                    for k in test_name_converting_table.keys():
                        prefix = 'qc.test.' + k + '.'
                        if key.lower().startswith(prefix.lower()):
                            key = 'qc.test.' + test_name_converting_table[k] + key[len(prefix) - 1:]
                            break

                conf[key] = value

    f.close()

    # Save configuration file path
    conf[Settings.AOZAN_CONF_FILE_PATH] = conf_file_path

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
    conf[Settings.RECOMPRESS_STEP_KEY] = 'False'
    conf[Settings.RECOMPRESS_THREADS_KEY] = str(Runtime.getRuntime().availableProcessors())

    # Recompression
    conf[Settings.RECOMPRESS_DELETE_ORIGINAL_FASTQ_KEY] = 'False'
    conf[Settings.RECOMPRESS_COMPRESSION_LEVEL_KEY] = '9'
    conf[Settings.RECOMPRESS_COMPRESSION_KEY] = 'bzip2'
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
    conf[Settings.BCL2FASTQ_COMPRESSION_LEVEL_KEY] = '9'
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
    conf[Settings.QC_CONF_FASTQSCREEN_MAPPING_IGNORE_PAIRED_END_MODE_KEY] = 'true'
    # Default mapper is bowtie
    conf[Settings.QC_CONF_FASTQSCREEN_MAPPER_KEY] = 'bowtie'

    # Docker URI
    conf[Settings.DOCKER_URI_KEY] = 'unix:///var/run/docker.sock'

    # Change output file rights
    conf[READ_ONLY_OUTPUT_FILES_KEY] = 'true'
