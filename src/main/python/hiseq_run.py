# -*- coding: utf-8 -*-

import os, time
from xml.etree.ElementTree import ElementTree
import common

def load_processed_run_ids(conf):
	"""Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

	return common.load_processed_run_ids(conf['aozan.var.path'] + '/hiseq.done')

def load_deny_run_ids(conf):
	"""Load the list of the run ids to not process.

    Arguments:
        conf: configuration dictionary
    """

	return common.load_processed_run_ids(conf['aozan.var.path'] + '/hiseq.deny')

def add_run_id_to_processed_run_ids(run_id, conf):
	"""Add a processed run id to the list of the run ids.

    Arguments:
        run id: The run id
        conf: configuration dictionary
    """

	common.add_run_id_to_processed_run_ids(run_id, conf['aozan.var.path'] + '/hiseq.done', conf)



def get_reads_number(run_id, conf):
	"""Get the number of read of a run.

	Arguments:
		runtId: the run id
		conf: configuration dictionary
	"""

	hiseq_data_path = find_hiseq_run_path(run_id, conf)

	run_parameters_path = hiseq_data_path + '/' + run_id + "/runParameters.xml"

	if not os.path.exists(run_parameters_path):
		return -1

	tree = ElementTree()
	tree.parse(hiseq_data_path + '/' + run_id + "/runParameters.xml")

	reads = tree.find("Setup/Reads")

	return len(reads)


def check_end_run(run_id, conf):
	"""Check the end of a run data transfert.

	Arguments:
		runtId: the run id
		conf: configuration dictionary
	"""

	hiseq_data_path = find_hiseq_run_path(run_id, conf)
	if hiseq_data_path == False:
		return False

	reads_number = get_reads_number(run_id, conf)

	# if reads_number equals -1, runParameters.xml is missing
	if reads_number == -1
		return False

	for i in range(reads_number):
		if not os.path.exists(hiseq_data_path + '/' + run_id + '/Basecalling_Netcopy_complete_Read' + str(i + 1) + '.txt'):
			return False

	return True

def check_end_run_since(run_id, secs, conf):
	"""Check the end of a run data transfert since a number of seconds.

	Arguments:
		runtId: the run id
		secs: naximal number of seconds
		conf: configuration dictionary
	"""

	hiseq_data_path = find_hiseq_run_path(run_id, conf)
	if hiseq_data_path == False:
		return 0

	reads_number = get_reads_number(run_id, conf)
	last = 0

	for i in range(reads_number):
		file_to_test = hiseq_data_path + '/' + run_id + '/Basecalling_Netcopy_complete_Read' + str(i + 1) + '.txt'
		if not os.path.exists(file_to_test):
			return 0
		else:
			m_time = os.stat(file_to_test).st_mtime
			if m_time > last:
				last = m_time

	if (time.time() - last) < secs:
		return last
	return 0


def check_run_id(run_id, conf):
	"""Check if the run id is valid.

	Arguments:
		runId: the run id
		conf: configuration dictionary
	"""

	fields = run_id.strip().split("_")

	if len(fields) != 4:
		return False

	date = fields[0]
	count = fields[2]
	flow_cell_id = fields[3]

	# Test the date
	if not date.isdigit() or len(date) != 6:
		return False

	# Test the run count
	if not count.isdigit() or len(count) != 4:
		return False

	# Test the flow cell id

	if not flow_cell_id.isalnum() or len(flow_cell_id) != 10:
		return False

	return True


def get_available_run_ids(conf):
	"""Get the list of the available runs.

	Arguments:
		conf: configuration dictionary
	"""

	result = set()

	for hiseq_data_path in get_hiseq_data_paths(conf):

		files = os.listdir(hiseq_data_path)
		for f in files:
			if os.path.isdir(hiseq_data_path + '/' + f) and check_run_id(f, conf) and check_end_run(f, conf):
				result.add(f)

	return result

def get_run_number(run_id):
	"""Get the run number from the run id.

	Arguments:
		run_id: the run id
	"""

	return int(run_id.split('_')[2])

def get_flow_cell(run_id):
	"""Get the flow cell id from the run id.

	Arguments:
		run_id: the run id
	"""

	return run_id.split('_')[3][1:]

def get_instrument_sn(run_id):
	"""Get the instrument serial number from the run id.

	Arguments:
		run_id: the run id
	"""

	return run_id.split('_')[1]

def send_mail_if_critical_free_space_available(conf):
	"""Check if disk free space is critical. If true send a mail.

    Arguments:
        conf: configuration dictionary
    """

	for path in get_hiseq_data_paths(conf):

		if os.path.exists(path):
			df = common.df(path)
			free_space_threshold = long(conf['hiseq.critical.min.space'])
			if df < free_space_threshold:
				common.send_msg('[Aozan] Critical: Not enough disk space on Hiseq storage for current run',
							'There is only %.2f' % (df / (1024 * 1024 * 1024)) + ' Gb left for Hiseq run storage in ' + path + '. '
							' The current warning threshold is set to %.2f' % (free_space_threshold / (1024 * 1024 * 1024)) + ' Gb.', conf)

def send_mail_if_recent_run(run_id, secs, conf):

	run_path = find_hiseq_run_path(run_id, conf)
	if run_path == False:
		return

	last = check_end_run_since(run_id, secs, conf)

	if last > 0:
		df = common.df(run_path) / (1024 * 1024 * 1024)
		du = common.du(run_path + '/' + run_id) / (1024 * 1024 * 1024)
		common.send_msg('[Aozan] End of the HiSeq run ' + run_id, 'A new run (' + run_id + ') has been terminated at ' +
					common.time_to_human_readable(last) + '.\n' +
					'Data for this run can be found at: ' + run_path +
					'\n\nFor this task %.2f GB has been used and %.2f GB still free.' % (du, df), conf)


def get_hiseq_data_paths(conf):

	paths = conf['hiseq.data.path'].split(':')
	for i in range(len(paths)):
		paths[i] = paths[i].strip()

	return paths

def find_hiseq_run_path(run_id, conf):

	for path in get_hiseq_data_paths(conf):
		if (os.path.exists(path.strip() + '/' + run_id)):
			return path.strip()

	return False
