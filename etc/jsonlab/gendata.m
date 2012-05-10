% L1 regularized logistic regression (not distributed)

%% Generate problem data

rand('seed', 0);
randn('seed', 0);

n = 50; 
m = 700;

w = sprandn(n, 1, .5);  % N(0,1), 10% sparse
v = randn();            % random intercept

X = sprandn(m, n, .1);
btrue = sign(X*w + v);

% noise is function of problem size use 0.1 for large problem
b = sign(X*w + v + sqrt(0.01)*randn(m,1)); % labels with noise

% A = spdiags(-b, 0, m, m) * X;

% savesparsemat(X,'../../etc/A.10000x500.data');
% disp('done A')
% savesparsevec(b,'../../etc/b.10000.data');
% disp('done b')

savesparsemat(X,'../../etc/A.data');
disp('done A')
savesparsevec(b,'../../etc/b.data');
disp('done b')