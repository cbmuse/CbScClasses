FBanalyzer {
	var <numSignals,<amp=0,<freq=440,<specCentroid=2000,<specCentroids,<starts,
	<peaks,<durs,<freqs,<onFlgs,<>bufs,<>fbBus=0,<>ctlBus=0,<>ctlBusInitVals,<>responders,
	<flowView,<gui,<pgui,<>offsets,local,<>lastSignal,<>dests,<lastdur,
	<>fbGroup,<>analyzer,<>player,<>synthDefs,<>network,<>analyzerSynths,<s;

	*new {|argNumSignals=1,argNetwork|
		^super.new.init(argNumSignals,argNetwork)
	}

	init {|argNumSignals=1,argNetwork|
		numSignals=argNumSignals;
		network=argNetwork;
		specCentroids=Array.fill(numSignals,{2000}).postln;
		starts = Array.fill(numSignals,{thisThread.seconds});
		peaks = Array.fill(numSignals,{0});
		durs = Array.fill(numSignals,{1});
		freqs = Array.fill(numSignals,{440});
		onFlgs = Array.fill(numSignals,{false});
		local =  NetAddr("127.0.0.1", 57110); // local server
		if(network.isNil,{
			this.makeAnalyzerGui;
			this.makePlayerGui;
			this.initResponders;
		});
		"initializing FBanalyzer".postln;
		s = Server.default;
		s.latency_(0.04);
		s.waitForBoot({
			bufs = Array.fill(numSignals,{Buffer.alloc(s, 2048, 1)});
			fbBus= Bus.audio(s,numSignals);
			ctlBus = Bus.control(s,3);
			ctlBusInitVals=[0.5,0.2,0.1];
			fbGroup = Group.new(s);
			this.loadSynthDefs;
			this.startAnalyzer
		})
	}

	initResponders {
		"initializing responders ".postln;
		responders = [
			OSCdef(\fbFreq,{|msg,time,addr,port|
				var bus = msg[4]- fbBus.index;
				var val = msg[3];
				msg.postln; //  example: [ /freq, 1001, 1204, 440, 16 ]
				freqs.put(bus,val); freq=val;
				("freq "++bus.asString++" = ").post;
				val = \frq.asSpec.unmap(val);
			  val.postln;
				if(network.notNil,{
					network.paramCtlViews[bus].inFreq.value_(val);
					network.sendParamToAll(\freq,bus,val) });
			},'/freq',local).fix,
			OSCdef(\fbTimbre, {|msg,time,addr,port|
				var bus = msg[4]- fbBus.index; var val = msg[3];
				msg.postln;
				specCentroids.put(bus,val); specCentroid=val;
			("specCentroid " ++ ((bus+1).asString) ++ " = ").post;
				val = \ffrq.asSpec.unmap(val);
			val.postln;
				if(network.notNil,{
					network.paramCtlViews[bus].inTimbr.value_(val);
					network.sendParamToAll(\timbre,bus,val) });
			},'/specCentroid',local).fix,
			OSCdef(\fbAmp, {|msg,time,addr,port|
				var bus = msg[4]- fbBus.index; var val = msg[3];
				msg.postln;
				peaks.put(bus,val); amp=val;
				("peak " ++ ((bus+1).asString)++ " = ").post;
				val = \amp.asSpec.unmap(val);
			val.postln;
				if(network.notNil,{
					network.paramCtlViews[bus].inAmp.value_(val);
					network.sendParamToAll(\amp,bus,val) });
			},'/peak',local).fix,
			OSCdef(\fbOn, {|msg,time,addr,port|
				var bus = msg[4]- fbBus.index; var val = msg[3];
				msg.postln;
				lastSignal=bus;
				starts.put(bus,thisThread.seconds);
				if(onFlgs[bus]==false,{
					onFlgs.put(bus,true);
				//	("on " ++ ((bus+1).asString)).postln;
				})
			},'/on',local).fix,
			OSCdef(\fbOff, {|msg,time,addr,port|
				var bus = msg[4]- fbBus.index; var val = msg[3];
				msg.postln;
				if(onFlgs[bus]==true,{
					onFlgs.put(bus,false);
				//	("off " ++ ((bus+1).asString)).post;
					(" lastdur " ++ ((bus+1).asString)).post; " ".post;
					durs.put(bus,lastdur=(thisThread.seconds- starts[bus]).postln);
					 })
			},'/off',local).fix
		];
	}

	startAnalyzer {|threshold=0.025,trgDur=0.01,dcy=0.99|
		analyzerSynths = numSignals.collect {|i|
			SynthDef(\FBanalyzer,{|fbBus=0,buf=0,
				threshold=0.025,trgDur=0.01,dcy=0.99|
				var freq, hasFreq, fTrig,lagFreq;
				// get audio input
				var in = InFeedback.ar(fbBus,1);
				var peak = PeakFollower.kr(in,dcy);
				var on = Trig1.kr(peak > threshold,trgDur);
				var off = Trig1.kr(peak < 0.0001,trgDur);
				// perform FFT analysis on input
				var chain = FFT(LocalBuf(2048), in);
				var sc = SpecCentroid.kr(chain);
				// trigger from input
				var trig = Onsets.kr(chain,threshold);	// threshold must pass
				#freq, hasFreq = Pitch.kr(in);
				lagFreq = Lag.kr(freq,trgDur);
				fTrig=Trig1.kr(((freq-1)>lagFreq) +(lagFreq>(freq+1)),trgDur);
				SendReply.kr(Trig1.kr(fTrig),'/freq',[i,freq],1204);
				SendReply.kr(Trig1.kr(trig),'/peak',[i,peak],1201);
				SendReply.kr(Trig1.kr(trig),'/specCentroid',[i,sc],1200);
				SendReply.kr(Trig1.kr(off,0.01),'/off',[i,off],1203);
				SendReply.kr(Trig1.kr(trig,0.01),'/on',[i,trig],1202);
			}).play(fbGroup,[\fbBus,fbBus.index+i,\buf,bufs[i],
				\threshold,threshold,\trgDur,trgDur,\dcy,dcy],
				\addToTail);
		}
	}

	changeAnalyzerBus { arg newBus=16;
		fbBus=newBus;
		analyzerSynths.do {|syn,i|
			syn.set(\fbBus,fbBus.index+i)
		};
	}

	playDefaultSynth {
		ctlBus.setn(ctlBusInitVals);
		player = SynthDef(\fbTest,{|freq=1000,modFrq=0,modDpth=0,amp=0|
			Out.ar([0,fbBus],
				SinOsc.ar(
					SinOsc.ar(
						(ctlBus.kr(1,0)+modFrq)*50,0,
						(ctlBus.kr(1,2)+modDpth)*freq,
						freq),
					0,
					(ctlBus.kr(1,1)+amp)*0.5)
			)
		}).play(fbGroup,addAction: \addBefore)
	}

	loadSynthDefs {
		synthDefs = [
			SynthDef(\fmFB,{|freq=1000,mfrq=0,modDpth=0,amp=0.1,vol=0.2|
				var sig;
				sig = SinOsc.ar(
					SinOsc.ar(
						mfrq,0,
						modDpth*freq,
						freq),0,
					amp);
				Out.ar(fbBus,sig);
				Out.ar([0,1],sig*vol)
			}),
			SynthDef(\fmDrumFB,{ arg freq=440,rtio=4,idx=2,modFrq=0.1,modDpth=0,trgf=4,
				phs=0,att=0.01,rls=0.25,amp=0.1,gate=1,vol=0.2;
				var modSig,env,sig;
				env = EnvGen.kr(Env.asr(attackTime: 0.001, releaseTime: 0.025),
					gate, doneAction: 2);
				modSig = SinOsc.kr(modFrq,0,modDpth*freq*0.5);
				sig = Decay2.ar(Impulse.ar(trgf,phs,amp),att,rls,
					PMOsc.ar(freq+modSig,freq*rtio,idx)
				)*env;
				Out.ar(fbBus,sig);
				Out.ar([0,1],sig*vol)
			}),
			SynthDef(\pluckFB,{ arg freq=400,brit=2000,plSpd=4,plDcy=1,clip=0.2,rq=0.5,
				modFrq=0.2,modDpth=1,att=0.01,rls=0.5,amp=0.1,vol=0.2, gate=1;
				var exciter,modSig,sig,bright,env;
				env = EnvGen.kr(Env.asr(attackTime: 0.025, releaseTime: 0.025),
					gate, doneAction: 2);
				bright = brit+freq;
				exciter = WhiteNoise.ar(Decay.kr(Impulse.kr(plSpd), 0.01));
				modSig = SinOsc.kr(modFrq,0.0,modDpth*(bright*0.5));
				sig = RLPF.ar(CombC.ar(exciter,0.1,freq.reciprocal,plDcy),
					modSig,rq,amp*env)
				.clip2(clip);
				Out.ar(fbBus,sig);
				Out.ar([0,1],sig*vol)
			}),
			SynthDef(\motoRevFB,{|freq=0.5,mfrq=4,modDpth=1,wdth=0.2,ffrq=2000,
				clip=1,amp=0.1,gate=1,vol=0.2|
				var env,sig;
				env = EnvGen.kr(Env.asr(attackTime: 0.025, releaseTime: 0.025),
					gate, doneAction:2);
				sig = RLPF.ar(LFPulse.ar(
					SinOsc.kr(mfrq,0,modDpth,freq*0.125) // freq
					,[0,0.1],wdth), 	// iphase, width
				ffrq,0.1).clip2(clip);  // filt freq, phase, clip
				sig=sig*amp*env;
				Out.ar(fbBus,sig);
				Out.ar([0,1],sig*vol)
			})
		].collect {|def| def.send(s) }
	}

	makePlayerGui {
		flowView.startRow;
		pgui = flowView.flow({|p|
			p.startRow;
			EZSlider.new(p, 400@20,\modFrq,nil,{|sl|player.set(\modFrq,sl.value)});
			p.startRow;
			EZSlider.new(p, 400@20,\modDpth,nil,{|sl|player.set(\modDpth,sl.value)});
			p.startRow;
			EZSlider.new(p, 400@20,\synthAmp,nil,{|sl|player.set(\amp,sl.value)});
			p.startRow;
			Button.new(p,80@20).states_([["SynthOn"],["SynthOff"]])
			.action_({|b| switch(b.value,
				0,{ player.free },
				1,{ this.playDefaultSynth })})
		},410@120).background_(Color.white);
	}

	setControl {|which=0,val=0|
		ctlBusInitVals.put(which,val);
		ctlBus.setn(ctlBusInitVals)
	}

	makeAnalyzerGui {
		var labels,header,guiCtls;
		dests = [2,1,0];
		flowView = FlowView.new(nil,Rect(8,60,500,404),4@4,1@1,"knowNo");
		labels = ["freq","specCent","amp"];
		header = StaticText(flowView,500@20)
		.string_("               input                          prob           scaling        dest offset");
		guiCtls = 3.collect {|i|
			if(i>0,{flowView.startRow }); StaticText(flowView,58@20).string_(labels[i]);
			[Slider(flowView,142@20),   // data input
				Slider(flowView,72@20),    // filterweight
				Slider(flowView,72@20),    // scale
				NumberBox(flowView,Rect(0,0,30,20)) //assgn#
				.align_(\center).value_(dests[i]),
				Knob.new(flowView,25@25).value_(0.5), // offset
				NumberBox(flowView,Rect(0,0,50,20)),
				if(i==0,{Button.new(flowView,40@20)  // on-off
				.states_([[ "OFF", Color(1.0, 1.0, 1.0, 1.0), Color(0.0, 0.0, 1.0, 1.0) ],
					[ "ON", Color(0.0, 0.0, 0.0, 1.0), Color(1.0, 0.0, 0.0, 1.0) ]]).value_(0)})
			]
		};
		offsets = Array.fill(3,{0.5});
		guiCtls.do {|tGui,i|
			tGui[0].action_({|ctl|
				if(tGui[1].value.coin,{ // rand filter gate
					var val = ((ctl.value+tGui[4].value)*tGui[2].value).wrap(0,1);
					this.setControl(tGui[3].value.asInt,val);
					tGui[5].value_(val);  // display output
				})
			});
			tGui[4].action_({|ctl|
				dests.put(i,ctl.value.asInt); }); // which slider
			tGui[3].action_({|v| offsets.put(i,v.value)}) // offsets
		}
	}

}

/*
~network.analyzers.specCentroids
x.startAnalyzer
x.fbGroup = Group.new(s)Ri
CBStartup
\clip.asSpec
\effOut.asSpec
RitmosSpecs
SynthDef(\fbTest,{Out.ar(0,SinOsc.ar(MouseX.kr(30,3000), 0, 0.4}).send(s);
RitmosPlayer

Spec.specs[\clip]
ServerOptions
s.options.blockSize_(16)
(
SynthDef("help-InFeedback", { arg out=0, in=0;
    var input, sound;
        input = InFeedback.ar(in, 1);
        sound = SinOsc.ar(input * 1300 + MouseX.kr(30,3000), 0, 0.4);
        Out.ar(out, sound);

}).play;
)
*/