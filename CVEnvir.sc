CVEnvir : EnvironmentRedirect {
	
	*new {
		^super.new;
	}
	
	keysValuesDo { |key, val| [key, val].postln }
	
	put { arg key, obj;

		var cv, val;
		if(key.isNil) { Error("CVEnvir:put() key is nil").throw };
		if(key.isSequenceableCollection) { ^this.putAll(key, obj.asArray) };
		
		case
		{obj.isKindOf(CV)} { ^super.put(key, obj) }
		{obj.isKindOf(Spec)} { ^super.put(key, CV(obj)) }
		{obj.isKindOf(Array)} { ^super.put(key, CV(obj[0], obj[1..].unbubble) )}
		{obj.isKindOf(Symbol)} { ^super.put(key, CV(obj)) }
		{ ^super.put(key, CV(key.asSpec ?? {[-inf, inf].asSpec} , obj) ) }
	}
	
	at { arg key;
		var res;
		if(key.isNil) { Error("CVEnvir:at() key is nil").throw };
		if(key.isSequenceableCollection) { ^this.getAll(key) };
		res = envir.at(key);
		if(res.isNil) { 
			res = CV(key.asSpec ?? {[-inf, inf].asSpec});
			envir.put(key, res); 
		};
		^res
	}
	
	putAll { arg keys, objects;
		keys.do { |key, i|
			this.put(key, objects.wrapAt(i))
		}
	}
	
	getAll { arg keys;
		^keys.collect { |key|
			this.at(key)
		}
	}
	
	putSeries { arg first, second, last, value;
		this.putAll(value.asArray, (first, second..last)) 
	}

}

/*
a = CVEnvir.new
a[\dur] = 2
a[\dur].spec
a[\freq] = 400
a[\freq2] = \freq
a[\freq2] = [\freq, 8000]

*/

