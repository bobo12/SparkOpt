
import pylab as pl

pl.plot(dejunk([i.peps for i in f.iters]))
pl.hold(True)
pl.plot(dejunk([i.pres for i in f.iters]))
pl.show()
