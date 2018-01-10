
KnowNoSynthPlayerOSCHub {
	var network,<vcNum,<view,<analyzers,synthDefs,<synthID,<curSynth,
	synthModCtls,<pguis,synthOnOff,<synCtl1,<synCtl2,<synCtl3,<curCtls,
	mod1Ctl,mod2Ctl,mod3Ctl,manCtl,volCtl,<>synCtlFunc,<startID,<lastID,s;

	*new {|network,vcNum,view| ^super.newCopyArgs(network,vcNum,view).init }

	init {
		startID = ((network.netPlayers.size+1)*400)+(50*vcNum)+10000;
		lastID=startID;
		analyzers = network.analyzers;
		s=network.oscHub;
		this.loadSynthDefs;
		this.makeGui;
	}

	makeGui {
		synthOnOff=CV(\unipolar,0);
		synCtl1=CV(\freq,0); synCtl2=CV(\mfrq,0); synCtl3=CV(\amp,0);
		curSynth=SV.new.sp((0..synthDefs.size),0)
		.items_(synthDefs.collect {|def| def.name.asSymbol });
		// synthControl display
		view.startRow;
		view.flow({|p|
			p.startRow;
			StaticText.new(p,10@20).visible_(false);
			pguis = [Button.new(p,80@20)
				.states_([["SynthOn"],["SynthOff"]]).background_(Color.white),
				StaticText.new(p,85@20).string_("SelectSynth").align_(\right);
				PopUpMenu.new(p,100@20),
				view.startRow;
				p.startRow; p.startRow;
				mod1Ctl= StaticText.new(p,80@15).font_(Font("Times",10))
				.string_("1-modFrq").align_(\right);
				Slider.new(p, 355@15),
				p.startRow;
				mod2Ctl = StaticText.new(p,80@15).font_(Font("Times",10))
				.string_("2-modDpth").align_(\right).align_(\right);
				Slider.new(p, 355@15),
				p.startRow;
				mod3Ctl = StaticText.new(p,80@15).font_(Font("Times",10))
				.string_("3-Amp").align_(\right);
				Slider.new(p, 355@15),
				p.startRow; p.startRow;
				manCtl = StaticText.new(p,80@20).string_("freq").align_(\right);
				Slider.new(p, 355@20).action_({|sl|
					synthID.notNil.if({s.sendMsg("/n_set",synthID,\manCtl,sl.value)})}),
				p.startRow;
				volCtl = StaticText.new(p,80@20).string_("vol").align_(\right);
				Slider.new(p, 355@20).action_({|sl|
					synthID.notNil.if({s.sendMsg("/n_set",synthID,\vol,sl.value)})});
		]},Rect(8, 340, 484, 175));

		synthOnOff.connect(pguis[0]);
		curSynth.connect(pguis[1]);
		synCtl1.connect(pguis[2]);
		synCtl2.connect(pguis[3]);
		synCtl3.connect(pguis[4]);

		synthOnOff.action_({|cv| var name= pguis[1].items[pguis[1].value];
			"turn on/off synth".post;
			if(cv.value.postln==1,{
				synthID=(lastID+1).wrap(startID,startID+200);
				lastID=synthID;
				s.sendMsg("/s_new",name,synthID,0,1,
					\vc,vcNum,\fbBus,analyzers.fbBus+vcNum);
		},{ s.sendMsg("/n_set",synthID,\gate,0); synthID=nil })});
		curSynth.action = {|m|
			curCtls = synthModCtls[curSynth.items[curSynth.value]];
				mod1Ctl.string_("1-"++(curCtls[0].asString));
				mod2Ctl.string_("2-"++(curCtls[1].asString));
				mod3Ctl.string_("3-"++(curCtls[2].asString));
			synCtl1.spec_(curCtls[0].asSpec);
			synCtl2.spec_(curCtls[1].asSpec);
			synCtl3.spec_(curCtls[2].asSpec);
			if(synthID.notNil,{{ synthOnOff.value_(0); // stop running synth
				pguis[6].value_(0.2) // reset 'vol' slider to 0.2

			}.defer });
		};
		synCtl1.action_({|ctl| synthID.notNil.if({(s.sendMsg("/n_set", synthID,curCtls[0],ctl.value))})});
		synCtl2.action_({|ctl| synthID.notNil.if({(s.sendMsg("/n_set", synthID,curCtls[1],ctl.value))})});
		synCtl3.action_({|ctl| synthID.notNil.if({(s.sendMsg("/n_set" ,synthID,curCtls[2],ctl.value))})});

		curSynth.value_(0)
	}

	loadSynthDefs {
		synthDefs = [
			SynthDef(\fmFB,{|freq=1000,mfrq=0.1,idx=2,amp=0.1,manCtl=0.1,
				vc=0,gate=1,vol=0.2,fbBus=64|
			var sig,env;
			freq=\freq.asSpec.map(manCtl);
			env = EnvGen.kr(Env.asr(attackTime: 0.001, releaseTime: 0.025),
				gate, doneAction: 2);
			sig = SinOsc.ar(
				SinOsc.ar(
					mfrq,0,
					idx*mfrq,
					freq),0,
				amp*env);
			Out.ar(fbBus,sig);
			Out.ar([0,1],sig*vol)
			}),
			SynthDef(\fmDrumFB,{ arg freq=440,rtio=4,idx=2,modFrq=0.1,manCtl=0.1,
				mod=0,trgf=4,phs=0,att=0.01,rls=0.25,amp=0.1,
				vc=0,gate=1,vol=0.2,fbBus=64;
				var modSig,env,sig;
				freq=\freq.asSpec.map(manCtl);
				env = EnvGen.kr(Env.asr(attackTime: 0.001, releaseTime: 0.025),
					gate, doneAction: 2);
				modSig = SinOsc.kr(modFrq,0,mod*freq);
				sig = Decay2.ar(Impulse.ar(trgf,phs,amp),att,rls,
					PMOsc.ar(freq+modSig,freq*rtio,idx)
				)*env;
				Out.ar(fbBus,sig);
				Out.ar([0,1],sig*vol)
			}),
			SynthDef(\pluckFB,{ arg freq=400,manCtl=0.1,brit=2000,plSpd=4,
				plDcy=1,clip=0.2,rq=0.5,mfrq=0.2,mod=1,att=0.01,rls=0.5,amp=0.1,
				vc=0,vol=0.2, gate=1,fbBus=64;
				var exciter,modSig,sig,bright,env;
				freq=\freq.asSpec.map(manCtl);
				env = EnvGen.kr(Env.asr(attackTime: 0.025, releaseTime: 0.025),
					gate, doneAction: 2);
				bright = brit+freq;
				exciter = WhiteNoise.ar(Decay.kr(Impulse.kr(plSpd), 0.01));
				modSig = SinOsc.kr(mfrq,0,mod*(bright*0.5));
				sig = RLPF.ar(CombC.ar(exciter,0.1,freq.reciprocal,plDcy),
					bright+modSig,rq,amp*env)
				.clip2(clip);
				Out.ar(fbBus,sig);
				Out.ar([0,1],sig*vol)
			}),
			SynthDef(\motoRevFB,{|freq=0.5,manCtl=0.1,lfrq=4,fmul=20,
				wdth=0.2,ffrq=2000,clip=1,amp=0.1,
				vc=0,gate=1,vol=0.2,fbBus=64|
				var env,sig;
				freq=\freq.asSpec.map(manCtl);
				env = EnvGen.kr(Env.asr(attackTime: 0.025, sustainLevel: 0.5, releaseTime: 0.025),
					gate, doneAction:2);
				sig = RLPF.ar(
					LFPulse.ar(
						SinOsc.kr(lfrq,0,fmul,freq*0.005) // freq
						,[0,0.1],wdth), 	// iphase, width
					ffrq,0.1).clip2(Rand(clip));  // filt freq, phase, clip
				sig=Mix(sig)*amp*env;
				Out.ar(fbBus,sig);
				Out.ar([0,1],sig*vol)
			}),

			SynthDef(\steam, {|manCtl=0.1,prm1=0.5, prm2=0.5,
				amp=0.2, vc=0, gate=1, vol=0.2,fbBus=64|
				var sig, env,freq;
				freq=[400,4000,\exp,0].asSpec.map(manCtl);
				env = EnvGen.kr(Env.asr(attackTime: 0.001, releaseTime: 0.025),
					gate, doneAction: 2);
				sig = SinOsc.ar(
					StandardN.ar(
						7+(prm1*193),
						0.1+(prm2*8.1)
					)*1500+freq
				).softclip
				*LFNoise0.ar(118/60, 0.3, 0.3)*env*amp;
				Out.ar(fbBus,sig);
				Out.ar(0,Pan2.ar(sig,0,vol));
			}),

			SynthDef(\gsLqdSld, {arg manCtl=0.1, prm1=0.5, prm2=0.5, amp=0.2,
				vc=0,  gate=1, vol=0.2,fbBus=64;
				var sig, env;
				var clockRate, clockTime, clock, centerFreq, freq;
				manCtl=[0.1,10.0,\exp,0].asSpec.map(manCtl);
				env = EnvGen.kr(Env.asr(attackTime: 0.001, releaseTime: 0.025),
					gate, doneAction: 2);
				clockRate = 8 + (12.3288*prm1).squared;
				clockTime = clockRate.reciprocal;
				clock = Dust.kr(clockRate*manCtl, 0.1);
				centerFreq = 100 + (88.882*prm2).squared;
				freq = Latch.kr(
					WhiteNoise.kr(
						centerFreq,
						centerFreq
					),
					clock
				);
				sig = SinOsc.ar(
					freq,
					0,
					Decay2.kr(
						clock,
						0.125*clockTime,
						1.6*clockTime
					)
				)*amp*env;
				Out.ar(fbBus,sig);
				Out.ar(0, sig*vol);
			}),

			SynthDef(\prickles, {arg prm1=0.5, prm2=0.5, amp=0.2, manCtl=0.1,
				vc=0,  gate=1, vol=0.2,fbBus=64;
				var trig, sig, env, spd;
				spd= [12,48,\exp,0].asSpec.map(manCtl);
				env = EnvGen.kr(Env.asr(attackTime: 0.001, releaseTime: 0.025),
					gate, doneAction: 2);
				trig = Impulse.ar(
					spd,
					0,
					SinOsc.kr(0.5, SinOsc.kr(3, 0, pi, pi), 1, 1)
				);
				sig = Ringz.ar(
					CoinGate.ar(0.05 + (0.9*prm1), trig*0.5),
					LFNoise0.kr(spd,1000,9000),
					0.001 + (0.31464*prm2).squared
				);
				sig = sig*env*amp;
				Out.ar(fbBus,sig);
				Out.ar(0,Pan2.ar( sig,0,vol))}),

			SynthDef(\noStation, {arg prm1=0.5, prm2=0.5, amp=0.2, manCtl=1.0,
				vc=0,  gate=1, vol=0.2,fbBus=64;
				var sig, env, freq;
				freq= [20, 4186, 'exp', 0].asSpec.map(manCtl);
				env = EnvGen.kr(Env.asr(attackTime: 0.001, releaseTime: 0.025),
					gate, doneAction: 2);
				sig = StandardN.ar(25 + 5488*prm1, 0.85 + 3.15*prm2)*amp*env;
				sig=LPF.ar(sig,freq);
				Out.ar(fbBus,sig);
				Out.ar(0,Pan2.ar(sig,0,vol));
			})

		].collect {|def| def.send(s) };
		// select feedback modulation params
		synthModCtls= (
			fmFB: [\idx,\mfrq,\amp],
			fmDrumFB: [\rtio,\trgf,\amp],
			pluckFB: [\plSpd,\mfrq,\amp],
			motoRevFB: [\lfrq,\ffrq,\amp],
			steam: [\prm1,\prm2,\amp],
			gsLqdSld: [\prm1,\prm2,\amp],
			prickles: [\prm1,\prm2,\amp],
			noStation: [\prm1,\prm2,\amp]
		);
		// provide specs for all fb mod keys
		Spec.specs.addAll([
			\idx->ControlSpec(1,20,\linear),
			\mfrq->	ControlSpec(0.1,200,\exp,0,2),
			\amp->ControlSpec(warp:\amp),
			\rtio->ControlSpec(0.01,20,\lin,0.1,1),
			\trgf->ControlSpec(0.25,40,\exp,default:4),
			\plSpd->[0.125,24,\exp,0,4].asSpec,
			\lfrq->ControlSpec(0.1,5,\lin),
			\ffrq->ControlSpec(80,8000,\exp,0,1200)]);

		// connect netgui to ctls
		synCtlFunc = {|v,dest,val| [synCtl1,synCtl2,synCtl3][dest].input_(val) };
		network.paramCtlViews[vcNum].ctlPlayerActions = [synCtlFunc,synCtlFunc,synCtlFunc];
	}

}
