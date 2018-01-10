

SendWave : UGen {
	checkInputs { ^this.checkSameRateAsFirstInput }

	*ar { arg in = 0, id = 0, port = 0, mul = 1.0, add = 0.0;
		^this.multiNew('audio', in,id,port).madd(mul,add)
	}

}
