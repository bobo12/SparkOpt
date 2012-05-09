a = loadjson('~/Desktop/accel');
r = loadjson('~/Desktop/reg');

figure;
plot([a.iters.peps],'k');
hold on;
plot([a.iters.pres],'b');
plot([r.iters.peps],'g');
plot([r.iters.pres],'r');
legend('accel: peps','accel: pres','reg: peps','reg: pres');
title('accelerated vs. gradient');
xlabel('iteration #')
ylabel('Cost')


figure;
plot([a.iters.time],'k');
hold on;
plot([r.iters.time],'b');
legend('accel', 'reg');
title('accelerated vs. gradient time per iterations');
xlabel('iteration #');
ylabel('time (ms)');
