% Big Trial : experiment with numbers
% lambda = 1
% nDocs = 500
% nFeatures = 50
% maxIters = 100
% nSlices = 5
% absTol = 1.0000e-004
% relTol = 0.0100
% rho = 1
% time = 10636
% topicId = 0

x = loadjson('BigTrial');

%loss function
y = [x.iters.loss];
figure(1)
plot(y(y>0)/min(y(y>0)));
xlabel('iteration');
ylabel('p/p*');
title('Decrease in loss function');

%primal residual
r = [x.iters.pres];
figure(2)
plot(r(r>0));
xlabel('iteration');
ylabel('primal residual');
title('Primal residual evolution');

%dual residual
s = [x.iters.dres];
figure(3)
plot(s(s>0));
xlabel('iteration');
ylabel('dual residual');
title('Dual residual evolution');

%time of each iteration
t = [x.iters.time];
figure(4)
plot(t(t>100))
xlabel('iteration');
ylabel('time');
title('Time per iteration');

