package admm.trials

/**
 * User: jdr
 * Date: 5/7/12
 * Time: 11:39 AM
 */
import admm.stats.SuccessRate.successRate
import admm.data.ParallelizedSyntheticData.generate_data
import admm.opt.SLRConfig

class SparseTrial extends SLRLaunchable {

  def launchWithConfig(kws: Map[String, String], conf: SLRConfig) {
    val sparsities = kws.get("sparsities").get.split(",").map(_.toDouble)
    sparsities.foreach(sp => {
      val sln = successRate(generate_data(sc, conf, sp, .5), conf = conf).toString()
    })
  }

  def launchID = 1

}
