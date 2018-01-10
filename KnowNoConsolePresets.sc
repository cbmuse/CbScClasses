KnowNoConsolePresets {
	var <>network,<>console,<>fbCtls,<>fbAssgns,<>heldNotes,
	<>presets, <>preset, <>curPresetName, <>targPreset, prstSelect, <>cnslPrst=0,
	<interpRoutine,interpSlider,<interpVal,<interpTime,
	<curPrstGUI,<targPrstGUI,<goBtn,<secsToTargGUI,<wind;

	*new {|network,console|
		^super.newCopyArgs(network,console).init
	}

	init {
		presets = (); preset = ();
		this.getViewValsToPrst;  // intialize instance variables
		presets.add(1-> preset); curPresetName=1;
		targPreset=presets[1]
	}

	getViewValsToPrst { // get values from views to preset
		preset.put(\fbCtls,network.paramCtlViews.collect {|view|
			[view.frqProb.value,view.frqScale.value,view.frqOffset.value,
				view.frqBlend.value,view.timbrProb.value,view.timbrScale.value,
				view.timbrOffset.value,view.timbrBlend.value,
				view.ampProb.value,view.ampScale.value,view.ampOffset.value,
				view.ampBlend.value]
		});
		preset.put(\fbAssgns,network.paramCtlViews.collect {|view|
			[view.netPlayer.value,view.frqCtlDest.value,
				view.timbrCtlDest.value,view.ampCtlDest.value] });
		if(console.notNil,{
			preset.put(\heldNotes,
				console.keyQueues.collect {|synths,k|
					synths.collect {|sig| [k,sig[1]] }}.flatten);
			preset.put(\cnslPrst, console.curPreset)
		})
	}

	putPrstValsToViews {  // put values to views from preset
		network.paramCtlViews.do {|view,i|
			[view.frqProb,view.frqScale,view.frqOffset,view.frqBlend,
				view.timbrProb,view.timbrScale,view.timbrOffset,view.timbrBlend,
				view.ampProb,view.ampScale,view.ampOffset,view.ampBlend].do {|ctl,j|
				ctl.value_(preset[\fbCtls][i][j])};
			[view.netPlayer,view.frqCtlDest,view.timbrCtlDest,view.ampCtlDest].do
			{|assgn,j| assgn.value_(preset[\fbAssgns][i][j].asInt)}
		};
		if(console.notNil,{
			heldNotes.do {|note| console.voiceOn(note[0],note[1])};
			cnslPrst = preset[\cnslPrst];
		});
	}

	savePresetsFile {|path|
		var file;
		path ?? {path = "presets"++Date.localtime.stamp};
		file = File.new(path, "w");
		file.write(presets.asCompileString);
		file.close;
	}

	loadPresetsFile {|path|
		var file;
		File.exists(path).if( {
			file = File.new(path,"r");
			if(console.notNil,{
				console.keyQueues.collect {|synths,k|
					synths.collect {|sig| [k,sig[1]] }}.flatten.do {|note|
					console.voiceOff(note[0],note[1]
				)};  // turn off currently held notes
			});
			presets = file.readAllString.interpret;
			presets.postln;
			this.loadPreset(1);
		})
	}

	loadPreset {|name|
		if(console.notNil,{
			~holdNotes.do {|hold| if(hold.isEmpty.not,{
				hold.do {|note|
					console.voiceOff(((note[0]/12).floor-3)
						.mod(console.numSignals),note[0].mod(12))}})
		}});
		name=name.wrap(1,presets.size);
		preset = presets[name];
		this.putPrstValsToViews;
		curPresetName = name;
		if(console.notNil,{
			// first need to release any holds?? or done by changePreset midiFunc?
			if(~holdsGuis.notNil,{ 4.do {|i| {~holdsGuis[i].value_(0)}.defer } });
			preset[\heldNotes].do {|note|
				var noteNum=(note[0]*12)+36+note[1];
				console.voiceOn(note[0],note[1]);
				if((~holds.notNil && ~holdsGuis.notNil && ~holdNotes.notNil),{
					~holds.put(note[0],true); {~holdsGuis[note[0]].value_(1)}.defer;
					~holdNotes=~holdNotes.put(note[0],~holdNotes[note[0]]
						.add([noteNum,64,9,1]))
				})};
			"load console preset = ".post; console.newPreset(cnslPrst.postln)
		})
	}

	loadFileDialog {
		File.openDialog("find KnowNoConsole presetsFile",{ arg filename;
			var file; file = File(filename,"r");
			presets = file.readAllString.interpret;
			preset = presets[1];
			file.close;
		},{ "loadFile cancelled!".postln })
	}

	saveFileDialog {
		var f;
		File.saveDialog("","",{ arg savePath;
			f = File(savePath,"w");
			f.write(presets.asCompileString);
			f.close
		},{ "save cancelled !!".postln })
	}

	savePreset {|name|
		this.getViewValsToPrst;
		presets = presets.add(name->preset);
		{ curPrstGUI.value_(name) }.defer
	}

	interpPresets {|intVal|
		network.paramCtlViews.do {|view,i|
			var ctlDiffs = presets[curPresetName][\fbCtls].collect {|curVal,j|
				targPreset[\fbCtls][i][j]-curVal };
			var assgnDiffs= targPreset[\fbAssgns]-(presets[curPresetName][\fbAssgns]);
			[view.frqProb,view.frqScale,view.frqOffset,view.frqBlend,
				view.timbrProb,view.timbrScale,view.timbrOffset,view.timbrBlend,
				view.ampProb,view.ampScale,view.ampOffset,view.ampBlend].do {|ctl,j|
				ctl.value_(ctlDiffs[i][j]*interpVal+presets[curPresetName][\fbCtls][i][j])};
			[view.netPlayer,view.frqCtlDest,view.timbrCtlDest,view.ampCtlDest].do {|assgn,j|
				assgn.value_(((assgnDiffs[i][j]*interpVal
					+presets[curPresetName][\fbAssgns][i][j]).round))
			};
			// heldNotes ??
		};
	}

	gui {
		var q;
		wind = Window.new("KnowNoConsole Presets",Rect(560, 700, 300, 160)).front;
		q = wind.view.addFlowLayout(10@10,10@5);
		// load and save Presets files
		Button.new(wind.view,100@20).states_([["LoadPresets",Color.red, Color.green]])
		.action_({ this.loadFileDialog });
		Button.new(wind.view,100@20).states_([["SavePresets",Color.red, Color.green]])
		.action_({ this.saveFileDialog });
		q.nextLine;
		// set current preset
		curPrstGUI = NumberBox(wind.view,30@20).align_(\center)
		.action_({|num| this.loadPreset(num.value.asInt.wrap(1,presets.size)) })
		.value_(1);
		// replace Preset
		Button.new(wind.view,100@20).states_([["RplcPrst",Color.red, Color.black]])
		.action_({|butt| this.savePreset(curPresetName);
		});
		// Save new Preset
		Button.new(wind.view,100@20).states_([["SvNewPrst",Color.red, Color.black]])
		.action_({ this.getViewValsToPrst;
			this.savePreset(presets.size+1);
			curPresetName=presets.size+1 });
		q.nextLine; StaticText(wind.view,80@20).string_("  ");
		q.nextLine;
		StaticText(wind.view,80@20).string_("selTargetPrst");
		// Select targPreset
		targPrstGUI = NumberBox(wind.view,30@20).align_(\center)
		.action_({|num| targPreset = presets[num.value.min(presets.size).asInt];
			interpVal=0; interpSlider.value_(0); interpRoutine.reset }).value_(1);
		StaticText(wind.view,80@20).string_("secs to targ"); interpTime=1;
		// set time to interpolate to targPreset
		secsToTargGUI = NumberBox(wind.view,30@20)
		.align_(\center).value_(1).action_({|num| interpTime=num.value});
		q.nextLine;
		interpSlider = Slider(wind.view,200@20).action_({|sl| interpVal=sl.value;
			this.interpPresets(interpVal)
		}); interpVal=0;
		interpRoutine=Routine({
			while({interpVal < 1.0},
				{{ interpSlider.valueAction_(interpVal+0.01)}.defer;
					(0.01*interpTime).wait }) });
		goBtn = Button.new(wind.view,40@20).states_([["GO!",Color.red,Color.black]])
		.action_({if(interpVal < 1.0,{ interpRoutine.play })});
	}
}
/*
~knowNoCnsPrst.curPresetName
~knowNoCnsPrst.presets.size
~knowNoCnsPrst.presets[2][\heldNotes]
~knowNoCnsPrst.preset[\heldNotes]
~console.newPreset(0)
~console.curPreset
*/