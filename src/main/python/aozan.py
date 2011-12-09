'''
Aozan main file.
Created on 25 oct. 2011

@author: Laurent Jourdren
'''

import sys, os
import common, hiseq_run, sync_run, demux_run, qc_run
from java.util import Locale

def create_lock_file(lock_file_path):
    """Create the lock file.

    Arguments:
        lock_file_path path of the lock file
    """

    f = open(lock_file_path, 'w')
    f.close()

def delete_lock_file(lock_file_path):
    """Create the lock file.

    Arguments:
        lock_file_path path of the lock file
    """

    os.unlink(lock_file_path)


# Main function
if __name__ == "__main__":

    # Create configuration
    conf = {}
    common.set_default_conf(conf)
    
    # Use default (C) locale
    Locale.setDefault(Locale.US)

    if len(sys.argv) < 1:
        print "No configuration file define in command line.\nSyntax: aozan.py conf_file"
        sys.exit(1)
    else:
        common.load_conf(conf, sys.argv[0])

    # End of Aozan if aozan is not enable
    if conf['aozan.enable'].lower().strip() == 'false':
        sys.exit(0)

    # Check critical free space available
    hiseq_run.send_mail_if_critical_free_space_available(conf)

    lock_file_path = conf['lock.file']

    # Run only if there is no lock
    if not os.path.exists(lock_file_path):

        create_lock_file(lock_file_path)

        print "Aozan v0.01"


        #
        # Discover hiseq run done
        #

        hiseq_run_ids_done = hiseq_run.load_processed_run_ids(conf)
        
        if conf['hiseq.step'].lower().strip() == 'true':
            for run_id in (hiseq_run_ids_done - hiseq_run.get_available_run_ids(conf)):
                print "Find a new run " + run_id
                hiseq_run.send_mail_if_recent_run(run_id, 12 * 3600, conf)
                hiseq_run.add_run_id_to_processed_run_ids(run_id, conf)
                hiseq_run_ids_done.add(run_id)

        #
        # Sync hiseq and storage
        #

        sync_run_ids_done = sync_run.load_processed_run_ids(conf)

        # Get the list of run available on HiSeq output
        if conf['sync.step'].lower().strip() == 'true':
            for run_id in (hiseq_run_ids_done - sync_run_ids_done):
                print "Synchronize " + run_id
                if sync_run.sync(run_id, conf):
                        sync_run.add_run_id_to_processed_run_ids(run_id, conf)
                        sync_run_ids_done.add(run_id)

        #
        # Demultiplexing
        #

        demux_run_ids_done = demux_run.load_processed_run_ids(conf)

        if conf['demux.step'].lower().strip() == 'true':
            for run_id in (sync_run_ids_done - demux_run_ids_done):
                    print "Demux " + run_id
                    if demux_run.demux(run_id, conf):
                        demux_run.add_run_id_to_processed_run_ids(run_id, conf)
                        demux_run_ids_done.add(run_id)

        #
        # Quality control
        #

        qc_run_ids_done = qc_run.load_processed_run_ids(conf)
        
        if conf['qc.step'].lower().strip() == 'true':
            for run_id in (demux_run_ids_done - qc_run_ids_done):
                    print "Qc " + run_id
                    if qc_run.qc(run_id, conf):
                        qc_run.add_run_id_to_processed_run_ids(run_id, conf)
                        qc_run_ids_done.add(run_id)

        delete_lock_file(lock_file_path)

        print "End of Aozan."
    else:
        print "A lock file exists."
        common.error('[Aozan] A lock file exists', 'A lock file exist on gna at ' + conf['lock.file'] +
                     ". Please investigate last error and then remove the lock file.", conf['aozan.var.path'] + '/aozan.lasterr', conf)
