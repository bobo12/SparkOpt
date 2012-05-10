from admm import *
import pylab as lab



def features_trial(n_slaves = 1):
    #    launch_cluster(n_slaves)
    #    post_init(big_data = False, small_data = True)
    output_path = "/root/trial"
    n_features = [10, 100, 1000]
    n_docs = 1000
    n_slices = n_slaves * 2
    host = make_master()
    file_path = "/user/root/smalldata"
    topic_index = 0
    n_iters = 3
    prog_name = 'admm.opt.SLRSparkImmutable'
    ## run_spark_program(prog_name,
    ##                   host,
    ##                   n_docs,
    ##                   ','.join(map(str,n_features)),
    ##                   n_slices,
    ##                   topic_index,
    ##                   n_iters,
    ##                   file_path,
    ##                   output_path)
    run_cmd([rsync_remote('/Users/jdr/Desktop/sync%i' % feat,
                 '%s%i' % (output_path, feat),
        False) for feat in n_features])
    stop_cluster()
    
def sparsity_trial(sparsities=[.01,.1,1.]):
    launch_cluster(10)
    init_sync()
    output_path = '/root/sparse'
    launch_trial(1, output_path, *sparsities)
    run_cmd(rsync_remote('/Users/jdr/Desktop/sync',output_path, False))
    destroy_cluster()
    
    
                      
    
    
    
def run_samples(n_slaves = 1):
    out_file = '/root/outfile'
    #    launch_cluster(n_slaves)
    #    start_cluster()
    post_init(small_data = False, big_data = False)
    n_features = [10, 20 , 50]
    n_iters = 3
    prog_name = 'admm.test.TestAlgorithm'
    host = make_master()
    cmd = lambda f: '%s 1000 %i .2 .2 .0002 1.0 .5 %i 20 %s %s' % (prog_name, f, n_iters, '%s_%i' % (out_file, f), host)
    for f in n_features:
        run_spark(cmd(f))
        run_cmd(rsync_remote('/Users/jdr/Desktop/synced_%i' % f, '%s_%i' % (out_file, f), False))
    stop_cluster()

def local_test():
    fn = '/Users/jdr/Desktop/jazz'
    n_docs = 200
    n_features = 20
    n_slices = 5
    n_iters = 100
    launch_local(1, sparsities = '.1', fn=fn, nd=n_docs, nf = n_features, ns = n_slices, ni = n_iters)
    x = load_output(fn)
    run_cmd('rm {0}'.format(fn))
    lab.plot(dejunk([i.pres for i in x.iters]), label='p Res')
    lab.hold(True)
    lab.plot(dejunk([i.peps for i in x.iters]), label='p Eps')
    lab.legend()
    lab.figure()
    lab.plot(dejunk([i.dres for i in x.iters]), label='d Res')
    lab.hold(True)
    lab.plot(dejunk([i.deps for i in x.iters]), label='d Eps')
    lab.legend()
    lab.figure()
    lab.plot(dejunk([i.time for i in x.iters]), label='d Eps')
    lab.show()

def remote_test():
    launch_cluster(10)
    post_init(False,False)
    localfn = '/Users/jdr/Desktop/jazz'
    remotefn = '/root/data'    
    launch_trial(4, remotefn )
    run_cmd(rsync_remote(localfn,remotefn, False))
    stop_cluster()
    do_stuff(localfn)
    
def simple_test():
    localfn = '/Users/jdr/Desktop/jazz'
    nd = 1000
    nf = 50
    ns = 10
    ni = 100
    lam = .01
    launch_local(200, nd = nd, nf = nf, ns = ns, ni = ni, lam = lam, fn =  localfn)
    stats = get_stat(localfn)
    lab.plot(dejunk([i.pres for i in stats.iters]), label='p Res')
    lab.hold(True)
    lab.plot(dejunk([i.peps for i in stats.iters]), label='p Eps')
    lab.legend()
    lab.figure()
    lab.plot(dejunk([i.dres for i in stats.iters]), label='d Res')
    lab.hold(True)
    lab.plot(dejunk([i.deps for i in stats.iters]), label='d Eps')
    lab.legend()
    lab.figure()
    lab.plot(dejunk([i.time for i in stats.iters]), label='time')
    lab.title('time')
    lab.show()
    
def simple_hdfs_remote_test():
    localfn = '/Users/jdr/Desktop/jazz'
    remotefn = '/root/data'
    launch_cluster(3)
    post_init(False,False)
    store_hdfs('https://s3.amazonaws.com/admmdata/A.data', 'A.data')
    store_hdfs('https://s3.amazonaws.com/admmdata/A.data', 'A.data')
    nd = 1000
    nf = 50
    ns = 10
    ni = 100
    lam = .01
    launch_trial(200, nd = nd, nf = nf, ns = ns, ni = ni, lam = lam, fn =  localfn)
    run_cmd(rsync_remote(localfn,remotefn, False))
    stop_cluster()
    stats = get_stat(localfn)

    
