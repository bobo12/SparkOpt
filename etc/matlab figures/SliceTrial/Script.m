% Big Trial
% correspond to SliceTrial3 launcher configurration (launch ID 13)
% To run this experiment just run admm.trials.Launcher with the following
% arguments :
% local 12 fn SliceTrial sparsityA 0.5 sparsityW 0.5 nd 700 nf 50 ns 10
% slices 1,5,10 lam 1 ni 100

% Then you have the SliceTrial1, SliceTrial5, SliceTrial10 file and you can run this script

% experiment with numbers

% noise standard deviation = sqrt(0.1)

% Result
% -----------------------------------------------------------------
% PROPORTION OF POSITIVE LABELS IN SYNTHEIC DATA
%     0.5014
% 
% RESULTS OF ALGORITHM
% positive success rate = 
%     0.9801
% 
% negative success rate = 
%     0.9713
% 
% total success rate = 
%     0.9757
% 
% -----------------------------------------------------------------
% PROPORTION OF POSITIVE LABELS IN SYNTHEIC DATA
%     0.5171
% 
% RESULTS OF ALGORITHM
% positive success rate = 
%     0.9696
% 
% negative success rate = 
%     0.9763
% 
% total success rate = 
%     0.9729
% 
% -----------------------------------------------------------------
% PROPORTION OF POSITIVE LABELS IN SYNTHEIC DATA
%     0.5300
% 
% RESULTS OF ALGORITHM
% positive success rate = 
%     0.9784
% 
% negative success rate = 
%     0.9453
% 
% total success rate = 
%     0.9629
%

%%Actual code
import java.util.ArrayList;

x(1)= loadjson('../../../SliceTrial1');
x(2)= loadjson('../../../SliceTrial5');
x(3)= loadjson('../../../SliceTrial10');

for j=1:3

    display('-----------------------------------------------------------------')
    display('PROPORTION OF POSITIVE LABELS IN SYNTHEIC DATA');
    disp(x(j).pos);
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
    ylabel('p/p*');
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
    Names.add(['DecreaseLoss',num2str(i)]);
    Names.add(['PrimalResidual',num2str(i)]);
    Names.add(['DualResidual',num2str(i)]);
    Names.add(['TimePerIteration',num2str(i)]);
    Names.add(['PrimalEpsilon',num2str(i)]);
    Names.add(['DualEpsilon',num2str(i)]);
    Names.add(['Cardinality',num2str(i)]);

    for i = 1:7
        figure(i)
        saveName = Names.get(i-1);
        saveas(gcf, saveName, 'png');
        saveas(gcf, saveName, 'fig');
        close
    end
end

%%Now look at results as a function nbSlices
