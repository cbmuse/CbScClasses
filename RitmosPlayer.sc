
RitmosPlayer {
	var <numVoices, <>tempo, <>maxLength,<>countFlg;
	var	<>countLimit=100, <>countLimitVoice=0, <>countLimitFunc, <>countLimitFuncs,		<voices, <gui,<ampMax, <>masterVol=1, <>presets, <>preset=0, <>prstName;
	var <>currentGoal;
	var <>intEnvir;

	*new { arg numVoices=4,tempo=60,maxLength=64,countFlg=false;
		^super.newCopyArgs(numVoices,tempo, maxLength,countFlg).init
	}

	*initClass {

		StartUp.add {
 //	you can comment out or delete this...copied from your startupfile
			// Spec.specs.addAll([
			// 	\bufnum->ControlSpec(0,100,\lin,1,0),     \sus->ControlSpec(default:1),
			// 	\sinFreq->Spec.specs.at(\freq),	      \sinAmp->ControlSpec(warp:\amp),
			// 	\sawFreq->Spec.specs.at(\freq),	      \sawPhs->ControlSpec(0,2),
			// 	\sawAmp->ControlSpec(warp:\amp),	      \plsFreq->Spec.specs.at(\freq),
			// 	\plsWdth->ControlSpec(default: 0.1),	      \plsAmp->ControlSpec(warp:\amp),
			// 	\lofreq->[0.01,100,\exp,0,0.1].asSpec,
			// 	\sinfrq->Spec.specs.at(\lofreq),	      \sawfrq->Spec.specs.at(\lofreq),
			// 	\plsfrq->Spec.specs.at(\lofreq),	      \sigAmp->ControlSpec(warp:\amp),
			// 	\gate->ControlSpec(default:1, step:1),	      \vol->ControlSpec(warp:\amp),
			// 	\effOut->[8,12,\lin,1,8].asSpec,	      \out->[0,7,\lin,1,0].asSpec,
			// 	\end->ControlSpec(default:1),	      \spd->ControlSpec(0.125,12,\exp,0,4),
			// 	\spdQ->ControlSpec(0.125,12,\exp,0.1,4),	      \pSpd->ControlSpec(0.0125,8,\exp,0.01,1),
			// 	\trgf->ControlSpec(1.0,80,\exp,default:4), \gdur->ControlSpec(0.01,1.0,\exp,0,0.1),
			// 	\dnse->ControlSpec(0.0625,8,\exp,default:2),	      \lfrq->[0.01,200,\exp,0,0.2].asSpec,
			// 	\fvar->ControlSpec(0.01,5,\exp),	      \fmul->ControlSpec(1,20,default:1),
			// 	\wdth->ControlSpec(default:0.1),	      \ffreq->ControlSpec(80,8000,\exp,0,1200),
			// 	\clip->ControlSpec(default:0.5),	      \rtio->ControlSpec(0.01,20,\lin,0.1,1),
			// 	\ratio->ControlSpec(0.01,20,\lin,0.1,1),	      \ratio2->ControlSpec(0.01,20,\lin,0.1,1),
			// 	\ratio3->ControlSpec(0.01,20,\lin,0.1,1),  \ratio4->ControlSpec(0.01,20,\lin,0.1,1),
			// 	\lrtio->ControlSpec(0.01,4,\lin,0.025,1),     \idx->ControlSpec(0,2pi,\linear),
			// 	\frq1->Spec.specs.at(\ffreq),	      \frq2->Spec.specs.at(\ffreq),
			// 	\dcy1->[0.001,0.1,\lin].asSpec,	      \dcy2->[0.001,0.1,\lin].asSpec,
			// 	\res->[0.001,0.1,\lin,0,0.005].asSpec,	      \dcyt->[0,16,\lin,0,1.0].asSpec,
			// 	\mfrq1->Spec.specs.at(\lofreq),	      \mDpt1->[0.001,4000,\exp].asSpec,
			// 	\aDpt1->[0.001,4000,\exp].asSpec,	      \mfrq2->Spec.specs.at(\lofreq),
			// 	\mfrq->Spec.specs.at(\lofreq),	      \mDpt2->[0.001,4000,\exp].asSpec,
			// 	\aDpt2->[0.001,4000,\exp].asSpec,	      \frqShift->[0.1,4000,\exp,0,4].asSpec,
			// 	\fDpth->[0.001,4000,\exp].asSpec,	      \dly->[0,8,\lin,0,0.2].asSpec,
			// 	\dlyt->[0,8,\lin,0,0.2].asSpec,	      \dly1->[0,8,\lin,0,0.2].asSpec,
			// 	\dly2->[0,8,\lin,0,0.2].asSpec,	      \dcyt1->[0,16,\lin,0,1.0].asSpec,
			// 	\dcyt2->[0,16,\lin,0,1.0].asSpec,	      \gfrq->[0.5,80,\exp,0,12.0].asSpec,
			// 	\amp1->ControlSpec(warp:\amp),	      \amp2->ControlSpec(warp:\amp),
			// 	\pShft->[0.25,4.0,\exp,0,1.0].asSpec,	      \wsize->[0.01,0.5,\lin,0.01,0.05].asSpec,
			// 	\pShft1->[0.25,4.0,\exp,0,1.0].asSpec,	      \wsize1->[0.01,0.5,\lin,0.01,0.05].asSpec,
			// 	\pShft2->[0.25,4.0,\exp,0,1.0].asSpec,	      \wsize2->[0.01,0.5,\lin,0.01,0.05].asSpec,
			// 	\bShft->[-32,96,\lin,1,16].asSpec,	      \stch->[1,8,\lin,0.1,1].asSpec,
			// 	\bShft1->[-32,96,\lin,1,16].asSpec,	      \stch1->[1,8,\lin,0.1,1].asSpec,
			// 	\bShft2->[-32,96,\lin,1,16].asSpec,	      \stch2->[1,8,\lin,0.1,1].asSpec,
			// 	\thrsh->[0.01,1,\exp,0,0.1].asSpec,	      \thrsh1->[0.01,1,\exp,0,0.1].asSpec,
			// 	\thrsh2->[0.01,1,\exp,0,0.1].asSpec,	      \slp1->[0,10,\lin,0.0,1].asSpec,
			// 	\slp2->[0,10,\lin,0.0,1].asSpec,	      \slpa->[0,10,\lin,0.0,1].asSpec,
			// 	\slpb->[0,10,\lin,0.0,1].asSpec,	      \wvsBuf->[1,16,\lin,1,1].asSpec,
			// 	\voscBuf->[1,7,\lin,0,1].asSpec,	      \coef->[-1,1,\lin,0.0,0.0].asSpec,
			// 	\brit->[1,12000,\exp,0,1000].asSpec,	      \plDcy->[0.1,8,\lin,0,2].asSpec,
			// 	\plSpd->[0.125,20,\exp,0,4].asSpec,	      \tblBuf->[1,8,\lin,0,1].asSpec,
			// 	\swpFrq->Spec.specs.at(\lofreq),	      \vib->Spec.specs.at(\lofreq),
			// 	\in->[1,8,\lin,1,1].asSpec,	      \att->[0,2.0,\lin,0,0.01].asSpec,
			// 	\rls->[0,2.0,\lin,0,0.2].asSpec,	      \bpm->[20,480,\exp,1.0,120].asSpec,
			// 	\lvl->[0.5,4.0,\amp,0,1.0].asSpec,	      \hWdth->[0.001,0.5,\exp,0,0.1].asSpec,
			// 	\pInt->[0.25,2,\exp,1,0.05].asSpec,	      \frq->[20,4186,\exp,0,512].asSpec,
			// 	\duty->[0,1,\lin,0,0.5].asSpec,	      \ring->[0.05,10,\exp,0,0.4].asSpec,
			// 	\frq3->Spec.specs.at(\ffreq),	      \loAmp->ControlSpec(warp:\amp),
			// 	\hiAmp->ControlSpec(warp:\amp),	      \loPan->Spec.specs[\pan],
			// 	\hiPan->Spec.specs[\pan],			\sustain->[0.01,120.0].asSpec
			// ]);

		SynthDef(\pluck,{ arg freq=400,brit=2000,plDcy=0.5,clip=1,rq=0.1,pan=0,amp=0.1,gate=1;
				var exciter,sig,bright;
				bright = brit+freq;
				exciter = PinkNoise.ar(Decay.kr(Impulse.kr(0.1), 0.01));
				sig = RLPF.ar(CombC.ar(exciter,0.1,freq.reciprocal,plDcy),bright,rq,
					EnvGen.kr(Env.asr(0.0025,1,0.05),gate,doneAction: 2))
						.clip2(clip)*4;
				Out.ar(0,Pan2.ar(sig,pan,amp));
			}).store;
		}
	}

	init {
		ampMax = numVoices.reciprocal/2;

		maxLength = maxLength ?? { 64 };

		preset = 0;

		countLimitFunc = 'none';

		countLimitFuncs =
			('seq-loop': { this.nextPreset },
			'seq-stop': { if((this.preset+1) != (this.presets.size),{  this.nextPreset },{
						this.stop }) },
			'rand': { this.loadPreset(presets.size.rand) },
			'none': nil );

		voices = Array.fill(numVoices,{|i| RitmosVoice.new(this,i) });

// NOTE this might could change...putting in to be able to interpolate tempo
		intEnvir = IdentityDictionary.new;

		gui = RitmosGUI.new(this, numVoices);

		SynthDescLib.global.read;

		voices.do {|v,i|
			v.gui = gui.voices[i];
			v.synth_(\pluck);
		};

		presets = List.new;


	}

	asPreset {
		var result = IdentityDictionary.new;

		// put global variables
		result.put(\global, Environment.new );
		result[\global].putAll((
			name: prstName.copy,
			tempo: tempo.copy,
			countLimit: countLimit.copy,
			countLimitVoice: countLimitVoice.copy,
			countLimitFunc: countLimitFunc.deepCopy
		));

		// put voices
		result.put(\voices, voices.collect(_.asPreset) );
		^result;
	}

// TODO nil check?  delete would mess this up...
	prevPreset { ^this.loadPreset((preset-1).wrap(0,presets.lastIndex)) }

	nextPreset { ^this.loadPreset((preset+1).wrap(0,presets.lastIndex)) }

	savePreset { |name|
		presets = presets.add(this.asPreset);
		this.loadPreset(preset);
		preset = presets.lastIndex
	}

	replacePreset { |index|
		presets = presets.put(index, this.asPreset);
		preset = index
	}

// TODO this is dangerous!  maybe have a way to check to be sure first?  modal dialog?
	deletePreset { |index|
		presets.removeAt(index);
		this.loadPreset(index);
		preset = index;
	}

	loadPreset { |index|
		var prstGlobals = this.presets[index][\global];
		preset = index;
		tempo = prstGlobals[\tempo];
		prstName = prstGlobals[\name].postln;
		countLimit = prstGlobals[\countLimit];
		countLimitVoice = prstGlobals[\countLimitVoice];
		countLimitFunc = prstGlobals[\countLimitFunc];

		presets.at(index)[\voices].do {|v,i|
			voices[i].loadPreset(v.deepCopy)
		};

		this.setCounts(0);		// reset counters to 0
		{ this.gui.updatePreset; }.defer;
		^this
	}

	savePresetsFile {|path|
		var file;
		file = File.new(path ?? {"presets"++Date.localtime.stamp}, "w");
		file.write(presets.asCompileString);
		file.close;
	}

	savePresetsDialog {
		File.saveDialog("save presets","",{ arg savePath;
			this.savePresetsFile(savePath)
		})
	}

	loadPresetsFile {|path|
		var file;
		File.exists(path).if( {
			file = File.new(path,"r");
			presets = file.readAllString.interpret;
			file.close;
			preset = 0;
			this.loadPreset(preset)
		},{ "file not found !! ".postln; })
	}

	loadPresetsDialog {
		File.openDialog("load presets",{|path|
			this.loadPresetsFile(path)
		})
	}

	reloadPreset {
		"not implemented yet!".warn;
	}

	resavePreset {
		"not implemented yet!".warn;
	}

	prevGoal {
		"not implemented yet!".warn;
	}

	nextGoal {
		"not implemented yet!".warn;
	}

	play {|voiceNum| // store EventStreams, each has its own independent clock
		voices.do({|vc|vc.play });
		{ gui.cButtons[0].value_(1) }.defer
	}

	setCounts {|num|
		voices.do({|vc| vc.count = num });
		if(gui.notNil && { gui.win.isClosed.not }) {
			{ gui.voices.do {|v| v.countView.view.value_(num) } }.defer;
		};
	}

	changeRhythms {|array|
		voices.do {|vc| vc.rhythm_(array) }
	}

	//	 beatDivs only occur on bar boundaries
	changeBeatDivs {|div|
		voices.do({|vc| vc.beatDiv_(div) })
	}

	changeVols {|vol|
		voices.do({|vc| vc.vol_(vol) })
	}

	changeSynths {|synth|
		voices.do({|vc| vc.synth_(synth) })
	}

	changeParams { |symbol,value|
		voices.do({|vc| vc.changeParam(symbol,value) })
	}

	// to change master tempo, change each voice clock tempo
	changeTempo {|newTempo|
		voices.do({|vc|
			vc.changeTempo(newTempo)
		});
		tempo = newTempo
	}

	mute {
		voices.do({|vc| vc.mute })
	}

	unmute {|voiceNum|
		voices.do({|vc| vc.unmute })
	}

	pause {
		voices.do({|vc| vc.pause })
	}

	resume {
		voices.do({|vc| vc.resume })
	}

	stop {
		voices.do({|vc| vc.stop; })
	}

}

