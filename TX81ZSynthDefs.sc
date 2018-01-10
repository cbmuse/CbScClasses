// TX81z modeled Synthdefs

TxSynthDefs {
	classvar <>synthDefs, <>numOuts = 2, <>azArray, <>elArray,  <>rhoArray=1, <>maxDist=12,
	<>buffers, <>server, <>skin, <>colors;


	*init {|server|
		if(server.isNil,{ server = Server.default });

		switch(numOuts,
			2,{azArray = [-0.25pi,0.25pi]; elArray = [0,0] },
			4,{azArray = [-0.25pi, 0.25pi, 0.75pi, 1.25pi]; elArray = [0,0,0,0] },
			6,{ azArray = [-0.25pi, 0.25pi, 0.75pi, 1.25pi,-0.5pi, 0.5pi];
				elArray = [-0.5pi,-0.5pi,-0.5pi,-0.5pi, 0.25pi,0.25pi];
				rhoArray = [1,1,1,1,1,1] },
			7,{ // littlefield (3 not used)
				azArray = [-0.454,0.454,0,0,-1.047,1.047,-2.39,2.39];  // angle
				elArray = [0.088,0.088,0,1.22,0.35,0.35,0.524,0.524];  // elev
				maxDist=25;  // meters
				rhoArray = [1,1,0,0,0.56,0.5,0.8,0.8]*maxDist} // rho (distance)} // rho (distance)
		);
		// for player GUIs
		colors = (
			yellow: Color(1.0, 0.8, 0.2, 1.0),
			gold: Color(0.6, 0.6, 0.2, 1.0),
			green: Color(0.2, 0.4, 0.2, 1.0),
			red: Color(0.6, 0.2, 0.0, 1.0),
			blue: Color(0.2, 0.4, 0.6, 1.0),
			brown: Color(0.4, 0.4, 0.2, 1.0)
		);

		skin = (
			fontSpecs:  ["Arial", 10],
			fontColor: 	Color.black,
			background: 	Color(0.8, 0.85, 0.7, 0.5),
			foreground:	Color.grey(0.95),
			onColor:		Color(0.5, 1, 0.5),
			offColor:		Color.clear,
			gap:			0 @ 0,
			margin: 		2@2,
			buttonHeight:	16
		);

		// initialize wavetables

		server.waitForBoot({
			MIDIdef.freeAll;
			MIDIClient.init; "initialized MIDIClient ".postln; // crashes on reboot unless Intepreter is rebooted !
			MIDIIn.connectAll; "connected MIDIIns ".postln;

			buffers =
			[ Wavetable.sineFill(8192, [1]),
				Wavetable.sineFill(8192, [1,0,0.19,0.09,0.03,0, 0.01]),
				Wavetable.sineFill(8192, [1,0.39,0,0.08,0,0.04,0,0.02,0,0.01,0,0.01]),
				Wavetable.sineFill(8192, [1,0.55,0.18,0,0.03,0,0.01]),
				Wavetable.sineFill(8192, [1,1.09,0.56,0,0.15,0,0.06,0,0.04,0,0.02]),
				Wavetable.sineFill(8192, [1,1.16,0.66,0,0.34,0.25,0.06,0,0.03,0.04,0.01]),
				Wavetable.sineFill(8192, [1,0,0.56,0.5,0.15,0,0.06,0.09,0.04,0,0.02,0.04]),
				Wavetable.sineFill(8192, [1,0,0.66,0.74,0.34,0,0.06,0,0.03,0,0.01])
			].collect {|coll| Buffer.sendCollection(server,coll) };

			synthDefs = [
				// 4->3->2->1
				SynthDef(\txAlg1,{ arg freq=440, amp=1, sustain=1,tempo=1,
					pan=0, out=0, elev=0, effAmp=0, effOut=16, vol=0.25,
					wv1=0, wv2=0, wv3=0, fb=0,
					rtio1=1, rtio2=1, rtio3=1, rtio4=1,
					amp1=1, amp2=1, amp3=1, amp4=1,
					p1_1=1, p2_1=0.1, p3_1=0.1, t1_1=0.01, t2_1=0.99, t3_1=0, rls_1=0.1, shft1=0,
					p1_2=1, p2_2=0.1, p3_2=0.1, t1_2=0.01, t2_2=0.99, t3_2=0, rls_2=0.1, shft2=0,
					p1_3=1, p2_3=0.1, p3_3=0.1, t1_3=0.01, t2_3=0.99, t3_3=0, rls_3=0.1, shft3=0,
					p1_4=1, p2_4=0.1, p3_4=0.1, t1_4=0.01, t2_4=0.99, t3_4=0, rls_4=0.1, shft4=0,
					amod1=0, amod2=0, amod3=0, amod4=0,indexMax=6,
					lfoSpd=0.2, pmod=0, vibRange=0.125,
					lfoSinAmp=0, lfoPlsAmp=0, lfoSawAmp=0, lfoNseAmp=0,  // these mix to create lfo shape
					det1=1, det2=1, det3=1, det4=1,
					fxFrq1=0, fxFrq2=0, fxFrq3=0, fxFrq4=0,  // fixed freq alternative
					ffrq=20000;

					var sus = sustain*(tempo.reciprocal);
					var carFreq,mod1Freq,mod2Freq,mod3Freq;
					var lfoAmp,lfoFrq,amodEnv,sig,w,x,y,z;
					// level of carrier env set to 0.5 max to allow summing with ampmod to peak at 1
					var idx1 = EnvGen.kr(Env.new([0.001,p1_1,p2_1,p3_1,0.0001],
						[t1_1,t2_1,t3_1,rls_1],\exp),
						levelScale: amp1.max(0.0001),levelBias: shft1,timeScale:sus,
					doneAction:2); // op1 env ends synth
					var idx2 = EnvGen.kr(Env.new([0.001,p1_2,p2_2,p3_2,0.001],
						[t1_2,t2_2,t3_2,rls_2],\exp),
						levelScale: amp2.max(0.0001),levelBias: shft2,timeScale:sus );
					var idx3 = EnvGen.kr(Env.new([0.001,p1_3,p2_3,p3_3,0.001],
						[t1_3,t2_3,t3_3,rls_3],\exp),
						levelScale: amp3.max(0.0001),levelBias: shft3,timeScale:sus);
					var idx4 = EnvGen.kr(Env.new([0.001,p1_4,p2_4,p3_4,0.001],
						[t1_4,t2_4,t3_4,rls_4],\exp),
						levelScale: amp4.max(0.0001),levelBias: shft4,timeScale:sus );

					lfoAmp = ((SinOsc.kr(lfoSpd).range(0,1)*lfoSinAmp) +
						(LFPulse.kr(lfoSpd,0,0.5).range(0,1)*lfoPlsAmp) +
						(LFSaw.kr(lfoSpd).range(0,1)*lfoSawAmp) +
						(LFNoise1.kr(lfoSpd).range(0,1)*lfoNseAmp))*0.5;

					lfoFrq = (SinOsc.kr(lfoSpd)*lfoSinAmp) +
					(LFPulse.kr(lfoSpd,0).range(-1,1)*lfoPlsAmp) +
					(LFSaw.kr(lfoSpd)*lfoSawAmp) + (LFNoise1.kr(lfoSpd)*lfoNseAmp);

					amodEnv=EnvGen.kr(Env.linen(0.01,1,0.01),timeScale:sus);
					idx1= ((lfoAmp*amod1*amodEnv)+idx1);  //  lfo modulation and idx sum is max 1.0
					idx2= ((lfoAmp*amod2*amodEnv)+idx2)*indexMax;
					idx3= ((lfoAmp*amod3*amodEnv)+idx3)*indexMax;
					idx4= ((lfoAmp*amod4*amodEnv)+idx4)*indexMax;

					// if fxFrq>0 then freq = fxFrq else freq = freq*rtio
					// add DETUNE !!
					carFreq = ((fxFrq1>0)*fxFrq1) + ((fxFrq1<=0)*(rtio1*freq*det1));
					mod1Freq = ((fxFrq2>0)*fxFrq2) + ((fxFrq2<=0)*(rtio2*freq*det2));
					mod2Freq = ((fxFrq3>0)*fxFrq3) + ((fxFrq3<=0)*(rtio3*freq*det3));
					mod3Freq = ((fxFrq4>0)*fxFrq4) + ((fxFrq4<=0)*(rtio4*freq*det4));

					sig = Osc.ar(wv1,
						Osc.ar(wv2,
							Osc.ar(wv3,
								SinOscFB.ar(
									mod3Freq,fb*pi,
									mod3Freq*idx4,		// mul
									mod2Freq			// add
								),
								0,mod2Freq*idx3,   // mul
								mod1Freq  // add
							),
							0,mod1Freq*idx2, 	// mul
							carFreq   // add
						)
						+ (lfoFrq*pmod*vibRange*carFreq),  // carfreq = lfo*pmod + cascade fm
						0,idx1
					);
					sig = LPF.ar(sig,ffrq);
					Out.ar(effOut,sig*amp*effAmp);
					#w,x,y,z = BFEncode1.ar(sig*amp*vol,(pan*pi),elev);
					Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
				}).load(server),


				//    3->2->1
				//    4->2->1
				SynthDef(\txAlg2,{ arg freq=440, amp=1, sustain=1,tempo=1,
					pan=0, out=0, elev=0, effAmp=0, effOut=16, vol=0.25,
					wv1=0, wv2=0, wv3=0, fb=0,
					rtio1=1, rtio2=1, rtio3=1, rtio4=1,
					amp1=1, amp2=1, amp3=1, amp4=1,
					p1_1=1, p2_1=0.1, p3_1=0.1, t1_1=0.01, t2_1=0.99, t3_1=0, rls_1=0.1, shft1=0,
					p1_2=1, p2_2=0.1, p3_2=0.1, t1_2=0.01, t2_2=0.99, t3_2=0, rls_2=0.1, shft2=0,
					p1_3=1, p2_3=0.1, p3_3=0.1, t1_3=0.01, t2_3=0.99, t3_3=0, rls_3=0.1, shft3=0,
					p1_4=1, p2_4=0.1, p3_4=0.1, t1_4=0.01, t2_4=0.99, t3_4=0, rls_4=0.1, shft4=0,
					amod1=0, amod2=0, amod3=0, amod4=0,indexMax=6,
					lfoSpd=0.2, pmod=0, vibRange=0.125,//  default vibrato range = (9/8)-1
					lfoSinAmp=0, lfoPlsAmp=0, lfoSawAmp=0, lfoNseAmp=0,  // these mix to create lfo shape
					det1=1, det2=1, det3=1, det4=1,
					fxFrq1=0, fxFrq2=0, fxFrq3=0, fxFrq4=0,  // fixed freq alternative
					ffrq=20000;

					var sus = sustain*(tempo.reciprocal);
					var carFreq,mod1Freq,mod2Freq,mod3Freq;
					var lfoAmp,lfoFrq,amodEnv,sig,w,x,y,z;
					// level of carrier env set to 0.5 max to allow summing with ampmod to peak at 1
					var idx1 = EnvGen.kr(Env.new([0.001,p1_1,p2_1,p3_1,0.0001],
						[t1_1,t2_1,t3_1,rls_1],\exp),
						levelScale: amp1.max(0.0001),levelBias: shft1,timeScale:sus,   doneAction:2); // op1 env ends synth
					var idx2 = EnvGen.kr(Env.new([0.001,p1_2,p2_2,p3_2,0.001],
						[t1_2,t2_2,t3_2,rls_2],\exp),
						levelScale: amp2.max(0.0001),levelBias: shft2,timeScale:sus, );
					var idx3 = EnvGen.kr(Env.new([0.001,p1_3,p2_3,p3_3,0.001],
						[t1_3,t2_3,t3_3,rls_3],\exp),
						levelScale: amp3.max(0.0001),levelBias: shft3,timeScale:sus,);
					var idx4 = EnvGen.kr(Env.new([0.001,p1_4,p2_4,p3_4,0.001],
						[t1_4,t2_4,t3_4,rls_4],\exp),
						levelScale: amp4.max(0.0001),levelBias: shft4,timeScale:sus,);

					lfoAmp = ((SinOsc.kr(lfoSpd).range(0,1)*lfoSinAmp) +
						(LFPulse.kr(lfoSpd,0,0.5).range(0,1)*lfoPlsAmp) + (LFSaw.kr(lfoSpd).range(0,1)*lfoSawAmp) +
						(LFNoise1.kr(lfoSpd).range(0,1)*lfoNseAmp))*0.5;
					lfoFrq = (SinOsc.kr(lfoSpd)*lfoSinAmp) + (LFPulse.kr(lfoSpd,0).range(-1,1)*lfoPlsAmp) +
					(LFSaw.kr(lfoSpd)*lfoSawAmp) + (LFNoise1.kr(lfoSpd)*lfoNseAmp);
					amodEnv=EnvGen.kr(Env.linen(0.01,1,0.01));
					idx1= ((lfoAmp*amod1*amodEnv)+idx1);  //  lfo modulation and idx sum is max 1.0
					idx2= ((lfoAmp*amod2*amodEnv)+idx2)*indexMax;
					idx3= ((lfoAmp*amod3*amodEnv)+idx3)*indexMax;
					idx4= ((lfoAmp*amod4*amodEnv)+idx4)*indexMax;

					// if fxFrq>0 then freq = fxFrq else freq = freq*rtio
					carFreq = ((fxFrq1>0)*fxFrq1) + ((fxFrq1<=0)*(rtio1*freq*det1));
					mod1Freq = ((fxFrq2>0)*fxFrq2) + ((fxFrq2<=0)*(rtio2*freq*det2));
					mod2Freq = ((fxFrq3>0)*fxFrq3) + ((fxFrq3<=0)*(rtio3*freq*det3));
					mod3Freq = ((fxFrq4>0)*fxFrq4) + ((fxFrq4<=0)*(rtio4*freq*det4));

					sig = Osc.ar(wv1,
						Osc.ar(wv2,
							SinOscFB.ar(
								mod3Freq,fb*pi,
								mod3Freq*idx4)			// mul
							+ Osc.ar(wv3,
								mod2Freq,0,
								mod2Freq*idx3,   // mul
								mod1Freq),  // add
							0,mod1Freq*idx2, 	// mul
							carFreq   // add
						) + (lfoFrq*pmod*vibRange*carFreq),  // carfreq = lfo*pmod + modulator fm
						0,idx1
					);
					sig = LPF.ar(sig,ffrq);
					Out.ar(effOut,sig*amp*effAmp);
					#w,x,y,z = BFEncode1.ar(sig*amp*vol,pan*pi,elev);
					Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
				}).load(server),

				//    4->1
				// 3->2->1
				SynthDef(\txAlg3,{ arg freq=440, amp=1, sustain=1,tempo=1,
					pan=0, out=0, elev=0, effAmp=0, effOut=16, vol=0.25,
					wv1=0, wv2=0, wv3=0, fb=0,
					rtio1=1, rtio2=1, rtio3=1, rtio4=1,
					amp1=1, amp2=1, amp3=1, amp4=1,
					p1_1=1, p2_1=0.1, p3_1=0.1, t1_1=0.01, t2_1=0.99, t3_1=0, rls_1=0.1, shft1=0,
					p1_2=1, p2_2=0.1, p3_2=0.1, t1_2=0.01, t2_2=0.99, t3_2=0, rls_2=0.1, shft2=0,
					p1_3=1, p2_3=0.1, p3_3=0.1, t1_3=0.01, t2_3=0.99, t3_3=0, rls_3=0.1, shft3=0,
					p1_4=1, p2_4=0.1, p3_4=0.1, t1_4=0.01, t2_4=0.99, t3_4=0, rls_4=0.1, shft4=0,
					amod1=0, amod2=0, amod3=0, amod4=0,indexMax=6,
					lfoSpd=0.2, pmod=0, vibRange=0.125,//  default vibrato range = (9/8)-1
					lfoSinAmp=0, lfoPlsAmp=0, lfoSawAmp=0, lfoNseAmp=0,  // these mix to create lfo shape
					det1=1, det2=1, det3=1, det4=1,
					fxFrq1=0, fxFrq2=0, fxFrq3=0, fxFrq4=0,  // fixed freq alternative
					ffrq=20000;  // fixed freq alternative

					var sus = sustain*(tempo.reciprocal);
					var carFreq,mod1Freq,mod2Freq,mod3Freq;
					var lfoAmp,lfoFrq,amodEnv,sig,w,x,y,z;

					// level of carrier env set to 0.5 max to allow summing with ampmod to peak at 1
					var idx1 = EnvGen.kr(Env.new([0.001,p1_1,p2_1,p3_1,0.001],
						[t1_1,t2_1,t3_1,rls_1],\exp),
						levelScale: amp1.max(0.0001),levelBias: shft1,timeScale:sus,   doneAction:2); // op1 env ends synth
					var idx2 = EnvGen.kr(Env.new([0.001,p1_2,p2_2,p3_2,0.001],
						[t1_2,t2_2,t3_2,rls_2],\exp),
						levelScale: amp2.max(0.0001),levelBias: shft2,timeScale:sus, );
					var idx3 = EnvGen.kr(Env.new([0.001,p1_3,p2_3,p3_3,0.001],
						[t1_3,t2_3,t3_3,rls_3],\exp),
						levelScale: amp3.max(0.0001),levelBias: shft3,timeScale:sus,);
					var idx4 = EnvGen.kr(Env.new([0.001,p1_4,p2_4,p3_4,0.001],
						[t1_4,t2_4,t3_4,rls_4],\exp),
						levelScale: amp4.max(0.0001),levelBias: shft4,timeScale:sus,);

					lfoAmp = ((SinOsc.kr(lfoSpd).range(0,1)*lfoSinAmp) +
						(LFPulse.kr(lfoSpd,0,0.5).range(0,1)*lfoPlsAmp) + (LFSaw.kr(lfoSpd).range(0,1)*lfoSawAmp) +
						(LFNoise1.kr(lfoSpd).range(0,1)*lfoNseAmp))*0.5;
					lfoFrq = (SinOsc.kr(lfoSpd)*lfoSinAmp) + (LFPulse.kr(lfoSpd,0).range(-1,1)*lfoPlsAmp) +
					(LFSaw.kr(lfoSpd)*lfoSawAmp) + (LFNoise1.kr(lfoSpd)*lfoNseAmp);

					amodEnv=EnvGen.kr(Env.linen(0.01,1,0.01));
					idx1= ((lfoAmp*amod1*amodEnv)+idx1);  //  lfo modulation and idx sum is max 1.0
					idx2= ((lfoAmp*amod2*amodEnv)+idx2)*indexMax;
					idx3= ((lfoAmp*amod3*amodEnv)+idx3)*indexMax;
					idx4= ((lfoAmp*amod4*amodEnv)+idx4)*indexMax;

					// if fxFrq>0 then freq = fxFrq else freq = freq*rtio
					carFreq = ((fxFrq1>0)*fxFrq1) + ((fxFrq1<=0)*(rtio1*freq*det1));
					mod1Freq = ((fxFrq2>0)*fxFrq2) + ((fxFrq2<=0)*(rtio2*freq*det2));
					mod2Freq = ((fxFrq3>0)*fxFrq3) + ((fxFrq3<=0)*(rtio3*freq*det3));
					mod3Freq = ((fxFrq4>0)*fxFrq4) + ((fxFrq4<=0)*(rtio4*freq*det4));

					sig = Osc.ar(wv1,
						(SinOscFB.ar(
							mod3Freq,fb*pi,
							mod3Freq*idx4,		// mul
							carFreq)
						+ Osc.ar(wv2,
							Osc.ar(wv3,
								mod2Freq,
								0,mod2Freq*idx3,   // mul
								mod1Freq),  // add
							0,mod1Freq*idx2
						)) + (lfoFrq*pmod*vibRange*carFreq),  // carfreq = lfo*pmod + modulator fm
						0,idx1
					);
					sig = LPF.ar(sig,ffrq);
					Out.ar(effOut,sig*amp*effAmp);
					#w,x,y,z = BFEncode1.ar(sig*amp*vol,pan*pi,elev);
					Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
				}).load(server),

				// 4->3->1
				//    2->1
				SynthDef(\txAlg4,{ arg freq=440, amp=1, sustain=1,tempo=1,
					pan=0, out=0, elev=0, effAmp=0, effOut=16, vol=0.25,
					wv1=0, wv2=0, wv3=0, fb=0,
					rtio1=1, rtio2=1, rtio3=1, rtio4=1,
					amp1=1, amp2=1, amp3=1, amp4=1,
					p1_1=1, p2_1=0.1, p3_1=0.1, t1_1=0.01, t2_1=0.99, t3_1=0, rls_1=0.1, shft1=0,
					p1_2=1, p2_2=0.1, p3_2=0.1, t1_2=0.01, t2_2=0.99, t3_2=0, rls_2=0.1, shft2=0,
					p1_3=1, p2_3=0.1, p3_3=0.1, t1_3=0.01, t2_3=0.99, t3_3=0, rls_3=0.1, shft3=0,
					p1_4=1, p2_4=0.1, p3_4=0.1, t1_4=0.01, t2_4=0.99, t3_4=0, rls_4=0.1, shft4=0,
					amod1=0, amod2=0, amod3=0, amod4=0,indexMax=6,
					lfoSpd=0.2, pmod=0, vibRange=0.125,//  default vibrato range = (9/8)-1
					lfoSinAmp=0, lfoPlsAmp=0, lfoSawAmp=0, lfoNseAmp=0,  // these mix to create lfo shape
					det1=1, det2=1, det3=1, det4=1,
					fxFrq1=0, fxFrq2=0, fxFrq3=0, fxFrq4=0,  // fixed freq alternative
					ffrq=20000;

					var sus = sustain*(tempo.reciprocal);
					var carFreq,mod1Freq,mod2Freq,mod3Freq;
					var lfoAmp,lfoFrq,amodEnv,sig,w,x,y,z;
					// level of carrier env set to 0.5 max to allow summing with ampmod to peak at 1
					var idx1 = EnvGen.kr(Env.new([0.001,p1_1,p2_1,p3_1,0.0001],
						[t1_1,t2_1,t3_1,rls_1],\exp),
						levelScale: amp1.max(0.0001),levelBias: shft1,timeScale:sus,   doneAction:2); // op1 env ends synth
					var idx2 = EnvGen.kr(Env.new([0.001,p1_2,p2_2,p3_2,0.001],
						[t1_2,t2_2,t3_2,rls_2],\exp),
						levelScale: amp2.max(0.0001),levelBias: shft2,timeScale:sus, );
					var idx3 = EnvGen.kr(Env.new([0.001,p1_3,p2_3,p3_3,0.001],
						[t1_3,t2_3,t3_3,rls_3],\exp),
						levelScale: amp3.max(0.0001),levelBias: shft3,timeScale:sus,);
					var idx4 = EnvGen.kr(Env.new([0.001,p1_4,p2_4,p3_4,0.001],
						[t1_4,t2_4,t3_4,rls_4],\exp),
						levelScale: amp4.max(0.0001),levelBias: shft4,timeScale:sus,);

					lfoAmp = ((SinOsc.kr(lfoSpd).range(0,1)*lfoSinAmp) +
						(LFPulse.kr(lfoSpd,0,0.5).range(0,1)*lfoPlsAmp) + (LFSaw.kr(lfoSpd).range(0,1)*lfoSawAmp) +
						(LFNoise1.kr(lfoSpd).range(0,1)*lfoNseAmp))*0.5;
					lfoFrq = (SinOsc.kr(lfoSpd)*lfoSinAmp) + (LFPulse.kr(lfoSpd,0).range(-1,1)*lfoPlsAmp) +
					(LFSaw.kr(lfoSpd)*lfoSawAmp) + (LFNoise1.kr(lfoSpd)*lfoNseAmp);

					amodEnv=EnvGen.kr(Env.linen(0.01,1,0.01));
					idx1= ((lfoAmp*amod1*amodEnv)+idx1);  //  lfo modulation and idx sum is max 1.0
					idx2= ((lfoAmp*amod2*amodEnv)+idx2)*indexMax;
					idx3= ((lfoAmp*amod3*amodEnv)+idx3)*indexMax;
					idx4= ((lfoAmp*amod4*amodEnv)+idx4)*indexMax;

					// if fxFrq>0 then freq = fxFrq else freq = freq*rtio
					carFreq = ((fxFrq1>0)*fxFrq1) + ((fxFrq1<=0)*(rtio1*freq*det1));
					mod1Freq = ((fxFrq2>0)*fxFrq2) + ((fxFrq2<=0)*(rtio2*freq*det2));
					mod2Freq = ((fxFrq3>0)*fxFrq3) + ((fxFrq3<=0)*(rtio3*freq*det3));
					mod3Freq = ((fxFrq4>0)*fxFrq4) + ((fxFrq4<=0)*(rtio4*freq*det4));

					sig = Osc.ar(wv1,
						SinOscFB.ar(
							mod3Freq,fb*pi,
							mod3Freq*idx4,		// mul
							carFreq			// add
						) +
						Osc.ar(wv2,
							Osc.ar(wv3,
								mod2Freq,0,
								mod2Freq*idx3,   // mul
								mod1Freq  // add
							),0,
							mod1Freq*idx2 	// mul
						) + (lfoFrq*pmod*vibRange*carFreq),  // carfreq = lfo*pmod + cascade fm
						0,idx1
					);
					sig = LPF.ar(sig,ffrq);
					Out.ar(effOut,sig*amp*effAmp);
					#w,x,y,z = BFEncode1.ar(sig*amp*vol,pan*pi,elev);
					Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
				}).load(server),


				// 4->3
				// 2->1
				SynthDef(\txAlg5,{ arg freq=440, amp=1, sustain=1,tempo=1,
					pan=0, out=0, elev=0, effAmp=0, effOut=16, vol=0.25,
					wv1=0, wv2=0, wv3=0, fb=0,
					rtio1=1, rtio2=1, rtio3=1, rtio4=1,
					amp1=1, amp2=1, amp3=1, amp4=1,
					p1_1=1, p2_1=0.1, p3_1=0.1, t1_1=0.01, t2_1=0.99, t3_1=0, rls_1=0.1, shft1=0,
					p1_2=1, p2_2=0.1, p3_2=0.1, t1_2=0.01, t2_2=0.99, t3_2=0, rls_2=0.1, shft2=0,
					p1_3=1, p2_3=0.1, p3_3=0.1, t1_3=0.01, t2_3=0.99, t3_3=0, rls_3=0.1, shft3=0,
					p1_4=1, p2_4=0.1, p3_4=0.1, t1_4=0.01, t2_4=0.99, t3_4=0, rls_4=0.1, shft4=0,
					amod1=0, amod2=0, amod3=0, amod4=0,indexMax=6,
					lfoSpd=0.2, pmod=0, vibRange=0.125,//  default vibrato range = (9/8)-1
					lfoSinAmp=0, lfoPlsAmp=0, lfoSawAmp=0, lfoNseAmp=0,  // these mix to create lfo shape
					det1=1, det2=1, det3=1, det4=1,
					fxFrq1=0, fxFrq2=0, fxFrq3=0, fxFrq4=0,  // fixed freq alternative
					ffrq=20000;

					var sus = sustain*(tempo.reciprocal);
					var car1Freq,car3Freq,mod1Freq,mod3Freq;
					var lfoAmp,lfoFrq,amodEnv,sig,w,x,y,z;
				// level of carrier env set to 0.5 max to allow summing with ampmod to peak at 1
					var idx1 = EnvGen.kr(Env.new([0.001,p1_1,p2_1,p3_1,0.001],
						[t1_1,t2_1,t3_1,rls_1],\exp),
						levelScale: amp1.max(0.0001),levelBias: shft1,timeScale:sus,   doneAction:2); // op1 env ends synth
					var idx2 = EnvGen.kr(Env.new([0.001,p1_2,p2_2,p3_2,0.001],
						[t1_2,t2_2,t3_2,rls_2],\exp),
						levelScale: amp2.max(0.0001),levelBias: shft2,timeScale:sus, );
					var idx3 = EnvGen.kr(Env.new([0.001,p1_3,p2_3,p3_3,0.001],
						[t1_3,t2_3,t3_3,rls_3],\exp),
					levelScale: amp3.max(0.0001),levelBias: shft3);
					var idx4 = EnvGen.kr(Env.new([0.001,p1_4,p2_4,p3_4,0.001],
						[t1_4,t2_4,t3_4,rls_4],\exp),
						levelScale: amp4.max(0.0001),levelBias: shft4);

					lfoAmp = ((SinOsc.kr(lfoSpd).range(0,1)*lfoSinAmp) +
						(LFPulse.kr(lfoSpd,0,0.5).range(0,1)*lfoPlsAmp) + (LFSaw.kr(lfoSpd).range(0,1)*lfoSawAmp) +
						(LFNoise1.kr(lfoSpd).range(0,1)*lfoNseAmp))*0.5;
					lfoFrq = (SinOsc.kr(lfoSpd)*lfoSinAmp) + (LFPulse.kr(lfoSpd,0).range(-1,1)*lfoPlsAmp) +
					(LFSaw.kr(lfoSpd)*lfoSawAmp) + (LFNoise1.kr(lfoSpd)*lfoNseAmp);

					amodEnv=EnvGen.kr(Env.linen(0.01,1,0.01));
					idx1= ((lfoAmp*amod1*amodEnv)+idx1);  //  lfo modulation and idx sum is max 1.0
					idx2= ((lfoAmp*amod2*amodEnv)+idx2)*indexMax;
					idx3= ((lfoAmp*amod3*amodEnv)+idx3);
					idx4= ((lfoAmp*amod4*amodEnv)+idx4)*indexMax;

					// if fxFrq>0 then freq = fxFrq else freq = freq*rtio
					car1Freq = ((fxFrq1>0)*fxFrq1) + ((fxFrq1<=0)*(rtio1*freq*det1));
					mod1Freq = ((fxFrq2>0)*fxFrq2) + ((fxFrq2<=0)*(rtio2*freq*det2));
					car3Freq = ((fxFrq3>0)*fxFrq3) + ((fxFrq3<=0)*(rtio3*freq*det3));
					mod3Freq = ((fxFrq4>0)*fxFrq4) + ((fxFrq4<=0)*(rtio4*freq*det4));

					sig = Osc.ar(wv1,
						Osc.ar(wv2,
							mod1Freq,
							0,mod1Freq*idx2, 	// mul
							car1Freq   // add
						) + (lfoFrq*pmod*vibRange*car1Freq),  // carfreq = lfo*pmod +  mod freq
						0,idx1
					) +
					Osc.ar(wv3,
						SinOscFB.ar(
							mod3Freq,fb*pi,
							mod3Freq*idx4,		// mul
							car3Freq			// add
						) + (lfoFrq*pmod*vibRange*car3Freq),   // carfreq = lfo*pmod +  mod freq,
						0,idx3
					);
					sig = LPF.ar(sig,ffrq);
					Out.ar(effOut,sig*amp*effAmp);
					#w,x,y,z = BFEncode1.ar(sig*amp*vol,pan*pi,elev);
					Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
				}).load(server),

				//    4->1
				//    4->2
				//    4->3
				SynthDef(\txAlg6,{ arg freq=440, amp=1, sustain=1,tempo=1,
					pan=0, out=0, elev=0, effAmp=0, effOut=16, vol=0.25,
					wv1=0, wv2=0, wv3=0, fb=0,
					rtio1=1, rtio2=1, rtio3=1, rtio4=1,
					amp1=1, amp2=1, amp3=1, amp4=1,
					p1_1=1, p2_1=0.1, p3_1=0.1, t1_1=0.01, t2_1=0.99, t3_1=0, rls_1=0.1, shft1=0,
					p1_2=1, p2_2=0.1, p3_2=0.1, t1_2=0.01, t2_2=0.99, t3_2=0, rls_2=0.1, shft2=0,
					p1_3=1, p2_3=0.1, p3_3=0.1, t1_3=0.01, t2_3=0.99, t3_3=0, rls_3=0.1, shft3=0,
					p1_4=1, p2_4=0.1, p3_4=0.1, t1_4=0.01, t2_4=0.99, t3_4=0, rls_4=0.1, shft4=0,
					amod1=0, amod2=0, amod3=0, amod4=0,indexMax=6,
					lfoSpd=0.2, pmod=0, vibRange=0.125,//  default vibrato range = (9/8)-1
					lfoSinAmp=0, lfoPlsAmp=0, lfoSawAmp=0, lfoNseAmp=0,  // these mix to create lfo shape
					det1=1, det2=1, det3=1, det4=1,
					fxFrq1=0, fxFrq2=0, fxFrq3=0, fxFrq4=0,  // fixed freq alternative
					ffrq=20000;

					var sus = sustain*(tempo.reciprocal);
					var car1Freq,car2Freq,car3Freq,modFreq,mod4;
					var lfoAmp,lfoFrq,amodEnv,sig,w,x,y,z;
					// level of carrier env set to 0.5 max to allow summing with ampmod to peak at 1
					var idx1 = EnvGen.kr(Env.new([0.001,p1_1,p2_1,p3_1,0.0001],
						[t1_1,t2_1,t3_1,rls_1],\exp),
						levelScale: amp1.max(0.0001),levelBias: shft1,timeScale:sus,   doneAction:2); // op1 env ends synth
					var idx2 = EnvGen.kr(Env.new([0.001,p1_2,p2_2,p3_2,0.001],
						[t1_2,t2_2,t3_2,rls_2],\exp),
						levelScale: amp2.max(0.0001),levelBias: shft2,timeScale:sus, );
					var idx3 = EnvGen.kr(Env.new([0.001,p1_3,p2_3,p3_3,0.001],
						[t1_3,t2_3,t3_3,rls_3],\exp),
						levelScale: amp3.max(0.0001),levelBias: shft3,timeScale:sus,);
					var idx4 = EnvGen.kr(Env.new([0.001,p1_4,p2_4,p3_4,0.001],
						[t1_4,t2_4,t3_4,rls_4],\exp),
						levelScale: amp4.max(0.0001),levelBias: shft4,timeScale:sus,);

					lfoAmp = ((SinOsc.kr(lfoSpd).range(0,1)*lfoSinAmp) +
						(LFPulse.kr(lfoSpd,0,0.5).range(0,1)*lfoPlsAmp) + (LFSaw.kr(lfoSpd).range(0,1)*lfoSawAmp) +
						(LFNoise1.kr(lfoSpd).range(0,1)*lfoNseAmp))*0.5;
					lfoFrq = (SinOsc.kr(lfoSpd)*lfoSinAmp) + (LFPulse.kr(lfoSpd,0).range(-1,1)*lfoPlsAmp) +
					(LFSaw.kr(lfoSpd)*lfoSawAmp) + (LFNoise1.kr(lfoSpd)*lfoNseAmp);

					amodEnv=EnvGen.kr(Env.linen(0.01,1,0.01));
					idx1= ((lfoAmp*amod1*amodEnv)+idx1);  //  lfo modulation and idx sum is max 1.0
					idx2= ((lfoAmp*amod2*amodEnv)+idx2);
					idx3= ((lfoAmp*amod3*amodEnv)+idx3);
					idx4= ((lfoAmp*amod4*amodEnv)+idx4)*indexMax;

					// if fxFrq>0 then freq = fxFrq else freq = freq*rtio
					car1Freq = ((fxFrq1>0)*fxFrq1) + ((fxFrq1<=0)*(rtio1*freq*det1));
					car2Freq = ((fxFrq2>0)*fxFrq2) + ((fxFrq2<=0)*(rtio2*freq*det2));
					car3Freq = ((fxFrq3>0)*fxFrq3) + ((fxFrq3<=0)*(rtio3*freq*det3));
					modFreq = ((fxFrq4>0)*fxFrq4) + ((fxFrq4<=0)*(rtio4*freq*det4));

					mod4 = SinOscFB.ar(modFreq,fb*pi,idx4);

					sig = Osc.ar(wv1,
						(car1Freq+mod4)+(lfoFrq*pmod*vibRange*car1Freq),0,idx1) +
					Osc.ar(wv2,0,
						(car2Freq+mod4)+(lfoFrq*pmod*vibRange*car2Freq),0,idx2) +
					Osc.ar(wv3,0,
						(car3Freq+mod4)+(lfoFrq*pmod*vibRange*car3Freq),0,idx3);
					sig = LPF.ar(sig,ffrq);

					Out.ar(effOut,sig*amp*effAmp);
					#w,x,y,z = BFEncode1.ar(sig*amp*vol,pan*pi,elev);
					Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
				}).load(server),

				// 4->3
				//    2
				//    1
				SynthDef(\txAlg7,{ arg freq=440, amp=1, sustain=1,tempo=1,
					pan=0, out=0, elev=0, effAmp=0, effOut=16, vol=0.25,
					wv1=0, wv2=0, wv3=0, fb=0,
					rtio1=1, rtio2=1, rtio3=1, rtio4=1,
					amp1=1, amp2=1, amp3=1, amp4=1,
					p1_1=1, p2_1=0.1, p3_1=0.1, t1_1=0.01, t2_1=0.99, t3_1=0, rls_1=0.1, shft1=0,
					p1_2=1, p2_2=0.1, p3_2=0.1, t1_2=0.01, t2_2=0.99, t3_2=0, rls_2=0.1, shft2=0,
					p1_3=1, p2_3=0.1, p3_3=0.1, t1_3=0.01, t2_3=0.99, t3_3=0, rls_3=0.1, shft3=0,
					p1_4=1, p2_4=0.1, p3_4=0.1, t1_4=0.01, t2_4=0.99, t3_4=0, rls_4=0.1, shft4=0,
					amod1=0, amod2=0, amod3=0, amod4=0,indexMax=6,
					lfoSpd=0.2, pmod=0, vibRange=0.125,//  default vibrato range = (9/8)-1
					lfoSinAmp=0, lfoPlsAmp=0, lfoSawAmp=0, lfoNseAmp=0,  // these mix to create lfo shape
					det1=1, det2=1, det3=1, det4=1,
					fxFrq1=0, fxFrq2=0, fxFrq3=0, fxFrq4=0,  // fixed freq alternative
					ffrq=20000;

					var sus = sustain*(tempo.reciprocal);
					var car1Freq,car2Freq,car3Freq,modFreq;
					var lfoAmp,lfoFrq,amodEnv,sig,w,x,y,z;
					// level of carrier env set to 0.5 max to allow summing with ampmod to peak at 1
					var idx1 = EnvGen.kr(Env.new([0.001,p1_1,p2_1,p3_1,0.0001],
						[t1_1,t2_1,t3_1,rls_1],\exp),
						levelScale: amp1.max(0.0001),levelBias: shft1,timeScale:sus,   doneAction:2); // op1 env ends synth
					var idx2 = EnvGen.kr(Env.new([0.001,p1_2,p2_2,p3_2,0.001],
						[t1_2,t2_2,t3_2,rls_2],\exp),
						levelScale: amp2.max(0.0001),levelBias: shft2,timeScale:sus, );
					var idx3 = EnvGen.kr(Env.new([0.001,p1_3,p2_3,p3_3,0.001],
						[t1_3,t2_3,t3_3,rls_3],\exp),
						levelScale: amp3.max(0.0001),levelBias: shft3,timeScale:sus,);
					var idx4 = EnvGen.kr(Env.new([0.001,p1_4,p2_4,p3_4,0.001],
						[t1_4,t2_4,t3_4,rls_4],\exp),
						levelScale: amp4.max(0.0001),levelBias: shft4,timeScale:sus,);

					lfoAmp = ((SinOsc.kr(lfoSpd).range(0,1)*lfoSinAmp) +
						(LFPulse.kr(lfoSpd,0,0.5).range(0,1)*lfoPlsAmp) + (LFSaw.kr(lfoSpd).range(0,1)*lfoSawAmp) +
						(LFNoise1.kr(lfoSpd).range(0,1)*lfoNseAmp))*0.5;
					lfoFrq = (SinOsc.kr(lfoSpd)*lfoSinAmp) + (LFPulse.kr(lfoSpd,0).range(-1,1)*lfoPlsAmp) +
					(LFSaw.kr(lfoSpd)*lfoSawAmp) + (LFNoise1.kr(lfoSpd)*lfoNseAmp);

					amodEnv=EnvGen.kr(Env.linen(0.01,1,0.01));
					idx1= ((lfoAmp*amod1*amodEnv)+idx1);  //  lfo modulation and idx sum is max 1.0
					idx2= ((lfoAmp*amod2*amodEnv)+idx2);
					idx3= ((lfoAmp*amod3*amodEnv)+idx3);
					idx4= ((lfoAmp*amod4*amodEnv)+idx4)*indexMax;

					// if fxFrq>0 then freq = fxFrq else freq = freq*rtio
					car1Freq = ((fxFrq1>0)*fxFrq1) + ((fxFrq1<=0)*(rtio1*freq*det1));
					car2Freq = ((fxFrq2>0)*fxFrq2) + ((fxFrq2<=0)*(rtio2*freq*det2));
					car3Freq = ((fxFrq3>0)*fxFrq3) + ((fxFrq3<=0)*(rtio3*freq*det3));
					modFreq = ((fxFrq4>0)*fxFrq4) + ((fxFrq4<=0)*(rtio4*freq*det4));

					sig = Osc.ar(wv1,
						car1Freq+(lfoFrq*pmod*vibRange*car1Freq),0,idx1) +
					Osc.ar(wv2,car2Freq+(lfoFrq*pmod*vibRange*car2Freq),0,idx2) +
					Osc.ar(wv3,
						SinOscFB.ar(
							modFreq,fb*pi,
							modFreq*idx4,		// mul
							car3Freq			// add
						) + (lfoFrq*pmod*vibRange*car3Freq),   // carfreq = lfo*pmod +  mod freq,
						0,idx3
					);
					sig = LPF.ar(sig,ffrq);
					Out.ar(effOut,sig*amp*effAmp);
					#w,x,y,z = BFEncode1.ar(sig*amp*vol,pan*pi,elev);
					Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
				}).load(server),

				// 1-2-3-4
				SynthDef(\txAlg8,{ arg freq=440, amp=1, sustain=1,tempo=1,
					pan=0, out=0, elev=0, effAmp=0, effOut=16, vol=0.25,
					wv1=0, wv2=0, wv3=0, fb=0,
					rtio1=1, rtio2=1, rtio3=1, rtio4=1,
					amp1=1, amp2=1, amp3=1, amp4=1,
					p1_1=1, p2_1=0.1, p3_1=0.1, t1_1=0.01, t2_1=0.99, t3_1=0, rls_1=0.1, shft1=0,
					p1_2=1, p2_2=0.1, p3_2=0.1, t1_2=0.01, t2_2=0.99, t3_2=0, rls_2=0.1, shft2=0,
					p1_3=1, p2_3=0.1, p3_3=0.1, t1_3=0.01, t2_3=0.99, t3_3=0, rls_3=0.1, shft3=0,
					p1_4=1, p2_4=0.1, p3_4=0.1, t1_4=0.01, t2_4=0.99, t3_4=0, rls_4=0.1, shft4=0,
					amod1=0, amod2=0, amod3=0, amod4=0,indexMax=6,
					lfoSpd=0.2, pmod=0, vibRange=0.125,//  default vibrato range = (9/8)-1
					lfoSinAmp=0, lfoPlsAmp=0, lfoSawAmp=0, lfoNseAmp=0,  // these mix to create lfo shape
					det1=1, det2=1, det3=1, det4=1,
					fxFrq1=0, fxFrq2=0, fxFrq3=0, fxFrq4=0,  // fixed freq alternative
					ffrq=20000;

					var sus = sustain*(tempo.reciprocal);
					var car1Freq,car2Freq,car3Freq,car4Freq;
					var lfoAmp,lfoFrq,amodEnv,sig,w,x,y,z;
					// level of carrier env set to 0.5 max to allow summing with ampmod to peak at 1
					var idx1 = EnvGen.kr(Env.new([0.001,p1_1,p2_1,p3_1,0.0001],
						[t1_1,t2_1,t3_1,rls_1],\exp),
						levelScale: amp1.max(0.0001),levelBias: shft1,timeScale:sus,   doneAction:2); // op1 env ends synth
					var idx2 = EnvGen.kr(Env.new([0.001,p1_2,p2_2,p3_2,0.001],
						[t1_2,t2_2,t3_2,rls_2],\exp),
						levelScale: amp2.max(0.0001),levelBias: shft2,timeScale:sus, );
					var idx3 = EnvGen.kr(Env.new([0.001,p1_3,p2_3,p3_3,0.001],
						[t1_3,t2_3,t3_3,rls_3],\exp),
						levelScale: amp3.max(0.0001),levelBias: shft3,timeScale:sus,);
					var idx4 = EnvGen.kr(Env.new([0.001,p1_4,p2_4,p3_4,0.001],
						[t1_4,t2_4,t3_4,rls_4],\exp),
						levelScale: amp4.max(0.0001),levelBias: shft4,timeScale:sus,);

					lfoAmp = ((SinOsc.kr(lfoSpd).range(0,1)*lfoSinAmp) +
						(LFPulse.kr(lfoSpd,0,0.5).range(0,1)*lfoPlsAmp) + (LFSaw.kr(lfoSpd).range(0,1)*lfoSawAmp) +
						(LFNoise1.kr(lfoSpd).range(0,1)*lfoNseAmp))*0.5;
					lfoFrq = (SinOsc.kr(lfoSpd)*lfoSinAmp) + (LFPulse.kr(lfoSpd,0).range(-1,1)*lfoPlsAmp) +
					(LFSaw.kr(lfoSpd)*lfoSawAmp) + (LFNoise1.kr(lfoSpd)*lfoNseAmp);

					amodEnv=EnvGen.kr(Env.linen(0.01,1,0.01));
					idx1= ((lfoAmp*amod1*amodEnv)+idx1);  //  lfo modulation and idx sum is max 1.0
					idx2= ((lfoAmp*amod2*amodEnv)+idx2);
					idx3= ((lfoAmp*amod3*amodEnv)+idx3);
					idx4= ((lfoAmp*amod4*amodEnv)+idx4);

					// if fxFrq>0 then freq = fxFrq else freq = freq*rtio
					car1Freq = ((fxFrq1>0)*fxFrq1) + ((fxFrq1<=0)*(rtio1*freq*det1));
					car2Freq = ((fxFrq2>0)*fxFrq2) + ((fxFrq2<=0)*(rtio2*freq*det2));
					car3Freq = ((fxFrq3>0)*fxFrq3) + ((fxFrq3<=0)*(rtio3*freq*det3));
					car4Freq = ((fxFrq4>0)*fxFrq4) + ((fxFrq4<=0)*(rtio4*freq*det4));

					sig = Osc.ar(wv1,
						car1Freq+(lfoFrq*pmod*vibRange*car1Freq),0,idx1) +
					Osc.ar(wv2,car2Freq+(lfoFrq*pmod*vibRange*car2Freq),0,idx2) +
					Osc.ar(wv3,car3Freq+(lfoFrq*pmod*vibRange*car3Freq),0,idx3) +
					SinOscFB.ar(car4Freq+(lfoFrq*pmod*vibRange*car4Freq),fb*pi,idx4);
					sig = LPF.ar(sig,ffrq);
					Out.ar(effOut,sig*amp*effAmp);
					#w,x,y,z = BFEncode1.ar(sig*amp*vol,pan*pi,elev);
					Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray))
				}).load(server)
			];
			"... TxSynthDefs loaded  ... ".postln; synthDefs.do {|sd| sd.name.post; " ".post }
		});

	}



}

