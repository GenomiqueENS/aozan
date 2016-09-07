"""
Created on 22 aug. 2016

@author: firmo
"""
import glob
import os
import time
from subprocess import call, CalledProcessError

import common
# noinspection PyUnresolvedReferences,PyUnresolvedReferences,PyUnresolvedReferences
from fr.ens.biologie.genomique.aozan.Settings import AOZAN_VAR_PATH_KEY
# noinspection PyUnresolvedReferences,PyUnresolvedReferences,PyUnresolvedReferences
from fr.ens.biologie.genomique.aozan.Settings import FASTQ_DATA_PATH_KEY
# noinspection PyUnresolvedReferences,PyUnresolvedReferences,PyUnresolvedReferences
from fr.ens.biologie.genomique.aozan.Settings import RECOMPRESS_COMPRESSION_KEY
# noinspection PyUnresolvedReferences,PyUnresolvedReferences,PyUnresolvedReferences
from fr.ens.biologie.genomique.aozan.Settings import RECOMPRESS_COMPRESSION_LEVEL_KEY
# noinspection PyUnresolvedReferences,PyUnresolvedReferences,PyUnresolvedReferences
from fr.ens.biologie.genomique.aozan.Settings import RECOMPRESS_DELETE_ORIGINAL_FASTQ_KEY
# noinspection PyUnresolvedReferences,PyUnresolvedReferences,PyUnresolvedReferences
from fr.ens.biologie.genomique.aozan.Settings import RECOMPRESS_THREADS_KEY
# noinspection PyUnresolvedReferences,PyUnresolvedReferences,PyUnresolvedReferences
from java.lang import Runnable
# noinspection PyUnresolvedReferences,PyUnresolvedReferences,PyUnresolvedReferences
from java.util.concurrent import Executors


class Worker(Runnable):
    def __init__(self, input_file, output_file, input_decompression_command, output_compression_command,
                 output_decompression_command,
                 compression_level_argument,
                 delete_original_files):
        """Class to execute recompression in different threads.

        Arguments:
            input_file: input file to decompress
            output_file: recompressed output file
            input_decompression_command: command to decompress input file
            output_compression_command: command to recompress input file
            output_decompression_command: command to decompress output file
            compression_level_argument: argument used in compression command
            delete_original_files: boolean which tells if input file should be removed or not
        """

        self.input_file = input_file
        self.output_file = output_file
        self.input_decompression_command = input_decompression_command
        self.output_compression_command = output_compression_command
        self.output_decompression_command = output_decompression_command
        self.compression_level_argument = compression_level_argument
        self.delete_original_files = delete_original_files
        self.success = False
        self.error_message = ""
        self.long_error_message = ""

    def run(self):
        (self.success, self.error_message, self.long_error_message) = recompress_fastq_process(self.input_file,
                                                                                               self.output_file,
                                                                                               self.input_decompression_command,
                                                                                               self.output_compression_command,
                                                                                               self.output_decompression_command,
                                                                                               self.compression_level_argument,
                                                                                               self.delete_original_files)

    def get_error_message(self):
        return self.error_message

    def get_long_error_message(self):
        return self.long_error_message

    def is_successful(self):
        return self.success


def get_info_from_file_type(file_type, compression_level=None):
    """return a tuple of information about a file_type or extension

    Arguments:
        file_type: type or extension
        compression_level: Formats compression level argument for the type given (Optional)
    """
    file_types = {
        # [type, extension, compression_command, decompression_command, compression_level_arg]
        "bzip2": ["bzip2", "bz2", "bzip2", "bzcat", "-" + str(compression_level)],
        "gzip": ["gzip", "gz", "gzip", "zcat", "-" + str(compression_level)],
        "fastq": ["fastq", "", "", "cat", ""]

    }
    # aliases of ext to info
    for type_key in file_types.keys():
        file_types[file_types[type_key][1]] = file_types[type_key]

    result = file_types[file_type]

    if compression_level is None:
        result[4] = ""

    return result