RitmosVoice {

	var <>player, <voiceNum, <rhythm, <beatDiv, <synth, <>inChan, <>pattern, <>vol=1, <>clock,
		<>count=0, <stream, <>voiceEnvir, <params, <>gui, <guiUpdate, <>ampGate=0.05,
		<downbeat, <rhythmTarg, <>inputBeats, <>weights,<>numGen=4,<>mutRate=0.25,
		<rhythmCritter, <gA,
		//	<breeder, <>breederRate=20,
		<inputHistory;

// TODO is this the best way to store info?  should it all be in one envir?
	var <>intEnvir, <>seqEnvir;

// 6/17 ls
	*new { arg player, voiceNum, rhythm, beatDiv, synth, inChan=1;
		^super.newCopyArgs(player, voiceNum, rhythm,beatDiv,synth,inChan).init
	}

	init {
		rhythm ?? { rhythm = [1,0,0,0,0,0,0,0] };

		voiceEnvir = (amp: rhythm.extend(player.maxLength, 0),
			freq: {110*(voiceNum+1)}!(player.maxLength),
			sustain: {1.0}!(player.maxLength)
		);

		// setup genetic algorithm
//		gA = GenAlg.new(critterSize: player.maxLength);
//		gA.valuePop(0.0);
//		rhythmTarg = rhythm.copy;	// initialize target to preset rhythm
//		gA.scoreAll(rhythmTarg);	// initialize gA pop
//		inputBeats = Array.fill(rhythm.size,0);
//		weights = [0.33,0.33,0.33];
//		inputHistory = MIDIPhraseAnalyzer.new(inChan).start;

// TODO maybe this should get parent envir from player just like specs?
		intEnvir = RitmosVoice.defaultIntEnvir;
		seqEnvir = RitmosVoice.defaultSeqEnvir;

		beatDiv ?? { beatDiv = 1 };
		pattern ?? { pattern = EventPatternProxy.new(this.makeEventCycle)};
		^this;
	}
// NOTE default key does nothing yet...but might be useful eventually?
	*defaultIntEnvir {
		^(amp: true, freq: true, instrument: false, default: true)
	}


	*defaultSeqEnvir {
		^(amp: true, freq: true, instrument: false, gate: false, default: false)
	}

	updateEnvir {
		voiceEnvir.keysValuesDo { |key, value|
			if( intEnvir.at(key).isNil ) {
				intEnvir.put(key, true);
			};
			if( seqEnvir.at(key).isNil ) {
// TODO change to default value
				seqEnvir.put(key, true);
			};

		}
	}

	asPreset {
		^(
			voiceNum: voiceNum.copy,
			rhythm: rhythm.deepCopy,
			beatDiv: beatDiv.copy,
			vol: vol.copy,
			envir: voiceEnvir.deepCopy,
			interpolate: intEnvir.deepCopy,
			sequence: seqEnvir.deepCopy
		)
	}

	loadPreset {|preset|
		voiceEnvir = preset.envir;
		intEnvir = preset.interpolate;
		seqEnvir = preset.sequence;

		this.rhythm_(preset.rhythm);
		voiceEnvir.at(\instrument) !? { this.synth_(voiceEnvir.at(\instrument)) };
		if(clock.notNil,{
			this.beatDiv_(preset.beatDiv);
		},{
			beatDiv = preset.beatDiv;
		});
		this.vol_(preset.vol);

		{
			this.gui.instSelectView.value_(this.gui.instrumentList.indexOf(this.voiceEnvir.at(\instrument)));
			this.gui.beatDivView.view.value_(preset.beatDiv);
			this.gui.levelView.value_(this.vol);
			this.gui.rhythmLengthView.view.value_(this.rhythm.size);

		}.defer;
	}

//	breedRhythm {
//		numGen.do {|i|	// run genetic algorithm
//			rhythmCritter = gA.tournament(rhythm.size);
//			gA.score(gA.sumDiffSquared(rhythmTarg,rhythmCritter));
//		};
//		gA.scoreAll(rhythmTarg);	// score after each #gen because target changes!
//	}

	getInputBeats {
		var cycTime, cycCtr=0,lastCycInput,dbTime;
		inputBeats = Array.fill(rhythm.size,{0});
		if(downbeat.notNil,{
			cycTime = rhythm.size*(clock.tempo.reciprocal);
			dbTime = downbeat - inputHistory.startTime;
			while({inputHistory.onTime(cycCtr) > dbTime},{
				cycCtr = (cycCtr+1) });
			lastCycInput = Array.fill(cycCtr,{|i|
				[inputHistory.onTime(i),inputHistory.velNum(i)]});

			lastCycInput.do {|vals| var ictus;
				ictus = rhythm.size - ((vals[0] - dbTime) / (cycTime / rhythm.size));
	//			if(voiceNum == 0,{ ictus.postln; });
				// quantize input to ictus (add saving frac as "swing" later)
				if(ictus.frac > 0.5,{ ictus = (ictus + 1).asInt },{ ictus = ictus.asInt});
				inputBeats.put(ictus%(rhythm.size),vals[1])
		}})
	}

	newRhythmTarg {
		// compute inputBeats from MidiHistory
		this.getInputBeats;
		if(voiceNum == 0,{ "input = ".post; inputBeats.round(0.001).postln; });
		// ^rhythmTarg = weighted sum of inputBeats, rhythmCritter, and this.rhythm
		^rhythmTarg = (inputBeats*weights[0]) + (rhythmCritter*weights[1]) + (rhythm*weights[2]);
	}

	rhythmToDur {|rhyt|
		var dur;
		if(rhyt[0] == 0,{ rhyt.put(0,0.0001) });  // first beat must not be empty
		dur = rhyt.inject([],{|array,val,i| if(((val>ampGate) || (i == 0)),{array ++ (i+1) },{array }) });
		^dur = dur.collect {|item,i|
			if((i==0) && (dur.size > 1),{
				dur[1]-item
			},{
				if( (i<(dur.size-1)),{ dur[i+1]-item
		 		},{ (rhyt.size+1)-item
		 		})
		 	})
		 };
	}

	makeEventCycle {
		^Pn( Plazy {
			var noteDur, indexes, event;
			this.newRhythmTarg; if(voiceNum == 0,{ "targ = ".post; rhythmTarg.round(0.001).postln;   });
			noteDur = this.rhythmToDur(rhythmCritter);
			if(voiceNum == 0,{ "critter = ".post; rhythmCritter.round(0.001).postln; " ".postln });
			indexes = [0]++(noteDur.integrate.drop(-1));
			event = voiceEnvir.copy;
			event.keysValuesChange {|key, value, i|
				Pseq(value.asArray.wrapAt(indexes),1);
			};
			event.put(\instrument,synth);
			event.put(\amp, Pseq(rhythmCritter.wrapAt(indexes)
					*player.ampMax*vol*player.masterVol, 1));
			event.put(\dur, Pseq(noteDur, 1));
			if(player.countFlg,{	// check cycle count var, execute countLimitFunc if limit reached
				count = count+1;
				if((player.countLimitVoice == this.voiceNum) && (count == (player.countLimit)),
					{ clock.sched(rhythm.size-1,{
						if(stream.isPlaying,{
							player.countLimitFuncs[player.countLimitFunc].value; });
						count = 0; nil }) });
				if(player.gui.win.notNil && { player.gui.win.isClosed.not }) {
					{ gui.countView.view.value_(count) }.defer;
				}
			});
			// store downbeat
			clock.sched(0,{ downbeat = thisThread.seconds });

			Pbind(*event.asKeyValuePairs);
		}, inf)
	}

// TODO BUG play doesn't remember proper beatDivs
	play {
		guiUpdate = Task({
			var grid = gui.rhythmPosCells;
			var skin = gui.skin;
// NOTE this could end up causing a bug
			var lastColor = skin.colors.rest;
			var lastCell = grid[rhythm.size-1];
			loop {
				rhythm.do {|val, i|
					if(player.gui.win.notNil && { player.gui.win.isClosed.not }) {
						{
							lastCell.background_(lastColor);
							lastCell = grid[i];
//							lastColor = lastCell.background;
							lastColor = if(val > 0) { skin.colors.event } { skin.colors.rest };
							grid[i].background_(skin.colors.active);
							nil;
						}.defer;
					};
					1.0.wait;
		}}});
//		breeder = Routine {
//			loop { this.breedRhythm; breederRate.reciprocal.wait }
//		}.play(AppClock);
		// set gui to current values -- DO WE NEED THIS?
		rhythm.do { |val, j|
			gui.updateRhythm(\amp, j, val, player);
//			gui.rhythmEditCells[j].valueAction_(val);
			{ gui.beatDivView.view.value = beatDiv; nil }.defer;
		};
		// start clock and EventPatternStream
		clock = TempoClock.new((player.tempo/60)*beatDiv);
		stream = pattern.play(clock).player;
		// gui update tasks use same player clocks
		guiUpdate.play(clock);
	}

	rhythm_ {|array|
		rhythm = array;
		voiceEnvir.put(\amp, voiceEnvir.at(\amp).overWrite(rhythm) );
		{ gui.rhythmLengthSlider.valueAction_(gui.rhythmLengthSpec.unmap(rhythm.size)) ; nil }.defer;
		if(clock.notNil) {
			clock.playNextBar {
				clock.beatsPerBar_(rhythm.size);
				rhythm.do {|val, j| gui.updateRhythm(\amp, j, val, player) };
			};
		} {
			rhythm.do {|val, j| gui.updateRhythm(\amp, j, val, player) };
		}
	}
// NOTE haven't checked to see if this is updated
//	params_ {|key,array|
//		params[key] = array;
//		if(gui.currentParameter == key,{
//			array.do({|val,j|
//				gui.rhythmEditCells[j].valueAction_(val)
//			})
//		},{
//			gui.paramRhythmLists[key] = array++gui.paramRhythmLists[key].drop(array.size);
//			voiceEnvir.put(key,array)
//		});
//	}

// TODO BUG gui display of position gets messed up with this
// NOTE changed behavior
	beatDiv_ {|div|
		clock.playNextBar({
			clock.tempo = player.tempo/60*div;
			clock.beatsPerBar_(rhythm.size) ;
			beatDiv = div;
			nil
		});
	}

	synth_ {|synthName|
		var oldSynthDesc,oldKeys,newCtls,newKeys,unused;
		oldSynthDesc = SynthDescLib.global.synthDescs.at(synth);
		synth = synthName.asSymbol;
		voiceEnvir.put(\instrument, synth);
		SynthDescLib.global.synthDescs.at(synth) !? {
			newCtls = SynthDescLib.global.synthDescs.at(synth).controls;
			oldSynthDesc !? {
				oldKeys = oldSynthDesc.controls.collect {|ctl| ctl.name.asSymbol }.asArray;
				newKeys = newCtls.collect {|ctl| ctl.name.asSymbol }.asArray;
				//	remove controls from envir that are unused in new synth
				unused = oldKeys.removeAllSuchThat({|str|
					(oldKeys.collect({|str| newKeys.select({|val| str == val }) }).flatten)
						.select({|val| str == val }).isEmpty });
				unused.do {|ctl| voiceEnvir.removeAt(ctl) };
			};
			// add new controls
			newCtls.do {|ctrl|
				if(voiceEnvir.at(ctrl.name.asSymbol).isNil) {
					voiceEnvir.put(ctrl.name.asSymbol, {ctrl.defaultValue}!(player.maxLength));
				}
			};

			this.updateEnvir;

// NOTE may only need to do seqEnvir check?
			gui.paramKeys = voiceEnvir.select { |obj,key|
				(obj.isSequenceableCollection) &&
				(seqEnvir.at(key))
			}.keys(Array).sort;
			//gui.paramKeys.remove(\gate);
			{ gui.paramSelectView.items_(gui.paramKeys) ; nil }.defer;
		};

	}

// TODO this should be in own data class
	addParam { |symbol, value|

	}

	changeParam { |symbol,value|
		pattern.set(symbol.asSymbol,value);
	}

	// clock tempo includes beatDiv multiplier
	changeTempo {|newTempo|
		clock.tempo = newTempo/60 * beatDiv;
	}

	changeCycle {|newCycle|
		var oldSize = rhythm.size;
		if(newCycle > oldSize,{
			rhythm = rhythm++Array.fill(newCycle-oldSize,0);
//			params = params.collect({|array| array++Array.fill(newCycle-oldSize,0)});
		},{ rhythm = rhythm.copyRange(0,newCycle-1);
//			params = params.collect({|array| array.copyRange(0,newCycle-1)});
		});
	}

	mute {
		stream.mute
	}

	unmute {|voiceNum|
		stream.unmute
	}

	pause {
		stream.pause; guiUpdate.pause
	}

	resume {
		stream.resume; { guiUpdate.reset.resume }.fork(clock)
	}

	stop {
		stream.stop;
		//	breeder.stop;
		count = 0; downbeat = nil;
		guiUpdate.stop.reset
	}
}

/*
		(
			c = RitmosPlayer.new();
			s.waitForBoot { TempoClock.default.playNextBar { c.play } };
		)
		c.tempo = 120
		c.savePreset;
		c.replacePreset(2)
		c.replacePreset(\testing); // seems to work! can use symbols or indexes
		// but doesn't show up in the compile string...
		c.presets;


*/
