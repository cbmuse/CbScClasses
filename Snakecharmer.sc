Snake {			// 	input pitches forced within window limits, window moves up and down
	var <>input,  // audio input analyzer
	<>player,  // TxPlayer
	<>voice, // TxVc preset or name
	<>behavior = \snake,   // types are \snake,\scaleEcho,\xposeEcho
	<>scale=\chromatic, // array of ratios, input pitch is forced to closest pitch class within it
	<>tonic=261.6255653006, // scale base freq Middle C
	<>tempo=4,  // in beats per second  = 240 mm
	<>dur=1,  // holds single value or Pseq
	<>pan=0,
	<>vol=0.25,  // TxVc function arg
	<name;
	// windowing variables
	var <>wLimLo,	//	bottom limit of pitch window
	<>wLimHi,	//	low limit of pitch window
	<>wEndLo,	//	lowest possible value for wLimLo
	<>wEndHi,	//	highest possible value for wLimHi
	<>wDir,	//	window change direction, 1 = up, 0 = stable, -1 = down
	<>wIncr,	// 	size of window change, in midiNotes
	<> transits=0,	// 	counts transits from lo to hi, and hi to lo
	<> transitsLim=2, // a single Snake goes down, then returns up
	<> endFlg=0, 	//  set flag when transits are done
	//
	<params,<>xposeInt=0,<octaves,<curTonic,<vocoder,<>playerNum,<lastPitch,
	<>delay,<>sus=1,<snakeTask,<>startFunc,<>stopFunc,<>state=\begin;


	*new {|input,player,voice=\gazimba,behavior=\playScale,scale=\chromatic,tonic=220,
		tempo=4,dur=1,pan=0,vol=0.5,name=\sn1|
		^super.newCopyArgs(input,player,voice,behavior,
			scale,tonic,tempo,dur,pan,vol,name).init;
	}

	init {
		playerNum = name.asString[2].digit-1;
		startFunc = { SnakeCharmer.score[name.asString[4].digit-1][playerNum].value_(1) };
		stopFunc = {  SnakeCharmer.score[name.asString[4].digit-1][playerNum].value_(4) };
		if(scale.isSymbol,{ scale = Scale.all[scale].ratios });
		this.setOctaves;  // fill octave boundary frequencies for given tonic freq
		curTonic=tonic;
		lastPitch=tonic;  // init lastPitch to tonic
		this.setTempo(tempo);
		// shift input signal to match last snake pitch
		SynthDef(\voc++name,{|inBus=8,ratio=1.0,amp=0.1,gsize=0.1,pan=0,out=0,vol=0.25|
			var shift = PitchShift.ar(
				In.ar(inBus),gsize,      // grain size
				ratio,    // snakeFrq/aInFrq = ratio
				0,                // pitch dispersion
				0.004            // time dispersion
			);
			Out.ar(out,Pan2.ar(shift,pan,vol))
		}).add;
	}

	storeParams {
		params = [dur,pan,tempo,tonic,transits,transitsLim,vol,wDir,
			wEndHi,wEndLo,wIncr,wLimHi,wLimLo];

	}

	restoreParams { dur=params[0]; pan=params[1]; tempo=params[2];
		tonic=params[3]; transits=params[4]; transitsLim=params[5]; vol=params[6];
		wDir=params[7]; wEndHi=params[8]; wEndLo=params[9]; wIncr=params[10];
		wLimHi=params[11];  wLimLo=params[12];
	}

	setTempo {|temp|
		tempo=temp; delay=tempo.reciprocal
	}

	pitchQuantize { arg freq;
		var wScale, which;
		freq = this.window(freq);	// force frequency to closest scale note within wLimLo-wLimHi
		curTonic = this.window(tonic);	// find tonic within the window;
		wScale = scale.collect({ arg item, i; // collect scale within window, curTonic
			var pitch;
			pitch = item *curTonic;
			this.window(pitch)
		}).sort({ arg a,b; a <b; });	// and sort scale low to hi
		wScale.detect({ arg item, i;
			which = i; (freq - item) < 0	// flag first scale note > freq
		});
		if (which == 0,{ freq = wScale.first; },{
			if ( which == nil,{ freq = wScale.last },{
				if ( (freq - wScale.at(which-1)).abs > (freq - wScale.at(which)).abs,{
					freq = wScale.at(which) },{ freq = wScale.at(which-1)
				})
			})
		});
		^freq
	}

	// changes 'tonic' to lowest octave tonic frequency
	setOctaves {
		while({tonic >  20},{ tonic = tonic / 2 }); tonic = tonic*2;
		octaves = 10.collect{|i| tonic*(2**(i)) }
	}

	playScale {|freq|
		var oct,freqs,freqsCents,degree;
		oct = octaves.detectIndex {|octFreq,i| octFreq>= freq }; oct=oct-1;
		freqs = scale.collect {|rtio,i| octaves[oct]*rtio };
		freqs = freqs ++ (freqs[0]*2);  // include 1st note of next upper octave
		freqsCents = freqs.collect {|f| f.cpsmidi };
/*		"freq = ".post; freq.cpsmidi.post;
		" freqsCents = ".post; freqsCents.postln;*/
		degree = freqsCents.detectIndex {|f,i|
			(i <(freqsCents.size-1)) && (freq.cpsmidi-f <= 0) };
		if(degree.isNil,{ degree = freqsCents.size-1 });
		if(degree != (freqsCents.size-1),{
			if( (freq.cpsmidi - freqsCents[degree]) >
				(freqsCents[degree+1] - freq.cpsmidi),
				{ degree = degree+1 });
		}); "degree = ".post; degree.postln;
		^freqs[degree]
	}

	xpose { arg freq;
		var oct,freqs,freqsCents,degree,xposeOct;
		oct = octaves.detectIndex {|octFreq,i| octFreq>= freq }; oct=oct-1;
		"oct = ".post; oct.postln;
		freqs = scale.collect {|rtio,i| octaves[oct]*rtio };
		freqs = freqs ++ (freqs[0]*2);  // include 1st note of next upper octave
		freqsCents = freqs.collect {|f| f.cpsmidi };
			degree = freqsCents.detectIndex {|f,i|
			(i <(freqsCents.size-1)) && (freq.cpsmidi-f <= 0) };
		if(degree.isNil,{ degree = freqsCents.size-1 });
		if(degree != (freqsCents.size-1),{
			if( (freq.cpsmidi - freqsCents[degree]) >
				(freqsCents[degree+1] - freq.cpsmidi),
				{ degree = degree+1 });
		}); // " degree = ".post; degree.postln;
		xposeOct = ((degree+xposeInt)/(scale.size)).floor;
		"xposeOct = ".post; xposeOct.post;
		if(xposeOct > 0,{ freqs = freqs.collect {|f,i| (xposeOct+1)*f }});
		degree = (degree+xposeInt).mod(scale.size);
		" xPosdegree = ".post; degree.postln;
		^freqs[degree]
	}

	window { arg freq;	//	force frequency to octave equivalent within wLimLo and wLimHi
		if ( freq < wLimLo,{
			(((wLimLo.cpsmidi - freq.cpsmidi)/12).asInt+1).do({ freq =2*freq });
		},{ if ( freq > wLimHi,{
			(((freq.cpsmidi - wLimHi.cpsmidi)/12).asInt+1).do({ freq=freq/2 });
		})
		});
		^freq
	}

	moveWindow { 	// move wLimLo and wLimHi by wIncr steps of octave
		var incr;
		"wLimHi= ".post; wLimHi.postln;
		"wLimLo= ".post; wLimLo.postln;
		"wDir= ".post; wDir.postln;
		"wEndHi= ".post; wEndHi.postln;
		"wEndLo= ".post; wEndLo.postln; "".postln;
		if ( (wDir != 0) && (endFlg==0),{
			if (wDir == -1,{
				wLimLo = (wLimLo.cpsmidi-wIncr).midicps.max(wEndLo);
				wLimHi = wLimLo*2;
				if (wLimLo == wEndLo,{ transits = transits+1; wDir = 1;
					if(transits==transitsLim,{ endFlg = 1; })
				});
			},{
				wLimLo = (wLimLo.cpsmidi+wIncr).midicps.min(wEndHi);
				wLimHi = wLimLo*2;
				if (wLimHi >= wEndHi,{ transits = transits+1; wDir = -1;
					if(transits==transitsLim,{ endFlg = 1; })
				});
			})
		});
	}

	start {
		state = \run;
		this.storeParams;  // store starting param values
		SnakeCharmer.curSnakes[playerNum] ?? { SnakeCharmer.curSnakes[playerNum].stop};
		SnakeCharmer.curSnakes.put(playerNum,this);
		vocoder = Synth(\voc++name,[\inBus,SnakeCharmer.inputBusNum,
			\pitch,lastPitch,\freq,input.lastPitch,\out,SnakeCharmer.outputBusNum,  // set vocoder output bas
			\vol,0,\pan,[-0.65,-0.2,0.2,0.65][playerNum]] // spread pan pos per sn1-4
		);

		snakeTask = Task({
			var freq,amp,sc,time,ev,pitch,vocRatio,susValue;
			player.loadPreset(voice);
			startFunc.notNil.if({{startFunc.value}.defer});
			while({endFlg == 0},{
				ev = input.getLastEvent;  // get last received event
				susValue = dur.next;
				if(ev.notNil,{
					freq=ev[0]; amp=ev[1]; sc=ev[2]; time=ev[3];
					switch(behavior,
						\snake,{ this.moveWindow; // change output octave
							pitch = this.pitchQuantize(freq) }, // pitchQuantize
						\scaleEcho,{ pitch = this.playScale(freq) },
						\xposeEcho,{ pitch = this.xpose(freq) });
					name.post; " new pitch= ".post; pitch.post;
					" curTonic = ".post; curTonic.postln;  "".postln;
					amp=amp*SnakeCharmer.ampMults[playerNum].value;
					player.playGuiSynth(pitch,amp,susValue*(rrand(sus*0.90,sus*1.1)));  // vary sustain length
					player.fmArgs[\freq].value_(pitch);
					player.fmArgs[\amp].value_(amp);
					player.fmArgs[\sustain].value_(susValue*sus);
					lastPitch=pitch;
					vocRatio=pitch/input.lastPitch;
					if(vocRatio>4.0,{while({vocRatio>4.0},{vocRatio=vocRatio*0.5})},{
						if(vocRatio<0.25,{while({vocRatio<0.25},{vocRatio=vocRatio*2})})});
					vocoder.set(\ratio,vocRatio,\amp,amp);
				});
				(delay*susValue).wait;
			});
			name.post; " end".postln; this.stop;
		}).start(quant:1)
	}

	stop {
		"stopping ".post; name.postln;
		snakeTask.stop; state=\stop;
		vocoder.free;
		stopFunc.notNil.if({{stopFunc.value}.defer});  // display Button state
		SnakeCharmer.curSnakes.put(playerNum,nil);
		SnakeCharmer.snakeOrder[playerNum].put(name.asString.last.digit-1,nil);
		if((SnakeCharmer.fbNetwork.notNil) && (playerNum == 3),{  // load next fb preset
			{ SnakeCharmer.fbNetwork.presets.curPrstGUI
				.valueAction_(name.asString.last.digit+1) }.defer
		});
	}

	reset { 		// reset variables after a section
		this.restoreParams; // restore starting param values, so can retrigger
		endFlg=0;
		snakeTask ?? { snakeTask.reset };
	}
}