def recompress_fastq_process(input_file, output_file, input_decompression_command, output_compression_command,
                             output_decompression_command,
                             compression_level_argument, delete_original_files):
    """process recompression for input fastq

    Arguments:
            input_file: input file to decompress
            output_file: recompressed output file
            input_decompression_command: command to decompress input file
            output_compression_command: command to recompress input file
            output_decompression_command: command to decompress output file
            compression_level_argument: argument used in compression command
            delete_original_files: boolean which tells if input file should be removed or not
    """

    # temporary output file name
    temp_file = output_file + ".tmp"

    # convert input_file to output_file
    if convert_fastq_file(input_file, temp_file, input_decompression_command, output_compression_command,
                          output_decompression_command, compression_level_argument):

        #  Check md5sum before proceeding
        if compare_md5sum(input_file, temp_file):

            # Rename and set previous time stamp and rights to output_file then remove md5sum files
            os.rename(temp_file, output_file)
            if call(["bash", "-c", "touch -r \'" + input_file + "\' \'" + output_file + "\'"]) != 0:
                error_message = "Error while trying to edit metadata using touch command."
                long_error_message = "Error while trying to edit metadata using touch command. Input file is " + str(
                    input_file) + " and output file is " + str(
                    output_file) + ""
                return False, error_message, long_error_message

            if call(["bash", "-c", "chmod --reference \'" + input_file + "\' \'" + output_file + "\'"]) != 0:
                error_message = "Error while trying to edit rights using chmod command."
                long_error_message = "Error while trying to edit right using touch command. Input file is " + str(
                    input_file) + " and output file is " + str(
                    output_file) + ""
                return False, error_message, long_error_message

            os.remove(input_file + ".md5")
            os.remove(temp_file + ".md5")

            # if enabled remove input_files
            if delete_original_files:
                os.remove(input_file)

        else:
            error_message = "Md5sum differs between initial file content and created file content."
            long_error_message = "Md5sum differs between initial file " + str(
                input_file) + " content and created file " + str(
                output_file) + " content."
            return False, error_message, long_error_message

    else:
        error_message = "Failed to recompress a file successfully"
        long_error_message = "Failed to recompress the file " + str(input_file) + "successfully."
        return False, error_message, long_error_message

    # If everything is OK
    return True, "", ""


def list_files(input_dir, extension):
    """lists all the files in input_dir with finishing by extension

    Arguments:
        input_dir: directory
        extension

    """
    r = list()
    for (input_path, input_dirs, input_files) in os.walk(input_dir):
        for input_file in glob.glob(input_path + '/*' + extension):
            r.append(input_file)
    return r


def convert_fastq_file(input_file, output_file, input_decompression_command, output_compression_command,
                       output_decompression_command, compression_level_argument):
    """covert input_file fastq into output_file

    Arguments:
            input_file: input file to decompress
            output_file: recompressed output file
            input_decompression_command: command to decompress input file
            output_compression_command: command to recompress input file
            output_decompression_command: command to decompress output file
            compression_level_argument: argument used in compression command
    """
    # Doesn't process if input_file doesn't exist
    if os.path.exists(input_file):

        input_md5sum = input_file + ".md5"
        output_md5sum = output_file + ".md5"

        # Command a decompresses input file create md5sum for this file then compresses it
        # Whereas Command b decompresses output file just to check md5sum
        # noinspection PyPep8
        command_a = input_decompression_command + " '" + input_file + "' |tee >(md5sum > '" + input_md5sum + "') | " \
                        + output_compression_command + " " + compression_level_argument + " > '" + output_file + "'"

        command_b = output_decompression_command + " '" + output_file + "' | md5sum > '" + output_md5sum + "'"

        # Actual commands execution
        try:

            if call(["bash", "-c", command_a]) == 0 and call(["bash", "-c", command_b]) == 0:
                return True

            return False

        except CalledProcessError:
            return False
    else:
        return False


def compare_md5sum(filename_a, filename_b):
    """compare md5sum between two files

    Arguments:
        filename_a
        filename_b
    """
    md5sum_filename_a = filename_a + ".md5"
    md5sum_filename_b = filename_b + ".md5"

    # Open md5sum files the read line with md5sum
    try:

        filehandle_a = open(md5sum_filename_a, 'r')
        filehandle_b = open(md5sum_filename_b, 'r')
        md5sum_a = filehandle_a.readline().split(" ")[0]
        md5sum_b = filehandle_b.readline().split(" ")[0]
        filehandle_a.close()
        filehandle_b.close()

        # compare md5sums
        if md5sum_a == md5sum_b:
            return True

    except IOError:
        return False


def load_denied_run_ids(conf):
    """Load the list of the denied run ids.

    Arguments:
        conf: configuration dictionary
    """
    return common.load_run_ids(conf[AOZAN_VAR_PATH_KEY] + '/' + common.RECOMPRESS_DENY_FILE)


