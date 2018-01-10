PfolString {
	var <>in, <>minFreq, <>maxFreq, <>execFreq, <>median, <>ampThresh, 	<>peakThresh, <>downSamp, <>synth, <>harms, <>balance=0, <>respFlg=0,
	s,numTables=8,numStrings=6,numHarms=4;
	
	*new { arg in=1,minFreq=60,maxFreq=1200,execFreq=100,median=1,
	ampThresh=0.01,peakThresh=0.5, downSamp=2;
		^super.newCopyArgs(in,minFreq,maxFreq,execFreq,median,ampThresh,
		peakThresh, downSamp).initString
	}
	
	*loadSynthDef {
		SynthDef("pitchFol",{ arg in=1,minFreq=60,maxFreq=600,execFreq=100,
			median=1,ampThresh=0.02,peakThresh=0.5,downSamp=1,
			table,numTables=8,numStrings=6,respFlg=0,bal=0,gate=1;
			var freq, hasFreq,amp,sig,env,pan,harms,chord,thrsh=0.001,input;
			input = AudioIn.ar(in);
			# freq, hasFreq = Pitch.kr(input,
				440,minFreq,maxFreq,execFreq,16,median,ampThresh,peakThresh,downSamp);
			amp = Amplitude.kr(input,0.05,0.05);
			SendTrig.kr(Impulse.kr(10)*(hasFreq>0)*respFlg,in,
				Peak.kr(freq,Impulse.kr(10)));
			SendTrig.kr(Impulse.kr(10)*(hasFreq>0)*respFlg,in+10,
				Peak.kr(amp,Impulse.kr(10)));
			harms = Control.names([\harms]).kr([1,1.5,2,2.5]);  // init harmonics
			table = (table+((FaderWarp.asWarp.map(amp))*numTables)).clip(table,table+(numTables-1)-0.01);
			env = EnvGen.kr(Env.asr(0.05,numStrings.reciprocal,0.1), 
				gate: gate, doneAction: 2);
			chord=freq*harms;
			sig = (Mix.new(VOsc.ar(table,chord,0,amp))*(1-bal))+
				(input*bal);
			sig = Compander.ar(sig,sig,thrsh,10,1,0.01,0.01);
			6.do({
				sig = AllpassN.ar(sig, 0.040, [0.040.rand,0.040.rand], 2)
			});
			Out.ar(0,sig*env);
		}).send(Server.default);
	}
	
	initString {
		s = Server.default;
		if( VOscBufs.buffers.isNil,{ 
			VOscBufs.initClass.makeBufs(numTables) },{
			VOscBufs.freeBufs; VOscBufs.makeBufs(numTables)  
		}); 
		Routine({ 0.1.wait; 		
			synth = Synth("pitchFol",["in",in,
				"minFreq",minFreq,"maxFreq",maxFreq,"execFreq",execFreq,
				"median",median,
				"ampThresh",ampThresh,"peakThresh",peakThresh,
				"downSamp",downSamp,"table",VOscBufs.buffers[0].bufnum,
				"numTables",numTables,"numStrings",numStrings,
				"respFlg",respFlg]			
			);
		}).play;
		harms = [1,1.5,2,2.5]
	}
	
	setHarms { arg harmArray;
		harms = harmArray;
		s.sendBundle(nil,
			["n_setn",synth.nodeID,"harms",numHarms] ++ harms);
	}
	
	setResp { arg flg=1;
		respFlg=flg;
		synth.set("respFlg",respFlg)
	}
	
}
		
/*
 PfolString.loadSynthDef; 
 a = PfolString.new;
 a.setHarms([1,1.5,1.9,2.1])
*/