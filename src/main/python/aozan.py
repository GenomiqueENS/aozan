'''
Aozan main file.
Created on 25 oct. 2011

@author: Laurent Jourdren
'''

import sys, os
import common, hiseq, sync_run, demux_run

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

# Enable debug mode 
debug = True

# Main function
if __name__ == "__main__":

    conf = {}

    if debug:
        conf = common.load_test_conf()
    else:
        if len(sys.argv) < 2:
            print "No configuration file set in command line.\nSyntax: aozan.py conf_file"
            sys.exit(1)
        else:
            conf = common.load_conf(sys.argv[1])

    lock_file_path = conf['lock.file']

    # Run only if there is no lock
    if not os.path.exists(lock_file_path):

        create_lock_file(lock_file_path)

        print "Aozan v0.01"
        sync_run_ids_done = sync_run.load_processed_run_ids(conf)


        # Get the list of run available on HiSeq output
        for run_id in hiseq.get_available_run_ids(conf):
            print "Found " + run_id
            if hiseq.check_run_id(run_id, conf) and run_id not in sync_run_ids_done:
                print "Synchronize " + run_id
                #os.system(process_run_script + " " + str(run_id) + " " + hiseq_data_path + " " + work_data_path + " " + storage_data_path)
                #if sync_run.sync(run_id, conf):
                    #sync_run.add_run_id_to_processed_run_ids(run_id)
                    #sync_run_ids_done.add(run_id)

        demux_run_ids_done = demux_run.load_processed_run_ids(conf)

        for run_id in (sync_run_ids_done - demux_run_ids_done):
                print "Demux " + run_id
                if not demux_run.demux(run_id, conf):
                    print "Error in demux"
                    break

        delete_lock_file(lock_file_path)

        print "End of Aozan."
    else:
        print "A lock file exists."
