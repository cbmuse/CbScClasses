AudioInAnalyzer {
	classvar <synthdefs, <trackerUgens, <analyzerSynthDefs;
	var <>analyzerSynth,<>inputSynth,<>inBus=8,<>outBus=16,<>analyzerName,
	<>w, <ampView,<freqView,<oscdefs,<>trgBtn,runBtn,
	<>pitchThresh=0.96,<>ampThresh=0.1,<>onFlg=false,<>events,
	<>lastPitch=0,<>lastOnTime,<>lastAmp=0,lastSpecCent=0,<>lastDur=0,
	<octaves,<>scale,<>rootFreq=261.63;

	// read audio inputs, write onto bus
	*init {
		Server.default.waitForBoot({
			synthdefs = [
				SynthDef(\audioIn,{|inbus=8, outbus=16|
					var in = In.ar(inbus);
					Out.ar(outbus,in);
				}).add,

				// read mono soundfile from disk
				SynthDef(\audioDiskIn,{|bufnum=0,loop=0,outbus=16|
					var in = DiskIn.ar(1,bufnum,loop);
					Out.ar(outbus,in);
				}).add,

				// track continuous pitch and amplitude from bus
				SynthDef(\audioTrack,{|inbus=16, pThresh=0.96, aThresh=0.1, rate=20|
					var freq, hasFreq, amp,startID=10000,gate;
					var in=In.ar(inbus);
					amp = Amplitude.kr(in);
					# freq, hasFreq = Tartini.kr(in);
					gate = (hasFreq>pThresh)*(amp>aThresh)*Impulse.kr(rate);
					SendReply.kr(gate,'/freq',freq,startID+inbus);
					SendReply.kr(gate,'/amp',amp,startID+60+inbus);
				}).add,

				// track discrete event freq, peak amplitude, and spectral centroid
				SynthDef(\audioAnalyzer,{|inbus=16,buf=0,
					pThresh=0.96,onThresh=0.95,aThresh=0.1,trgDur=0.01,dcy=0.99,offSens=0.25|
					var freq, hasFreq,lagFreq,trig,on,off,onFlg,offFlg;
					var numOnsets = Dseries(0,1,inf);
					// get audio input
					var in = InFeedback.ar(inbus,1);
					// perform FFT analysis on input
					var onsetChain = FFT(LocalBuf(512), in);
					var analyzeChain = FFT(LocalBuf(2048),in);
					var sc = SpecCentroid.kr(analyzeChain);
					var peak = PeakFollower.kr(in,0.99);
					// trigger from input
					#freq, hasFreq = Tartini.kr(in);
					// smooth frequency over trgDur
					lagFreq = Lag.kr(freq,trgDur);
					//send continuous freq values
					SendReply.kr(Impulse.kr(trgDur.reciprocal)*(pThresh<hasFreq)*(peak>aThresh),
						'/freqAnalyze',lagFreq);
				//	trig = Onsets.kr(onsetChain,onThresh)*(pThresh<hasFreq);	// onset has freq
				//	Demand.kr(trig,0,numOnsets).poll(trig);  // count onsets
				//	SendReply.kr(trig,'/onset',[lagFreq,peak,sc,numOnsets]);
					on = Trig1.kr(peak>aThresh,trgDur);
					SendReply.kr(on,'/onAnalyze',peak);
					off = Trig1.kr(peak < (aThresh*offSens),trgDur); // break in ev stream
					SendReply.kr(off,'/offAnalyze',peak);
				}).add
			];
			analyzerSynthDefs = synthdefs.collect {|sd| sd.name }
			.removeAll([\audioIn,\audioDiskIn]);
		});
	}

	*new {| argAnalName=\audioTrack,argInBus=8|
		^super.new.init(argAnalName,argInBus)
	}

	init {|argAnalName,argInBus,argOutBus|
		analyzerName=argAnalName;
		inBus = argInBus;
		events = List.new(100);  // store freq, amp, spectral Centr, ontime with each new onset
		this.makeGui;
		scale = Scale.chromatic;
		scale.tuning_(\just);
		rootFreq = 440;
		this.setOctaves;
	}

	makeGui  {
		var thv;
		w = Window.new("audio analyzer",Rect(5,5,360,300));
		w.view.decorator = FlowLayout(w.view.bounds);
		w.view.background=Color(0.6,0.8,0.8);

		EZSlider(w.view,280@20,"pitchThresh",[0.5,1.0,\exp,0.01].asSpec,initVal: 0.96,labelWidth: 80,
			action: {|v| analyzerSynth.set(\pThresh,v.value); pitchThresh=v.value  });

		trgBtn = Button(w.view,60@20)
		.states_([["no-trg",Color.white,Color.black],["trg",Color.white,Color.red]]);

		thv =EZSlider(w.view,280@20,"ampThresh",
			[0.001,0.5,\exp,0.01,0.01].asSpec, initVal: 0.1,
			labelWidth: 80,
			action: {|v| analyzerSynth.set(\aThresh,v.value); ampThresh=v.value });
		thv.numberView.step_(0.001).decimals_(3);

		runBtn = Button(w.view,60@20)
		.states_([["run",Color.white,Color.black],["pause",Color.white,Color.red]])
		.value_(1)
		.action_({|b| if(b.value == 0,{ analyzerSynth.run },{ analyzerSynth.run(false) })});

		w.view.decorator.nextLine;

		freqView = EZNumber(w.view,100@20,"freq",\freq,labelWidth:40);
		ampView = EZNumber(w.view,100@20,"amp",labelWidth:40);

		Stethoscope.new(Server.default,1,outBus,view:w.view)
		.view.bounds_(Rect(0,80,360,220));
		w.onClose_({ this.quit  });
		w.front;
	}

	// play & analyze one input
	analyzeInput  {|inbus=8,outbus=16|
		inBus=inbus; outBus=outbus;
		inputSynth = Synth(\audioIn,[\inBus,inBus,\outBus,outBus]);
		{ 0.05.wait;
			analyzerSynth = Synth.after(inputSynth,analyzerName,
				[\pThresh,pitchThresh,\aThresh,ampThresh]).run(false); // start paused
		}.fork;
		oscdefs = [
			OSCdef('/onset', {|msg|
				var freq=this.getPitch(msg[3]),amp=msg[4],sc=msg[5],onsetNum=msg[6];
				events.add([freq,amp,sc,thisThread.seconds,onsetNum]);
				lastPitch = freq;
				{ freqView.value_(freq.cpsmidi.round(0.1));
					ampView.value_(amp.round(0.01))}.defer
			}, '/onset'),
			OSCdef('/freq', {|msg|
				var now,startIdx,pitch,freq=msg[3];
			//	msg.postln;
				if(onFlg,{
					pitch = this.getPitch(freq);   // quantize to nearest scale freq
					if(pitch != lastPitch,{        // only use this freq if it's a new pitch in the scale
						// msg.postln;
						lastPitch=pitch;
						events = events.add([pitch,lastAmp,lastSpecCent,now=thisThread.seconds]);
						// remove events rcved more than 1 second ago
						startIdx = events.selectIndices {|ev,i| now - ev[3] > 1 }.last;
						if(startIdx.notNil,{events = events.copyToEnd(startIdx)});
						{ freqView.value_(freq.round(0.1)) }.defer
				})})
			},
			'/freqAnalyze'),    // 'Analyze' is required to avoid conflict with FBnetwork '/freq' messages
			OSCdef( '/amp', {|msg| // msg.postln;
				{ ampView.value_(msg[3].round(0.01)) }.defer },
			'/ampAnalyze'),
			OSCdef( '/on', {|msg| // msg.postln;
				lastAmp = msg[3];
				if(onFlg.not,{
					"start on, time = ".post;
					onFlg = true;
					lastOnTime = thisThread.seconds.round(0.01);
					{ trgBtn.valueAction_(1); ampView.value_(msg[3].round(0.001)) }.defer })},
			'/onAnalyze'),   // 'Analyze' is required to avoid conflict with FBnetwork '/on' messages
			OSCdef( '/off', {|msg| // msg.postln;
				if(onFlg,{
					// "lastDur = ".post;
					onFlg = false;
					lastDur = (thisThread.seconds - lastOnTime);
					{trgBtn.valueAction_(0)}.defer})},
			'/offAnalyze'),     // 'Analyze' is required to avoid conflict with FBnetwork '/off' messages
			OSCdef('/spectralCentroid',{|msg|
				if(onFlg,{
					lastSpecCent= msg[3]
					// msg.postln;
			})},
			'/specCent')
		];
	}

	// get first event, shrink size
	getFirstEvent { var first;
		if(events.notEmpty,{ first=events.first; events.removeAt(0);
			// "events size = ".post; events.size.postln;
		});
		^first
	}

	// get last event, shrink size
	getLastEvent {
		var last,startIdx, now = thisThread.seconds;  // remove unused data older than 1 sec
		startIdx = events.selectIndices {|ev,i| now - ev[3] > 1 }.last;
		if(startIdx.notNil,{events = events.copyToEnd(startIdx)});
		if(events.notEmpty,{ last = events.last; events.removeAt(events.size-1)});
		^last
	}

	setOctaves {
		while({rootFreq >  20},{ rootFreq = rootFreq / 2 }); rootFreq = rootFreq*2;
		octaves = 10.collect{|i| rootFreq*(2**(i)) }
	}

	// quantize freq to nearest freq from scale and tuning and rootFreq
	getPitch {|freq|
		var oct,freqs,freqsCents,degree;
		oct = octaves.detectIndex {|octFreq,i| octFreq>= freq }; oct=oct-1;
		freqs = scale.degrees.size.collect {|i| octaves[oct]*scale.ratios[i] };
		freqsCents = freqs.collect {|f| f.cpsmidi }++[rootFreq*2];
		degree = freqsCents.detectIndex {|f,i| if(i<(freqsCents.size-1),{
			(freq.cpsmidi - f).abs <
			(freqsCents[i+1] - freq.cpsmidi).abs}) };
		^freqs[degree]
	}

	quit { analyzerSynth.free; inputSynth.free; oscdefs.do {|d| d.free } }

}

