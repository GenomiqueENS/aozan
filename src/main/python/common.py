# -*- coding: utf-8 -*-

'''
Created on 25 oct. 2011

@author: Laurent Jourdren
'''

import smtplib, os.path, time
from java.io import File
from java.lang import Runtime
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.mime.audio import MIMEAudio
from email.mime.base import MIMEBase
from email.mime.image import MIMEImage
import mimetypes
from email import encoders



def df(path):
    """Get the free space on a partition.

    Arguments:
        path: file on the partition
    """
    #s = os.statvfs('/')
    #return (s.f_bavail * s.f_frsize)
    if os.path.exists(path):
        return long(File(path).getFreeSpace())

    return 0L

def du(path):
    """Get the disk usage of a directory.

    Arguments:
        path: path of the directory
    """
    cmd = 'du -b --max-depth=0 ' + path
    child_stdin, child_stdout = os.popen2(cmd)
    lines = child_stdout.readlines()
    child_stdin.close()
    child_stdout.close()

    return long(lines[0].split('\t')[0])


def send_msg(subject, message, conf):
    """Send a message to the user about the data extraction."""


    send_mail = conf['send.mail'].lower() == 'true'
    smtp_server = conf['smtp.server']
    mail_to = conf['mail.to']
    mail_from = conf['mail.from']
    mail_cc = None
    mail_bcc = None
    COMMASPACE = ', '

    message = conf['mail.header'].replace('\\n', '\n') + message + conf['mail.footer'].replace('\\n', '\n')
    message = message.replace('\n', '\r\n')
    msg = ''

    if mail_to != None :
        if type(mail_to) == str or type(mail_to) == unicode:
            mail_to = [mail_to]
        msg = msg + ("To: %s\r\n" % COMMASPACE.join(mail_to))

    if mail_cc != None :
        if type(mail_cc) == str or type(mail_cc) == unicode:
            mail_cc = [mail_cc]
        msg = msg + ("Cc: %s\r\n" % COMMASPACE.join(mail_cc))

    if mail_bcc != None :
        if type(mail_bcc) == str or type(mail_bcc) == unicode:
            mail_bcc = [mail_bcc]
        msg = msg + ("Bcc: %s\r\n" % COMMASPACE.join(mail_bcc))


    msg = msg + "Subject: " + subject + "\r\n" + message

    if send_mail:
        server = smtplib.SMTP(smtp_server)
        dests = []
        dests.extend(mail_to)
        if  mail_cc != None :
            dests.extend(mail_cc)
        if mail_bcc != None :
            dests.extend(mail_bcc)
        server.sendmail(mail_from, dests, msg)
        server.quit()
    else:
        print '-------------'
        print msg
        print '-------------'


def send_msg_with_attachment(subject, message, attachment_file, conf):
    """Send a message to the user about the data extraction."""


    send_mail = conf['send.mail'].lower() == 'true'
    smtp_server = conf['smtp.server']
    mail_to = conf['mail.to']
    mail_from = conf['mail.from']
    mail_cc = None
    mail_bcc = None
    COMMASPACE = ', '

    message = conf['mail.header'].replace('\\n', '\n') + message + conf['mail.footer'].replace('\\n', '\n')



    msg = MIMEMultipart()

    if mail_to != None :
        if type(mail_to) == str or type(mail_to) == unicode:
            mail_to = [mail_to]
        msg['To'] = COMMASPACE.join(mail_to)

    if mail_cc != None :
        if type(mail_cc) == str or type(mail_cc) == unicode:
            mail_cc = [mail_cc]
        msg['Cc'] = COMMASPACE.join(mail_cc)

    if mail_bcc != None :
        if type(mail_bcc) == str or type(mail_bcc) == unicode:
            mail_bcc = [mail_bcc]
        msg['Bcc'] = COMMASPACE.join(mail_bcc)

    msg['Subject'] = subject

    # Not seen
    msg.preamble = message

    # The message
    part1 = MIMEText(message, 'plain')
    msg.attach(part1)


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
        if  mail_cc != None :
            dests.extend(mail_cc)
        if mail_bcc != None :
            dests.extend(mail_bcc)
        server.sendmail(mail_from, dests, composed)
        server.quit()
    else:
        print '-------------'
        print composed
        print '-------------'


def error(short_message, message, last_error_file_path, conf):
    """Error handling.

    Arguments:
        short_message: short description of the message
        message: message
        conf: configuration dictionary
    """

    new_error = short_message + message
    new_error.replace('\n', ' ')
    log('CRITICAL', new_error, conf)

    if os.path.exists(last_error_file_path):
        f = open(last_error_file_path, 'r')
        last_error = f.readline()
        f.close()

        if not new_error == last_error:
            send_msg(short_message, message, conf)
    else:
        send_msg(short_message, message, conf)

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

    msg = time_to_human_readable(time.time()) + '\t' + level + '\t' + message

    print(msg)

    try:
        f = open(conf['aozan.var.path'] + '/aozan.log', 'a')
        f.write(msg + '\n')
        f.close()
    except:
        pass



def duration_to_human_readable(time):
    """Convert a number of seconds in human readable string.
    
    Arguments:
        time: the number of seconds
    """

    hours = int(time / 3600)
    hours_rest = time % 3600
    minutes = int(hours_rest / 60)
    minutes_rest = time % 60
    seconds = int(minutes_rest)

    return "%02d:%02d:%02d" % (hours, minutes, seconds)

