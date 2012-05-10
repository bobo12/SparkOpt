% L1 regularized logistic regression (not distributed)

%% Generate problem data

rand('seed', 0);
randn('seed', 0);

n = 10000; 
m = 100000;

w = sprandn(n, 1, .1);  % N(0,1), 10% sparse
v = randn();            % random intercept

X = sprandn(m, n, .01);
btrue = sign(X*w + v);

% noise is function of problem size use 0.1 for large problem
b = sign(X*w + v + sqrt(0.01)*randn(m,1)); % labels with noise

% A = spdiags(-b, 0, m, m) * X;

savesparsemat(X,'/Users/jdr/Documents/github-projects/SparkOpt/etc/A.100000x10000.data');
disp('done A')
savesparsevec(b,'/Users/jdr/Documents/github-projects/SparkOpt/etc/b.100000.data');
disp('done b')
