
+ TuningRatios {
	
	newTonic_ {|root=0|
		root = root.mod(12);
		if(root != 0) {
			root = tunings.size - root;
			tunings = tunings[root..]++(tunings[..root-1]*2);
			tunings = tunings/tunings[0];
		} 
		^this
	}
}