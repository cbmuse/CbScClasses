
PulseBox : Window {
	classvar <>rhythms;
	var <>name, <>numIcti, <>numVoices, <>tempo, <>rytName, <>synth, visible,
		rhythm, voices, pointer, colors, routine, routIndex;

	*new { arg argName="pulseBox", argNumIcti=64, numVoices=8, tempo=120, rytName, synth,
		visible=true;
		^super.new(argName,Rect(256,20,argNumIcti*10+40, numVoices*15+50))
			.init(argName, argNumIcti, numVoices, tempo, rytName, synth, visible)
	}

	*loadRhythms { |rhythmsFilePath|
		var r;
		var 	f = File(rhythmsFilePath,"r");
		var paths = f.readAllString.interpret;
		f.close;
		rhythms = IdentityDictionary.new;
		paths.do {|p| f = File(p,"r");
			r = f.readAllString.interpret;
			f.close;
			rhythms = rhythms.put(r.key,r.value);
		};
	}

	init { arg argName, argNumIcti, argNumVoices, argTempo,
			argRytName, argSynth, argVisible;
		var box;
		name = argName; numIcti = argNumIcti; numVoices = argNumVoices; tempo = argTempo;
		rytName = argRytName; synth = argSynth; visible = argVisible;
		box = SCNumberBox(this,Rect((numIcti-4)*10,0,30,20));
			box.setProperty(\align,\center); box.font("Arial",9); box.value_(tempo);
			box.action_({ arg item; tempo = item.value });
		box = SCButton(this,Rect(10,0,40,20));
			box.states = [["save",Color.white, Color.blue]];
			box.action = { this.saveRhythm };
		colors = [Color.red, Color.green, Color.blue, Color.yellow, Color.grey,
			Color.new255(238, 18, 137), Color.new255(102, 205, 170), Color.new255(138, 43, 226)];
		colors = colors.select({ arg item, i; i < numVoices });
		voices = colors.collect({ arg color, j;
			Array.fill(numIcti,{ arg i;
				box = SCButton(this,Rect(10+(i*10),30+(j*15),10,10))
						.states = [["",Color.black, color],["",Color.black, Color.white]];
				box.action_({ arg item; rhythm.at(j).put(i,item.value) });
			});
		});
		pointer = Array.fill(numIcti,{ arg i;
			var button;
			button = SCButton(this,Rect(10+(i*10),30+(numVoices*15),10,10))
				.states = [["",Color.black, Color.black,]];
			button.visible_(false)
		});
		rhythm = Array.fill(numVoices,{Array.fill(numIcti,0)});  // default

		rhythms !? { rytName !? { 	this.loadRhythm(rytName) }};

		if( synth.isNil,{
			SynthDef("ping",{ arg freq=200;
				Out.ar(0,EnvGen.kr(Env.perc,1.0,doneAction: 2) * SinOsc.ar(freq,0,0.1))
			}).send(Server.local);
			synth = { arg voice=0; Synth("ping",["freq",200*(voice+1)]) }
		});
		if( visible == true,{ this.front; });
		this.onClose_({ this.stop; this.free })

	}

	setSynth { arg argSynth; synth = argSynth; }

	setRhythm { arg voiceArray;		// a rhythm is a list of lists, for ex. [ [1,0,0,0,0] ]
		rhythm = voiceArray.asArray.value;
		rhythm.do({ arg list, i;
			list.do({ arg item, j; voices.at(i).at(j).valueAction_(item) })
		});
	}

	saveRhythm {
		var f, rytName;
		File.saveDialog("","",{ arg savePath;
			f = File(savePath,"w");
			rhythm = voices.collect({ arg voice;
				voice.collect({ arg item; item.value })
			});
			rytName = savePath.split.last;
			f.write(rytName.asSymbol.asCompileString ++ " -> " ++ (rhythm.value.asCompileString));
			f.close;
		})
	}

	loadRhythm {|rytName|
		this.setRhythm(rhythms[rytName])
	}

	routine { arg sigNum, index=0;
		routine = Routine({
			loop({
				numVoices.do({ arg i;
					if( rhythm.at(i).at(index).value == 1,{
						synth.value(i,index)	// send voice# and index#
					});
				});
			{ pointer.at((index-2).wrap(0,numIcti-1)).visible_(false) }.defer;
			{ pointer.at((index-1).wrap(0,numIcti-1)).visible_(true)}.defer;
				index = (index+1).wrap(0,numIcti-1);
				routIndex = index;
				1.0.wait
			})
		});
		^routine
	}

	stop {
		if( routine.notNil,{ routine.stop; });
	}

	reset {
		if( routine.notNil,{ routine.reset; });
	}

}

	