
RitmosSynthDefs	{
	classvar <>synthDefs, <>numOuts = 2, <>azArray, <>elArray, panFactor=0.5pi,effbus=16;

	*init {
		switch(numOuts,
			2,{panFactor = 0.5pi; azArray = [-0.25pi,0.25pi]; elArray = [0,0] },
			4,{panFactor = pi; azArray = [-0.25pi, 0.25pi, 0.75pi, 1.25pi];
				elArray = [0,0,0,0] }
		);

		 synthDefs = [

			SynthDef(\boomRp,{ arg freq=60,att=0.001,rls=0.5,amp=0.1,curve=4,fold=0.6,
				effAmp=0,pan=0,effBus=effbus,elev=0,gate=1;
				var sig,env,w,x,y,z;
				env = EnvGen.kr(Env.perc(att,rls,amp,curve),doneAction:2);
				sig = SinOsc.ar([freq,freq*0.97],[0,2pi*0.015]);
				sig = Mix((sig fold2: fold).softclip*env);
				Out.ar(effBus,sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

		 	// "homemade" Klank uses 4 mixed Decay->Ringz->PinkNoise partials w preset freqratios
		 	SynthDef(\klnkR,{ arg freq=400,ring=1,ratio2=2.6775,
					ratio3=3.3825,ratio4=4.3075,att=0.001, rls=0.01,pan=0,elev=0,amp=0.1,
					out=0, effBus=effbus, effAmp=0, gate=1;
				var sig,env,safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.asr(0.025,amp, 0.2),gate,doneAction:2);
				sig = Mix(Ringz.ar(
						Decay2.ar(Impulse.ar(0.01,0,0.25),att,rls,
							PinkNoise.ar(freq.explin(20,8000,0.2,2.0))),
						[freq,freq*ratio2,freq*ratio3,freq*ratio4],
					ring,env*0.25));
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\klnkRp,{ arg freq=400,ring=1,ratio2=2.6775,
					ratio3=3.3825,ratio4=4.3075,att=0.001, rls=0.01,pan=0,elev=0,amp=0.1,
					out=0, effBus=effbus, effAmp=0, gate=1;
				var sig,env,w,x,y,z;
				env = EnvGen.kr(Env.perc(att,rls,amp),gate,doneAction:2);
				sig = Mix(Ringz.ar(
						Decay2.ar(Impulse.ar(0.01,0,0.25),att,rls,
							PinkNoise.ar(freq.explin(20,8000,0.2,4.0))),   // scale amplitude to freq
						[freq,freq*ratio2,freq*ratio3,freq*ratio4],
					ring,env*0.25));
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			// 12 osc Klank with rand generated partials (lrtio scales their freq range)
			SynthDef(\chimeR, { arg freq=440, pan=0,elev=0, mod=0, lofreq=0.1, lrtio=1.0,
				ring=0.4, att=0.001, rls=0.03, amp=0.1, out=0, effBus=effbus, effAmp=0, gate=1;
				var env, modSig, specs, harmonics, sig, amps, modrange,safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				harmonics = Array.series(11,2,1+(lrtio.rand)); harmonics.addFirst(1);
				amps = Array.exprand(12,0.05,0.5).normalize(0.05).sort.reverse;
				specs = `[ harmonics, // partials
						amps,  // amplitudes
						Array.rand(12, 0.1, 2) ]; // ring times
				env = EnvGen.kr( Env.asr(0.0001, amp, 0.5), gate, doneAction:2);
				modSig = SinOsc.kr(lofreq,Rand(0,2pi),mod);
				modrange = (freq - (freq.cpsmidi+1).midicps).abs;
				sig = Klank.ar(specs,
					Decay2.ar(Impulse.ar(0.1),att, rls, ClipNoise.ar(0.025)),
					freq, // freq.scale
					modSig*modrange, // freq.offset
					ring
				)*env;
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

				// 12 osc Klank with rand generated partials (lrtio scales their freq range)
			SynthDef(\chimeRp, { arg freq=440, pan=0,elev=0, mod=0, lofreq=0.1, lrtio=1.0,
				ring=0.4, att=0.001, rls=0.03, amp=0.1, out=0, effBus=effbus, effAmp=0, gate=1;
				var env, modSig, specs, harmonics, sig, amps, modrange,w,x,y,z;
				harmonics = Array.series(11,2,1+(lrtio.rand)); harmonics.addFirst(1);
				amps = Array.exprand(12,0.05,0.5).normalize(0.05).sort.reverse;
				specs = `[ harmonics, // partials
						amps,  // amplitudes
						Array.rand(12, 0.1, 2) ]; // ring times
				env = EnvGen.kr( Env.perc(att,rls,amp), gate, doneAction:2);
				modSig = SinOsc.kr(lofreq,Rand(0,2pi),mod);
				modrange = (freq - (freq.cpsmidi+1).midicps).abs;
				sig = Klank.ar(specs,
					Decay2.ar(Impulse.ar(0.1),att, rls, ClipNoise.ar(0.025)),
					freq, // freq.scale
					modSig*modrange, // freq.offset
					ring
				)*env;
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			// freq is more even
			SynthDef(\whistle2R,{ arg freq,lofreq,clipAmp,rq,mod=1,sinePhs,att,rls,
					amp,pan=0,elev=0,out=0, effBus=effbus, effAmp=0, gate=1;
				var env,noise,sig,bend,modSig,safeDur=10,safeEnv,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				//env times are rough
				env = EnvGen.kr(Env.asr(att,amp, rls),gate,doneAction:2);
				modSig = SinOsc.kr(lofreq,sinePhs,mod*(freq/2));
				noise = ClipNoise.ar(clipAmp*(freq.explin(20,2000,20,1)*(0.05/rq)));
				sig = Resonz.ar(noise,freq+modSig,rq,env);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\whistle2Rp,{ arg freq,lofreq,clipAmp,rq,mod=1,sinePhs,att,rls,
					amp,pan=0,elev=0,out=0, effBus=effbus, effAmp=0, gate=1;
				var env,noise,sig,bend,modSig,safeDur=10,safeEnv,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				//env times are rough
				env = EnvGen.kr( Env.perc(att, rls, amp), gate, doneAction:2);
				modSig = SinOsc.kr(lofreq,sinePhs,mod*(freq/2));
				noise = ClipNoise.ar(clipAmp*(freq.explin(20,2000,20,1)*(0.05/rq)));
				sig = Resonz.ar(noise,freq+modSig,rq,env);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			// resonance is more even
			SynthDef(\whistleR,{ arg freq=800,lofreq=20,clipAmp=1,rq=0.1,mod=1,sinePhs=pi,
					att=0.01,rls=0.1,amp=0.1,pan=0,elev=0,out=0, effBus=effbus, effAmp=0, gate=1;
				var env,noise,sig,bend,modSig,safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				//env times are rough
				env = EnvGen.kr(Env.asr(att,amp, rls),gate, doneAction:2)
					*(rq.linexp(0.0,8.0,8.0,0.01));
				modSig = SinOsc.kr(lofreq,sinePhs,mod);
				noise = ClipNoise.ar(clipAmp);
				sig = Resonz.ar(noise,freq+(modSig),rq,env);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\bassR,{ arg freq=440,lofreq=44,mod=0,rq=0.1,ffrqInt=3.0,
				att=0.01,rls=0.1,dcyt=0.05,pan=0,elev=0,amp=0.1,out=0, effBus=effbus, effAmp=0, gate=1;
				var env,fenv, sig,modSig,safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.asr(att,amp,rls),gate, doneAction:2);
				fenv = XLine.kr((ffrqInt*freq).min(10000),(freq*4).min(10000),dcyt);
				modSig = SinOsc.ar(lofreq,0,mod);
				sig = RLPF.ar(LFSaw.ar(freq+(((freq.cpsmidi+1).midicps-freq)*modSig),0,0.25),
					fenv,rq)*env;
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\snareR,{ arg freq=440,noiseAmp=0.1, lofreq=4, mod=0.1,
				att=0.01,rls=0.1,dcyt=4,pan=0,elev=0,amp=0.1,out=0, effBus=effbus, effAmp=0, gate=1;
				var env,sig,ringz,modSig,safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr( Env.asr(0.0001, amp, 0.5), gate, doneAction:2);
				modSig = SinOsc.kr(lofreq,Rand(0.0,2pi),mod);
				ringz = Ringz.ar(WhiteNoise.ar(noiseAmp),freq+(modSig*freq),dcyt,env);
				sig = Decay2.ar(Impulse.ar(0.01),att,rls,ringz);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\snareRp,{ arg freq=440,noiseAmp=0.1, lofreq=4, mod=0.1,
				att=0.01,rls=0.1,dcyt=4,pan=0,elev=0,amp=0.1,out=0, effBus=effbus, effAmp=0, gate=1;
				var env,sig,ringz,modSig,w,x,y,z;
				env = EnvGen.kr( Env.perc(att, rls, amp), gate, doneAction:2);
				modSig = SinOsc.kr(lofreq,Rand(0.0,2pi),mod);
				ringz = Ringz.ar(WhiteNoise.ar(noiseAmp),freq+(modSig*freq),dcyt,env);
				sig = Decay2.ar(Impulse.ar(0.01),att,rls,ringz);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\pluckR,{ arg freq=400,brit=2000,plDcy=0.5,clip=1,rq=0.1,
				pan=0,elev=0,att=0.01,rls=0.1,amp=0.1,out=0, effBus=effbus, effAmp=0, gate=1;
				var exciter,sig,env,bright,safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.asr(att,amp,rls),gate,doneAction: 2);
				bright = brit+freq;
				exciter = WhiteNoise.ar(Decay2.kr(Impulse.kr(0.1),0.001, 0.01, 8));
				sig = RLPF.ar(CombC.ar(exciter,
						0.1,freq.reciprocal,plDcy),bright,rq,env).clip2(clip);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\pluckRp,{ arg freq=400,brit=2000,plDcy=0.5,clip=1,rq=0.1,
				pan=0,elev=0,att=0.01,rls=0.1,amp=0.1,out=0, effBus=effbus, effAmp=0, gate=1;
				var exciter,sig,env,bright,w,x,y,z;
				env = EnvGen.kr(Env.perc(att,rls,amp),gate,doneAction: 2);
				bright = brit+freq;
				exciter = WhiteNoise.ar(Decay2.kr(Impulse.kr(0.1),0.001, 0.01, 8));
				sig = RLPF.ar(CombC.ar(exciter,
						0.1,freq.reciprocal,plDcy),bright,rq,env).clip2(clip);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\pluck2R,{ arg freq=400,plDcy=0.1,coef=0.1,plSpd=0.1,pan=0,elev=0,
				amp=0.1, out=0, effBus=effbus, effAmp=0, gate=1;
				var sig,env,safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = env = EnvGen.kr(Env.asr(0.0025,amp,0.05),gate,doneAction: 2);
				sig = Pluck.ar(WhiteNoise.ar(1.0),Impulse.kr(plSpd),
							20.reciprocal,freq.reciprocal,plDcy,coef,env);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\pluck2Rp,{ arg freq=400,plDcy=0.1,coef=0.1,plSpd=0.1,pan=0,elev=0,
				amp=0.1, att=0.01,rls=0.1, out=0, effBus=effbus, effAmp=0, gate=1;
				var sig,env,safeEnv,safeDur=10,w,x,y,z;
				env = env = EnvGen.kr(Env.perc(att,rls,amp),gate,doneAction: 2);
				sig = Pluck.ar(WhiteNoise.ar(1.0),Impulse.kr(plSpd),
							20.reciprocal,freq.reciprocal,plDcy,coef,env);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			// Decay2 envelope filters PMOsc
			SynthDef(\fmDrumR,{ arg freq=440,ratio=10,idx=4,lofreq=0.5,mod=0,
				att=0.025,rls=0.1,pan=0,elev=0,amp=0.1,out=0, effBus=effbus, effAmp=0, gate=1;
				var modSig,env,sig,attRelTime=0.025,safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.asr(att, amp, rls),gate,doneAction: 2);
				modSig = SinOsc.kr(lofreq,0,mod,pi);
				sig = Decay2.ar(Impulse.ar(0.01),att,rls,
					PMOsc.ar(freq,freq*ratio,idx,modSig,env)
				);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			// Decay2 envelope filters PMOsc
			SynthDef(\fmDrumRp,{ arg freq=440,ratio=10,idx=4,lofreq=0.5,mod=0,
				att=0.025,rls=0.1,pan=0,elev=0,amp=0.1,out=0, effBus=effbus, effAmp=0, gate=1;
				var modSig,env,sig,attRelTime=0.025,w,x,y,z;
				env = EnvGen.kr(Env.perc(att,rls,amp),gate,doneAction: 2);
				modSig = SinOsc.kr(lofreq,0,mod,pi);
				sig = Decay2.ar(Impulse.ar(0.01),att,rls,
					PMOsc.ar(freq,freq*ratio,idx,modSig,env)
				);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			// unfiltered percussive env PMOsc
			SynthDef(\pmDecayR,{ arg freq=440,ratio=10,idx=4,att=0.05,rls=0.1, lofreq=0.5,mod=0,
					pan=0,elev=0,amp=0,out=0, effBus=effbus, effAmp=0, gate=1;
				var sig, modSig, env, noteEnv, modFreq,safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				// next Env is  used only to avoid error message for note turnoff!
				EnvGen.kr(Env.asr(att, amp, rls),gate,doneAction: 2);
				env = EnvGen.kr(Env.perc(att, rls, amp, -4), gate);
				modFreq = freq*ratio;
				sig = PMOsc.ar(freq+SinOsc.kr(lofreq,0,mod*freq),modFreq,idx,0,env);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			//  percussive PMOsc requires no off gate
			SynthDef(\pmDecayRp,{ arg freq=440,ratio=10,idx=4,att=0.05,rls=0.1, lofreq=0.5,mod=0,
					pan=0,elev=0,amp=0,out=0, effBus=effbus, effAmp=0, gate=1;
				var sig, modSig, env, noteEnv, modFreq,safeEnv,safeDur=10,w,x,y,z;
				env = EnvGen.kr(Env.perc(att, rls, amp, -4), gate,doneAction: 2);
				modFreq = freq*ratio;
				sig = PMOsc.ar(freq+SinOsc.kr(lofreq,0,mod*freq),modFreq,idx,0,env);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			// sustained PMOsc wrapped in RLPF with randomly moving filter
			SynthDef(\pmR, { arg freq=440, rq=0.1, att=0.0025, rls=0.05,  ratio = 10, idx = 4,
					amp=0.1, pan=0, elev=0, out=0, effBus=effbus, effAmp=0, gate=1;
				var sig,env,safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.asr(att,amp,rls),gate,doneAction: 2);
				sig = RLPF.ar(PMOsc.ar(freq, freq * ratio, idx, 0, 0.3),
						XLine.kr((Rand(2.0,6.0)*freq).min(10000),(Rand(2.0,6.0)*freq)
							.min(10000)),rq,env);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\stringR, { arg freq = 440, rq=0.4, att=0.5, rls=0.2, pan=0,elev=0,
				amp=0.1, out=0, effBus=effbus, effAmp=0, gate=1;
				var sig, env, fc, osc, a, b,safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.asr(att,amp,rls), gate, doneAction:2);
				fc = LinExp.kr(LFNoise1.kr(Rand(0.25,0.4)), -1,1,1000,3000);
				osc = Mix.fill(8, {LFSaw.ar(freq, 0, amp)}).distort * 0.25;
				sig = RLPF.ar(osc, fc, rq, env);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef("sineR", { arg freq=440, att=0.025, rls=0.3, pan=0,elev=0, amp=0.1,
					out=0, effBus=effbus, effAmp=0, gate=1;
				var sig,env,safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.asr(att, amp, rls),gate, doneAction:2);
				SendTrig.kr(Impulse.kr(1),1,freq);
				sig = SinOsc.ar(freq, 0, env);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef("formR", { arg freq=440, ffreq=1200, bw=0.2, lofreq=0.5,mod=0,
					att=0.01, rls=0.25, pan=0,elev=0, amp=0.1, out=0, effBus=effbus, effAmp=0, gate=1;
				var env,sig, safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.asr(att, amp, rls),gate, doneAction:2);
				sig = Formant.ar(freq, ffreq+SinOsc.kr(lofreq,Rand(0,2pi),mod*ffreq*0.5),
					bw*freq, env)*0.2;
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\dblPluckR,{ arg freq=400,ratio=2,brit=2000,
				plDcy=0.5,clip=1,rq=0.1,pan=0,elev=0,amp=0.1,out=0, effBus=effbus, effAmp=0, gate=1;
				var exciter,sig,env,bright,safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.asr(0.0025,amp,0.05),gate,doneAction: 2);
				bright = brit+freq;
				exciter = Decay.ar(Impulse.ar(0.05), 0.1,WhiteNoise.ar);
				sig = RLPF.ar(CombC.ar(exciter,0.05,[freq.reciprocal,(freq*ratio).reciprocal],
						plDcy,mul:0.5),bright,rq,env*0.5);
				sig = Mix(sig).clip2(clip);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,


			SynthDef(\dblStringR, { arg freq = 440, ratio=2, rq=0.1,att, rls, pan=0,elev=0,
				out=0, effBus=effbus, effAmp=0, amp=0.1, gate=1;
				var sig, env, fc, osc, osc2, a, b,safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.asr(att,amp,rls), gate, doneAction:2);
				fc = LinExp.kr(LFNoise1.kr(Rand(0.25,0.4)), -1,1,500,2000);
				osc = Mix.fill(4, {Mix(LFSaw.ar(freq * [Rand(0.99,1.01),Rand(0.99,1.01)],
						0, amp)) }).distort * 0.2;
				osc2 = Mix.fill(4, {Mix(LFSaw.ar(freq*ratio * [Rand(0.99,1.01),Rand(0.99,1.01)],
						0, amp)) }.distort * 0.2);
				sig = RLPF.ar(Mix([osc,osc2]), fc, rq, env);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\claveR, { arg freq=440, amp=0.1, ratio, idx, att=0.001, rls=0.5, pan=0,
				elev=0, out=0, effBus=effbus, effAmp=0, gate=1;
				var sig,env,safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2); 				env = EnvGen.kr(Env.asr(att,amp,rls), gate, doneAction:2);
				sig = Decay2.ar(Impulse.ar(0.1,mul: 1000.0/freq),att,rls,
							PMOsc.ar(freq,freq*ratio,idx,0, env));
				sig = Compander.ar(sig,sig,thresh: 0.1, slopeBelow: 1, slopeAbove: 0.5,
					clampTime: 0.01,relaxTime: 0.01 );
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\claveRp, { arg freq=440, amp=0.1, ratio, idx, att=0.001, rls=0.5, pan=0,
				elev=0, out=0, effBus=effbus, effAmp=0, gate=1;
				var sig,env,w,x,y,z;
 				env = EnvGen.kr(Env.perc(att,rls,amp), gate, doneAction:2);
				sig = Decay2.ar(Impulse.ar(0.1,mul: 1000.0/freq),att,rls,
							PMOsc.ar(freq,freq*ratio,idx,0, env));
				sig = Compander.ar(sig,sig,thresh: 0.1, slopeBelow: 1, slopeAbove: 0.5,
					clampTime: 0.01,relaxTime: 0.01 );
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\mnomeR, { arg freq=440, amp=0.1, att=0.001, rls=0.5, out=0, effBus=effbus,
				effAmp=0, gate=1;
				var sig,env,safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2); 				env = EnvGen.kr(Env.asr(0.02,amp,rls), gate, doneAction:2);
				sig = Decay2.ar(Impulse.ar(0.1,mul: 1000.0/freq),att,rls,
							FSinOsc.ar(freq,0,env));
				sig = Compander.ar(sig,sig,thresh: 0.1, slopeBelow: 1, slopeAbove: 0.5,
					clampTime: 0.01,relaxTime: 0.01 );
				Out.ar(effBus, sig*effAmp);
				Out.ar(out,sig)
			}).store,

			SynthDef(\mnomeRp, { arg freq=440, amp=0.1, att=0.001, rls=0.5, out=0, effBus=effbus,
				effAmp=0, gate=1;
				var sig,env,w,x,y,z;							env = EnvGen.kr(Env.perc(att,rls,amp), gate, doneAction:2);
				sig = Decay2.ar(Impulse.ar(0.1,mul: 1000.0/freq),att,rls,
							FSinOsc.ar(freq,0,env));
				sig = Compander.ar(sig,sig,thresh: 0.1, slopeBelow: 1, slopeAbove: 0.5,
					clampTime: 0.01,relaxTime: 0.01 );
				Out.ar(effBus, sig*effAmp);
				Out.ar(out,sig)
			}).store,

			SynthDef(\barClvR, { arg freq=440, amp=0.1, att=0.001, rls=0.5,pan=0,elev=0,
				out=0, effBus=effbus, effAmp=0, gate=1;
				var sig,env,safeEnv,safeDur=10,w,x,y,z;
				freq = freq/2;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.asr(att,amp,rls), gate, doneAction:2);
				sig = Decay2.ar(Impulse.ar(0.1,mul: 1000.0/freq),att,rls,
							FSinOsc.ar(freq,0,env));
				sig = Compander.ar(sig,sig,thresh: 0.1, slopeBelow: 1, slopeAbove: 0.5,
					clampTime: 0.01,relaxTime: 0.01 );
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\barClvRp, { arg freq=440, amp=0.1, att=0.001, rls=0.5,pan=0,elev=0,
				out=0, effBus=effbus, effAmp=0, gate=1;
				var sig,env,w,x,y,z;
				freq = freq/2;
				env = EnvGen.kr(Env.perc(att,rls,amp), gate, doneAction:2);
				sig = Decay2.ar(Impulse.ar(0.1,mul: 1000.0/freq),att,rls,
							FSinOsc.ar(freq,0,env));
				sig = Compander.ar(sig,sig,thresh: 0.1, slopeBelow: 1, slopeAbove: 0.5,
					clampTime: 0.01,relaxTime: 0.01 );
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\bassClvR, { arg freq=440, amp=0.1, att=0.001, rls=0.5,pan=0,elev=0,
				out=0, effBus=effbus, effAmp=0, gate=1;
				var sig,env,safeEnv,safeDur=10,w,x,y,z;
				freq = freq/4;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.asr(att,amp,rls), gate, doneAction:2);
				sig = Decay2.ar(Impulse.ar(0.1,mul: 1000.0/freq),att,rls,
							FSinOsc.ar(freq,0,env));
				sig = Compander.ar(sig,sig,thresh: 0.1, slopeBelow: 1, slopeAbove: 0.5,
					clampTime: 0.01,relaxTime: 0.01 );
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\bassClvRp, { arg freq=440, amp=0.1, att=0.001, rls=0.5,pan=0,elev=0,
				out=0, effBus=effbus, effAmp=0, gate=1;
				var sig,env,w,x,y,z;
				freq = freq/4;
				env = EnvGen.kr(Env.perc(att,rls,amp), gate, doneAction:2);
				sig = Decay2.ar(Impulse.ar(0.1,mul: 1000.0/freq),att,rls,
							FSinOsc.ar(freq,0,env));
				sig = Compander.ar(sig,sig,thresh: 0.1, slopeBelow: 1, slopeAbove: 0.5,
					clampTime: 0.01,relaxTime: 0.01 );
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\wavOscR,{arg freq=220,att=0.025, rls=0.05, pan=0,elev=0,amp=0.1,
					out=0, effBus=effbus, effAmp=0, gate=1;
				var env,sig,safeEnv,safeDur=10,w,x,y,z, buf;
				var list = Wavetable.sineFill(512, 1.0 / [1,3,5,7]);
				buf = LocalBuf.newFrom(list);
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.asr(att,amp,rls),gate,doneAction: 2);
				sig = Osc.ar(buf,freq,0,env);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\ringzR, { arg freq=440, in=0, amp=0.1, att=0.001,
				rls=0.5,pan=0,elev=0,out=0, effBus=effbus, effAmp=0, gate=1;
				var sig,env,safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2); 				env = EnvGen.kr(Env.asr(att,amp,rls), gate, doneAction:2);
				sig = Ringz.ar(SoundIn.ar(in),freq,rls,env);
				sig = Decay2.ar(Impulse.ar(0.1,mul: 1000.0/freq),att,rls,sig);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\minimoog2R,{ arg freq=440,int1=5,int2 = -5,
						width1=0.1,width2=0.1,width3=0.1,
						ffrqInt=0, rq=0.4, amp=0.1, att=0.001,
						rls=0.5, pan=0, out=0, effBus=effbus, effAmp=0, elev=0, gate=1;
				var sig,env,safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.asr(att,amp,rls), gate, doneAction:2);
				sig=Pulse.ar([ freq  * int1.midiratio, freq, freq * int2.midiratio],
					[ width1,width2,width3],0.3);
				sig = RLPF.ar(Mix.ar(sig),freq * ffrqInt.midiratio,rq,env);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\minimoog2Rp,{ arg freq=440,int1=5,int2 = -5,
						width1=0.1,width2=0.1,width3=0.1,
						ffrqInt=0, rq=0.4, amp=0.1, att=0.001,
						rls=0.5, pan=0, out=0, effBus=effbus, effAmp=0, elev=0, gate=1;
				var sig,env,w,x,y,z;
				env = EnvGen.kr(Env.perc(att,rls,amp), gate, doneAction:2);
				sig=Pulse.ar([ freq  * int1.midiratio, freq, freq * int2.midiratio],
					[ width1,width2,width3],0.3);
				sig = RLPF.ar(Mix.ar(sig),freq * ffrqInt.midiratio,rq,env);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef("granSamplerR",{ arg bufnum=0, bufOffset=0, rate=1,start=0,end=1, loop=1,
				lofreq=2, mod=0, att, rls, pan=0,elev=0, amp=0.1, out=0, effBus=effbus, effAmp=0, gate=1;
				var dir, thisRate, thisDur, loopFrq, sig, env, sampler,
					sineLFO,safeEnv,safeDur=26,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.asr(att, amp,rls), gate, doneAction: 2);
				sineLFO = SinOsc.kr(lofreq,0,mod);
				dir = (end-start).sign;
				bufnum=bufnum+(bufOffset);  // bufOffset addresses multiple SampleBuffers
				thisRate = (BufRateScale.kr(bufnum)*rate)+sineLFO*dir;
				thisDur = ((end-start).abs*BufDur.kr(bufnum)).max(att+rls);
				loopFrq = (1.0/thisDur).min(1.0/(att+rls));
				sampler = PlayBuf.ar(1,bufnum,thisRate, Impulse.kr(loopFrq)*loop,
							start*BufFrames.kr(bufnum),loop)*
				// an EnvGen is 'gated' at the same rate as the sample is looped
					EnvGen.ar(Env.new([0,1,1,0],[0.0125,1.0-0.025,0.0125],\welch),
						Impulse.kr(loopFrq),timeScale: thisDur.max(BufRateScale.kr(bufnum)*0.01));
				sig = sampler*env;
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef("granSamplerRp",{ arg bufnum=0, bufOffset=0, rate=1,start=0,end=1, loop=1,
				lofreq=2, mod=0, att, sdur, rls, pan=0,elev=0, amp=0.1, out=0, effBus=effbus, effAmp=0, gate=1;
				var dir, thisRate, thisDur, loopFrq, sig, env, sampler,
					sineLFO,safeEnv,safeDur=10,w,x,y,z;
				env = EnvGen.kr(Env.linen(att,sdur,rls,amp), gate, doneAction:2);
				sineLFO = SinOsc.kr(lofreq,0,mod);
				dir = (end-start).sign;
				bufnum=bufnum+(bufOffset);  // bufOffset addresses multiple SampleBuffers
				thisRate = (BufRateScale.kr(bufnum)*rate)+sineLFO*dir;
				thisDur = ((end-start).abs*BufDur.kr(bufnum)).max(att+rls);
				loopFrq = (1.0/thisDur).min(1.0/(att+rls));
				sampler = PlayBuf.ar(1,bufnum,thisRate, Impulse.kr(loopFrq)*loop,
							start*BufFrames.kr(bufnum),loop)*
				// an EnvGen is 'gated' at the same rate as the sample is looped
					EnvGen.ar(Env.new([0,1,1,0],[0.0125,1.0-0.025,0.0125],\welch),
						Impulse.kr(loopFrq),timeScale: thisDur.max(BufRateScale.kr(bufnum)*0.01));
				sig = sampler*env;
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef("randGranSamplerR",{ arg bufnum=0,bufOffset=0,rate=1,start=0,end=1, lofreq=2, mod=0,
					  att, rls, pan=0,elev=0, amp=0.1, out=0, effBus=effbus, effAmp=0, gate=1;
				var dir, thisRate, thisDur, loopFrq,sig, env, sampler,
					sineLFO, attRelTime=0.05, rand,safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.asr(att, amp,rls), gate, doneAction: 2 );
				sineLFO = SinOsc.kr(lofreq,0,mod);
				dir = (end-start).sign;
				bufnum=bufnum+(bufOffset);  // bufOffset addresses multiple SampleBuffers
				thisRate = (BufRateScale.kr(bufnum)*rate)+sineLFO*dir;
				thisDur = ((end-start).abs*BufDur.kr(bufnum)).max(attRelTime);
				rand =TRand.kr(start,end,Impulse.kr(thisDur.reciprocal));
				thisDur = ((end-rand).abs*BufDur.kr(bufnum)).max(attRelTime);
				loopFrq = (1.0/thisDur).min(1.0/(attRelTime/2));
				sampler = PlayBuf.ar(1,bufnum,thisRate, Impulse.kr(loopFrq),
							rand*BufFrames.kr(bufnum),1)*
					// an EnvGen is 'gated' at the same rate as the sample is looped
					EnvGen.ar(Env.new([0,1,1,0],[0.0125,1.0-0.025,0.0125],\welch),
						Impulse.kr(loopFrq),timeScale: thisDur.max(BufRateScale.kr(bufnum)*0.01));
				sig = sampler*env;
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef("randGranSamplerRp",{ arg bufnum=0,bufOffset=0,rate=1,start=0,end=1, lofreq=2, mod=0,
					  att,sdur,rls, pan=0,elev=0, amp=0.1, out=0, effBus=effbus, effAmp=0, gate=1;
				var dir, thisRate, thisDur, loopFrq,sig, env, sampler,
					sineLFO, attRelTime=0.05, rand,safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.linen(att,sdur,rls,amp), gate, doneAction:2);
				sineLFO = SinOsc.kr(lofreq,0,mod);
				dir = (end-start).sign;
				bufnum=bufnum+(bufOffset);  // bufOffset addresses multiple SampleBuffers
				thisRate = (BufRateScale.kr(bufnum)*rate)+sineLFO*dir;
				thisDur = ((end-start).abs*BufDur.kr(bufnum)).max(attRelTime);
				rand =TRand.kr(start,end,Impulse.kr(thisDur.reciprocal));
				thisDur = ((end-rand).abs*BufDur.kr(bufnum)).max(attRelTime);
				loopFrq = (1.0/thisDur).min(1.0/(attRelTime/2));
				sampler = PlayBuf.ar(1,bufnum,thisRate, Impulse.kr(loopFrq),
							rand*BufFrames.kr(bufnum),1)*
					// an EnvGen is 'gated' at the same rate as the sample is looped
					EnvGen.ar(Env.new([0,1,1,0],[0.0125,1.0-0.025,0.0125],\welch),
						Impulse.kr(loopFrq),timeScale: thisDur.max(BufRateScale.kr(bufnum)*0.01));
				sig = sampler*env;
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef("rndGrainSampR",{ arg bufnum=0,bufOffset=0,rate=1,start=0,end=1, spd=1,gfrq=8,gdur=0.2,
				lofreq=2, mod=0,att,rls,pan=0,elev=0, amp=0.1, out=0, effBus=effbus, effAmp=0, gate=1;
				var dir,thisRate,thisDur,sig,env,sampler,length,
					rand,clk,startPos,endPos,centerPos,sineLFO, attRelTime=0.05,
				safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.asr(att, amp,rls), gate, doneAction: 2 );
				sineLFO = SinOsc.kr(lofreq,0,mod);
				dir = (end-start).sign;
				bufnum=bufnum+(bufOffset);  // bufOffset addresses multiple SampleBuffers
				thisRate = (BufRateScale.kr(bufnum)*rate)+sineLFO*dir;
				thisDur = ((end-start).abs*BufDur.kr(bufnum)).max(attRelTime);
				rand =TRand.kr(start,end,Impulse.kr(thisDur.reciprocal));
				thisDur = ((end-rand).abs*BufDur.kr(bufnum)).max(attRelTime);
				startPos = rand*BufDur.kr(bufnum);
				endPos = end*BufDur.kr(bufnum);
				length = (endPos-startPos);
				centerPos = LFSaw.kr(spd*(length.abs.max(0.01).reciprocal),0,0.5*length,length);
				clk = Impulse.kr(gfrq);
				sampler = Mix(TGrains.ar(2,clk,bufnum,BufRateScale.kr(bufnum)*rate,
					startPos+centerPos,gdur.max(0.005),0,1,2));
				sig = sampler*env;
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef("rndGrainSampRp",{ arg bufnum=0,bufOffset=0,rate=1,start=0,end=1, spd=1,gfrq=8,gdur=0.2,
				lofreq=2, mod=0,att,sdur,rls,pan=0,elev=0, amp=0.1, out=0, effBus=effbus, effAmp=0, gate=1;
				var dir,thisRate,thisDur,sig,env,sampler,length,
					rand,clk,startPos,endPos,centerPos,sineLFO, attRelTime=0.05,
				safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.linen(att,sdur,rls,amp), gate, doneAction:2);
				sineLFO = SinOsc.kr(lofreq,0,mod);
				dir = (end-start).sign;
				bufnum=bufnum+(bufOffset);  // bufOffset addresses multiple SampleBuffers
				thisRate = (BufRateScale.kr(bufnum)*rate)+sineLFO*dir;
				thisDur = ((end-start).abs*BufDur.kr(bufnum)).max(attRelTime);
				rand =TRand.kr(start,end,Impulse.kr(thisDur.reciprocal));
				thisDur = ((end-rand).abs*BufDur.kr(bufnum)).max(attRelTime);
				startPos = rand*BufDur.kr(bufnum);
				endPos = end*BufDur.kr(bufnum);
				length = (endPos-startPos);
				centerPos = LFSaw.kr(spd*(length.abs.max(0.01).reciprocal),0,0.5*length,length);
				clk = Impulse.kr(gfrq);
				sampler = Mix(TGrains.ar(2,clk,bufnum,BufRateScale.kr(bufnum)*rate,
					startPos+centerPos,gdur.max(0.005),0,1,2));
				sig = sampler*env;
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef("tGrainSampR",{ arg bufnum=0,bufOffset=0, rate=1,start=0,end=1,
				spd=1,gfrq=8,gdur=0.2,att, rls, pan=0,elev=0,amp=0.1,out=0,
				effBus=effbus, effAmp=0, gate=1;
				var sig,centerPos,clk,env,startPos,endPos,length,
				safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.asr(0.025,amp,0.05),gate,doneAction:2);
				bufnum=bufnum+(bufOffset);  // bufOffset addresses multiple SampleBuffers
				startPos = start*BufDur.kr(bufnum);
				endPos = end*BufDur.kr(bufnum);
				length = (endPos-startPos);
				centerPos = LFSaw.kr(spd*(length.abs.max(0.01).reciprocal),0,0.5*length,length);
				clk = Impulse.kr(gfrq);
				sig = TGrains.ar(2,clk,bufnum,BufRateScale.kr(bufnum)*rate,
					startPos+centerPos,gdur.max(0.005),pan,1,2);
				sig = Mix(sig)*env;
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef("tGrainSampRp",{ arg bufnum=0,bufOffset=0,
				rate=1,start=0,end=1,spd=1,gfrq=8,gdur=0.2,
				att,sdur,rls, pan=0,elev=0,amp=0.1,out=0,
				effBus=effbus, effAmp=0, gate=1;
				var sig,centerPos,clk,env,startPos,endPos,length,
				safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.linen(att,sdur,rls,amp), gate, doneAction:2);
				bufnum=bufnum+(bufOffset);  // bufOffset addresses multiple SampleBuffers
				startPos = start*BufDur.kr(bufnum);
				endPos = end*BufDur.kr(bufnum);
				length = (endPos-startPos);
				centerPos = LFSaw.kr(spd*(length.abs.max(0.01).reciprocal),0,0.5*length,length);
				clk = Impulse.kr(gfrq);
				sig = TGrains.ar(2,clk,bufnum,BufRateScale.kr(bufnum)*rate,
					startPos+centerPos,gdur.max(0.005),pan,1,2);
				sig = Mix(sig)*env;
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef("tGrainScanR",{ arg bufnum=0,bufOffset=0, rate=1,start=0,end=1,spd=1,gfrq=8,gdur=0.2,
					att, rls, pan=0,elev=0,amp=0.1,out=0, effBus=effbus, effAmp=0, gate=1;
				var sig,thisDur,thisPos,env,startPos,endPos,length,
					safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.asr(att,amp,rls),gate,doneAction:2);
				bufnum=bufnum+(bufOffset);  // bufOffset addresses multiple SampleBuffers
				startPos = start*BufDur.kr(bufnum);
				endPos = end*BufDur.kr(bufnum);
				length = (startPos-endPos).abs;
				thisPos= Phasor.ar(0,(SampleRate.ir.reciprocal)*spd,0,length);
				gdur=gdur.max(0.005);
				sig = TGrains.ar(2,Impulse.kr(gfrq),bufnum,BufRateScale.kr(bufnum)*rate,
					(start*BufDur.kr(bufnum))+thisPos+(gdur/2),gdur,pan,1,2);
				sig = Mix(sig)*env;
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef("tGrainScanRp",{ arg bufnum=0,bufOffset=0, rate=1,start=0,end=1,spd=1,gfrq=8,gdur=0.2,
					att,sdur=0.2,rls, pan=0,elev=0,amp=0.1,out=0, effBus=effbus, effAmp=0, gate=1;
				var sig,thisDur,thisPos,env,startPos,endPos,length,
					safeEnv,safeDur=8,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.linen(att,sdur,rls,amp), gate, doneAction:2);
				bufnum=bufnum+(bufOffset);  // bufOffset addresses multiple SampleBuffers
				startPos = start*BufDur.kr(bufnum);
				endPos = end*BufDur.kr(bufnum);
				length = (startPos-endPos).abs;
				thisPos= Phasor.ar(0,(SampleRate.ir.reciprocal)*spd,0,length);
				gdur=gdur.max(0.005);
				sig = TGrains.ar(2,Impulse.kr(gfrq),bufnum,BufRateScale.kr(bufnum)*rate,
					(start*BufDur.kr(bufnum))+thisPos+(gdur/2),gdur,pan,1,2);
				sig = Mix(sig)*env;
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef("tGrnScnPhsR",{ arg bufnum=0,bufOffset=0,rate=1,start=0,end=1,spd=1,gfrq=8,
				gdur=0.2, numTeeth=16,phs=0, plsWdth=0.2,att, rls, pan=0,elev=0,amp=0.1,
				out=0, effBus=effbus, effAmp=0, gate=1;
				var sig,granulator,thisDur,thisPos,env,startPos,endPos,length,
					safeEnv,safeDur=10,w,x,y,z,chain;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.asr(att,amp,rls),gate,doneAction:2);
				bufnum=bufnum+(bufOffset);  // bufOffset addresses multiple SampleBuffers
				startPos = start*BufDur.kr(bufnum);
				endPos = end*BufDur.kr(bufnum);
				length = (startPos-endPos).abs;
				thisPos= Phasor.ar(0,(SampleRate.ir.reciprocal)*spd,0,length);
				gdur=gdur.max(0.005);
				granulator = TGrains.ar(2,Impulse.kr(gfrq),bufnum,BufRateScale.kr(bufnum)*rate,
					(start*BufDur.kr(bufnum))+thisPos+(gdur/2),gdur,pan,1,2);
				granulator =  Mix(granulator)*env;
				chain = FFT(LocalBuf(2048), granulator);
				chain = PV_RectComb(chain, numTeeth,phs, plsWdth);
				sig = IFFT(chain);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef("tGrnScnPhsRp",{ arg bufnum=0,bufOffset=0, rate=1,start=0,end=1,spd=1,gfrq=8,
				gdur=0.2, numTeeth=16,phs=0, plsWdth=0.2,att,sdur,rls, pan=0,elev=0,amp=0.1,
				out=0, effBus=effbus, effAmp=0, gate=1;
				var sig,granulator,thisDur,thisPos,env,startPos,endPos,length,
					safeEnv,safeDur=10,w,x,y,z,chain;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.linen(att,sdur,rls,amp), gate, doneAction:2);
				startPos = start*BufDur.kr(bufnum);
				bufnum=bufnum+(bufOffset);  // bufOffset addresses multiple SampleBuffers
				endPos = end*BufDur.kr(bufnum);
				length = (startPos-endPos).abs;
				thisPos= Phasor.ar(0,(SampleRate.ir.reciprocal)*spd,0,length);
				gdur=gdur.max(0.005);
				granulator = TGrains.ar(2,Impulse.kr(gfrq),bufnum,BufRateScale.kr(bufnum)*rate,
					(start*BufDur.kr(bufnum))+thisPos+(gdur/2),gdur,pan,1,2);
				granulator =  Mix(granulator)*env;
				chain = FFT(LocalBuf(2048), granulator);
				chain = PV_RectComb(chain, numTeeth,phs, plsWdth);
				sig = IFFT(chain);
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef("scratchR",{ arg bufnum=0,bufOffset=0,rate=1, endRt=0.5,start=0,end=1,spd=1,
					mfrq1=1.0,fMod=0, loop=1.0,att, rls, pan=0,elev=0, amp=0.2,
					out=0, effBus=effbus, effAmp=0, gate=1;
				var sig,clk,env,scratchEnv,startPos,endPos,length,trgf,
				safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.asr(att,amp,rls),gate:gate,doneAction:2);
				spd = SinOsc.kr(mfrq1,0,fMod,spd);
				bufnum=bufnum+(bufOffset);  // bufOffset addresses multiple SampleBuffers
				startPos = start*BufDur.kr(bufnum);
				endPos = end*BufDur.kr(bufnum);
				length = (endPos-startPos).abs;
				trgf = length.reciprocal;
				scratchEnv = EnvGen.kr(Env.new([1.0, 1.0, 0.01, 1.0, 1.0],
					[0.01,0.49,0.49, 0.01],\sin,3,0),
						gate, timeScale: length*((spd).reciprocal),
							levelScale: (rate-endRt).abs, doneAction: 2);
				scratchEnv = ((rate>=endRt)*(scratchEnv+endRt)) +
									((endRt>rate)*(endRt-scratchEnv));
				sig = PlayBuf.ar(1,bufnum,BufRateScale.kr(bufnum)*scratchEnv,
					Impulse.kr(trgf)*loop, start*BufFrames.kr(bufnum),loop)
					*EnvGen.ar(Env.new([0,1,1,0],[0.025,1.0-0.05,0.025],\welch),
						Impulse.kr(trgf),timeScale: length)*env;
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef("scratchRp",{ arg bufnum=0,bufOffset=0,rate=1, endRt=0.5,start=0,end=1,spd=1,
					mfrq1=1.0,fMod=0, loop=1.0,att,sdur,rls, pan=0,elev=0, amp=0.2,
					out=0, effBus=effbus, effAmp=0, gate=1;
				var sig,clk,env,scratchEnv,startPos,endPos,length,trgf,
				safeEnv,safeDur=10,w,x,y,z;
				safeEnv = EnvGen.kr(Env.new([0,1,1,0],[0.001,safeDur,0.1],-4), doneAction: 2);
				env = EnvGen.kr(Env.linen(att,sdur,rls,amp), gate, doneAction:2);
				spd = SinOsc.kr(mfrq1,0,fMod,spd);
				bufnum=bufnum+(bufOffset);  // bufOffset addresses multiple SampleBuffers
				startPos = start*BufDur.kr(bufnum);
				endPos = end*BufDur.kr(bufnum);
				length = (endPos-startPos).abs;
				trgf = length.reciprocal;
				scratchEnv = EnvGen.kr(Env.new([1.0, 1.0, 0.01, 1.0, 1.0],
					[0.01,0.49,0.49, 0.01],\sin,3,0),
						gate, timeScale: length*((spd).reciprocal),
							levelScale: (rate-endRt).abs, doneAction: 2);
				scratchEnv = ((rate>=endRt)*(scratchEnv+endRt)) +
									((endRt>rate)*(endRt-scratchEnv));
				sig = PlayBuf.ar(1,bufnum,BufRateScale.kr(bufnum)*scratchEnv,
					Impulse.kr(trgf)*loop, start*BufFrames.kr(bufnum),loop)
					*EnvGen.ar(Env.new([0,1,1,0],[0.025,1.0-0.05,0.025],\welch),
						Impulse.kr(trgf),timeScale: length)*env;
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\diskIn_granR,{ arg bufnum=0,bufOffset=0,spd=1,dens=1,att, rls, pan=0,elev=0,amp=0.1,
				out=0, effBus=effbus, effAmp=0, gate=1;
				var sig,stream1,stream2,stream3,stream4,env,grainEnv,w,x,y,z;
				grainEnv = EnvGen.kr(Env.new([ 0.001,1.0,0.001], [ 0.5,0.5 ], 'sine'),
					Impulse.kr(spd), timeScale: spd.reciprocal*dens);
				env = EnvGen.kr(Env.asr(att,amp,rls),gate: gate,doneAction: 2);
				bufnum=bufnum+(bufOffset);  // bufOffset addresses multiple SampleBuffers
				sig = Mix(DiskIn.ar(2,bufnum));
				stream1 = sig*grainEnv;
				stream2 = sig*DelayN.kr(grainEnv,1,spd.reciprocal*0.25);
				stream3 = sig*DelayN.kr(grainEnv,1,spd.reciprocal*0.5);
				stream4 = sig*DelayN.kr(grainEnv,1,spd.reciprocal*0.75);
				sig = Mix([stream1,stream2,stream3,stream4])*env;
				Out.ar(effBus, sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store
		]
	}
}
