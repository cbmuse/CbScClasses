GenAlg : Object { // Genetic Algorithm object  trp 10.13.99
	 	critter = this.lastCritter;
	 	if ( (1.0.rand < this.mutationRate), 
	 		{  this.lastCritter = this.getMutant(theKid); },{
	 		this.lastCritter = theKid } );	
//		if( (1.0.rand < this.mutationRate), 
//	 		{ theKid = this.getMutant(theKid) });
//	 	this.lastCritter = theKid;
	 	