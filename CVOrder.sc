/*

	x = CVOrder(\midi, 48, 8);
	x.value_([0,3,0,17])
	x.asCompileString

*/

CVOrder : SparseArray {
	var <spec;

	*new 	{ |spec, default, defaultSize=8|
		^super.new.spec_(spec).default_(default).defaultSize_(defaultSize);
	}
	
	action_	{ | function | ^SimpleController(this) .put(\synch, function) }
	
	value	{ ^this.asArray }

	value_ 	{|val|
		this.clear(0);
		val.asArray.do {|v,i| super.put(i,spec.constrain(v)) };
		this.changed(\synch, this, nil);
	}
	
	valueAt	{|i| ^this.at(i) }
	valuePut	{|i,val| this.put(i, val) }
	
	input_	{|in| this.value_(spec.map(in)) }
	input	{ ^spec.unmap(this.value) }
	
	inputAt	{|i|	^spec.unmap(this.at(i)) }
	inputPut	{|i,in| this.put(i,spec.map(in)) }
	
	spec_	{|s,v|
		spec = s.asSpec;
		if(spec.isNil) {
			"Spec for % not found, using none".format(s).warn;
			spec = NoSpec;
		};
		default ?? { default = spec.default };
		if(v.notNil) { this.value_(v) }
	}

	default_	{|d|
		d ?? { d = spec.default };
		default = spec.constrain(d);
		if(spec!=NoSpec) { spec.default = default };
	}
	
	sp	{ | default= 0, lo = 0, hi=0, step = 0, warp = 'lin' |
		this.spec = ControlSpec(lo,hi, warp, step, default);
	}

	db	{ | default= 0, lo = -100, hi = 20, step = 1, warp = 'lin' |
		this.spec = ControlSpec(lo,hi, warp, step, default);
	}
	
	split {
		^this.value.collect { |v| CV(spec, v) }
	}
	
	draw { |win, name =">"|
		~multicvGUI.value(win, name, this);
	}

	connect { | view |
		CVSyncMulti.new(this, view) ;
	}		
	
	add { arg obj;
			var idx = this.pos;
			array = array.add(spec.constrain(obj));
			indices = indices.add(idx);
			this.changed(\synch, this, idx);
	}
	
	at { arg index;
		if(index.isSequenceableCollection) {
			^index.collect {|i| this.at(i) }
		} {
			^super.at(index) ? default
		}
	}

	put { arg index, obj;
		this.prPutSlot(this.nextSlotFor(index), index, spec.constrain(obj));
		this.changed(\synch, this, index);
	}
	
	copyRange { arg start, end;
		^( (start..end).collect {|i| this.at(i) } )
	}
	
	copySeries { arg first, second, last;
		^( (first, second..last).collect {|i| this.at(i) } )
	}	

	asCompileString {|verbose=false|
		var specStr = if(verbose.not) { Spec.specs.findKeyForValue(this.spec) ?? { this.spec.asCompileString } } { this.spec.asCompileString };
		var str = "%.newFromIndices(%,%).spec_(%)".format(this.class.asString, this.array.asCompileString, this.indices.asCompileString, specStr);
		if(defaultSize.notNil) { str = str++(".defaultSize_(%)".format(this.defaultSize)) };
		^str
	}
	

}
