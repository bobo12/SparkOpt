import sys
sys.path.append('boto-2.0b2')
import subprocess
import boto
import time
import os


MAIN_DIR = '../'

REMOTE_SPARKOPT = '/root/SparkOpt/'

address = None

def init_address(name = 'admm'):
    conn = boto.connect_ec2()
    rsvs = conn.get_all_instances()
    add = None
    for rsv in rsvs:
        if 'master' in rsv.groups[0].id:
            add = rsv.instances[0].public_dns_name
    if add is None:
        raise Exception("no shit!")
    return add

address = init_address()

def concat_commands(cmds):
    if isinstance(cmds,list):
        return ';'.join(cmds)
    return cmds

def run_cmd(cmd):
    subprocess.check_call(concat_commands(cmd), shell=True)

def copy_dir_remote(path):
    return remote_cmd('~/mesos-ec2/copy-dir %s' % path)

def open_web_ui():
    run_cmd('open http://%s:8080' % address)

def rsync(from_dir, to_dir):
    return (("rsync -rv -e 'ssh -o StrictHostKeyChecking=no -i %s' " + 
                "'%s' 'root@%s:%s'") % ('/Users/jdr/.ec2/admm.pem', from_dir, address, to_dir))

def sync_target():
    return rsync(os.path.join(MAIN_DIR, 'target'), REMOTE_SPARKOPT)

def sync_jars():
    return rsync(os.path.join(MAIN_DIR, 'lib'), REMOTE_SPARKOPT)

def remote_cmd(cmd):
    return 'ssh -i %s root@%s "%s"' % ('/Users/jdr/.ec2/admm.pem', address, cmd)


def init_sync():
    run_cmd([sync_target(), 
             sync_jars(), 
             copy_dir_remote(REMOTE_SPARKOPT)])

def code_sync():
    run_cmd([sync_target(), copy_dir_remote(os.path.join(REMOTE_SPARKOPT, 'target'))])

def run_spark(cmd):
    run_cmd(remote_cmd('/root/spark/run %s' % cmd))    
