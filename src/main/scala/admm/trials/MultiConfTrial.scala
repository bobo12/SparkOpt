package admm.trials

import admm.opt.SLRConfig
import admm.opt.Experiment
import admm.data.ParallelizedSyntheticData.generate_data

/**
 * User: jdr
 * Date: 5/8/12
 * Time: 12:42 PM
 */

class MultiConfTrial extends Launchable{
  override def launch(args: Array[String]) {
    val fn = args(0)
    val confs = (1 to 3).map(_ => new SLRConfig)
    confs.foreach(conf => {
      conf.nDocs = 2000
      conf.nFeatures = 100
      conf.nSlices = 20
      conf.rho = 1
      conf.topicId = 0
    })
    val lambdas = List(.0001, .1, 1.0)
    confs.zip(lambdas).foreach{
      case (conf, l) => {
        conf.lam = l
      }
    }
    val rdd = generate_data(sc, confs(0), .5, .5)
    val exp = new Experiment(rdd, confs, fn)
    exp.serializeSolution
    sc.stop()
  }

  def launchID = 4

}
