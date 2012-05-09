function savesparsevec(vec, fn)

[i,j, val] = find(vec);
data_dump = [i,val];
fid = fopen(fn,'w');
fprintf(fid, '%d %d\n', data_dump' );
fclose(fid);

end