def simple_hdfs_test(local = True, launch = False, stop = False, destroy=False, localfn = '/Users/jdr/Desktop/jazz', instance = 'm1.small'):
    launch_id = 201
    if local:
        remotefn = '/Users/jdr/Desktop/jazz'
        A_path = '/Users/jdr/Documents/github-projects/SparkOpt/etc/A.100000x10000.data'
        b_path = '/Users/jdr/Documents/github-projects/SparkOpt/etc/b.100000.data'
        hdfs_path = "/usr/local/Cellar/hadoop/1.0.1/libexec"
    else:
        remotefn = '/root/data'
        A_path = '/user/root/A.data'
        b_path = '/user/root/b.data'
        hdfs_path = "/root/persistent-hdfs"
    if launch:
        launch_cluster(10, i_type = instance)
        post_init(False,False)
        store_hdfs('https://s3.amazonaws.com/admmdata/A.100000x10000.data', 'A.data')
        store_hdfs('https://s3.amazonaws.com/admmdata/b.100000.data', 'b.data')
    nd = 100000
    nf = 2000
    ns = 100
    ni = 2
    lam = 1.0
    if local:
        launch_local(launch_id, nd = nd, nf = nf, ns = ns, ni = ni, lam = lam, fn =  remotefn, apath = A_path, bpath = b_path, hdfs=hdfs_path)
    else:
        launch_trial_kws(launch_id, nd = nd, nf = nf, ns = ns, ni = ni, lam = lam, fn =  remotefn, apath = A_path, bpath = b_path, hdfs=hdfs_path)
        run_cmd(rsync_remote(localfn,remotefn, False))
    if stop:
        stop_cluster()
    if destroy:
        destroy_cluster()
    return get_stat(localfn)

def reuters_hdfs_test(local = True, launch = False, stop = False, destroy=False, localfn = '/Users/jdr/Desktop/jazz', instance = 'm1.small'):
    launch_id = 300
    if local:
        remotefn = '/Users/jdr/Desktop/jazz'
        path = '/Users/jdr/Documents/github-projects/SparkADMM/etc/data/labeled_rcv1.admm.data'
        hdfs_path = "/usr/local/Cellar/hadoop/1.0.1/libexec"
    else:
        remotefn = '/root/data'
        path = '/root/data'
        hdfs_path = "/root/persistent-hdfs"
    if launch:
        launch_cluster(10, i_type = instance)
        post_init(False,False)
        store_hdfs('https://s3.amazonaws.com/admmdata/A.100000x10000.data', 'A.data')
        store_hdfs('https://s3.amazonaws.com/admmdata/b.100000.data', 'b.data')
    nd = 2000
    nf = 500
    ns = 5
    ni = 50
    lam = .1
    if local:
        launch_local(launch_id, nd = nd, nf = nf, ns = ns, ni = ni, lam = lam, fn =  remotefn, path=path, hdfs=hdfs_path)
    else:
        launch_trial_kws(launch_id, nd = nd, nf = nf, ns = ns, ni = ni, lam = lam, fn =  remotefn, path=path, hdfs=hdfs_path)
        run_cmd(rsync_remote(localfn,remotefn, False))
    if stop:
        stop_cluster()
    if destroy:
        destroy_cluster()
    return get_stat(localfn)
    
def instance_size_test():
    """docstring for instance_size_test"""
    instances = ['m1.medium', 'm1.large']
    launcher = [False, True]
    for ins, do_launch in zip(instances, launcher):
        simple_hdfs_test(local=False, launch=do_launch, stop = False, destroy = True, localfn = "/Users/jdr/Desktop/ins-test-large-%s" % ins, instance = ins)
    
def broke():
    """docstring for instance_size_test"""
    instances = [ 'm1.large']
    for ins in instances:
        post_init(False,False)
        store_hdfs('https://s3.amazonaws.com/admmdata/A.data', 'A.data')
        store_hdfs('https://s3.amazonaws.com/admmdata/b.data', 'b.data')
        simple_hdfs_test(local=False, launch=False, stop = False, destroy = True, localfn = "/Users/jdr/Desktop/ins-test-%s" % ins, instance = ins)
    
    
def local_test():
    """docstring for remote_test"""
    localfn = '/Users/jdr/Desktop/jazz'
    launch_local(4,**{localfn: " "})
    do_stuff(localfn)
    
def get_stat(fn, delete=False):
    x = load_output(fn)
    if delete:
        run_cmd('rm {0}'.format(fn))
    return x    
    
def do_stuff(x):
    """docstring for do_stuff"""
    lab.plot(dejunk([i.pres for i in x.iters]), label='p Res')
    lab.hold(True)
    lab.plot(dejunk([i.peps for i in x.iters]), label='p Eps')
    lab.legend()
    lab.figure()
    lab.plot(dejunk([i.dres for i in x.iters]), label='d Res')
    lab.hold(True)
    lab.plot(dejunk([i.deps for i in x.iters]), label='d Eps')
    lab.legend()
    lab.figure()
    lab.plot(dejunk([i.time for i in x.iters]), label='d Eps')
    lab.show()
    
if __name__ == '__main__':
    #instance_size_test()
    #broke()
    #simple_hdfs_test()
    do_stuff(reuters_hdfs_test())

