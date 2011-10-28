#!/usr/bin/python
# -*- coding: utf-8 -*-

import os, time
from xml.etree.ElementTree import ElementTree
import common

# Globals
#hiseq_data_path = '/import/freki01'
#work_data_path = '/import/bara02/aozan_work'
#storage_data_path = '/import/mimir03/sequencages/runs'

#hiseq_sn = "SNL110"
#hiseq_sn = "SN501"
#base_path = '/usr/local/aozan'
#processed_file_path = base_path + '/processed.txt'
#lock_file_path = base_path + '/sync.lock'
#lock_file_path = '/var/lock/aozan-sync.lock'
#process_run_script = base_path + '/sync_run.sh'

def load_processed_run_ids(conf):
	"""Load the list of the processed run ids.

    Arguments:
        conf: configuration dictionary
    """

	return common.load_processed_run_ids(conf['aozan.var.path'] + '/hiseq.done')

def add_run_id_to_processed_run_ids(run_id, conf):
	"""Add a processed run id to the list of the run ids.

    Arguments:
        run id: The run id
        conf: configuration dictionary
    """

	common.add_run_id_to_processed_run_ids(conf['aozan.var.path'] + '/hiseq.done')



def get_reads_number(run_id, conf):
	"""Get the number of read of a run.

	Arguments:
		runtId: the run id
		conf: configuration dictionary
	"""

	hiseq_data_path = conf['hiseq.data.path']

	tree = ElementTree()
	tree.parse(hiseq_data_path + '/' + run_id + "/runParameters.xml")

	reads = tree.find("Setup/Reads")
	list_reads = list(reads.iter("Read"))

	print len(list_reads)

	read_count = 0
	for r in list_reads:
		read_count += 1
		print("\t" + str(read_count) + '\t' + 'NumCycles=' + r.get('NumCycles') + "\t" + 'IsIndexedRead=' + r.get('IsIndexedRead'))


	return len(list_reads)


def check_end_run(run_id, conf):
	"""Check the end of a run data transfert.

	Arguments:
		runtId: the run id
		conf: configuration dictionary
	"""

	hiseq_data_path = conf['hiseq.data.path']
	reads_number = get_reads_number(run_id)

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

	hiseq_data_path = conf['hiseq.data.path']
	reads_number = get_reads_number(run_id)
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

	hiseq_sn = conf['hiseq.sn']
	fields = run_id.strip().split("_")

	if len(fields) != 4:
		return False

	date = fields[0]
	sn = fields[1]
	count = fields[2]
	flow_cell_id = fields[3]

	# Test the date
	if not date.isdigit() or len(date) != 6:
		return False

	# test if the sn is the serial number of our HiSeq
	if sn != hiseq_sn:
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

	hiseq_data_path = conf['hiseq.data.path']
	result = []

	files = os.listdir(hiseq_data_path)
	for f in files:
		if os.path.isdir(hiseq_data_path + '/' + f) and check_run_id(f, conf):
			result.append(f)

	return result

def get_run_number(run_id):
	"""Get the run number from the run id.

	Arguments:
		run_id: the run id
	"""

	return int(run_id.split('_')[2])

def send_mail_if_critical_free_space_available(conf):

	df = common.du(conf['hiseq.data.path'])
	free_space_threshold = long(conf['hiseq.critical.min.space'])
	if df < free_space_threshold:
		common.send_msg('[Aozan] Critical: Not enough disk space on Hiseq storage for current run',
					'There is only %.2f' % (df / (1024 * 1024 * 1024)) + ' Gb left for Hiseq run storage.' +
					' The current warning threshold is set to %.2f' % (df / (1024 * 1024 * 1024)) + ' Gb.', conf)

def send_mail_if_recent_run(run_id, secs, conf):

	run_path = conf['hiseq.data.path'] + '/' + run_id

	df = common.du(run_path)
	free_space_threshold = long(conf['hiseq.warning.min.space'])
	if df < free_space_threshold:
		common.send_msg('[Aozan] WARNING: Not enough disk space on Hiseq storage to save a 100b indexed pair-end run',
					'There is only %.2f' % (df / (1024 * 1024 * 1024)) + ' Gb left for Hiseq run storage.' +
					' The current warning threshold is set to %.2f' % (df / (1024 * 1024 * 1024)) + ' Gb.', conf)

	last = check_end_run_since(run_id, secs, conf)

	if last > 0:
		df = common.df(run_path) / (1024 * 1024 * 1024)
		du = common.du(run_path) / (1024 * 1024)
		common.send_msg('[Aozan] End of HiSeq run %04d' % get_run_number(run_id), 'A new run (' + run_id + ') has been terminated at ' +
					time.strftime("%a, %d %b %Y %H:%M:%S +0000", time.localtime(last)) + '.\n' +
					'Data for this run can be found at: ' + run_path +
					'\n\n%.2f Gb has been used,' % du + ' %.2f Gb still free.' % df, conf)
