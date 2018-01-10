TxPlayer {
	var <>name, <>midiChan, <>fmArgs, <>presets,<fmArrays,<fmValues,<arrayGUIs, <valGUIs,
	<fmArrayArgNames,<group,<synth,<synths,<susNotes,susFlg=false,
	<pbends,<>ampScale,<>tuning,
	<>win, <>arrayNumDisplay, <>layout, <>presetName, <>presetNameView, <selectPresetView,
	<interpPresetNameView, <>interpPresetData, <>interpSliderView,
	<pButtons, saveFilePath, loadFilePath;

	*new {|argName,argChan=0,argPrstPath,argTuning|
		^super.new.init(argName,argChan,argPrstPath,argTuning)
	}

	init {|argName,argChan,argPrstPath,argTuning|
		var maxBus = Server.local.options.numAudioBusChannels;
		name = argName ?? { \default };
		midiChan = argChan;
		tuning = argTuning;
		presetName = \one;
		group = Group.new;
		fmArgs = (\waves: CV.new.sp([0,0,0],0,7,1,\lin),
			\amps: CV.new.sp([1,0.1,0.1,0.1],0,1,0,\amp),
			\rtios: CV.new.sp([1,2,3,4],0.0625,24,0.0625,\lin),
			\fxFreqs: CV.new(\widefreq,[0.1,0.1,0.1,0.1]),
			\times_1: CV.new.sp([0.01,0.1,0.5,0.1],0.001,1.0,0.01,\lin),
			\points_1: CV.new.sp([1,0.5,0.05],0.001,1.0,0.001,\lin),
			\times_2: CV.new.sp([0.01,0.1,0.5,0.1],0.001,1.0,0.01,\lin),
			\points_2: CV.new.sp([1,0.5,0.05],0.001,1.0,0.001,\lin),
			\times_3: CV.new.sp([0.01,0.1,0.5,0.1],0.001,1.0,0.01,\lin),
			\points_3: CV.new.sp([1,0.5,0.05],0.001,1.0,0.001,\lin),
			\times_4: CV.new.sp([0.01,0.1,0.5,0.1],0.001,1.0,0.01,\lin),
			\points_4: CV.new.sp([1,0.5,0.05],0.001,1.0,0.001,\lin),
			\lfoWvs: CV.new.sp([0.1,0,0,0],0,1,0,\amp),
			\lfoSpd: CV.new.sp(0.5,0,20,0,\lin),\pmod: CV.new(nil,0),
			\amods: CV.new(nil,[0,0,0,0]),\alg: CV.new.sp(1,1,8,1,\lin),
			\freq: CV.new(\freq,440),\sustain: CV.new.sp(1,0,10,0,\lin),
			\pan: CV.new(\bipolar,0),\fb: CV.new(nil,0),\out: CV.new.sp(0,0,maxBus,1,\lin),
			\amp: CV.new(nil,1),\ffrq: CV.new.sp(20000,100,20000,nil,20000),
			\effOut: CV.new.sp(0,0,maxBus,1,\lin), \effAmp: CV.new(nil,0),
			\vol: CV.new([0,3,\amp,0].asSpec,0.25),
			\interpPresetName: OV.new(\one,(_.asSymbol))
		);
		fmArrayArgNames = (
			\waves: [\wv1,\wv2,\wv3,\wv4],\amps: [\amp1,\amp2,\amp3,\amp4],
			\rtios: [\rtio1,\rtio2,\rtio3,\rtio4],
			\fxFreqs: [\fxFrq1,\fxFrq2,\fxFrq3,\fxFrq4],
			\amods: [\amod1,\amod2,\amod3,\amod4],
			\lfoWvs: [\lfoSinAmp,\lfoPlsAmp,\lfoSawAmp,\lfoNseAmp],
			\times_1: [\t1_1,\t2_1,\t3_1,\rls_1],\times_2: [\t1_2,\t2_2,\t3_2,\rls_2],
			\times_3: [\t1_3,\t2_3,\t3_3,\rls_3],\times_4: [\t1_4,\t2_4,\t3_4,\rls_4],
			\points_1: [\p1_1,\p2_1,\p3_1],\points_2: [\p1_2,\p2_2,\p3_2 ],
			\points_3: [\p1_3,\p2_3,\p3_3],\points_4: [\p1_4,\p2_4,\p3_4 ]
		);
		this.setChanMIDIdef(midiChan);
		presets = ( );
		this.gui;
		fmArgs[\interpPresetName].action_({|ov|
			if(ov.value.notNil,{
			{ interpPresetNameView.value_(interpPresetNameView.items.indexOf(ov.value))
			}.defer })
		});
		if(argPrstPath.notNil,{this.loadPresetsFile(argPrstPath)});
		synths = ();
		16.do {|i| synths.put(i,Array.fill(128,{[]}))}; // room to file midi triggered nodes
		susNotes=[];
		pbends=Array.fill(16,{1.0});
	}

	setChanMIDIdef {|chan|
		midiChan = chan;
		MIDIdef.noteOn ((name.asString++"On").asSymbol,{|vel,num,chan,src|
			var noteAmp = if(ampScale.notNil,{ampScale.map(num/127)*(vel/127)},{vel/127});
			var tdev = if(tuning.notNil,{
				KeyboardTunings.centsDevs[tuning][num.mod(12)]},{0});  // accommodate alt tunings
			var freq=(num+tdev).midicps*pbends[chan];
			"NoteOn ".post; [num,vel,chan].post; " ".post;
			this.playGuiSynth(freq,noteAmp);
			fmArgs[\freq].value_(freq);
			fmArgs[\amp].value_(noteAmp);
			"NoteOn ".post; " ".post; [num,vel,chan].post; " ".post;
			synths[chan][num].add(synth.postln);
			if(susFlg,{ susNotes.add(synths[chan][num].last) })
		},nil,chan-1).fix;

		MIDIdef.noteOff ((name.asString++"Off").asSymbol,{|vel,num,chan,src|
			if(susFlg.not,{synths[chan][num].removeAt(synths[chan][num].size-1).release
			},{ synths[chan].put(num,[]) })
		},nil,midiChan-1).fix;

		MIDIdef.cc ((name.asString++"cc").asSymbol,{|val,num,chan,src|
			switch(num,
				// "cc ".post; " ".post; [num,val,chan].post; " ".postln;
				1,{ {interpSliderView.valueAction_(val/127)}.defer },
				7,{ fmArgs[\vol].value_(\amp.asSpec.map(val/127)) },
				// axe11 encoder knob responders
				0,{ fmArgs[\alg].input_(val/127) },
				2,{ fmArgs[\lfoSpd].input_(val/127) },
				4,{ fmArgs[\pmod].input_(val/127) },
				8,{ fmArgs[\waves].value_(fmArgs[\waves]
					.value.put(0,[1,7,\lin,1].asSpec.map(val/127))) },
				9,{ fmArgs[\waves].value_(fmArgs[\waves]
					.value.put(1,[1,7,\lin,1].asSpec.map(val/127))) },
				10,{ fmArgs[\waves].value_(fmArgs[\waves]
					.value.put(2,[1,7,\lin,1].asSpec.map(val/127))) },
				11,{ fmArgs[\fb].input_(val/127) },
				12,{ fmArgs[\sustain].input_(val/127) },
				// sus pedal
				64,{ if(val==127,{ susFlg=true;
					susNotes= synths[chan].select {|n| n.notEmpty }; susNotes
				},{  susNotes.flatten.do {|n|  n.release };
					susNotes=nil; susFlg=false  })
				}
			);
		},nil,midiChan-1).fix;

		MIDIdef.touch ((name.asString++"Touch").asSymbol,{|val,chan,src|
			"aftertouch ".post; " ".post; [val,chan].post; " ".postln;
			synths[chan].do {|list| if( list.isEmpty.not,{
				list.do {|n| var curVol= fmArgs[\vol];
					n.postln.set(\vol,(curVol+((3.0-curVol)*(val/127))))
			}})}
		},chan: midiChan-1).fix;

		MIDIdef.bend ((name.asString++"pb").asSymbol,{|bend,chan,src|
			var ratio = 3.midiratio; 	// ratio to bend up by 3 semitones
			var spec = [ratio.reciprocal, ratio, \exponential].asSpec;
			var pbend = spec.map(bend/16383);
			pbends.put(chan,pbend);
			synths[chan].do {|list| if( list.isEmpty.not,{
				list.do {|n|
					n.set(\pbend,(pbend))
			}})}},chan: midiChan-1).fix
	}

	clearFmMIDIdefs {
		[(name.asString++"pb").asSymbol,(name.asString++"Touch").asSymbol,
			(name.asString++"cc").asSymbol,(name.asString++"On").asSymbol,
			(name.asString++"Off").asSymbol]
		.do {|name| MIDIdef.all[name].postln.free }
	}

	get {|name| ^fmArgs[name].value }

	getFxFrq {|i|
		if(fmArgs[\fxFreqs].value[i]>0.1,{ ^fmArgs[\fxFreqs].value[i] },{ ^0 })
	}

	playGuiSynth {|freq,amp,sus|
		if(freq.isNil,{freq=this.get(\freq)});if(amp.isNil,{amp=this.get(\amp)});
		if(sus.isNil,{sus=this.get(\sustain)});
		synth=Synth("txAlg"++(this.get(\alg).asString),[
			\freq,freq,\amp,amp,\sustain,sus,
			\pan,this.get(\pan),\fb,this.get(\fb),
			\rtio1,this.get(\rtios)[0],\rtio2,this.get(\rtios)[1],
			\rtio3,this.get(\rtios)[2],\rtio4,this.get(\rtios)[3],
			\fxFrq1,this.getFxFrq(0),\fxFrq2,this.getFxFrq(1),
			\fxFrq3,this.getFxFrq(2),\fxFrq4,this.getFxFrq(3),
			\wv1,TxSynthDefs.buffers[this.get(\waves)[0]],
			\wv2,TxSynthDefs.buffers[this.get(\waves)[1]],
			\wv3,TxSynthDefs.buffers[this.get(\waves)[2]],
			\amp1,this.get(\amps)[0],\amp2,this.get(\amps)[1],
			\amp3,this.get(\amps)[2],\amp4,this.get(\amps)[3],
			\t1_1,this.get(\times_1)[0], \p1_1,this.get(\points_1)[0].max(0.0001),
			\t2_1,this.get(\times_1)[1],\p2_1,this.get(\points_1)[1].max(0.0001),
			\t3_1,this.get(\times_1)[2],\p3_1,this.get(\points_1)[2].max(0.0001),
			\rls_1,this.get(\times_1)[3],
		\t1_2,this.get(\times_2)[0],\p1_2,this.get(\points_2)[0].max(0.0001),
			\t2_2,this.get(\times_2)[1],\p2_2,this.get(\points_2)[1].max(0.0001),
			\t3_2,this.get(\times_2)[2],\p3_2,this.get(\points_2)[2].max(0.0001),
			\rls_2,this.get(\times_2)[3],
			\t1_3,this.get(\times_3)[0],\p1_3,this.get(\points_3)[0].max(0.0001),
			\t2_3,this.get(\times_3)[1],\p2_3,this.get(\points_3)[1].max(0.0001),
			\t3_3,this.get(\times_3)[2],\p3_3,this.get(\points_3)[2].max(0.0001),
			\rls_3,this.get(\times_3)[3],
			\t1_4,this.get(\times_4)[0],\p1_4,this.get(\points_4)[0].max(0.0001),
			\t2_4,this.get(\times_4)[1],\p2_4,this.get(\points_4)[1].max(0.0001),
			\t3_4,this.get(\times_4)[2],\p3_4,this.get(\points_4)[2].max(0.0001),
			\rls_4,this.get(\times_4)[3],
			\lfoSinAmp,this.get(\lfoWvs)[0],\lfoSawAmp,this.get(\lfoWvs)[1],
			\lfoPlsAmp,this.get(\lfoWvs)[2],\lfoNseAmp,this.get(\lfoWvs)[3],
			\lfoSpd,this.get(\lfoSpd),\pmod,this.get(\pmod),
			\amod1,this.get(\amods)[0],\amod2,this.get(\amods)[1],
			\amod3,this.get(\amods)[2],\amod4,this.get(\amods)[3],
			\ffrq,this.get(\ffrq),\out,this.get(\out),
			\effOut,this.get(\effOut),\effAmp,this.get(\effAmp),
			\vol,this.get(\vol)], target: group
		);
		NodeWatcher.register(synth);
	}

	allNotesOff { group.freeAll }

	setSynthArray {|name,synth|
		var arrayVals = fmArgs[name].value;
		if(name ==\fxFreqs,{ arrayVals = arrayVals.collect{|f| if(f>0.1,{f},{0})}});
		if(synth.isNil,{ synth = this.synth });
		fmArrayArgNames[name].do {|argName,i|
				synth.set(argName,arrayVals[i])
		}
	}

	setSynthValue {|name,synth|
		if(synth.isNil,{ synth = this.synth });
		synth.set(name,fmArgs[name].value)
	}

	presetNameChange {|name|
		presetName = name.asSymbol
	}

	storePreset {
		var presetData = fmArgs.deepCopy;
		if(presets.keys.includes(presetName).not,{
			presets.put(presetName,presetData);
			interpPresetNameView.items_(presets.keys.asArray.sort);
			selectPresetView.items_(presets.keys.asArray.sort);
			selectPresetView.value_(selectPresetView.items.indexOf(presetName));
			},{
				ModalDialog.new({arg layout; ActionButton(layout,"ReplacePreset?")},
					name: "StorePreset",
					okFunc: {presets.put(presetName,presetData)},
					cancelFunc: {"choose new name!!".postln })
		})
	}

	deletePreset {|name|
		ModalDialog.new({arg layout;
			ActionButton(layout,"Are You Sure?")},
			name: "DeletePreset", okFunc: { presets.removeAt(name.asSymbol);
				interpPresetNameView.items_(presets.keys.asArray.sort);
				selectPresetView.items_(presets.keys.asArray.sort)})
	}

	replacePreset {|name|
		if(presets.keys.includes(name.asSymbol),{
			ModalDialog.new({arg layout; ActionButton(layout,"Overwrite?")},
				name: "ReplacePreset", okFunc: { presets.put(name.asSymbol,fmArgs.deepCopy)})
		})
	}

	loadPreset {|name|
		this.presetNameChange(name);
		presets[presetName].keysValuesDo {|k,v| fmArgs[k].value_(v.value) };
		this.loadInterpPreset(fmArgs[\interpPresetName].value);
		{ selectPresetView.value_(selectPresetView.items.indexOf(name));
		interpSliderView.value_(0); presetNameView.value_(presetName) }.defer
	}
	// this crashes if name is not already a presetName
	loadInterpPreset {|name|
		if(name != 'nil',{
			{ interpPresetNameView.valueAction_(
				interpPresetNameView.items.indexOf(name)) }.defer
		})
	}

	interpFunc {|x,y,i| if(x>y,{ ^((x-y)*(1-i)+y) },{ ^((y-x)*i+x) }) }

	interpPresets {|interp|
		fmArgs.keysValuesDo {|k,v|
			var thisPre = presets[presetName];
			var intVal = if(thisPre[k].value.isArray,{
				thisPre[k].value.collect {|val,i|
					this.interpFunc(val,interpPresetData[k].value[i],interp) }
				},{
					if([\freq,\amp,\sustain,\vol,\interpPresetName].includes(k).not,{
						this.interpFunc(thisPre[k].value,interpPresetData[k].value,interp)
			},{ v.value })});
			v.value_(intVal);
		}
	}

	presetAsData	{|prst|	// evaluate CV, OV, CVOrder data mirroring data structure
		prst.keysValuesChange {|k,v|
			if((v.class == CV) || (v.class == OV),{
				v.value
				},{
					v.asCompileString
			})
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
		//		prsts.put(\dir,sequence.value.deepCopy);   // insert directory
		file.write(prsts.asCompileString);
		file.close;
	}

	savePresetsDialog {
		File.saveDialog("save presets","",{ arg savePath;
			this.savePresetsFile(savePath)
		})
	}

	loadStringToPreset	{|loadPre,curPre|
		loadPre.keysValuesDo {|k,v|
			if(curPre[k].notNil,{
				curPre[k].value_(v.value)
			})
		};
	}

	loadPresetsFile {|path|
		var file, prsts,names;
		File.exists(path).if( {
			loadFilePath = path;
			file = File.new(path,"r");
			prsts = file.readAllString.interpret.postln;
			presets = (); names=[];
			// build new presets, transferring prst. values to CVs
			prsts.keys.do {|name|
				names=names.add(name);
				presets.put(name,fmArgs.deepCopy);
				this.loadStringToPreset(prsts[name],presets[name])
			};
			interpPresetNameView.items_(presets.keys.asArray.sort);
			selectPresetView.items_(presets.keys.asArray.sort);
			this.loadPreset(presets.keys.asArray.sort[0]);
			"loaded presets file, with preset names = ".post; names.postln;
		},{ "file not found !! ".postln; })
	}

	loadPresetsDialog {
		File.openDialog("load presets",{|path|
			this.loadPresetsFile(path.postln)
		})
	}

	// reload presetsFile
	reloadPresets {
		ModalDialog.new({arg layout;
			ActionButton(layout,"Erase Recent Edits?")},
			name: "ReLoad Presets",
			okFunc: { if(loadFilePath.notNil,{
				this.loadPresetsFile(loadFilePath)
		},{ "no presets loaded" }) })
	}

	// resave presets to last savePresetsFile
	resavePresets {
		ModalDialog.new({arg layout;
			ActionButton(layout,"Overwrite Last Saved Presets File?")},
			name: "ReSave Presets",
			okFunc: { if(saveFilePath.notNil,{
				this.savePresetsFile(saveFilePath)
		},{ "no presetsFile saved" }) })
	}

	makeLayout {
		layout = FlowView.new(win, win.view.bounds).resize_(5);
		layout.decorator.gap = TxSynthDefs.skin.gap;
		layout.decorator.margin = TxSynthDefs.skin.margin;
	}

	makePresetControls {
		var globalHeight = 50;
		var globalLayout = PeekView.new(layout, (layout.innerBounds.width)@50, "", true);
		var presetWidth = 400;
		var pLay = FlowView.new(globalLayout, Rect(2, 0, presetWidth, globalHeight));
		var cLay = FlowView.new(globalLayout,
			Rect(presetWidth+2,0,layout.innerBounds.width-presetWidth-2, globalHeight));

		var buttonWidth = 20;
		var buttonHeight = 20;

		var presetTitles = ["St","Rp","Rm","Ld","Sv","rL","rS"];
		var presetActions = [
			{|view| this.storePreset(presetName) },
			{|view| this.replacePreset(presetName) },
			{|view| this.deletePreset(presetName)  },
			{|view| this.loadPresetsDialog },
			{|view| this.savePresetsDialog },
			{|view| this.reloadPresets },
			{|view| this.resavePresets },
		];

		selectPresetView = GUI.popUpMenu.new(pLay, Rect(0, 0, 80, 20) )
		.items_(presets.keys.asArray.sort)
		.font_(Font(TxSynthDefs.skin.fontSpecs[0],TxSynthDefs.skin.fontSpecs[1]))
		.action_({|view|
			this.loadPreset(view.items[view.value]);
		});

		presetNameView = TextField.new(pLay, Rect(0,0,80,20))
		.align_(\center)
		.string_(\one)
		.font_(Font(TxSynthDefs.skin.fontSpecs[0],TxSynthDefs.skin.fontSpecs[1]))
		.action_({|view| this.presetNameChange(view.value)});

		pButtons = presetTitles.collect {|name, i|
			var res = GUI.button.new(pLay, Rect(0, 0, buttonWidth, buttonHeight) )
			.states_([[name]])
			.font_(Font(TxSynthDefs.skin.fontSpecs[0],TxSynthDefs.skin.fontSpecs[1]))
			.action_(presetActions[i]);
			res;
		};

		pLay.startRow;

		interpPresetNameView = GUI.popUpMenu.new(pLay, Rect(0, 0, 80, 20) )
		.items_(presets.keys.asArray.sort)
		.font_(Font(TxSynthDefs.skin.fontSpecs[0],TxSynthDefs.skin.fontSpecs[1]))
		.action_({|menu| fmArgs[\interpPresetName].value_(menu.items[menu.value]);
			interpPresetData = (presets[fmArgs[\interpPresetName].value]).deepCopy });

		interpSliderView = GUI.slider.new(pLay, Rect(0, 0, 256, 20) )
		.action_({|view|
			if(interpPresetData.notNil,{
				this.interpPresets (view.value)
			},{ "interpPresetData undefined".postln
			})
		});
		GUI.button.new(pLay, Rect(0,0,50,20)).states_(
			[["play",Color.black,Color.red],["stop",Color.black,Color.green]])
		.action_({|b| if( b.value==1,{
			this.playGuiSynth;
			{ b.value_(0); synth=nil }.defer(this.get(\sustain))
		},{ synth.free })});
	}

	gui { var chanView;
		win=Window.new("fm " ++ name.asString,Rect(6,805,710,261));
		this.makeLayout;

		// fm gui controls
		fmArrays=[\waves,\rtios,\amps,\times_1,\points_1,\times_2,\points_2,
			\times_3,\points_3,\times_4,\points_4,\amods,\lfoWvs,\fxFreqs];
		fmArrays.do ({|name,i|
			StaticText(layout,50@20).font_("Times",9)
			.stringColor_(Color.white).background_(Color.grey)
			.string=name.asString });
		layout.startRow;
		arrayGUIs= fmArrays.collect {|name,i|
			var m= MultiSliderView(layout,50@128);
			if([\waves,\points_1,\points_2,\points_3,\points_4].includes(name),
				{m.size=3},{m.size=4});
			m.elasticMode=1;
			m.value = fmArgs[name].value;
			fmArgs[name].connect(m).action_({
				var str = name.asString ++ " = " ++
				(fmArgs[name].value.collect{|val| val.round(0.001)}.asString);
				{ arrayNumDisplay.string_(str) }.defer;
				if(synth.notNil,{ if(synth.isRunning,{this.setSynthArray(name)}) });
			});

			m.valueThumbSize=4; m
		};
		layout.startRow;
		fmValues=[\alg,\freq,\amp,\sustain,\pan,\fb,\lfoSpd,\pmod,\ffrq,\effAmp,\effOut,\vol];
		valGUIs= fmValues.collect({|name,i|
			var n= EZNumber(layout,50@40,name,layout: \line2);
			n.setColors(Color.grey,Color.white);
			n.labelView.font_("Times",9).string=name.asString;
			n.value = fmArgs[name].value;
			fmArgs[name].connect(n.numberView)
			.action_({|nv| if((synth.notNil && synth.isPlaying),{ this.setSynthValue(name) }) });
			fmArgs[name];
		});
		chanView = EZNumber(layout,50@40,"chan",[1,16,\lin,1].asSpec,
			{|b|midiChan=b.value; this.setChanMIDIdef(midiChan) },midiChan,layout: \line2);
		chanView.setColors(Color.grey,Color.white);
		chanView.labelView.font_("Times",9);
		layout.startRow;
		arrayNumDisplay = StaticText(layout,220@15).font_("Times",9).string="value = nil ";
		this.makePresetControls;
		win.onClose_({this.allNotesOff; this.clearFmMIDIdefs });
		win.front;
	}

}
/*
TxSynthDefs.init
y.presets
t.interpPresets(x=1.0.rand); t.interpSliderView.value_(x)
x=TxPlayer.new(\sop,1,"/Users/chris1/Desktop/txTest2");
y=TxPlayer.new(\alto,2,"/Users/chris1/Desktop/txTest2");
z=TxPlayer.new(\tenor,3,"/Users/chris1/Desktop/txTest2");
u=TxPlayer.new(\bass,4,"/Users/chris1/Desktop/txTest2");
x.interpSliderView.bounds_(86,24,256,20)
x.presets[\one][\alg].value
y.presets[\two][\alg].value
x.storePreset(\three)
x.savePresetsFile("/Users/chris1/Desktop/txOne")
x.loadPresetsFile("/Users/chris1/Desktop/txOne")
x.loadPreset(\two)
x.synth.release
x.playGuiSynth
x.presets[x.presets.keys.asArray.sort[0]]
RitmosPlay
TxSynthDefs.skin
x.selectPresetView.items_(x.presets.keys.asArray.sort)
56.midicps
94.midicps
p = CV.new(\widefreq,[0.1,0.001,0,0])
u.fmArgs.keysValuesDo {|k,v| k.postln; v.postln; " ".postln }
p.input_([0.5,0.001,0.1,0.1])
	p.value
	\fxFrq1.asString.last.asString.asInt
	String
p = ( one: 1, two: 2)
p.keys.do {|k| k.post; " ".post; p[k].postln }
p.keys
RitmosPlay
RitmosCtlGUI
y.fmArgs.keysValuesDo {|k,v| k.post; v.postln }
			t.synths[1]
TxSusPlayer
*/