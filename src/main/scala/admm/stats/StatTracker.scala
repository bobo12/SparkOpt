package admm.stats
import scala.collection.mutable.ListBuffer
import cern.colt.matrix.tdouble.DoubleMatrix1D
import collection.immutable.HashMap
import com.twitter.json.{JsonSerializable, JsonQuoted, Json}
import admm.opt.SLRConfig

/**
 * User: jdr
 * Date: 5/7/12
 * Time: 12:48 AM
 */



class Tracker extends Serializable with JsonSerializable{
  var startTime = -1L
  var time = -1L
  def start {
    startTime = System.currentTimeMillis()
  }
  def stop {
    time = System.currentTimeMillis() - startTime
  }
  def toJson(): String = {
    Json.build(jsonMap).toString()
  }
  def jsonMap: Map[Any, Any] = {
    HashMap("time"-> time)
  }
}

object Tracker {
  def fac = new Tracker
}

class IterTracker[A<:Tracker](fac: () => A = Tracker.fac _) extends Tracker {
  var iters: ListBuffer[A] = ListBuffer[A]()

  def cur = iters.last

  def newIter {
    val iter = fac()
    iters += iter
    iter.start
  }

  def endIter {
    if (iters.size > 0) cur.stop
  }
  def resetIter {
    endIter
    newIter
  }
  override def jsonMap: Map[Any, Any] = {
    val map = super.jsonMap
    val iterMaps = iters.map(_.jsonMap)
    map + (("iters", iterMaps))
  }

}

object IterTracker {
  def fac[A<:Tracker](innerFac: () => A) = new IterTracker[A](innerFac)
}
class MapTracker extends IterTracker[GradientTracker](GradientTracker.fac _)

object MapTracker {
  def fac = new MapTracker
}

class XTracker extends IterTracker[MapTracker](MapTracker.fac _)

object XTracker {
  def fac = new XTracker
}


class GradientTracker extends Tracker {
  var gradientNorm = -1d
}
object GradientTracker {
  def fac = new GradientTracker
}


class InnerTracker extends Tracker {
  val xTracker = new XTracker
  val uTracker = new IterTracker[Tracker]
  var pRes = -1.
  var pEps = -1.
  var dRes = -1.
  var dEps = -1.
  var card = -1
  var loss = -1.
  override def jsonMap = {
    val map = super.jsonMap
    val titles = List[String]("time","pres", "peps", "dres", "deps", "card", "loss")
    val values = List(time.toDouble, pRes, pEps, dRes, dEps, card, loss)
    val zips = HashMap(titles.zip(values): _*)
    map ++ zips
  }
}

object InnerTracker {
  def fac = new InnerTracker
}

class StatTracker extends IterTracker[InnerTracker](InnerTracker.fac _) {
  var z: DoubleMatrix1D = null
  var conf: SLRConfig = null
  override def jsonMap = {
    val map = super.jsonMap
    val zArr = z.toArray
    val zConf = conf.jsonMap
    (map + (("z", zArr))) ++ zConf
  }
  def dumpToFile {
    val fn = conf.getWriter
    fn.write(toJson())
    fn.close()
  }
}
