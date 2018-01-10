
KbdVoicers	{
	classvar <>synthDefs, <>voicers, <>guiControls, <>voicersData,
		<>window, <>keyboard, <>curChan=1, <>noteChanGuiResp, storeGui,
		<nameGui, <chanGui, <vcsGui, <rootGui, <freqGui, <tuningGui,
		<knobs, <labels, <>effBusNum=16,<>recBusNum=24;

	*init {
		curChan=1;
		voicers = Array.fill(16,{nil});
		voicersData = Array.fill(16,{ (params: ( ) )});
		VoicerSynthDefs.init;
		synthDefs = VoicerSynthDefs.synthDefs;
		Spec.specs.add(\effBus -> [effBusNum,effBusNum+7.99,\lin,1,effBusNum].asSpec);
		Spec.specs.add(\recBus -> [recBusNum,recBusNum+7.99,\lin,1,recBusNum].asSpec);
		Spec.specs.add(\fdiff->[0.001,0.1,\exp,0,0.0025].asSpec);

		guiControls = (
			\bassK:  [\amp,\mod,\lofreq,\ffreq,\rq,\pan],
			\chimeK: [\amp,\mod, \lofreq, \lrtio, \start, \ring],
			\fatSawK: [\amp,\mod,\ffreq,\rq,\fdiff,\pan],
			\formantK: [\amp,\ffreq, \bw, \lofreq,\mod,\chor,\pan],
			\greenOrgK: [\amp,\gain,\spd,\pan],
			\minimoog2K: [\amp,\int1,\int2,\width1,\ffrqInt,\rq],
			\pianoK: [\amp,\ffreq, \lofreq, \mod, \rq, \pan],
			\pluckK: [\amp,\plDcy,\coef,\pan],
			\pmDcyK: [\amp,\lofreq,\mod, \ratio,\idx,\pan],
			\pmK: [\amp,\lrtio, \idx,\ffrqInt,\chor,\rq,\pan],
			\silentK: [ ],
			\sineK: [\amp,\mod,\lofreq,\chor,\pan],
			\snareK: [\amp,\lofreq, \mod, \noiseAmp,\dcyt,\pan],
			\stringK: [\amp,\ffrqInt,\spd,\rq,\chor,\pan],
			\tunSusK: [\amp,\phs,\wdth,\pan]
		);
		knobs = []; labels = [];
	}

	*gui {
		var loadBtn, saveBtn;
		window = GUI.window.new("",Rect(295, 1, 403, 241)).front;
		window.onClose_({
			voicers.do {|v,i| if(v.notNil,{ v.free }) };
			if(noteChanGuiResp.notNil,{ noteChanGuiResp.remove; noteChanGuiResp=nil });
			window.free
		});

		keyboard = MIDIKeyboard.new(window, Rect(10,0,364,60), 5, 36);
		keyboard.keyDownAction_({|note|
			var chan = curChan-1;
				var num = (note - keyboard.keys[0].note);
				if(keyboard.keys[num].inscale.not,{
					voicers[chan].sockets[0].noteOn(note,127);
					keyboard.keys[num].inscale_(true);
					keyboard.setColor(note,Color.red)
				},{
					voicers[chan].sockets[0].noteOff(note,127);
					keyboard.keys[num].inscale_(false);
					keyboard.removeColor(note)
		})});

		storeGui = Button.new(window,Rect(310, 196, 60, 20))
			.states_([ ["New", Color.black, Color.red] ])
		.action_{
			var v;
			var tuning = tuningGui.items[tuningGui.value];
			var chan = chanGui.value;
			voicers[chanGui.value].free;
			voicers.put(chan,
				v = KbdChanVoicer.new(nameGui.items.sort[nameGui.value],
					vcsGui.items[vcsGui.value].interpret,
					chanGui.value+1,tuning,freqGui.value,rootGui.value+60));
			voicersData[chan].put(\synth,nameGui.value);
			voicersData[chan].put(\voices,vcsGui.value);
			voicersData[chan].put(\root,rootGui.value);
			voicersData[chan].put(\freq,freqGui.value);
			voicersData[chan].put(\tuning,tuningGui.value);
			v.name.post; " loaded on channel ".post; (chanGui.value+1).postln;
			KbdVoicers.guiRefresh;
			// set default voicersData params
			v.ccs.do {|cc| voicersData[chan][\params].put(cc.name, cc.destValue ) };
			KbdVoicers.makeKnobs(v.name);
			"".postln;
		};

		GUI.staticText.new(window,Rect(45, 120, 36, 21))
			.string_("chan");
		chanGui = GUI.popUpMenu.new(window,Rect(45, 146, 36, 21))
			.items_([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16].collect {|n| n.asString})
			.action_{|v|
				var chan = curChan-1;
			// save all data for previous voicer to voicersData
				if(voicers[chan].notNil,{
					voicers[chan].sockets[0].ccs.do {|cc|
					voicersData[chan][\params]
								.put(cc.name, cc.destination.value) };
				});
				chan = curChan = v.value.asInt+1;
				if (voicers[v.value.asInt].notNil,{
					KbdVoicers.makeKnobs(voicers[curChan-1].name);
					KbdVoicers.guiRefresh;
					KbdVoicers.setCCsFromData(chan-1);
				},{ // if this voicer is empty, then create new KbdChanVoicer
					"channel undefined, create NEW ".postln });
			}
			.value_(0);

		GUI.staticText.new(window,Rect(97, 120, 72, 21))
			.string_("synthName");
		nameGui = GUI.popUpMenu.new(window,Rect(97, 146, 182, 21))
			.items_(KbdVoicers.synthDefs.collect {|def| def.name.asSymbol }.sort)
			.action_{|v| };
		nameGui.value_(nameGui.items.indexOf(\pianoK));

		GUI.staticText.new(window,Rect(310, 120, 36, 21))
			.string_("#vcs");
		vcsGui = GUI.popUpMenu.new(window,Rect(310, 146, 48, 21))
			.items_([4,8,12,16,24,32].collect {|n| n.asString})
			.action_{|v| }
			.value_(1);

		GUI.staticText.new(window,Rect(45, 170, 36, 21))
			.string_("root");
		rootGui = GUI.popUpMenu.new(window,Rect(45, 196, 36, 21))
			.items_([\C,\Db,\D,\Eb,\E,\F,\Gb,\G,\Ab,\A,\Bb,\B])
			.action_{|v| }
			.value_(9);
		GUI.staticText.new(window,Rect(97, 170, 36, 21)).string_("refFrq");
		freqGui = GUI.numberBox.new(window,Rect(97,196,40,21))
			.value_(440.0)
			.align_(\center);
		GUI.staticText.new(window,Rect(145, 170, 36, 21))
			.string_("tuning");
		tuningGui = GUI.popUpMenu.new(window,Rect(145, 196, 135, 21))
			.items_((KeyboardTunings.tunings.keys.asArray++\EqTemp12).sort)
			.action_{|v| if(voicers[curChan-1].notNil,{
				voicers[curChan-1].setTuning_(
					tuningGui.items[v.value],freqGui.value,rootGui.value);
				voicersData[curChan-1].put(\tuning,v.value)}
			)}
			.value_(0);

		loadBtn = Button.new(window,Rect(310,70, 60, 20))
			.states_([ [ "Load", Color(0.0, 0.0, 0.0, 1.0), Color.blue] ])
			.action_{
				File.openDialog("load presets",{|path|
					KbdVoicers.loadVoicersPresets(path)
				})
			};

		saveBtn = Button.new(window,Rect(310,100, 60, 20))
			.states_([ [ "Save", Color(0.0, 0.0, 0.0, 1.0), Color.green] ])
			.action_{
				File.saveDialog("save MIDIkeys Voicers","",{ arg savePath;
					KbdVoicers.saveVoicersPresets(savePath)
				})
			};

		 KbdVoicers.newChanVoicer(1);		// create a voicer on chan 1
		 KbdVoicers.noteChanGui_(true)		// default note channel selects instrument
	}

	*newChanVoicer {|chan| var v, tuning;
		chan = chan-1;
		tuning = tuningGui.items[tuningGui.value];
		v = KbdChanVoicer.new(nameGui.items.sort[nameGui.value],
			vcsGui.items[vcsGui.value].interpret,
			chan+1,tuning,freqGui.value, rootGui.value+60);
		voicers.put(chan,v.postln);
		voicersData[chan].put(\synth,nameGui.value);
		voicersData[chan].put(\voices,vcsGui.value);
		voicersData[chan].put(\root,rootGui.value);
		voicersData[chan].put(\freq,freqGui.value);
		voicersData[chan].put(\tuning,tuningGui.value);
		// set default voicerData
		v.ccs.do {|cc| voicersData[0][\params].put(cc.name, cc.destValue ) };
		v.name.post; " loaded on channel ".post; (chan+1).postln; "".postln;
		KbdVoicers.guiRefresh;
		KbdVoicers.makeKnobs(v.name);


	}

	*makeKnobs {|name|
		var v = voicers[curChan-1];
		var data = KbdVoicers.voicersData[curChan-1];
		knobs.do {|knob| knob.remove }; knobs = [];
		labels.do {|label| label.remove }; labels = [];
		guiControls[name].do {|n,i|
			knobs = knobs.add(Knob.new(window,Rect(i*35+60,70,25,25))
				.action_({|knob|
					v.sockets[0].ccs[7+i].set(knob.value,1);
					data[\params].put(n,n.asSpec.map(knob.value))
				}
			));
			knobs[i].value_(n.asSpec.unmap(data[\params][n]));
			labels = labels.add(StaticText.new(window,Rect(i*35+60,95,30,16))
						.string = n.asString);
		};
		window.refresh;
	}

	// set cc values from voicerData
	*setCCsFromData {|chan|
		voicers[chan].sockets[0].ccs.do {|cc|
			if(voicersData[chan][\params][cc.name].notNil,{
				if(cc.spec.notNil,{ cc.set(cc.spec.unmap(voicersData[chan][\params][cc.name]),1)
				},{ cc.set(voicersData[chan][\params][cc.name],1) })})
		}
	}

	*guiRefresh	{	//	set gui items (except knobs) from voicersData
		var data = voicersData[curChan-1];
		var name = nameGui.items[data[\synth]];
		var ctlNames = guiControls[name];
		nameGui.value_(data[\synth]);
		vcsGui.value_(data[\voices]);
		rootGui.value_(data[\root]);
		freqGui.value_(data[\freq]);
		tuningGui.value_(data[\tuning]);
	}

	*loadVoicersPresets	{|path|
		var file,v,data;
		File.exists(path).if({
			voicers.do {|v| v.free };
			voicers = Array.fill(16,{nil});
			voicersData = Array.fill(16,{ ( params: ( ) )});
			file = File.new(path,"r");
			data = file.readAllString.interpret;
			data.do {|vc|		// convert text into perform data format
				if(vc[\params].notEmpty,{
					vc.put(\tuning,(KeyboardTunings.tunings.keys.asArray++\EqTemp12)
							.sort.indexOf(vc[\tuning]));
					vc.put(\synth,(synthDefs.collect {|def| def.name.asSymbol })
						.sort.indexOf(vc[\synth]));
					vc.put(\root,[\C,\Db,\D,\Eb,\E,\F,\Gb,\G,\Ab,\A,\Bb,\B]
							.indexOf(vc[\root]))})
			};
			voicersData = data.deepCopy;
			voicersData.do {|data,i|
				if(data[\params].notEmpty,{
					data = voicersData[i];
					if(voicers[i].notNil,{ voicers[i].free });  // remove any prev voicer
					voicers.put(i,
						v = KbdChanVoicer.new(  // create new voicer
							nameGui.items[data[\synth]],
							vcsGui.items[data[\voices]].interpret,
						 	i+1,tuningGui.items[data[\tuning]], data[\freq],
						 	(data[\root])+60));

			})};
			curChan = 1; chanGui.value_(0);
			KbdVoicers.makeKnobs(voicers[curChan-1].name);
			KbdVoicers.guiRefresh;
			KbdVoicers.setCCsFromData(curChan-1);
		},{ "file not found !!".postln })
	}

	*saveVoicersPresets	{|path|
		var file, data;
		path ?? {path = "voicers"++Date.localtime.stamp};
		file = File.new(path,"w");
		data = voicersData.deepCopy;
		"curData =  ".postln;
	//	data.postln;
		data.do {|vc,i|
			// remove data that no longer applies to current synth
			("voice" ++ (i.asString) ++ " = ").post; vc.postln;
			if( KbdVoicers.voicers[i].notNil,{
				vc[\params].keys.asArray.difference
				(KbdVoicers.voicers[i].sockets[0].ccs.collect {|c| c.name })
					.do {|name| vc[\params].removeAt(name) }
			});
			if(vc[\params].notEmpty,{
				vc.put(\tuning,(KeyboardTunings.tunings.keys.asArray++\EqTemp12)
					.sort[vc[\tuning]]);
				vc.put(\synth,(KbdVoicers.synthDefs.collect {|def|
					def.name.asSymbol }).sort[vc[\synth]]);
				vc.put(\root,[\C,\Db,\D,\Eb,\E,\F,\Gb,\G,\Ab,\A,\Bb,\B][vc[\root]])
			});
		};
		"writeData =  ".postln; data.postln;
		file.write(data.asCompileString);
		file.close
	}

	*noteChanGui_ {|flg|
		if(flg,{
			if(noteChanGuiResp.isNil,{
				noteChanGuiResp = NoteOnResponder({|src,chan,num,vel|
					if(KbdVoicers.voicers[chan].notNil,{
					{ KbdVoicers.chanGui.valueAction_(chan) }.defer;
					})})
			});
		},{ if(noteChanGuiResp.notNil,{noteChanGuiResp.remove; noteChanGuiResp=nil}) })
	}
}