/*
AudioInAnalyzer.init
a = AudioInAnalyzer.new(\audioAnalyzer).analyzeInput
a.getFirstEvent
a.events.size
a.getLastEvent
SnakeCharmer.aIn.inputSynth.get(\inbus,{|x| x.postln })

a.analyzerSynth.get(\aThresh,{|v| v.postln })
a.inputSynth.set(\in,16)
a.inputSynth.get(\inBus,{|v| v.postln })
a.analyzerSynth.run(false)
Node run
a.freqView.value_(24.00)
a.ampView.value_(0.2340987)
OSCFunc.trace(true)
OSCdef.all
a.oscdefs.do {|d| d.postln }
[1,2,3,4].do {|d| d.postln }
a.events
Synth
0.1*0.04
~scale = Scale.chromatic24
~root = 440;
Find Octave of input to Scale
while({~root >  20},{ ~root = ~root / 2 }); ~root = ~root*2;
~octaves = 10.collect{|i| ~root*(2**(i)) }
~freq = exprand(20,20000)
~oct = ~octaves.detectIndex {|octFreq,i| octFreq>= ~freq }; ~oct=~oct-1;
~freqs = ~scale.degrees.size.collect {|i| ~octaves[~oct]*~scale.ratios[i] }
~freqsCents = ~freqs.collect {|f| f.cpsmidi }++[~root*2]
~freqQtize = ~freqsCents.detectIndex {|f,i| if(i<(~freqsCents.size-1),{(~freq.cpsmidi - f).abs.postln < (~freqsCents[i+1] - ~freq.cpsmidi).abs.postln}) }
~qtizeFreq = ~freqs[~freqQtize]
*/