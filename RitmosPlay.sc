
RitmosPlay {
	var <numVoices, <>tempo, <numCells,
		<prstFilePath, <>instFilePath, <>sampFilePath, <prstRoutFilePath, <>netFlg, <>startFlg;
	var <voices, <>countLimitFuncs, <>countFlg, <seqGui, <ctlGui, <>masterVol;
	var <>presets, <>presetRoutines, <>presetNum, <>presetName, <>sequence, <>changePresetNum, <>interpPreset;
	var loadFilePath, saveFilePath, <>sampleBuffers, <>instrumentList, <isRunning=false;
	var <>data, <>routine;
	var <>interpValue, <>interpDefaultKeys;

	*initClass {
		StartUp.add {
			RitmosSynthDefs.init;
		}
	}

	//	add options for gui and gen usage
	*new { arg numVoices=4,tempo=60,numCells=64,
			prstFilePath, instFilePath, sampFilePath, prstRoutFilePath,
			netFlg=false, startFlg=false;
		^super.newCopyArgs(numVoices,tempo, numCells,
			prstFilePath, instFilePath, sampFilePath, prstRoutFilePath, netFlg, startFlg ).init
	}

	init {
		var voicesData, file;
		numCells = numCells ?? { 64 };
		sampleBuffers = SampleBuffers.new(sampFilePath); // loads defaults if no file
		if(prstRoutFilePath.notNil,{
			if(File.exists(prstRoutFilePath),{
				file = File(prstRoutFilePath,"r");
				presetRoutines = file.readAllString.interpret;
				file.close;
				presetRoutines.put('none', Routine({ nil }) ); // add no routine stub
			},{ presetRoutines = ('none':  Routine({ nil }) )})
		},{ presetRoutines = ('none':  Routine({ nil }) ) });

		SynthDescLib.global.read;
		RitmosSpecs.initClass;		// make sure specs are customized for Ritmos
		instrumentList =  RitmosSynthDefs.synthDefs.collect {|def| def.name.asSymbol }.sort;

		voicesData = ();
		numVoices.collect {
			(	ctl: (
							weights: OV([0.5,0,0.5],
								{|v| v = v.asArray;
									if(v.size!=3,{
										if(v.size<3,{v=v.wrapExtend(3)
										},{ v=v.copyRange(0,2)})});
					 				v=v.normalizeSum
					 			}),
							numGen: CV([0,100,\lin,1],20),
							mutRate: CV([0,1.0,\lin,0],0.25),
							gateLev: CV([0,1.0,\lin,0],0.15),
							xfrmFunc: OV(\echo,(_.asSymbol)),
							inst: OV(\pmDecayRp, (_.asSymbol)),
							level: CV([0.001,3.0,\amp,0],0.5),
							mute: OV(false,{|val| // convert number 1 to false, 0 to true
								if(val.isNumber,{val = val.booleanValue.not}); val }),
							cycCtr: CV([1,1000,\lin,1],1),
							cycle: CV([1,numCells,\lin,1],16),
							beatDiv: CV([1,36,\lin,0],4),
							param: OV(\amp,(_.asSymbol)),
							inChan: CV([1,16,\lin,1],1),
							inBounds: OV([21,108],(_.asArray)),
							breedFlg: OV(false),
							startBeat: CV([0,numCells,\lin],0),
							tonic: OV(\C,(_.asSymbol)),
							tuning: OV(\none, (_.asSymbol)),
							susDur: OV(false)
						),
				clv: 	( 	ClaveEnvir.new(numCells) ),
				interp: (
							weights: OV(false,(_.asSymbol)), numGen: OV(false,(_.asSymbol)),
							mutRate: OV(false,(_.asSymbol)), gateLev: OV(false,(_.asSymbol)),
							cycle: OV(false,(_.asSymbol)),  beatDiv: OV(false,(_.asSymbol)),
							level: OV(false,(_.asSymbol)),
							param: OV(false,(_.asSymbol))
						)
				) }.do {|ev,i| voicesData.put(("vc" ++ (i.asString)).asSymbol,ev) };

		data =  (
			voices:  voicesData,
			global: (
					ctl: (	tempo: CV([10,600,\exp,0.01],60),
							intGoal: OV.new(\pre1,(_.asSymbol)),
							countLimit: CV([1,1000,\lin,1],10),
							countVc: OV(\vc0,(_.asSymbol)),
							limitFunc: OV(\none,(_.asSymbol)),
							presetRout: OV(\none,(_.asSymbol)),
							syncStart: OV(true)
						),
					interp: (tempo: OV(false,(_.asSymbol)), countLimit: OV(false,(_.asSymbol)) )
			)
		);

		// store voices in this.voices (different than this.data.voices !!)
		voices = IdentityDictionary.new(numVoices);
		data.voices.keys.asArray.sort.do {|k,i| voices.put(k,RitmosVc.new(this, k)) };

		presets = ();
		presetNum = CV.new([1,10000,\lin,1],1);
		presetName = OV.new(\pre1,(_.asSymbol));
		sequence = OV.new(List.new(100),(_.asList));
		masterVol = CV.new([0,3,\amp,0.001],0.5);

		interpValue = CV.new.sp(0,0,1,0);
		countFlg = OV.new(startFlg);
		interpDefaultKeys = Set[ \cycle, \gateLev, \weights, \level, \beatDiv, \numGen, \mutRate ];

		countLimitFuncs =
			('seq_loop': { if((presetNum.value) < (this.sequence.value.size),
				{ this.nextPreset },{ this.loadPreset(1) }) },
			'seq_stop': { if((presetNum.value) < (this.sequence.value.size),
				{  this.nextPreset },{ this.mute;
					{ "ending".postln; this.stop; this.unmute }.defer
			})},
			'rand': { this.loadPreset(sequence.value.choose) },
			'none': { nil }
		);

		// link data to GUIS
		seqGui = RitmosSeqGUI.new(this);
		ctlGui = RitmosCtlGUI.new(this);
		MIDIListener.initClass;	//  reset noteIn histories and listeners

		if(prstFilePath.isNil,{
			this.storePreset(1);
			// this.loadPreset(1)
		},{
			this.loadPresetsFile(prstFilePath)
		});

	}

	presetNumChange {|val|
		val = val.asInt.clip(1,sequence.value.size+1);
		presetNum.value_(val);
	}

	presetNameChange {|name|
		presetName.input_(name)
	}

	// remove unused CVOrders from ClaveEnvir, avoiding data bloat!!
	reduceClaveEnvirs {
		var params;
		voices.keys.do {|vc|
			params = SynthDescLib.global.synthDescs[data.voices[vc].ctl.inst.value]
				.controlNames.collect {|it| it.asSymbol }.add(\sus);
			params.remove(\sus);
			data.voices[vc].clv.keysValuesDo {|k,v|
				if(params.includes(k).not,{
					data.voices[vc].clv.removeAt(k)
		})}}
	}

	storePreset {|argName|	// store named preset
		if(argName.class == Integer,{ 		// if arg is Int, give 'pre-n' name
			presetName.input_("pre" ++ (sequence.value.size+1));
		},{ if(sequence.value.includes(argName.asSymbol).not,{
				presetName.input_(argName)
			},{
				presetName.input_("pre" ++ (sequence.value.size+1));
			})
		});
		presetNum.value_(sequence.value.size+1);
		sequence.value_(sequence.value.add(presetName.value));
		this.reduceClaveEnvirs;
		presets.put(presetName.value,data.deepCopy);
	}

	loadPreset {|pre|
		var rem, index;
		if(pre.class == Integer,{ index = pre-1; presetName.input_(sequence.value[index])
		},{ presetName.input_(pre); index = sequence.value.indexOf(pre.asSymbol) });

		// turn off data.interp CVs of clave CVOrders NOT among the saved interp CVs !!
		data.voices.do {|vc|
			rem = vc.interp.keys.difference(interpDefaultKeys);
			rem.do {|key| vc.interp[key].value_(false) }
		};
		// stop presetRoutine, if it exists
		if(routine.notNil,{ routine.stop;  });
		this.loadPresetToData(presets[presetName.value.postln]);

		// update all voice claves to new values of \amp
		voices.keysValuesDo {|k,v|
			v.clave = data.voices[k].clv[\amp].copyRange(0,data.voices[k].ctl.cycle.value.asInt-1)
		};
		this.presetNumChange(index+1);
		interpValue.value_(0);		// reset interpValue
		this.setCounts(0);			// reset cycle counters to 0
		// set flag to change tempo of countVc on next beat
		voices[data.global.ctl.countVc.value].changeTempoFlg_(true);
		// update param menu to show args for current instruments
		ctlGui.voices.keysValuesDo {|k,v|
				{ v.instSelectView.action.value(v.instSelectView)}.defer;
		};
		voices.keysValuesDo {|k,v|
			data.voices[k].ctl.param.value_(\amp);
		};
		if(netFlg,{ 	// if network active, send section change message
			SystemClock.sched(Server.default.latency,{
				voices.do {|vc| vc.netAddr.do {|addr|
				//	"sending new section to net ".post;
					addr.sendMsg('/section',index)}};
		})});
		if(isRunning,{	// start or stop breeder if running
			this.syncStart;	// if syncStart on,  stop and restart all voice streams
			voices.keysValuesDo {|k,v| if(data.voices[k].ctl.breedFlg.value,{
				v.startBreeder },{ v.stopBreeder }) };
		// start presetRoutine, if one has been specified for this preset
			if(presetRoutines[data.global.ctl.presetRout.value].notNil,{
				if((data.global.ctl.presetRout.value != \none),{
					"starting routine ".post;
					// reset it first since it might have been played before
					presetRoutines[data.global.ctl.presetRout.value].reset;
					routine = presetRoutines[data.global.ctl.presetRout.value.postln].play
				})
			});
		})
	}

	loadPresetToData	{|prst|	// load CV, OV, CVOrder values from preset into data
		prst.voices.keysValuesDo {|name,vc|
			vc.keys.do {|k|
				vc[k].keysValuesDo {|intkey,cv|
					if(k == \clv,{
						data.voices[name][k][intkey].value_(cv)
					},{
						if(k == \ctl,{
							data.voices[name][k][intkey].value_(cv.value)
						},{
						// update paramIntView if param is currently displayed
							if((vc.ctl.param.value == intkey),{
								data.voices[name].interp.param.value_(cv.value)
							});
					// if interp key for param in preset doesn't exist in data, create it
							if( data.voices[name][k][intkey].isNil,{
								data.voices[name][k].put(intkey,OV.new(cv.value,(_.asSymbol)))
							},{
								data.voices[name][k][intkey].value_(cv.value)
							})
						})
					})
				}
			}
		};
		prst.global.keys.do {|k|
			prst.global[k].keysValuesDo {|key,val|
				data.global[k][key].value_(val.value)
			}
		}
	}

	presetAsData	{|prst|	// evaluate CV, OV, CVOrder data mirroring data structure
		prst.keysValuesChange {|k,v|
		if(v.isKindOf(Dictionary)) {
			this.presetAsData(v)
			} {
				if(v.isKindOf(ClaveEnvir)) {
					 this.presetAsData(v.envir)
				} {
					if((v.class == CV) || (v.class == OV)) {
						v.value
					} {
						v.asCompileString
					}
				}
			}
		};
		^prst
	}

	savePresetsFile {|path|
		var file, prsts;
		path ?? {path = "presets"++Date.localtime.stamp};
		saveFilePath = path;
		file = File.new(path, "w");
		prsts = presets.deepCopy;
		prsts.do {|p| this.presetAsData(p) };
		prsts.put(\dir,sequence.value.deepCopy);   // insert directory
		// strip presets-string of string error that appeared in SC3.7
		prsts = prsts.asCompileString.replace(".proto_(Environment[  ])","");
	//	prsts.postln;
		file.write(prsts);
		file.close;
	}

	savePresetsDialog {
		File.saveDialog("save presets","",{ arg savePath;
			this.savePresetsFile(savePath)
		})
	}

	loadStringToPreset	{|loadPre,curPre|	// expand CVOrder compileString format into data format
		curPre.voices.keys.do {|vc|
			loadPre.voices[vc].clv.keysValuesDo {|clvkey,clvval|
				curPre.voices[vc].clv[clvkey].value_(clvval.interpret)
			};
			loadPre.voices[vc].ctl.keysValuesDo {|key,val|
				if( curPre.voices[vc].ctl[key].class != OV,{
					curPre.voices[vc].ctl[key].value_(val)
				},{ curPre.voices[vc].ctl[key].input_(val) })
			};
			loadPre.voices[vc].interp.keysValuesDo {|key,val|
			// if interp key for param in preset doesn't exist in data, create it
				if( curPre.voices[vc].interp[key].isNil,{
					curPre.voices[vc].interp.put(key,OV.new(val,(_.asSymbol)))
				},{
					curPre.voices[vc].interp[key].value_(val)
				})
			}
		};
		curPre.global.keys.do {|k|
			loadPre.global[k].keysValuesDo {|key,val|
				curPre.global[k][key].value_(val)
			}
		}
	}

	loadPresetsFile {|path|
		var file, prsts;
		File.exists(path).if( {
			loadFilePath = path;
			file = File.new(path,"r");
			prsts = file.readAllString.interpret;
			sequence.value_(prsts[\dir].deepCopy.postln);
			prsts.removeAt(\dir);
			presets = ();
			presetNum.value_(1);
			// build new presets from data architecture, transferring prst. values to CVs
			sequence.value.do {|v,i|
				presets.put(v,data.deepCopy);
				this.loadStringToPreset(prsts[v],presets[v])
			};
			this.loadPreset(1)
		},{ "file not found !! ".postln; })
	}

	loadPresetsDialog {
		File.openDialog("load presets",{|path|
			this.loadPresetsFile(path.postln)
		})
	}

	namedSequence_ {|names|
		sequence.value_(names.select {|name| sequence.value.includes(name.asSymbol)} );
	}

	prevPreset {
		^this.loadPreset((presetNum.value.asInt-1).clip(1,sequence.value.size))	}

	nextPreset {
		^this.loadPreset((presetNum.value.asInt+1).clip(1,sequence.value.size))
	}

	replacePreset {
		presets.removeAt(sequence.value[presetNum.value-1]);
		presets.put(presetName.value,data.deepCopy);
		sequence.value_(sequence.value.put(presetNum.value-1,presetName.value))
	}

	deletePreset { |index|
		var seq;
		ModalDialog.new({arg layout;
			ActionButton(layout,"Are You Sure?")},
			name: "DeletePreset", okFunc:
		{
			if(presets.notEmpty,{
				presets.removeAt((\pre ++ (index.asString)).asSymbol);
				seq = sequence.value;
				seq.removeAt(presetNum.value-1);  // do it this way so updates
				sequence.value_(seq);
				if((index != 0),{
					this.presetNumChange(presetNum.value-1);
					this.loadPreset(presetNum.value.asInt);
				});
			})
		})
	}

	reloadPreset {
		this.loadPresetsFile(loadFilePath)
	}

	resavePreset {
		this.savePresetsFile(saveFilePath)
	}

	prevGoal {
		var goalNum = sequence.value.indexOf(data.global.ctl.intGoal.value);
		if(goalNum.notNil,{
			data.global.ctl.intGoal.value_(sequence.value[(goalNum-1)
				.clip(0,sequence.value.size-1)])
		},{
			data.global.ctl.intGoal.value_(sequence.value[presetNum.value-1])
		})
	}

	nextGoal {
		var goalNum = sequence.value.indexOf(data.global.ctl.intGoal.value);
		if(goalNum.notNil,{
			data.global.ctl.intGoal.value_(sequence.value[(goalNum+1)
				.clip(0,sequence.value.size-1)])
		},{
			data.global.ctl.intGoal.value_(sequence.value[presetNum.value-1])
		})
	}

	play {
		if(isRunning.not, {
			{ ctlGui.cButtons[0].value_(1)}.defer;
			isRunning = true;
			voices.do({|vc|vc.play });
			// start presetRoutine, if one has been specified for this preset
			if(presetRoutines[data.global.ctl.presetRout.value].notNil,{
				// reset first to allow restart after stop
				presetRoutines[data.global.ctl.presetRout.value].reset;
				routine = presetRoutines[data.global.ctl.presetRout.value].play
			});
		})
	}
	// executes within loadPreset stopping & restarting all voice streams at beginning of a preset
	syncStart {
		if(data.global.ctl.syncStart.value,{
			voices.do {|vc,i| vc.stream.stop; vc.stopBreeder };
		// schedule  at countVc' next downbeat
		//	"syncStart at ".post;
			SystemClock.schedAbs(voices[data.global.ctl.countVc.value].nextDownbeat,
				{ voices.do {|vc,i| vc.play }
			})
		})
	}

	setCounts {|num|
		voices.do({|vc| vc.voice.ctl.cycCtr.value_(num) });
	}

	//	 beatDivs only occur on bar boundaries
	changeBeatDivs {|div|
		voices.do({|vc| vc.voice.ctl.beatDiv.value_(div) })
	}

	changeLevels {|val|
		voices.do({|vc| vc.voice.ctl.level.value_(val) })
	}

	changeInst {|inst|
		voices.do({|vc| vc.voice.ctl.inst.value_(inst) })
	}

	// to change master tempo, change each voice clock tempo
	changeTempo {|newTempo|
		data.global.ctl.tempo.value_(newTempo);
		voices.do({|vc|
			vc.clock.notNil.if { vc.changeTempo(newTempo) }
		})
	}

	setParamDefault {|vnum,param,val|
		var vc = (\vc ++ vnum).asSymbol;
		data.voices[vc].clv[param.asSymbol].value_(
			Array.fill(data.voices[vc].ctl.cycle.value,val));
		{ seqGui.voices[vc].editParam_(param.asSymbol) }.defer ;
	}

	setParamValues {|vnum,param,valArray|
		var vc = (\vc ++ vnum).asSymbol;
		data.voices[vc].clv[param.asSymbol].value_(valArray);
		{ seqGui.voices[vc].editParam_(param.asSymbol) }.defer
	}

	setPresetsGlobalDefault {|param, val|
		presets.do {|pre| pre.global.ctl[param].value_(val) }
	}

	setPresetsParamDefault	{|vnum,param,val|
		var vc = (\vc ++ vnum).asSymbol;
		presets.do {|pre| pre.voices[vc].clv[param.asSymbol].value_(
		 Array.fill(pre.voices[vc].ctl.cycle.value,val))}
	}

	setPresetsParamValues	{|vnum,param,valArray|
		var vc = (\vc ++ vnum).asSymbol;
		presets.do {|pre| pre.voices[vc].clv[param.asSymbol].value_(valArray)}
	}

	setPresetsCtlDefault	{|vnum,param,val|
		var vc = (\vc ++ vnum).asSymbol;
		presets.do {|pre| pre.voices[vc].ctl[param.asSymbol].value_(val)}
	}

	rotateClaveData {|vc,shift|
		vc = vc.asSymbol;
		data.voices[vc].clv.do {|list|
			list.value_(list.copyRange(0,data.voices[vc].ctl.cycle.value-1).rotate(shift))
		}
	}

	reverseClaveData {|vc,start,end|
		vc = vc.asSymbol;
		data.voices[vc].clv.do {|list|
			list.value_(list.copyRange(start,end).reverse)
		}
	}

	mute {
		voices.do({|vc| vc.mute })
	}

	unmute {
		voices.do({|vc| vc.unmute })
	}

	pause {
		voices.do({|vc| vc.pause })
	}

	resume {
		voices.do({|vc| vc.resume })
	}

	stop {
		voices.do({|v| v.stop });
		if(routine.notNil,{ routine.stop });
		isRunning = false;
		{ ctlGui.cButtons[0].value_(0) }.defer
	}

	interpFunc {|x,y,i| if(x>y,{ ^((x-y)*(1-i)+y) },{ ^((y-x)*i+x) }) }

	globalInterpolate {|pKey|
		var target = interpPreset.global.ctl[pKey].value;
		var start = presets[presetName.value].global.ctl[pKey].value;
		^data.global.ctl[pKey].value_(this.interpFunc(start,target,interpValue.value))
	}

	vcCtlInterpolate {|name,vc,cKey|
		var target,start,wstart,wend;
		if(cKey != \weights,{
			start = presets[presetName.value].voices[name].ctl[cKey].value;
			target = interpPreset.voices[name].ctl[cKey].value;
			^vc.ctl[cKey].value_(this.interpFunc(start,target,interpValue.value))
		},{	// funky way around interpolating array of weights!!
			start = presets[presetName.value].voices[name].ctl[cKey].value[2];
			target = interpPreset.voices[name].ctl[cKey].value[2];
			wstart = presets[presetName.value].voices[name].ctl[cKey].value;
			this.interpFunc(start,target,interpValue.value);
			wend = wstart.copy.put(2,this.interpFunc(start,target,interpValue.value));
			^vc.ctl[cKey].value_(wend)
		})
	}

	vcClaveInterpolate {|name,vc,clvKey|
	// first test if data for this param is already present in presets
		var start, target, current;
		var clave = presets[presetName.value].voices[name].clv[clvKey];
		start = clave.copyRange(0, vc.ctl.cycle.value.asInt-1);
		target = interpPreset.voices[name].clv[clvKey];
		current = start.collect {|sv,i|
			this.interpFunc(sv,target.valueAt(i),interpValue.value)
		};
		^vc.clv[clvKey].value_(current);
	}

	interpolate {
		interpPreset = presets[data.global.ctl.intGoal.value];
		data.global.interp.keysValuesDo {|k,v| if(v.value,{ this.globalInterpolate(k) }) };
		data.voices.keysValuesDo {|name, vc|
			vc.interp.keysValuesDo {|k,v|
				if(vc.ctl.keys.includes(k),{
					if(v.value,{ this.vcCtlInterpolate(name, vc,k) })
				},{
					if(v.value,{ this.vcClaveInterpolate(name, vc,k) })
				})
			}
		}
	}

	free	{
		sampleBuffers.do {|buf| buf.free }
	}

}

