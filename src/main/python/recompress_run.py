'''
Created on 22 aug. 2016

@author: firmo
'''
import glob
import os
import stat
from subprocess import call

import common
import demux_run
import hiseq_run
import time
from fr.ens.biologie.genomique.aozan.Settings import AOZAN_VAR_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import FASTQ_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import RECOMPRESS_COMPRESSION_LEVEL_KEY
from fr.ens.biologie.genomique.aozan.Settings import RECOMPRESS_DELETE_ORIGINAL_FASTQ_KEY
from fr.ens.biologie.genomique.aozan.Settings import RECOMPRESS_THREADS_KEY
from fr.ens.biologie.genomique.aozan.Settings import REPORTS_DATA_PATH_KEY
from fr.ens.biologie.genomique.aozan.Settings import REPORTS_URL_KEY
from fr.ens.biologie.genomique.aozan.Settings import TMP_PATH_KEY
from java.lang import Runnable
from java.util.concurrent import Executors

DENY_FILE = "recompress.deny"
DONE_FILE = "recompress.done"
LASTERR_FILE = "recompress.lasterr"


class Worker(Runnable):
    def __init__(self, fq, conf):
        self.fq = fq
        self.conf = conf

    def run(self):
        recompress_fastq_process(self.fq, self.conf)


def recompress_fastq_process(fq, conf):
    ext = os.path.splitext(fq)[-1]
    if ext == ".gz":
        cat = "zcat"
        base_fq = os.path.splitext(fq)[0]
    else:
        cat = "cat"
        base_fq = fq
    bz2 = base_fq + ".bz2"
    bz2_temp = bz2 + ".tmp"
    # Skip if the bz2 already exists
    if not os.path.exists(bz2):
        # convert fq to bz2 then check md5sum before proceeding
        if convert_fastq_to_bzip2(fq, bz2_temp, cat, conf):
            if check_md5sum(fq, bz2_temp, conf):
                os.rename(bz2_temp, bz2)
                remove_related_md5_file(fq)
                remove_related_md5_file(bz2_temp)
                if common.is_conf_value_equals_true(RECOMPRESS_DELETE_ORIGINAL_FASTQ_KEY, conf):
                    common.log("INFO", "Removing " + fq + " since recompression is a success.", conf)
                    os.remove(fq)
            else:
                error("Md5sum differs between initial file content and created file content.",
                      "Md5sum differs between initial file " + str(fq) + " content and created file " + str(
                          bz2) + " content.", conf)
        else:
            error("Failed to recompress a file successfully",
                  "Failed to recompress the file " + str(fq) + "successfully.", conf)
    else:
        common.log("WARNING", "Recompress step: Skipping: The file " + bz2 + " already exists.", conf)


def load_fastqgz_list(fastq_input_dir):
    r = list()
    for (fq_path, fq_dirs, fq_files) in os.walk(fastq_input_dir):
        for fq in glob.glob(fq_path + '/*.fastq.gz'):
            r.append(fq)
    return r


def load_fastq_list(fastq_input_dir):
    r = list()
    for (fq_path, fq_dirs, fq_files) in os.walk(fastq_input_dir):
        for fq in glob.glob(fq_path + '/*.fastq'):
            r.append(fq)
    return r


def convert_fastq_to_bzip2(fq, bz2, cat, conf):
    if os.path.exists(fq):
        fq_md5sum = fq + ".md5"
        bz2_md5sum = bz2 + ".md5"
        compression_level = int(conf[RECOMPRESS_COMPRESSION_LEVEL_KEY])
        cmdA = cat + " '" + fq + "' |tee >(md5sum > '" + fq_md5sum + "') | bzip2 -" + str(
            compression_level) + " > '" + bz2 + "'"
        cmdB = "bzcat '" + bz2 + "' | md5sum > '" + bz2_md5sum + "'"
        try:
            common.log('INFO', 'exec: ' + str(cmdA), conf)
            if call(["bash", "-c", cmdA]) == 0:
                common.log('INFO', 'exec: ' + str(cmdB), conf)
                if call(["bash", "-c", cmdB]) == 0:
                    return True
            common.log('SEVERE', 'Execution error on converting fastq to bzip2', conf)
            return False
        except:
            common.log('SEVERE', 'Execution error on converting fastq to bzip2', conf)
            return False
    else:
        return False


def check_md5sum(fileA, fileB, conf):
    md5sum_fileA = fileA + ".md5"
    md5sum_fileB = fileB + ".md5"
    try:
        fA = open(md5sum_fileA, 'r')
        fB = open(md5sum_fileB, 'r')
        md5A = fA.readline().split(" ")[0]
        md5B = fB.readline().split(" ")[0]
        if md5A == md5B:
            return True
    except:
        return False


def remove_related_md5_file(filein):
    os.remove(filein + ".md5")


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

    common.error('[Aozan] recompress: ' + short_message, message, conf[AOZAN_VAR_PATH_KEY] + '/' + LASTERR_FILE, conf)


def recompress(run_id, conf):
    """Proceed to recompression of a run.

    Arguments:
        run_id: The run id
        conf: configuration dictionary
    """

    start_time = time.time()

    input_run_data_path = common.get_input_run_data_path(run_id, conf)

    if input_run_data_path is None:
        return False

    fastq_input_dir = conf[FASTQ_DATA_PATH_KEY] + '/' + run_id
    common.log('INFO', 'Recompress step: start', conf)

    # Check if input root fastq root data exists
    if not common.is_dir_exists(FASTQ_DATA_PATH_KEY, conf):
        error("Fastq data directory does not exists",
              "Fastq data directory does not exists: " + conf[FASTQ_DATA_PATH_KEY], conf)
        return False

    # process each fastq and fastq.gz recursively in each fastq directory
    exe = Executors.newFixedThreadPool(int(conf[RECOMPRESS_THREADS_KEY]))
    for fq in load_fastqgz_list(fastq_input_dir) + load_fastq_list(fastq_input_dir):
        exe.execute(Worker(fq, conf))

    exe.shutdown();
    while not exe.isTerminated():
        time.sleep(1)

    duration = time.time() - start_time
    msg = 'End of recompression for run ' + run_id + '.' + \
          '\nJob finished at ' + common.time_to_human_readable(time.time()) + \
          ' with no error in ' + common.duration_to_human_readable(duration) + '. '
    common.send_msg('[Aozan] End of recompress for run ' + run_id + ' on ' +
                    common.get_instrument_name(run_id, conf), msg, False, conf)
    common.log('INFO', 'Recompress step: success in ' + common.duration_to_human_readable(duration), conf)
    return True
