/*
Holds any object and is compatible with CV
Useful for storing strings or symbols
the variable func allows one to add a processing function that evaluates every time the value is set through input:
a = OV(\hello, (_.asSymbol) );
a.value = "test"; // a.value.class == String
a.input = "test"; // becomes a symbol, using the func

*/
OV : Stream {
	var <value, <>func;

	*new { |value, func| ^super.newCopyArgs(value, func ? {|x| x}) }

	action_ { | function | ^SimpleController(this) .put(\synch, function) }

	value_ { |val| value = val; this.changed(\synch) }

	input_ { |in| this.value_(func.value(in)) }
	input { ^value }
	asInput { |val| ^val }

	spec { NotYetImplementedError("don't call spec on OV").reportError }
	spec_ { NotYetImplementedError("don't call spec on OV").reportError }

	sp { this.spec }
	db { this.spec }

	split { ^value.collect { |v| OV(v) } }
	next { ^value }
	reset {}
	embedInStream { ^value.yield }

	draw { |win, name =">"|
		~label.value(win, name);
		GUI.textField.new(win, ~smallNumericalRect.value).connect(this);
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
/*
+SNBox2 {
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
} */

