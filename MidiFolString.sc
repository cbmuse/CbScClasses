MidiFolString {
	var <>in, <>minFreq, <>maxFreq, <>execFreq, <>median, <>ampThresh, 	<>peakThresh, <>downSamp, <>synth, <>harms, <>balance=0, <>respFlg=0,
	s,<>numTables=6,<>numStrings=6,<>numHarms=4,<>harmAmps=4;
	
	*new { arg in=1,minFreq=60,maxFreq=1200,execFreq=100,median=1,
		ampThresh=0.01,peakThresh=0.5, downSamp=2;
		^super.newCopyArgs(in,minFreq,maxFreq,execFreq,median,ampThresh,
		peakThresh, downSamp).initString
	}
	
	*loadSynthDef {
		SynthDef("midiFol",{ arg freq, amp,table,pan=0,mod=0,
			numTables=6,numHarms=4,numStrings=6,wet=1,gate=1;
			var sig,rvb,env,harms,chord,harmAmps,thrsh=0.001,lfo;
			harms = Control.names([\harms]).kr([1,2,3,4]);  // init harmonics
			harmAmps = Control.names([\harmAmps]).kr([1,1,1,1]);  // init harmonics
			table = (table+((Lag.kr(amp,0.05))*numTables))
				.clip(table,table+(numTables-1)-0.01);
			amp = Lag.kr(amp,0.1)+(LFNoise2.kr(1,mod).abs);
			//	SendTrig.kr(Impulse.kr(10)*(amp>0)*respFlg,1,amp);
			env = EnvGen.kr(Env.asr(0.05,1,0.1), 
				gate: gate, doneAction: 2);
			chord=freq*harms;
			sig = Mix.new(VOsc.ar(table,chord,0,amp*harmAmps
				*(numHarms.reciprocal)*(numStrings.reciprocal)));
			//	sig = Compander.ar(sig,sig,thrsh,10,1,0.01,0.01)*env;
			rvb = sig;
			6.do({
				rvb = AllpassN.ar(rvb, 0.040, [0.040.rand,0.040.rand], 2)
			});
			Out.ar(0,Pan2.ar(sig,pan)+(rvb*wet));
		}).send(Server.default);
	}
	
	initString {
		s = Server.default;
		if( VOscBufs.buffers.isNil,{ 
			VOscBufs.initClass.makeBufs(numTables) },{
			VOscBufs.freeBufs; VOscBufs.makeBufs(numTables)  
		}); 
		Routine({ 0.1.wait; 		
			synth = Synth("midiFol",["in",in,"table",VOscBufs.buffers[0].bufnum,
				"numTables",numTables,"numStrings",numStrings,
				"respFlg",respFlg]			
			);
		}).play;
		harms = [1,2,3,4];
		harmAmps = [1,1,1,1]
	}
	
	setHarms { arg harmArray;
		harms = harmArray;
		s.sendBundle(nil,
			["n_setn",synth.nodeID,"harms",numHarms] ++ harms);
	}
	
	setHarmAmps { arg harmArray;
		harmAmps = harmArray;
		s.sendBundle(nil,
			["n_setn",synth.nodeID,"harmAmps",numHarms] ++ harmAmps);
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