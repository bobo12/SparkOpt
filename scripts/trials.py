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
#    launch_cluster(12)
    #init_sync()
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
