
TxVc {
	classvar <>voices;
	/*  TX81ZSynthDef default args:

	freq=440,amp=1, dur=1, pan=0, elev=0, vol=0.25,
	wv1=0, wv2=0, wv3=0, fb=0,
	rtio1=1, rtio2=1, rtio3=1, rtio4=1,
	amp1=1, amp2=1, amp3=1, amp4=1,
	p1_1=1, p2_1=0.1, p3_1=0.1, t1_1=0.01, t2_1=0.99, t3_1=0, rls_1=0.1, shft1=0,
	p1_2=1, p2_2=0.1, p3_2=0.1, t1_2=0.01, t2_2=0.99, t3_2=0, rls_2=0.1, shft2=0,
	p1_3=1, p2_3=0.1, p3_3=0.1, t1_3=0.01, t2_3=0.99, t3_3=0, rls_3=0.1, shft3=0,
	p1_4=1, p2_4=0.1, p3_4=0.1, t1_4=0.01, t2_4=0.99, t3_4=0, rls_4=0.1, shft4=0,
	lfoSpd=0.2, pmod=0, vibRange=0.125,//  default vibrato range = (9/8)-1
	amod1=0, amod2=0, amod3=0, amod4=0,indexMax=6,
	lfoSpd=0.2, pmod=0, vibRange=0.125,
	lfoSinAmp=0, lfoPlsAmp=0, lfoSawAmp=0, lfoNseAmp=0,  // these mix to create lfo shape
	det1=1, det2=1, det3=1, det4=1,
	fxFrq1=0, fxFrq2=0, fxFrq3=0, fxFrq4=0;
	*/

	*initClass {
		voices = (
			// I09
			gazimba: {|freq=440,amp=0.1,dur=1,pan=0,vol=0.25|
				Synth(\txAlg3,[\freq,freq,\amp,amp,\dur,dur,\pan,pan,\vol,vol,
					\rtio1,1,\rtio2,7.02,\rtio3,7.0,\rtio4,13,
					\amp1,1,\amp2,0.4,\amp3,0.0,\amp4,0.01,
					\wv1,6,\wv4,6,\fb,6.5,
					\t1_1,0.005,\t2_1,0.2, \t3_1,0.1,\rls_1,0.1,    \p1_1,1,\p2_1,0.05,\p3_1,0.0,
					\t1_2,0.005, \t2_2,0.2, \t3_2,0.1,\rls_2,0.1,    \p1_2,1,\p2_2,0.05,\p3_2,0.0,
					\t1_3,0.005, \t2_3,0.22, \t3_3,0.01,\rls_3,0.4,   \p1_3,1,\p2_3,0.5,\p3_3,0.1,
					\t1_4,0.005, \t2_4,0.05, \t3_4,0.01, \rls_4,0.1,  \p1_4,1,\p2_4,0.3,\p3_4,0.0,
					\lfoSinAmp,0,\lfoSpd,0.3,\pmod,0;
				])
			},
			// I01 and I02
			symballs1: {|freq=440,amp=0.1,dur=1,pan=0,vol=0.25|
				Synth(\txAlg2,[\freq,freq,\amp,amp,\dur,dur,\pan,pan,\vol,vol,
					\rtio1,7.85,\rtio2,1.41,\rtio3,20.76,\rtio4,20.41,
					\amp1,1,\amp2,0.85,\amp3,0.4,\amp4,0.1,\fb,0.0,\indexMax,8,
					\t1_1,0.001, \t2_1,0.02, \t3_1,0.997,\rls_1,4,\p1_1,1,\p2_1,0.3,\p3_1,0.05,
					\t1_2,0.01, \t2_2,0.2, \t3_2,0.7,\rls_2,1,\p1_2,1,\p2_2,0.4,\p3_2,0.1,
					\t1_3,0.02, \t2_3,0.4, \t3_3,0.4,\rls_3,4,\p1_3,1,\p2_3,0.6,\p3_3,0.1,
					\t1_4,0.4, \t2_4,0.2, \t3_4,0.4,\rls_4,1,\p1_4,0.3,\p2_4,0.2,\p3_4,0.1,
					\lfoSinAmp,1,\lfoSpd,0.2,\pmod,0.01,\amod1,0.1,\amod2,0.5,\amod3,0.1,\amod4,0])
			},
			// same as above for now
			symballs2:{|freq=440,amp=0.1,dur=1,pan=0,vol=0.25|
				Synth(\txAlg3,[\freq,freq,\amp,amp,\dur,dur,\pan,pan,\vol,vol,
					\rtio1,7.85,\rtio2,1.41,\rtio3,20.76,\rtio4,20.41,
					\amp1,1,\amp2,0.85,\amp3,0.4,\amp4,0.1,\fb,0.0,\indexMax,8,
					\t1_1,0.001, \t2_1,0.02, \t3_1,0.997,\rls_1,2,\p1_1,1,\p2_1,0.3,\p3_1,0.05,
					\t1_2,0.01, \t2_2,0.2, \t3_2,0.7,\rls_2,1,\p1_2,1,\p2_2,0.4,\p3_2,0.1,
					\t1_3,0.02, \t2_3,0.4, \t3_3,0.4,\rls_3,4,\p1_3,1,\p2_3,0.6,\p3_3,0.1,
					\t1_4,0.4, \t2_4,0.2, \t3_4,0.4,\rls_4,1,\p1_4,0.3,\p2_4,0.2,\p3_4,0.1,
					\lfoSinAmp,1,\lfoSpd,0.2,\pmod,0.01,\amod1,0.1,\amod2,0.5,\amod3,0.1,\amod4,0])
			},

			// I07
			riteCelst: {|freq=440,amp=0.1,dur=1,pan=0,vol=0.25|
				Synth(\txAlg5,[\freq,freq,\amp,amp,\dur,dur,
					\rtio1,1.5,\rtio2,10.37,\rtio3,1.49,\rtio4,10.38,
					\amp1,1,\amp2,0.8,\amp3,1,\amp4,0.8,
					\t1_1,0.01, \t2_1,0.3, \t3_1,0.3,\rls_1,0.7,\p1_1,0.9,\p2_1,0.8,\p3_1,0.1,
					\t1_2,0.01, \t2_2,0.25, \t3_2,0.1,\rls_2,0.7,\p1_2,0.1,\p2_2,0.001,\p3_2,0.001,
					\t1_3,0.01, \t2_3,0.1, \t3_3,0.3,\rls_3,0.7,\p1_3,0.9,\p2_3,0.4,\p3_3,0.1,
					\t1_4,0.01, \t2_4,0.25, \t3_4,1,\rls_4,0.7,\p1_4,0.1,\p2_4,0.001,\p3_4,0.001,
					\fb,0.25,\indexMax,1,\pan,0,
					\lfoSpd,2.6,\lfoNseAmp,0,\pmod,0,\amod1,0,\amod2,0,\amod3,0,\amod4,0,
					\wv1,0,\wv2,2,\wv3,0])
			},

			// I08
			briteCelst: {|freq=440,amp=0.1,dur=1,pan=0,vol=0.25|
				Synth(\txAlg5,[\freq,freq,\amp,amp,\dur,dur,\pan,pan,\vol,vol,
					\rtio1, 1.0, \rtio2, 10.5, \rtio3, 1.5, \rtio4, 16.0 ,
					\amp1,1,\amp2,0.8,\amp3,1,\amp4,0.8,
					\lfoSpd,2.0,\lfoSineAmp,0.1,
					\amp1,1.0,\amp2,0.1,\amp3,0.1,\amp4,0.1,\pmod,0.6,\fb,0,
					\fxFreq1,0.1,\fxFreq2,0.1,\fxFreq3,0.1,\fxFreq4,0.1,

					\t1_1,0.01, \t2_1,0.3, \t3_1,0.35,\rls_1,0.4,\p1_1,0.92,\p2_1,0.2,\p3_1,0.17,
					\t1_2,0.01, \t2_2,0.34, \t3_2,0.92,\rls_2,0.5,\p1_2,0.025,\p2_2,0.2,\p3_2,0.17,
					\t3_3,0.01, \t3_3,0.3, \t3_3,0.4,\rls_3,0.4,\p1_3,0.92,\p2_3,0.2,\p3_3,0.11,
					\t4_4,0.01, \t4_4,0.28, \t3_4,1,\rls_4,0.92,\p1_4,0.3,\p2_4,0.2,\p3_4,0,
					\wv1,0,\wv2,2,\wv3,0])
			},

			// I04
			dblBass: {|freq=440,amp=0.1,dur=1,pan=0,vol=0.25|
				Synth(\txAlg3,[\freq,freq,\amp,amp,\dur,dur,\pan,pan,\vol,vol,
					\rtio1,1,\rtio2,3,\rtio3,8,\rtio4,1,
					\wv1,0,\wv2,0,\wv3,0,\fb,0.1,
					\amp1,1,\amp2,0.66,\amp3,0.71,\amp4,0.55,
					\t1_1,0.25, \t2_1,0.25, \t3_1,0.5,\rls_1,0.2,\p1_1,1,\p2_1,1,\p3_1,0.1,
					\t1_2,0.02, \t2_2,0.7, \t3_2,0.1,\rls_2,0.2,\p1_2,1,\p2_2,0.8,\p3_2,0.1,
					\t1_3,0.2, \t2_3,0.3, \t3_3,0.4,\rls_3,0.1,\p1_3,1,\p2_3,0.4,\p3_3,0.1,
					\t1_4,0.2, \t2_4,0.7, \t3_4,0.1,\rls_4,0.2,\p1_4,1,\p2_4,0.8,\p3_4,0.1,
					\lfoSinAmp,1,\spd,2,\pmod,0.1,\amod1,0,\amod2,0,\amod3,0,\amod4,0])
			},

			// I27
			waterGlass: {|freq=440,amp=0.1,dur=1,pan=0,vol=0.25|
				Synth(\txAlg2,[\freq,freq,\amp,amp,\dur,dur,\pan,pan,\vol,vol,
					\rtio1,1,\rtio2,1.73,\rtio3,3.46,\rtio4,17.27,
					\wv1,0,\wv2,0,\wv3,0,\fb,7,
					\amp1,1,\amp2,1,\amp3,1,\amp4,0.65,
					\t1_1,0.005, \t2_1,0.005, \t3_1,0.5,\rls_1,0.6,\p1_1,1,\p2_1,1,\p3_1,0.1,
					\t1_2,0.005, \t2_2,0.67, \t3_2,0.55,\rls_2,0.8,\p1_2,1,\p2_2,0.8,\p3_2,0.1,
					\t1_3,0.005, \t2_3,0.35, \t3_3,0.6,\rls_3,0.5,\p1_3,1,\p2_3,0.4,\p3_3,0.1,
					\t1_4,0.005, \t2_4,0.005, \t3_4,1.0,\rls_4,0.5,\p1_4,1,\p2_4,0.8,\p3_4,0.1,
					\lfoSinAmp,0,\lfoSpd,4,\pmod,0.7])
			},

			// I24
			brthBells: {|freq=440,amp=0.1,dur=1,pan=0,vol=0.25|
				Synth(\txAlg5,[\freq,freq,\amp,amp,\dur,dur,\pan,pan,\vol,vol,
					\rtio1,5.0,\rtio2,12.72,\rtio3,1.0,\rtio4,5.99,
					\amp1,1,\amp2,0.88,\amp3,0.99,\amp4,0.1,\fb,0.5,\indexMax,8,
					\wv1,6,\wv2,5,\wv3,5,\wv4,1,
					\t1_1,0.6, \t2_1,0.7, \t3_1,0.6,\rls_1,1,\p1_1,1,\p2_1,0.7,\p3_1,0.05,
					\t1_2,0.1, \t2_2,0.2, \t3_2,0.7,\rls_2,2,\p1_2,1,\p2_2,0.6,\p3_2,0.05,
					\t1_3,0.7, \t2_3,0.3, \t3_3,0.3,\rls_3,1,\p1_3,1,\p2_3,0.7,\p3_3,0.05,
					\t1_4,0.7, \t2_4,0.2, \t3_4,1,\rls_4,0.5,\p1_4,0.3,\p2_4,0.6,\p3_4,0.05,
					\lfoNseAmp,1,\lfoSpd,10,\pmod,0.71])
			},

			// I14,
			handDrum1:  {|freq=440,amp=0.1,dur=1,pan=0,vol=0.25|
				Synth(\txAlg2,[\freq,freq,\amp,amp,\dur,dur,\pan,pan,\vol,vol,
					\rtio1,1.06,\rtio2,0.62,\rtio3,2.06,\rtio4,2.90,
					\wv1,0,\wv2,0,\wv3,0,
					\amp1,1,\amp2,1,\amp3,0.6,\amp4,0.8,\fb,1,\indexMax,8,
					\t1_1,0.01, \t2_1,0.58, \t3_1,0.4,\rls_1,0.95,\p1_1,0.55,\p2_1,0.2,\p3_1,0.1,
					\t1_2,0.01, \t2_2,0.85, \t3_2,0.25,\rls_2,0.85,\p1_2,0.33,\p2_2,0.1,\p3_2,0.1,
					\t1_3,0.05, \t2_3,0.22, \t3_3,0.22,\rls_3,0.85,\p1_3,0.2,\p2_3,0.2,\p3_3,0.1,
					\t1_4,0.01, \t2_4,0.65, \t3_4,0.33,\rls_4,0.85,\p1_4,0.2,\p2_4,0.2,\p3_4,0.1,
					\pmod,0.66,\amod1,0,\amod2,0,\amod3,0,\amod4,0,
					\lfoSinAmp,0, \lfoPlsAmp,0, \lfoSawAmp,0,\lfoNseAmp,0,  // these mix to create lfo shape
					\fxFrq1,0, \fxFrq2,0,\fxFrq3,0, \fxFrq4,0.1,  // fixed freq alternative
				]);
			},

			//  I15
			handDrum2: {|freq=440,amp=0.1,dur=1,pan=0,vol=0.25|
				Synth(\txAlg2,[\freq,freq,\amp,amp,\dur,dur,\pan,pan,\vol,vol,
					\rtio1,2.06,\rtio2,0.68,\rtio3,2.06,\rtio4,0.56,
					\wv1,0,\wv2,0,\wv3,0,
					\amp1,1,\amp2,1,\amp3,0.6,\amp4,0.8,\fb,0.2,\indexMax,8,
					\t1_1,0.01, \t2_1,0.18, \t3_1,0.5,\rls_1,0.85,\p_1,0.7,\p2_1,0.2,\p3_1,0.1,
					\t1_2,0.01, \t2_2,0.65, \t3_2,0.8,\rls_2,0.85,\p1_2,0,\p2_2,0.1,\p3_2,0.1,
					\t1_3,0.05, \t2_3,0.24, \t3_3,0.55,\rls_3,0.85,\p1_3,0.4,\p2_3,0.2,\p3_3,0.1,
					\t1_4,0.5, \t2_4,0.55, \t3_4,1.0,\rls_4,0.85,\p1_4,0.47,\p2_4,0.2,\p3_4,0.1,
					\pmod,0.66,\amod1,0,\amod2,0,\amod3,0,\amod4,0,
					\lfoSinAmp,0, \lfoPlsAmp,0, \lfoSawAmp,0,\lfoNseAmp,0,  // these mix to create lfo shape
					\fxFrq1,0, \fxFrq2,0,\fxFrq3,0, \fxFrq4,0.1,  // fixed freq alternative
				]);
			},

			// I16
			handDrum3: {|freq=440,amp=0.1,dur=1,pan=0,vol=0.25|
				Synth(\txAlg2,[\freq,freq,\amp,amp,\dur,dur,\pan,pan,\vol,vol,
					\rtio1,1.66,\rtio2,8.97,\rtio3,2.06,\rtio4,0.56,
					\wv1,0,\wv2,0,\wv3,0,
					\amp1,1,\amp2,1,\amp3,0.6,\amp4,0.8,\fb,0.2,\indexMax,8,
					\t1_1,0.01, \t2_1,0.32, \t3_1,0.8,\rls_1,0.85,\p_1,0.7,\p2_1,0.1,\p3_1,0.01,
					\t1_2,0.01, \t2_2,0.65, \t3_2,0.9,\rls_2,0.93,\p1_2,0,\p2_2,0.1,\p3_2,0.01,
					\t1_3,0.05, \t2_3,0.22, \t3_3,0.85,\rls_3,0.88,\p1_3,0.4,\p2_3,0.08,\p3_3,0.01,
					\t1_4,0.01, \t2_4,0.7, \t3_4,1.0,\rls_4,0.99,\p1_4,0.47,\p2_4,0.1,\p3_4,0.01,
					\pmod,0.66,\amod1,0,\amod2,0,\amod3,0,\amod4,0,
					\lfoSinAmp,0, \lfoPlsAmp,0, \lfoSawAmp,0,\lfoNseAmp,0,  // these mix to create lfo shape
					\fxFrq1,0, \fxFrq2,0,\fxFrq3,0, \fxFrq4,0,  // fixed freq alternative
				]);
			},

			// I17
			wDrum: {|freq=440,amp=0.1,dur=1,pan=0,vol=0.25|
				Synth(\txAlg2,[\freq,freq,\amp,amp,\dur,dur,\pan,pan,\vol,vol,
					\rtio1,1.66,\rtio2,8.97,\rtio3,0.56,\rtio4,0.56,
					\wv1,0,\wv2,0,\wv3,0,
					\amp1,1,\amp2,1,\amp3,0.6,\amp4,0.8,\fb,0.2,\indexMax,8,
					\t1_1,0.01, \t2_1,0.45, \t3_1,0.8,\rls_1,0.85,\p_1,0.3,\p2_1,0.1,\p3_1,0.01,
					\t1_2,0.01, \t2_2,0.35, \t3_2,0.9,\rls_2,0.93,\p1_2,0.3,\p2_2,0.1,\p3_2,0.01,
					\t1_3,0.1, \t2_3,0.85, \t3_3,0.85,\rls_3,0.88,\p1_3,0.2,\p2_3,0.08,\p3_3,0.01,
					\t1_4,0.01, \t2_4,0.4, \t3_4,1.0,\rls_4,0.99,\p1_4,0.18,\p2_4,0.1,\p3_4,0.01,
					\pmod,0.66,\amod1,0,\amod2,0,\amod3,0,\amod4,0,
					\lfoSinAmp,0, \lfoPlsAmp,0, \lfoSawAmp,0,\lfoNseAmp,0,  // these mix to create lfo shape
					\fxFrq1,0, \fxFrq2,0,\fxFrq3,0, \fxFrq4,0,  // fixed freq alternative
				]);
			},

			// I19
			xylo1: {|freq=440,amp=0.1,dur=1,pan=0,vol=0.25|
				Synth(\txAlg3,[\freq,freq,\amp,amp,\dur,dur,\pan,pan,\vol,vol,
					\rtio1,1.0,\rtio2,5.0,\rtio3,8,\rtio4,3,
					\wv1,1,\wv2,5,\wv3,0,
					\amp1,1,\amp2,1,\amp3,1,\amp4,0.9,\fb,0.1,\indexMax,2,
					\t1_1,0.01, \t2_1,0.01, \t3_1,0.5,\rls_1,0.5,\p_1,1,\p2_1,0.1,\p3_1,0.01,
					\t1_2,0.01, \t2_2,0.25, \t3_2,0.45,\rls_2,0.3,\p1_2,0.3,\p2_2,0.1,\p3_2,0.01,
					\t1_3,0.01, \t2_3,0.01, \t3_3,0.6,\rls_3,0.5,\p1_3,1,\p2_3,0.1,\p3_3,0.01,
					\t1_4,0.01, \t2_4,0.3, \t3_4,1.0,\rls_4,0.6,\p1_4,0.3,\p2_4,0.1,\p3_4,0.01,
					\pmod,0,\amod1,0,\amod2,0,\amod3,0,\amod4,0,
					\lfoSinAmp,0, \lfoPlsAmp,0, \lfoSawAmp,0,\lfoNseAmp,0,  // these mix to create lfo shape
					\fxFrq1,0, \fxFrq2,0,\fxFrq3,0.15, \fxFrq4,0,  // fixed freq alternative
				]);
			},
			// I18
			xylo2: {|freq=440,amp=0.1,dur=1,pan=0,vol=0.25|
				Synth(\txAlg3,[\freq,freq,\amp,amp,\dur,dur,\pan,pan,\vol,vol,
					\rtio1,2.0,\rtio2,5.0,\rtio3,1,\rtio4,3,
					\wv1,0,\wv2,6,\wv3,0,
					\amp1,1,\amp2,1,\amp3,1,\amp4,1,\fb,0.2,\indexMax,4,
					\t1_1,0.01, \t2_1,0.01, \t3_1,0.5,\rls_1,0.5,\p_1,1,\p2_1,0.1,\p3_1,0.01,
					\t1_2,0.01, \t2_2,0.25, \t3_2,0.45,\rls_2,0.3,\p1_2,0.3,\p2_2,0.1,\p3_2,0.01,
					\t1_3,0.01, \t2_3,0.01, \t3_3,0.6,\rls_3,0.5,\p1_3,1,\p2_3,0.1,\p3_3,0.01,
					\t1_4,0.01, \t2_4,0.1, \t3_4,0.7,\rls_4,0.6,\p1_4,0.3,\p2_4,0.1,\p3_4,0.01,
					\pmod,0.85,\amod1,0,\amod2,0,\amod3,0,\amod4,0,
					\lfoSinAmp,0, \lfoPlsAmp,0, \lfoSawAmp,0,\lfoNseAmp,0,  // these mix to create lfo shape
					\fxFrq1,0, \fxFrq2,0,\fxFrq3,0, \fxFrq4,0.11,  // fixed freq alternative
				]);
			}
		);
		// perf presets 19-24

	}

	*storeVoice {|name,func|
		TxVc.voices.put(\name,func)
	}

	*presets {|num|
		^switch(num,
			1,{[\symballs1,\symballs1,\symballs1,\symballs1]},
			2,{[\symballs2,\wDrum,\gazimba,\symballs1]},
			3,{[\dblBass,\wDrum,\gazimba,\handDrum2]},
			4,{[\handDrum3,\xylo1,\briteCelst,\handDrum1]},
			5,{[\waterGlass,\xylo2,\riteCelst,\handDrum1]},
			6,{[\waterGlass,\b,\brthBells,\handDrum1]}
		)
	}
}

