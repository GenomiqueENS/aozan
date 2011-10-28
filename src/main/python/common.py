# -*- coding: utf-8 -*-

'''
Created on 25 oct. 2011

@author: Laurent Jourdren
'''

import smtplib
import os.path
from java.io import File



def df(path):
    """Get the free space on a partition.

    Arguments:
        path: file on the partition
    """
    #s = os.statvfs('/')
    #return (s.f_bavail * s.f_frsize)
    if os.path.exists(path):
        return File(path).getFreeSpace()
    
    return 0

def du(path):
    """Get the disk usage of a directory.

    Arguments:
        path: path of the directory
    """
    cmd = 'du  --max-depth=0 ' + path
    child_stdin, child_stdout = os.popen2(cmd)
    lines = child_stdout.readlines()
    child_stdin.close()
    child_stdout.close()
            
    return lines[0].split('\t')[0]


def send_msg(subject, message, conf):
    """Send a message to the user about the data extraction."""


    send_mail = conf['send.mail'].lower() == 'true'
    smtp_server = conf['smtp.server']
    mail_to = conf['mail.to']
    mail_from = conf['mail.from']
    mail_cc = None
    mail_bcc = None

    message = message.replace('\n', '\r\n')
    msg = ''

    if mail_to != None :
        if type(mail_to) == str:
            mail_to = [mail_to]
        msg = msg + ("To: %s\r\n" % ", ".join(mail_to))

    if mail_cc != None :
        if type(mail_cc) == str:
            mail_cc = [mail_cc]
        msg = msg + ("Cc: %s\r\n" % ", ".join(mail_cc))

    if mail_bcc != None :
        if type(mail_bcc) == str:
            mail_bcc = [mail_bcc]
        msg = msg + ("Bcc: %s\r\n" % ", ".join(mail_bcc))


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


def error(short_message, message, last_error_file_path, conf):
    """Error handling.

    Arguments:
        short_message: short description of the message
        message: message
        conf: configuration dictionary
    """

    new_error = short_message + message
    new_error.replace('\n','')

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


def load_processed_run_ids(done_file_path):
    """Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

    result = set()

    f = open(done_file_path, 'r')

    for l in f:
        run_id = l[:-1]
        if len(run_id) == 0:
            continue
        result.add(run_id)

    f.close()

    return result


def add_run_id_to_processed_run_ids(run_id, done_file_path):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run id: The run id
        conf: configuration dictionary
    """

    f = open(done_file_path, 'a')

    f.write(run_id + '\n')

    f.close()


def load_conf(conf_file_path):
    """Load configuration file"""

    result = {}

    f = open(conf_file_path, 'r')

    for l in f:
        s = l[:-1].strip()
        if len(s) == 0 or l[0]=='#' :
            continue
        fields = s.split('=')
        if len(fields) == 2:
            result[fields[0].strip()] = fields[1].strip()

    f.close()

    return result

def load_test_conf():

    result = {}

    # HiSeq
    result['hiseq.sn'] = 'SNL110'

    # Mail configuration
    result['send.mail'] = 'False'
    result['smtp.server'] = 'smtp.biologie.ens.fr'
    result['mail.from'] = 'jourdren@biologie.ens.fr'
    result['mail.to'] = 'jourdren@biologie.ens.fr'

    # Data paths
    result['hiseq.data.path'] = '/import/freki01'
    result['work.data.path'] = '/import/bara02/aozan_work'
    result['fastq.data.path'] = '/import/bara02/aozan_work'
    result['storage.data.path'] = '/import/mimir03/sequencages/runs'
    result['casava.designs.path'] = '/import/mimir03/sequencages/casava_designs'
    result['tmp.path'] = '/tmp'

    # Lock file
    result['lock.file'] = '/var/lock/aozan.lock'

    # Casava
    result['casava.path'] = '/usr/local/casava'
    result['casava.compression'] = 'bzip2'
    result['casava.fastq.cluster.count'] = '1000000000'
    result['casava.compression.level'] = '9'
    result['casava.mismatches'] = '0'
    result['casava.threads'] = '4'

    # Achievement files
    result['sync.done.file'] = '/home/jourdren/tmp/sync.done'
    result['demux.done.file'] = '/home/jourdren/tmp/demux.done'
    result['qc.done.file'] = '/home/jourdren/tmp/qc.done'
    
    # Last error files
    result['sync.last.error.file'] = '/home/jourdren/tmp/sync.lasterr'
    result['demux.last.error.file'] = '/home/jourdren/tmp/demux.lasterr'
    result['qc.last.error.file'] = '/home/jourdren/tmp/qc.lasterr'
    

    return result
