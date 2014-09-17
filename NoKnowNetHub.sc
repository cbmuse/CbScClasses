NoKnowNetHub {
	var myName,<size=20,<>analyzer,<view,<guiCtls,<pguis,netPlayerSel;
	var <network,local,netDisp,<responders,<sendNetIndex,<netUpdateRoutine,<paramData,<synth;
	var <inOnOff,<inFrq,<frqProb,<frqScale,<frqOffset,<outFrq,<netFrq,<frqBlend,<frqCtlDest;
	var <inTimbr,<timbrProb,<timbrScale,<timbrOffset,<outTimbr,<netTimbr,<timbrBlend,<timbrCtlDest;
	var <inAmp,<ampProb,<ampScale,<ampOffset,<outAmp,<netAmp,<ampBlend,<ampCtlDest;
	var <netPlayer,netData,<synthOnOff,<>synthDefs,<curSynth,<synthModCtls,<curCtls,
	mod1Ctl,mod2Ctl,mod3Ctl,<synCtl1,<synCtl2,<synCtl3;

	*new {|name=\noKnow,size=20|
		^super.new.init(name,size)
	}

	init {|argName,argSize|
		myName=argName.asSymbol;
		size=argSize;
		network=NetHub();
		local =  NetAddr("127.0.0.1", 57110); // local server
		analyzer=FBanalyzer.new(1,this);
		{ while({ synthDefs.isNil },{ 0.1.wait }); // wait for boot and synthdefs load
			{ this.makeGui;
			paramData= (freq:0,timbre:0,amp:0);
			this.setParamResponses;
				this.setSendNetIndex;
			this.initResponders;
			view.onClose_({ this.stop }) }.defer
		}.fork
	}

	setSendNetIndex {
		netUpdateRoutine =
		Routine({
			inf.do({
			var a = network.clients.values.collect {|n| n.ip }.asSet; // save only unique IPs
			sendNetIndex = a.asArray.sort.detectIndex {|n| n == NetAddr.myIP }; // index low to high
			1.wait; })
		}).play
	}

	sendParamToAll {|param,val|
		if(((paramData[param.asSymbol]-val).abs > 0.01),{  // don't send repeating values
			paramData.put(param.asSymbol,val);
			network.toAll(["/param",sendNetIndex,param,val])})
	}

	setParamResponses {
		network.setAllResponses({|msg|
			var fromMe; // display netData from self, but block it from netPlayer inputs
			if(msg[1] == this.sendNetIndex,{fromMe=true },{fromMe=false});
			if(fromMe.not,{ msg.postln });
			switch(msg[2],
				\freq,{ var pl=msg[1]; var f=msg[3];
					this.displayNetData(pl,\freq,f);
					if(fromMe.not,{ if(netPlayer.value == pl,{ netFrq.value_(f) })});
				},
				\timbre,{ var pl=msg[1]; var f=msg[3];
					this.displayNetData(pl,\timbre,f);
					if(fromMe.not,{if(netPlayer.value == pl,{ netTimbr.value_(f) })});
				},
				\amp,{ var pl=msg[1]; var f=msg[3];
					this.displayNetData(pl,\amp,f);
					if(fromMe.not,{if(netPlayer.value == pl,{ netAmp.value_(f) })});
			})
		},'/param');
	}

	displayNetData {|idx=0,param=\freq,value=0.5|
		{ netDisp.index_((idx)*6+3+(\freq:0,\timbre:1,\amp:2)[param]);
			netDisp.currentvalue_(value) }.defer
	}

	makeGui {
		var onState,header;
		var labels = ["freq","timbre","amp"];
		view = FlowView.new(nil,Rect(8,60,480,500),8@8,2@2,"knowNo");
		header = StaticText(view,315@20)
		.string_("               input          prob          scale         offset");
		onState = Button.new(view,40@20)  // on-off
		.states_([[ "OFF", Color(1.0, 1.0, 1.0, 1.0), Color(0.0, 0.0, 1.0, 1.0) ],
			[ "ON", Color(0.0, 0.0, 0.0, 1.0), Color(1.0, 0.0, 0.0, 1.0) ]]).value_(0);
		inOnOff = CV(\unipolar,0);
		inOnOff.connect(onState);

		inFrq = CV(nil,0); frqProb= CV(nil,0); frqScale= CV(nil,0); frqOffset=CV(nil,0);
		outFrq=CV(nil,0); netFrq= CV(nil,0); frqBlend= CV(nil,0);
		inTimbr= CV(nil,0); timbrProb= CV(nil,0); timbrScale= CV(nil,0); timbrOffset= CV(nil,0);
		outTimbr=CV(nil,0); netTimbr= CV(nil,0); timbrBlend= CV(nil,0);
		inAmp= CV(nil,0); ampProb= CV(nil,0); ampScale= CV(nil,0); ampOffset= CV(nil,0);
		outAmp=CV(nil,0); netAmp= CV(nil,0); ampBlend= CV(nil,0);  synthOnOff=CV(\unipolar,0);
		synCtl1=CV(nil,0); synCtl2=CV(nil,0); synCtl3=CV(nil,0);

		frqCtlDest=SV.new((1..3),1);
		timbrCtlDest=SV.new((1..3),2);
		ampCtlDest=SV.new((1..3),0);
		netPlayer=SV.new.sp((0..size),0).items_(Array.fill(size,{|i| (i+1).asString }));
		//	netData=CV.new(Array.fill(size*6,{0},0));

		inFrq.action_({|cv| if(frqProb.value.coin,{ // rand filter gate
			var val = (cv.value*(1-frqBlend.value))+(netFrq.value*frqBlend.value);
			outFrq.value_(((val*frqScale.value)+frqOffset.value).wrap(0,1.001))})});
		inTimbr.action_({|cv| if(timbrProb.value.coin,{ // rand filter gate
			var val = (cv.value*(1-timbrBlend.value))+(netTimbr.value*timbrBlend.value);
			outTimbr.value_(((val*timbrScale.value)+timbrOffset.value).wrap(0,1.001))})});
		inAmp.action_({|cv| if(ampProb.value.coin,{ // rand filter gate
			var val = (cv.value*(1-ampBlend.value))+(netAmp.value*ampBlend.value);
			outAmp.value_(((val*ampScale.value)+ampOffset.value).wrap(0,1.001))})});
		netFrq.action_({|cv| if(frqProb.value.coin,{ // rand filter gate
			var val = (cv.value*frqBlend.value)+(inFrq.value*(1-frqBlend.value));
			outFrq.value_(((val*frqScale.value)+frqOffset.value).wrap(0,1.001))})});
		netTimbr.action_({|cv| if(timbrProb.value.coin,{ // rand filter gate
			var val = (cv.value*timbrBlend.value)+(inTimbr.value*(1-timbrBlend.value));
			outTimbr.value_(((val*timbrScale.value)+timbrOffset.value).wrap(0,1.001))})});
		netAmp.action_({|cv| if(ampProb.value.coin,{ // rand filter gate
			var val = (cv.value*ampBlend.value)+(inAmp.value*(1-ampBlend.value));
			outAmp.value_(((val*ampScale.value)+ampOffset.value).wrap(0,1.001))})});
		frqBlend.action_({|cv|
			var val = (inFrq.value*(1-cv.value))+(netFrq.value*cv.value);
			outFrq.value_(((val*frqScale.value)+frqOffset.value).wrap(0,1.001))});
		timbrBlend.action_({|cv|
			var val = (inTimbr.value*(1-cv.value))+(netTimbr.value*cv.value);
			outTimbr.value_(((val*timbrScale.value)+timbrOffset.value).wrap(0,1.001))});
		ampBlend.action_({|cv|
			var val = (inAmp.value*(1-cv.value))+(netAmp.value*cv.value);
			outAmp.value_(((val*ampScale.value)+ampOffset.value).wrap(0,1.001))});
		frqOffset.action_({|cv|
			var val = (inFrq.value*(1-frqBlend.value))+(netFrq.value*frqBlend.value);
			outFrq.value_(((val*frqScale.value)+cv.value).wrap(0,1.001))});
		timbrOffset.action_({|cv|
			var val = (inTimbr.value*timbrBlend.value)+(netTimbr.value*(1-timbrBlend.value));
			outTimbr.value_(((val*timbrScale.value)+cv.value).wrap(0,1.001))});
		ampOffset.action_({|cv|
			var val = (inAmp.value*(1-ampBlend.value))+(netFrq.value*ampBlend.value);
			outAmp.value_(((val*ampScale.value)+cv.value).wrap(0,1.001))});
		outFrq.action_({|cv|[synCtl1,synCtl2,synCtl3][frqCtlDest.value].value_(cv.value);
			this.sendParamToAll(\freq,cv.value)});
		outTimbr.action_({|cv|[synCtl1,synCtl2,synCtl3][timbrCtlDest.value].value_(cv.value);
			this.sendParamToAll(\timbre,cv.value)});
		outAmp.action_({|cv| [synCtl1,synCtl2,synCtl3][ampCtlDest.value].value_(cv.value);
			this.sendParamToAll(\amp,cv.value) });

		guiCtls = 3.collect {|i|
			view.startRow;
			StaticText(view,60@20).string_(labels[i]);
			[Slider(view,72@20),   // data input
				Slider(view,72@20),    // filterweight
				Slider(view,72@20),   // scale
				Knob.new(view,25@25).value_(0.5), // offset
				NumberBox(view,Rect(0,0,50,20)),
				view.startRow;
				StaticText(view,60@12).string_("net-"++labels[i]).font_(Font("Monaco",10));
				Slider(view,72@12).knobColor_(Color.red),
				StaticText(view,40@12).string_("blend").font_(Font("Monaco",12));
				Knob.new(view,25@25).value_(0.5), // blend
				StaticText.new(view,70@20).visible_(false);
				StaticText.new(view,40@20).string_("ctl->");
				PopUpMenu.new(view,40@20).items_([1,2,3]).font_(Font("Times",10)).value_(i)
			]
		};

		[inFrq,frqProb,frqScale,frqOffset,outFrq,netFrq,frqBlend].do {|cv,i|
			if(cv.notNil,{ cv.connect(guiCtls[0][i]) })};
		[inTimbr,timbrProb,timbrScale,timbrOffset,outTimbr,netTimbr,timbrBlend].do {|cv,i|
			if(cv.notNil,{ cv.connect(guiCtls[1][i]) })};
		[inAmp,ampProb,ampScale,ampOffset,outAmp,netAmp,ampBlend].do {|cv,i|
			if(cv.notNil,{ cv.connect(guiCtls[2][i]) })};
		[frqCtlDest,timbrCtlDest,ampCtlDest].do {|sv,i| sv.connect(guiCtls[i][7]) };

		// net Data display
		view.startRow;
		StaticText(view,70@22).string_("netPlayer").align_(\right);
		netPlayerSel = PopUpMenu.new(view,50@22)
		.action_({|m| (m.value+1) });
		netPlayer.connect(netPlayerSel);
		view.startRow;
		netDisp = MultiSliderView.new(view,435@100).size_(120).elasticMode_(1);
		netDisp.strokeColor_(Color(1,alpha:0.5)).fillColor_(Color.blue).background_(Color.grey);
		netDisp.value_(Array.fill(size*6,{0})).isFilled_(true).startIndex_(3);
		view.startRow;
		StaticText.new(view,4@20).visible_(false);
		size.do {|i| StaticText.new(view,20@20).string_((i+1).asString).font_(Font("Times",9)) };

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
				mod1Ctl= StaticText.new(p,90@20).string_("1-modFrq").align_(\right);
				Slider.new(p, 355@15)
				.action_({|sl|synth.notNil.if({synth.set(\modFrq,sl.value)})}),
				p.startRow;
				mod2Ctl = StaticText.new(p,90@20)
				.string_("2-modDpth").align_(\right).align_(\right);
				Slider.new(p, 355@15)
				.action_({|sl|synth.notNil.if({synth.set(\modDpth,sl.value)})}),
				p.startRow;
				mod3Ctl = StaticText.new(p,90@20).string_("3-Amp").align_(\right);
				Slider.new(p, 355@15)
				.action_({|sl|synth.notNil.if({synth.set(\amp,sl.value)})}),
				p.startRow;
				EZSlider.new(p, 440@20,"freq",\freq,
					{|sl| synth.notNil.if({
						synth.set(\freq,sl.value)})},initVal: 110, labelWidth:85),
				p.startRow;
				EZSlider.new(p, 440@20,"vol",\amp,
					{|sl| synth.notNil.if({synth.set(\vol,sl.value)})},
					initVal: 0.2, labelWidth:85)
		]},Rect(8, 340, 484, 152));

		synthOnOff.connect(pguis[0]);
		curSynth.connect(pguis[1]);
		synCtl1.connect(pguis[2]);
		synCtl2.connect(pguis[3]);
		synCtl3.connect(pguis[4]);

		synthOnOff.action_({|cv| var name= pguis[1].items[pguis[1].value];
			if(cv.value==1,{
				synth=Synth.before(analyzer.fbGroup,name)
		},{ synth.release; synth=nil })});
		curSynth.action = {|m|
			curCtls = synthModCtls[curSynth.items[curSynth.value]];
					mod1Ctl.string_("1-"++(curCtls[0].asString));
					mod2Ctl.string_("2-"++(curCtls[1].asString));
					mod3Ctl.string_("3-"++(curCtls[2].asString));
			if(synth.notNil,{synth.release});  // stop running synth
			pguis[6].value_(0.2) // reset 'vol' slider to 0.2
		};
		curSynth.value_(0);  // initialize curCtls
		synCtl1.action_({|cv| if(synth.notNil,{
			synth.set(curCtls[0],curCtls[0].asSpec.map(cv.value)) })});
		synCtl2.action_({|cv| if(synth.notNil,{
			synth.set(curCtls[1],curCtls[1].asSpec.map(cv.value)) })});
		synCtl3.action_({|cv| if(synth.notNil,{
			synth.set(curCtls[2],curCtls[2].asSpec.map(cv.value)) })});

		view.parent.onClose_({this.stopAll })
	}

	initResponders {
		responders = [
			OSCdef(\fbFreq,{|msg,time,addr,port|
				var bus = msg[4]- analyzer.fbBus.index;
				var val = msg[3];
			//	msg.postln; //  example: [ /freq, 1001, 1204, 440, 16 ]
				val = \freq.asSpec.unmap(val);
				inFrq.value_(val)
			},'/freq',local).fix,
			OSCdef(\fbTimbre, {|msg,time,addr,port|
				var bus = msg[4]- analyzer.fbBus.index;
				var val = msg[3];
			//	msg.postln; //  example: [ /freq, 1001, 1204, 440, 16 ]
				val = \freq.asSpec.unmap(val);
				inTimbr.value_(val)
			},'/specCentroid',local).fix,
			OSCdef(\fbAmp, {|msg,time,addr,port|
				var bus = msg[4]- analyzer.fbBus.index;
				var val = msg[3];
			//	msg.postln; //  example: [ /freq, 1001, 1204, 440, 16 ]
				val = \amp.asSpec.unmap(val);
				inAmp.value_(val)
			},'/peak',local).fix,
			OSCdef(\fbOn, {|msg,time,addr,port|
				var bus = msg[4]- analyzer.fbBus.index;
				var val = msg[3];
			//	msg.postln; //  example: [ /freq, 1001, 1204, 440, 16 ]
				inOnOff.value_(1)
			},'/on',local).fix,
			OSCdef(\fbOff, {|msg,time,addr,port|
				var bus = msg[4]- analyzer.fbBus.index;
				var val = msg[3];
			//	 msg.postln; //  example: [ /freq, 1001, 1204, 440, 16 ]
				inOnOff.value_(0)
			},'/off',local).fix
		];
	}

	loadSynthDefs {
		synthDefs = [SynthDef(\fmFB,{|freq=1000,mfrq=1,idx=2,amp=0.1,gate=1,vol=0.2|
			var sig,env;
			env = EnvGen.kr(Env.asr(attackTime: 0.001, releaseTime: 0.025),
					gate, doneAction: 2);
			sig = SinOsc.ar(
				SinOsc.ar(
					mfrq,0,
					idx*mfrq,
					freq),0,Ò
				amp*env);
			Out.ar(analyzer.fbBus,sig);
			Out.ar([0,1],sig*vol)
			}),
			SynthDef(\fmDrumFB,{ arg freq=440,rtio=4,idx=2,modFrq=0.1,mod=0,trgf=4,
				phs=0,att=0.01,rls=0.25,amp=0.1,gate=1,vol=0.2;
				var modSig,env,sig;
				env = EnvGen.kr(Env.asr(attackTime: 0.001, releaseTime: 0.025),
					gate, doneAction: 2);
				modSig = SinOsc.kr(modFrq,0,mod*freq);
				sig = Decay2.ar(Impulse.ar(trgf,phs,amp),att,rls,
					PMOsc.ar(freq+modSig,freq*rtio,idx)
				)*env;
				Out.ar(analyzer.fbBus,sig);
				Out.ar([0,1],sig*vol)
			}),
			SynthDef(\pluckFB,{ arg freq=400,brit=2000,plSpd=4,plDcy=1,clip=0.2,rq=0.5,
				mfrq=0.2,mod=1,att=0.01,rls=0.5,amp=0.1,vol=0.2, gate=1;
				var exciter,modSig,sig,bright,env;
				env = EnvGen.kr(Env.asr(attackTime: 0.025, releaseTime: 0.025),
					gate, doneAction: 2);
				bright = brit+freq;
				exciter = WhiteNoise.ar(Decay.kr(Impulse.kr(plSpd), 0.01));
				modSig = SinOsc.kr(mfrq,0,mod*(bright*0.5));
				sig = RLPF.ar(CombC.ar(exciter,0.1,freq.reciprocal,plDcy),
					bright+modSig,rq,amp*env)
				.clip2(clip);
				Out.ar(analyzer.fbBus,sig);
				Out.ar([0,1],sig*vol)
			}),
			SynthDef(\motoRevFB,{|freq=0.5,lfrq=4,fmul=20,wdth=0.2,ffrq=2000,
				clip=1,amp=0.1,gate=1,vol=0.2|
				var env,sig;
				env = EnvGen.kr(Env.asr(attackTime: 0.025, releaseTime: 0.025),
					gate, doneAction:2);
				sig = RLPF.ar(
					LFPulse.ar(
						SinOsc.kr(lfrq,0,fmul,freq*0.005) // freq
					,[0,0.1],wdth), 	// iphase, width
					ffrq,0.1).clip2(clip);  // filt freq, phase, clip
				sig=Mix(sig)*amp*env;
				Out.ar(analyzer.fbBus,sig);
				Out.ar([0,1],sig*vol)
			})
		].collect {|def| def.send(analyzer.s) };
		curSynth=SV.new.sp((0..synthDefs.size),0)
		.items_(synthDefs.collect {|def| def.name.asSymbol });
		// select feedback modulation params
		synthModCtls= (
			fmFB: [\idx,\mfrq,\amp],
			fmDrumFB: [\rtio,\trgf,\amp],
			pluckFB: [\plSpd,\mfrq,\amp],
			motoRevFB: [\lfrq,\ffrq,\amp]
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
			\ffrq->ControlSpec(80,8000,\exp,0,1200)])
	}

	stopAll {
		analyzer.free;
		synth.notNil.if({synth.release});
		netUpdateRoutine.stop;
		OSCdef.freeAll
	}

}


/*

Know-No feedback Instructions:

Sound producing synth(s) play to 1 audio rate Bus which is analyzed for pitch, timbre, and amplitude.
Analysis data is mixed individually by each player with data received from 1 other player,
then published to the network.  Players choose which stream they are currently mixing with their own analysis.
Synth(s) have 3 input control rate buses that are connected to the published 3 feedback parameters of their sound
While playing, choose  different netPlayers to patch their analysis data to your input controls. Manipulate the probability control to change how often the feedback is applied to the synth; manipulate scale control to change its range, and offset to tune that range of change to different values.  Tune the blend knob to mix your feedback with that of your chosen netPlayer.  The result of this feedback data matrix mix process is shown in the numberBox, and in the three sliders in the Synth gui section.  The
last two sliders are entirely manually controlled, allowing you control the frequency range of the synth independent of the feedback, and to control the volume of your local sound output.


*/
	
