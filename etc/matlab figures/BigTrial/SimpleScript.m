% Big Trial 
% correspond to BigTrial launcher configurration
% To run this experiment just run admm.trials.Launcher with the following
% arguments :
% local 13 fn BigTrial sparsityA 0.5 sparsityW 0.5 nd 500 nf 50 ns 10 k 3 p 6 ns 5 lam 1 ni 100
% Then you have the BigTrial file and you can run this script

% experiment with numbers
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

import java.util.ArrayList;

x = loadjson('../../../BigTrial');

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

%primal epsilon evolution
peps = [x.iters.peps];
figure(5)
plot(peps(peps>0));
xlabel('iteration');
ylabel('primal epsilon');
title('Primal epsilon evolution');

%dual epsilon evolution
deps = [x.iters.deps];
figure(6)
plot(deps(deps>0));
xlabel('iteration');
ylabel('dual epsilon');
title('Dual epsilon evolution');

Names = ArrayList();
Names.add('DecreaseLoss');
Names.add('PrimalResidual');
Names.add('DualResidual');
Names.add('TimePerIteration');
Names.add('PrimalEpsilon');
Names.add('DualEpsilon');

for i = 1:6 
    figure(i)
    saveName = Names.get(i-1);
    saveas(gcf, saveName, 'png');       
    saveas(gcf, saveName, 'fig');
    close 
end

