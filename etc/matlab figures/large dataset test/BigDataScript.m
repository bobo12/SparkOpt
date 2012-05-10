% Tests on Reuters data and on synthetic data
% reuters1239 - big reuters
% standard-large1246 - big standard

% Results

%%Actual code
import java.util.ArrayList;

clear all
close all
clc

display('Reuters data');
x(1)= loadjson('reuters1239')
display('Synthetic data')
x(2)= loadjson('standard-large1246')

data = ArrayList();
data.add('ReutersData');
data.add('SyntheticData');

for j=1:2

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
    plot(y(y>0)/min(y(y>0)));
    xlabel('iteration');
    ylabel('loss / loss of last iteration (p/p*)');
    title('Decrease in loss function');

    %primal residual
    r = [x(j).iters.pres];
    figure(2)
    plot(r(r>0));
    xlabel('iteration');
    ylabel('primal residual');
    title('Primal residual evolution');

    %dual residual
    s = [x(j).iters.dres];
    figure(3)
    plot(s(s>0));
    xlabel('iteration');
    ylabel('dual residual');
    title('Dual residual evolution');

    %time of each iteration
    t = [x(j).iters.time];
    figure(4)
    plot(t(t>100))
    xlabel('iteration');
    ylabel('time');
    title('Time per iteration');

    %primal epsilon evolution
    peps = [x(j).iters.peps];
    figure(5)
    plot(peps(peps>0));
    xlabel('iteration');
    ylabel('primal epsilon');
    title('Primal epsilon evolution');

    %dual epsilon evolution
    deps = [x(j).iters.deps];
    figure(6)
    plot(deps(deps>0));
    xlabel('iteration');
    ylabel('dual epsilon');
    title('Dual epsilon evolution');

    %cardinality
    card = [x(j).iters.card];
    figure(7)
    plot(card(card>0));
    xlabel('iteration');
    ylabel('cardinality of current estimate z');
    title('Evolution of the estimate''s cardinality');

    Names = ArrayList();
    Names.add(['DecreaseLoss',data.get(j-1)]);
    Names.add(['PrimalResidual',data.get(j-1)]);
    Names.add(['DualResidual',data.get(j-1)]);
    Names.add(['TimePerIteration',data.get(j-1)]);
    Names.add(['PrimalEpsilon',data.get(j-1)]);
    Names.add(['DualEpsilon',data.get(j-1)]);
    Names.add(['Cardinality',data.get(j-1)]);

    for i = 1:7
        figure(i)
        saveName = Names.get(i-1);
        saveas(gcf, saveName, 'png');
        saveas(gcf, saveName, 'fig');
        close
    end
end

%compare loss decrease between the two data sets
lossReuters = [x(1).iters.loss];
lossSynth = [x(2).iters.loss];
figure()
l1 = lossReuters(lossReuters>0)/min(lossReuters(lossReuters>0));
plot(l1(1:16));
hold on
l2 = lossSynth(lossSynth>0)/min(lossSynth(lossSynth>0));
plot(l2(1:16),':');
legend('Reuters data','Synthetic data');
xlabel('iteration');
ylabel('loss / loss of last iteration');
title('Decrease in loss function for two different algorithms');
saveas(gcf, 'compareBigDataLoss', 'png');
saveas(gcf, 'compareBigDataLoss', 'fig');

%display success rate
display('Reuters data : {negative success rate, positive success rate, total success rate}')
disp(x(1).nsr)
disp(x(1).psr)
disp(x(1).tsr)

display('Synthetic data : {negative success rate, positive success rate, total success rate}')
disp(x(2).nsr)
disp(x(2).psr)
disp(x(2).tsr)