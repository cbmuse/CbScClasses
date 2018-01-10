CVProxy : Stream {
	var <source, link;

	*new { arg source;
		^super.new.source_(source)
	}
	
	source_ { arg obj;
		source = obj;
		
		// if link to old source remove it
		link !? { link.remove };
		// link to new cv to pass on any changed messages
		link = SimpleController(obj).put(\synch, { arg changer, what;
			this.changed(\synch);
		});
		
		this.changed(\synch);
		^this;
	}
	
	action_ { | function | ^SimpleController(this) .put(\synch, function) }
	
	value { ^this.source.value }
	value_ { |val| ^this.source.value_(val) }
	
	input { ^this.source.input }
	input_ { |in| ^this.source.input_(in) }

	spec { ^this.source.spec }
	spec_ { |s, v| ^this.source.spec_(s, v) }
	
	next { ^this.source.value }
	reset {}
	embedInStream { ^this.source.value.yield }
	
	draw { |win, name =">"|
		if (this.source.value.isKindOf(Array) ) {
			~multicvGUI.value(win, name, this);
		} {
			~cvGUI.value(win, name, this);
		}
	}
		
	doesNotUnderstand { arg selector ... args;
		^this.source.perform(selector, *args);
	}

	isSourceProxy { ^true }
}
/*
a = CV(\freq, 800);
b = CV(\rq, 1);
c = CVProxy.new(a);
c.dump
*/