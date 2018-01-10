
RitmosGUI {
	classvar <>skin;
	
	var <>win; // window
	var <>layout;
	
	var <>player,<>numVoices;
	var <>voices;
	
	var <>presetView, <>goalView, <>tempoView;
	var pButtons;
	var <cButtons;
	var <>limitView, <>limitVoiceView, <>limitFuncView, <>countFlagView, <>masterVolView;
	
	*initClass {
		skin = (
			fontSpecs:  ["Arial Narrow", 9],
			fontColor: 	Color.black,
			background: 	Color(0.8, 0.85, 0.7, 0.5),
			foreground:	Color.grey(0.95),
			onColor:		Color(0.5, 1, 0.5), 
			offColor:		Color.clear,
			gap:			0 @ 0,
			margin: 		2@2,
			buttonHeight:	16			
		);
		
	}
	
	*new { arg player, numVoices;
		^super.new.init(player, numVoices);
	}
	
	init { arg argPlayer, argNumVoices, argMaxLength;
		player = argPlayer ?? { RitmosPlayer.new };
		numVoices = argNumVoices ?? { player.numVoices };
//		voices = { IdentityDictionary.new() }!numVoices;
		this.makeGUI;
		win.onClose_({ this.player.stop });
		^this;
		
	}
	
	// sets up entire window, overall arrangement and tempo stuff
	makeGUI { arg argBounds;
		var bounds = argBounds ?? { Rect(45, 307, 720, (120*(numVoices+1)).min(800)) };
		win = GUI.window.new("Ritmos", bounds, scroll: true).front;
		win.view.hasHorizontalScroller_(false);
		win.acceptsMouseOver_(true);
		
		layout = FlowView.new(win, win.view.bounds)
			.relativeOrigin_(true)
			.resize_(5);
		layout.decorator.gap = skin.gap;
		layout.decorator.margin = skin.margin;
		
		this.makePresetControls;
		
		voices = Array.fill(numVoices, {|i|
			var res = RitmosVoiceGUI.new(layout, i, player);
			layout.hr(height: 5);
			layout.startRow;
			res;
		});
		
// ... finish		
	}

	updatePreset {
		presetView.value = player.preset;
		tempoView.view.value = player.tempo;
		limitView.view.value = player.countLimit;
		limitVoiceView.value = player.countLimitVoice;
		if(player.countLimitFunc != \none,{
			limitFuncView.value = player.countLimitFuncs.keys(Array).sort.indexOf			(player.countLimitFunc)});	
	}
		
	makePresetControls {
		var globalHeight = 50;
		var globalLayout = PeekView.new(layout, (layout.innerBounds.width)@50, "presets", true);
		var presetWidth = 280;
		var pLay = FlowView.new(globalLayout, Rect(2, 0, presetWidth, globalHeight));
		var cLay = FlowView.new(globalLayout, Rect(presetWidth+2, 0, globalLayout.innerBounds.width - presetWidth - 2, globalHeight) );
		
		var buttonWidth = 20;
		var buttonHeight = 20;
				
		var presetTitles = ["<",">","St","Rp","Rm","Ld","Sv","rL","rS"];
		var presetActions = [
			{|view| player.prevPreset; this.updatePreset },
			{|view| player.nextPreset; this.updatePreset },
			{|view| player.savePreset; this.updatePreset },
			{|view| player.replacePreset(presetView.value) },
			{|view| player.deletePreset(presetView.value) },
			{|view| player.loadPresetsDialog(presetView.value) },
			{|view| player.savePresetsDialog },
			{|view| player.reloadPreset },
			{|view| player.resavePreset }
		];
		
		var controlStates = [[["play"],["stop"]],[["pause"],["resume"]], [["mute"], ["unmute"]]];
		var controlActions = [
			{|view| if(view.value==1) { player.play } { player.stop } },
			{|view| if(view.value==1) { player.pause } { player.resume } },
			{|view| if(view.value==1) { player.mute } { player.unmute } }
		];
		var countFuncItems = player.countLimitFuncs.keys(Array).sort;

		
//		var o = 0@0;

		presetView = NumberBox.new(pLay, Rect(0, 0, 40, 20) )
			.step_(1)
			.align_(\center)
			.value_(0)
			.action_({|vw| player.loadPreset(vw.value.asInt.wrap(0,player.presets.lastIndex)); this.updatePreset });
			
//		o = o.translate(42@0);
		pButtons = presetTitles.collect {|name, i|
			var res = GUI.button.new(pLay, Rect(0, 0, buttonWidth, buttonHeight) )
				.states_([[name]])
				.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
				.action_(presetActions[i]);
//			o = o.translate((buttonWidth+2)@0);
			res;
		};

//		o = 0@20;
		pLay.startRow;
		
		goalView = NumberBox.new(pLay, Rect(0, 0, 40, 20) )
			.step_(1)
			.align_(\center)
			.value_(1)
			.action_({|vw| player.currentGoal = vw.value.asInt });
			
//		o = o.translate(42@0);
		GUI.button.new(pLay, Rect(0, 0, buttonWidth, buttonHeight) )
			.states_([["<"]])
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
			.action_({|view| player.prevGoal });
//		o = o.translate((buttonWidth+2)@0);
		GUI.button.new(pLay, Rect(0, 0, buttonWidth, buttonHeight) )
			.states_([[">"]])
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
			.action_({|view| player.nextGoal });
//		o = o.translate((buttonWidth+2)@0);
		GUI.slider.new(pLay, Rect(0, 0, 7*22, 20) );

		// control stuff
		
		tempoView = RitmosGUI.makeEasyNumBox(cLay, Rect(0, 0, 100, 20), \tempo, 
			{|view| player.changeTempo(view.value) }, 
			player.tempo ? 60, 0, inf, 1
			, envir: player.intEnvir
		);
		
		cButtons = controlStates.collect {|state, i|
			var res = GUI.button.new(cLay, Rect(0, 0, buttonWidth*2, buttonHeight) )
				.states_(state)
				.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
				.action_(controlActions[i]);
//			o = o.translate((buttonWidth+2)@0);
			res;
		};
		
		masterVolView = EZSlider.new(cLay, 160@20, "masterVol", [0,3,\amp,0], 
			{|me| player.masterVol_(me.value)},
			0.5, false, 55, 30
		);

		RitmosGUI.makeInterpolateSwitch(cLay, \masterVol, envir: player.intEnvir);
			
//		GUI.slider.new(cLay, Rect(0, 0, 80, 20) );
		
		cLay.startRow;
		
		limitView = RitmosGUI.makeEasyNumBox(cLay, Rect(0, 0, 150, 20), \countLimit, 
			{|view| player.countLimit_(view.value) }, 
			100, 1, inf, 1
			, envir: player.intEnvir
		);

		limitVoiceView = GUI.popUpMenu.new(cLay, Rect(0, 0, 60, buttonHeight) )
//			.font_(Font(RitmosGUI.skin.fontSpecs))
			.items_(Array.fill(numVoices, { |x| "voice %".format(x) }) )
			.action_({|view|
				player.countLimitVoice = view.value.asInteger;
			});

		limitFuncView = GUI.popUpMenu.new(cLay, Rect(0, 0, 60, buttonHeight) )
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
			.items_(countFuncItems)
			.action_({|view|
				player.countLimitFunc = countFuncItems.at(view.value);
			});

		GUI.staticText.new(cLay, Rect(0, 0, buttonWidth, buttonHeight) )
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
			.string_("countFlg");
		countFlagView = GUI.button.new(cLay, Rect(0, 0, buttonWidth, buttonHeight) )
				.states_([[" "],["X"]])
				.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
				.action_({|view| player.countFlg = (view.value>0) });

		layout.startRow;

	}
	
	// make small button that mutes or activates interpolation
	*makeInterpolateSwitch { arg lay, name, bounds, envir;
		var res;
		
		// TODO: should this be here? initalizing key for interpolation
		//envir !? { envir.put(name, true) };
//[lay, name, bounds, envir].postln;
		res = GUI.button.new(lay, bounds ?? { Rect(0,0,10,16) })
			.states_([
				["", Color.white, Color.blue(1.0, 0.5)], 
				["", Color.white, Color.red(1.0, 0.5)]
			])
			.canFocus_(false)
			.value_(0)
			.action_({ |me|
				if(me.value==1) {
					"interpolating % muted\n".postf(name);
					envir !? { envir.put(name.asSymbol, false) };
				} {
					"interpolating % activated\n".postf(name);
					envir !? { envir.put(name.asSymbol, true) };
				}
			});
		^res;
	}
	
	// make NumberBox with label and interpolate switch
	// assumes FlowView as parent
	*makeEasyNumBox { arg lay, bounds, name, action, 
					value=0, clipLo=0, clipHi=1, step=0.05, 
					labelRatio=0.5, numberRatio=0.4, envir;
		var res, width, height;
		
		// NOTE assuming skin.gap
		// NOTE was sending point as bounds...should be rect?
		width = ((bounds.asRect.width-12) - (skin.gap.x));
		height = bounds.asRect.height;
// TODO should this be it's own class?  result as envir is awkward/dangerous
		res = ( 
// TODO figure out good fixed sizes, don't use labelRatio
			text: GUI.staticText.new(lay, Rect(0,0,width*labelRatio, height))
				.string_(name)
				.align_(\right),
// NOTE NumberBox is not GUI...so not PC compatible
			view: NumberBox.new(lay, Rect(0,0,width*numberRatio, height))
				.action_(action)
				.value_(value)
				.clipLo_(clipLo)
				.clipHi_(clipHi)
				.step_(step)
				.align_(\center),
// TODO fix the whole interpolation switch envir thing -- dummy right now
			switch: RitmosGUI.makeInterpolateSwitch(lay, name, Rect(0,0,10,height), envir: envir)
		);

		^res;
	}
}

