CVPreset2 {

	var <>items;			// items to preset
	var <>keys;			// names of items
	var <presets;			// dict of input values corresponding to different presets
	var <presetCV;		// CV that controls preset selection

	
	*new { ^super.new.init }
	
	init { 
		var preset;
		preset = this;
		presetCV = CV.new.sp(0,0,0,1);
		presetCV.action_({ |cv| preset.set(cv.value)});
	}

	value  {  ^[[], presets] }
	value_ { | v| this.presets = v[1]; }  
	
	input_ { | kvs | 
		kvs.do { |kv|	
			items.at(kv[0]).input_(kv[1])
		} 
	}

	input {
		^items.collect({|cv, key| [key, cv.input] }).asArray
	}

//	input_ { | input | presetCV.input_(input) }
//	input { ^presetCV.input }
		

	presets_ { | argPresets|
		presets = argPresets;
		presetCV.spec.maxval = presets.size;
	}
	
	add { 	
		this.presets = presets.add(this.input); 
		presetCV.spec.maxval = presets.size;
	}
	
	remove { arg index;
		if (presets.notNil, {
			presets.removeAt(index);
			presetCV.spec.maxval = presets.size;
		});
	}

	set { arg index;
		var preset;
		preset = presets[index];
		preset.do { |kv|
			items.at(kv[0]).input_(kv[1])
		}
//		items.do { | p, i | p.input_(preset[i]) }
	}
	
	draw { |win, name, preset|
		~presetGUI.value(win, name, this)
	}
	
}
/*
+ Conductor {
	presetKeys_ {| keys, argPreset |
		argPreset = argPreset ? preset;
		preset.items = IdentityDictionary.new;
		keys.do {|k| preset.items.put(k, this[k]) };
	}
	
	usePresets {
		this[\settings] = ConductorSettingsGUI(this);
		this[\preset] = preset = CVPreset2.new;
		this.presetKeys_(valueKeys);
	}
}
*/