SnakeCharmer {
	classvar <aIn,<>snakes,<>curSnakes,<window,<>score,<sliders,<tasks,<streams,
	<>sn1,<>sn2,<>sn3,<>sn4,<>snakeOrder,<>vocoderVolumes,<>ampMults,<>fbNetwork,
	<>fbFunction,<>outputBusNum=0,<>inputBusNum=8;

	*init {|numAudioOutputs=2,outOffset=0,inOffset=8,vcsPath|
		outputBusNum=outOffset; inputBusNum=inOffset;
		TxSusSynthDefs.numOuts_(numAudioOutputs);
		TxSusSynthDefs.init;
		TxSynthDefs.numOuts_(numAudioOutputs);
		TxSynthDefs.init;
		{ while({TxSynthDefs.synthDefs.isNil},{ 0.1.wait });
			{AudioInAnalyzer.init}.defer;  // boot and load SynthDefs and OSCdefs
			{ while({AudioInAnalyzer.analyzerSynthDefs.isNil},{ 0.1.wait });
				// start inputSynth and analyzerSynth
				{ aIn =  AudioInAnalyzer.new(\audioAnalyzer) }.defer;  // set analyzer synthname and other variables, open window
				while({aIn.isNil},{ 0.1.wait });
				aIn.analyzeInput(inputBusNum,outputBusNum+inputBusNum);  // reads from input, writes to private output bus
				while({aIn.analyzerSynth.isNil},{0.1.wait });
				{ this.loadSnakeFMVoices(vcsPath); }.defer;
				while({sn4.isNil},{ 0.1.wait });
				this.makeSnakes;
				curSnakes=Array.fill(4,{nil});
				sliders = [];
				vocoderVolumes = Array.fill(4,{CV.new(\vol,0.25)});
				ampMults  = Array.fill(4,{CV.new([0.01,10,\exp,0,1.0].asSpec,0.001)});
				{this.makeWindow}.defer;
				while({sliders.size < 8 },{ 0.1.wait });
				fbNetwork = KnowNoNetwork.new(\snakeCharmer,
						oscAddr: "localhost",numVcs:4,synthsFlg:false);
				while({fbNetwork.isNil},{ 0.1.wait });
				fbFunction = this.makeFbFunction;
				while({fbNetwork.paramCtlViews.isNil},{ 0.1.wait });
				fbNetwork.paramCtlViews.do {|view|
					view.ctlPlayerActions =  [fbFunction,fbFunction,fbFunction]};
				this.makeTaskSeq;
				this.scoreMidiInit;
			}.fork;
		}.fork
	}

	*start {

		streams = tasks.do {|t| t.start }
	}

	*stop {
		curSnakes.do {|sn| if(sn.notNil,{ sn.stop })};
		streams.do {|strm| strm.stop.reset };
		tasks.do {|t| t.reset };
	}

	*pause {
		curSnakes.do {|sn| if(sn.notNil,{ sn.snakeTask.pause })};
	}

	*resume {
		curSnakes.do {|sn| if(sn.notNil,{ sn.snakeTask.resume })};
	}

	*makeFbFunction {
		^{|view,dest,val|
			var thisVoice,spec,param;
			// view.post; " ".post; dest.post; " ".post; val.post;  " ".post;
			dest=dest.asInt.mod(7);
			thisVoice=view.vcNum;
			thisVoice=
			[SnakeCharmer.sn1,SnakeCharmer.sn2,SnakeCharmer.sn3,SnakeCharmer.sn4][thisVoice];
			param = switch(dest.asInt,
				0,{\pan},
				1,{\pmod },
				2,{\lfoSpd},
				3,{\amps},
				4,{\fb},
				5,{\waves},
				6,{\lfoWvs});
			val = switch(dest.asInt,
				0,{\pan.asSpec.map(val)},
				1,{val},
				2,{[0,20,\lin,0].asSpec.map(val)},
				3,{thisVoice.fmArgs[\amps].value.put(3,val)},
				4,{val},
				5,{thisVoice.fmArgs[\waves].value.put(0,[0,7].asSpec.map(val))},
				6,{thisVoice.fmArgs[\lfoWvs].value.put(3,val)},
			);
			thisVoice.fmArgs[param].value_(val)
		}
	}

	*makeWindow {
		var q,c,b,ez;
		window = Window("SnakeCharmer",Rect(368, 0, 650, 293)).front;
		q = window.addFlowLayout(10@10,10@5);
		score = 6.collect {|j|
			c = 4.collect {|i| var name;
				b= Button.new(window.view,150@20).states_([
					[name = ("sn"++((i+1).asString)++"_"++((j+1).asString)),
						Color.black,Color.clear],
					[name,Color.black,Color.green],
					[name,Color.black,Color.yellow],
					[name,Color.black,Color.blue],
					[name,Color.black,Color.red]
				])
				.action_({|bt| switch(bt.value,
					0,{ "reset ".post; snakes[name.asSymbol.postln].reset },
					1,{ if((snakes[name.asSymbol].state ==\stop) ||
						(snakes[name.asSymbol].state == \begin),{
							"start ".post; snakes[name.asSymbol.postln].start })},
					2,{ "pause ".post; snakes[name.asSymbol.postln].snakeTask.pause },
					3,{ "resume ".post; snakes[name.asSymbol.postln].snakeTask.resume },
					4,{ if(snakes[name.asSymbol].state ==\run,{
						"stop ".post; snakes[name.asSymbol.postln].stop }) }

				)});
				b };
			q.nextLine; c
		};
		Button(window.view,310@20).states_([
			["start",Color.black,Color.green],["stop",Color.black,Color.red]
		])
		.action_({|btn| switch(btn.value,
			0,{  this.stop },1,{ this.start })});
		Button(window.view,310@20).states_([
			["playing",Color.black,Color.green],["paused",Color.black,Color.yellow]
		])
		.action_({|btn| switch(btn.value,
			0,{ this.resume },1,{  this.pause  })});
		q.nextLine;

		StaticText(window.view,60@20).string_("sn1-Vol");
		sliders=sliders.add(Slider(window.view,240@20).action_({|sl|
			SnakeCharmer.ampMults[0].input_(sl.value)  }));
		StaticText(window.view,60@20).string_("sn2-Vol");
		sliders=sliders.add(Slider(window.view,240@20).action_({|sl|
			SnakeCharmer.ampMults[1].input_(sl.value)  }));
		q.nextLine;
		StaticText(window.view,60@20).string_("sn3-Vol");
		sliders=sliders.add(Slider(window.view,240@20).action_({|sl|
			SnakeCharmer.ampMults[2].input_(sl.value)  }));
		StaticText(window.view,60@20).string_("sn4-Vol");
		sliders=sliders.add(Slider(window.view,240@20).action_({|sl|
			SnakeCharmer.ampMults[3].input_(sl.value)  }));
		q.nextLine;

		StaticText(window.view,60@20).string_("voc1-Vol");
		sliders=sliders.add(Slider(window.view,240@20)
			.action_({|sl|vocoderVolumes[0].input_(sl.value) }));
		StaticText(window.view,60@20).string_("voc2-Vol");
		sliders=sliders.add(Slider(window.view,240@20)
			.action_({|sl| vocoderVolumes[1].input_(sl.value) }));
		q.nextLine;
		StaticText(window.view,60@20).string_("voc3-Vol");
		sliders=sliders.add(Slider(window.view,240@20)
			.action_({|sl| vocoderVolumes[2].input_(sl.value) }));
		StaticText(window.view,60@20).string_("voc4-Vol");
		sliders=sliders.add(Slider(window.view,240@20)
			.action_({|sl| vocoderVolumes[3].input_(sl.value) }));

		ampMults.do {|cv,i| cv.connect(sliders[i])};
		vocoderVolumes.do {|cv,i| cv.connect(sliders[i+4]);
			cv.action_({ curSnakes[i].notNil.if({
				curSnakes[i].vocoder.set(\vol,cv.value) })})};

		window.onClose_({ SnakeCharmer.stop; aIn.quit  });
	}

	*scoreMidiInit {
		MIDIdef.cc (("SnakeCharmer_cc").asSymbol,{|val,num,chan,src|
			var susSpec = ControlSpec(0.125,16.0,\exp,0,1.0);
			switch(num,
				// "cc ".post; " ".post; [num,val,chan].post; " ".postln;
				// snakeFM 1-4
				20,{if(sn1.notNil,{sn1.fmArgs[\vol].value_(val/127)})},
				21,{if(sn2.notNil,{sn2.fmArgs[\vol].value_(val/127)})},
				22,{if(sn3.notNil,{sn3.fmArgs[\vol].value_(val/127)})},
				23,{if(sn4.notNil,{sn4.fmArgs[\vol].value_(val/127)})},
				24,{ if(curSnakes[0].notNil,{curSnakes[0].sus_(susSpec.map(val/127))})},
				25,{if(curSnakes[1].notNil,{curSnakes[1].sus_(susSpec.map(val/127))})},
				26,{if(curSnakes[2].notNil,{curSnakes[2].sus_(susSpec.map(val/127))})},
				27,{if(curSnakes[3].notNil,{curSnakes[3].sus_(susSpec.map(val/127))})},

				30,{ ampMults[0].input_(val/127) },
				31,{ ampMults[1].input_(val/127) },
				32,{ ampMults[2].input_(val/127) },
				33,{ ampMults[3].input_(val/127) },
				// snakeVocoder 1-4
				34,{ vocoderVolumes[0].input_(val/127)},
				35,{ vocoderVolumes[1].input_(val/127) },
				36,{ vocoderVolumes[2].input_(val/127) },
				37,{ vocoderVolumes[3].input_(val/127) },

				80,{ var p = if(val==127,{2},{3});    // pause and resume
					{this.nextSnakeBtn(0).valueAction_(p)}.defer },
				81,{ var p = if(val==127,{2},{3});
					{this.nextSnakeBtn(1).valueAction_(p)}.defer },
				82,{ var p = if(val==127,{2},{3});
					{this.nextSnakeBtn(2).valueAction_(p)}.defer },
				83,{ var p = if(val==127,{2},{3});
					{this.nextSnakeBtn(3).valueAction_(p)}.defer },

				103,{ if(val==127,{{this.nextSnakeBtn(0).valueAction_(1)}.defer}) }, // on
				104,{ if(val==127,{{this.nextSnakeBtn(1).valueAction_(1)}.defer}) },
				105,{ if(val==127,{{this.nextSnakeBtn(2).valueAction_(1)}.defer}) },
				106,{ if(val==127,{{this.nextSnakeBtn(3).valueAction_(1)}.defer}) }, // off
				107,{ if(val==127,{{this.nextSnakeBtn(0).valueAction_(4)}.defer}) },
				108,{ if(val==127,{{this.nextSnakeBtn(1).valueAction_(4)}.defer}) },
				40,{ if(val==127,{{this.nextSnakeBtn(2).valueAction_(4)}.defer}) },
				41,{ if(val==127,{{this.nextSnakeBtn(3).valueAction_(4)}.defer}) },

				116,{ if(val==127,{ this.stop } ) },
				117,{ if(val==127,{ this.start }) },
				// pause
				118,{ if(val==127,{ this.pause},{ this.resume  })
				});
		}).fix;
	}


	*loadSnakeFMVoices {|argPath|
		var path = argPath ?? { "/Users/chrisbrown/CB-SC3/snakecharmer/snake FM voices/snakeVoices3.24.scd"};
		sn1=TxPlayer.new(\sn1FM,1,path);
		sn1.ampScale_(CurveWarp([1,0.25],-4));  // scale amplitude for piano keyboard
		sn1.fmArgs[\out].value_(0);
		sn1.win.bounds_(Rect(6, 594, 710, 261));

		sn2=TxPlayer.new(\sn2FM,2,argPrstPath:path);
		sn2.ampScale_(CurveWarp([1,0.25],-4));  // scale amplitude for piano keyboard
		sn2.fmArgs[\out].value_(0);
		sn2.win.bounds_(Rect(720, 596, 710, 261));

		sn3=TxPlayer.new(\sn3FM,3,argPrstPath: path);
		sn3.ampScale_(CurveWarp([1,0.25],-4));  // scale amplitude for piano keyboard
		sn3.fmArgs[\out].value_(0);
		sn3.win.bounds_(Rect(6, 310, 710, 261));

		sn4=TxPlayer.new(\sn4FM,4,argPrstPath: path);
		sn4.ampScale_(CurveWarp([1,0.25],-4));  // scale amplitude for piano keyboard
		sn4.fmArgs[\out].value_(0);
		sn4.win.bounds_(Rect(720, 310, 710, 261));
	}

	// |input,player,voice,behavior,scale,tonic=60.midicps,tempo=4,dur=1,pan=0,vol=0.5,name|
	*makeSnakes {
		"snake players are ".post; sn1.postln; sn2.postln; sn3.postln; sn4.postln;
		^snakes = (
			// snake1
			sn1_1: Snake.new(aIn,sn1,\symballs1_1,\snake,Scale.chromatic.tuning_(\just).ratios,
				60.midicps,4,0.2,-0.25,0.5,\sn1_1)
			.wLimLo_(96.midicps).wLimHi_(108.midicps).wEndLo_(24.midicps).wEndHi_(108.midicps)
			.wDir_(-1).wIncr_(0.25).sus_(8),
			// scale = 5th (restrict to pitch-classes 7 and 0)
			sn1_2: Snake.new(aIn,sn1,\symballs1_2,\snake,Scale([0,7],tuning: \just).ratios,
				60.midicps,4,0.4,-0.25,0.5,\sn1_2)
			.wLimLo_(84.midicps).wLimHi_(96.midicps).wEndLo_(36.midicps).wEndHi_(96.midicps)
			.wDir_(-1).wIncr_(0.15).sus_(0.5),
			// dorian scale
			sn1_3: Snake.new(aIn,sn1,\dblBass1_3,\snake,Scale([0,3,7,10],tuning: \just).ratios,
				60.midicps,4,2.0,-0.25,0.25,\sn1_3)
			.wLimLo_(60.midicps).wLimHi_(72.midicps).wEndLo_(48.midicps).wEndHi_(72.midicps)
			.transitsLim_(4).wDir_(-1).wIncr_(0.25).sus_(0.5),
			// dorian scale
			sn1_4: Snake.new(aIn,sn1,\handDrum3,\scaleEcho,Scale.dorian.tuning_(\just).ratios,
				60.midicps,4,0.5,-0.25,0.5,\sn1_4),
			sn1_5: Snake.new(aIn,sn1,\waterGlass1_5,\scaleEcho,Scale.dorian.tuning_(\just).ratios,
				60.midicps,4,0.5,-0.25,0.5,\sn1_5).sus_(0.35),
			sn1_6: Snake.new(aIn,sn1,\waterGlass1_6,\xposeEcho,Scale.chromatic.tuning_(\just).ratios,
				60.midicps,4,0.25,-0.25,0.5,\sn1_6).xposeInt_(2).sus_(4),

			// snake2
			sn2_1: Snake.new(aIn,sn2,\symballs2_1,\snake,Scale.chromatic.tuning_(\just).ratios,
				60.midicps,4,0.4,0.25,0.5,\sn2_1)
			.wLimLo_(96.midicps).wLimHi_(108.midicps).wEndLo_(24.midicps).wEndHi_(108.midicps)
			.wDir_(-1).wIncr_(0.35).sus_(4),
			// aug4 scale  = [ tonic, p5, maj6 ] (dorian without m3)
			sn2_2: Snake.new(aIn,sn2,\wDrum2_2,\snake,Scale([0,7,9],tuning: \just).ratios,
				60.midicps,4,1.2,0.25,0.5,\sn2_2)
			.wLimLo_(84.midicps).wLimHi_(96.midicps).wEndLo_(36.midicps).wEndHi_(96.midicps)
			.wDir_(-1).wIncr_(0.4).sus_(2),
			// min7 scale = restrict to 10,7, 3, 0
			sn2_3: Snake.new(aIn,sn2,\wDrum2_3,\scaleEcho,Scale([0,3,7,10],tuning: \just).ratios,
				60.midicps,4,Pseq([2,2,1,1],inf).asStream,0.25,0.5,\sn2_3),
			sn2_4: Snake.new(aIn,sn2,\xylo2_4,\scaleEcho,Scale.dorian.tuning_(\just).ratios,
				60.midicps,4,Pseq([1,1,0.5,0.5],inf).asStream,0.25,0.5,\sn2_4),
			sn2_5: Snake.new(aIn,sn2,\xylo2_5,\xposeEcho,\chromatic,
				60.midicps,4,Prand([1,2],inf).asStream,0.25,0.5,\sn2_5).xposeInt_(-5),  //  1 8 X.EC

			// snake3
			// scale = 8ves and 4ths (only 0 and 5)  6 BT
			sn3_1: Snake.new(aIn,sn3,\symballs3_1,\snake,Scale([0,5],tuning: \just).ratios,
				60.midicps,4,0.6,-0.75,0.5,\sn3_1)
			.wLimLo_(96.midicps).wLimHi_(108.midicps).wEndLo_(24.midicps).wEndHi_(108.midicps)
			.wDir_(-1).wIncr_(0.5).sus_(2),

			// min triad scale [7,3,0]
			sn3_2: Snake.new(aIn,sn3,\gazimba3_2,\snake,Scale([0,3,7],tuning: \just).ratios,
				60.midicps,4,0.8,-0.75,0.5,\sn3_2)
			.wLimLo_(84.midicps).wLimHi_(96.midicps).wEndLo_(36.midicps).wEndHi_(96.midicps)
			.wDir_(-1).wIncr_(0.35).sus_(3),

			// dorian scale
			sn3_3: Snake.new(aIn,sn3,\gazimba3_3,\scaleEcho,Scale.dorian.tuning_(\just).ratios,
				60.midicps,4,Pseq([0.5,0.5,1.0,1.0],inf).asStream,-0.75,0.5,\sn3_3).sus_(2),

			sn3_4: Snake.new(aIn,sn3,\briteCelst3_4,\scaleEcho,Scale.dorian.tuning_(\just).ratios,
				60.midicps,4,Pseq([0.5,0.5,1.0],inf).asStream,-0.75,0.5,\sn3_4),

			sn3_5: Snake.new(aIn,sn3,\riteCelst3_5,\xposeEcho,\chromatic,
				60.midicps,4,Prand([0.5,1.0,1,2],inf).asStream,-0.75,0.5,
				\sn3_5).sus_(1.5).xposeInt_(-10),
			// " SECTION 9 ".post; " X.EC...3OUT ".postln;

			// snake4
			// scale = 8ves and 5ths (only 0 and 7) 1.2 BT
			sn4_1: Snake.new(aIn,sn4,\symballs4_1,\snake,Scale([0,7],tuning: \just).ratios,
				60.midicps,4,1.2,0.75,0.5,\sn4_1)
			.wLimLo_(96.midicps).wLimHi_(108.midicps).wEndLo_(24.midicps).wEndHi_(108.midicps)
			.wDir_(-1).wIncr_(1).sus_(2),

			//dorian scale
			sn4_2: Snake.new(aIn,sn4,\symballs4_1,\snake,Scale.dorian.tuning_(\just).ratios,
				60.midicps,4,1.6,0.75,0.5,\sn4_2)
			.wLimLo_(84.midicps).wLimHi_(96.midicps).wEndLo_(36.midicps).wEndHi_(96.midicps)
			.wDir_(-1).wIncr_(1).sus_(2).transitsLim_(4),

			sn4_3: Snake.new(aIn,sn4,\handDrum4_3,\snake,Scale.dorian.tuning_(\just).ratios,
				60.midicps,4,Pseq([0.5,0.5,1,1,2,2,4,4],inf).asStream,0.75,0.5,\sn4_3)
			.sus_(0.4)
			.wLimLo_(72.midicps).wLimHi_(84.midicps).wEndLo_(48.midicps).wEndHi_(84.midicps)
			.wDir_(-1).wIncr_(1).transitsLim_(4),

			sn4_4: Snake.new(aIn,sn4,\handDrum4_4,\snake,Scale.dorian.tuning_(\just).ratios,
				60.midicps,4,Pseq([1,1,2,2,4],inf).asStream,0.75,0.5,\sn4_4)
			.wLimLo_(72.midicps).wLimHi_(84.midicps).wEndLo_(48.midicps).wEndHi_(84.midicps)
			.wDir_(-1).wIncr_(1).transitsLim_(6),

			sn4_5: Snake.new(aIn,sn4,\handDrum4_5,\snake,Scale.dorian.tuning_(\just).ratios,
				60.midicps,4,Pseq([1,2],inf).asStream,0.75,0.5,\sn4_5)
			.wLimLo_(60.midicps).wLimHi_(72.midicps).wEndLo_(48.midicps).wEndHi_(72.midicps)
			.wDir_(-1).wIncr_(1).transitsLim_(12),

			sn4_6: Snake.new(aIn,sn4,\handDrum4_6,\xposeEcho,Scale.chromatic.tuning_(\just).ratios,
				60.midicps,4,0.5,0.75,0.5,\sn4_6).sus_(8).xposeInt_(10)
			// " X.EC...2OUT ".postln;   // 10 2 X.EC
		)
	}

	*makeTaskSeq {
		tasks = (
			sn1: Task({
				snakes[\sn1_1].start; // start sn1_1
				while({(snakes[\sn1_1].state!=\stop) || (snakes[\sn4_1].state!=\stop)},
					{ 0.01.wait;  });  // wait until 1_1 AND 4_1 complete
				snakes[\sn1_2].start;   // start 1_2
				while({snakes[\sn1_2].state!=\stop},{ 0.01.wait });  // wait for 1_2 to complete
				snakes[\sn1_3].start;  // start 1_3
				while({(snakes[\sn1_3].state!=\stop) || (snakes[\sn4_3].state!=\stop)},
					{ 0.01.wait });  // wait for 1_3 AND 4_3 to complete
				snakes[\sn1_4].start;   // start 1_4 (scale-echo)
				while({snakes[\sn4_4].state!=\stop},{ 0.01.wait });  // wait for 4_4 to complete
				snakes[\sn1_4].stop;   // stop 1_4
				snakes[\sn1_5].start;   // start 1_5
				while({snakes[\sn4_5].state!=\stop},{ 0.01.wait }); // wait for 4_5 to complete
				snakes[\sn1_5].stop;   // stop 1_5
				snakes[\sn1_6].start;  // start 1_6
			}),

			sn2:  Task({
				while({snakes[\sn1_1].state!=\stop},{ 0.01.wait }); // wait for 1_1 to complete
				snakes[\sn2_1].start;
				while({(snakes[\sn2_1].state!=\stop) || (snakes[\sn1_2].state!=\stop)},
					{ 0.01.wait });  // wait until 2_1 AND 1_2 to complete
				snakes[\sn2_2].start;  // start 2_2
				while({snakes[\sn2_2].state!=\stop},{ 0.01.wait }); // wait until 2_2 ends
				snakes[\sn2_3].start; // start 2_3  (scale-echo)
				while({snakes[\sn1_4].state!=\stop},{ 0.01.wait });  // wait until 1_4 ends
				snakes[\sn2_3].stop; snakes[\sn2_4].start; // start 2_4
				while({snakes[\sn1_5].state!=\stop},{ 0.01.wait });  // wait until 1_5 ends
				snakes[\sn2_4].stop; snakes[\sn2_5].start; // start 2_5
			}),

			sn3: Task({
				while({snakes[\sn2_1].state!=\stop},{ 0.01.wait });  // starts after 2-1 ends
				snakes[\sn3_1].start; // start 3_1
				while({snakes[\sn3_1].state!=\stop},{ 0.01.wait });  // wait until 3_1 ends
				snakes[\sn3_2].start;  // start after 3_1 ends
				while({snakes[\sn3_2].state!=\stop},{ 0.01.wait }); // wait until 3_2 ends
				snakes[\sn3_3].start;  // start 3_3 (scale-echo)
				while({snakes[\sn2_3].state!=\stop},{ 0.01.wait });  // continue until 2_3 ends
				snakes[\sn3_3].stop; snakes[\sn3_4].start; // start 3_4
				while({snakes[\sn2_4].state!=\stop},{ 0.01.wait }); // continue until 2_4 ends
				snakes[\sn3_4].stop; snakes[\sn3_5].start; // start 3_5
			}),

			sn4: Task({
				while({snakes[\sn2_1].state!=\stop},{ 0.01.wait }); // starts after 2-1 ends
				snakes[\sn4_1].start; // start 4_1
				while({(snakes[\sn4_1].state!=\stop) || (snakes[\sn1_2].state!=\stop)},
					{ 0.01.wait }); // wait until 4_1 AND 1_2 end
				snakes[\sn4_2].start; // start 4_2
				while({(snakes[\sn4_2].state!=\stop) || (snakes[\sn3_2].state!=\stop)},
					{ 0.01.wait });  // wait until 4_2 AND 3_2 end
				snakes[\sn4_3].start;  // start 4_3
				while({snakes[\sn4_3].state!=\stop},{ 0.01.wait });  // wait until 4_3 done
				snakes[\sn4_4].start;  // start 4_4
				while({snakes[\sn4_4].state!=\stop},{ 0.01.wait }); // wait until 4_4 done
				snakes[\sn4_5].start; // start 4_5
				while({snakes[\sn4_5].state!=\stop},{ 0.01.wait });  // wait until 4_5 done
				snakes[\sn4_6].start;  // start 4_6, xposeEcho
			})
		);
		snakeOrder = [[\sn1_1,\sn1_2,\sn1_3,\sn1_4,\sn1_5,\sn1_6],
			[\sn2_1,\sn2_2,\sn2_3,\sn2_4,\sn2_5,\sn2_6],
			[\sn3_1,\sn3_2,\sn3_3,\sn3_4,\sn3_5,\sn3_6],
			[\sn4_1,\sn4_2,\sn4_3,\sn4_4,\sn4_5,\sn4_6]];
	}

	*nextSnakeBtn {|num|
		var idx = snakeOrder[num].detectIndex {|sn| snakes[sn].notNil };
		^score[idx][num]   // returns button
	}



}

