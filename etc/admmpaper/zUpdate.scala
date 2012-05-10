val xLS = oldSet 
	.map(.xUpdateEnv) 
	.cache() 
	
val z = { 
	val reduced = xLS 
		.map(ls => ls.x + ls.u)
		.reduce(_+_) 

	shrinkage(reduced / nSlices) 
}