% Test on the size on the cluster

%ins-test-m1.medium = running a simple test with medium sized cluster
%ins-test-m1.large = running a simple test with large sized cluster

%%Actual code
clear all
close all
clc

import java.util.ArrayList;

x(1)= loadjson('ins-test-m1.medium');
x(2)= loadjson('ins-test-m1.large');

%compare time iterations between the two algorithms
tMed = [x(1).iters.time];
tLar = [x(2).iters.time];
figure(1)
plot(tMed(tMed>2400));
hold on
plot(tLar(tLar>2400),':');
legend('medium performance','large performance');
xlabel('iteration');
ylabel('time per iteration');
title('Time per iteration for two different type of clusters');
saveas(gcf, 'compareClusters', 'png');
saveas(gcf, 'compareClusters', 'fig');
close