/*
SnakeCharmer.nextSnakeBtn(0).
~idx = SnakeCharmer.snakeOrder[0].detectIndex {|sn| SnakeCharmer.snakes[sn].notNil };
var idx = snakeOrder[num].detectIndex {|sn| snakes[sn].notNil };
		SnakeCharmer.score[~idx][0]   // returns button
SnakeCharmer.init
SnakeCharmer.curSnakes[0].sus
SnakeCharmer.score[5].removeAt(2)
SnakeCharmer.vocoderVolumes[0].value_(0.5)

SnakeCharmer.vocoderVolumes[0].value_(0.25)

SnakeCharmer.curSnakes[0].notNil.if({SnakeCharmer.sn1.vocoder.set(\vol,val/127)}
SnakeCharmer.sn1.fmArgs[\out].value connect(SnakeCharmer.sliders[0]);
SnakeCharmer.sn2.fmArgs[\vol].connect(SnakeCharmer.sliders[2]);
SnakeCharmer.sn3.fmArgs[\vol].connect(sliders[2]);
SnakeCharmer.sn4.fmArgs[\vol].connect(sliders[3]);
SnakeCharmer.ampMults[2].value

SnakeCharmer.curSnakes[1].input.inBus
SnakeCharmer.curSnakes[1].vocoder.set(\out,0)

SynthDef(\vocsn1,{|inBus=0,ratio=1.0,amp=0.1,gsize=0.1,pan=0,out=0,vol=0.25|
			var shift = PitchShift.ar(
In.ar(inBus).poll(40),gsize,      // grain size
				ratio,    // snakeFrq/aInFrq = ratio
				0,                // pitch dispersion
				0.004            // time dispersion
			);
			Out.ar(out,Pan2.ar(shift,pan,vol))
		}).add;

SnakeCharmer.outputBusOffset  // 0
SnakeCharmer.aIn.inbus  // 0
a = Synth(\vocsn1)
a.set(\inBus,8)  // must be 8 or no go! ... qhy
a.free
SnakeCharmer.ampMults[2].value

*/

