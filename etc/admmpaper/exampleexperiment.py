def simple_hdfs_remote_test():
    localfn = '/Users/jdr/Desktop/jazz'
    remotefn = '/root/data'
    launch_cluster(3)
    post_init(False,False)
    store_hdfs('s3.amazonaws.com/admmdata/A.data', 'A.data')
    store_hdfs('s3.amazonaws.com/admmdata/A.data', 'A.data')
    launch_trial(200, nd=nd, nf=nf, fn=localfn)
    run_cmd(rsync_remote(localfn,remotefn, False))
    stop_cluster()
    stats = get_stat(localfn)
