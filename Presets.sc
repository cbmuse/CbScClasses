/*
BUG
chaing gui values should update current performance envir and preset

Look at CV

TODO

add .order method to change order

library of presets

add CV

SCRangeSlider -- change default behavior
click moves range -- same behavior as current ctrl+click

TODO make sure inst view updates changes parameters correctly

figure out how to properly deal with arrays

add in optimization of saved presets

	p = Presets.new;
	p.current.putAll((foo: 4, bar: 8));
	p.savePreset;
	p.current.putAll((foo: 1, bar: 5));
	p.savePreset;
a = MultiLevelIdentityDictionary.new
a.put(\foo, 4);
a.put(\branch, \next, 4)
	p.currentPreset = 0;
	p.currentGoal = 1;
CV	
	p.template = Set[\foo, \bar];
	(
		foo: ControlSpec
		bar: (
		)
	p.interpolate(0.9)
	p.current
*/

Presets {
	var <>presets;
	var <>currentPreset;
	var <>current;
	var <>currentGoal;
	var <>path;
	var <>refState;
	var <>goalState;

	var <>updateFunc;
	
	var <>template;
	
	*new { 
		^super.new.init;
	}
	
	*read { arg path;
		^super.newCopyArgs(path).init.loadPresetsFile(path);
	}
	
	init {
		presets = List.new;
		currentPreset = 0;
		current = Environment.new;	// for safety?
		template = IdentitySet.new;
	}
	
	value { ^current }
	value_ {|val| current = val }
	
	// TODO nil check?  delete would mess this up...
	prevPreset { 
		^this.loadPreset((currentPreset-1).wrap(0,presets.lastIndex));
	}

	nextPreset { 
		^this.loadPreset((currentPreset+1).wrap(0,presets.lastIndex)) 
	}

	savePreset {
		presets = presets.add(current.deepCopy);
		currentPreset = presets.lastIndex;
	}

	replacePreset { |index| 
		currentPreset = index;
		presets = presets.put(index, current.deepCopy); 
	}
	
// TODO this is dangerous!  maybe have a way to check to be sure first?  modal dialog?
	deletePreset { |index|
		presets.removeAt(index);
	}
	
	loadPreset { |index|
		// making sure we don't corrupt or share data with deepcopy
		current = presets.at(index).deepCopy;
		currentPreset = index;
		// this is how we'll tell everything else what just happened
		updateFunc.value(current);
		this.changed(thisMethod.name);
		^current;
	}
	
	savePresetsFile {|argPath|
		var file;
		argPath !? { path = argPath };
		path ?? { path = "presets"++Date.localtime.stamp };
		
		file = File.new(path, "w");
		file.write(presets.asCompileString);
		file.close;
	}
	
	savePresetsDialog {
		File.saveDialog("save presets","",{ arg savePath;
			this.savePresetsFile(savePath)
		})
	}
	
	loadPresetsFile {|argPath|
		var file;
		File.exists(path).if( {
			path = argPath;
			file = File.new(path,"r");
			presets = file.readAllString.interpret;
			file.close;
			currentPreset = 0;
			this.loadPreset(currentPreset);
//			this.settemplate();
		},{ "file not found !! ".postln; })
	}
	
	loadPresetsDialog {
		File.openDialog("load presets",{|path|
			this.loadPresetsFile(path)
		})
	}
	
	reloadPreset {
		this.loadPreset(currentPreset);
	}
	
	resavePreset {
		this.replacePreset(currentPreset);
	}
	
	prevGoal {
		currentGoal = (currentGoal-1).wrap(0,presets.lastIndex);
		this.changed(thisMethod.name);
	}

	nextGoal {
		currentGoal = (currentGoal+1).wrap(0,presets.lastIndex);
		this.changed(thisMethod.name);
	}

// 	NOTE this assumes linear interpolation
//	TODO is preset interpolation on saved states or including current?
	interpolate { arg index;
// TODO this should probably be in loadPreset or elsewhere?...
		refState ?? { refState = presets.at(currentPreset) };
		goalState ?? { goalState = presets.at(currentGoal) };
// NOTE this wouldn't work the way we've setup the ritmos preset...would need multilevelID?
//		MultiLevelIdentityDictionary
		template.do { |key|
			current.put(key, refState.at(key).blend(goalState.at(key), index) );
		};
		
		this.updateFunc.value;
		this.changed(thisMethod.name);
		^current;
	}

//	blend { arg another, frac;
//		var res = this.copy;
//		another.updateBundle;
//		another.setArgs.pairsDo { |key, x| 
//			var s = settings[key], y;
//			s !? { y = s.getValue };
//			y !? { res.set(key, blend(y, x, frac)) }
//		};
//		^res
//	}
	
	
//	copy {
//		var res, nset;
//		res = this.class.new;
//		nset = res.settings; 
//		settings.keysValuesDo({ arg key, val; nset.put(key, val.copy) });
//		^res
//	}
}