/*

TxSynthDefs.init
TxVc.voices[\gazimba].(200,0.3,1)
TxVc.voices[\symballs].(400,0.3,1)
TxVc.voices[\riteCelst].(880,0.3,1)
TxVc.voices[\dblBass].(110,0.3,2)
TxVc.voices[\waterGlass].(840,0.3,2)
(
~wdrum= {|freq=440,amp=0.1,dur=1,pan=0,vol=0.25|
Synth(\txAlg2,[\freq,freq,\amp,amp,\dur,dur,\pan,pan,\vol,vol,
\rtio1,1.66,\rtio2,8.97,\rtio3,2.06,\rtio4,0.56,
\amp1,1,\amp2,0.88,\amp3,0.99,\amp4,0.6,\fb,0.8,\indexMax,8,
\t1_1,1, \t2_1,0.6, \t3_1,0.4,\rls_1,0.1,\p1_1,1,\p2_1,0.6,\p3_1,0.05,
\t1_2,1, \t2_2,0.75, \t3_2,0.15,\rls_2,0.2,\p1_2,1,\p2_2,0.6,\p3_2,0.05,
\t1_3,0.95, \t2_3,0.3, \t3_3,0.3,\rls_3,0.2,\p1_3,1,\p2_3,0.35,\p3_3,0.05,
\t1_4,1, \t2_4,0.7, \t3_4,0.1,\rls_4,0.2,\p1_4,1,\p2_4,0.4,\p3_4,0.05,
\lfoSinAmp,1,\lfoSpd,0.7,\pmod,0.8])
};
TxVc.storeVoice(\wdrum,~wdrum);
~wdrum.value(220,0.5,4,0,0.5)
)

// I17 Wdrum
Alg2 Fb=7
LFO tri spd=35 dly=0 PMmod Depth= 8 Amod Depth = 0 Sync=off
PMod sens = 4
AMS = of of of of
EBS = 0 0 0 0
KVS 1 0 1 1
FRQ Rtio 1.66 8.97 2.06 0.56
Wvs 1 1 1 1
Det 0 1 0 0
AR 31 31 29 31  // 0-31
D1R 17 24 7 22 // 0-15
D1L 9 9 6 7  // 0-15
D2R 7 3 6 0
RR 1 3 3 3  // 1-15
Shft of of of off
OUT 99 99 60 80
SCALING RS 0 0 0 0  // 0-3  op4
LS 0 2 0 0  // 0-99
PBend RNGE = 0
note 60 = C3
RVB Rate = 7

)
*/
