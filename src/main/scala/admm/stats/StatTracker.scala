package admm.stats
import scala.collection.mutable.ListBuffer

/**
 * User: jdr
 * Date: 5/7/12
 * Time: 12:48 AM
 */


class Tracker extends Serializable{
  var startTime = -1L
  var time = -1L
  def start {
    startTime = System.currentTimeMillis()
  }
  def stop {
    time = System.currentTimeMillis() - startTime
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
}

object InnerTracker {
  def fac = new InnerTracker
}

class StatTracker extends IterTracker[InnerTracker](InnerTracker.fac _) {
  override def toString = {
    iters.zipWithIndex.map{
      case (inner, ind) => {
        val titles = List[String]("time","pres", "peps", "dres", "deps")
        val values = List(inner.time.toDouble, inner.pRes, inner.pEps, inner.dRes, inner.dEps)
        val zips = titles.zip(values)
        val maps = zips.map{
          case (title, value) => {
            "%s: %f".format(title, value)
          }
        }
        val combine = maps.toArray.deepMkString("\n")
        "Inner Loop %d\n%s"
          .format(ind, combine)
      }
    }.toArray.deepMkString("\n")
  }
}
