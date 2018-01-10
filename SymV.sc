/*
 A CV models a value constrained by a ControlSpec. The value can be a single Float or an array of Floats.

 Whenever the CV's value changes, it sends a changed message labeled 'synch'.  This way dependants
 (such as GUI objects or server value) can be updated with SimpleControllers.  The method
 		aCV-action_(function)
 creates such a connection.

 A CV's value can be read with the 'value' message.
 CV can also be used as a Pattern (in Pbind) or in combination with other Streams.
*/

SymV : CV {
	var <value, <spec;

	*new { | default |
		^super.new.value_(default.asSymbol);
	}

	action_ { | function | ^SimpleController(this) .put(\synch, function) }

// reading and writing the CV
	value_ { | val |
		value = val.asString;
		this.changed(\synch);
	}

	input_	{ | in | this.value_(in.asSymbol); }
	input 	{ ^value }
	asInput 	{ | val | ^(val.asSymbol) }

// setting the ControlSpec
	spec_ 	{ | s, v |
"unnecessary, symbol\n".warn;
		this.value_(v ?? {\blank});
	}
	sp	{ | default= "" |
		this.value_(\blank);
	}

	db	{ | default= "" |
		this.value_(\blank);
	}

// split turns a multi-valued CV into an array of single-valued CV's
	split {
		^value.collect { |v| SymV(v) }
	}

// Stream and Pattern support
	next { ^value }
	reset {}
	embedInStream { ^value.yield }


// ConductorGUI support
	draw { |win, name =">"|
		if (value.isKindOf(Array) ) {
			~multicvGUI.value(win, name, this);
		} {
			~cvGUI.value(win, name, this);
		}
	}

	connect { | view |
		view.class.asSymbol.postln;
	}
}

+QTextField {
	connect { arg ctl;
	var link;
		this.value_(ctl.value);
		this.action_({ctl.value_(this.value); });
		link = SimpleController(ctl)
			.put(\synch,
			 { arg changer, what;
			 	defer({
			 	this.value = ctl.value;
			 	nil
				});
			}
		);
		this.onClose = { link.remove };
	}
}

/*+SNBox2 {
	connect { arg ctl;
	var link;
		this.value_(ctl.value);
		this.action_({ctl.value_(this.value); });
		link = SimpleController(ctl)
			.put(\synch,
			 { arg changer, what;
			 	defer({
			 	this.value = ctl.value									.round(pow(10, floor(max(min(0, ctl.value.abs.log10 - 3).asInteger,-12))));
					nil
				});
			}
		);
		this.onClose = { link.remove };
	}
}*/

