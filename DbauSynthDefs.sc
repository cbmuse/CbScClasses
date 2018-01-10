DbauSynthDefs {
	classvar <>synthDefs;

	*init {

		synthDefs = [

			// dbau record tracking

			SynthDef(\dbau_DiskIn,{|inbuf=0, outbus=16,loop=0 |
				var in = DiskIn.ar(1,inbuf,loop);
				Out.ar([0,outbus],in);
			}).add;

			SynthDef(\dbau_in,{|inbus=0, outbus=16|
				var in = SoundIn.ar(0);
				Out.ar(outbus,in);
			}).add;

			SynthDef(\dbau_track,{|inbus=16, pThresh=0.96, aThresh=0.002 |
				var freq, hasFreq, amp;
				var in=In.ar(inbus);
				amp = Amplitude.kr(in);
				# freq, hasFreq = Tartini.kr(in);
				SendReply.kr((hasFreq>pThresh)*(amp>aThresh)*Impulse.kr(20),
					'/dbauEvent',[freq,amp],0);
			}).add;

			// limiter
			SynthDef(\dbau_limiter,{|inbus=0,outbus=0,thresh=0.5,
				slopeBelow=1,slopeAbove=0.5,amp=0.1|
				var sig = In.ar(inbus,2);
				sig = Compander.ar(sig,sig,thresh,slopeBelow,slopeAbove,0.01,0.01);
				Out.ar(outbus,sig*amp)
			}).add;

			SynthDef(\dbau_reverb,{|inbus=0,outbus=0,amp=0.1,rvbVol=0.25,fb=0.25,flt=2000,gate=1|
				var local,in,env;
				in = In.ar(inbus);
				local = LocalIn.ar(2) * fb.min(0.98);
				local = OnePole.ar(local, flt.min(0.98));
				local = Rotate2.ar(local[0], local[1], 0.237);
				local = AllpassN.ar(local, 0.05, {Rand(0.01,0.05)}.dup, 2);
				local = DelayN.ar(local, 0.3, {Rand(0.15,0.33)}.dup);
				local = AllpassN.ar(local, 0.05, {Rand(0.03,0.15)}.dup, 2);
				local = LeakDC.ar(local);
				local = local + in;
				4.do {var t;
					t = {Rand(0.005,0.02)}.dup;
					local = AllpassN.ar(local, t, t, 1);
				};
				LocalOut.ar(local);
				env = EnvGen.kr(Env.asr(0.01,1,0.1),gate: gate, doneAction: 2);
				Out.ar(outbus,local*rvbVol*amp)
			}).add;

			// granular synths

			SynthDef("granSin",{ arg freq=400,lfreq=0.5,mod=0,gdur=0.01,pan=0,amp=0.1,out=0;
				var env = Env.sine(gdur);
				var modfreq = SinOsc.kr(lfreq,0,mod*(freq*0.04));
				var sig = SinOsc.ar(freq+modfreq,0,EnvGen.ar(env,doneAction: 2));
				Out.ar(out,
					Pan2.ar(sig,pan,amp))
			}).add;

			SynthDef("granNse",{ arg freq=400,gdur=0.01,lfreq=0.5,mod=0,pan=0,amp=0.1,out=0;
				var env = Env.sine(gdur);
				var modfreq = SinOsc.kr(lfreq,0,mod*(freq*0.04));
				var sig = Ringz.ar(WhiteNoise.ar(freq.explin(20,8000,0.001,0.02)),
					freq+modfreq,gdur,amp);
				Out.ar(out,
					Pan2.ar(sig,pan,EnvGen.ar(env,doneAction: 2)))
			}).add;

			SynthDef("granRingz",{ arg freq=400,lfreq=0.5,mod=0,gdur=0.001,rls=0.1,pan=0,
				amp=0.1,out=0;
				var env = Env.new([0,1,0],[0.25,0.75],\welch);
				var modfreq = SinOsc.kr(lfreq,0,mod*(freq*0.04));
				var sig =Ringz.ar(Decay2.ar(Impulse.ar(0.005,0,0.25),
					0.001,gdur,ClipNoise.ar(freq.explin(20,8000,0.1,0.4))),
					freq+modfreq,gdur,amp);
				Out.ar(out,Pan2.ar(sig,pan,EnvGen.ar(env,doneAction: 2,
					timeScale: gdur.max(0.00025))))
			}).add;

			SynthDef("granPinkRingz",{ arg freq=400,lfreq=0.5,mod=0,gdur=0.01,rls=0.1,pan=0,
				amp=0.1,out=0;
				var env = Env.new([0,1,0],[0.25,0.75],\welch);
				var modfreq = SinOsc.kr(lfreq,0,mod*(freq*0.04));
				Out.ar(out,
					EnvGen.ar(env,doneAction: 2,
						timeScale: gdur.max(0.00025))*
					Pan2.ar(
						Ringz.ar(
							Decay2.ar(Impulse.ar(0.01,0,0.25),0.001,rls,
								PinkNoise.ar(freq.explin(20,8000,0.2,2.0))),
							freq+modfreq,gdur,amp),
						pan))
			}).add;

			SynthDef(\granFM, {arg freq=200,rtio=1.0,lfreq=1,idx=1,mod=0,
				gdur=0.01,amp = 0.2,pan=0,out=0;
				Out.ar(out,Pan2.ar(
					PMOsc.ar(freq+SinOsc.kr(lfreq,0,mod*freq*0.1),freq*rtio,idx,0)
					*EnvGen.kr(Env([0,1,0],[gdur*0.5,gdur*0.5],\sin),
						levelScale: amp, doneAction: 2),pan))
			}).add;

			SynthDef(\granEnvIdxFM, {arg freq=200,rtio=1,idx=1,idxmin=1,
				crv = -4,gdur=0.01,amp = 0.2,pan=0,out=0;
				var sig = PMOsc.ar(freq,freq*rtio,
					EnvGen.kr(Env.perc(0.01,gdur-0.01,idx,crv),doneAction:2)+idxmin);
				Out.ar(out,Pan2.ar(sig,pan,EnvGen.kr(Env([0, 1, 0],[gdur*0.5,gdur*0.5],\sin),
					levelScale: amp, doneAction: 2)))
			}).add;

			SynthDef(\granFmDrum,{|freq=200,rtio=1,lfreq=1,mod=0,idx=1,gdur=0.01,
				pan=0,rls=0.1,amp=0.2,out=0|
				var env = Env.new([0,1,0],[0.25,0.75],\welch);
				Out.ar(out,
					EnvGen.ar(env,doneAction: 2,
						timeScale: gdur.max(0.00025))*
					Pan2.ar(
						Decay2.ar(Impulse.ar(0.1),0.001,gdur,
							PMOsc.ar(freq+SinOsc.kr(lfreq,0,mod*freq*0.5),
								freq*rtio,idx)),
						pan,amp))
			}).add;

			SynthDef(\granChime,{arg freq=440,pan=0,gdur=0.1,mod=0,lfreq=0.1,rtio=1.0,
				att=0.01,amp=0.1,out=0;
				var env, modSig, specs, harmonics, sig, amps, modrange;
				harmonics = Array.series(8,2,1+(rtio.rand)); harmonics.addFirst(1);
				amps = Array.exprand(12,0.01,0.5).normalize.sort.reverse;
				specs = `[ harmonics, // partials
					amps,  // amplitudes
					Array.rand(12, 0.1, 2) ]; // ring times
				env = EnvGen.kr(Env.linen(att,gdur,att,amp), doneAction:2);
				modSig = SinOsc.kr(lfreq,Rand(0,2pi),mod);
				modrange = (freq - (freq.cpsmidi+1).midicps).abs;
				sig = Klank.ar(specs,
					Decay2.ar(Impulse.ar(0.1),att,gdur-att,ClipNoise.ar(0.01)),
					freq, // freq.scale
					modSig*modrange, // freq.offset
					gdur)*env;
				Out.ar(out,Pan2.ar(sig,pan))
			}).add;

			SynthDef(\granPluck,{ arg freq=400,brite=8,gdur=0.1,coef=0.25,pan=0,
				att=0.005,amp=0.1,out=0;
				var sig,env,bright;
				env = EnvGen.kr(Env.linen(att,gdur*4,0.1,amp),doneAction:2);
				sig = Pluck.ar(WhiteNoise.ar(1),1,0.1,
					freq.reciprocal,gdur,coef.min(0.75),env);
				Out.ar(out,Pan2.ar(LPF.ar(sig,(freq*brite).min(10000)),pan))
			}).add;

			SynthDef(\granClave, {|freq=440,amp=0.1,rtio=1,idx=1,
				att=0.01,gdur=0.5,pan=0,out=0|
				var sig,env;
				env = EnvGen.kr(Env.perc(att,gdur,amp,-2), doneAction:2);
				sig = Decay2.ar(Impulse.ar(0.1,mul: freq.explin(20,8000,0.2,0.4)),
					att,gdur-att,PMOsc.ar(freq,freq*rtio,idx,0, env));
				Out.ar(out,Pan2.ar(sig,pan))
			}).add;

			SynthDef(\granSnare,{|freq=440,lfreq=4,mod=0.1,att=0.001,gdur=0.1,pan=0,amp=0.1,out=0|
				var env,sig,ringz,modSig;
				env = EnvGen.kr(Env.linen(att,gdur,att,amp),doneAction:2);
				modSig = SinOsc.kr(lfreq,Rand(0.0,2pi),mod);
				sig = Ringz.ar(WhiteNoise.ar(freq.explin(20,8000,0.1,0.05)),
					freq+(modSig*freq),(gdur-att)*0.25,env);
				sig = Decay2.ar(Impulse.ar(0.01),att,gdur-att,sig);
				Out.ar(out,Pan2.ar(sig.atan,pan))
			}).add;

			SynthDef(\granWhistle,{ arg freq=800,rq=0.1,lfreq=2,mod=0.1,
				sinePhs=pi,att=0.01,gdur=0.1,amp=0.1,pan=0,out=0;
				var env,noise,sig,modSig,dry;
				env=EnvGen.kr(Env([0, 1, 0],[gdur*0.5,gdur*0.5],\welch),doneAction:2);
				modSig = SinOsc.kr(lfreq,sinePhs,mod*(freq*0.04));
				noise = ClipNoise.ar(freq.explin(20,8000,0.2,1.0));
				sig = Resonz.ar(noise,freq+modSig,rq,env);
				Out.ar(out,Pan2.ar(sig,pan,amp))
			}).add;
		]
	}
}
