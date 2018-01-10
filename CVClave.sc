
ClaveEnvir : EnvironmentRedirect {
	var <>defaultSize;

	*new { arg defaultSize=8;
		^super.new.defaultSize_(defaultSize);
	}

	at { arg key, index;
		var order;
		if(key.isSequenceableCollection) {
			^key.collect({|i| this.at(i, index) })
		} {
			order = super.at(key);
			if(order.isNil) {
				order = this.makeOrder(key);
				envir.put(key, order);
			};
			if(index.notNil) {
				^order[index]
			} {
				^order
			}
		}
	}

	put { arg key, obj, index;
		var order = this.at(key);
		if(index.notNil) {
			order.put(index, obj);
		} {
			order.value = obj
		}
	}

	makeOrder { arg key;
		^CVOrder(key, nil, defaultSize)
	}


	copyRange { arg start, end;
		^( (start..end).collect {|i| this.at(i) } )
	}

	copySeries { arg first, second, last;
		^( (first, second..last).collect {|i| this.at(i) } )
	}



}

/***

	x = ClaveEnvir.new(16);
	x[\freq]; // returns freq array
	x[\freq].value = 365;
	x[\freq].first; // returns only one value
	x[\freq].value = {rrand(20,2000)}!16; // fill with random array
	x[\freq, 3]; // 2D index style indexing
	x[\freq][3]; // double index style indexing
	x[\amp] = [1,0,0,1,0,0,1,0];
	x[\amp][0..3];
	x[[\freq, \amp, \rq], (0..3)].flop; // get series of events
	x[\test][5] = 323; // if spec not found, warning but still works
	x[\test].spec = [0,255,\lin,1];
	x[\test][5] = 323;
	x.keysValuesDo {|k,v,i| [k,v.first].postln }

(\amp -> "CVOrder.newFromIndices([  ],[  ]).spec_(ControlSpec(0, 1, 'amp', 0, 0.0, \"\")).defaultSize_(64)")
~ritmos = RitmosPlay.new

x=(~ritmos.presets[\pre1][\voices][\vc3][\clv].envir).asCompileString
x.replace(".proto_(Environment[  ])","")

// IN 3.7 THE RESPONSE TO ABOVE (BELOW) IS DIFFERENT THAN 3.6.5, ADDING 'proto_(Environment[])'  -- SOMETHING IS WRONG WITH HOW 'presetAsData' TREATS
A ClaveEnvir

	Environment[ ('amp' -> "CVOrder.newFromIndices([  ],[  ]).spec_(ControlSpec(0, 1, 'amp', 0, 0.0, \"\")).defaultSize_(64)")
.proto_(Environment[  ]) ]


**/
