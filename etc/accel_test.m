a = loadjson('~/Desktop/accel')
r = loadjson('~/Desktop/reg')

figure
plot([a.iters.peps],'k')
plot([a.iters.pres],'b')
legend('peps','pres')
title('accelerated')


figure
plot([r.iters.peps],'k')
plot([r.iters.pres],'b')
legend('peps','pres')
title('regular gradient')