KbdChanVoicer	{
	var <voicer, <>name, <>tuning, <>sockets, <>chan, <>proxy, <ccs, updateFunc;

	*new	{| synthName, numVoices, chan, tuning, freq, root |
		^super.new.init(synthName, numVoices, chan, tuning, freq, root)
		}

	init { | argName, argNumVoices, argChan, argTuning, argFreq=440, argRoot=69|
		voicer = Voicer.new(argNumVoices,argName, addAction:\addToHead);
		name = argName;
		chan = argChan;
		proxy = voicer.proxify;
		if(MIDIClient.sources == nil,{
			MIDIClient.init;
			MIDIClient.sources.do({|src,i| MIDIIn.connect(i,src) })
		});
		// create a socket for each MIDI source, merging inputs on the same channel
		this.sockets = MIDIClient.sources.collect {|port,i|
			VoicerMIDISocket.new(MIDIChannelIndex(port,argChan-1),proxy);
		};
		if(this.sockets.isEmpty,{
			this.sockets = [ VoicerMIDISocket.new(argChan-1,proxy) ];
		});

		this.setTuning_(argTuning,argFreq, argRoot);
		this.addDefControls;		// create all ccs
		ccs = this.sockets[0].ccs;
		updateFunc = 	{|what,valEvent| // changer,param,val
			var thisKnobIndex,pval,val,param;
			val = valEvent[\val]; param = valEvent[\param];
			//	param.post; " ".post; val.postln;
			if(param.notNil,{
				KbdVoicers.voicersData[chan-1][\params].put(param,val);
				// if this is voicer is currently displayed and param has a knob gui, update it
				if((KbdVoicers.curChan == chan) && (KbdVoicers.guiControls[name]
					.includes(param)),{
						thisKnobIndex = KbdVoicers.guiControls[name].detectIndex
						{|knobname| knobname == param };
						{ KbdVoicers.knobs[thisKnobIndex].value_(param.asSpec.unmap(val)) }.defer });
			});
		};
		this.addCcUpdaters;
		MIDIClient.sources.do {|port,i| VoicerSusPedal.new(MIDIChannelIndex(port,argChan-1),64,proxy) };
		^this
	}

	addControl	{|num,name,argSpec,initVal|
		var spec = if(argSpec.isNil,{ name.asSymbol.asSpec },{ argSpec }) ;
		if(initVal.isNil,{if(spec.notNil,{ initVal = spec.default },{ initVal = 0})});
		this.sockets.do {|sock| sock.addControl(num,name,initVal,spec) }
	}

	addDefControls	{
		this.addControl(\pb, \pb, 1, 3);	// pitchbend
		this.addControl(1,\effAmp,initVal: 0);  // mod controller = effect send level
		this.addControl(0,\effBus); // effect bus select
		this.addControl(2,\att,initVal: 0.1);
		this.addControl(3,\decay,initVal: 0.2);
		this.addControl(4,\rls,initVal: 0.1);
		this.addControl(5,\susLev,initVal: 0.5);
		this.addControl(7,\amp,initVal: 0.5);
		KbdVoicers.guiControls[name].do {|param,i|
			switch(i,
				1,{ this.addControl(8,param) },
				2,{ this.addControl(9,param) },
				3,{ this.addControl(10,param) },
				4,{ this.addControl(14,param) },
				5,{ this.addControl(15,param) },
				6,{ this.addControl(16,param) });
		};
		this.addControl(6,\recBus);

	//	this.addControl(\touch,\mod);		// aftertouch
	}

	removeControl	{|num|
		this.sockets.do {|sock| sock.removeControl(num) }
	}

	addCcUpdaters	{
		ccs.do {|cc,i| cc.destination.addDependant(updateFunc)}
	}

	setNumEffects {|num|
		this.removeControl(\effBus);
		this.voicer.unmapGlobal(\effBus);
		// add new effBus controller to end of ccs array
		this.addControl(0,\effBus);
		// reinsert effBus controller as 3rd of ccs array
		this.sockets.do {|sock| var eCC = sock.ccs.pop; sock.ccs.insert(2,eCC) };
	}

	setTuning_ {|name, freq, rt|
		var cRoot;
		var ratios = KeyboardTunings.tunings[name];
		// change calibratefreq to equal 12Tet pitchclass, so freq arg sets 'A', not root
		var newTemp = EqualTemperament.new(calibratefreq: freq, calibratenote: 69);
		this.tuning = name;
		freq = newTemp.cps(rt);
		if( ratios.notNil,{
			cRoot = rt-60;
			if(((cRoot != 0)), {
				cRoot = ratios.size - cRoot;
				ratios = ratios[cRoot..] ++ (ratios[..cRoot-1] * 2);
				ratios = ratios / ratios[0];
			});
			// this fails if there is no MIDI driver !!
			this.sockets.do {|sock|
				sock.midiToFreq = TuningRatios(ratios.size, freq, rt, ratios) }
		},{
			//	EqualTemperament ignores 'rt' arg, uses 'A'
			this.sockets.do {|sock| sock.midiToFreq = newTemp  }
		});
		"KbdVoicer tuning = ".post; name.postln;
		^this.sockets[0].midiToFreq
	}


	free {
		this.voicer.free;
		KbdVoicers.voicers.put(chan-1,nil);
		ccs.do {|cc| cc.removeDependant(updateFunc) };
		MIDIPort.removeAt(chan-1)
	}

}

