import sys
sys.path.append('boto-2.0b2')
import subprocess
import boto
import time
import os


MAIN_DIR = '../'

REMOTE_SPARKOPT = '/root/SparkOpt/'

IDENTITY_FILE = '/Users/jdr/.ec2/admm.pem'

address = None
def get_address():
    global address
    if address:
        return address
    # ... figure out result
    conn = boto.connect_ec2()
    rsvs = conn.get_all_instances()
    for rsv in rsvs:
        if 'master' in rsv.groups[0].id and len(rsv.instances[0].public_dns_name) > 5:
            address = rsv.instances[0].public_dns_name
            break
    if address is None:
        raise Exception("no servers!")
    return address

def make_master():
    return '1@%s:5050' % get_address()

def concat_commands(cmds):
    print cmds
    if isinstance(cmds,list):
        return ';'.join(cmds)
    return cmds

def run_cmd(cmd):
    cmds = concat_commands(cmd)
    print cmds
    subprocess.check_call(cmds, shell=True)

def copy_dir_remote(path):
    return remote_cmd('/root/mesos-ec2/copy-dir %s' % path)

def open_web_ui():
    run_cmd('open http://%s:8080' % get_address())

def remote_dir_address(add_dir):
    return 'root@%s:%s' % (get_address(), add_dir)

def quotize(ln):
    return "'%s'" % ln

def rsync(from_dir, to_dir):
    return ("rsync -rv -e 'ssh -o StrictHostKeyChecking=no -i %s' %s %s") % (IDENTITY_FILE, quotize(from_dir), quotize(to_dir))

def rsync_remote(local, remote, to_remote = True):
    remote = remote_dir_address(remote)
    if to_remote:
        to_path = remote
        from_path = local
    else:
        to_path = local
        from_path = remote
    return rsync(from_path, to_path)
        

def sync_target():
    return rsync_remote(os.path.join(MAIN_DIR, 'target'), REMOTE_SPARKOPT)

def sync_jars():
    return rsync_remote(os.path.join(MAIN_DIR, 'lib'), REMOTE_SPARKOPT)

def remote_cmd(cmd):
    return 'ssh -i %s root@%s "%s"' % (IDENTITY_FILE, get_address(), concat_commands(cmd))


def init_sync():
    run_cmd([sync_target(), 
             sync_jars(), 
             copy_dir_remote(REMOTE_SPARKOPT)])

def code_sync():
    run_cmd([sync_target(), copy_dir_remote(os.path.join(REMOTE_SPARKOPT, 'target'))])

def run_spark(cmd, update = False):
    if update:
        code_sync()
    run_cmd(remote_cmd('/root/spark/run %s' % cmd))    

def run_spark_program(prog_name, *args, **kwargs):
    update = kwargs.get('update',False)
    if update:
        code_sync()
    run_cmd(remote_cmd('/root/spark/run %s %s' % (prog_name, ' '.join(map(str,args)))))
    
def launch_trial(trial_id, *args):
    run_spark_program("admm.trials.Launcher", make_master(), trial_id, *args, update=True)
    
def start_stop_trial(trial_id, *args):
    start_cluster()
    launch_trial(trial_id, *args)
    stop_cluster()

def store_hdfs(web_address, local_path):
    pull = 'wget %s' % web_address
    store = '/root/persistent-hdfs/bin/hadoop fs -put %s %s' % (web_address.split('/')[-1], local_path)
    delete = 'rm -rf %s' % web_address.split('/')[-1]
    run_cmd(remote_cmd([pull, store, delete]))


def store_env_var(name, value):
    run_cmd(remote_cmd('echo export %s=%s >> root/.bashrc' % (name, value)))

def add_master_env_var():
    store_env_var('master', '$(cat /root/mesos-ec2/cluster-url)')


def post_init(big_data = False, small_data = True):
    if small_data:
        store_hdfs('https://s3.amazonaws.com/admmdata/labeled_rcv1.admm.data', 'smalldata')
    if big_data:
        store_hdfs('https://s3.amazonaws.com/admmdata/bigdata_labeled.svm', 'bigdata')
    init_sync()


def run_admm_opt(file, nDocs, nFeatures, nSlices, topicIndex, nIters, update=False):
    run_spark('admm.opt.SLRSparkImmutable %s %i %i %i %i %i' % (make_master(), nDocs ,nFeatures, nSlices, topicIndex, nIters), update=update)

def launch_cluster(n_slaves = 1):
    run_cmd('./mesos-ec2 -s %i launch admm' % n_slaves)

def stop_cluster():
    run_cmd('./mesos-ec2 stop admm')

def start_cluster():
    run_cmd('./mesos-ec2 start admm')
def destroy_cluster():
    run_cmd('./mesos-ec2 destroy admm')
