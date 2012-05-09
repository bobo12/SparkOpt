function savesparsemat(mat, fn)

[i,j,val] = find(mat);
data_dump = [i,j,val];
fid = fopen(fn,'w');
fprintf(fid, '%d %d %f\n', data_dump' );
fclose(fid);

end