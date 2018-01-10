ConsoleSynthDefs {
	classvar <>synthDefs, <>numOuts = 2, <>azArray, <>elArray, <>rhoArray=1, maxDist=12, <>panFactor=0.5pi,
	mstrLev=0.25;

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
			SynthDef(\boom,{ arg freq=60,sustain=1,curve=4,fold=0.6,amp=0.2,
				effAmp,pan=0,sigAmp=0.1,gate,recBus,effBus,vol,elev=0;
				var sig,env,w,x,y,z;
				env = EnvGen.kr(Env.perc(0.01,sustain,amp,curve),doneAction:2);
				sig = SinOsc.ar([freq,freq*0.97],[0,2pi*0.015]);
				sig = Mix((sig fold2: fold).softclip*env);
				Out.ar(recBus,sig*sigAmp);
				Out.ar(effBus,sig*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(sig*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\multiOsc,{ arg sinFreq=220, sinAmp=0.1, sawFreq=220, sawAmp=0.1,
				plsFreq=220, plsW=0.1, plsAmp=0.1, att=0, dcy=0, sus=1, rls=0,
				a11,a12,effAmp,pan=0,sigAmp=0.1,gate,recBus,effBus,vol,elev=0;
				var sig, env,w,x,y,z;
				env = EnvGen.kr(Env.adsr(attackTime: att,
					decayTime: dcy, sustainLevel: sus,
					releaseTime: rls), levelScale: mstrLev,
				gate: gate, doneAction: 2 );
				sig = (SinOsc.ar(sinFreq,0,sinAmp)+LFSaw.ar(sawFreq,0,sawAmp)+
					LFPulse.ar(plsFreq,0,plsW,plsAmp))*env;
				Out.ar(recBus,sig*sigAmp);
				Out.ar(effBus,sig*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(sig*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\vOsc,{ arg frq=440,tblBuf=1,swpFrq=1,swDpth=0,vib=1,dpth=0,
				rng=0.067,a8,a9,a10,a11,a12,a13,
				effAmp=0, pan=0, sigAmp=0,gate,recBus,effBus,vol,bufNum,elev=0;
				var env,swpLFO, vibrLFO,sig,bufStart,bufPtr,numBufs=8,w,x,y,z;
				numBufs=numBufs-1.001;
				vibrLFO = SinOsc.kr(vib,0,dpth);
				bufPtr=bufNum+((tblBuf-1).min(numBufs-0.001));
				swpLFO = LFTri.kr(swpFrq*0.5,0,swDpth*(numBufs-(tblBuf-1))).abs;
				env = EnvGen.kr(Env.asr(0.05,1,0.1), gate: gate,  levelScale: mstrLev, doneAction: 2);
				sig= VOsc.ar(bufPtr+swpLFO,frq+(frq*(rng.max(0.067))*vibrLFO))*env;
				Out.ar(recBus,sig*sigAmp);
				Out.ar(effBus,sig*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(sig*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\vOscHrm,{ arg frq,tblBuf,rng,timbre,btFrq,swpFrq,swDpth,
				att=0.05,rls=0.1,a10,a11,a12,a13,
				effAmp,pan,sigAmp,gate,recBus,effBus,vol,bufNum,elev=0;
				var sig,env,bufPtr,numHrms=7,frqDiv=4,bufOffset,swpLFO,
				w,x,y,z,w2,y2,x2,z2;
				frq = frq/frqDiv;
				bufPtr = (bufNum+((Lag.kr(timbre,0.05))*rng))
				.clip(bufNum,bufNum+(rng-1)-0.01);
				bufOffset = (tblBuf-1)*numHrms;	// which harmonic table offset
				env = EnvGen.kr(Env.asr(att,1,rls), gate: gate,  levelScale: mstrLev, doneAction: 2);
				swpLFO = LFTri.kr(swpFrq*0.5,0,swDpth*(numHrms-1-bufPtr)).abs;
				sig = VOsc.ar(bufPtr+bufOffset+swpLFO,[frq,frq+btFrq])*env;
				Out.ar(recBus,Mix.new(sig)*sigAmp);
				Out.ar(effBus,Mix.new(sig)*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(sig[0]*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				#w2,x2,y2,z2 = BFEncode1.ar(sig[1]*(1-effAmp)*sigAmp*vol,
					pan.wrap(-1.0,1.0)*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
			}).store,

			SynthDef("granSampler",{ arg rate=1,strt=0,end=0, sinfrq=2, sinAmp=0.1,
				sawfrq=1, sawPhs=0,sawAmp=0.1, plsfrq=2,plsPhs=0,plsWdth=0.1,plsAmp=0.1,
				sampBuf,effAmp=0,pan=0, sigAmp=0.1, gate=1, bufNum,recBus,effBus,vol,elev=0;
				var dir, thisRate, grainDur,clk,env,gateEnvGen,sampler,looplength=1,
				chngTrg,plsLFO,sineLFO, sawLFO,attRelTime=0.0125,minGrainDur=0.005,w,x,y,z;
				// synth env
				gateEnvGen = EnvGen.kr(
					Env.asr(attackTime: attRelTime*0.5, releaseTime: attRelTime*0.5),
					gate: gate, levelScale: mstrLev, doneAction: 2
				);
				// compute looplength,grainDur,clk
				looplength=(end-strt).abs;
				chngTrg = Changed.kr(looplength); // trg on change of length
				grainDur = (looplength*BufDur.kr(bufNum)*(rate.reciprocal)).max(minGrainDur);
				clk = Impulse.kr(grainDur.reciprocal);  // trigger grain env
				// update loop strt and end before every loop, and also if looplength changes
				#strt,end = Demand.kr(clk+chngTrg,0,[strt,end]);
				//  sample rate combines rate-modulators, direction, rate
				sineLFO = SinOsc.kr(sinfrq,0,sinAmp);
				sawLFO = LFSaw.kr(sawfrq,sawPhs,sawAmp);
				plsLFO = LFPulse.kr(plsfrq,plsPhs,plsWdth,plsAmp);
				dir = (end-strt).sign;
				dir = (dir.abs>0).if(dir,1);   // if end-strt == 0, then dir = 1 (not 0)
				thisRate = ((BufRateScale.kr(bufNum)*rate)+(sineLFO+sawLFO+plsLFO))*dir;
				// loop env
				env = EnvGen.ar(Env.new([0,1,1,0],
					[minGrainDur*0.5,grainDur-minGrainDur,minGrainDur*0.5],
					\welch),clk+chngTrg);  // also triggers if loop-length changes
				sampler = PlayBuf.ar(1,bufNum,thisRate,clk,strt*BufFrames.kr(bufNum),1)
				*env*gateEnvGen;
				Out.ar(recBus,sampler*sigAmp);
				Out.ar(effBus,sampler*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(sampler*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			/* recBufSample same as granSampler but has 'att' and 'rls' args to smooth onset and release of gateEnv, and no sineLFO */

			SynthDef("recBufSampler",{ arg rate=1,strt=0,end=0,att=0.05,rls=0.1,sawfrq=1,
				sawPhs=0,sawAmp=0.1,plsfrq=2,plsPhs=0,plsWdth=0.1,plsAmp=0.1,
				recBuf,effAmp=0,pan=0,sigAmp=0.1,gate=1,bufNum,recBus,effBus,vol,elev=0;
				var dir, thisRate, grainDur,clk,env,gateEnvGen,sampler,looplength=1,
				chngTrg,plsLFO, sawLFO,attRelTime=0.0125,minGrainDur=0.005,w,x,y,z;
				gateEnvGen = EnvGen.kr(
					Env.asr(att,1.0,rls),
					gate: gate, 	// gate
					levelScale: mstrLev, doneAction: 2
				);
				// compute looplength,grainDur,clk
				looplength=(end-strt).abs;
				chngTrg = Changed.kr(looplength); // trg on change of length
				grainDur = (looplength*BufDur.kr(bufNum)*(rate.reciprocal)).max(minGrainDur);
				clk = Impulse.kr(grainDur.reciprocal);  // trigger grain env
				// update loop strt and end before every loop, and also if looplength changes
				#strt,end = Demand.kr(clk+chngTrg,0,[strt,end]);
				//  sample rate combines rate-modulators, direction, rate
				sawLFO = LFSaw.kr(sawfrq,sawPhs,sawAmp);
				plsLFO = LFPulse.kr(plsfrq,plsPhs,plsWdth,plsAmp);
				dir = (end-strt).sign;
				dir = (dir.abs>0).if(dir,1);   // if end-strt == 0, then dir = 1 (not 0)
				thisRate = ((BufRateScale.kr(bufNum)*rate)+(sawLFO+plsLFO))*dir;
				// loop env
				env = EnvGen.ar(Env.new([0,1,1,0],
					[minGrainDur*0.5,grainDur-minGrainDur,minGrainDur*0.5],
					\welch),clk+chngTrg);  // also triggers if loop-length changes
				sampler = PlayBuf.ar(1,bufNum,thisRate,clk,strt*BufFrames.kr(bufNum),1)
				*env*gateEnvGen;
				Out.ar(recBus,sampler*sigAmp);
				Out.ar(effBus,sampler*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(sampler*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			// rand choice of loop points for each loop within chosen strt and end boundaries
			SynthDef("randGranSampler",{ arg rate=1,strt=0,end=0, sinfrq=2, sinAmp=0.1,
				sawfrq=1, sawPhs=0,sawAmp=0.1, plsfrq=2,plsPhs=0,plsWdth=0.1,plsAmp=0.1,
				sampBuf,effAmp=0,pan=0, sigAmp=0.1, gate=1, bufNum,recBus,effBus,vol,elev=0;
				var dir, thisRate, thisDur, loopFrq,env,gateEnvGen,sampler,plsLFO,sineLFO,
				sawLFO, attRelTime=0.0125,rstrt,rand,w,x,y,z;
				gateEnvGen = EnvGen.kr(
					Env.asr(attackTime: attRelTime/2, releaseTime: attRelTime/2),
					gate: gate, 	// gate
					levelScale: mstrLev, doneAction: 2
				);
				sineLFO = SinOsc.kr(sinfrq,0,sinAmp);
				sawLFO = LFSaw.kr(sawfrq,sawPhs,sawAmp);
				plsLFO = LFPulse.kr(plsfrq,plsPhs,plsWdth,plsAmp);
				dir = (end-strt).sign;
				dir = (dir.abs>0).if(dir,1);   // if end-strt == 0, then dir = 1 (not 0)
				thisDur = ((end-strt).abs*BufDur.kr(bufNum)*(rate.reciprocal))
				.max(attRelTime);
				rand = TRand.kr(strt,end,Impulse.kr(thisDur.reciprocal));
				thisRate = BufRateScale.kr(bufNum)*rate+(sineLFO+sawLFO+plsLFO)*dir;
				thisDur = ((end-rand).abs*BufDur.kr(bufNum)).max(0.05);
				loopFrq = thisDur.reciprocal;
				env = Env.new([0,1,1,0],[0.006,1.0-0.025,0.019],\welch);
				sampler = PlayBuf.ar(1,bufNum,thisRate, Impulse.kr(loopFrq),
					rand*BufFrames.kr(bufNum),1)
				*EnvGen.ar(env,Impulse.kr(loopFrq),timeScale: thisDur.max(0.01));
				Out.ar(recBus,sampler*gateEnvGen*sigAmp);
				Out.ar(effBus,sampler*gateEnvGen*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(sampler*gateEnvGen*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef("randTransSampler",{ arg rate=1,strt=0,end=0, sinfrq=2, sinAmp=0.1,
				sawfrq=1, sawPhs=0,sawAmp=0.1, plsfrq=2,plsPhs=0,plsWdth=0.1,plsAmp=0.1,
				sampBuf,effAmp=0,pan=0, sigAmp=0.1, gate=1, bufNum,recBus,effBus,vol,elev=0;
				var dir,thisRate,thisDur,loopFrq,env,gateEnvGen,sampler,plsLFO,sineLFO,sawLFO,
				attRelTime=0.0125,rstrt,rand,w,x,y,z;
				gateEnvGen = EnvGen.kr(
					Env.asr(attackTime: attRelTime/2, releaseTime: attRelTime/2),
					gate: gate, 	// gate
					levelScale: mstrLev, doneAction: 2
				);
				sineLFO = SinOsc.kr(sinfrq,0,sinAmp);
				sawLFO = LFSaw.kr(sawfrq,sawPhs,sawAmp);
				plsLFO = LFPulse.kr(plsfrq,plsPhs,plsWdth,plsAmp);
				dir = (end-strt).sign;
				dir = (dir.abs>0).if(dir,1);   // if end-strt == 0, then dir = 1 (not 0)
				thisDur = ((end-strt).abs*BufDur.kr(bufNum)).max(attRelTime);
				rand = TRand.kr(strt,end,Impulse.kr(thisDur.reciprocal));
				thisRate = BufRateScale.kr(bufNum)*rate+(sineLFO+sawLFO+plsLFO)*dir;
				thisDur = ((end-strt).abs*BufDur.kr(bufNum)*(rate.reciprocal))
				.max(attRelTime);
				loopFrq = thisDur.reciprocal;
				env = Env.new([0,1,1,0],[0.0125,1.0-0.025,0.0125],\welch);
				sampler = PlayBuf.ar(1,bufNum,thisRate,Impulse.kr(loopFrq),
					rand*BufFrames.kr(bufNum)
					,1)*
				// an EnvGen is 'gated' at the same rate as the sample is looped
				EnvGen.ar(env,Impulse.kr(loopFrq),timeScale: thisDur.max(0.01));
				Out.ar(recBus,sampler*gateEnvGen*sigAmp);
				Out.ar(effBus,sampler*gateEnvGen*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(sampler*gateEnvGen*
					(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef("randGranBufSamp",{ arg rate=1,strt=0,end=0, sinfrq=2, sinAmp=0.1,
				sawfrq=1, sawPhs=0,sawAmp=0.1, plsfrq=2,plsPhs=0,plsWdth=0.1,plsAmp=0.1,
				recBuf,effAmp=0,pan=0, sigAmp=0.1, gate=1, bufNum,recBus,effBus,vol,elev=0;
				var dir, thisRate, thisDur, loopFrq, env, gateEnvGen, sampler,
				plsLFO, sineLFO, sawLFO, attRelTime=0.05,rstrt,rand,w,x,y,z;
				gateEnvGen = EnvGen.kr(
					Env.asr(attackTime: attRelTime/2, releaseTime: attRelTime/2),
					gate: gate, 	// gate
					levelScale: mstrLev, doneAction: 2
				);
				sineLFO = SinOsc.kr(sinfrq,0,sinAmp);
				sawLFO = LFSaw.kr(sawfrq,sawPhs,sawAmp);
				plsLFO = LFPulse.kr(plsfrq,plsPhs,plsWdth,plsAmp);
				dir = (end-strt).sign;
				thisDur = ((end-strt).abs*BufDur.kr(bufNum)).max(attRelTime);
				rand =TRand.kr(strt,end,Impulse.kr(thisDur.reciprocal));
				thisRate = BufRateScale.kr(bufNum)*rate+(sineLFO+sawLFO+plsLFO)*dir;
				thisDur = ((end-strt).abs*BufDur.kr(bufNum)*(rate.reciprocal))
				.max(attRelTime);
				loopFrq = thisDur.reciprocal;
				env = EnvGen.ar(Env.new([0,1,1,0],[0.0025,1.0-0.005,0.0025],\welch),
					Impulse.kr(loopFrq),timeScale: thisDur.max(0.01));
				sampler = PlayBuf.ar(1,bufNum,thisRate,Impulse.kr(loopFrq),
					rand*BufFrames.kr(bufNum),1)
				*env*gateEnvGen;
				Out.ar(recBus,sampler*sigAmp);
				Out.ar(effBus,sampler*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(sampler*(1-effAmp)
					*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef("scratcher",{ arg rate, endRt,strt,end,spd,mfrq1=1,fMod=0, loop=1.0,a9,a10,
				a11, a12, sampBuf,effAmp,pan,sigAmp,gate,vol,recBus,effBus,bufNum,elev=0;
				var src,clk,gateEnvGen,scratchEnv,startPos,endPos,length,trgf,w,x,y,z;
				spd = SinOsc.kr(mfrq1,0,fMod,spd);
				startPos = strt*BufDur.kr(bufNum);
				endPos = end*BufDur.kr(bufNum);
				length = (endPos-startPos).abs.max(0.001)*(rate.reciprocal);
				trgf = length.reciprocal;
				clk = Impulse.kr(trgf)*loop;
				#strt, end = Demand.kr(clk,0,[strt,end]);
				scratchEnv = EnvGen.kr(Env.new([1.0, 1.0, 0.01, 1.0, 1.0],
					[0.01,0.49,0.49, 0.01],\sin,3,0),timeScale: length*((spd).reciprocal),
				levelScale: (rate-endRt).abs, doneAction: 2);
				scratchEnv = ((rate>=endRt)*(scratchEnv+endRt)) + ((endRt>rate)*(endRt-scratchEnv));
				src = PlayBuf.ar(1,bufNum,scratchEnv*BufRateScale.kr(bufNum),
					clk, strt*BufFrames.kr(bufNum),loop, doneAction: 2)
				*EnvGen.ar(Env.new([0,1,1,0],[0.025,1.0-0.05,0.025],\welch),
					Impulse.kr(trgf),timeScale: length);
				gateEnvGen = EnvGen.kr(Env.asr(0.025,1.0,0.05),gate,doneAction:2);
				Out.ar(recBus,src*gateEnvGen*sigAmp);
				Out.ar(effBus,src*effAmp*gateEnvGen*sigAmp);
				#w,x,y,z = BFEncode1.ar(src*gateEnvGen*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w,x,y,z, azArray, elArray))
			}).store,

			SynthDef("tGrainSamp",{ arg rate,strt,end,spd,trgf,gdur,mfrq1=1,fMod=0,
				mfrq2=1,dMod=0, a11, a12,sampBuf,effAmp,pan,sigAmp,gate,vol,
				recBus,effBus,bufNum,elev=0;
				var src,centerPos,clk,gateEnvGen,startPos,endPos,length,sign,
				scanSpd,w,x,y,z,w2,x2,y2,z2;
				gateEnvGen = EnvGen.kr(Env.asr(0.01,1.0,0.025),gate,
					levelScale: mstrLev,doneAction:2);
				trgf =  SinOsc.kr(mfrq1,0,fMod*trgf,trgf);   // modulate trg freq
				gdur = SinOsc.kr(mfrq2,0,dMod*gdur,gdur);   // modulate grain dur
				clk = Impulse.kr(trgf);
				startPos = strt*BufDur.kr(bufNum);
				endPos = end*BufDur.kr(bufNum);
				length = (endPos-startPos);
				sign = length.sign;
				sign = (sign >= 0) + (sign*(sign<0));  // ensure sign != 0
				//  grain centerPos moves thru selection of samp, at spd related to length sel
				scanSpd = spd*((length.abs.max(0.001)).reciprocal);
				centerPos = LFSaw.kr(scanSpd,1,0.5,0.5)*length;
				src = Mix(TGrains.ar(2,clk,bufNum,rate*sign,
					(centerPos+startPos+(gdur*0.5)),gdur,0,1,2))
				*sigAmp*gateEnvGen;
				Out.ar(recBus,src);
				Out.ar(effBus,src*effAmp);
				#w,x,y,z = BFEncode1.ar(src*(1-effAmp)*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w,x,y,z, azArray, elArray))
			}).store,

			// same as tGrainSamp except buffer arg is 'recBuf', instead of 'sampBuf'
			SynthDef("bufTGrainSamp",{ arg rate,strt,end,spd,trgf,gdur,mfrq1=1,fMod=0,
				mfrq2=1,dMod=0,a11,a12,recBuf,effAmp,pan,sigAmp,gate,vol,
				recBus,effBus,bufNum,elev=0;
				var src,centerPos,clk,gateEnvGen,startPos,endPos,length,sign,
				scanSpd,w,x,y,z,w2,x2,y2,z2;
				gateEnvGen = EnvGen.kr(Env.asr(0.01,1.0,0.025),gate,
					levelScale: mstrLev,doneAction:2);
				trgf =  SinOsc.kr(mfrq1,0,fMod*trgf,trgf);   // modulate trg freq
				gdur = SinOsc.kr(mfrq2,0,dMod*gdur,gdur);   // modulate grain dur
				clk = Impulse.kr(trgf);
				startPos = strt*BufDur.kr(bufNum);
				endPos = end*BufDur.kr(bufNum);
				length = (endPos-startPos);
				sign = length.sign;
				sign = (sign >= 0) + (sign*(sign<0));  // ensure sign != 0
				//  grain centerPos moves thru selection of samp, at spd related to length sel
				scanSpd = spd*((length.abs.max(0.001)).reciprocal);
				centerPos = LFSaw.kr(scanSpd,1,0.5,0.5)*length;
				src = Mix(TGrains.ar(2,clk,bufNum,rate*sign,
					(centerPos+startPos+(gdur*0.5)),gdur,0,1,2))
				*sigAmp*gateEnvGen;
				Out.ar(recBus,src);
				Out.ar(effBus,src*effAmp);
				#w,x,y,z = BFEncode1.ar(src*(1-effAmp)*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w,x,y,z, azArray, elArray))
			}).store,

			// tGrainScan has different order and names of grain args than tGrainSamp (otherwise same) !!
			SynthDef("tGrainScan",{ arg rate,strt,end,spd,gdur,gfrq,mfrq1=1,fMod=0,
				mfrq2=1,dMod=0,a11,a12,sampBuf,effAmp,pan,sigAmp,gate,vol,recBus,effBus,bufNum,elev=0;
				var thisPos,thisRate,thisDur,gateEnvGen, granulator,bufDur,scanSpd,sign,w,x,y,z,w2,x2,y2,z2;
				gateEnvGen = EnvGen.kr(Env.asr(attackTime: 0.01, releaseTime: 0.01),gate,
					levelScale: mstrLev,doneAction: 2 );
				bufDur = BufDur.kr(bufNum);
				thisDur = (end*bufDur)-(strt*bufDur); // is signed, or can be 0
				sign = thisDur.sign;
				sign = (sign >= 0) + (sign*(sign<0));
				scanSpd = spd*((thisDur.abs.max(0.01)).reciprocal);
				thisPos = LFSaw.kr(scanSpd,1,0.5,0.5)*thisDur;
				gfrq = SinOsc.kr(mfrq1,0,fMod*gfrq,gfrq);
				gdur = SinOsc.kr(mfrq2,0,dMod*gdur,gdur);
				granulator = Mix(TGrains.ar(2,Impulse.kr(gfrq),bufNum,
					rate*sign,
					(strt*bufDur)+thisPos+(gdur*0.5),  // buffer centerPos in secs
					gdur,0,1,4));
				Out.ar(recBus,granulator*sigAmp*gateEnvGen);
				Out.ar(effBus,granulator*effAmp*sigAmp*gateEnvGen);
				#w,x,y,z = BFEncode1.ar(granulator*gateEnvGen*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w,x,y,z, azArray, elArray))
			}).store,

			SynthDef("tGrainScan2",{ arg rate,strt,end,spd=1,gdur,gfrq,mfrq1=1,fMod=0,
				mfrq2=1,dMod=0,a11,a12,sampBuf,effAmp,pan,sigAmp,gate,vol,recBus,effBus,bufNum,elev=0;
				var thisPos,thisSamp,gateEnv,grainEnv,sig,bufDur,bufFrames,w,x,y,z,w2,x2,y2,z2;
				gateEnv = EnvGen.kr(Env.asr(attackTime: 0.01, releaseTime: 0.01),gate,
					levelScale: mstrLev,doneAction: 2 );
				bufFrames = BufFrames.kr(bufNum);
				thisSamp  = (end-strt)*bufFrames; // is signed, or can be 0
				rate = rate*BufRateScale.kr(bufNum);
				// need TWO phasors -- one for grainEnv and one for moving the grain by spd
				thisPos = Phasor.ar(Impulse.kr(spd),rate.reciprocal,strt*bufFrames,thisSamp);
				grainEnv = EnvGen.kr(Env.new([0,1,0],[0.5,0.5],\welch),
					Impulse.kr(gfrq), timeScale: gdur);
				sig = BufRd.ar(1,bufNum,thisPos)*grainEnv*gateEnv;
				Out.ar(recBus,sig*sigAmp);
				Out.ar(effBus,sig*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(sig*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w,x,y,z, azArray, elArray))
			}).store,

			SynthDef("bufTGrainScan",{ arg rate,strt,end,spd,gdur,gfrq,att,rls,
				mfrq1=1,fMod=0,mfrq2=1,dMod=0,recBuf,effAmp,pan,sigAmp,
				gate,vol,recBus,effBus,bufNum,elev=0;
				var thisPos,thisRate,thisDur,gateEnvGen, granulator,bufDur,scanSpd,sign,w,x,y,z,w2,x2,y2,z2;
				gateEnvGen = EnvGen.kr(Env.asr(attackTime: 0.01, releaseTime: 0.01),gate,
					levelScale: mstrLev,doneAction: 2 );
				bufDur = BufDur.kr(bufNum);
				thisDur = (end*bufDur)-(strt*bufDur); // is signed, or can be 0
				sign = thisDur.sign;
				sign = (sign >= 0) + (sign*(sign<0));
				scanSpd = spd*((thisDur.abs.max(0.01)).reciprocal);
				thisPos = LFSaw.kr(scanSpd,1,0.5,0.5)*thisDur;
				gfrq = SinOsc.kr(mfrq1,0,fMod*gfrq,gfrq);
				gdur = SinOsc.kr(mfrq2,0,dMod*gdur,gdur);
				granulator = Mix(TGrains.ar(2,Impulse.kr(gfrq),bufNum,
					rate*sign,
					(strt*bufDur)+thisPos+(gdur*0.5),  // buffer centerPos in secs
					gdur,0,1,4));
				Out.ar(recBus,granulator*sigAmp*gateEnvGen);
				Out.ar(effBus,granulator*effAmp*sigAmp*gateEnvGen);
				#w,x,y,z = BFEncode1.ar(granulator*gateEnvGen*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w,x,y,z, azArray, elArray))
			}).store,

			SynthDef("recCircBuf",{ arg recBuf, gate=1, effIn, effInVol=1, bufNum;
				var envelope, in, phasor;
				envelope = EnvGen.kr(Env.asr(0.05, effInVol, 0.05), gate: gate, doneAction: 2);
				in = In.ar(effIn)*envelope;
				phasor = Phasor.ar(0, BufRateScale.kr(bufNum), 0, BufFrames.kr(bufNum));
				BufWr.ar(in,bufNum,phasor); 0.0
			}).store,


			SynthDef("04recCircBuf",{ arg recBuf=0, busIn=0, busCtl=0, gate=1, volCtl=0;
				var envelope, in, vol;
				vol = In.kr(volCtl);
				envelope = EnvGen.kr(Env.asr(0.05, 1, 0.05), gate: gate, doneAction: 2);
				in = In.ar(busIn)*envelope;
				BufWr.ar(in,recBuf,
					Phasor.ar(BufFrames.kr(recBuf).rand, 	// random rec start point
						BufRateScale.kr(recBuf), 0, BufFrames.kr(recBuf)),0);
				0.0
			}).store;

			// reads sound from effIn bus, gate in new input over threshold, save old sound if under thresh
			SynthDef("bufInfRec",{ arg recBuf, threshold, gate=1, effIn, effInVol=1, bufNum;
				var envelope, input, inputAmp, chanSelect, recycle,phasor;
				envelope = EnvGen.kr(Env.asr(0.05, effInVol, 0.05), gate: gate, doneAction: 2);
				input = In.ar(effIn)*envelope;
				phasor = Phasor.ar(0, BufRateScale.kr(bufNum), 0, BufFrames.kr(bufNum));
				recycle = BufRd.ar(1,bufNum,phasor);
				inputAmp = Amplitude.kr(input,releaseTime: 0.25);
				chanSelect = Lag.kr(inputAmp > threshold, 0.1)>0;
				input = (input * chanSelect) + (recycle * (1 - chanSelect));
				BufWr.ar(input,bufNum,phasor); 0
			}).store,

			// reads sound from audio input
			SynthDef("bufInRec",{ arg in=1,recBuf,threshold=0.05, gate=1, effIn, effInVol=1, bufNum;
				var envelope, input, inputAmp, chanSelect, recycle,phasor;
				envelope = EnvGen.kr(Env.asr(0.05, effInVol, 0.05), gate: gate, doneAction: 2);
				input = SoundIn.ar(in-1)*envelope;
				phasor = Phasor.ar(0, BufRateScale.kr(bufNum), 0, BufFrames.kr(bufNum));
				recycle = BufRd.ar(1,bufNum,phasor);
				inputAmp = Amplitude.kr(input,releaseTime: 0.25);
				chanSelect = Lag.kr(inputAmp > threshold, 0.1)>0;
				input = (input * chanSelect) + (recycle * (1 - chanSelect));
				BufWr.ar(input,bufNum,phasor); 0
			}).store,

			// plays and records from audioInput, record gates in new input over threshold, saves old sound if under thresh
			SynthDef("aInfBufRec",{ arg in=1, recBuf, threshold, a4, a5, a6, a7, a8, a9, a10,
				a11, a12, a13, effAmp, pan, sigAmp, gate=1, vol, effBus, bufNum, elev=0;
				var envelope, input, inputAmp, bufInput, chanSelect, recycle,phasor,w,x,y,z;
				envelope = EnvGen.kr(Env.asr(0.05, 1, 0.05), gate: gate, doneAction: 2);
				input = SoundIn.ar(in-1)*envelope;
				phasor = Phasor.ar(0, BufRateScale.kr(bufNum), 0, BufFrames.kr(bufNum));
				recycle = BufRd.ar(1,bufNum,phasor);
				inputAmp = Amplitude.kr(input,releaseTime: 0.25);
				chanSelect = Lag.kr(inputAmp > threshold, 0.1)>0;
				bufInput = (input * chanSelect) + (recycle * (1 - chanSelect));
				BufWr.ar(bufInput,bufNum,phasor);
				Out.ar(effBus,input*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(input*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			// plays and records from audioInput, with circular buffer
			SynthDef("aInCircBufRec",{ arg in=1,recBuf,threshold=0.05,a4,a5,a6,a7,a8,a9,
				a10,a11,a12,a13,effAmp,pan,sigAmp,gate=1,vol,effBus,bufNum,elev=0;
				var envelope, input, phasor,w,x,y,z;
				envelope = EnvGen.kr(Env.asr(0.05, 1, 0.05), gate: gate, doneAction: 2);
				input = SoundIn.ar(in-1)*envelope;
				phasor = Phasor.ar(0, BufRateScale.kr(bufNum), 0, BufFrames.kr(bufNum));
				BufWr.ar(input,bufNum,phasor);
				Out.ar(effBus,input*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(input*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef("busInfRec",{ arg bus=0,recBuf,threshold=0.05,gate=1,recBus,bufNum=0;
				var envelope, input, inputAmp, chanSelect, recycle,phasor;
				envelope = EnvGen.kr(Env.asr(0.05, 4, 0.05), gate: gate, doneAction: 2);
				input = In.ar(recBus)*envelope;
				phasor = Phasor.ar(0, BufRateScale.kr(bufNum), 0, BufFrames.kr(bufNum));
				recycle = BufRd.ar(1,bufNum,phasor);
				inputAmp = Amplitude.kr(input,releaseTime: 0.25);
				chanSelect = Lag.kr(inputAmp > threshold, 0.1)>0;
				input = (input * chanSelect) + (recycle * (1 - chanSelect));
				BufWr.ar(input,bufNum,phasor); 0
			}).store,

			SynthDef("busCircRec",{ arg bus=0,recBuf,threshold=0.05, gate=1, bufNum;
				var envelope, input, phasor;
				envelope = EnvGen.kr(Env.asr(0.05, 4, 0.05), gate: gate, doneAction: 2);
				input = In.ar(bus)*envelope;
				phasor = Phasor.ar(0, BufRateScale.kr(bufNum), 0, BufFrames.kr(bufNum));
				BufWr.ar(input,bufNum,phasor); 0
			}).store,

			SynthDef("motorev", { arg lfrq,fvar,fmul,wdth,ffrq,clip,sawfrq,sawPhs,sawAmp,
				plsfrq,phs,plsWdth,plsAmp,effAmp,pan,sigAmp,gate,recBus,effBus,vol,elev=0;
				var env,sig,lfo,attRelTime=0.05,w,x,y,z,w2,x2,y2,z2;
				env = EnvGen.kr(Env.asr(attackTime: attRelTime/2, releaseTime: attRelTime/2),
					gate,  levelScale: mstrLev, doneAction:2);
				lfo = LFSaw.kr(sawfrq,sawPhs,sawAmp*0.5,1)+
				LFPulse.kr(plsfrq,phs,plsAmp,plsAmp*0.5,1);
				sig = Mix(RLPF.ar(LFPulse.ar(
					SinOsc.kr(fvar,0,fmul,lfrq*lfo) // freq
					,[0,0.1],wdth), 	// iphase, width
				ffrq,0.1).clip2(clip));  // filt freq, phase, clip
				Out.ar(recBus,sig*env*sigAmp);
				Out.ar(effBus,sig*effAmp*env*sigAmp);
				#w,x,y,z = BFEncode1.ar(sig*env*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef("fmDrum",{ arg frq,rtio,idx,lfrq,mod,trgf,phs,att,rls,a9,a10,a11,a12,
				effAmp,pan,sigAmp,gate,recBus,effBus,vol,elev=0;
				var modSig,env,sig,attRelTime=0.05,w,x,y,z;
				env = EnvGen.kr(Env.asr(attackTime: attRelTime/2, releaseTime: attRelTime/2),
					gate, levelScale: mstrLev,doneAction: 2);
				modSig = SinOsc.kr(lfrq,0,mod*frq*0.5);
				sig = Decay2.ar(Impulse.ar(trgf,phs),att,rls,
					PMOsc.ar(frq+modSig,frq*rtio,idx)
				)*env;
				Out.ar(recBus,sig*sigAmp);
				Out.ar(effBus,sig*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(sig*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef(\pmTone,{ arg frq,rtio,idx,lfrq,mod,
				a6,a7,a8,a9,a10,a11,a12,a13,effAmp,pan,sigAmp,gate,recBus,effBus,vol,elev=0;
				var sig, modSig, env, modFreq,w,x,y,z;
				env = EnvGen.kr(Env.asr(0.05, 1, 0.2), gate,  levelScale: mstrLev,  doneAction: 2 );
				modFreq = frq*rtio;
				sig = PMOsc.ar(frq+SinOsc.kr(lfrq,0,mod*frq*0.1),modFreq,idx,0)*env;
				Out.ar(recBus,sig*sigAmp);
				Out.ar(effBus,sig*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(sig*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef(\pmToneDecay,{ arg frq,rtio,idx,lfrq,mod,
				a6,a7,a8,a9,a10,a11,a12,a13,effAmp,pan,sigAmp,gate,recBus,effBus,vol,elev=0;
				var sig, modSig, env, modFreq,w,x,y,z;
				env = EnvGen.kr(Env.perc(0.05, 0.1, 0.25, -8), gate,  levelScale: mstrLev, doneAction: 2 );
				modFreq = frq*rtio;
				sig = PMOsc.ar(frq+SinOsc.kr(lfrq,0,mod*frq*0.1),modFreq,idx,0)*env;
				Out.ar(recBus,sig*sigAmp);
				Out.ar(effBus,sig*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(sig*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			// SoundIn on first audio input
			SynthDef("audioIn",{ arg effAmp,pan,sigAmp,gate,recBus,effBus,vol,elev=0;
				var sig,env,w,x,y,z;
				env = EnvGen.kr(Env.asr(attackTime: 0.2, releaseTime: 0.4),
					gate, levelScale: sigAmp, doneAction:2);
				sig = SoundIn.ar(0)*env;
				Out.ar(recBus,sig);
				Out.ar(effBus,sig*effAmp);
				#w,x,y,z = BFEncode1.ar(sig*(1-effAmp)*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef("audioInX",{ arg in,a2,a3,a4,a5,a6,a7,a8,a9,a10,a11,a12,a13,effAmp,
				pan,sigAmp,gate,recBus,effBus,vol,elev=0;
				var envelope,w,x,y,z;
				// input level is *4
				envelope = EnvGen.kr(Env.asr(0.1,sigAmp, 0.1),	gate,doneAction:2, levelScale: 4);
				Out.ar(recBus,SoundIn.ar(in-1)*envelope*sigAmp);
				Out.ar(effBus,SoundIn.ar(in-1)*envelope*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(SoundIn.ar(in-1)*envelope*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef("aInBufRec",{ arg in,buf,a3,a4,a5,a6,a7,a8,a9,a10,a11,a12,a13,effAmp,
				pan,sigAmp,gate,recBus,effBus,vol,elev=0;
				var input,envelope,w,x,y,z;
				input = SoundIn.ar(in-1);
				envelope = EnvGen.kr(Env.asr(attackTime: 0.2, releaseTime: 0.4),
					gate,doneAction:2, levelScale: 4);
				Out.ar(recBus,input*envelope);
				Out.ar(effBus,input*envelope*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(input*envelope*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef("blipModOsc",{ arg frq,trSpd,henv,hrm1,hrm2,hrm3,clip,
				a8,a9,a10,a11,a12,a13,effAmp,pan,sigAmp,gate,recBus,effBus,vol,elev=0;
				var sig, fEnv, hEnv, aEnv,w,x,y,z;
				hEnv = Env.new([hrm1,hrm1,hrm2,hrm3,hrm3],
					[0.001,henv,1.0-henv,0.001],-4,3,0);
				aEnv = Env.asr(0.01,1,0.1);
				sig = Blip.ar(frq,EnvGen.kr(hEnv,timeScale: 1/trSpd),
					EnvGen.kr(aEnv,gate, levelScale: mstrLev,doneAction:2))
				.clip2(clip)*vol;
				Out.ar(recBus,sig*sigAmp);
				Out.ar(effBus,sig*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(sig*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef("fltTrgOsc",{ arg spd,ring,duty,ffrq1,ffrq2,ffrq3,clip,a8,a9,a10,
				a11,a12,a13,effAmp,pan,sigAmp,gate,recBus,effBus,vol,elev=0;
				var sig, fEnv, aEnv,trig,w,x,y,z;
				fEnv = Env.new([ffrq1,ffrq1,ffrq2,ffrq3,ffrq3],
					[0.001,duty,1.0-duty,0.001],-4,3,0);
				aEnv = Env.asr(0.01,1,0.1);
				trig = Impulse.ar(spd);
				sig = Ringz.ar(Decay2.ar(trig),
					EnvGen.kr(fEnv,timeScale: 1/spd),
					ring,	// ringtime
					EnvGen.kr(aEnv,gate, levelScale: mstrLev,doneAction:2))
				.clip2(clip);
				Out.ar(recBus,sig*sigAmp);
				Out.ar(effBus,sig*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(sig*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			// this one tends to explode on fast frq changes -- Lag(frq) makes it safe(r)
			SynthDef(\klank,{ arg frq,spd,mod,ring=2,fMlt1,fMlt2,fMlt3,fMlt4,att,rls,
				a11,a12,a13,effAmp,pan,sigAmp,gate,recBus,effBus,vol,elev=0;
				var ringDur,sig,env,w,x,y,z;
				ringDur=(spd.reciprocal)*ring;
				frq = Lag.kr(frq,0.1);
				env = EnvGen.kr(Env.asr(att,1.0,rls,\welch),
					gate,levelScale: mstrLev, doneAction:2);
				sig = Mix(Ringz.ar(
					LFNoise2.ar(frq*(mod+1)*2)*Impulse.kr(spd,mul:0.01),
					[frq*fMlt1,frq*fMlt2,frq*fMlt3,frq*fMlt4],
					ringDur,(1-(\frq.asSpec.unmap(frq*0.5))).max(0.2))*env);
				Out.ar(recBus,sig*sigAmp);
				Out.ar(effBus,sig*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(sig*(1-effAmp)*sigAmp*vol,
					LFSaw.kr(pan,Rand(0,2))*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store;

			SynthDef(\pluckedString,{ arg frq,plSpd,brit,mfrq,mod,plDcy,clip,rq,
				a8,a9,a10,a11,a12,effAmp,pan,sigAmp,gate,recBus,effBus,vol,elev=0;
				var exciter,modSig,sig,bright,w,x,y,z;
				bright = brit+frq;
				exciter = PinkNoise.ar(Decay.kr(Impulse.kr(plSpd), 0.01));
				modSig = SinOsc.kr(mfrq,0.0,mod*(bright*0.5));
				sig = RLPF.ar(CombC.ar(exciter,0.1,frq.reciprocal,plDcy),
					bright+modSig,rq,
					EnvGen.kr(Env.asr(0.0025,1,0.05),gate, levelScale: mstrLev,doneAction: 2))
				.clip2(clip)*4;
				Out.ar(recBus,sig*sigAmp);
				Out.ar(effBus,sig*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(sig*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray))

			}).store,

			SynthDef(\pluckedStringW,{ arg frq,plSpd,brit,mfrq,mod,plDcy,clip,rq,
				a8,a9,a10,a11,a12,effAmp,pan,sigAmp,gate,recBus,effBus,vol,elev=0;
				var exciter,modSig,sig,bright,w,x,y,z;
				bright = brit+frq;
				exciter = WhiteNoise.ar(Decay.kr(Impulse.kr(plSpd), 0.01));
				modSig = SinOsc.kr(mfrq,0.0,mod*(bright*0.5));
				sig = RLPF.ar(CombC.ar(exciter,0.1,frq.reciprocal,plDcy),
					bright+modSig,rq,
					EnvGen.kr(Env.asr(0.0025,1,0.05),gate, levelScale: mstrLev,doneAction: 2))
				.clip2(clip);
				Out.ar(recBus,sig*sigAmp);
				Out.ar(effBus,sig*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(sig*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef(\nseBurst,{ arg freq,plSpd,fb,mfrq,mod,clip,
				a7,a8,a9,a10,a11,a12,a13,effAmp,pan,sigAmp,gate=1,recBus,effBus,vol,elev=0;
				var sig,imp,env,dly,w,x,y,z;
				dly = freq.reciprocal*100;
				imp= Decay2.ar(Impulse.ar(plSpd),
					0.001,dly,
					LFNoise2.ar(freq*SinOsc.kr(mfrq,0,mod,1)).distort);
				env = EnvGen.kr(Env.asr(0.01,1.0, 0.2),gate, levelScale: mstrLev,doneAction:2);
				sig = CombC.ar(imp,1.0,dly.max(0.001),fb*5,env).clip2(clip);
				Out.ar(recBus,sig*sigAmp);
				Out.ar(effBus,sig*sigAmp*effAmp);
				#w,x,y,z = BFEncode1.ar(sig*env*(1-effAmp)*sigAmp*vol,
					LFSaw.kr(pan,Rand(0,2))*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store;

			SynthDef(\nseFlt,{ arg freq,plSpd,rq,mfrq,mod,att,rls,duty,
				a9,a10,a11,a12,a13,effAmp,pan,sigAmp,gate=1,recBus,effBus,vol,elev=0;
				var sig,imp,env,dly,w,x,y,z;
				dly = (freq.min(12000.0)).reciprocal*100;
				imp= GrayNoise.ar(EnvGen.kr(Env.asr(att,1.0,rls),LFPulse.kr(plSpd,0,duty)));
				env = EnvGen.kr(Env.asr(0.01,1.0, 0.2),gate, levelScale: mstrLev,doneAction:2);
				sig = RLPF.ar(imp,freq*(SinOsc.kr(mfrq,0,mod,1)),rq.min(1.0),env);
				sig=Compander.ar(sig,sig,0.25,1,0.5,0.01,0.01);
				Out.ar(recBus,sig*sigAmp);
				Out.ar(effBus,sig*sigAmp*effAmp);
				#w,x,y,z = BFEncode1.ar(sig*env*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef(\sinGran,{ arg frq,rng,gfrq,dnse,
				a5,a6,a7,a8,a9,a10,a11,a12,a13,effAmp,pan,sigAmp,gate=1,recBus,effBus,vol,elev=0;
				var sig,env,trig,dur,frqRng,w,x,y,z;
				env = EnvGen.kr(Env.asr(0.1,1.0,0.2),gate, levelScale: mstrLev,doneAction:2);
				dur = (gfrq.reciprocal);
				frqRng=((frq*rng*0.5)-(frq*rng))*2;
				sig = Mix.fill(8,{|i|
					SinOsc.ar(LFNoise2.kr(gfrq,frqRng,frq),0,
						EnvGen.ar(Env.sine(dur*dnse,dnse.reciprocal*0.25),
							gate: Impulse.kr(gfrq*0.125,i*0.125)))})*4;
				sig=Compander.ar(sig,sig,0.25,1,0.5,0.01,0.01)*env;
				Out.ar(recBus,sig*sigAmp);
				Out.ar(effBus,sig*sigAmp*effAmp);
				#w,x,y,z = BFEncode1.ar(sig*env*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef(\nseGran,{ arg freq,rng,gfrq,dnse,rq,
				a6,a7,a8,a9,a10,a11,a12,a13,effAmp,pan,sigAmp,gate=1,recBus,effBus,vol,elev=0;
				var sig,env,trig,dur,frqRng,w,x,y,z;
				env = EnvGen.kr(Env.asr(0.1,1.0,0.2),gate, levelScale: mstrLev,doneAction:2);
				dur = (gfrq.reciprocal);
				frqRng=((freq*rng*0.5)-(freq*rng))*2;
				sig = Mix.fill(8,{|i|
					RLPF.ar(WhiteNoise.ar(EnvGen.ar(Env.sine(dur*dnse,dnse.reciprocal*0.25),
						gate: Impulse.kr(gfrq*0.125,i*0.125))),
					LFNoise0.kr(gfrq,frqRng,freq),rq)
				});
				sig=Compander.ar(sig,sig,0.25,1,0.5,0.01,0.01)*env;
				Out.ar(recBus,sig*sigAmp);
				Out.ar(effBus,sig*sigAmp*effAmp);
				#w,x,y,z = BFEncode1.ar(sig*env*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef(\fmGran,{ arg frq,rng,gfrq,dnse,ffrq,mrng,
				a7,a8,a9,a10,a11,a12,a13,effAmp,pSpd,sigAmp,gate=1,recBus,effBus,vol,elev=0;
				var sig,env,trig,dur,frqRng,mRng,randDir,randPos,w,x,y,z;
				env = EnvGen.kr(Env.asr(0.05,1.0, 0.05),gate, levelScale: mstrLev,doneAction:2);
				randDir=IRand(0,1);
				randPos=Rand(0,2);
				pSpd=(pSpd*randDir)+(pSpd*(randDir-1));
				dur = (gfrq.reciprocal);
				mRng=((ffrq*mrng*0.5)-(ffrq*mrng))*2;
				frqRng=((frq*rng*0.5)-(frq*rng))*2;
				sig = Mix.fill(8,{|i|
					PMOsc.ar(LFNoise2.kr(gfrq,frqRng,frq),
						LFNoise2.kr(gfrq,mRng,ffrq),
						LFNoise2.kr(gfrq*0.1,20.0,0.1),0,
						EnvGen.ar(Env.sine(dur*dnse,(dnse.max(1.0).dbamp.reciprocal)*0.5),
							gate: Impulse.kr(gfrq*0.125,i*0.125)))})*env;
				Out.ar(recBus,sig*sigAmp);
				Out.ar(effBus,sig*sigAmp*effAmp);

				#w,x,y,z = BFEncode1.ar(sig*env*(1-effAmp)*sigAmp*vol,
					LFSaw.kr(pSpd,Rand(0,2))*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef(\lofmGrn,{ arg frq,rng,gfrq,dnse,mfrq2,mrng,
				a7,a8,a9,a10,a11,a12,a13,effAmp,pSpd,sigAmp,gate=1,recBus,effBus,vol,elev=0;
				var sig,env,trig,dur,frqRng,mRng,randDir,randPos,w,x,y,z;
				env = EnvGen.kr(Env.asr(0.05,1.0, 0.05),gate, levelScale: mstrLev,doneAction:2);
				randDir=IRand(0,1);
				randPos=Rand(0,2);
				pSpd=(pSpd*randDir)+(pSpd*(randDir-1));
				dur = (gfrq.reciprocal);
				mRng=((mfrq2*mrng*0.5)-(mfrq2*mrng))*2;
				frqRng=((frq*rng*0.5)-(frq*rng))*2;
				sig = Mix.fill(8,{|i|
					PMOsc.ar(LFNoise2.kr(gfrq,frqRng,frq),
						LFNoise2.kr(gfrq,mRng,mfrq2),
						LFNoise2.kr(gfrq*0.1,20.0,0.1),0,
						EnvGen.ar(Env.sine(dur*dnse,(dnse.max(1.0).dbamp.reciprocal)*0.5),
							gate: Impulse.kr(gfrq*0.125,i*0.125)))})*env;
				Out.ar(recBus,sig*sigAmp);
				Out.ar(effBus,sig*sigAmp*effAmp);
				#w,x,y,z = BFEncode1.ar(sig*env*(1-effAmp)*sigAmp*vol,
					LFSaw.kr(pSpd,Rand(0,2))*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef("envWah",{ arg ffrq,dcy,mfrq,mDpt,aDpt,a6,pan,effAmp,
				a9,a10,a11,a12,a13,a14,a15,a16,gate=1,effIn,effInVol=1,vol,elev=0;
				var in,flt,maxFreq=4000,w,x,y,z;
				in = In.ar(effIn)*effInVol;
				flt = Ringz.ar(
					in*((dcy*250).reciprocal),
					SinOsc.kr(mfrq,		// lfoFreq
						0,mDpt*maxFreq,			// lfoDepth
						ffrq+			// centerFreq
						(Amplitude.kr(in,0.01,0.5)
							*aDpt*maxFreq)),		// ampDepth
					dcy							// decaytime
				)*effAmp*EnvGen.kr(Env.asr(0.25,1.0,0.5),gate:gate,
					doneAction: 2);
				#w,x,y,z = BFEncode1.ar(flt*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,

			SynthDef("2envWahs", { arg ffrq1,dcy1,mfrq1,mDpt1,aDpt1,a6,a7,vol1,
				ffrq2, dcy2, mfrq2, mDpt2, aDpt2,wdth,pan,vol2,gate=1, effIn, effInVol=1, vol,elev=0;
				var fltPair,in, maxFreq=4000,w,x,y,z,w2,x2,y2,z2;
				in = In.ar(effIn)*effInVol;
				fltPair = [
					Ringz.ar(
						in*((dcy1*250).reciprocal),
						SinOsc.kr(mfrq1,		// lfoFreq
							0,mDpt1*maxFreq,			// lfoDepth
							ffrq1+			// centerFreq
							(Amplitude.kr(in,0.01,0.5)
								*aDpt1*maxFreq)),		// ampDepth
						dcy1							// decaytime
					)*vol1,						// volume
					Ringz.ar(
						in*((dcy2*250).reciprocal),
						SinOsc.kr(mfrq2,		// lfoFreq
							0,mDpt2*maxFreq,			// lfoDepth
							ffrq2+			// centerFreq
							(Amplitude.kr(in,0.01,0.5)
								*aDpt2*maxFreq)),		// ampDepth
						dcy2							// decaytime
					)*vol2
				];
				#w,x,y,z = BFEncode1.ar(fltPair[0]
					*EnvGen.kr(Env.asr(0.25,1.0,0.5),gate:gate, doneAction: 2)*vol,
					pan*panFactor,elev);
				#w2,x2,y2,z2 = BFEncode1.ar(fltPair[1]
					*EnvGen.kr(Env.asr(0.25,1.0,0.5),gate:gate,doneAction: 2)*vol,
					(pan+wdth).wrap(-1.0,1.0)*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
			}).store,

			SynthDef("2cmpEnvWahs", { arg ffrq1,dcy1,mfrq1,mDpt1,aDpt1,thrsh,slp1,vol1,
				ffrq2, dcy2, mfrq2, mDpt2, aDpt2,pan,slp2,vol2,gate=1,effIn,effInVol=1, vol,elev;
				var fltPair,in, maxFreq=4000,w,x,y,z,w2,x2,y2,z2;
				in = In.ar(effIn)*effInVol;
				ffrq1 = Lag.kr(ffrq1,0.1);
				ffrq2 = Lag.kr(ffrq2,0.1);
				fltPair = [
					Ringz.ar(
						in*((dcy1*250).reciprocal),
						SinOsc.kr(mfrq1,		// lfoFreq
							0,mDpt1,	// lfoDepth
							ffrq1+			// centerFreq
							(Amplitude.kr(in,0.01,0.5)
								*aDpt1)),		// ampDepth
						dcy1							// decaytime
					)*vol1,						// volume
					Ringz.ar(
						in*((dcy1*250).reciprocal),
						SinOsc.kr(mfrq2,		// lfoFreq
							0,mDpt2,	// lfoDepth
							ffrq2+			// centerFreq
							(Amplitude.kr(in,0.01,0.5)
								*aDpt2)),		// ampDepth
						dcy2							// decaytime
					)*vol2
				];
				#w,x,y,z = BFEncode1.ar(Compander.ar(fltPair[0],fltPair[0],thrsh,1,slp1,0.01,0.1)
					*EnvGen.kr(Env.asr(0.25,1.0,0.5),gate:gate, doneAction: 2)*vol,
					pan*panFactor,elev);
				#w2,x2,y2,z2 = BFEncode1.ar(
					Compander.ar(fltPair[1],fltPair[1],thrsh,1,slp2,0.01,0.1)
					*EnvGen.kr(Env.asr(0.25,1.0,0.5),gate:gate ,doneAction: 2)*vol,
					(pan+1).wrap(-1.0,1.0)*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
			}).store,

			SynthDef("2cmpEnvWahs2", { arg ffrq1,dcy1,mfrq1,mDpt1,aDpt1,thrsh,slp1,vol1,
				ffrq2, dcy2, mfrq2, mDpt2, aDpt2,pan,slp2,vol2,gate=1, effIn, effInVol=1, vol,elev;
				var fltPair,in, maxFreq=4000,w,x,y,z,w2,x2,y2,z2;
				in = In.ar(effIn)*effInVol;
				fltPair = [
					Ringz.ar(
						in*((dcy1*250).reciprocal),
						SinOsc.kr(mfrq1+
							(Amplitude.kr(in,0.01,0.5)
								*aDpt1*maxFreq),		// lfoFreq
							0,mDpt1,	// lfoDepth
							ffrq1.min(maxFreq)),			// centerFreq
						dcy1							// decaytime
					)*vol1,						// volume
					Ringz.ar(
						in*((dcy1*250).reciprocal),
						SinOsc.kr(mfrq2+
							(Amplitude.kr(in,0.01,0.5)*aDpt2), // lfoFreq
							0,mDpt2,	// lfoDepth
							ffrq2.min(maxFreq)),		// centerFreq
						dcy2							// decaytime
					)*vol2
				];
				#w,x,y,z = BFEncode1.ar(Compander.ar(fltPair[0],fltPair[0],thrsh,1,slp1,0.01,0.1)
					*EnvGen.kr(Env.asr(0.25,1.0,0.5),gate:gate, doneAction: 2)*vol,
					pan*panFactor,elev);
				#w2,x2,y2,z2 = BFEncode1.ar(
					Compander.ar(fltPair[1],fltPair[1],thrsh,1,slp2,0.01,0.1)
					*EnvGen.kr(Env.asr(0.25,1.0,0.5),gate:gate,doneAction: 2)*vol,
					(pan+1).wrap(-1.0,1.0)*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
			}).store,

			SynthDef("audio1RecCircBuf",{ arg recBuf,effAmp,pan,sigAmp,gate=1,effBus,
				bufNum,vol,elev;
				var envelope, in,w,x,y,z;
				envelope = EnvGen.kr(Env.asr(0.05, 1, 0.05), gate: gate,  doneAction: 2);
				in = AudioIn.ar(1)*envelope;
				BufWr.ar(in,bufNum,
					Phasor.ar(0, BufRateScale.kr(bufNum), 0, BufFrames.kr(bufNum)),0);
				Out.ar(effBus,in*effAmp);
				#w,x,y,z = BFEncode1.ar(in*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef(\diskIn_stereo,{ arg dskBuf=0,
				strt,att=0.05,rls=0.05,a5,a6,a7,a8,a9,a10,a11,a12,a13,
				effAmp,pan,sigAmp=0.1,gate,recBus,effBus,bufNum,vol,elev;
				var stream,env,w,x,y,z,w2,x2,y2,z2;
				env = EnvGen.kr(Env.asr(att,1.0,rls),gate: gate,doneAction: 2);
				stream = DiskIn.ar(2,bufNum);
				Out.ar(recBus,Mix(stream)*env*sigAmp);
				Out.ar(effBus,Mix(stream)*env*sigAmp*effAmp);
				#w,x,y,z = BFEncode1.ar(stream[0],pan*panFactor,elev)*sigAmp*vol*env;
				#w2,x2,y2,z2 = BFEncode1.ar(stream[1],(pan+1)
					.wrap(-1.0,1.0)*panFactor,elev)*sigAmp*vol*env;
				Out.ar(0,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
			}).store,

			SynthDef(\diskIn_PEnvFol,{ arg dskBuf=0,
				strt,a3,a4,a5,a6,a7,a8,a9,a10,a11,a12,a13,
				effAmp,pan,sigAmp=0.1,gate,recBus,effBus,bufNum,vol,elev;
				var stream,env,pitch,freq,hasFreq,amp,w,x,y,z;
				env = EnvGen.kr(Env.asr(0.05,1,0.05),gate: gate,doneAction: 2);
				stream = Mix(DiskIn.ar(2,bufNum));
				#freq, hasFreq = Pitch.kr(stream,120,60,1720,ampThreshold: 0.02,median: 7);
				amp = Amplitude.kr(stream,0.02,0.02);
				Out.ar(recBus,stream*env*sigAmp);
				Out.ar(effBus,stream*env*sigAmp*effAmp);
				#w,x,y,z = BFEncode1.ar(stream,pan*panFactor,elev)*(1-effAmp)*sigAmp*vol*env;
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef(\diskIn_mono,{ arg dskBuf=0,
				strt,a3,a4,a5,a6,a7,a8,a9,a10,a11,a12,a13,effAmp,pan,	sigAmp=0.1,gate,recBus,effBus,bufNum,vol,elev;
				var stream,env,w,x,y,z;
				env = EnvGen.kr(Env.asr(0.05,1,0.05),gate: gate,doneAction: 2);
				stream = DiskIn.ar(1,bufNum);
				Out.ar(recBus,stream*env*sigAmp);
				Out.ar(effBus,stream*env*sigAmp*effAmp);
				#w,x,y,z = BFEncode1.ar(stream,pan*panFactor,elev)*(1-effAmp)*sigAmp*vol*env;
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef(\diskIn_gran,{ arg dskBuf=0,strt,spd,dens,a5,a6,a7,a8,a9,a10,a11,a12,a13,
				effAmp,pan,sigAmp=0.1,gate,recBus,effBus,bufNum,vol,elev;
				var src,stream1,stream2,stream3,stream4,sig,env,grainEnv,w,x,y,z;
				grainEnv = EnvGen.kr(Env.new([ 0.001,1.0,0.001], [ 0.5,0.5 ], 'sine'),
					Impulse.kr(spd), timeScale: spd.reciprocal*dens);
				env = EnvGen.kr(Env.asr(0.05,1,0.05),gate: gate,doneAction: 2);
				src = Mix(DiskIn.ar(2,bufNum));
				stream1 = src*grainEnv;
				stream2 = src*DelayN.kr(grainEnv,1,spd.reciprocal*0.25);
				stream3 = src*DelayN.kr(grainEnv,1,spd.reciprocal*0.5);
				stream4 = src*DelayN.kr(grainEnv,1,spd.reciprocal*0.75);
				sig = Mix([stream1,stream2,stream3,stream4]);
				Out.ar(recBus,sig*env*sigAmp);
				Out.ar(effBus,sig*env*sigAmp*effAmp);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev)*(1-effAmp)*sigAmp*vol*env;
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef(\diskIn2Gran,{ arg dskBuf=0,strt=0,gfrq=2.0,dnse=0.5,attR=0.5,
				mfrq1=1,fMod=0,mfrq2=1,dMod=0,a11,a12,a13,a14,
				effAmp,pan,sigAmp=0.1,gate,recBus,effBus,bufNum,vol,elev;
				var stream,env,grainEnv,w,x,y,z,w2,x2,y2,z2,gspd,dense;
				env = EnvGen.kr(Env.asr(0.05,1.0,0.1),gate: gate,doneAction: 2);
				gspd = SinOsc.kr(mfrq1,0,fMod*gfrq,gfrq);
				dense = SinOsc.kr(mfrq2,0,dMod*dnse,dnse);
				grainEnv = EnvGen.kr(Env.new([ 0.001,1.0,0.001], [ attR,1-attR ], 'sine'),
					Impulse.kr(gspd), timeScale: gspd.reciprocal*dense);
				stream = DiskIn.ar(2,bufNum,1)*grainEnv;
				Out.ar(recBus,Mix(stream)*env*sigAmp);
				Out.ar(effBus,Mix(stream)*env*sigAmp*effAmp);
				#w,x,y,z = BFEncode1.ar(stream[0],pan*panFactor,elev)
				*sigAmp*vol*env*(1-effAmp);
				#w2,x2,y2,z2 = BFEncode1.ar(stream[1],(pan+1)
					.wrap(-1.0,1.0)*panFactor,elev)*sigAmp*vol*env*(1-effAmp);
				Out.ar(0,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
			}).store,

			SynthDef(\diskIn1Gran,{ arg dskBuf=0,strt=0,gfrq=2.0,dnse=0.5,attR=0.5,
				mfrq1=1,fMod=0,mfrq2=1,dMod=0,a11,a12,a13,a14,
				effAmp,pan,sigAmp=0.1,gate,recBus,effBus,bufNum,vol,elev;
				var stream,env,grainEnv,w,x,y,z,w2,x2,y2,z2,gspd,dense;
				env = EnvGen.kr(Env.asr(0.05,1.0,0.1),gate: gate,doneAction: 2);
				gspd = SinOsc.kr(mfrq1,0,fMod*gfrq,gfrq);
				dense = SinOsc.kr(mfrq2,0,dMod*dnse,dnse);
				grainEnv = EnvGen.kr(Env.new([ 0.001,1.0,0.001], [ attR,1-attR ], 'sine'),
					Impulse.kr(gspd), timeScale: gspd.reciprocal*dense);
				stream = DiskIn.ar(1,bufNum,1)*grainEnv;
				Out.ar(recBus,stream*env*sigAmp);
				Out.ar(effBus,stream*env*sigAmp*effAmp);
				#w,x,y,z = BFEncode1.ar(stream,pan*panFactor,elev)
				*sigAmp*vol*env*(1-effAmp);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			// effect Synths

			SynthDef("combDelay", { arg dlyt,dcyt,cAmp,pan,effAmp,gate=1,effIn,effInVol=1,vol,elev;
				var comb,in, maxDelay=4, maxDecay=20, delay, ctrVol,w,x,y,z,env;
				in = In.ar(effIn)*effInVol;
				env=EnvGen.kr(Env.asr(0.25,1,0.5), gate: gate,  doneAction: 2);
				comb = CombC.ar(in, maxDelay, dlyt,
					dcyt.neg*dlyt,env)*effAmp;   		// decay
				#w,x,y,z = BFEncode1.ar((comb+(in*cAmp))*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,


			SynthDef("combAutoPan_4", { arg dlyt,dcyt,cAmp,spd,effAmp,gate=1,effIn,effInVol=1,vol,elev;
				var comb,in, maxDelay=4, maxDecay=20, delay,panlfo,w,x,y,z;
				in = In.ar(effIn)*effInVol;
				panlfo = LFNoise1.kr(spd);
				comb = CombC.ar(in, maxDelay, dlyt,
					(dcyt.neg)*dlyt);   		// decay
				#w,x,y,z = BFEncode1.ar((comb+(in*cAmp))*vol,panlfo*panFactor,elev)
				*EnvGen.kr(Env.asr(0.25,1.0,0.5), gate: gate,  doneAction: 2);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
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
				Out.ar(0,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
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
				Out.ar(0,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
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
				Out.ar(0,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
			}).store,

			SynthDef("frqShftCmp", { arg frqShift,fmul,lfrq,fDpth,lPhs,aDpt1,loPan,loAmp,
				thrsh,slpb,slpa,att,rls,a13,hiPan,hiAmp,gate=1, effIn, effInVol=1, vol,elev;
				var in, r1,c1, r2, c2, pmodLFO, src,maxFreq=25,lfoFrq,w,x,y,z,w2,x2,y2,z2;
				in = In.ar(effIn)*effInVol;
				lfoFrq = lfrq+(Amplitude.kr(in,0.01,0.5)
					*aDpt1);		// lfoFreq
				pmodLFO = SinOsc.kr(lfoFrq,lPhs,fDpth);
				#c1,r1 = Hilbert.ar(LeakDC.ar(in));
				r2 = SinOsc.ar(((frqShift+pmodLFO)*fmul)-(fmul/2), pi/2);
				c2 = SinOsc.ar(((frqShift+pmodLFO)*fmul)-(fmul/2));
				// does it make sense for loPan and hiPan to be used to set both pan position and then bFormat azimuth pan speed???
				src = Pan2.ar(
				// 12.17 swapped formulas for hi and lo shift channels
					Compander.ar(((r1*r2)-(c1*c2)),in,thrsh,slpb,slpa,att,rls),hiPan,hiAmp)+
				Pan2.ar(
					Compander.ar(((r1*r2)+(c1*c2)),in,thrsh,slpb,slpa,att,rls),loPan,loAmp);
				#w,x,y,z = BFEncode1.ar(src[0]
					*EnvGen.kr(Env.asr(0.25,1.0,0.5), gate: gate, doneAction: 2)*vol,
					LFSaw.kr(loPan,Rand(0,2))*panFactor,elev);
				#w2,x2,y2,z2 = BFEncode1.ar(src[1]
					*EnvGen.kr(Env.asr(0.25,1.0,0.5), gate: gate, doneAction: 2)*vol,
					LFSaw.kr(hiPan,Rand(0,2))*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
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
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
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
				Out.ar(0,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
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
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
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
				Out.ar(0,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
			}).store,

			SynthDef("pitchShift",{ arg pShft,pDsp,tDsp,wsize,pan,amp,gate=1, effIn, effInVol=1, vol,elev;
				var in,src,w,x,y,z;
				in = In.ar(effIn)*effInVol;
				src = PitchShift.ar(in,wsize,pShft,pDsp,tDsp,amp);
				#w,x,y,z = BFEncode1.ar(src*vol*
					EnvGen.kr(Env.asr(0.25,1.0,0.5),gate: gate, doneAction: 2),
					pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
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
				Out.ar(0,BFDecode1.ar(w+w2,x+x2,y+y2,z+z2,azArray,elArray))
			}).store,

			SynthDef(\binShift,{ arg bShft,stch,fftBuf,a4,a5,a6,a7,a8,a9,a10,a11,a12,a13,a14,
				pan,amp,gate=1,effIn,effInVol=1, bufNum,vol,elev;
				var in, fft, chain, shift, stretch,w,x,y,z;
				in = In.ar(effIn)*effInVol;
				fft = FFT(bufNum,in);
				chain = PV_BinShift(fft, stch, bShft);
				#w,x,y,z = BFEncode1.ar(IFFT(chain),pan*panFactor,elev,gain: amp)*
				EnvGen.kr(Env.asr(0.25,1.0,0.5),gate: gate, doneAction: 2)*vol;
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
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
				Out.ar(0,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
			}).store,

			SynthDef("waveShaper",{ arg wvsBuf,thrsh,slpb,slpa,pan,amp,
				gate=1,effIn,effInVol=1, bufNum,vol,elev;
				var in,sig,w,x,y,z;
				in = In.ar(effIn)*effInVol;
				sig = LeakDC.ar(Shaper.ar(bufNum,in,amp),0.995);
				sig = Compander.ar(sig,sig,thrsh,slpb,slpa,0.001,0.3);
				#w,x,y,z = BFEncode1.ar(sig,pan*panFactor,elev)*amp*vol*
				EnvGen.kr(Env.asr(0.01,1,0.1),gate: gate, doneAction: 2);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
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
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
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
				Out.ar(0,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
			}).store,

			// effect synthdefs from Retrospectacles
			// s.options.blockSize
			SynthDef("recBufs2_rSpec",{ arg
				rate1=1,len1=8,on1=1,amp1=0.2,a5,a6,a7,a8,
				rate2=1,len2=8,on2=1,amp2=0.2,a13,a14,a15,recBuf,
				gate=1,effIn,effInVol=1,bufNum,vol=0,elev=0;
				var in,in1,in2,thisRate1, thisRate2,loopDur1,loopDur2,
				trg1,trg2,env1,env2,phs1,phs2,
				buf1,buf2,recycle1=0,recycle2=0,gateEnvGen,sampler1,sampler2,looplength=1,
				chngTrg1,chngTrg2,randLFO,minGrainDur=0.005,maxBufLen,tap1=0,tap2=0,blockSize=64,
				w,x,y,z,w2,x2,y2,z2;
				maxBufLen = BufDur.kr(bufNum)-(BufSampleRate.kr(bufNum).reciprocal*blockSize);
				buf1=bufNum; buf2=bufNum+1;
				gateEnvGen = EnvGen.kr(Env.asr(0.1,1.0,0.5),
					gate: gate, levelScale: 1, doneAction: 2);
				thisRate1 = BufRateScale.kr(buf1)*rate1;
				thisRate2 = BufRateScale.kr(buf2)*rate2;
				in=(In.ar(effIn)*effInVol);

				phs1 = Phasor.ar(0, BufRateScale.kr(buf1), 0, BufFrames.kr(buf1));
				recycle1 = BufRd.ar(1,buf1,phs1);
				on1 = Lag.kr(on1 > 0.5, 0.1)>0;
				in1 = (in * on1) + (recycle1 * (1 - on1));
				BufWr.ar(in1,buf1,phs1);
				trg1 = Impulse.kr(len1.reciprocal);  // trigger loop reset
				env1 = EnvGen.ar(Env.new([0,1,1,0],  // loop env
					[minGrainDur*0.5,len1-minGrainDur,minGrainDur*0.5],
					\welch),trg1);  // also triggers if loop-length changes
				sampler1 = PlayBuf.ar(1,buf1,thisRate1,trg1,0,1)*env1;

				phs2 = Phasor.ar(0, BufRateScale.kr(buf2), 0, BufFrames.kr(buf2));
				recycle2 = BufRd.ar(1,buf2,phs2);
				on2 = Lag.kr(on2 > 0.5, 0.1)>0;
				in2 = (in * on2) + (recycle2 * (1 - on2));
				BufWr.ar(in2,buf2,phs2);
				trg2 = Impulse.kr(len2.reciprocal);  // trigger grain env
				env2 = EnvGen.ar(Env.new([0,1,1,0],
					[minGrainDur*0.5,len2-minGrainDur,minGrainDur*0.5],
					\welch),trg2);  // also triggers if loop-length changes
				sampler2 = PlayBuf.ar(1,buf2,thisRate2,trg2,0,1)*env2;

				randLFO = LFNoise1.kr(1,0.5);  // pan and output
				#w,x,y,z = BFEncode1.ar(sampler1*amp1*vol,
					randLFO*panFactor,elev);
				#w2,x2,y2,z2 = BFEncode1.ar(sampler2*amp2*vol,
					randLFO*(-1)*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray)*gateEnvGen)
			}).store,

			SynthDef("stereoDelayRing2_rSpec",{ arg mfrq1,dly1,dcyt1,a4,a5,a6,pan1,amp1,
				mfrq2,dly2,dcyt2,a12,a13,a14,pan2,amp2,gate=1,effIn,effInVol=1,vol,elev;
				var modOsc1,modOsc2,maxDelay=2,in,env,src,w,x,y,z,w2,x2,y2,z2;
				env = EnvGen.kr(Env.asr(0.5, effInVol, 1), gate: gate, doneAction: 2);
				in = In.ar(effIn)*effInVol;
				modOsc1 = SinOsc.ar(mfrq1,0,amp1);
				modOsc2 = SinOsc.ar(mfrq2,0,amp2);
				src = [CombC.ar(in,maxDelay,dly1.max(0.005),dcyt1,modOsc1),
					CombC.ar(in,maxDelay,dly2.max(0.005),dcyt2,modOsc2)];
				#w,x,y,z = BFEncode1.ar(src[0]*env,pan1*pi,elev)*vol;
				#w2,x2,y2,z2 = BFEncode1.ar(src[1]*env,pan2*pi,elev)*vol;
				Out.ar(0,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
			}).store,

			SynthDef("stereoDelayRing3_rSpec",{ arg mfrq1,dly1,dcyt1,a4,a5,a6,pan1,amp1,
				mfrq2,dly2,dcyt2,a12,a13,a14,pan2,amp2,gate=1,effIn,effInVol=1,vol,elev;
				var modOsc1,modOsc2,maxDelay=2,in,env,src,w,x,y,z,w2,x2,y2,z2;
				env = EnvGen.kr(Env.asr(0.5, effInVol, 1), gate: gate, doneAction: 2);
				in = In.ar(effIn)*effInVol;
				modOsc1 = SinOsc.ar(mfrq1,0,amp1);
				modOsc2 = SinOsc.ar(mfrq2,0,amp2);
				src = [CombC.ar(in,maxDelay,dly1.max(0.005),dcyt1,modOsc1),
					CombC.ar(in,maxDelay,dly2.max(0.005),dcyt2,modOsc2)];
				#w,x,y,z = BFEncode1.ar(src[0]*env,LFNoise1.kr(pan1,Rand(0,2))*pi,elev)*vol;
				#w2,x2,y2,z2 = BFEncode1.ar(src[1]*env,LFNoise1.kr(pan2,Rand(0,2))*pi,elev)*vol;
				Out.ar(0,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
			}).store,

			// scale pitch playback from 4 octaves down (0.0625) to 4 octaves up (16)
			SynthDef("delayPShifts2_rSpec",{|
				pShft1=1,dly1=0.1, fb1=0, pDsp1=0, tDsp1=0, wsize=0.05, pan1=0.5,amp1=0.2,
				pShft2=1,dly2=0.1,fb2=0,pDsp2=0,tDsp2=0,recBuf=0,pan2=0.5,amp2=0.2,
				gate=1,effIn,effInVol=1, bufNum,vol,elev |
				var envelope,in,buf1,buf2,tap1,tap2,phs1,phs2,w,x,y,z,w2,x2,y2,z2;
				envelope = EnvGen.kr(Env.asr(0.5, effInVol, 0.5), gate: gate, doneAction: 2);
				buf1=bufNum; buf2=bufNum+1;
				in = In.ar(effIn)*effInVol;
				phs1=Phasor.ar(Impulse.kr(dly1.reciprocal),BufRateScale.ir(buf1),
					0,BufFrames.ir(buf1)*(dly1*(BufDur.ir(buf1).reciprocal)));
				tap1 = PitchShift.ar(BufRd.ar(1,buf1,phs1,1),
					wsize,pShft1,pDsp1,tDsp1,amp1);
				BufWr.ar(in+(tap1*fb1),buf1,phs1,1);
				phs2=Phasor.ar(Impulse.kr(dly2.reciprocal),BufRateScale.ir(buf2),
					0,BufFrames.ir(buf2)*(dly2*(BufDur.ir(buf2).reciprocal)));
				tap2 = PitchShift.ar(BufRd.ar(1,buf2,phs2,1),
					wsize,pShft2,pDsp2,tDsp2,amp2);
				BufWr.ar(in+(tap2*fb2),buf2,phs2,1);
				#w,x,y,z = BFEncode1.ar(tap1,pan1*panFactor,elev,rhoArray);
				#w2,x2,y2,z2 = BFEncode1.ar(tap2,pan2*panFactor,elev,rhoArray);
				Out.ar(0,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray)*envelope*vol)
			}).store,

			SynthDef("scanRecBufs_rSpec",{ arg
				rate1=1,strt1=0,end1=1,spd1=1,gdur1=0.1,gfrq1=10,amp1=0.2,a8,
				rate2=1,strt2=0,end2=1,spd2=1,gdur2=0.1,gfrq2=10,amp2=0.2,recBuf,
				gate=1,effIn,effInVol=1,bufNum,vol=0,elev=0;
				var in,loopDur1,loopDur2,buf1,buf2,
				thisPos1,thisRate1,thisDur1,thisPos2,thisRate2,thisDur2,
				gateEnvGen,granulator1,granulator2,bufDur1,bufDur2,
				scanSpd1,sign1,scanSpd2,sign2,panwidth=0.5,
				minGrainDur=0.005,w,x,y,z,w2,x2,y2,z2;
				buf1=bufNum; buf2=bufNum+1;
				gateEnvGen = EnvGen.kr(Env.asr(0.5,1.0,1.0),
					gate: gate, doneAction: 2);
				in=In.ar(effIn)*effInVol;
				RecordBuf.ar(in,buf1);
				RecordBuf.ar(in,buf2);
				bufDur1 = BufDur.kr(buf1);
				thisDur1 = (end1*bufDur1)-(strt1*bufDur1); // is signed, or can be 0
				sign1 = thisDur1.sign;
				sign1 = (sign1 >= 0) + (sign1*(sign1<0));
				scanSpd1 = spd1*((thisDur1.abs.max(0.01)).reciprocal);
				thisPos1 = LFSaw.kr(scanSpd1,1,0.5,0.5)*thisDur1;
				granulator1 = Mix(TGrains.ar(2,Impulse.kr(gfrq1),buf1,
					rate1*sign1,
					(strt1*bufDur1)+thisPos1+(gdur1*0.5),  // buffer centerPos in secs
					gdur1,0));
				bufDur2 = BufDur.kr(buf2);
				thisDur2 = (end2*bufDur2)-(strt2*bufDur2); // is signed, or can be 0
				sign2 = thisDur2.sign;
				sign2 = (sign2 >= 0) + (sign2*(sign2<0));
				scanSpd2 = spd2*((thisDur2.abs.max(0.01)).reciprocal);
				thisPos2 = LFSaw.kr(scanSpd2,1,0.5,0.5)*thisDur2;
				granulator2 = Mix(TGrains.ar(2,Impulse.kr(gfrq2),buf2,
					rate2*sign2,
					(strt2*bufDur2)+thisPos2+(gdur2*0.5),  // buffer centerPos in secs
					gdur2,0));
				// pan and output
				#w,x,y,z = BFEncode1.ar(granulator1*gateEnvGen*amp1*vol,
					panwidth*panFactor,elev);
				#w2,x2,y2,z2 = BFEncode1.ar(granulator2*gateEnvGen*amp2*vol,
					panwidth.neg*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray))
			}).store,

			SynthDef("binShifts_rSpec",{ arg bShft1=1,stch1=0,hop=0.25,wtyp=1,lpfrq1=4000,a6,pan1,amp1,
				bShft2,stch2,a11,slp=0.1,lpfrq2=4000,fftBuf,pan2,amp2,gate=1,effIn,effInVol=1,bufNum,vol,elev;
				var in, fft,fft2,chain1,chain2,lpf1,lpf2,lim1,lim2,w,x,y,z,w2,x2,y2,z2,envelope;
				envelope = EnvGen.kr(Env.asr(0.1,1.0,0.1),gate:gate,doneAction:2);
				in = In.ar(effIn)*effInVol*envelope;
				fft = FFT(bufNum,in,hop,wtyp,1);  // or use a buffer size 1
				fft2 = FFT(bufNum+1+1,in,hop,wtyp,1);	// bufNum+1 must be < numFFTbuffers
				chain1 = PV_BinShift(fft, stch1, bShft1);
				chain2 = PV_BinShift(fft2, stch2, bShft2);
				lpf1 = LPF.ar(IFFT.ar(chain1,1,1024),lpfrq1)*envelope;
				lpf2 = LPF.ar(IFFT.ar(chain2,1,1024),lpfrq2)*envelope;
				lim1 = LeakDC.ar(CompanderD.ar(lpf1,0.8,1,slp,0.005,0.01));
				lim2 = LeakDC.ar(CompanderD.ar(lpf2,0.8,1,slp,0.005,0.01));
				#w,x,y,z = BFEncode1.ar(lpf1,pan1*panFactor,elev,rhoArray,gain: amp1);
				#w2,x2,y2,z2 = BFEncode1.ar(lpf2,pan2*panFactor,elev,rhoArray,gain: amp2);
				Out.ar(0,BFDecode1.ar(w+w2, x+x2, y+y2, z+z2, azArray, elArray)*vol)
			}).store,

			// "shuffle" effect synthdefs

			SynthDef("rings_shuf",{|frq1,a2,pan1,amp1,frq2,a6,pan2,amp2,
				frq3,a10,pan3,amp3,frq4,a14,pan4,amp4,
				gate=1,effIn,effInVol=1,vol,elev|
				var env,in,ring,r1,r3,r4,
				w,x,y,z,w2,x2,y2,z2,w3,x3,y3,z3,w4,x4,y4,z4;
				env = EnvGen.kr(Env.asr(0.05,1,0.05),gate:gate,doneAction: 2);
				in = In.ar(effIn);
				ring = in*SinOsc.ar(frq1);
				r1 = in.ring1(SinOsc.ar(frq2));
				r3 = SinOsc.ar(frq3).ring3(in);
				r4 = in.ring4(SinOsc.ar(frq4));
				#w,x,y,z = BFEncode1.ar(in,pan1*panFactor,elev,rhoArray,gain: amp1);
				#w2,x2,y2,z2 = BFEncode1.ar(r1,pan2*panFactor,elev,rhoArray,gain:amp2);
				#w3,x3,y3,z3 = BFEncode1.ar(r3,pan3*panFactor,elev,rhoArray,gain:amp3);
				#w4,x4,y4,z4 = BFEncode1.ar(r4,pan4*panFactor,elev,rhoArray,gain:amp4);
				Out.ar(0,BFDecode1.ar(w+w2+w3+w4, x+x2+x3+x4, y+y2+y3+y4, z+z2+z3+z4,
					azArray, elArray)*vol)*env
			}).store,

			SynthDef("compClip_shuf",{arg thrsh=0.1,slpb=1,slpa=1,clip,a5,a6,a7,a8,
				a9,a10,a11,a12,arg13,a14,pan=0,amp=0,gate=1,effIn,effInVol=1,vol,elev=0;
				var env, in, compSig, clipSig,w,x,y,z;
				env = EnvGen.kr(Env.asr(0.05, 1, 0.05), gate: gate, levelScale: effInVol, doneAction: 2);
				in = In.ar(effIn);
				compSig = Compander.ar(in,in,thrsh,slpb,slpa,0.01, 0.01);
				clipSig = compSig clip2: clip;
				#w,x,y,z = BFEncode1.ar(clipSig,pan*panFactor,elev,rhoArray,gain:amp);
				Out.ar(0,BFDecode1.ar(w, x, y, z,azArray, elArray)*vol)*env*vol
			}).store,

			SynthDef("compFold_shuf",{arg thrsh=0.1,slpb=1,slpa=1,fold=0.5,
				a5,a6,a7,a8,a9,a10,a11,a12,a13,a14,
				pan=0,amp=0,gate=1,effIn,effInVol=1,vol,elev=0;
				var env, in, compSig, foldSig,w,x,y,z;
				env = EnvGen.kr(Env.asr(0.05, 1, 0.05), gate: gate, levelScale: effInVol, doneAction: 2);
				in = In.ar(effIn);
				compSig = Compander.ar(in,in,thrsh,slpb,slpa,0.01, 0.01);
				foldSig = compSig fold2: fold;
				#w,x,y,z = BFEncode1.ar(foldSig,pan*panFactor,elev,rhoArray,gain:amp);
				Out.ar(0,BFDecode1.ar(w, x, y, z,azArray, elArray)*vol)*env*vol
			}).store,

			// shuffle signal synthdefs

			SynthDef("rndGrnSamp_shuf",{ arg rate=1,strt=0,end=0, a3, a4,
				a5,a6,rtTbl=1, a8,a9,a10,a11,
				recBuf,effAmp=0,pan=0, sigAmp=0.1, gate=1,bufNum,recBus,effBus,vol,elev=0;
				var dir, thisRate, thisDur, loopFrq,env,gateEnvGen,sampler,plsLFO,sineLFO,
				sawLFO, attRelTime=0.0125,rstrt,rand,w,x,y,z;
				gateEnvGen = EnvGen.kr(
					Env.asr(attackTime: attRelTime/2, releaseTime: attRelTime/2),
					gate: gate, 	// gate
					levelScale: mstrLev, doneAction: 2
				);
				dir = (end-strt).sign;
				dir = (dir.abs>0).if(dir,1);   // if end-strt == 0, then dir = 1 (not 0)
				thisDur = ((end-strt).abs*BufDur.kr(bufNum)*(rate.reciprocal))
				.max(attRelTime);
				rand = TRand.kr(strt,end,Impulse.kr(thisDur.reciprocal));
				thisRate = BufRateScale.kr(bufNum)*rate*dir;
				thisDur = ((end-rand).abs*BufDur.kr(bufNum)).max(0.05);
				loopFrq = thisDur.reciprocal;
				env = Env.new([0,1,1,0],[0.006,1.0-0.025,0.019],\welch);
				sampler = PlayBuf.ar(1,bufNum,thisRate, Impulse.kr(loopFrq),
					rand*BufFrames.kr(bufNum),1)
				*EnvGen.ar(env,Impulse.kr(loopFrq),timeScale: thisDur.max(0.01));
				Out.ar(recBus,sampler*gateEnvGen*sigAmp);
				Out.ar(effBus,sampler*gateEnvGen*effAmp*sigAmp);
				#w,x,y,z = BFEncode1.ar(sampler*gateEnvGen*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			// recBufScan -- uses Phasor
			// scnRate and wSize have no spec, so values are 0->1
			// //"rate strt end wSize scnRt          rtTbl                smp  effAmp pan  vol",
			SynthDef("recBufScan_shuf",{ arg rate=1,strt=0,end=1,wSize=0.05,
				scnRt=1,a5,a6,rtTbl=1,a8,a9,a10,a11,
				recBuf=0,effAmp=0,pan=0,sigAmp=0,gate,bufNum,effBus,vol,elev=0;
				var w,x,y,z,thisPos,dir,thisRate,loopFrq,env,gateEnvGen,sampler;
				gateEnvGen = EnvGen.kr(Env.asr(attackTime: 0.1, releaseTime: 0.1),
					gate,doneAction: 2);
				env = Env.new([0,1,1,0],[0.1,0.8,0.1]);
				dir = (end-strt).sign;
				thisRate = BufRateScale.kr(bufNum)*rate*dir;
				wSize = wSize.max(0.002);
				loopFrq = wSize.reciprocal;
				thisPos=Phasor.ar(0,scnRt,strt*BufFrames.kr(bufNum),
					end*BufFrames.kr(bufNum));
				sampler = PlayBuf.ar(1,bufNum,thisRate,Impulse.kr(loopFrq),
					strt*BufFrames.kr(bufNum)+thisPos,1)*
				// an EnvGen is 'gated' at the same rate as the sample is looped
				EnvGen.kr(env,Impulse.kr(loopFrq), timeScale: wSize);
				Out.ar(effBus,sampler*effAmp*sigAmp*gateEnvGen);
				#w,x,y,z = BFEncode1.ar(sampler*gateEnvGen*(1-effAmp)*sigAmp*vol,
					pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w,x,y,z, azArray, elArray))
			}).store,

			// is this SynthDef just more versatile than the previous one?
			SynthDef("bufTgrScan_shuf",{ arg rate=1,strt=0,end=1,dens=1,gfrq=0.8,
				spd=1,a6,rtTbl=1,a8,a9,a10,a11,recBuf=0,effAmp=0,pan=0,sigAmp=0,
				gate,vol,recBus,effBus,bufNum,elev=0;
				var thisPos,thisRate,thisDur,gateEnvGen, granulator,bufDur,scanSpd,gdur,sign,w,x,y,z,w2,x2,y2,z2;
				gateEnvGen = EnvGen.kr(Env.asr(attackTime: 0.01, releaseTime: 0.01),gate,
					levelScale: mstrLev,doneAction: 2 );
				gdur=dens/gfrq;
				bufDur = BufDur.kr(bufNum);
				thisDur = (end*bufDur)-(strt*bufDur); // is signed, or can be 0
				sign = thisDur.sign;
				sign = (sign >= 0) + (sign*(sign<0));
				scanSpd = spd*((thisDur.abs.max(0.01)).reciprocal);
				thisPos = LFSaw.kr(scanSpd,1,0.5,0.5)*thisDur;
				thisPos=Phasor.ar(0,(1.0/SampleRate.ir)*spd,0,thisDur);
				granulator = Mix(TGrains.ar(2,Impulse.kr(gfrq),bufNum,
					rate*sign,
					(strt*bufDur)+thisPos+(gdur*0.5),  // buffer centerPos in secs
					gdur,0,1,4));
				Out.ar(effBus,granulator*effAmp*sigAmp*gateEnvGen);
				#w,x,y,z = BFEncode1.ar(granulator*gateEnvGen*(1-effAmp)*sigAmp*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w,x,y,z, azArray, elArray))
			}).store,

			SynthDef("audioIn_shuf",{ arg a1,a2,a3,a4,a5,a6,a7,a8,a9,a10,a11,a12,a13,
				effAmp=0,pan=0,sigAmp=0,gate,vol,recBus,effBus,bufNum,elev=0;
				var envelope,in,w,x,y,z;
				envelope = EnvGen.kr(Env.asr(attackTime: 0.2, releaseTime: 0.4),
					gate, doneAction: 2 );
				in = Mix(SoundIn.ar([0,1]));
				in = Compander.ar(in,in,0.01,10,1,0.01,0.1)*envelope;
				Out.ar(effBus,in*effAmp);
				#w,x,y,z = BFEncode1.ar(in*sigAmp*(1-effAmp)*vol,pan*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w,x,y,z,azArray,elArray));
			}).store,

			SynthDef("combDlyAutoPan_shuf", { arg dlyt=0.05,dcyt=0.1,
				a3,a4,a5,a6,a7,a8,a9,a10,a11,a12,a13,
				dpth=0.1,pSpd=0.2,amp=0,gate=1,effIn,effInVol=1,vol,elev;
				var comb,in, maxDelay=4, maxDecay=20, delay,panlfo,w,x,y,z;
				in = In.ar(effIn)*effInVol;
				panlfo = LFNoise1.kr(pSpd);
				comb = CombC.ar(in, maxDelay, dlyt,
					(dcyt.neg)*dlyt);   		// decay
				#w,x,y,z = BFEncode1.ar((comb+(in*dpth))*amp*vol,panlfo*panFactor,elev)
				*EnvGen.kr(Env.asr(0.25,1.0,0.5), gate: gate,  doneAction: 2);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray ))
			}).store,

			SynthDef("delayRing_shuf",{ arg mfrq=0.2,mDpt=0.01,dly=0.1,dcyt=0.1,
				a5,wdth=0.1,dpth=0.1,a8,a9,a10,a11,a12,a13,a14,
				pSpd=0.2,amp=0,gate=1,effIn,effInVol=1,vol,elev;
				var modOsc,maxDelay=2, maxFreq=4000,in,src,delay,w,x,y,z;
				dly=dly.max(0.005);
				in = In.ar(effIn)*effInVol;
				modOsc = SinOsc.kr(mfrq,0,mDpt);
				delay = CombC.ar(in,maxDelay,dly,dly*(dcyt.neg),modOsc+amp);
				src = (delay+(in*dpth))*vol;
				#w,x,y,z = BFEncode1.ar(src*
					EnvGen.kr(Env.asr(0.25,1.0,0.5), gate: gate, doneAction: 2),
					LFSaw.kr(pSpd,Rand(0,wdth))*panFactor,elev);
				Out.ar(0,BFDecode1.ar(w, x, y, z, azArray, elArray))
			}).store,
			// start recording at random location thru buffer, then loop
			SynthDef("recCircBuf_shuf",{ arg recBuf=0,start=0, gate=1,effIn,effInVol=1,
				bufNum;
				var envelope, in, phasor,bufSize;
				envelope = EnvGen.kr(Env.asr(0.05, effInVol, 0.05), gate: gate, doneAction: 2);
				in = In.ar(effIn)*envelope;
				bufSize = BufFrames.kr(bufNum);
				start= Rand(0,BufFrames.ir(bufNum));
				// phasor has random rec start point
				phasor = (Phasor.ar(0,1,0,bufSize)+(start.mod(bufSize)));
				BufWr.ar([in],bufNum,phasor,1);
				0.0
			}).store
		]
	}
}