def load_processed_run_ids(conf):
    """Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

    return common.load_run_ids(conf[AOZAN_VAR_PATH_KEY] + '/' + common.RECOMPRESS_DONE_FILE)


def add_run_id_to_processed_run_ids(run_id, conf):
    """Add a processed run id to the list of the run ids.

    Arguments:
        run_id: The run id
        conf: configuration dictionary
    """
    common.add_run_id(run_id, conf[AOZAN_VAR_PATH_KEY] + '/' + common.RECOMPRESS_DONE_FILE, conf)


def error(short_message, message, conf):
    """Error handling.

    Arguments:
        short_message: short description of the message
        message: message
        conf: configuration dictionary
    """

    common.error('[Aozan] recompress: ' + short_message, message,
                 conf[AOZAN_VAR_PATH_KEY] + '/' + common.RECOMPRESS_LASTERR_FILE, conf)


def recompress(run_id, conf):
    """Proceed to recompression of a run.

    Arguments:
        run_id: The run id
        conf: configuration dictionary
    """

    common.log('INFO', 'Recompress step: start', conf)

    # Check if input root fastq root data exists
    if not common.is_dir_exists(FASTQ_DATA_PATH_KEY, conf):
        error("Fastq data directory does not exists",
              "Fastq data directory does not exists: " + conf[FASTQ_DATA_PATH_KEY], conf)
        return False

    start_time = time.time()
    fastq_input_dir = conf[FASTQ_DATA_PATH_KEY] + '/' + run_id

    # initial du for comparing with ending disk usage
    previous_du_in_bytes = common.du(fastq_input_dir)

    # get information about compression type
    compression_type = conf[RECOMPRESS_COMPRESSION_KEY]
    compression_level = conf[RECOMPRESS_COMPRESSION_LEVEL_KEY]
    compression_info_tuple = get_info_from_file_type(compression_type, compression_level)

    if compression_info_tuple is None:
        error("Unknown compression type",
              "Unknown compression type: " + compression_type, conf)
        return False

    (compression_type_result, output_file_extension, output_compression_command, output_decompression_command,
     compression_level_argument) = compression_info_tuple

    # The following list contains the processed type of files to recompress
    types_to_recompress = ["fastq.gz", "fastq"]

    # list of program to check if exists in path before execution
    program_set = {"bash", "tee", "touch", "chmod", "md5sum", output_compression_command, output_decompression_command}

    # get list of file to process
    input_files = []
    for extension in types_to_recompress:

        input_files.extend(list_files(fastq_input_dir, extension))
        simple_extension = os.path.splitext(extension)[-1][1:]
        extension_info_tuple = get_info_from_file_type(simple_extension)

        if extension_info_tuple is None:
            error("Unknown extension type",
                  "Unknown extension type: " + extension, conf)
            return False

        program_set.add(extension_info_tuple[3])

    # actual program list check
    for program in program_set:
        if not common.exists_in_path(program):
            error("Can't find all needed commands in PATH env var",
                  "Can't find all needed commands in PATH env var. Unable to find: " + program + " command.", conf)
            return False

    # Create executor and for parallelization of processus
    executor = Executors.newFixedThreadPool(int(conf[RECOMPRESS_THREADS_KEY]))
    workers = []

    # process each fastq and fastq.gz recursively in each fastq directory
    for input_file in input_files:

        simple_extension = os.path.splitext(input_file)[-1][1:]

        # get info about the type of input file
        extension_info_tuple = get_info_from_file_type(simple_extension)
        if extension_info_tuple is None:
            error("Unknown extension type",
                  "Unknown extension type: " + simple_extension, conf)
            return False

        input_decompression_command = extension_info_tuple[3]

        # get file base name and create output_file name, if file is already .fastq its ready to be base_input_file
        base_input_file = input_file[0: input_file.index(".fastq") + 6]
        output_file = base_input_file + "." + output_file_extension

        # Skip if the output_file already exists
        if not os.path.exists(output_file):

            # Create worker then execute in thread
            worker = Worker(input_file, output_file, input_decompression_command, output_compression_command,
                            output_decompression_command,
                            compression_level_argument,
                            common.is_conf_value_equals_true(RECOMPRESS_DELETE_ORIGINAL_FASTQ_KEY, conf))
            workers.append(worker)
            executor.execute(worker)

        else:
            common.log("WARNING", "Recompress step: Skipping: The file " + output_file + " already exists.", conf)

    # Wait for all thread to finish
    executor.shutdown()
    while not executor.isTerminated():
        time.sleep(1)

    # Check if any worker is in error
    for worker in workers:
        if not worker.is_successful():
            error(worker.get_error_message(),
                  worker.get_long_error_message(), conf)
            return False

    # check new disk usage
    df_in_bytes = common.df(fastq_input_dir)
    du_in_bytes = common.du(fastq_input_dir)
    previous_du = previous_du_in_bytes / (1024 * 1024)
    df = df_in_bytes / (1024 * 1024 * 1024)
    du = du_in_bytes / (1024 * 1024)

    common.log("WARNING", "Recompress step: output disk free after step: " + str(df_in_bytes), conf)
    common.log("WARNING", "Recompress step: space previously used: " + str(previous_du_in_bytes), conf)
    common.log("WARNING", "Recompress step: space now used by step: " + str(du_in_bytes), conf)

    duration = time.time() - start_time

    msg = 'End of recompression for run ' + run_id + '.' + \
          '\nJob finished at ' + common.time_to_human_readable(time.time()) + \
          ' with no error in ' + common.duration_to_human_readable(duration) + '. '

    msg += '\n\nAfter recompress step fastq folder is now %.2f MB (previously %.2f MB) and %.2f GB still free.' % (
        du, previous_du, df)

    common.send_msg('[Aozan] End of recompress for run ' + run_id + ' on ' +
                    common.get_instrument_name(run_id, conf), msg, False, conf)
    common.log('INFO', 'Recompress step: success in ' + common.duration_to_human_readable(duration), conf)
    return True
