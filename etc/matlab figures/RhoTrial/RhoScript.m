% RhoTrial
% correspond to  rhoTrial launcher configuration (launch ID 15)
% To run this experiment just run admm.trials.Launcher with the following
% arguments :
% local 15 fn RhoTrial nd 700 nf 50 ns 5 ni 50

% Then you have the RhoTrial's files and you can run this script

% experiment with numbers rho = [0.01,0.1,1,10]

% noise standard deviation = sqrt(0.1)

%Results

% -----------------------------------------------------------------
% RESULTS OF ALGORITHM
% positive success rate =
%      1
%
% negative success rate =
%      1
%
% total success rate =
%      1
%
% -----------------------------------------------------------------
% RESULTS OF ALGORITHM
% positive success rate =
%      1
%
% negative success rate =
%      1
%
% total success rate =
%      1
%
% -----------------------------------------------------------------
% RESULTS OF ALGORITHM
% positive success rate =
%      1
%
% negative success rate =
%      1
%
% total success rate =
%      1
%
% -----------------------------------------------------------------
% RESULTS OF ALGORITHM
% positive success rate =
%     0.8902
%
% negative success rate =
%      1
%
% total success rate =
%     0.9870

%%Actual code
clear all
close all
clc

import java.util.ArrayList;

lines = ArrayList();
lines.add('-');
lines.add('--');
lines.add(':');
lines.add('-.');
rhos = [0.01;0.1;1;10];

x(1)= loadjson('../../../RhoTrial0.01');
x(2)= loadjson('../../../RhoTrial0.1');
x(3)= loadjson('../../../RhoTrial1');
x(4)= loadjson('../../../RhoTrial10');

figure(1)
figure(2)
figure(3)
figure(4)
figure(5)
figure(6)
figure(7)

for j=1:4

    display('-----------------------------------------------------------------')
    display('RESULTS OF ALGORITHM');
    display('positive success rate = ');
    disp(x(j).psr);
    display('negative success rate = ');
    disp(x(j).nsr);
    display('total success rate = ');
    disp(x(j).tsr);

    %loss function
    y = [x(j).iters.loss];
    figure(1)
    plot(y(y>0)/min(y(y>0)),lines.get(j-1));
    hold on

    %primal residual
    r = [x(j).iters.pres];
    figure(2)
    plot(r(r>0),lines.get(j-1));
    hold on

    %dual residual
    s = [x(j).iters.dres];
    figure(3)
    plot(s(s>0),lines.get(j-1));
    hold on

    %time of each iteration
    t = [x(j).iters.time];
    figure(4)
    plot(t(t>100),lines.get(j-1))
    hold on

    %primal epsilon evolution
    peps = [x(j).iters.peps];
    figure(5)
    plot(peps(peps>0),lines.get(j-1));
    hold on

    %dual epsilon evolution
    deps = [x(j).iters.deps];
    figure(6)
    plot(deps(deps>0),lines.get(j-1));
    hold on

    %cardinality
    card = [x(j).iters.card];
    figure(7)
    plot(card(card>0),lines.get(j-1));
    hold on

end

Names = ArrayList();
Names.add('DecreaseLoss');
Names.add('PrimalResidual');
Names.add('DualResidual');
Names.add('TimePerIteration');
Names.add('PrimalEpsilon');
Names.add('DualEpsilon');
Names.add('Cardinality');

figure(1)
xlabel('iteration');
ylabel('p/p*');
title('Decrease in loss function');

%primal residual
figure(2)
xlabel('iteration');
ylabel('primal residual');
title('Primal residual evolution');

%dual residual
figure(3)
xlabel('iteration');
ylabel('dual residual');
title('Dual residual evolution');

%time of each iteration
figure(4)
xlabel('iteration');
ylabel('time');
title('Time per iteration');

%primal epsilon evolution
figure(5)
xlabel('iteration');
ylabel('primal epsilon');
title('Primal epsilon evolution');

%dual epsilon evolution
figure(6)
xlabel('iteration');
ylabel('dual epsilon');
title('Dual epsilon evolution');

%cardinality
figure(7)
xlabel('iteration');
ylabel('cardinality of current estimate z');
title('Evolution of the estimate''s cardinality');

for i = 1:7
    figure(i)
    legend('rho=0.01','rho=0.1','rho=1','rho=10')
    saveName = Names.get(i-1);
    saveas(gcf, saveName, 'png');
    saveas(gcf, saveName, 'fig');
    close
end

%%Now look at results as a function nbSlices

psr = zeros(4,1);
nsr = zeros(4,1);
tsr = zeros(4,1);
for j=1:4
    psr(j)=x(j).psr;
    nsr(j)=x(j).nsr;
    tsr(j)=x(j).tsr;
end

%positive success rate
figure(8)
plot(rhos,psr);
xlabel('rho');
ylabel('positive success rate');
title('Positive success rate and rho');

%negative success rate
figure(9)
plot(rhos,nsr);
xlabel('rho');
ylabel('negative success rate');
title('Negative success rate and rho');

%total success rate
figure(10)
plot(rhos,tsr);
xlabel('rho');
ylabel('total success rate');
title('Total success rate and rho');

Names = ArrayList();
Names.add('PosSuccNbSlices');
Names.add('NegSuccNbSlices');
Names.add('TotSuccNbSlices');

for i = 8:10
    figure(i)
    saveName = Names.get(i-8);
    saveas(gcf, saveName, 'png');
    saveas(gcf, saveName, 'fig');
    close
end