/*

// fm model
SynthDef(\cascadeFM,{  arg carFreq=200, m1fixFrq=0, m2fixFrq=0,carAmp=0.5, cmRatio=1.0, index1=1, mmRatio=1, index2=1;
	var modFreq1 = (carFreq*cmRatio)+m1fixFrq;
	var modFreq2 = (modFreq1*mmRatio)+m2fixFrq;
	var sig = SinOsc.ar(
		SinOsc.ar(
			SinOsc.ar(modFreq2,0,
				modFreq2*index2,   // mul
				modFreq1),   // add
			0,
			modFreq1*index1,   // mul
			carFreq    // add
		),
		0,carAmp);
	Out.ar(out,sig)
}).load(s);
)
f = Synth(\cascadeFM);
f.set(\carFreq,600)
(
// tx fm model
SynthDef(\txFM,{  arg freq=200, fmRatio1=1.0, fmRatio2=4, fmRatio3=5, carAmp=0.1, index1=1, index2=1;
	var carFreq= freq*fmRatio1;
	var modFreq1 = (freq*fmRatio2);
	var modFreq2 = (freq*fmRatio3);
	var sig = SinOsc.ar(
		SinOsc.ar(
			SinOsc.ar(modFreq2,0,
				modFreq2*index2,   // mul
				modFreq1),   // add
			0,
			modFreq1*index1,   // mul
			carFreq    // add
		),
		0,carAmp);
	Out.ar(out,sig)
}).load(s);
)
f = Synth(\txFM);
f.set(\freq,60)
TXSusSynthdefs
*/