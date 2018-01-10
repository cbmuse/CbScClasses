
RecordBufferArray {
	
	var <>size, <>length, <>numChans, <buffers, <>recInBus=0, <>curRecBuf=0, <>curRecPos=0, 
		<>threshold=0.01,recSynth,recRoutine,server;
	
	*new { arg arraySize=8, bufLength=8, bufNumChans=1;
		^super.new.init(arraySize, bufLength, bufNumChans)
	}
	
	init { |arraySize, bufLength, bufNumChans|
		size = arraySize; length = bufLength;	numChans = bufNumChans;
		server = Server.default;
		"creating ".post; size.post; " ".post; length.post;  " second buffers".postln;
		buffers = Buffer.allocConsecutive(size,server,server.sampleRate*length);
		SynthDef("bufArrayRecord",{|inBus=0,bufnum=0,trig=0,gate=1|
			var phasor = Phasor.ar(trig,BufRateScale.kr(bufnum),0,BufFrames.kr(bufnum),curRecPos);
			var env = EnvGen.kr(Env.asr(0.025,1,0.025),gate: gate,doneAction:2);
			var in = In.ar(inBus)*env;
			var recycle = BufRd.ar(numChans,bufnum,phasor);
			var inputAmp = Amplitude.kr(in,releaseTime: 0.25);
			var inputSel = Lag.kr(inputAmp>threshold,0.1);
			in = (in*inputSel)+(recycle*(1-inputSel));
			BufWr.ar(in,bufnum,phasor); 0
		}).send(server);
	}
	
	record { arg inBus;
		var buf=buffers[curRecBuf].bufnum;
		recInBus=inBus;
		recSynth = Synth("bufArrayRecord",[\inBus,inBus,\bufnum,buf]).play
	}
	
	recordAll { arg inBus;
		this.record(inBus);
		recRoutine = Routine({
			loop({ curRecBuf = (curRecBuf+1)%size; 
				recSynth.set(\bufnum,buffers[curRecBuf].bufnum);
				server.sampleRate*length.wait
			})
		}).play
	}
			
			
	stop {
		recSynth.set(\gate,0); recRoutine !? { recRoutine.stop };
	}
	
	free {
		buffers.do({|buf| buf.free })
	}
			
}
		
		
	