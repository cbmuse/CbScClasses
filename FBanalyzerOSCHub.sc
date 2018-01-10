FBanalyzerOSCHub {
	var <numSignals,<>network,s,
	<amp=0,<freq=440,<specCentroid=2000,<specCentroids,<starts,
	<peaks,<durs,<freqs,<onFlgs,<>bufs,<>fbBus=0,<>ctlBus=0,<>ctlBusInitVals,
	<>responders,<flowView,<gui,<pgui,<>offsets,local,<>lastSignal,<>dests,<lastdur,
	<>fbGroup,<>analyzer,<>player,<>synthDefs;

	*new {|numSignals=1,network|
		^super.newCopyArgs(numSignals,network).init
	}

	init {
		specCentroids=Array.fill(numSignals,{2000});
		starts = Array.fill(numSignals,{thisThread.seconds});
		peaks = Array.fill(numSignals,{0});
		durs = Array.fill(numSignals,{1});
		freqs = Array.fill(numSignals,{440});
		onFlgs = Array.fill(numSignals,{false});
		local =  NetAddr("127.0.0.1", 57110); // local server
	}

	startAnalyzer {|threshold=0.025,trgDur=0.01,dcy=0.99|
		s = Server.local;
		bufs = Array.fill(numSignals,{Buffer.alloc(s, 2048, 1)});
		fbBus = 64+((network.netPlayers.size-1)*4); // max 4 signals/player, busses 64-127 = room for 16 players
		ctlBus = Bus.control(s,3);
		ctlBusInitVals=[0.5,0.2,0.1];
		fbGroup = Group.new(s,\addToTail);

		SynthDef(\FBanalyzer,{|fbBus=0,buf=0,vcNum=0,threshold=0.025,trgDur=0.01,dcy=0.99|
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
			SendReply.kr(Trig1.kr(fTrig),'/freq',[vcNum,freq],1204);
			SendReply.kr(Trig1.kr(trig),'/peak',[vcNum,peak],1201);
			SendReply.kr(Trig1.kr(trig),'/specCentroid',[vcNum,sc],1200);
			SendReply.kr(Trig1.kr(off,0.01),'/off',[vcNum,off],1203);
			SendReply.kr(Trig1.kr(trig,0.01),'/on',[vcNum,trig],1202);
		}).send(s);
		// avoid 1001 nodeID which is used for OSCHub chat's pong sound
		numSignals.do {|i|
			var synth = Synth.basicNew(\FBanalyzer,s,1002+i);
			s.sendBundle(1,
				synth.newMsg(fbGroup,[\fbBus,fbBus+i,\buf,bufs[i],\vcNum,i,\threshold,threshold,
					\trgDur,trgDur,\dcy,dcy],\addToTail));
		}

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
(
SynthDef("help-InFeedback", { arg out=0, in=0;
    var input, sound;
        input = InFeedback.ar(in, 1);
        sound = SinOsc.ar(input * 1300 + MouseX.kr(30,3000), 0, 0.4);
        Out.ar(out, sound);

}).play;
)
*/