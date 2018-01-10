MidiDiamondString {
	var <>in, <>minFreq, <>maxFreq, <>execFreq, <>median, <>ampThresh, 	<>peakThresh, <>downSamp, <>synth, <>balance=0, <>respFlg=0,
	s,<>numTables=6,<>numStrings=6;
	
	*new { | freq=120,amp=0.0,table=0|
		^super.newCopyArgs(freq,amp,table).initString(freq,amp,table)
	}
	
	*loadSynthDef {
		SynthDef("midiDiamond",{ arg freq=120, amp=0,table=0,pan=0,numStrings=6,wet=0.25,gate=1;
			var sig,rvb,env,harmAmps,thrsh=0.001,lfo;
			table = (table+((Lag.kr(amp,0.05))*numStrings))
				.clip(table,table+(numStrings-1)-0.01);
			//	SendTrig.kr(Impulse.kr(1),1,table);
			env = EnvGen.kr(Env.asr(0.05,1,0.1), 
				gate: gate, doneAction: 2);
			sig = VOsc.ar(table,freq,0,amp*(numStrings.reciprocal));
			rvb = sig;
			6.do({
				rvb = AllpassN.ar(rvb, 0.040, [0.040.rand,0.040.rand], 2)
			});
			Out.ar(0,Pan2.ar(sig,pan)+(rvb*wet));
		}).send(Server.default);
	}
	
	initString {|freq,amp,table|
		s = Server.default;
		if( VOscBufs.buffers.isNil,{ 
			VOscBufs.initClass.makeDiamondBufs },{
			VOscBufs.freeBufs; VOscBufs.makeDiamondBufs
		}); 
		Routine({ 0.1.wait; 		
			synth = Synth("midiDiamond",[\freq,freq,\amp,amp,\table,table]);
		}).play;
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