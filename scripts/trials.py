from admm import *



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