RitmosVoiceGUI {
	var <>parent;
	var <>name;
	var <>player;
	var <>layout, <>peekView;
	var <>interpolateStates; // envir of all the interpolateable values and if they're muted or not
	var <>paramKeys, <>paramColors;
	var <>currentParameter, <>currentParamColors;
	var <>synthWindow;

	var <rhythmLength;
	var <>rhythmLengthSpec;
	var <boxWidth;
	var <>numCells;
	var <>rhythmLayout, <>rhythmLengthSlider;
	var <>rhythmEditCells, <>rhythmPosCells;
	
	var <>zoomLength;
	var <>zoomFitButton, <>zoomAllButton, <>zoomRangeSlider;
	
	var <>sideLayout, <>paramSelectView, <>synthEditButton, <>xfrmFuncSelectView;
	
	var <>controlLayout, <>weightView, <>numGenView, <>mutRateView, <>gateView, <>breedView, breedViewNums;
	var <>countView, <>levelView, <>rhythmLengthView, <>beatDivView, <>instSelectView;

	var paramRhythmLists;
	
	var <>instrumentList;

// TODO this should be temporary...shouldn't it?  find better way
	var <>skin;
	
	*new { arg parent, name, player;
		^super.newCopyArgs(parent, name, player).init;
	}
	
	init { 
	
		instrumentList = [\pluck, \pluckR, \pluck2R, \dblPluckR, \dblStringR,
			\clave, \klnkR, \fmDrumR, 
			\pmDecayR,\pmR,  \stringR, \sineR, \formR, \granSamplerR, 
			\randGranSamplerR, \tGrainSampR, \tGrainScanR, \diskIn_granR];
	
		skin = (
			// rhythm colors
			colors: (
				rest: Color.grey(0.0, 0.8),
				wait: Color.yellow(1.0, 0.5),
				active: Color.green(1.0, 0.5),
				event: Color.red(0.5, 0.5),
				cellText: Color.white
			),
			fontSpecs:  ["Arial Narrow", 9],
			gap: 2@2,
			margin: 2@2
		);
// TODO think about just making this one color, not min and max
		paramColors = (
			amp: [Color(1.0, 0.953, 0.943, 1.0), Color(0.934, 0.481, 0.472, 1.0)],
			sus: [Color(0.953, 1.0, 0.954, 1.0), Color(0.519, 0.821, 0.604, 1.0)],
			freq: [Color(1.0, 0.951, 1.0, 1.0), Color(0.604, 0.481, 0.83, 1.0)],
			default: [Color(1.0, 1.0, 0.95, 1.0), Color(0.8, 0.8, 0.4, 1.0)]
		);

// TODO get from inital stuff
		rhythmLength = 8;
		zoomLength = 9;

// TODO change this variable name		
		numCells = if(player.notNil) { player.maxLength } { 64 };

// TODO these should be stored in a dictionary?
// TODO need method to add need params in
//		paramKeys = [\amp, \sus, \freq];
		paramKeys = player.voices[name].voiceEnvir.keys(Array);

//		paramKeys = [\amp, \sus, \freq];
		paramRhythmLists = IdentityDictionary.new;
// TODO need to be able to fill these with given data in
		paramKeys.do {|key|
			paramRhythmLists.put(key, {0}!numCells)
		};
		
		currentParameter = \amp;
		currentParamColors = paramColors.at(currentParameter);
		this.makeGUI;

		^this
	}
	
	makeGUI {	
// TODO figure out where/how to store defaults and constants
		var voiceHeight = 100;

		boxWidth = 18;
		
		peekView = PeekView.new(parent, (parent.innerBounds.width)@(voiceHeight), (name+1).asString, true );
		layout = FlowView.new(peekView, (peekView.bounds.width)@(voiceHeight) );
		layout.decorator.gap = RitmosGUI.skin.gap;
		layout.decorator.margin = RitmosGUI.skin.margin;
		
// TODO this should be saved elsewhere also
// NOTE took out steplength = 1.0 to join with zoomspec
		rhythmLengthSpec = ControlSpec(1, numCells, \lin, 0);
		
		this.makeRhythmView;
		this.makeSideView;
		this.makeControlView;
		
		this.setZoomRange(0, zoomLength);
// ... finish		
	}
	
	makeRhythmView {
// TODO this needs to be somewhere global
		var sideViewWidth = 66;
		var rhythmViewWidth = layout.innerBounds.width - sideViewWidth - (RitmosGUI.skin.gap.x*2);
		var sliderHeight = 10;
		var posCellHeight = 12;
		var editCellHeight = 20;
		var zoomViewHeight = 15;
		var zoomButtonWidth = 20;
		var rhythmViewHeight = sliderHeight + posCellHeight + editCellHeight + zoomViewHeight;

		var lay = GUI.compositeView.new(layout, Rect(2, 0, rhythmViewWidth, rhythmViewHeight));
		var o = 0@0;
			
	// actually doing GUI stuff here
		rhythmLayout = GUI.scrollView.new(lay, Rect(o.x, o.y, rhythmViewWidth, rhythmViewHeight-zoomViewHeight) )
			.hasBorder_(false)
			.relativeOrigin_(true)
			.hasHorizontalScroller_(false)
			.resize_(2);

		rhythmLengthSlider = GUI.slider.new(rhythmLayout, 
			Rect(o.x, o.y, boxWidth*numCells, sliderHeight) )
			.canFocus_(false)
			.thumbSize_(boxWidth)
			.step_((numCells-1).reciprocal)
			.value_( rhythmLength/numCells )
			.action_({|view|
				this.rhythmLength_(rhythmLengthSpec.map(view.value) ); 
				rhythmLengthView.view.value_(rhythmLength)
			});
		
		// update offset
		o = o.translate(0@sliderHeight);
		
// NOTE as it stands now if o.x != 0 then resize is broken
		rhythmPosCells = Array.fill(numCells, {|i|
			var pos = o.x + (i*boxWidth);
			GUI.numberBox.new(rhythmLayout, Rect(pos, o.y, boxWidth, posCellHeight) )
				.canFocus_(false)
				.align_(\center)
				.background_(skin.colors.rest)
				.normalColor_(skin.colors.cellText)
				.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
				.value_(i+1)
//				.mouseDownAction_({|view|
//					var val = (view.boxColor == (skin.colors.rest) ).binaryValue;
//
//					this.updateRhythm(\amp, i, val, view); 
//				})
		});

		// update offset
		o = o.translate(0@posCellHeight);

// TODO inital value should be given by input rhythm array
	
		rhythmEditCells = Array.fill(numCells, {|i|
			var pos = o.x + (i*boxWidth);
			NumberBox.new(rhythmLayout, Rect(pos, o.y, boxWidth, editCellHeight) )
				.canFocus_(true)
				.align_(\left)
				.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
				.value_(0)
				.clipLo_(0).clipHi_(1.0)
				.step_(0.05)
				.action_({|view|
					this.updateRhythm(currentParameter, i, view.value, view);
				})
		});

		o = o.translate(0@editCellHeight);

		zoomFitButton = GUI.button.new(lay, Rect(o.x, o.y, zoomButtonWidth, zoomViewHeight) )
			.states_([["fit"]])
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
			.action_({|view| this.setZoomRange(0, rhythmLength+1) });
		
		o = o.translate(zoomButtonWidth@0);
		
		zoomAllButton = GUI.button.new(lay, Rect(o.x, o.y, 20, zoomViewHeight) )
			.states_([["all"]])
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
			.action_({|view| this.setZoomRange(0, numCells) });

		o = o.translate(zoomButtonWidth@0);

		zoomRangeSlider = GUI.rangeSlider.new(lay, Rect(o.x, o.y, rhythmViewWidth - 40 - (skin.gap.x*0), zoomViewHeight) )
			.resize_(2)
			.canFocus_(false)
			.lo_(0).hi_(rhythmLengthSpec.unmap(rhythmLength+1))
			.action_({|me|
				this.setZoomRange(rhythmLengthSpec.map(me.lo), rhythmLengthSpec.map(me.hi));
			});

	}
	
	// draw paramSelectView and synthEditButton
	makeSideView {
		var sideViewWidth = 66;
		var rhythmViewWidth = layout.innerBounds.width - sideViewWidth - (skin.gap.x*2);
		var rhythmViewHeight = 46;
		var switchWidth = 10;
		var paramSelectHeight = 20;
		var synthEditHeight = 30;

// NOTE resize probably needs to change depending on placement of side (left or right)
		sideLayout = FlowView.new(layout, Rect(0, 0, sideViewWidth, rhythmViewHeight) )
			.resize_(3);
		sideLayout.decorator.gap = skin.gap;
		sideLayout.decorator.margin = skin.margin;
		
		paramSelectView = GUI.popUpMenu.new(sideLayout, Rect(0, 0, sideLayout.innerBounds.width-(switchWidth + (skin.gap.x*2)), paramSelectHeight) )
			.items_(paramKeys)
			.value_(0)
			.action_({|view| this.setCurrentParameter(paramKeys[view.value]) });

// TODO I get the feeling this might become a problem with multiple voices...
		RitmosGUI.makeInterpolateSwitch(sideLayout, currentParameter, envir: player.voices[name].intEnvir);
		
		sideLayout.startRow;
		
		synthEditButton = GUI.button.new(sideLayout, Rect(0, 0, sideLayout.innerBounds.width, synthEditHeight) )
			.states_([["synth", Color.black, Color.gray(0.8, 0.5)]])
			.action_({|view|
				this.openSynthEditWindow();
			});

	}

// draw weightViews, gen params, count, level, inst, cycle, beatDiv, and xfrmFuncSelect
	makeControlView {
		layout.startRow;
		controlLayout = PeekView.new(layout, Rect(0, 0, layout.innerBounds.width-40, 44), "ctrls", false, Rect(0, 0, 36, 16) );
		controlLayout.hasBorder_(false).resize_(1).relativeOrigin_(true);
		
		weightView = GUI.multiSliderView.new(controlLayout, Rect(0, 0, 120, 40) )
			.canFocus_(false)
			.value_([0.5, 0.0, 0.5])
			.indexIsHorizontal_(false )
			.gap_(4)
			.indexThumbSize_(10)
			.valueThumbSize_(20)
			.isFilled_(true)
			.colors_(Color.clear, Color.green(1.0, 0.5))
			.background_(Color.black)
			.action_(	{|me| 
				var newValues;
				newValues = me.value.normalizeSum;
				if(newValues[me.index]>=0.99) { newValues = newValues.normalize(0.01, 0.99) };
				if( newValues.includes(0/0).not ) {
					me.value_(newValues.normalizeSum);
				} {
					me.value_([0.333, 0.333, 0.333]);
				};
				player.voices[name].weights = newValues.copy;
			});
		
		Array.fill(3, {|i| 
			GUI.staticText.new(controlLayout, Rect(2, (14*i), 20,12))
				.string_(["my1","cur","clv"][i])
				.stringColor_(Color.gray(1.0,0.5)); 
		});

		RitmosGUI.makeInterpolateSwitch(controlLayout, "weights", Rect(120, 0, 10, 39), 
			envir: player.voices[name].intEnvir);
		
		controlLayout.flow({ arg gaValLayout;
			numGenView = RitmosGUI.makeEasyNumBox(gaValLayout, Rect(0, 0, 120, 12), "numGen",
				{|me| player.voices[name].numGen_(me.value) },
				4, 1, 64, 1,
				envir: player.voices[name].intEnvir
			);
			
			gaValLayout.startRow;
			
			mutRateView = RitmosGUI.makeEasyNumBox(gaValLayout, Rect(0, 0, 120, 12), "mutRate",
				{|me| player.voices[name].gA.mutationRate_(me.value) },
				0.25, 0, 1, 0.05,
				envir: player.voices[name].intEnvir
			);
			
			gaValLayout.startRow;
			
			gateView = RitmosGUI.makeEasyNumBox(gaValLayout, Rect(0, 0, 120, 12), "gateLev",
				{|me| player.voices[name].gate_(me.value) },
				0.25, 0, 1, 0.05,
				envir: player.voices[name].intEnvir
			);

		}, Rect(128, 0, 128, 40) );	
		
		controlLayout.flow({ arg claveLayout;
			countView = RitmosGUI.makeEasyNumBox(claveLayout, Rect(0, 0, 100, 18), "count",
				{|me| "implement me!".postln },
				0, 0, inf, 1
				, envir: player.voices[name].intEnvir
			);
			
			levelView = EZSlider.new(claveLayout, 160@16, "level", [0,3,\amp,0], 
	//			{|me| player.changeVols(me.value,name+1)},
				{|me| player.voices[name].vol_(me.value)},
				0.5, false, 40, 40
			);
			RitmosGUI.makeInterpolateSwitch(claveLayout, "level", Rect(0,0,10,16), envir: player.voices[name].intEnvir);
			
			xfrmFuncSelectView = GUI.popUpMenu.new(claveLayout, (60@18));

			claveLayout.startRow;

			GUI.staticText.new(claveLayout, Rect(0, 0, 24, 18) ).string_("inst ").align_(\right);
			instSelectView = GUI.popUpMenu.new(claveLayout, (72@18)).items_(instrumentList)
				.action_({|me| player.voices[name].synth_(instSelectView.items[me.value])});

			rhythmLengthView = RitmosGUI.makeEasyNumBox(claveLayout, Rect(0, 0, 120, 18), "cycle",
				{|me| var voice = player.voices[name];
					rhythmLength = me.value.asInt; 
//					voice.rhythm = paramRhythmLists.at(\amp).copyRange(0,rhythmLength-1); 
//					voice.params = paramRhythmLists.collect({|array| 
//							array.copyRange(0,rhythmLength-1) });
					voice.changeCycle(rhythmLength) },
				rhythmLength, 1, numCells, 1
				, envir: player.voices[name].intEnvir
			);

			beatDivView = RitmosGUI.makeEasyNumBox(claveLayout, Rect(0, 0, 120, 18), "beatDiv",
	//			{|me| player.changeBeatDivs(me.value.asInt,name+1) },
				{|me| player.voices[name].beatDiv_(me.value.asInt) },
				4, 1, 32, 1
				, envir: player.voices[name].intEnvir	
				
			);


// TODO change this so that it reads the list from somewhere globalish
			instSelectView = GUI.popUpMenu.new(claveLayout, (72@18)).items_(instrumentList)
				.action_({|me| player.voices[name].synth_(instSelectView.items[me.value])});
			RitmosGUI.makeInterpolateSwitch(claveLayout, \instrument, envir: player.voices[name].intEnvir);
			
			
		}, Rect(260, 0, 400, 42) );
		
	}
	
	
// TODO this whole thing is a bit clunky
	updateRhythm { |param, index, value, changer|
		var c = paramColors.at(param) ?? { paramColors.at(\default) };
		var spec = Spec.specs.at(param) ?? { Spec.specs.at(\unipolar) };
		if(changer == player) {
			if(currentParameter == param) { 
				{
					rhythmEditCells[index].value_(value);
					rhythmEditCells[index].background_( c[0].blend(c[1], spec.unmap(value)) );
					nil
				}.defer;
			};
			if(param==\amp) {
				{
					rhythmPosCells[index].background_( 
						if(value>0) { skin.colors.event } { skin.colors.rest }
					);
					nil
				}.defer;
			};
		} {
			player.voices[name].voiceEnvir.at(param).put(index, value);
// TODO it's clunky to check if amp or not, but necessary for now
// NOTE assuming name is starting at 0
			if(param==\amp) { 
				player.voices[name].rhythm.put(index, value);
				rhythmPosCells[index].background_( 
					if(value>0) { skin.colors.event } { skin.colors.rest } 
				)
			};
			rhythmEditCells[index].background_( c[0].blend(c[1], spec.unmap(value) ) );

		};
		
		

	}

// TODO fix color problem on update...and probably align also
	setCurrentParameter { arg key;
		var array = player.voices[name].voiceEnvir.at(key);
// TODO maybe keep this in preset?
		var spec = Spec.specs.at(key) ?? { Spec.specs.at(\unipolar) };
		currentParameter = key;
		currentParamColors = paramColors.at(key) ?? { paramColors.at(\default) };
		rhythmEditCells.do {|view, i|
			view.clipLo_(spec.minval)
				.clipHi_(spec.maxval)
				.step_(if(spec.step.notNil && { spec.step>0 }) {spec.step} { spec.range/64 })
		};
		array.do {|val, i|
			this.updateRhythm(key, i, val, player);
		};

	}
	
	openSynthEditWindow {
// TODO make it so this shows which voice it belongs to and the name of the synth
// TODO the synth window should be a seperate window really
// TODO the window should take into account how far down the voice is in the gui
		synthWindow ?? { synthWindow = GUI.window.new("instrument", Rect(parent.bounds.right, parent.bounds.bottom-300, 300, 300) ).front };
		"not yet implemented!".postln;

	}

/*
(
c = RitmosPlayer.new;
s.waitForBoot {
	c.play;
};
)
c.voiceEnvir[0]

*/	

	setZoomRange { arg start=0, end=8;
		zoomLength = (end - start).abs.clip(1, numCells);
		boxWidth = rhythmLayout.bounds.width/zoomLength;
		rhythmLengthSlider.bounds = rhythmLengthSlider.bounds.width_((boxWidth)*numCells);
		rhythmLengthSlider.thumbSize = boxWidth;

// NOTE this assumes 0 xOffset
		numCells.do {|i|
			var pos = i*boxWidth;
			rhythmPosCells[i].bounds = rhythmPosCells[i].bounds.left_(pos).width_(boxWidth);
			rhythmEditCells[i].bounds = rhythmEditCells[i].bounds.left_(pos).width_(boxWidth);
		};

		rhythmLayout.visibleOrigin = (rhythmLayout.innerBounds.width*rhythmLengthSpec.unmap(start))@0;

	}
	
// TODO this should probably be setRhythmLength()
	rhythmLength_ { arg newLength;
		var oldLength = rhythmLength;
		rhythmLength = newLength.asInteger.clip(1, numCells);
// ["old",oldLength,"new",rhythmLength,"ply",player.voices[name].rhythm.size].postln;		
// NOTE this still assumes funky method of making NumberBox view (i.e. envir instead of class)
/*		rhythmLengthView !? { rhythmLengthView.view.value_(rhythmLength) }; */
		if(player.voices[name].clock.notNil) {
			{ 
				rhythmPosCells[(rhythmLength-1)..(oldLength-1)].do { |cell| 
					cell.background_(skin.colors.wait)
				}; nil
			}.defer;
			if(player.voices[name].rhythm.size != rhythmLength) {
				player.voices[name].clock.playNextBar {
	"set voice % rhythm to %\n".postf(name, rhythmLength);
					{ 
						rhythmEditCells[rhythmLength..(oldLength-1)].do { |cell|
							cell.background_(Color.white)
						}; nil
					}.defer(player.voices[name].clock.beatDur*player.voices[0].clock.beatsPerBar);

					player.voices[name].rhythm_(
						player.voices[name].voiceEnvir.at(\amp)[0..(rhythmLength-1)]
					); 
					nil;
				};
			}
		} {
			if(player.voices[name].rhythm.size != rhythmLength) {
				player.voices[name].rhythm_(
					player.voices[name].voiceEnvir.at(\amp)[0..(rhythmLength-1)]
				)
			};
		};

		if(zoomLength<=rhythmLength) {
			// if we haven't scrolled then zoom to fit
			if(rhythmLayout.visibleOrigin.x < boxWidth) {
				zoomRangeSlider.activeHi_(rhythmLengthSpec.unmap(rhythmLength+1));
			}
		};

	}
	
}
