PEnvMeter : AAudioMeter {
	var <>amprespfunc, <>pitchrespfunc, <pitch, <pitchval,<curNote,<>evThreshold,
	<>offThreshFactor,<threshCtls,<onFlg=false, <>evOnFunction,<>evOffFunction,lastAttackTime=0,
	<>debounceTime=0.0825;


	*new { arg index, target, addAction, point, label;
		^super.new(index, target, addAction, point, label);
	}

	init { arg argindex, argtarget, argaddAction, argpoint, arglabel;
		var evOnShow;
		active=false;

		target = argtarget ? Server.default;
		target = target.asTarget;
		server = target.server;
		if(server.serverRunning.not) {
			("server '" ++ server.name ++ "' not running.\n  unable to make an AudioMeter!").warn; ^nil };
		addAction = argaddAction ? \addAfter;

		index = argindex ? [0, 1] ;  // Default bus = Stereo Output;
		if (index.isArray.not) {index = [index]};
		size = index.size;

		if(argpoint.notNil)
			{ rect = Rect(argpoint.x, argpoint.y, size * 120 + 25, 190); }
			{ rect = Rect((idCount * 150) + 330, 10, size * 120 + 25, 190); };
		label = arglabel ? "";

		"opening Pitch Env Meter window ".postln;
		w = Window(label, rect, resizable: false);
		w.front;
		w.alwaysOnTop = alwaysOnTop;
		w.view.background = Color.clear;
		w.alpha = 1.0;
		w.onClose = { this.free; };
		w.view.keyDownAction = { arg view, char; this.keyDown(char) };

		meter = Array.new(size);
		clip = Array.new(size);
		peak = Array.new(size);
		peakval = Array.new(size);
		oscresp = Array.newClear(size*2);
		amprespfunc = Array.new(size);
		pitch = Array.new(size);
		pitchval = Array.new(size);
		curNote = Array.new(size);
		pitchrespfunc = Array.new(size);
		evThreshold = Array.new(size);
		offThreshFactor = Array.new(size);
		threshCtls = Array.new(size);

		dbrange = dbmax - dbmin;

		this.activate(true);
//		this.autoactivate.value;

		evOnFunction = {|p,amp,i|
			" pitch on = ".post; p.post;
			" amp = ".post; amp.round(0.001).postln;
		};

		evOnShow = {|p,i|
			{ pitch[i].background_(Color.red); pitch[i].normalColor = Color.black; pitch[i].value = p; }.defer;
			AppClock.sched(0.5,
				{ pitch[i].background_(Color.black); pitch[i].normalColor = Color.green;
				pitch[i].value = p; });
		};

		evOffFunction = {|pitch|
			"pitch off = ".post; pitch.post; "   ".postln;
		};

		amprespfunc = { arg i, x;
			meter[i].value = (x.ampdb - dbmin) / dbrange;
			if( meter[i].value > peakval[i]) {
				peakval[i] = meter[i].value;
				peak[i].value = x.ampdb.round(0.0001);
				};
			if(x >= 1.0) {clip[i].value = 1 };
	// is there a way to store event, but check its midi value later, to enable correct debouncing of pitch follower?
			if((onFlg.not && (x > evThreshold[i])),{
				onFlg=true;
				curNote[i] = pitchval[i].cpsmidi.round;
				"new note at time ".post; lastAttackTime = thisThread.seconds.round(0.001).postln;
				evOnFunction.value(curNote[i],x,i);
				evOnShow.value(curNote[i],i);
			},{ if(onFlg,{ // if previous note has not completed
					if ((x <= (evThreshold[i]*offThreshFactor[i])),{ // turn noteOff if amp < threshold
						onFlg=false;
						evOffFunction.value(curNote[i])
					},{			// or if there is a new note and debounceTime is passed
						if( (curNote[i] != pitchval[i].cpsmidi.round) &&
							 	(thisThread.seconds > (lastAttackTime+debounceTime) ),{
							 evOffFunction.value(curNote[i]);
							"new note after  ".post;
							(thisThread.seconds - lastAttackTime).round(0.001).post; " seconds".postln;
							curNote[i] = pitchval[i].cpsmidi.round;
							evOnFunction.value(curNote[i],x,i);
							evOnShow.value(curNote[i],i);
						})
					})
				})
			});
		};

		pitchrespfunc = { arg i,x;
			pitchval[i] = x.round(0.001);
		};

		resetfunc = { arg i;
			clip[i].value = 0;
		//	meter[i].hi = 1.0;
			peakval[i] = 0.0;
//			peak[i].value = -90.0;
		};

		schedfunc = {
			if (autoreset > 0)
				{ AppClock.sched(autoreset,
					{ size.do({arg i; resetfunc.(i)  });
					  schedfunc.value;
					  nil
					}
				)};
		};

		index.do( { arg ix, i;

			StaticText(w, Rect( (i*130) + 10, 5, 25, 15))
				.string_(ix.asString).stringColor_(Color.black).align_(\center);

			clip.add(Button(w, Rect( (i*130) + 10, 25, 10, 10)));
			clip[i].canFocus = false;
			//clip[i].font = Font("Arial",9);
			clip[i].states = [ [" ", Color.black, Color.grey(0.5)] ,
						     [" ", Color.black, Color.red] ];
			clip[i].action = { arg view;
				resetfunc.(i);
				};

			meter.add(AMeter(w, Rect( (i*130) + 10, 80, 120, 80)));
		//	meter[i].knobColor = Color.black;
		//	meter[i].background = Gradient(Color.yellow, Color(0, 0.8, 0.2), \v);
		//	meter[i].canFocus = false;
		//	meter[i].hi = 1.0;
		//	meter[i].lo = 0.0;

			peakval.add(0.0);

			peak.add(NumberBox(w, Rect( (i*130) + 10, 155, 40, 15)));
			peak[i].font = Font("Arial",11);
			peak[i].value = -90.0;
			peak[i].normalColor = Color.green;
			peak[i].background = Color.black;

			pitchval.add(0.0);
			pitch.add(NumberBox(w,Rect((i*130)+60,155,40,15)));
			pitch[i].font = Font("Arial",11);
			pitch[i].value = 0;
			pitch[i].normalColor = Color.green;
			pitch[i].background = Color.black;
			curNote.add(60.0);
			this.addresponder(i);

			evThreshold.add(0.05);
			offThreshFactor.add(0.125);
			w.view.decorator = FlowLayout(w.view.bounds).shift(35);
			threshCtls.add([EZKnob.new(w,32 @ 16,"onTh",[0,0.25].asSpec,
						{|v| evThreshold[i]=v.value },0.05),
					EZKnob.new(w,32 @ 16,"offTh",[0,0.5].asSpec,
						{|v| offThreshFactor[i]=v.value },0.125)]);

			resetfunc.(i);

			});

		//schedfunc.value;
		idCount = idCount + size;
	}

	addresponder { arg i;
		var commandpath;

		oscresp[i*2].remove;
		commandpath = ['/tr', synth.nodeID, id + idCount + i];
		oscresp[i*2].add( OSCpathResponder(server.addr, commandpath, { arg time,responder,msg;
					{ amprespfunc.(i, msg[3]) }.defer
				}).add );
		oscresp[i*2+1].remove;
		commandpath = ['/tr', synth.nodeID, (id*2 + idCount + i)];
		oscresp[i*2+1].add( OSCpathResponder(server.addr, commandpath, { arg time,responder,msg;
					{ pitchrespfunc.(i,msg[3]) }.defer
				}).add );
	}

	run {
		if(synth.isPlaying.not) {
			//index.dump;
			synth = SynthDef(label ++ (id + idCount), {arg decay=0.99994, rate=15;
				var peak, freq=60, hasFreq=0, t;
				peak = PeakFollower.ar(index.collect({ arg ix; In.ar(ix, 1)}), decay);
				t = Impulse.ar(rate);
				SendTrig.ar(t, Array.series(size, id + idCount) , peak);

				#freq, hasFreq = Tartini.kr(index.collect({ arg ix; In.ar(ix, 1)}));
				t = Impulse.kr(rate);
				SendTrig.kr(t, Array.series(size, (id*2 + idCount)) , freq[0]);
			}).play(target, [\decay, this.decayrate(decay), \rate, rate], addAction);

			synth.isPlaying = true;
			NodeWatcher.register(synth);
		}
	}

	free {
		amprespfunc = nil;
		pitchrespfunc = nil;
		resetfunc = nil;
		if(synth.isPlaying) {  synth.free };
		synth = nil;
		oscresp.do({arg item, i; item.remove});
		CmdPeriod.remove(this);
		idCount = idCount - size;
	}
}


