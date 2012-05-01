import sys
sys.path.append('boto-2.0b2')
import subprocess
import boto
import time
import os


MAIN_DIR = '../'

REMOTE_SPARKOPT = '/root/SparkOpt/'

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

def rsync(from_dir, to_dir):
    return (("rsync -rv -e 'ssh -o StrictHostKeyChecking=no -i %s' " + 
                "'%s' 'root@%s:%s'") % ('/Users/jdr/.ec2/admm.pem', from_dir, get_address(), to_dir))

def sync_target():
    return rsync(os.path.join(MAIN_DIR, 'target'), REMOTE_SPARKOPT)

def sync_jars():
    return rsync(os.path.join(MAIN_DIR, 'lib'), REMOTE_SPARKOPT)

def remote_cmd(cmd):
    return 'ssh -i %s root@%s "%s"' % ('/Users/jdr/.ec2/admm.pem', get_address(), concat_commands(cmd))


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

def store_hdfs(web_address, local_path):
    pull = 'wget %s' % web_address
    store = '/root/persistent-hdfs/bin/hadoop fs -put %s %s' % (web_address.split('/')[-1], local_path)
    delete = 'rm -rf %s' % web_address.split('/')[-1]
    run_cmd(remote_cmd([pull, store, delete]))


def store_env_var(name, value):
    run_cmd(remote_cmd('echo export %s=%s >> root/.bashrc' % (name, value)))

def add_master_env_var():
    store_env_var('master', '$(cat /root/mesos-ec2/cluster-url)')


def post_init(big = False):
    store_hdfs('https://s3.amazonaws.com/admmdata/labeled_rcv1.admm.data', 'smalldata')
    if big:
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

def run_trial(n_slaves = 1):
    launch_cluster(n_slaves)
    post_init(False)
    run_admm_opt('',10000, 1000, 50, 0, 2, True)
    destroy_cluster()
