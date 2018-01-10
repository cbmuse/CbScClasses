
VoicerSynthDefs	{
	classvar <>synthDefs, <>numOuts = 2, <>azArray, <>elArray, panFactor=0.5pi, <>effbus=16, <>recbus=24;

	*init {
		azArray = [-0.25pi, 0.25pi, 0.75pi, 1.25pi];
		elArray = [0,0,0,0];
		switch(numOuts,
			2,{ panFactor = 0.5pi },
			4,{ panFactor = pi }
		);
		synthDefs = [

			SynthDef(\fatSawK,{arg freq=100,pb=1,ffreq=800,rq=0.5,
				fdiff=0.0025,att=0.05,decay=0.2,susLev=0.125,rls=0.25,phs=0.1,lspd=0.2,
				amp=0.1,pan=0,elev=0,gate=1,
				effAmp = 0, recBus=recbus, effBus=effbus, outbus=0;
				var sig,env,w,x,y,z;
				amp = Latch.kr(gate, gate) * amp;
				freq=freq*pb;
				env = EnvGen.kr(Env.adsr(att,decay,susLev,rls), gate, doneAction:2);
				sig = RLPF.ar(
					LFSaw.ar([freq,freq+SinOsc.kr(lspd,0,freq*fdiff)],phs,1),
					ffreq,rq,env);
				sig=Mix(sig)*amp;
				Out.ar(recBus,sig);
				Out.ar(effBus,sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig*(1-effAmp),pan*panFactor,elev);
				Out.ar(outbus,BFDecode1.ar(w,x,y,z,azArray,elArray))
			}).add,

			SynthDef(\greenOrgK,{ arg freq=440,pb=1,spd=0.2,gain=2,
				amp=0.1, att=0.01, decay=0.1,
				susLev=0.5,rls=0.5,pan=0,elev=0,gate=1,
				effAmp = 0,effBus=effbus,recBus=recbus,outbus=0;
				var sig,eg,w,x,y,z;
				eg = EnvGen.kr(Env.adsr(att,decay,susLev,rls,0.5), gate, doneAction:2);
				amp = Latch.kr(gate, gate) * amp;
				freq = freq*pb;
				sig = MoogFF.ar(VarSaw.ar(freq,0,0.5,amp*eg),
					LFTri.kr(spd,Rand(0,2pi),400,600),gain);
				Out.ar(recBus,sig);
				Out.ar(effBus,sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig*(1-effAmp),pan*panFactor,elev);
				Out.ar(outbus,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).add,

			SynthDef(\minimoog2K,{ arg freq=440,pb=1,int1=5.neg,int2 = 5,
				width1=0.1,width2=0.2,width3=0.5,
				ffrqInt=12,rq=0.4, amp=0.1, att=0.01, decay=0.1,
				susLev=0.5,rls=0.5,pan=0,elev=0,gate=1,
				effAmp = 0, recBus=recbus, effBus=effbus, outbus=0;
				var sig,eg,w,x,y,z;
				amp = Latch.kr(gate, gate) * amp;
				freq = freq*pb;
				eg = EnvGen.kr(Env.adsr(att,decay,susLev,rls,0.5), gate, doneAction:2);
				sig=Pulse.ar([ freq  * int1.midiratio, freq, freq * int2.midiratio],
					[ width1,width2,width3],eg);
				sig = RLPF.ar(Mix.ar(sig),freq * (ffrqInt.midiratio)*pb,rq.max(0.001),amp);
				Out.ar(recBus,sig);
				Out.ar(effBus,sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig*(1-effAmp),pan*panFactor,elev);
				Out.ar(outbus,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).add,

			SynthDef(\chimeK, { arg freq=440, pb=1,
				pan=0,elev=0, mod=0, lofreq=0.1, lrtio=1.0, start=0,
				ring=0.4, amp=0.1, att=0.01, decay=0.1,
				susLev=0.5,rls=0.5, gate=1,
				effAmp = 0, recBus = recbus, effBus=effbus, outbus=0;
				var eg, modSig, specs, harmonics, sig,modrange,w,x,y,z;
				amp = Latch.kr(gate, gate) * amp;
				freq = freq*pb;
				harmonics = Array.series(11,2,1+(lrtio.rand2)); harmonics.addFirst(1);
				specs = `[ harmonics, // partials
					[amp,amp,amp],  // amplitudes (default to 1.0)
					Array.rand(12, 0.1, 2) ]; // ring times
				eg = EnvGen.kr( Env.adsr(att,decay,susLev,rls,0.5), gate, doneAction:2);
				modSig = SinOsc.kr(lofreq,2pi*start,mod);
				modrange = (freq - (freq.cpsmidi+1).midicps).abs;
				sig = DynKlank.ar(specs,
					Decay2.ar(Impulse.ar(0.01),0.001, 0.03, ClipNoise.ar(0.1)),
					freq, // freq.scale
					modSig*modrange, // freq.offset
					ring
				);
				Out.ar(recBus,sig*eg);
				Out.ar(effBus,sig*eg*effAmp);
				#w,x,y,z = BFEncode1.ar(sig*eg*(1-effAmp),pan*panFactor,elev);
				Out.ar(outbus,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).add,

			SynthDef(\bassK,{ arg freq=440,pb=1,lofreq=44,mod=0,rq=0.1,ffreq=2000,
				pan=0,elev=0,amp=0.1,att=0.01, decay=0.1,
				susLev=0.25,rls=0.5, gate=1,
				effAmp = 0, recBus = recbus, effBus=effbus, outbus=0;
				var env,sig,modSig,w,x,y,z;
				amp = Latch.kr(gate, gate) * amp;
				freq = freq*pb;
				env = EnvGen.kr(Env.adsr(att,decay,susLev,rls,0.5),gate, doneAction:2);
				modSig = SinOsc.ar(lofreq,0,mod);
				sig = RLPF.ar(
					LFSaw.ar(freq+(((freq.cpsmidi+1).midicps-freq)*modSig),0,env),
					ffreq,rq.max(0.001),amp);
				Out.ar(recBus,sig);
				Out.ar(effBus,sig*(effAmp));
				#w,x,y,z = BFEncode1.ar(sig*(1-effAmp),pan*panFactor,elev);
				Out.ar(outbus,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).add,

			SynthDef(\pluckK,{ arg freq=400,pb=1,plDcy=0.1,coef=0.1,pan=0,elev=0,
				att=0.01, decay=0.1,susLev=0.5,rls=0.5,
				amp=0.1,effAmp = 0, recBus = recbus, effBus=effbus, outbus=0, gate=1;
				var sig,env, w,x,y,z;
				amp = Latch.kr(gate, gate) * amp;
				freq = freq*pb;
				env = EnvGen.kr( Env.adsr(att,decay,susLev,rls,0.5), gate, doneAction:2);
				sig = Pluck.ar(WhiteNoise.ar(amp),Impulse.kr(0.01),
					20.reciprocal,freq.reciprocal,plDcy,coef,env);
				Out.ar(recBus,sig);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig*(1-effAmp),pan*panFactor,elev);
				Out.ar(outbus,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).add,

			SynthDef(\pmK, { arg freq=440, pb=1, ffrqInt=24, rq=0.1,
				lrtio = 2.0, idx = 4, att=0.0025, decay=0.1,rls=0.05, susLev=0.5,
				chor=0.1, amp=0.1, pan=0, elev=0, effAmp = 0,
				recBus = recbus, effBus=effbus, outbus=0, gate=1;
				var sig,env,w,x,y,z;
				amp = Latch.kr(gate, gate) * amp;
				freq = freq*pb;
				env = EnvGen.kr(Env.adsr(att,decay,susLev,rls),gate, doneAction:2);
				sig = 	RLPF.ar(PMOsc.ar(freq, freq *lrtio, idx, 0,env),
					freq * (ffrqInt.midiratio)*pb,rq.max(0.001),env);
				sig = Mix.fill(8, {DelayC.ar(sig*0.125,
					0.05, LFNoise1.kr(IRand(0,0.2),0.005*chor,0.005)) });
				Out.ar(recBus,sig*amp);
				Out.ar(effBus, sig*amp*effAmp);
				#w,x,y,z = BFEncode1.ar(sig*amp*(1-effAmp),pan*panFactor,elev);
				Out.ar(outbus,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).add,

			SynthDef(\pmDcyK,{ arg freq=440,pb=1,ratio=10,idx=4,att=0.05,rls=0.1, 				lofreq=0.5,mod=0,pan=0,elev=0,amp=0,outbus=0, recBus = recbus, effBus=effbus, effAmp=0, gate=1;
				var sig, modSig, env, modFreq,w,x,y,z;
				amp = Latch.kr(gate, gate) * amp;
				freq = freq*pb;
				env = EnvGen.kr(Env.perc(att, rls, 1, -4),gate, doneAction: 2 );
				modFreq = freq*ratio;
				sig = PMOsc.ar(freq+SinOsc.kr(lofreq,0,mod*freq*0.1),modFreq,idx,0,env);
				Out.ar(recBus,sig*amp);
				Out.ar(effBus, sig*amp*effAmp);
				#w,x,y,z = BFEncode1.ar(sig*amp*(1-effAmp),pan*panFactor,elev);
				Out.ar(outbus,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).add,

			// next is just a stub placeholder for KbdVoicers
			SynthDef(\silentK,{
				var sig=0;
				EnvGen.kr(Env.perc(0.001, 0.01),doneAction: 2 );
				Out.ar(0,sig)
			}).add,

			SynthDef(\snareK,{ arg freq=440,noiseAmp=0.1, lofreq=4, mod=0.1,
				att=0.0025, decay=0.1,rls=0.05, susLev=0.5,
				dcyt=4,pan=0,elev=0,amp=0.1,outbus=0, recBus=recbus, effBus=effbus, effAmp=0, gate=1;
				var env,sig,ringz,modSig,w,x,y,z;
				env = EnvGen.kr(Env.adsr(att,decay,susLev,rls,0.5),gate, doneAction:2);
				modSig = SinOsc.kr(lofreq,Rand(0.0,2pi),mod);
				ringz = Ringz.ar(WhiteNoise.ar(noiseAmp),freq+(modSig*freq),dcyt,env);
				sig = Decay2.ar(Impulse.ar(0.01),att,rls,ringz);
				Out.ar(recBus,sig*amp);
				Out.ar(effBus, sig*amp*effAmp);
				#w,x,y,z = BFEncode1.ar(sig*amp*(1-effAmp),pan*panFactor,elev);
				Out.ar(outbus,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).add,

			SynthDef(\stringK, { arg freq = 440, pb=1, spd=0.4, rq=0.4, att=0.01, decay=0.1,ffrqInt=24,
				susLev=0.5,rls=0.5,chor=0.1, pan=0,elev=0, effAmp = 0, recBus=recbus, effBus=effbus, outbus=0,
				amp=0.1, gate=1;
				var sig, env, fc, osc, a, b,w,x,y,z;
				amp = Latch.kr(gate, gate) * amp;
				freq = freq*pb;
				fc = LinExp.kr(LFNoise1.kr(spd*(0.1.rrand(1.0))),-1,1,
					freq*2,freq * (ffrqInt.midiratio)*pb);
				osc = Mix.fill(8, {DelayC.ar(LFSaw.ar(freq, 0, amp*0.125),
					0.05, LFNoise1.kr(IRand(0,0.2),0.005*chor,0.005)) }).distort;
				env = EnvGen.kr(Env.adsr(att,decay,susLev,rls,0.5),gate, doneAction:2);
				sig = env * RLPF.ar(osc, fc, rq.max(0.001));
				Out.ar(recBus,sig);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig*(1-effAmp),pan*panFactor,elev);
				Out.ar(outbus,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).add,

			SynthDef(\sineK, { arg freq=440, pb=1, att=0.01, decay=0.1,susLev=0.5,rls=0.5,
				lofreq=0.1,mod=0,pan=0,elev=0,  effAmp = 0, recBus=recbus, effBus=effbus, outbus=0,
				chor=0.1, amp=0.1, gate=1;
				var sig, env,w,x,y,z;
				amp = Latch.kr(gate, gate) * amp;
				SendTrig.kr(Impulse.kr(1),0,freq);
				freq = freq*pb;
				env = EnvGen.kr(Env.adsr(att,decay,susLev,rls,0.5),gate, doneAction:2);
				sig = SinOsc.ar(freq, SinOsc.kr(lofreq,0,mod*pi), env);
				sig = Mix.fill(8, {DelayC.ar(sig*0.125,
					0.05, LFNoise1.kr(IRand(0,0.2),0.005*chor,0.005)) });
				env = EnvGen.kr(Env.adsr(att,decay,susLev,rls,0.5),gate, doneAction:2);
				Out.ar(recBus,sig*amp);
				Out.ar(effBus, sig*amp*effAmp);
				#w,x,y,z = BFEncode1.ar(sig*amp*(1-effAmp),pan*panFactor,elev);
				Out.ar(outbus,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).add,

			SynthDef(\formantK, { arg freq=440, pb=1, ffreq=1200, bw=0.2, lofreq=0.5,mod=0,
				att=0.01, decay=0.1,susLev=0.5,rls=0.5, chor=0.1,
				pan=0,elev=0,  effAmp = 0, recBus=recbus, effBus=effbus, outbus=0,
				amp=0.1, gate=1;
				var env,sig, w,x,y,z;
				amp = Latch.kr(gate, gate) * amp;
				freq = freq*pb;
				env = EnvGen.kr(Env.adsr(att,decay,susLev,rls),gate, doneAction:2);
				sig = Formant.ar(freq, ffreq+SinOsc.kr(lofreq,Rand(0,2pi),mod*ffreq*0.5),
					bw*ffreq, env);
				sig = Mix.fill(8, {DelayC.ar(sig*0.25,
					0.05, LFNoise1.kr(IRand(0,0.2),0.005*chor,0.005)) });
				env = EnvGen.kr(Env.adsr(att,decay,susLev,rls),gate, doneAction:2);
				Out.ar(recBus,sig*amp);
				Out.ar(effBus, sig*amp*effAmp);
				#w,x,y,z = BFEncode1.ar(sig*amp*(1-effAmp),pan*panFactor,elev);
				Out.ar(outbus,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).add,

			SynthDef(\pianoK, { arg freq=440, pb=1, ffreq=1200, rq=0.05,
				lofreq=0.5,mod=0,att=0.01, decay=0.1,susLev=0.5,rls=0.5,
				pan=0,elev=0,  effAmp = 0, recBus=recbus, effBus=effbus, outbus=0,
				amp=0.1, gate=1;
				var env,oscEnv,pnoEnvGen,noteDur,sig, w,x,y,z;
				var harms = 10;
				amp = Latch.kr(amp, gate) * amp;
				freq = freq*pb;
				oscEnv = Env.perc(0.001,1,1,-4);
				noteDur = (1-(\ffreq.asSpec.unmap(freq)))*harms;  // dur decreases with freq from 10 - 1.25
				pnoEnvGen = EnvGen.kr(Env.new([0,1,0.5,0.001,0]*amp,[0.001,0.1,noteDur,0.01],-12));
				env = EnvGen.kr(Env.asr(0.001,2,rls,0.5),gate, doneAction:2);
				sig = Mix.fill(4,{
					var f = (freq.cpsmidi+(rrand(-0.025,0.025))).midicps;
					var w = Rand(0,pi);
					DynKlang.ar(`[
						Array.fill(harms,{|i| f*(i+1) }),
						nil,
						Array.fill(harms,{|i| EnvGen.kr(oscEnv,
							timeScale:((harms-i)+1), levelScale: harms/(i+1) ) }),
						Array.fill(harms,{w})
					]) + WhiteNoise.ar(EnvGen.kr(Env.perc(0.001,1,0.005)))
				})*pnoEnvGen*env;
				sig = LPF.ar(sig,ffreq+SinOsc.kr(lofreq,0,mod*ffreq));
				Out.ar(recBus,sig*amp);
				Out.ar(effBus, sig*amp*effAmp);
				#w,x,y,z = BFEncode1.ar(sig*amp*(1-effAmp),pan*pi,elev);
				Out.ar(outbus,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).add,

			SynthDef(\tunSusK,{|freq=200,pb=1,wdth=0.2,phs=0,att=0.01,susLev=0.5,rls=0.5,
				pan=0,elev=0,effAmp=0,recBus=0,effBus=0,outbus=0,amp=0.1,gate=1|
				var w,x,y,z,sig;
				var eg = EnvGen.ar(Env.asr(att,susLev,rls),gate,doneAction:2);
				amp = Latch.kr(amp, gate) * amp;
				freq = freq*pb;
				sig = VarSaw.ar(freq,phs,wdth,amp*eg);
				Out.ar(recBus,sig*amp);
				Out.ar(effBus, sig*amp*effAmp);
				#w,x,y,z = BFEncode1.ar(sig*amp*(1-effAmp),pan*panFactor,elev);
				Out.ar(outbus,BFDecode1.ar(w, x, y, z, azArray, elArray))}).add
		]

	}


	*assgn	{| synthName, numVoices, chan, tuning, root|
		var v = Voicer.new(numVoices,synthName);
		var voice = VoicerMIDISocket.new((chan-1).asInt,v);
		var func,t;
		if(tuning.notNil,{
			func =  { |ratios, rt = 0|
				if(root != 0) {
					rt = ratios.size - rt;
					ratios = ratios[rt..] ++ (ratios[..rt-1] * 2);
					ratios = ratios / ratios[0];
				};
				TuningRatios(ratios.size, tunings: ratios);
			};
			t = func.(tuning, root);
			voice.midiToFreq=t;
		});
		^voice	// return VoicerMIDISocket, can get Voices with 'voice.destination'

	}

	//	VoicerSynthDefs.assgn("bass",16,1,JustKeyBoardTunings.tunings[\pythagoras],9)


}

	