def time_to_human_readable(time_since_epoch):
    """Convert a number of seconds since epoch in a human readable string.

    Arguments:
        time: the number of seconds
    """

    return time.strftime("%a %b %d %H:%M:%S %Z %Y", time.localtime(time_since_epoch))

def load_processed_run_ids(done_file_path):
    """Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

    result = set()

    if not os.path.exists(done_file_path):
        return result

    f = open(done_file_path, 'r')

    for l in f:
        run_id = l[:-1]
        if len(run_id) == 0:
            continue
        result.add(run_id)

    f.close()

    return result


def add_run_id_to_processed_run_ids(run_id, done_file_path, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run id: The run id
        done_file_path: path of the done file
        conf: configuration dictionary
    """

    log('DEBUG', 'Add ' + run_id + ' to ' + os.path.basename(done_file_path), conf)

    f = open(done_file_path, 'a')

    f.write(run_id + '\n')

    f.close()


def load_conf(conf, conf_file_path):
    """Load configuration file"""

    f = open(conf_file_path, 'r')

    for l in f:
        s = l[:-1].strip()
        if len(s) == 0 or l[0] == '#' :
            continue
        fields = s.split('=')
        if len(fields) == 2:
            conf[fields[0].strip()] = fields[1].strip()

    f.close()
    return conf


def create_html_index_file(conf, output_file_path, run_id, sections):
    """Create an index.html file that contains links to all the generated reports.

    Arguments:
        conf: configuration dictionary
        output_file_path: path of the index.html file to create
        run_id: The run id
        sections: The list of section to write
    """

    text = """<html>
        <head>
                <title>Run ###RUN_ID###</title>
        </head>
        <body>
                <h1>Run ###RUN_ID### reports</h1>

###START_SECTION sync
                <h2>HiSeq reports</h2>

                <ul>
                        <li><a href="report_###RUN_ID###/First_Base_Report.htm">First base report</a></li>
                        <li><a href="report_###RUN_ID###/Status.htm">Run info</a></li>
                        <li><a href="report_###RUN_ID###.tar.bz2">All reports(compressed archive)</a></li>
                        <li><a href="hiseq_log_###RUN_ID###.tar.bz2">HiSeq log (compressed archive)</a></li>
                </ul>
###END_SECTION

###START_SECTION demux
                <h2>Demultiplexing reports</h2>

                <ul>
                        <li><a href="basecall_stats_###RUN_ID###/All.htm">All</a></li>
                        <li><a href="basecall_stats_###RUN_ID###/IVC.htm">IVC</a></li>
                        <li><a href="basecall_stats_###RUN_ID###/Demultiplex_Stats.htm">Demultiplex stats</a></li>
                        <li><a href="basecall_stats_###RUN_ID###.tar.bz2">All reports(compressed archive)</a></li>
                </ul>
###END_SECTION

###START_SECTION qc
                <h2>Quality control reports</h2>

                <ul>
                        <li><a href="qc_###RUN_ID###/###RUN_ID###.html">QC report</a></li>
                </ul>
###END_SECTION

        </body>
</html>"""

    template_path = conf['index.html.template']
    if template_path != None and template_path != '' and os.path.exists(template_path):
        f_in = open(template_path, 'r')
        text = ''.join(f_in.readlines())
        f_in.close()

    
    lines = text.split('\n');
    write_lines = True
    result = ''

    for line in lines:
        if line.startswith('###START_SECTION'):
            section_name = line.split(' ')[1]
            if section_name in  sections:
                write_lines = True
            else:
                write_lines = False
        elif line.startswith('###END_SECTION'):
            write_lines = True
        elif write_lines == True:
            result += line.replace('###RUN_ID###', run_id) + '\n'
        
    f_out = open(output_file_path, 'w')
    f_out.write(result)
    f_out.close()


def set_default_conf(conf):

    # Global
    conf['aozan.enable'] = 'True'
    conf['send.mail'] = 'False'
    conf['first.base.report.step'] = 'True'
    conf['hiseq.step'] = 'True'
    conf['sync.step'] = 'True'
    conf['demux.step'] = 'True'
    conf['qc.step'] = 'False'

    # Lock file
    conf['lock.file'] = '/var/lock/aozan.lock'
    
    # Rsync
    conf['rsync.exclude.cif'] = 'true'

    # Casava
    conf['casava.design.format'] = 'xls'
    conf['casava.path'] = '/usr/local/casava'
    conf['casava.compression'] = 'bzip2'
    conf['casava.fastq.cluster.count'] = '0'
    conf['casava.compression.level'] = '9'
    conf['casava.mismatches'] = '0'
    conf['casava.threads'] = str(Runtime.getRuntime().availableProcessors())
    conf['casava.adapter.fasta.file.path'] = ''
    conf['casava.with.failed.reads'] = 'True'

    # Data path
    conf['tmp.path'] = '/tmp'
    conf['index.sequences'] = ''
    conf['index.html.template'] = ''
    conf['reports.url'] = ''
    conf['qc.report.stylesheet'] = ''

    # Space needed
    conf['hiseq.warning.min.space'] = str(3 * 1024 * 1024 * 1024 * 1024)
    conf['hiseq.critical.min.space'] = str(1 * 1024 * 1024 * 1024 * 1024)
    conf['sync.space.factor'] = str(0.2)
    conf['demux.space.factor'] = str(0.7)

    # Mail configuration
    conf['mail.header'] = 'THIS IS AN AUTOMATED MESSAGE.\\n\\n'
    conf['mail.footer'] = '\\n\\nThe Aozan team.\\n'


