// console effect Synths

ConsoleEffSynthDefs {
	classvar <>synthDefs, <>numOuts = 2, <>out=0, <>azArray, <>elArray, <>rhoArray=1, maxDist=12, <>panFactor=0.5pi,mstrLev=0.25;

	*init {
		azArray = [-0.25pi, 0.25pi, 0.75pi, 1.25pi];
		elArray = [0,0,0,0]; rhoArray = [1,1,1,1];
		switch(numOuts,
			2,{ panFactor = 0.5pi; azArray= [-0.25pi,0.25pi]; elArray = [0,0]},
			4,{ panFactor = pi; azArray = [-0.25pi, 0.25pi, 0.75pi, 1.25pi] },
			6,{ panFactor = pi; azArray = [-0.25pi, 0.25pi, 0.75pi, 1.25pi,-0.5pi, 0.5pi];
				elArray = [-0.5pi,-0.5pi,-0.5pi,-0.5pi, 0.25pi,0.25pi];
				rhoArray = [1,1,1,1,1,1] },
			8,{ panFactor = pi;  // littlefield (3 not used)
				azArray = [-0.454,0.454,0,0,-1.047,1.047,-2.39,2.39];  // angle
			elArray = [0.088,0.088,0,1.22,0.35,0.35,0.524,0.524];  // elev
				maxDist=25;  // meters
				rhoArray = [1,1,0,0,0.56,0.5,0.8,0.8]*maxDist}); // rho (distance)

		synthDefs = [

			SynthDef("combDelay", { arg dlyt,dcyt,cAmp,pan,effAmp,gate=1,effIn,effInVol=1,vol,elev;
				var comb,in, maxDelay=4, maxDecay=20, delay, ctrVol,w,x,y,z,env;
				in = In.ar(effIn)*effInVol;
				env=EnvGen.kr(Env.asr(0.25,1,0.5), gate: gate,  doneAction: 2);
				comb = CombC.ar(in, maxDelay, dlyt,
					dcyt.neg*dlyt,env)*effAmp;   		// decay
				#w,x,y,z = BFEncode1.ar((comb+(in*cAmp))*vol,pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef("combAutoPan_4", { arg dlyt,dcyt,cAmp,spd,effAmp,gate=1,effIn,effInVol=1,vol,elev;
				var comb,in, maxDelay=4, maxDecay=20, delay,panlfo,w,x,y,z;
				in = In.ar(effIn)*effInVol;
				panlfo = LFNoise1.kr(spd);
				comb = CombC.ar(in, maxDelay, dlyt,
					(dcyt.neg)*dlyt);   		// decay
				#w,x,y,z = BFEncode1.ar((comb+(in*cAmp))*vol,panlfo*panFactor,elev)
				*EnvGen.kr(Env.asr(0.25,1.0,0.5), gate: gate,  doneAction: 2);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef("2combDelays", { arg dly1,dcyt1,a3,a4,a5,a6,a7,amp1,
				dly2,dcyt2,a10,a11,a12,wdth,pan,amp2,gate=1,effIn,effInVol=1, vol,elev;
				var comb,in, maxDelay=4, maxDecay=20, delay, ctrVol,w,x,y,z,w2,x2,y2,z2;
				in = In.ar(effIn)*effInVol;
				comb = CombC.ar(in, maxDelay,
					[dly1,dly2], [dcyt1.neg*dly1,dcyt2.neg*dly2], [amp1,amp2])
				*EnvGen.kr(Env.asr(0.25,1.0,0.5),gate:gate, doneAction: 2);
				#w,x,y,z = BFEncode1.ar(comb[0],pan*panFactor,elev,gain:vol);
				#w2,x2,y2,z2 = BFEncode1.ar(comb[1],(pan+wdth).wrap(-1.0,1.0)*panFactor,elev,gain:vol);
				Out.ar(out,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
			}).store,

			SynthDef("freqShifter", { arg frqShift,fmul,lfrq,fDpth,lAmp,rAmp,pan,wdth,gate=1,effIn, effInVol=1, vol,elev;
				var in, r1,c1, r2, c2, pmodLFO, src,w,x,y,z,w2,x2,y2,z2;
				in = In.ar(effIn)*effInVol;
				pmodLFO = SinOsc.kr(lfrq,0,fDpth);
				#c1,r1 = Hilbert.ar(LeakDC.ar(in));
				r2 = SinOsc.ar((frqShift*fmul)-(fmul/2)+pmodLFO, pi/2);
				c2 = SinOsc.ar((frqShift*fmul)-(fmul/2)+pmodLFO);
				src = [(((r1*r2)-(c1*c2)))*lAmp,
					(((r1*r2)+(c1*c2)))*rAmp];
				#w,x,y,z = BFEncode1.ar(src[0]
					*EnvGen.kr(Env.asr(0.25,1.0,0.5), gate: gate, doneAction: 2)*vol,
					pan*panFactor,elev);
				#w2,x2,y2,z2 = BFEncode1.ar(src[1]
					*EnvGen.kr(Env.asr(0.25,1.0,0.5), gate: gate, doneAction: 2)*vol,
					(pan+wdth).wrap(-1.0,1.0)*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
			}).store,

			SynthDef("frqShift", { arg frqShift,fmul,lfrq,fDpth,lPhs,aDpt1,loPan,loAmp,
				a9,a10,a11,a12,a13,a14,hiPan,hiAmp,gate=1, effIn, effInVol=1, vol,elev;
				var in, r1,c1, r2, c2, pmodLFO, src,maxFreq=25,lfoFrq,w,x,y,z,w2,x2,y2,z2;
				in = In.ar(effIn)*effInVol;
				lfoFrq = lfrq+(Amplitude.kr(in,0.01,0.5)
					*aDpt1);		// lfoFreq
				pmodLFO = SinOsc.kr(lfoFrq,lPhs,fDpth);
				#c1,r1 = Hilbert.ar(LeakDC.ar(in));
				r2 = SinOsc.ar(((frqShift+pmodLFO)*fmul)-(fmul/2), pi/2);
				c2 = SinOsc.ar(((frqShift+pmodLFO)*fmul)-(fmul/2));
				src = [((r1*r2)-(c1*c2))*loAmp,
					((r1*r2)+(c1*c2))*hiAmp];
				#w,x,y,z = BFEncode1.ar(src[0]
					*EnvGen.kr(Env.asr(0.25,1.0,0.5), gate: gate, doneAction: 2)*vol,
					LFSaw.kr(loPan,Rand(0,2))*panFactor,elev);
				#w2,x2,y2,z2 = BFEncode1.ar(src[1]
					*EnvGen.kr(Env.asr(0.25,1.0,0.5), gate: gate, doneAction: 2)*vol,
					LFSaw.kr(hiPan,Rand(0,2))*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
			}).store,

			SynthDef("frqShftCmp", { arg  frqShift,fmul,lfrq,fDpth,lPhs,aDpt1,loPan,loAmp,
				thrsh,slpb,slpa,att,rls,a13,hiPan,hiAmp,gate=1, effIn, effInVol=1, vol,elev;
				var in, r1,c1, r2, c2, pmodLFO, src,maxFreq=25,lfoFrq,w,x,y,z,w2,x2,y2,z2;
				in = In.ar(effIn)*effInVol;
				lfoFrq = lfrq+(Amplitude.kr(in,0.01,0.5)
					*aDpt1);		// lfoFreq
				pmodLFO = SinOsc.kr(lfoFrq,lPhs,fDpth);
				#c1,r1 = Hilbert.ar(LeakDC.ar(in));
				r2 = SinOsc.ar(((frqShift+pmodLFO)*fmul)-(fmul/2), pi/2);
				c2 = SinOsc.ar(((frqShift+pmodLFO)*fmul)-(fmul/2));
				src = Pan2.ar(
					Compander.ar(((r1*r2)-(c1*c2)),in,thrsh,slpb,slpa,att,rls),loPan,loAmp)+
				Pan2.ar(
					Compander.ar(((r1*r2)+(c1*c2)),in,thrsh,slpb,slpa,att,rls),hiPan,hiAmp);
				#w,x,y,z = BFEncode1.ar(src[0]
					*EnvGen.kr(Env.asr(0.25,1.0,0.5), gate: gate, doneAction: 2)*vol,
					LFSaw.kr(loPan,Rand(0,2))*panFactor,elev);
				#w2,x2,y2,z2 = BFEncode1.ar(src[1]
					*EnvGen.kr(Env.asr(0.25,1.0,0.5), gate: gate, doneAction: 2)*vol,
					LFSaw.kr(hiPan,Rand(0,2))*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
			}).store,

			SynthDef("freqShftCmp", { arg frqShift,fmul,thrsh1,slpb1,slpa1,att1,rls1,ampL,
				lfrq,fDpth,thrsh2,slpb2,slpa2,att2,rls2,ampR,gate=1, effIn, effInVol=1, vol, elev;
				var in, r1,c1, r2, c2, pmodLFO, src,w,x,y,z;
				in = In.ar(effIn)*effInVol;
				pmodLFO = SinOsc.kr(lfrq,0,fDpth);
				#c1,r1 = Hilbert.ar(LeakDC.ar(in));
				r2 = SinOsc.ar((frqShift*fmul)-(fmul/2)+pmodLFO, pi/2);
				c2 = SinOsc.ar((frqShift*fmul)-(fmul/2)+pmodLFO);
				src = [Compander.ar(((r1*r2)-(c1*c2))*ampL,in,thrsh1,slpb1,slpa1,att1,rls1),
					Compander.ar(((r1*r2)+(c1*c2))*ampR,in,thrsh2,slpb2,slpa2,att2,rls2)];
				#w,x,y,z = BFEncode1.ar(Mix(src)
					*EnvGen.kr(Env.asr(0.25,1.0,0.5), gate: gate, doneAction: 2)*vol,0,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef("2delayRings",{ arg mfrq1,mDpt1,dly1,dcyt1,a5,a6,lPan, lAmp,mfrq2,mDpt2,dly2,dcyt2,a13,wdth,rPan,rAmp,gate=1,effIn,effInVol=1, vol,elev;
				var leftModOsc, rightModOsc,maxDelay=2, maxFreq=4000,in,src,w,x,y,z,w2,x2,y2,z2;
				in = In.ar(effIn)*effInVol;
				leftModOsc = SinOsc.ar((mfrq1*mDpt1));
				rightModOsc = SinOsc.ar((mfrq2*mDpt2));
				src = [CombC.ar(in,maxDelay,dly1.max(0.005),dcyt1,leftModOsc),
					CombC.ar(in,maxDelay,dly2.max(0.005),dcyt2,rightModOsc)]*vol;
				#w,x,y,z = BFEncode1.ar(src[0]
					*EnvGen.kr(Env.asr(0.25,1.0,0.5), gate: gate, doneAction: 2)*lAmp,
					LFSaw.kr(lPan,Rand(0,wdth))*panFactor,elev);
				#w2,x2,y2,z2 = BFEncode1.ar(src[1]
					*EnvGen.kr(Env.asr(0.25,1.0,0.5), gate: gate, doneAction: 2)*rAmp,
					LFSaw.kr(rPan,Rand(0,wdth))*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
			}).store,

			SynthDef(\bufPShift,{ arg dlyPhs,fb,pShft,pDsp,tDsp,wsize,recBuf,pan,amp,
				gate=1,effIn,effInVol=1, bufNum,vol,elev;
				var envelope,in,tap,w,x,y,z;
				envelope = EnvGen.kr(Env.asr(0.05, 1, 0.05), gate: gate, doneAction: 2);
				in = In.ar(effIn)*effInVol;
				tap = PitchShift.ar(
					BufRd.ar(1,bufNum,
						Phasor.ar(Impulse.kr(BufDur.ir(bufNum).reciprocal),
							BufRateScale.ir(bufNum), 0, BufFrames.ir(bufNum),
							BufFrames.ir(bufNum)*(1-dlyPhs))
						,1),
					wsize,pShft,pDsp*pShft,tDsp*wsize,amp);
				BufWr.ar(in+(tap*fb),bufNum,
					Phasor.ar(0, BufRateScale.ir(bufNum), 0, BufFrames.ir(bufNum)),1);
				#w,x,y,z = BFEncode1.ar(tap*envelope*vol,
					pan,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef("2bufPShifts",{ arg pShft1,dlyPhs1,fb1,pDsp1,tDsp1,recBuf,pan1,amp1,
				pShft2,dlyPhs2,fb2,pDsp2,tDsp2,wsize,pan2,amp2,gate=1,effIn,effInVol=1, bufNum,vol,elev;
				var envelope,in,tap1,tap2,w,x,y,z,w2,x2,y2,z2;
				envelope = EnvGen.kr(Env.asr(0.05, effInVol, 0.05), gate: gate, doneAction: 2);
				in = In.ar(effIn)*envelope;
				tap1 = PitchShift.ar(
					BufRd.ar(1,bufNum,
						Phasor.ar(Impulse.kr(BufDur.ir(bufNum).reciprocal),
							BufRateScale.ir(bufNum), 0, BufFrames.ir(bufNum),
							BufFrames.ir(bufNum)*(1-dlyPhs1))
						,1),
					wsize,pShft1,pDsp1*pShft1,tDsp1*wsize,amp1);
				BufWr.ar(in+(tap1*fb1),bufNum,
					Phasor.ar(0, BufRateScale.ir(bufNum), 0, BufFrames.ir(bufNum)),1);
				tap2 = PitchShift.ar(
					BufRd.ar(1,bufNum+1,
						Phasor.ar(Impulse.kr(BufDur.ir(bufNum+1).reciprocal),
							BufRateScale.ir(bufNum+1), 0, BufFrames.ir(bufNum+1),
							BufFrames.ir(bufNum+1)*(1-dlyPhs2))
						,1),
					wsize,pShft2,pDsp2*pShft2,tDsp2*wsize,amp2);
				BufWr.ar(in+(tap2*fb2),bufNum+1,
					Phasor.ar(0, BufRateScale.ir(bufNum+1), 0, BufFrames.ir(bufNum+1)),1);
				#w,x,y,z = BFEncode1.ar(tap1,LFSaw.kr(pan1,Rand(0,2))*panFactor,elev)*vol;
				#w2,x2,y2,z2 = BFEncode1.ar(tap2,LFSaw.kr(pan2,Rand(0,2))*panFactor,elev)*vol;
				Out.ar(out,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
			}).store,

			SynthDef("pitchShift",{ arg pShft,pDsp,tDsp,wsize,pan,amp,gate=1, effIn, effInVol=1, vol,elev;
				var in,src,w,x,y,z;
				in = In.ar(effIn)*effInVol;
				src = PitchShift.ar(in,wsize,pShft,pDsp,tDsp,amp);
				#w,x,y,z = BFEncode1.ar(src*vol*
					EnvGen.kr(Env.asr(0.25,1.0,0.5),gate: gate, doneAction: 2),
					pan*panFactor,elev);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef("2PitchShifts",{ arg pShft1,pDsp1,tDsp1,wsize1,a5,wdth1,pan1,amp1,
				pShft2,pDsp2,tDsp2,wsize2,a13,wdth2,pan2,amp2,gate=1,effIn,effInVol=1, vol,elev;
				var envelope,in,sh1,sh2,w,x,y,z,w2,x2,y2,z2;
				envelope = EnvGen.kr(Env.asr(0.05, 1, 0.05), gate: gate, doneAction: 2);
				in = In.ar(effIn)*effInVol;
				sh1 = PitchShift.ar(
					in,wsize1,pShft1,pDsp1*pShft1,tDsp1*wsize1,amp1);
				sh2 = PitchShift.ar(
					in,wsize2,pShft2,pDsp2*pShft2,tDsp2*wsize2,amp2);
				#w,x,y,z = BFEncode1.ar(sh1,LFSaw.kr(pan1,Rand(0,wdth1))*panFactor,elev)*envelope*vol;
				#w2,x2,y2,z2 = BFEncode1.ar(sh2,LFSaw.kr(pan2,Rand(0,wdth2))*panFactor,elev)*envelope*vol;
				Out.ar(out,BFDecode1.ar(w+w2,x+x2,y+y2,z+z2,azArray,elArray))
			}).store,

			SynthDef(\binShift,{ arg bShft,stch,fftBuf,a4,a5,a6,a7,a8,a9,a10,a11,a12,a13,a14,
				pan,amp,gate=1,effIn,effInVol=1, bufNum,vol,elev;
				var in, fft, chain, shift, stretch,w,x,y,z;
				in = In.ar(effIn)*effInVol;
				fft = FFT(bufNum,in);
				chain = PV_BinShift(fft, stch, bShft);
				#w,x,y,z = BFEncode1.ar(IFFT(chain),pan*panFactor,elev,gain: amp)*
				EnvGen.kr(Env.asr(0.25,1.0,0.5),gate: gate, doneAction: 2)*vol;
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef("2binShifts",{ arg bShft1,stch1,fftBuf,a4,a5,a6,pan1,amp1,
				bShft2,stch2,a11,a12,a13,a14,pan2,amp2,gate=1,effIn,effInVol=1, bufNum,vol,elev;
				var in, fft,fft2,chain1,chain2,w,x,y,z,w2,x2,y2,z2,envelope;
				envelope = EnvGen.kr(Env.asr(0.25,1.0,0.5),gate: gate, doneAction: 2);
				in = In.ar(effIn)*effInVol;
				fft = FFT(bufNum,in);
				fft2 = FFT(bufNum+1,in);	// potential bug if bufNum+1 > numFFTbuffers
				chain1 = PV_BinShift(fft, stch1, bShft1);
				chain2 = PV_BinShift(fft2, stch2, bShft2);
				#w,x,y,z =
				BFEncode1.ar(IFFT(chain1),LFSaw.kr(pan1,Rand(0,2))*panFactor,elev)*amp1*envelope*vol;
				#w2,x2,y2,z2 =
				BFEncode1.ar(IFFT(chain2),LFSaw.kr(pan2,Rand(0,2))*panFactor,elev)*amp2*envelope*vol;
				Out.ar(out,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
			}).store,

			SynthDef("waveShaper",{ arg wvsBuf,thrsh,slpb,slpa,pan,amp,
				gate=1,effIn,effInVol=1, bufNum,vol,elev;
				var in,sig,w,x,y,z;
				in = In.ar(effIn)*effInVol;
				sig = LeakDC.ar(Shaper.ar(bufNum,in,amp),0.995);
				sig = Compander.ar(sig,sig,thrsh,slpb,slpa,0.001,0.3);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev)*amp*vol*
				EnvGen.kr(Env.asr(0.01,1,0.1),gate: gate, doneAction: 2);
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef("wvsAmpFlanger",{ arg wvsBuf,thrsh,slpb,slpa,lofreq,mod,fb,
				ampMod,a9,a10,a11,a12,a13,a14,pan,amp,
				gate=1,effIn,effInVol=1,bufNum,vol,elev;
				var in,sig,local,dMod,dly,minDly=0.0011,maxDly=0.001,
				inAmp,w,x,y,z,eg;
				eg=EnvGen.kr(Env.asr(0.01,1,1),gate: gate,doneAction:2);
				in = In.ar(effIn)*effInVol;
				inAmp=Slew.kr(Amplitude.kr(in,0.1,1,ampMod));
				sig = LeakDC.ar(Shaper.ar(bufNum,in,amp),0.995);
				sig = Compander.ar(sig,sig,thrsh,slpb,slpa,0.0025,0.03)*eg;
				local = (LocalIn.ar(1)*fb)+sig;
				dMod = SinOsc.kr(lofreq+(40*inAmp), 3pi*0.5, maxDly*mod, minDly);
				dly = DelayC.ar(local, 1, dMod);
				LocalOut.ar(dly);
				#w,x,y,z = BFEncode1.ar(dly,pan*panFactor,elev)*amp*vol;
				Out.ar(out,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef("reverb",{ arg fb,flt,pan,amp,wdth,gate=1,effIn,effInVol=1,vol,elev;
				var local, in, inAmp,w,x,y,z,w2,x2,y2,z2,env;
				in = In.ar(effIn)*effInVol;
				inAmp = Amplitude.kr(in);
				//	in = in * (inAmp > 0.04); // noise gate
			//	in = in * 0.4;
				local = LocalIn.ar(2) * fb.min(0.98);
				local = OnePole.ar(local, flt.min(0.98));
				local = Rotate2.ar(local[0], local[1], 0.237);
				local = AllpassN.ar(local, 0.05, {Rand(0.01,0.05)}.dup, 2);
				local = DelayN.ar(local, 0.3, {Rand(0.15,0.33)}.dup);
				local = AllpassN.ar(local, 0.05, {Rand(0.03,0.15)}.dup, 2);
				local = LeakDC.ar(local);
				local = local + in;
				4.do {
					var t;
					t = {Rand(0.005,0.02)}.dup;
					local = AllpassN.ar(local, t, t, 1);
				};
				LocalOut.ar(local);
				env = EnvGen.kr(Env.asr(0.01,1,0.1),gate: gate, doneAction: 2);
				#w,x,y,z = BFEncode1.ar(local[0],pan*panFactor,elev)*amp*vol*env;
				#w2,x2,y2,z2 = BFEncode1.ar(local[1],(pan+wdth).wrap(-1.0,1.0)*panFactor,elev)*amp*vol*env;
				Out.ar(out,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
			}).store
		]
	}
}