/*

(
s.isRunning
Server.all
VoicerSynthDefs.effbus
KbdVoicers.init.gui;
v = KbdVoicers.voicers[0].sockets[0].destination.panic
Spec.specs[\effBus]
v.sockets[1].noteOn(60,100)
v.sockets[1].destination.panic
v = KbdVoicers.voicers[0].sockets[0].noteOff(60,100)
KbdVoicers.voicers[0].sockets[0].ccs.do {|cc| cc.destination.value.postln }
KbdVoicers.voicersData[0]
v.sockets[0].ccs.do {|cc| cc.set(KbdVoicers.voicersData[1][\params][cc.destination.name],1)};
KbdVoicers.voicers[1].sockets[0].ccs.do {| cc| cc.spec.postln }
r = Routine { loop({ KbdVoicers.knobsRefresh(\formantK) ; 0.33.wait }) }.play(AppClock)
r.stop;
v = KbdVoicers.new("bass",32,1);
v.setTuning_(\lim13,9);
ConsoleSynthDefs
)

KbdVoicers.voicers[0].voicer.releaseAll
v.voicer.panic

v.socket.removeControl(20);

v.free
	var azArray = [0.5pi,-0.5pi]; var elArray = [0,0] ; var effbus=16;


KbdVoicerSynthDefs
	v = KbdVoicers.voicers[0].voicerData
v.sockets[0].ccs.do {|c| c.spec.unmap(KbdVoicers.voicersData[0][\params][c.name]).postln }
Spec.specs[\effBus]
*/

	