RitmosVc {
	var <>player, <voiceName, <>synths;
	var <>data, <>voice, <>preset;
	var <>clock, <stream, <>downbeat,<nextDownbeat, <>cycOffset,<>changeTempoFlg=false,
		<rhythm, <>input, <>clave,<tuning,<>retuneFlg=false,
		<gA, <breeder, <rhythmTarg, <rhythmCritter, <>breedRate=10, <>startUpCycles=2,
		<listener, <>inPhrasesFlg = false,<>playPhrase,<>listenerIsVisible=false,
		<>pauseFlg=false, <netAddr,<>verbose=false;

	*new { arg player, voiceName;
		^super.newCopyArgs(player, voiceName).init
	}

	init {
		data = player.data;
		voice = data.voices[voiceName];
		synths = Array.fill(player.numCells,nil);	// store synths to turn off after sustain
		rhythm = Array.fill(voice.ctl.cycle.value,{0});
		input = Array.fill(rhythm.size,[0,60]);  // initialize array holding midi input
		this.tuning_(voice.ctl.tuning.value);
		this.clearPlayPhrase;
		this.initBreeder;
		^this;
	}

	initBreeder {		// setup genetic algorithm
		gA = GenAlg.new(critterSize: player.numCells);
		gA.mutationRate_(voice.ctl.mutRate.value);
		gA.valuePop(0.0);
	}

	initListener { 	// start recording and analyzing MIDI input
		var bounds;
		// replace \none as value for bounds, backwards compatability for old preset files
		if(voice.ctl.inBounds.value == [\none],{ voice.ctl.inBounds.input_([21,108])});
		if(listener.notNil,{	// create new listener if bounds or midiChan have changed
			bounds = listener.bounds;
			if((voice.ctl.inBounds.value != listener.bounds) ||
				(voice.ctl.inChan.value != listener.midiChan),{
					listener.stop;	// first, stop and remove old listener
					listener = MIDIListener.new(voice.ctl.inChan.value.asInt,
						voice.ctl.inBounds.value);
			})
			},{
				listener = MIDIListener.new(voice.ctl.inChan.value.asInt,voice.ctl.inBounds.value)
		});
		if(listener.isRunning.not,{
			listener.start;
			if(inPhrasesFlg,{ "starting".postln;
				{ listener.view;
					listener.window.visible_(listenerIsVisible) }.defer })
		});
		if(listener.pauseFlg,{ listener.unpause })
	}

	startBreeder	{
		this.stopBreeder;	// stop routine if running before restarting it
		// start playing the clave-rhythm
		clave = voice.clv[\amp].copyRange(0,voice.ctl.cycle.value.asInt-1);
		rhythm = Array.fill(voice.ctl.cycle.value.asInt,0);
		rhythmTarg = clave.copy*voice.ctl.weights.value[2]; // initialize target to clave rhythm
		rhythmCritter = rhythmTarg.copy;
		input = Array.fill(rhythm.size,[0,60]);
		gA.scoreAll(rhythmTarg);	// initialize gA pop
		if(inPhrasesFlg,{ {listener.view }.defer; listener.startPhraseAnalysis });
		breeder = Routine{ loop {this.breedRhythm; breedRate.reciprocal.wait }}.play(AppClock);
	}

	stopBreeder {
		if(inPhrasesFlg,{ listener.stopPhraseAnalysis });
		breeder.stop
	}

	getinput {
		var cycTime, cycEvents, lastCycInput, dbTime;
		// input is [velocity, note, swing, dur, phrasenum]
		input = Array.fill(rhythm.size,{[0,60,0,0.99,nil]});
		if(downbeat.notNil,{
			cycTime = rhythm.size*(clock.tempo.reciprocal);
			dbTime = downbeat - listener.startTime ;
			cycEvents = listener.history.detectIndex {|item,i|
				listener.onTime(i) < (thisThread.seconds-listener.startTime-cycTime)
			};
			if(cycEvents.notNil,{
		//		"db = ".post; dbTime.postln;
				lastCycInput = Array.fill(cycEvents,{|i|
					[listener.onTime(i),listener.velNum(i), listener.noteNum(i),
						listener.duration(i), listener.evChord(i),
						listener.phraseNum(i)]});
				lastCycInput.do {|vals,i| var ictus, swing, thisIctus;
					ictus = (vals[0] - dbTime) / (cycTime/rhythm.size);
					swing = if((ictus.frac <= 0.5),{ ictus.frac },{ 1.0-ictus.frac });
					// quantize input to ictus
					thisIctus = ictus.round%(rhythm.size);
					// save note vel, note,  swing, duration, and phrasenum
					if(input.at(thisIctus).at(0) == 0,{
						input.put((thisIctus),[vals[1],vals[2],swing,vals[3],vals[5]]);
					},{ // disposes of repeated notes on same ictus, saving only loudest
						if(vals[0] > input.at(thisIctus).at(0),{
							input.put((thisIctus),[vals[1],vals[2],swing,vals[3],vals[5]]);
						})
					});
		//			input.postln; "".postln
				}
			});
		})
	}

	newRhythmTarg {
		var inputRhythm;
		this.getinput; // compute input from MidiHistory
		inputRhythm = input.collect {|val| val[0].round(0.001) };
		// rhythmTarg = weighted sum of input, rhythmCritter, and this.rhythm
		^rhythmTarg = ((inputRhythm*voice.ctl.weights.value[0]) +
				(rhythmCritter*voice.ctl.weights.value[1]) +
					(clave*voice.ctl.weights.value[2]))
	}

	breedRhythm {
		// compute new target
		this.newRhythmTarg;
		// run genetic algorithm
		voice.ctl.numGen.value.do {|i|
			var size = voice.ctl.cycle.value.asInt;
			rhythmCritter = gA.tournament(size);
			gA.score(gA.sumDiffSquared(rhythmTarg,rhythmCritter));
			rhythmCritter = rhythmCritter.copyRange(0,size-1);  // 'score' extends rhythmCritter size!
		};
		gA.scoreAll(rhythmTarg);	// scoreAll after each #gen because target changes!
	}

	play {
		// start breeder Routine
		var clv, ctl, amps, events, freq, snd, clockRes;
		clv = voice.clv; ctl = voice.ctl;
		preset = player.presets[player.presetName.value].voices[voiceName];
		this.initListener;
		if(ctl.breedFlg.value,{ this.startBreeder });
		// start clock
		clock = TempoClock.new((data.global.ctl.tempo.value/60)*voice.ctl.beatDiv.value);
		stream = Routine {
			var susTime, keys, isStarting=true, bufOffset=0;
			loop {  // play loop, executes at beginning of every cycle
				cycOffset = 0;
				keys = clv.envir.keys; keys.remove(\sus); keys.remove(\amp);
				if(keys.includes(\bufnum),{
						bufOffset=player.sampleBuffers.buffers[0].bufnum });				clave = clv[\amp].copyRange(0,ctl.cycle.value.asInt-1);
				// record new downbeat
				if(ctl.startBeat.value.asInt == 0,{
					downbeat = thisThread.seconds
				},{
					downbeat = thisThread.seconds -
						(ctl.startBeat.value * (clock.tempo.reciprocal))
				});
		//		if((voiceName == \vc0),{downbeat.post; " = downbeat".postln });
				// if breeding, play gA's rhythmCritter
				if((ctl.breedFlg.value),{
					// make sure amps are same size as cycle (can be out of sync)
					amps = rhythmCritter.extend(ctl.cycle.value.asInt,0);
					if(ctl.cycCtr.value < startUpCycles,{ // stabilize clave at startup
						amps = (clave*ctl.weights.value[2]).collect {|v,i|
							if(v>rhythm[i],{v},{rhythm[i]})}
					})
				},{
					amps = clave*ctl.weights.value[2]  // else play clave
				});
				// gate out low values
				amps = amps.collect {|x| if((x>voice.ctl.gateLev.value),{x},{0})};
				// rhythm = current beats
				rhythm = amps.copyRange(0,ctl.cycle.value.asInt-1);
				// xfrm amps before playing??
				if(voice.ctl.xfrmFunc.value != 'echo',{
					 amps = RitmosXfrm.funcs[voice.ctl.xfrmFunc.value].value(amps,this)
				});
				// collect cycle indices of events to be played
				events = amps.collect {|amp,i| amp > 0 }.indicesOfEqual(true);
				// send preview of current cycle to net
				if((player.netFlg && netAddr.notNil),{
					SystemClock.sched(Server.default.latency,{
						netAddr.do {|addr|
							addr.sendMsg('/preview',
								voiceName.asString.last.digit,ctl.cycle.value.asInt,
								clock.tempo.reciprocal, '/evList', *events)
				}})});
				// tune freq data
				if(((keys.includes(\freq)) && (retuneFlg==true)
					&& (voice.ctl.tuning.value != \none)),{
						"retuning".postln;
					events.do {|i| clv[\freq].valuePut(i,
						(tuning.cps(clv[\freq].valueAt(i).cpsmidi.round(1.0)))) };
					retuneFlg = false;	// only retune freqs once after changing tuning
					"".postln;
				});
				// if starting, set cycle offset to startBeat
				if((isStarting),{
					cycOffset = (ctl.startBeat.value).asInt;
					isStarting = false;
				});
				// display input above current rhythm data
				{ if(input.notNil,{input[..(player.numCells-1)].do {|r,i|
						player.seqGui.voices[voiceName].cells[1]
								[i].value_(r[0].round(0.1))} }) }.defer;
				// display current rhythm above clave data
				{ if(amps.notNil,{ amps[..(player.numCells-1)].do {|r,i| var val;
						player.seqGui.voices[voiceName].cells[2][i].value_(r.round(0.1));
						if(r == 0,{  player.seqGui.voices[voiceName].showNoEvent(i) });
					} })
				 }.defer;
				// play cycle loop
				(ctl.cycle.value.asInt - cycOffset).do {|val, i|
					var thisSynth, thisIctus;
					// if downbeat of tempoChange, change tempi for all voices
					if(changeTempoFlg,{ player.changeTempo(data.global.ctl.tempo.value);
					 	changeTempoFlg = false;
					});
					i = (i + cycOffset);
					// turn off any synths scheduled to end
					if(synths[i].notNil,{
						Server.default.bind {
							synths[i].do {|synth| synth.set("gate",0) };
							synths.put(i,nil)
						}
					});
					player.seqGui.voices[voiceName].index_(i);  // show index in seqGui
					if((amps[i] > 0),{
						player.seqGui.voices[voiceName].showEvent(i);
						if(ctl.mute.value.not,{   // mute Synth play functions
							// play event with server latency to smooth rhythmic performance
							Server.default.bind { thisSynth = Synth(ctl.inst.value,
									(keys.collect {|key| [key,clv.at(key,i)] }
									.asArray.flatten ++ [\bufOffset,bufOffset] ++
										[\amp,amps[i]*(ctl.level.value-0.001)
											*(player.masterVol.value)]));
							};
							// compute sus time, length based on icti until next note
							if(ctl.inst.value.asString.last != $p,{ // no susTime needed for perc synths
								if(ctl.susDur.value.not,{
									susTime = (amps.copyRange(0,ctl.cycle.value.asInt-1)
									.rotate((i+1).neg)
										.detectIndex {|it| it > ctl.gateLev.value });
								},{ //	compute sustain length based on cycle time
									susTime = ctl.cycle.value;  // turns off before next cycle!!
								});
								if(susTime.notNil,{
									susTime = (susTime = susTime*clv.at(\sus,i)).max(1.0)
								},{ susTime = 1.0 });
								// synth-record in ictus when it will be turned off
								thisIctus = (i+susTime).mod(ctl.cycle.value).asInt;
								if(synths[thisIctus].isNil,{
									synths.put(thisIctus, [thisSynth])
								},{ synths[thisIctus].add(thisSynth)});
					//			" = susTime ".postln;
							});
					//	 	"on ".post; thisSynth.postln

							// if network enabled, send info on latest event
							if((player.netFlg && netAddr.notNil),{
								if(keys.includes(\bufnum),{
									snd = clv.at(\bufnum,i).asInt;
								});
								snd ?? { snd = 0 };
								if(keys.includes(\freq),{
									freq = clv.at(\freq,i);
								});
								freq ?? { freq = 0.0 };
								clock.sched(Server.default.latency,{
									netAddr.do { |addr|
										addr.sendMsg('/trg','/vc',voiceName.asString.last.digit,
											'/cycle',ctl.cycle.value.asInt,'/beat',i,'/amp',
										amps[i],'/frq',freq,'/snd',snd)}})
							});

					})});
	// if last event in cycle, update loop counter and schedule changePreset before next downbeat
					if( i == (amps.size-1),{
						// set nextDownbeat, used for syncStart -- BUT IT CAUSES A DOUBLE-START!!
						nextDownbeat = downbeat+(ctl.cycle.value*(clock.tempo.reciprocal));
						//  counter update for all voices
						ctl.cycCtr.value_(ctl.cycCtr.value+1);
					//	clockRes for last possible event, based on max 32 beatDivs of the player's clock tempo
						clockRes = (data.global.ctl.tempo.value/60).reciprocal/32;
						// schedule preset changes from ctlGUI events
						if(player.changePresetNum.notNil && (data.global.ctl.countVc.value == voiceName),{
							SystemClock.sched((clockRes+0.0001),{ player.loadPreset(player.changePresetNum);
							player.changePresetNum = nil  })
						});
						// test countLimit for countVc, if true schedule limitFunc for preset change
						if((data.global.ctl.countVc.value == voiceName) && (player.countFlg.value)
							 && ((ctl.cycCtr.value) > data.global.ctl.countLimit.value),{
								SystemClock.sched((clockRes+0.0001),{
									player.countLimitFuncs[data.global.ctl.limitFunc.value]
										.value})
						})
					});
					while({pauseFlg},{ 0.01.wait });
					1.0.wait
				};
				this.beatDivToTempo	// change beatDivs on cycle boundaries only
			}
		}.play(clock);
	}

	netAddr_ {|addrArray, port|
		if(addrArray.isString,{ addrArray = [addrArray] });
		netAddr = addrArray.collect {|addr| NetAddr(addr,port) };
	}

	inst_ {|name|
		voice.ctl.inst.value_(name.asSymbol)
	}

	tuning_ {|t|
		var ratios,tonicNote,freq,rt;
		if(t != \none,{
			if(t != \eq12,{
				ratios = KeyboardTunings.tunings[t];
				tonicNote = [\C,\Db,\D,\Eb,\E,\F,\Gb,\G,\Ab,\A,\Bb,\B].indexOf(voice.ctl.tonic.value);
				if((tonicNote != 0), {
					rt = ratios.size - tonicNote;
					ratios = ratios[rt..] ++ (ratios[..rt-1] * 2);
					ratios = ratios / ratios[0];
				});
				tonicNote = tonicNote+60;	// convert to mid octave for calibratenote
				// calibratefreq = equal 12Tet freq of root note
				freq = EqualTemperament.new(calibratefreq: 440, calibratenote: 69).cps(tonicNote);
				tuning = TuningRatios(ratios.size, freq, tonicNote, ratios);
				retuneFlg = true	// set flag to change frequencies
			},{
				tuning = EqualTemperament.new; retuneFlg = true
			})
		},{ tuning = nil })
	}

	// store phrase data to play, possibly over multiple ritmos cycles
	makePlayPhrase	{|targ|	// target PhrsNum from Listener
		targ = targ.asInt;
		playPhrase.put(\phrsNum,targ);
		playPhrase.put(\phrsDur,listener.phraseDur(targ));
		playPhrase.put(\phrsNotes,listener.evMelody(targ));
		 // phrsDurs are note->note durations, not on-off durations
		playPhrase.put(\phrsDurs,listener.evRhythm(targ));
		// phrsVels are average values of this pitch class within the phrase
		playPhrase.put(\phrsVels,listener.evMelody(targ).collect {|note|
			listener.vels(targ)[note.mod(12)] });
		"make new phrase = ".post; playPhrase[\phrsNum].post;
		" dur =  ".post; playPhrase[\phrsDur].postln;
		"   notes = ".post; playPhrase[\phrsNotes].postln;
		"   durs = ".post; playPhrase[\phrsDurs].postln;
		"   vels = ".post; playPhrase[\phrsVels].postln;
		"".postln
	}

	clearPlayPhrase {
		playPhrase = ( phrsNum: 0, phrsSegment: 1, phrsNumSegments: 0,
			phrsDur: 0, phrsNotes:  [], phrsDurs: [], phrsVels: [],
			phrsIndices: [], continue: false, lastCycle: false )
	}

	// extract newMelody from playPhrase, quantize it to cycle ictus, saving only one note per ictus
	// store note and ictus indices for each cycle segement of each phrase
	// play one cycle per execution, and clear playPhrase when last segment is played
	getCycleFromPhrase	{
		var newMelody,phrsDur,onsets,grid,quant,indices,qIndices,nilNotes;
		var startIndex,endIndex,startIctus,nextStartIctus,cycRemainTime,lastIndex,idxCount;
		// get current playPhrase data
		var phrsNum = playPhrase[\phrsNum];
		var phrsDurs = playPhrase[\phrsDurs];
		var phrsNotes = playPhrase[\phrsNotes];
		var phrsVels = playPhrase[\phrsVels];
		var phrsIndices = playPhrase[\phrsIndices];
		var phrsNumSegments = playPhrase[\phrsNumSegments];
		var phrsSegment = playPhrase[\phrsSegment];
		var ictus = clock.tempo.reciprocal;  // time of single ictus of this voice
		var cycTime = rhythm.size*ictus;
		var show = voice.ctl.mute.value.not;

		if(show,{ "".postln; "getCycleFromPhrase .. phraseNum = ".post; phrsNum.post });
		phrsDur=phrsDurs.integrate[phrsDurs.size-1]; " phraseDur = ".post; phrsDur.postln;
		if(playPhrase[\continue].not,{ // first time, quantize and analyze whole phrase into segments
			if(show,{"extract new phrase melody".postln;});
			onsets = [0] ++ phrsDurs.integrate; // onset times = 0 ++ duration between phrsNotes
			// "onsets = ".post; onsets.postln;
			// grid to quantize note onsets to NEAREST ictus
			grid = Array.fill((phrsDur+ictus/ictus).ceil+1,{|i| ictus*i });
			// "grid = ".post; grid.postln;
			// detect multiple notes per ictus
			quant = grid.collect {|gnum,i|
				onsets.select {|on,j| (on>(gnum-(ictus*0.5)))
					&&  (on<=(gnum+(ictus*0.5)))}}.select {|list| list.notEmpty};
		//	"quantized eventTimes = ".post; quant.postln;
			// find phrsNote indices of multiple notes at each sounded ictus
			qIndices = quant.collect {|p| p.collect {|i| onsets.indexOf(i) }};
			//  get indices of quantized events sort for max velocity, then save index of greatest
			indices = qIndices.collect {|ict,i| ict.collect {|n| [phrsVels[n],n] }}
			.collect {|p| p.sort({|a, b|  a[0] >= b[0] })}  // sorted phrsVels
			.collect {|v,i| v[0][1]};  // indices are qtized events with highest velocity
		//	"indices = ".post; indices.postln;
			//  remove lowest velocity notes from phrsNotes, phrsVels, phrsDurs
			qIndices=qIndices.flatten;
			phrsNotes=indices.collect {|idx,i| phrsNotes[idx] };
			phrsVels=indices.collect {|idx,i| phrsVels[idx] };
			phrsDurs=indices.collect {|idx,i| phrsDurs[idx] };
			// change phrsDurs to equal duration from note to note
			quant = indices.collect {|idx,i| onsets[idx] };
			phrsDurs = quant.collect {|time,i|
				if(i<(quant.size-1),{quant[i+1]-time },{quant[i]-time})};  // last phrsDur valu is always 0
			// eliminate any nil notes when phrase is only one note long!!
			nilNotes = phrsNotes.collect{|note,i| if(note.isNil, {i}) }.select {|idx| idx.notNil };
			nilNotes.do {|idx| phrsNotes.removeAt(idx); phrsVels.removeAt(idx); phrsDurs.removeAt(idx) };
	//		"q'tized phrsDurs = ".post; phrsDurs.post; " size = ".post; phrsDurs.size.postln;
	//		"q'tized phrsNotes = ".post; phrsNotes.postln; " size = ".post; phrsNotes.size.postln;
	//		"q'tized phrsVels = ".post; phrsVels.postln;
			// compute grid and phrsNote indices for each segment of the phrase
			startIctus = ((listener.phraseStartTime(phrsNum)-listener.phrasePauseSum(phrsNum))
				.mod(cycTime)/ictus).asInt;
	//		"startIctus = ".post; startIctus.postln;
			cycRemainTime = cycTime - (startIctus*ictus);
			startIndex=0;
			onsets= [0] ++ phrsDurs.integrate;  // redefine onset times
			endIndex = onsets.detectIndex {|on,i| on>cycRemainTime };
			if(endIndex.isNil,{endIndex = phrsNotes.size-1},{endIndex=endIndex-1});
			nextStartIctus = (((onsets[(endIndex+1).min(onsets.size-1)])-cycRemainTime).mod(cycTime)/ictus).round;
			// store for each phrase segment: startIndex,endIndex (phrsNotes),startIctus,nextIctus (grid)
			phrsIndices=List.newUsing([(startIndex: 0, endIndex: endIndex,
				startIctus:startIctus, nextIctus:nextStartIctus)]);
			// "phrase segment 1 phrsIndices= ".post; phrsIndices.postln;
			lastIndex=endIndex; idxCount=0;
			while({lastIndex < (phrsNotes.size)},{
				var strtIdx,endIdx,strtIct,nextIct;
				strtIdx=(phrsIndices[phrsIndices.size-1][\endIndex]+1).min(phrsNotes.size-1);
				endIdx=onsets.detectIndex {|on| on>(cycRemainTime+(cycTime*(idxCount+1))) };
				if(endIdx.isNil,{endIdx = phrsNotes.size},{endIdx=endIdx-1});
				strtIct= phrsIndices[phrsIndices.size-1][\nextIctus];
				nextIct=(((onsets[(endIdx+1).min(onsets.size-1)])-cycRemainTime).mod(cycTime)/ictus).round;
				phrsIndices.add((startIndex:strtIdx,endIndex:endIdx,startIctus:strtIct,nextIctus:nextIct ));
				lastIndex=endIdx; idxCount=idxCount+1;
			});
			if(show,{"phrase indices: ".post; phrsIndices.postln;});
			playPhrase.put(\phrsIndices,phrsIndices);
			phrsNumSegments=phrsIndices.size;
			playPhrase.put(\phrsNumSegments,phrsNumSegments);
			playPhrase.put(\phrsNotes,phrsNotes);
			playPhrase.put(\phrsVels,phrsVels);
			playPhrase.put(\phrsDurs,phrsDurs);
			},{
				if(show,{
					voiceName.post; " continuing phrase = ".post; phrsNum.post;
					" segment = ".post; phrsSegment.postln; "indices =  ".post;
					playPhrase[\phrsIndices][phrsSegment-1].postln; " ".postln;
				});
				startIndex = playPhrase[\phrsIndices][phrsSegment-1][\startIndex];
				endIndex = playPhrase[\phrsIndices][phrsSegment-1][\endIndex];
				startIctus= playPhrase[\phrsIndices][phrsSegment-1][\startIctus];
				nextStartIctus = playPhrase[\phrsIndices][phrsSegment-1][\nextIctus];
		});
		newMelody = Array.fill(startIctus,{[0,0]}) ++
			phrsDurs.copyRange(startIndex,endIndex).collect {|dur,i|
			[[phrsVels[i+startIndex],phrsNotes[i+startIndex]]] ++
				Array.fill((dur/ictus).round.asInt-1,{[0,0]})
			}.flatten;
		newMelody = newMelody.extend(rhythm.size,[0,0]);
		if((endIndex<(playPhrase[\phrsNotes].size-1)),{  // if phrase doesn't finish within this cycle
			playPhrase.put(\phrsSegment,phrsSegment+1);
			// "next cycle of phrase = ".post; phrsSegment.post; " ".postln;
			playPhrase.put(\continue,true)
			},{ // if phrase DOES finish in this cycle...
				if(show,{"playing last segment of phrase = ".post; phrsSegment.postln; "".postln});
				this.clearPlayPhrase
		});
		"newMelody = ".post;
		^newMelody.postln
	}

	// clock tempo includes beatDiv multiplier
	changeTempo {|newTempo|
		clock.tempo = newTempo/60 * voice.ctl.beatDiv.value;
	}

	beatDivToTempo {
		clock.tempo = data.global.ctl.tempo.value/60*voice.ctl.beatDiv.value
	}

	mute {
		voice.ctl.mute.value_(true)
	}

	unmute {
		voice.ctl.mute.value_(false)
	}

	pause {
		pauseFlg=true;
		{ this.release }.defer(1)    // release any sustaining notes
	}

	resume {
		pauseFlg=false;
	}

	release {
		Server.default.bind({
			synths.do {|synth,i|
				if(synth.notNil,{
					synth.do {|syn| syn.set("gate",0) };
					synths.put(i,nil)})
		}})
	}

	stop {
		//	"stopping voice ".post; voiceName.postln;
		stream.stop; this.stopBreeder; voice.ctl.cycCtr.value_(1); downbeat = nil;
		this.release;
		if(listener.notNil,{ listener.pause })
	}
}
