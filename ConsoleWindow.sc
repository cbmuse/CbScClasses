ConsoleWindow {
 	var <>window,<>console, effSliderView, <>effCtlSliders, effNumView, <>effToggleView, 	<>effCtlNumViews, effParamView, <>effParamLabels, effSendView, inBusView, <>curPresetView,
	<>targPresetView, <>findEffButton, storePresetButton, replacePresetButton, <>curVoiceView, 	deletePresetButton, loadButton, saveButton, <>interpSelBtn, <>interpSliderView, appendButton, <>sampPlaying, loopOnView, 	loopDurSlider, loopDurView, maxDur=20, sliderGUIView, sigNumView, 	<>signalSelect, <>effSelect, <>sigCtlSliders, 	sigCtlNumViews, sigParamView, sigSliderView, 	<>volSliderView, <>sigParamLabels, <>sigParamSpecs, <>effParamSpecs, thisSample, 	sigSynthAssgn, effSynthAssgn, <>sigRoutToggleView, sigRoutNamesView, <>sigRoutSelect, 	<>sigRoutTempoView,<>effRoutToggleView, effRoutNamesView, <>effRoutSelect, <>effRoutTempoView,
	plusPrst, minusPrst, synthDescs;

	*new { arg argConsole, argBounds;
		^super.new.initConsoleWindow(argConsole, argBounds)
	}

	truncLabels { arg synth;
		var labels;
		labels = "".scatList(synthDescs.at(synth).controlNames
			.collect({ |item| item = item.asString.copyRange(0,3);
				if( item.size < 4,{ (4 - item.size).do({ item = item ++ " " })});
				item
			})
		);
		if( labels.find("gate").notNil,{ // don't display any labels after "gate"
					labels = labels.select({ |item,i| i < labels.find("gate") });
				});
		^labels
	}

	initConsoleWindow	{ arg argConsole, argBounds;
		argBounds = argBounds ?? {Rect(280, 6, 1000,500)};
		window = Window.new("Console", argBounds, true, true, false);
		this.console = argConsole;
		synthDescs = SynthDescLib.global.synthDescs;
		volSliderView = Slider(window,Rect(505,450,127,20))
			.action_({ arg item;
				console.s.sendMsg ("/c_set", console.volBus.index,
					((\amp.asSpec).map(item.value)*3));
		});
		StaticText(window,Rect(505,470,70,20)).string_("masterVol");
		sigSliderView = HLayoutView(window,Rect(10,10,480,300));
		sigSliderView.setProperty(\spacing,10);
		effSliderView = HLayoutView(window,Rect(505,10,480,300));
		effSliderView.setProperty(\spacing,10);
		sigNumView = HLayoutView(window,Rect(10,305,480,18));
		sigNumView.setProperty(\spacing,0);
		effNumView = HLayoutView(window,Rect(505,305,480,18));
		effNumView.setProperty(\spacing,0);
		if( sigParamSpecs.isNil,{ sigParamSpecs = [];				console.numSignals.do({
			 	sigParamSpecs = sigParamSpecs.add(				[nil,nil,nil,nil,nil,nil,nil,nil,nil,nil,nil,nil,nil,nil,nil,nil])
			});
		});
		if( effParamSpecs.isNil,{ effParamSpecs = [];				console.numEffects.do({
			 	effParamSpecs = effParamSpecs.add(				[nil,nil,nil,nil,nil,nil,nil,nil,nil,nil,nil,nil,nil,nil,nil,nil])
			});
		});
		sigCtlSliders = Array.fill(console.numParams,{ arg i;
			// horizontal size of slider?
			Slider(sigSliderView,Rect(0,0,30,75))
				.action_({ arg item;
					var value, spec,thisSynth,sampCtlNum,controlNames;
					spec = sigParamSpecs.at(console.curSignalNum).at(i);
					if( spec.notNil,{
						value = spec.map(item.value) },{ value = item.value });
					console.sigCtlChange(console.curSignalNum,console.curVoiceNum,i,value);
					if( value < 1000,{
						sigCtlNumViews.at(i).value_(value.round(0.01))
						},{ sigCtlNumViews.at(i).value_(value.trunc(1.0)) });
				thisSynth = synthDescs[console.curSignals[console.curSignalNum].asSymbol];
					if(thisSynth.notNil,{
						if( thisSynth.controlNames.notNil,{
						controlNames = thisSynth.controlNames.collect {|n| n.asString };
							if(controlNames.includesEqual("sampBuf"),{
								controlNames.detect({|item,j|
									sampCtlNum = j; item == "sampBuf"
								});
								if(sampCtlNum == i,{
									thisSample.string_(console.sampBufs.at((value-1)
										.clip(0,console.sampleFiles.size-1).asInt).path)
								});
							});
							if(controlNames.includesEqual("dskBuf"),{
								controlNames.detect({|item,j|
									sampCtlNum = j; item == "dskBuf"
								});
								if(sampCtlNum == i,{
									thisSample.string_(console.diskInBufs.at((value-1)
										.clip(0,console.diskInFiles.size-1).asInt).path)
								});
							});
						});
					});
				})
			.keyDownAction_({nil})
		});
		effCtlSliders = Array.fill(console.numParams,{ arg i;
			Slider(effSliderView,Rect(0,0,30,75))
				.action_({ arg item;
					var value, spec;
					spec = effParamSpecs.at(console.curEffNum).at(i);
					if( spec.notNil,{
					value = spec.map(item.value) },{ value = item.value });
					console.effCtlChange(console.curEffNum,i,value);
					if( value < 1000,{
						effCtlNumViews.at(i).value_(value.round(0.01))
					},{ effCtlNumViews.at(i).value_(value.trunc(1.0)) });
				})
				.keyDownAction_({nil})
		});
		sigCtlNumViews = Array.fill(console.numParams,{ arg i;
			var box;
			box = NumberBox(sigNumView, Rect(0,0,30,18));
			box.align_(\center);
			box.font = Font("Times", 12);
			box.scroll_step_(0.01);
			box.action_({ arg item;
				var spec, value;
				spec = sigParamSpecs[console.curSignalNum][i];
				if(spec.notNil,{ value = spec.unmap(item.value) },{ value = item.value });
				console.sigCtlChange(console.curSignalNum,console.curVoiceNum,i,value);
				sigCtlSliders[i].value_(value);
				item.focus(false)
			})
		});
		effCtlNumViews = Array.fill(console.numParams,{ arg i;
			var box;
			box = NumberBox(effNumView, Rect(0,0,30,18));
			box.align_(\center);
			box.font = Font("Times", 12);
			box.scroll_step_(0.01);
			box.decimals_(4);
			box.action_({ arg item;
				var spec, value;
				console.effCtlChange(console.curEffNum,i,item.value);
				spec = effParamSpecs[console.curEffNum][i];
				if(spec.notNil,{
					value = spec.unmap(item.value)
				},{ value = item.value });
				effCtlSliders[i].value_(value);
				item.focus(false)
			})
		});
		sigParamLabels = Array.fill(console.numSignals,{""});
		sigParamView = StaticText(window,Rect(8,325,500,20));
			sigParamView.string_(sigParamLabels.at(0));
			sigParamView.font = Font("Monaco", 10);
			sigParamView.stringColor = Color.blue;
			sigParamView.align = \left;
		effParamLabels = Array.fill(console.numEffects,{""});
		effParamView = StaticText(window,Rect(503,325,500,20));
			effParamView.string_(effParamLabels.at(0));
			effParamView.font = Font("Monaco", 10);
			effParamView.stringColor = Color.blue;
			effParamView.align = \left;

		StaticText(window,Rect(190,345,90,25)).string_("sigROUTINES");
		sigRoutToggleView = Button(window,Rect(190,370,25,25));
			sigRoutToggleView.states = 								[["Off",Color.white,Color.black],["On",Color.white,Color.blue]];
			sigRoutToggleView.action_({ arg item;
				if( item.value == 0,{ console.sigRoutineOff(console.curSignalNum)
					},{ console.sigRoutineOn(console.curSignalNum); });
			});
		sigRoutNamesView = StaticText(window,Rect(220,370,80,20)).string_("sDefault");
		sigRoutSelect = PopUpMenu(window,Rect(190,400,100,20))
			.items_(Library.at(\consoleRoutines).keys.asArray
				.select({ |item| item.asString.at(0).asString == "s" }))
			.action_({ arg item;
				console.sigRoutNames.put(console.curSignalNum.post,
				sigRoutSelect.items.at(item.value).post);
				sigRoutNamesView.string_(sigRoutSelect.items.at(item.value));
			});
		sigRoutTempoView = NumberBox(window,Rect(190,425,35,20)).value_(120);
			sigRoutTempoView.align_(\center); sigRoutTempoView.font_(Font("Times",12));
			sigRoutTempoView.action_({ arg item;
				console.setSigRoutTempo(console.curSignalNum, item.value);
				item.focus(false)
			});
		StaticText(window,Rect(230,425,80,20)).string_("tempo");
		StaticText(window,Rect(670,345,90,25)).string_("effROUTINES");
		effRoutToggleView = Button(window,Rect(670,370,25,25));
			effRoutToggleView.states = 								[["Off",Color.white,Color.black],["On",Color.white,Color.blue]];
			effRoutToggleView.action_({ arg item;
				if( item.value == 0,{ console.effRoutineOff(console.curEffNum)
					},{ console.effRoutineOn(console.curEffNum); });
			});
		effRoutNamesView = StaticText(window,Rect(700,370,80,20)).string_("eDefault");
		effRoutSelect = PopUpMenu(window,Rect(670,400,100,20));
			effRoutSelect.items_(Library.at(\consoleRoutines).keys.asArray
				.select({ |item| item.asString.at(0).asString == "e" }));
			effRoutSelect.action_({ arg item;
				console.effRoutNames.put(console.curEffNum, effRoutSelect.items.at(item.value));
				effRoutNamesView.string_(effRoutSelect.items.at(item.value));
			item.focus(false)
			});
		effRoutTempoView = NumberBox(window,Rect(670,425,30,20)).value_(120);
			effRoutTempoView.align_(\center); effRoutTempoView.font_(Font("Times",12));
			effRoutTempoView.action_({ arg item;
				console.setEffRoutTempo(console.curEffNum, item.value); // fixed bug?
				item.focus(false)
			});
		StaticText(window,Rect(710,425,80,20)).string_("tempo");

		signalSelect = PopUpMenu(window,Rect(300,370,140,25));
		StaticText(window,Rect(300,345,60,25)).string_("SIGNALS");
			signalSelect.items_((console.signals[0].collect({|item|item})));
			signalSelect.action = { arg item;
				console.curSignalNum = item.value.clip(0, console.numSignals-1);
				sigParamView.string_(sigParamLabels.at(console.curSignalNum));
				this.curVoiceUpdate;
				sampPlaying.string_(
					console.keyQueues.at(console.curSignalNum).collect({ arg item; item.at(0) 						}).asString
				);
				sigRoutToggleView.value_(console.sigRoutinesToggle.at(console.curSignalNum));
				sigRoutNamesView.string_(console.sigRoutNames.at(console.curSignalNum));
				sigRoutTempoView.valueAction_(console.sigRoutTempi.at(console.curSignalNum));
			};
		sigSynthAssgn = PopUpMenu(window,Rect(300,400,140,25));
			sigSynthAssgn.items_(
			(ConsoleSynthDefs.synthDefs.collect {|syn| syn.name.asString }
			.select {|n|synthDescs[n.asSymbol]
				.controlNames.collect {|n|n.asString }.includesEqual("effIn").not}).sort);
			sigSynthAssgn.action_({ arg item;
				var cSrcNum, synth, specs, labels, maxParams;
				cSrcNum = console.curSignalNum;
				synth = (sigSynthAssgn.items.at(item.value)).asSymbol;
				signalSelect.items_(signalSelect.items.put(cSrcNum, synth.asString));
				console.curSignals.put(cSrcNum, synth.asString);
				synthDescs.at(synth).controlNames
			.collect {|n| n.asString }.detect({ |item, i|
					maxParams = i; item.asString == "gate" });
				specs = synthDescs.at(synth).controlNames
						.collect({ |item| item.asSymbol.asSpec })
						.collect({|item| if(item.class == ControlSpec,{ item },{nil}) });
				specs = specs.select({ |item,i| i<maxParams });
				sigParamSpecs.put(cSrcNum,specs);
				console.numVoices.do({ arg  i;  // initialize to spec defaults
					maxParams.do({ arg j;
						var val=0;
						if(specs.at(j).notNil,{
							val = specs.at(j).default
						});
						console.sigCtlBusValues.at(cSrcNum).at(i).put(j,val)
					});
				});
				labels = this.truncLabels(synth);
				sigParamLabels.put(cSrcNum,labels);
				this.curVoiceUpdate;			// load and unmap stored effect data to GUI
				sigParamView.string_(sigParamLabels.at(cSrcNum));
			});
		curVoiceView = NumberBox(window,Rect(10,400,25,20));
		curVoiceView.align_(\center);
		curVoiceView.font_(Font("Times",14));
		curVoiceView.value_(console.curVoiceNum+1);
		curVoiceView.action_({ arg item;
			console.curVoiceNum = (item.value-1).asInt.clip(0,console.numVoices-1);
			this.curVoiceUpdate;
			// item.focus(false)
		});
		StaticText(window,Rect(40,400,75,20)).string_("curVoice");
		effSelect = PopUpMenu(window,Rect(790,370,140,25));
		effSelect.items_(console.effects[0].collect({|item|item}));
		effSelect.action_({ arg item;
			effSelect.value_(item.value);		// set this for remote action activate
			console.curEffNum = item.value;
			this.curEffectUpdate;			// load and unmap stored effect data to GUI
			effParamView.string_(effParamLabels.at(item.value));
			effRoutToggleView.value_(console.effRoutinesToggle.at(console.curEffNum));
			effRoutNamesView.string_(console.effRoutNames.at(console.curEffNum));
			effRoutTempoView.valueAction_(console.effRoutTempi.at(console.curEffNum));
		});
		StaticText(window,Rect(790,345,60,25)).string_("EFFECTS");
		effSynthAssgn = PopUpMenu(window,Rect(790,400,140,25));
			effSynthAssgn.items_((ConsoleSynthDefs.synthDefs.collect {|syn| syn.name.asString }
				.select {|n| synthDescs[n.asSymbol].controlNames
				.collect {|n| n.asString }.includesEqual("effIn")}).sort);
			effSynthAssgn.action_({ arg item;
				var cEffNum, synth, specs, labels, maxParams;
				cEffNum = console.curEffNum;
				synth = (effSynthAssgn.items.at(item.value)).asSymbol;
				effSelect.items_(effSelect.items.put(cEffNum, synth.asString));
				console.curEffects.put(cEffNum, synth.asString);
				synthDescs.at(synth).controlNames.detect({ |item, i|
					maxParams = i; item.asString == "gate" });
				specs = synthDescs.at(synth).controlNames
						.collect({ |item| item.asSymbol.asSpec });
				specs = specs.select({ |item,i| i<maxParams });
				effParamSpecs.put(cEffNum,specs);
	 // initialize to spec defaults
				maxParams.do({ arg j;
					var val=0;
					if( specs.at(j).notNil,{ val = specs.at(j).default });
						console.effCtlBusValues.at(cEffNum).put(j,val)
				});
				labels = this.truncLabels(synth);
				effParamLabels.put(cEffNum, labels);
				this.curEffectUpdate;			// load and unmap stored effect data to GUI
				effParamView.string_(effParamLabels.at(cEffNum));
			});
		effToggleView = Button(window,Rect(790,430,25,25));
		effToggleView.states = [["Off",Color.white,Color.black],["On",Color.white,Color.red]];
		effToggleView.action = { arg item;
			if( item.value == 1,{ console.effectOn(console.curEffNum) },{				console.effectOff(console.curEffNum) });
		};
		effSendView = NumberBox(window,Rect(450,370,25,20));
		effSendView.align_(\center);
		effSendView.font = Font("Times", 14)	;
		effSendView.value_(1);
		effSendView.action_({ arg item; var val;
			val = item.value.clip(1,console.numEffects); effSendView.value = val;
			console.effSend.at(console.curSignalNum).
				put(console.curVoiceNum,(val-1).asInt);
			item.focus(false)
		});
		StaticText(window,Rect(445,390,50,25)).string_("effSend");
		inBusView = NumberBox(window,Rect(940,370,25,20));
			inBusView.align_(\center);
			inBusView.font = Font("Times", 14)	;
			inBusView.value_(1);
			inBusView.action_({ arg item; var val;
				val = item.value.clip(1,console.numEffects); inBusView.value = val;
				console.inBus.put(console.curEffNum,(val-1).asInt);
				item.focus(false)
			});
		StaticText(window,Rect(940,391,40,25)).string_("effBus");

		curPresetView = NumberBox(window,Rect(553,393,25,25));
			curPresetView.align_(\center);
			curPresetView.font = Font("Times", 14);
			curPresetView.value_(1);
			curPresetView.action_({ arg item;
				console.newPreset(item.value-1);
				item.focus(false)
			});

		if(this.console.interp,{
			targPresetView = NumberBox(window,Rect(640,442,25,25));
			targPresetView.align_(\center);
			targPresetView.font = Font("Monaco", 12);
			targPresetView.value_(2);
			targPresetView.action_({ arg item;
				console.targPreset_(item.value-1);
				item.focus(false)
			});
			interpSelBtn = Button(window,Rect(638,470,30,23));
			interpSelBtn.states=[
				["targ",Color.white,Color.black],["interp",Color.red,Color.green]];
			interpSelBtn.setProperty(\font,Font("Arial",9));
			interpSliderView = Slider(window,Rect(645,370,15,70)).value_(0)
				.action = { arg slider;
					if(interpSelBtn.value == 1,{console.interpolate(slider.value)}) };
		});

		minusPrst = Button(window,Rect(528,393,25,25));
		minusPrst.states=[["-",Color.white,Color.black]];
		minusPrst.action_({ console.newPreset(console.curPreset-1) });
		plusPrst = Button(window,Rect(578,393,25,25));
		plusPrst.states=[["+",Color.white,Color.black]];
		plusPrst.action_({ console.newPreset(console.curPreset+1)});
		StaticText(window,Rect(545,345,90,25)).string_("PRESETS");
		saveButton = Button(window,Rect(505,420,60,20));
		saveButton.states = [["save",Color.white, Color.red]];
		saveButton.action_({ console.savePresetFile });
		loadButton = Button(window,Rect(565,420,60,20));
		loadButton.states = [["load",Color.white,Color.blue]];
		loadButton.action_({
			File.openDialog("find your file",{ arg filename;
				var file; file = File(filename,"r");
				console.loadPresetFile(file); file.close;
				console.newPreset(0)
			})
		});
		storePresetButton = Button(window,Rect(505,368,40,20));
		storePresetButton.states = [["store",Color.white,Color.new255(255, 127, 80)]];
		storePresetButton.action_({ console.storePreset });
		replacePresetButton = Button(window,Rect(545,368,40,20));
		replacePresetButton.states = [["rplace",Color.white,Color.green]];
		replacePresetButton.action_({
			console.signals = console.signals.put(console.curPreset,
					console.curSignals.collect({ arg item; item.value }));
			console.effects =
				console.effects.put(console.curPreset,
					console.curEffects.collect({ arg item; item.value }));
			console.presets.put(console.curPreset,console.storePresetValues);
		});
		deletePresetButton = Button(window,Rect(585,368,40,20));
		deletePresetButton.states = [["delete",Color.white,Color.red]];
		deletePresetButton.action_({ console.deletePreset });
		StaticText(window,Rect(45,345,90,25)).string_("NOTES");
		loopDurSlider = Slider(window,Rect(10,370,100,20)).value_(1/maxDur)
			.action = { arg slider; var dur;
				dur = [0.1,40,\exp,0.01].asSpec.map(slider.value);
				console.loopDur.at(console.curSignalNum).put(console.curVoiceNum,dur);
				loopDurView.value_(dur)
			};
		loopDurView = NumberBox(window,Rect(115,370,30,20)).value_(1);
		loopDurView.align_(\center);
		StaticText(window,Rect(150,370,40,20)).string_("dur");
		sampPlaying = StaticText(window,Rect(10,450,200,20)).string_("file: ");
		thisSample = StaticText(window,Rect(20,475,400,20));
		loopOnView = Button(window,Rect(115,400,25,20));
		StaticText(window,Rect(150,400,40,20)).string_("sus");
		loopOnView.states = [ ["off",Color.white,Color.black],["on",Color.red,Color.white] ];
		loopOnView.action = { arg button;
			console.loopOn.at(console.curSignalNum).put(console.curVoiceNum,button.value);
		};
		findEffButton = Button(window,Rect(450,420,30,20));
		findEffButton.states = [["find",Color.black, Color.white]];
		findEffButton.action_({|item|
			var effIndex, effBus;
			effBus = console.effSend.at(console.curSignalNum).at(console.curVoiceNum);
			console.inBus.detect({ arg item, i;
				if( ((item == effBus) && (console.effToggle.at(i))) == 1,{ effIndex = i });
				((item == effBus) && (console.effToggle.at(i))) == 1
			});
			if( effIndex.notNil,{ effSelect.valueAction_(effIndex); });
			item.focus(false)
		});
		window.view.keyDownAction_(console.keyTrgFunc);
		window.onClose_({ console.shutDown });
		window.front;
	}

	curVoiceUpdate {
		var cSrcNum, cVcNum, sCtlBusVs;
		cSrcNum = console.curSignalNum; cVcNum = console.curVoiceNum;
		sCtlBusVs = console.sigCtlBusValues;
		sigCtlSliders.do({ arg item, i;
			var val, spec;
			spec = sigParamSpecs.at(cSrcNum).at(i);
			if( spec.notNil,{
				val = spec.unmap(
				sCtlBusVs.at(cSrcNum).at(cVcNum).at(i)) },{
				val = sCtlBusVs.at(cSrcNum).at(cVcNum).at(i)
			});
			item.value_(val); item.action.value(item);
		});
		effSendView.value = console.effSend.at(cSrcNum).at(cVcNum)+1;
		loopOnView.value = console.loopOn.at(cSrcNum).at(cVcNum);
		loopDurView.value = console.loopDur.at(cSrcNum).at(cVcNum);
		loopDurSlider.value = loopDurView.value/40;
	}

	curEffectUpdate {
		var cEffNum;
		cEffNum = console.curEffNum;
		if( effSelect.items.at(cEffNum) != "nil",{
			effCtlSliders.do({ arg item, i;
				var val, spec;
				spec = effParamSpecs.at(cEffNum).at(i);
				if( spec.notNil,{
					val = spec.unmap(console.effCtlBusValues.at(cEffNum).at(i)) },{
					val = console.effCtlBusValues.at(cEffNum).at(i) });
				item.value_(val); item.action.value(item);
			});
		});
		inBusView.value = console.inBus.at(cEffNum)+1;
		effToggleView.value = console.effToggle.at(cEffNum);
	}

}