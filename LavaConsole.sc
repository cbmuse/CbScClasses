LavaConsole { 
	var sampleFile, presetFile, presetRoutineFile, midiInFuncs, 
	<>presets, <>presetRoutines, <prRout, <>curPreset=0, 
	<>numVoices=8, <>numParams=16, <>numSignals=4, <>numEffects=4, numBuffers=4, 	numFFTBuffers=2, <>recBuffers, <bufLength=4, fftBuffers, recBufAlloc, fftBufAlloc,
	<>window, <genProcGroup, <>signals, <>curSignalNum=0, <>curSignals,<curSigSynths,
	<>sigCtlBus, <>sigCtlBusValues,
	<> curVoiceNum=0, keyMap, srcMap, effMap, vcMap, numKeyMap, <>keyQueues, <>sampBufs,	<>loopOn, <>loopDur, susNotes, 	<> gateBus, <> inBus, <>volBus,
	<>effects, <>curEffNum=0, <>curEffects, <prevEffect, 
	<>effCtlBus, <>effCtlBusValues, <>effToggle,<>effSend,<>effMixBus,
	<>effRoutinesToggle, <>effRoutNames,
	<>effRoutines, <>effRoutTempi, <>effRoutWindows, 
	<>sigRoutinesToggle, <>sigRoutNames, 	<>sigRoutines, <>sigRoutTempi,<>sigRoutWindows, 
	<>soundfiles, <sampRate=44100, <>keyTrgFunc, <s, audioBusOffset, rlsTime=0.25;

	*new { arg presetFile, presetRoutineFile, sampleFile,  midiInFuncs; 
		^super.newCopyArgs(sampleFile, presetFile, presetRoutineFile, midiInFuncs).initConsole
	}

	initConsole	{ 
		var file, name;
		s = Server.local; s.latency = 0.03;
		if( s.sampleRate.notNil,{ sampRate = s.sampleRate; });
		audioBusOffset = s.options.numOutputBusChannels + s.options.numInputBusChannels;
		genProcGroup = Group.new;
		this.initMaps;
		this.initKeyTrgFunc;
		MIDIClient.init(2,1); 2.do({ arg i; MIDIIn.connect(i,MIDIClient.sources[i]) });
		ConsoleRoutine.initRoutineLib;
		SynthDescLib.global.read;		// read in all SynthDescs
		// load samples from file
		name = sampleFile;
		"".postln;
		if( name.notNil,{ 		
			if( File.exists(name),{
				file = File(name,"r");
				soundfiles = file.readAllString.interpret; file.close;
			},{ "can't find samplefile !!".postln; 
			soundfiles = ["sounds/a11wlk01.wav", "sounds/a11wlk01.wav"] });
		},{ "default soundfile loaded".postln; 
			soundfiles = ["sounds/a11wlk01.wav", "sounds/a11wlk01.wav"]});
		~soundfiles = soundfiles;		// use env variable to pass ref to soundfiles to Lib
		soundfiles.do({ arg filename,i;
			var buf, file;
			buf = Buffer.read(s,filename);
			sampBufs = sampBufs.add(buf);
		});
		Spec.specs.add(\sampBuf->ControlSpec(1,soundfiles.size,step:1));
		Spec.specs.add(\recBuf->ControlSpec(1,numBuffers,step:1));
		Spec.specs.add(\fftBuf->ControlSpec(1,numFFTBuffers,step:1));
		// load Presets from file
		name = presetFile;
		if( name.notNil,{ 
			if(File.exists(name),{
				file = File(name,"r");
				this.loadPresetFile(file); file.close;
			});
			this.initBusses; 
			numSignals.do({ 			// these aren't yet running, so must be nil
				sigRoutines = sigRoutines.add(nil);
				sigRoutWindows = sigRoutWindows.add("nil");
			});
			numEffects.do({ 
				effRoutines = effRoutines.add(nil); 
				effRoutWindows = effRoutWindows.add("nil");
			});
			this.initBuffers;
			Spec.specs.add(\recBuf->ControlSpec(1,soundfiles.size,step:1));
			~buffers = recBuffers;
			this.restorePresetValues(0);
			window = ConsoleWindow.new(this);
			// load presetRoutines from file
			name = presetRoutineFile;
			if( name.notNil,{
				if( File.exists(name),{
					file = File(name,"r");
					presetRoutines = file.readAllString.interpret; 
					file.close;
				},{ "preset Routines file doesn't exist !!".postln });
			});
			if( presets.size != presetRoutines.size,{ "wrong number of PresetRoutines".postln });
			this.newPreset(0)
		},{ 
			this.initParamNums;  
		});
		if( midiInFuncs.notNil,{ "Init MidiFuncs".postln; this.midiInit(midiInFuncs) });
	}
	
	initParamNums {
		var w, b, l, d;
		w = SCWindow("Input Console-Params",Rect(20, 425, 240, 300)).front;
		l = ["numParams", "numVoices","numSignals","numEffects", "numRecBuffers", 				"recBufSecs", "numFFTBuffers"];
		d = [16,8,4,4,4,4,2];
		7.do({ arg i;
			b = SCNumberBox(w,Rect(20,30*(i+1),40,20));
			SCStaticText(w,Rect(70,30*(i+1),100,20)).string_(l.at(i));
			b.setProperty(\boxColor,Color.grey); b.setProperty(\stringColor,Color.white);			b.setProperty(\align,\center); b.value_(d.at(i));
		});
		b = SCButton(w,Rect(20,250,40,20)); 
		b.states = [["done",Color.black,Color.red]];
		b.action_({  
			numParams = w.view.children.at(0).value.asInt;
			numVoices = w.view.children.at(2).value.asInt;
			numSignals = w.view.children.at(4).value.asInt;
			numEffects = w.view.children.at(6).value.asInt;
			numBuffers = w.view.children.at(8).value.asInt;
			bufLength = w.view.children.at(10).value;
			numFFTBuffers = w.view.children.at(10).value.asInt;
			this.initBusses; this.initArrays; this.initBuffers;
			window = ConsoleWindow.new(this);
			presets = presets.add(this.storePresetValues);
			w.close
		});
		b.focus.keyDownAction_({ arg view, key, modifiers, unicode; 
			if( unicode == 3,{ b.action.value }); });
		presetRoutines = List.newClear(1);
	}
	
	initArrays { 
		var array;
		"initializing arrays".postln;
		sigCtlBusValues = List[];
		array = Array.fill(numSignals,{Array.fill(numParams,{0})});
		array.do({ arg list;
			sigCtlBusValues = sigCtlBusValues.add(Array.fill(numVoices,{ list.copy }));
		});
		signals = []; curSignals = []; susNotes = []; keyQueues = []; effSend = []; 
		sigRoutinesToggle = []; sigRoutNames = [];
		sigRoutines = []; sigRoutTempi = []; sigRoutWindows = [];
		effRoutinesToggle = []; effRoutNames = [];
		effRoutines = []; effRoutTempi = []; effRoutWindows = [];
		numSignals.do({ arg i;
			signals = signals.add("nil");
			curSignals = curSignals.add("nil");
			s.performList(\sendMsg,\c_setn,sigCtlBus.at(i).at(0).index, 					numParams*numVoices,sigCtlBusValues.at(i).flatten(2));
			effSend = effSend.add(Array.fill(numVoices,{0}));
			loopOn = loopOn.add(Array.fill(numVoices,{0}));
			loopDur = loopDur.add(Array.fill(numVoices,{1}));
			susNotes = susNotes.add(Array.fill(numVoices,{ List[] }));
			keyQueues = keyQueues.add(List[]);
			sigRoutinesToggle = sigRoutinesToggle.add(0);
			sigRoutNames = sigRoutNames.add("nil");
			sigRoutines = sigRoutines.add(nil);
			sigRoutTempi = sigRoutTempi.add(60);
			sigRoutWindows = sigRoutWindows.add("nil") 
		}); 
		signals = [signals]; 
		effCtlBusValues = Array.fill(numEffects,{Array.fill(numParams,{0})});
		inBus = []; effToggle = []; prevEffect = [];
		numEffects.do({ arg i;
			effects = effects.add("nil");
			curEffects = curEffects.add("nil");
			s.performList(\sendMsg, \c_setn, effCtlBus.at(i).index, 
				numParams, effCtlBusValues.at(i));
			prevEffect = prevEffect.add(nil);
			inBus = inBus.add(0);	
			effToggle = effToggle.add(0);
			effRoutinesToggle = effRoutinesToggle.add(0);
			effRoutNames = effRoutNames.add("nil");
			effRoutines = effRoutines.add(nil);
			effRoutTempi = effRoutTempi.add(60);
			effRoutWindows = effRoutWindows.add("nil") 
		});
		effects = [effects];
	}
	
	initBusses	{ "".postln; "initializing busses".postln;
		effMixBus = Bus.audio(s,4);
		sigCtlBus = []; gateBus = []; curSigSynths = [];
		numSignals.do({ sigCtlBus = 
			sigCtlBus.add(Array.fill(numVoices,{ Bus.control(s,numParams) }));
			susNotes = susNotes.add(Array.fill(numVoices,{ List[] }));
			gateBus = gateBus.add(Bus.control(s,numVoices)); 
			curSigSynths = curSigSynths.add(Array.fill(numVoices,{nil}));
			keyQueues = keyQueues.add(List[]);
		});
		effCtlBus = [];  prevEffect = [];
		numEffects.do({ 
			effCtlBus = effCtlBus.add(Bus.control(s,numParams));
			prevEffect = prevEffect.add(nil) 
		});
		volBus = Bus.control(s,1); volBus.value_(0);
	}
	
	initBuffers {
		"initializing buffers, length = ".post; bufLength.postln;
		recBuffers = List[]; recBufAlloc = List[];
		numBuffers.do({ arg i;
			recBuffers.add(Buffer.alloc(s,(sampRate*bufLength),1));
			recBufAlloc.add(nil);
		});
		recBuffers = recBuffers.asArray; recBuffers.do({ arg buf; buf.zero; }); 
		if( FFTBufs.buffers.isNil,{ FFTBufs.initClass(s).makeBufs(numFFTBuffers) },{
			FFTBufs.freeBufs; FFTBufs.makeBufs(numFFTBuffers) });
		fftBufAlloc = List[]; 
		numFFTBuffers.do({
			fftBufAlloc.add(nil)
		});
		if( ChebyBufs.buffers.isNil,{ ChebyBufs.initClass(s).makeBufs },{
			ChebyBufs.freeBufs; ChebyBufs.makeBufs(s) });
		if( VOscBufs.buffers.isNil,{ VOscBufs.initClass(s).makeBufs(8) },{
			VOscBufs.freeBufs; VOscBufs.makeBufs(8)  });
	}
	
	initMaps	{
		keyMap = IdentityDictionary[
			$q -> 0, $w -> 1, $e -> 2, $r -> 3, $t -> 4, $y -> 5, $u -> 6, $i -> 7, 
				$o->8, $p->9, $[->10, $]->11
		];
		srcMap = IdentityDictionary[
			$a -> 0, $s -> 1, $d -> 2, $f -> 3, $g -> 4, $h -> 5, $j -> 6, $k -> 7
		];
		effMap = IdentityDictionary[
			$z -> 0, $x -> 1, $c -> 2, $v -> 3, $b -> 4, $n -> 5, $m -> 6, $, -> 7
		];
		vcMap = IdentityDictionary[
			$! -> 1, $@ -> 2, $# -> 3, $$ -> 4, $% -> 5, $^ -> 6, $& -> 7, $* -> 8,
				$(->9,$)->10,$-->11,$=->12
		];
		numKeyMap = IdentityDictionary[ 0 -> $q,1 -> $w,2 -> $e,3 -> $r,4 -> $t,5 -> $y,
			6 -> $u,7 -> $i, 8->$o, 9->$p , 10->$[ , 11->$] ];
	}
	
	initKeyTrgFunc {
		keyTrgFunc = { arg view, key, modifiers, unicode;
			var voiceNum, susNote, keyQueue, thisVoiceNum, thisSignalNum;
		//	"key = ".post; key.post; " mod = ".post; modifiers.post; 
		// 	"uni = ".post; unicode.postln;
			voiceNum = keyMap.at(key); 
			keyQueue = keyQueues.at(curSignalNum);
			if( (voiceNum.notNil && (curSignals[curSignalNum] != "nil")),{ 		// if this is a trigger key
				window.curVoiceView.valueAction_(voiceNum.mod(numVoices)+1); 
					// view thisVoice values
				susNote = keyQueue.detect({ arg item, i; 
					(item.at(0)) == key });
				if( susNote.isNil,{	  // if note is not already playing, then play it 
					this.voiceOn(curSignalNum, voiceNum);
					// schedule note-off if loopOn is off
					if( loopOn.at(curSignalNum).at(curVoiceNum)  == 0,{
						thisVoiceNum = curVoiceNum.value; 
						thisSignalNum = curSignalNum.value;
						{ this.voiceOff(thisSignalNum, thisVoiceNum);
						}.defer(loopDur.at(curSignalNum).at(curVoiceNum));
					});
				},{		// if the note is playing, then turn it off
					this.voiceOff(curSignalNum,voiceNum)
				});
			},{ if( srcMap.at(key).notNil,{ window.signalSelect.valueAction_(srcMap.at(key));
					window.curPresetView.focus
				},{ if( effMap.at(key).notNil,{ window.effSelect.valueAction_(effMap.at(key));
						window.curPresetView.focus 
					},{ 
						if( vcMap.at(key).notNil,{  // shift-number key?
							curVoiceNum = vcMap.at(key);   // # sets curVoiceNum
							window.curVoiceView.valueAction_(curVoiceNum);
						},{ if (unicode == 32,{ 	// space-bar activates routine
							if(window.sigRoutToggleView.value == 0,{
								window.sigRoutToggleView.value_(1);
								this.sigRoutineOn(curSignalNum);
							},{ window.sigRoutToggleView.value_(0);
								this.sigRoutineOff(curSignalNum);
							})
						},{
							if (unicode == 127,{ this.allSignalsOff },{  // delete key kills all Notes
							 if( key == $/,{ window.findEffButton.valueAction_(1) },
							{ if( key == $.,{   // period toggles current effect on/off
								if( window.effToggleView.value == 0,{
									window.effToggleView.valueAction_(1)},{
									window.effToggleView.valueAction_(0)}) },{
										if( unicode == 63235,{ // left-right keys inc-dec presets											window.curPresetView.valueAction_(curPreset+2)},{
										if( unicode == 63234,{ 
											window.curPresetView.valueAction_(curPreset)}
									)})}
		)})})})})})})})};	
	}

	storePresetValues {
		^[ 
			sigCtlBusValues.collect({ arg genList; 
			genList.collect({ arg voiceList; 
				voiceList.collect({ arg it; it.value.round(0.000001) }); })
			}),
			curSignalNum.value,curVoiceNum.value,
			effSend.collect({ arg list; list.collect({ arg it; it.value }) }),
			loopOn.collect({ arg list; list.collect({ arg it; it.value }) }),
			loopDur.collect({ arg list; list.collect({ arg it; it.value }) }),
			sigRoutinesToggle.collect({ arg it; it.value }),
			sigRoutNames.collect({ arg it; it.value }),
			sigRoutTempi.collect({ arg it; it.value }),
			effCtlBusValues.collect({ arg effList; 
				effList.collect({ arg it; it.value.round(0.000001) }) }),
			curEffNum.value,
			effToggle.collect({ arg it; it.value }),
			inBus.collect({ arg it; it.value }),
			effRoutinesToggle.collect({ arg it; it.value }),
			effRoutNames.collect({ arg it; it.value }),
			effRoutTempi.collect({ arg it; it.value }),
		];  	
	}
	
	storePreset { var preset;
		signals = signals.add(curSignals.collect({ arg item; item.value }));
		effects = effects.add(curEffects.collect({ arg item; item.value }));
		preset = this.storePresetValues;
		presets = presets.add(preset);
		presetRoutines.add(nil);
		window.curPresetView.value_(presets.size);
		curPreset = presets.size-1;
	}
	
	deletePreset {
		if( curPreset+1 < presets.size,{ // if not the last Preset
			presets.remove(presets.at(curPreset));
			presetRoutines.remove(presetRoutines.at(curPreset));
			window.curPresetView.action.value(curPreset+1);
		},{							// if this is the last Preset 
			if( curPreset != 0,{		// and also not the only Preset
				presets.remove(presets.at(curPreset));
				curPreset = curPreset-1;
				window.curPresetView.action.value(curPreset+1);
			});
		});
	}
	
	restorePresetValues {  arg presetNum;
		sigCtlBusValues = presets.at(presetNum).at(0).collect({ arg genList; 
			genList.collect({ arg voiceList; voiceList.collect({ arg it; it.value }); })
		}); 
		curSignalNum = presets.at(presetNum).at(1).value;
		curVoiceNum = presets.at(presetNum).at(2).value;
		effSend = presets.at(presetNum).at(3).collect({ arg list; 
			list.collect({ arg it; it.value }) });
		loopOn = presets.at(presetNum).at(4).collect({ arg list; 
			list.collect({ arg it; it.value }) });
		loopDur = presets.at(presetNum).at(5).collect({ arg list; 
			list.collect({ arg it; it.value }) });
		sigRoutinesToggle = presets.at(presetNum).at(6).collect({ arg it; it.value });
		sigRoutNames = presets.at(presetNum).at(7).collect({ arg it; it.value });
		sigRoutTempi = presets.at(presetNum).at(8).collect({ arg it; it.value });
		effCtlBusValues = presets.at(presetNum).at(9).collect({  arg effList; 
			effList.collect({ arg it; it.value }) });
		curEffNum = presets.at(presetNum).at(10).value;
		effToggle = presets.at(presetNum).at(11).collect({ arg it; it.value }); 
		inBus = presets.at(presetNum).at(12).collect({ arg it; it.value }); 
		effRoutinesToggle = presets.at(presetNum).at(13).collect({ arg it; it.value });
		effRoutNames = presets.at(presetNum).at(14).collect({ arg it; it.value });
		effRoutTempi = presets.at(presetNum).at(15).collect({ arg it; it.value });
	}
	
	savePresetFile {
		var f;
		File.saveDialog("","",{ arg savePath; 
			f = File(savePath,"w");
			f.write(presets.size.asCompileString++"        //  numPresets "++"\n");
			f.write(numParams.asCompileString++"        //  numParams "++"\n");
			f.write(numVoices.asCompileString++"        //  numVoices "++"\n");
			f.write(numSignals.asCompileString++"        //  numSignals "++"\n");
			f.write(numEffects.asCompileString++"        //  numEffects "++"\n");
			f.write(numBuffers.asCompileString++"        //  numBuffers "++"\n");
			f.write(bufLength.asCompileString++"        //  bufLength "++"\n");
			f.write(numFFTBuffers.asCompileString++"        //  numFFTBuffers "++"\n");
			presets.size.do({ arg h;
				this.restorePresetValues(h);
				f.write("presetNum = "++h.asCompileString++"\n");
				signals.at(h).do({ arg sig, i;
					f.write(sig ++ "\n");
					numVoices.do({ arg j;
						var voiceData; voiceData = List[]; 
						numParams.do({ arg k;
							voiceData = voiceData.add(
								sigCtlBusValues.at(i).at(j).at(k).value);
						});
						f.write(voiceData.asArray.asCompileString++"\n"); 
					});
				});
				f.write(curSignalNum.value.asCompileString++ "\n");
				f.write(curVoiceNum.value.asCompileString++ "\n");
				f.write("effect sends"++"\n"); 
				f.write(effSend.value.asCompileString++ "\n");
				f.write("loopOns"++"\n"); 
				f.write(loopOn.value.asCompileString++ "\n");
				f.write("loopDurs"++"\n"); 
				f.write(loopDur.value.asCompileString++ "\n");
				f.write("sigRoutinesToggle"++"\n"); 
				f.write(sigRoutinesToggle.value.asCompileString++ "\n");
				f.write("sigRoutNames"++"\n"); 
				f.write(sigRoutNames.value.asCompileString++ "\n");
				f.write("sigRoutTempi"++"\n");
				f.write(sigRoutTempi.value.asCompileString++ "\n");
				effects.at(h).do({ arg eff, i;
					var effData; effData = List[];
					f.write(eff++"\n");
					numParams.do({ arg j;
						effData = effData.add(effCtlBusValues.at(i).at(j).value);
					});
					f.write(effData.asArray.asCompileString++"\n");
				});
				f.write("curEffNum"++"\n");
				f.write(curEffNum.value.asCompileString++ "\n");
				f.write("effToggle"++"\n");
				f.write(effToggle.value.asArray.asCompileString++"\n");
				f.write("inBus"++"\n");
				f.write(inBus.value.asArray.asCompileString++"\n");
				f.write("effRoutinesToggle"++"\n"); 
				f.write(effRoutinesToggle.value.asCompileString++ "\n");
				f.write("effRoutNames"++"\n"); 
				f.write(effRoutNames.value.asCompileString++ "\n");
				f.write("effRoutTempi"++"\n");
				f.write(effRoutTempi.value.asCompileString++ "\n");
			});
			f.close;
			this.restorePresetValues(curPreset)
		},{ "cancelled".postln });
	}
	
	loadPresetFile { arg f;
		var numPresets; 
		presets = [];
		numPresets = f.getLine.interpret; 
		"numPresets = ".post; numPresets.postln;
		presetRoutines = List.newClear(numPresets);
		numParams = f.getLine.interpret; numVoices = f.getLine.interpret; 
		numSignals = f.getLine.interpret; numEffects = f.getLine.interpret;
		numBuffers = f.getLine.interpret; bufLength = f.getLine.interpret;
		numFFTBuffers = f.getLine.interpret;
		signals = []; effects = [];
		numPresets.do({ arg i;
			var sigArray, prstSignals, prstEffects;
			f.getLine;
			prstSignals = [];  prstEffects = [];
			sigCtlBusValues = []; 
			numSignals.do({ arg i;
				var vcArray; vcArray = [];
				sigArray = [];
				prstSignals = prstSignals.add(f.getLine);
				numVoices.do({ arg i;
					vcArray = vcArray.add(f.getLine.interpret) 
				});
				sigArray = vcArray.collect({ arg list; 
						list.collect({ arg it; it.value }) });
				sigCtlBusValues = sigCtlBusValues.add(sigArray.value);
			});
			curSignalNum = f.getLine.interpret; curVoiceNum = f.getLine.interpret;
			f.getLine; effSend = f.getLine.interpret;
			f.getLine; loopOn = f.getLine.interpret;
			f.getLine; loopDur = f.getLine.interpret;
			f.getLine; sigRoutinesToggle = f.getLine.interpret;
			f.getLine; sigRoutNames = f.getLine.interpret;
			f.getLine; sigRoutTempi = f.getLine.interpret;
			effCtlBusValues = []; 
			numEffects.do({ arg i;
				prstEffects = prstEffects.add(f.getLine);
				effCtlBusValues = effCtlBusValues.add(f.getLine.interpret); 
			});
			f.getLine; curEffNum = f.getLine.interpret;
			f.getLine; effToggle = f.getLine.interpret;
			f.getLine; inBus = f.getLine.interpret;
			f.getLine; effRoutinesToggle = f.getLine.interpret;
			f.getLine; effRoutNames = f.getLine.interpret;
			f.getLine; effRoutTempi = f.getLine.interpret;
			signals = signals.add(prstSignals.collect({ arg item; item.value }));
			effects = effects.add(prstEffects.collect({ arg item; item.value }));
			presets = presets.add(this.storePresetValues);
		});
		f.close;
	}
	
	newPreset  { arg presetNum;
		var effNumHolder, signalNumHolder, prRoutDelay;
	//	" starting preset number ".post;	presetNum.postln; 
		if( prRout.notNil,{ prRout.stop; });
		effToggle.do({ arg eff, i;				// turn off effects
			window.effToggleView.value_(0); 
			this.effectOff(i)
		});
		sigRoutinesToggle.do({ arg rout, i;
			if( rout == 1,{
				window.sigRoutToggleView.value_(0);
				this.sigRoutineOff(i);
			});
		}); 
		effRoutinesToggle.do({ arg rout, i;
			if( rout == 1,{
				window.effRoutToggleView.value_(0);
				this.effRoutineOff(i);
			});
		}); 
		AppClock.sched(rlsTime,{
			recBufAlloc.fill(nil); fftBufAlloc.fill(nil);
			curPreset = ((presetNum).clip(0,presets.size-1)).asInt;
			window.curPresetView.value = (curPreset+1);
			this.restorePresetValues(curPreset);
			this.enableSignals;
			signalNumHolder = curSignalNum.value; // save actual curSignalNum
			curSignals.do({ arg sig, i;
				numVoices.do({ arg  j;
					curSignalNum = j;
					s.performList(\sendMsg,\c_setn, sigCtlBus.at(i).at(j).index,
						numParams,sigCtlBusValues.at(i).at(j));
				});
			});
			curSignalNum = signalNumHolder.value;
			this.enableEffects;
			curEffects.do({ arg eff, i;	// turn on new effects
				if( (effToggle.at(i) == 1) && (eff != "nil"),{
					s.performList(\sendMsg, \c_setn, effCtlBus.at(i).index, numParams, 							effCtlBusValues.at(i));
					this.effectOn(i);
				}); 
			});
			this.enableRoutines;
			sigRoutinesToggle.do({ arg rout, i;
				if( sigRoutinesToggle.at(i) == 1,{
					if( curSignalNum == i,{ window.sigRoutToggleView.value_(1); });
					this.sigRoutineOn(i)
				});
			});
			effRoutinesToggle.do({ arg rout, i;
				if( effRoutinesToggle.at(i) == 1,{
					if( curEffNum == i,{ window.effRoutToggleView.value_(1); });
					this.effRoutineOn(i)
				});
			});
			if( presetRoutines.at(curPreset).notNil,{
				prRout = Routine({ arg rout;
					loop({ 
						prRoutDelay = presetRoutines.at(curPreset).value(this); 
						if( prRoutDelay.notNil,{ prRoutDelay.wait },{ rout.alwaysYield })
					});
				});
				prRout.play(SystemClock);
			}); 
			window.curVoiceView.action.value(curVoiceNum+1);
			window.signalSelect.action.value(curSignalNum);  
			window.signalSelect.value = curSignalNum;
			window.effSelect.action.value(curEffNum); 
			window.effSelect.value = curEffNum;
			window.signalSelect.focus;			// defocus from this curPresetView
		});
	}
	
	selectRecBuffer { arg sigNum;
		var which;
		recBufAlloc.detect({ |item, i| if( item.isNil,{ which = i }); item.isNil });
		if(which.notNil,{
			recBufAlloc.put(which,sigNum)
		},{ "rec buffer not available !! ".post; 
		});
	}
	
	selectFftBuffer { arg sigNum;
		var which;
		fftBufAlloc.detect({ |item, i| if( item.isNil,{ which = i }); item.isNil });
		if(which.notNil,{
			fftBufAlloc.put(which,sigNum)
		},{ "fft buffer not available !! ".post; 
		});
	}
	
	getBuffers { arg sigNum;
		var r, f, result;
		result = recBufAlloc.detect({ arg item, i;  r = i; recBufAlloc.at(i) == sigNum });
		if( result.isNil,{ r = nil });
		result = fftBufAlloc.detect({ arg item, i;  f = i; fftBufAlloc.at(i) == sigNum });
		if( result.isNil,{ f = nil });
		^[r, f]
	}
		
	enableSignals {
		curSignals = signals[curPreset].collect({|sig| sig });
		numSignals.do({ arg i;
			var synth, specs, labels;
			synth = curSignals.at(i).asSymbol;
			if( SynthDescLib.global.synthDescs.at(synth).notNil,{
				specs = SynthDescLib.global.synthDescs.at(synth).controlNames
						.collect({ |item| item.asSymbol.asSpec });
				labels = window.truncLabels(synth);
				window.sigParamSpecs.put(i,specs);
				window.sigParamLabels.put(i,labels);
			},{ curSignals.at(i).post;  " signal synth not found !!".postln;
				curSignals.put(i,"nil"); 
			});
			window.signalSelect.items_(curSignals);
		});
	}
	
	enableRoutines {
		numSignals.do({ arg i;
			var rout;
			rout = Library.at(\consoleRoutines,(sigRoutNames.at(i)).asSymbol);
			if( (rout.class != ConsoleRoutine) && (sigRoutNames.at(i) != "nil"),{
				sigRoutNames.at(i).post; " routine not found !!".postln; 				sigRoutNames.put(i,"nil");
			});
		});
	}
	
	enableEffects {
		curEffects = effects[curPreset].collect({|eff| eff });
		numEffects.do({ arg i;
			var synth, specs, labels;
			synth = curEffects.at(i).asSymbol;
			if( SynthDescLib.global.synthDescs.at(synth).notNil,{
				specs = SynthDescLib.global.synthDescs.at(synth).controlNames
						.collect({ |item| item.asSymbol.asSpec });
				labels = window.truncLabels(synth);
				window.effParamSpecs.put(i,specs);
				window.effParamLabels.put(i,labels);
				if( SynthDescLib.global.synthDescs.at(synth).controlNames
					.includesEqual("recBufNum"),{ this.selectRecBuffer(i); });
				if( SynthDescLib.global.synthDescs.at(synth).controlNames
					.includesEqual("fftBufNum"),{ this.selectFftBuffer(i); });
			},{ curEffects.at(i).post;  " effect synth not found !!".postln;
				curEffects.put(i,"nil"); 
			});
			window.effSelect.items_(curEffects);
		});
	}
	
	voiceOn {  arg sigNum, vcNum=0,args;
		var synth,synthName, synthArgs,controlNames, vcIdx, gateIdx, synthNumParams, buf, bufNum, bufIdx, keyQueue;
		synthName = SynthDescLib.global.synthDescs
				.at((curSignals.at(sigNum)).asSymbol).name;
		controlNames = SynthDescLib.global.synthDescs.at(synthName.asSymbol).controlNames;
		synthNumParams = controlNames.collect({ |item|item.asSymbol })
				.removeAll([\gate,\effOut,\vol]).size;
		if( synthName.notNil,{ 
			synth = Synth.basicNew(synthName.asSymbol,s);
			vcIdx = (sigCtlBus.at(sigNum).at(vcNum)).index;
			gateIdx = gateBus.at(sigNum).index+vcNum;
			if( controlNames.detect({ |item| item.containsi("buf") }).notNil,{
				if(controlNames.includesEqual("sampbuf"),
					{ controlNames.detect({ |item,i| bufIdx = i; item == "sampBuf"});
						buf = sigCtlBusValues.at(sigNum).at(vcNum).at(bufIdx).
							clip(1,soundfiles.size).asInt-1;
						bufNum = sampBufs.at(buf).bufnum  
					},{ if(controlNames.includesEqual("recBuf"),{
							controlNames.detect({ |item,i| bufIdx = i; item == "recBuf"});
							buf = sigCtlBusValues.at(sigNum).at(vcNum).at(bufIdx
									.min(numParams-1)).clip(1,numBuffers).asInt-1;
							bufNum = recBuffers.at(buf).bufnum
						},{ if(controlNames.includesEqual("tblBuf"),{
							bufNum = VOscBufs.buffers[0].bufnum
					})})
			})});
			if( args.notNil,{
					synthArgs = [\bufNum,sampBufs[(args[0])].bufnum,\vol,args[1],
						\effOut, effMixBus.index+(effSend[sigNum][vcNum%numVoices])]
				},{
					synthArgs = [\bufNum,bufNum,
						\effOut, effMixBus.index+(effSend[sigNum][vcNum%numVoices])]
			});
			// start voice, map controls to busses
			s.sendBundle(s.latency,
				synth.newMsg(genProcGroup,synthArgs,\addToHead),
					[\n_mapn,synth.nodeID,0,vcIdx,synthNumParams],
					[\n_map,synth.nodeID,\gate,gateIdx],
					[\n_map,synth.nodeID,\vol,volBus.index],
					[\c_set,gateIdx,1]);  
			curSigSynths[sigNum].put(vcNum,synth);	
			keyQueue = keyQueues.at(sigNum);
			keyQueue = keyQueue.add([numKeyMap.at(vcNum),vcNum]); // put in keyQueue
			keyQueues.put(sigNum,keyQueue); 
			if( sigNum == curSignalNum,{			// show in window, if current
				{ window.notesPlaying.string_( keyQueues.at(curSignalNum)
					.collect({ arg item; item.at(0) }).asString) }.defer;
			});
		});
	}
	
	voiceOff { arg sigNum, vcNum=0;
		s.sendBundle(nil,[\c_set, 								(gateBus.at(sigNum).index+vcNum),-1.1]); 
		curSigSynths[sigNum].put(vcNum,nil);
		{ this.purgeQueue([numKeyMap.at(vcNum),vcNum],sigNum); }.defer;
	}
	
	voiceOnSched { arg delta, sigNum,vcNum=0;
		SystemClock.sched(delta,{ this.voiceOn(sigNum,vcNum) })
	}
	
	voiceOffSched { arg delta, sigNum,vcNum=0;
		SystemClock.sched(delta,{ this.voiceOff(sigNum,vcNum) })
	}
		
	purgeQueue {  arg note, thisSignalNum;
		var keyQueue;
		keyQueue = (keyQueues.at(thisSignalNum).flatten.removeAll(note).clump(2));
		keyQueues.put(thisSignalNum,keyQueue);		// replace keyQueue
		if( thisSignalNum == this.curSignalNum,{
			window.notesPlaying.string_(keyQueue.collect({ arg item; item.at(0) }).asString);
		})
	}
	
	allSignalsOff {
		numSignals.do({ arg i;
			numVoices.do({ arg j; this.voiceOff(i,j); })
		});
	}
	
	allVoicesOff { arg i;
		numVoices.do({ arg j; this.voiceOff(i,j); })
	}
	
	sigRoutineOn { arg sigNum;	
		var clock;
		if( sigRoutines.at(sigNum).isNil,{  //  if not already playing
			sigRoutinesToggle.put(sigNum,1);
			//	sigNum+1.post; " started".postln;
			clock = TempoClock.new(sigRoutTempi[sigNum]/60);
			clock.play(Library.at(\consoleRoutines,(sigRoutNames.at(sigNum)).asSymbol)
				.routine.value(this,sigNum));
			sigRoutines.put(sigNum,clock);
			if( curSignalNum == sigNum,{ { this.window.sigRoutToggleView.value_(1) }.defer });
		});
	}
	
	sigRoutineOff { arg sigNum; 
		sigRoutinesToggle.put(sigNum,0); 
		//	sigNum+1.post; " stopped ".postln;
		sigRoutines.at(sigNum).stop; 
		sigRoutines.put(sigNum,nil);
		if( curSignalNum == sigNum,{ { this.window.sigRoutToggleView.value_(0) }.defer });
		if( sigRoutWindows.at(sigNum) != "nil",{ sigRoutWindows.at(sigNum).close });
	}
	
	setSigRoutTempo { arg sigNum, tempo;
		sigRoutTempi.put(sigNum,tempo);
		if( sigRoutines[sigNum].notNil,{ sigRoutines[sigNum].tempo_(tempo/60) });
	}
	
	effRoutineOn { arg effNum;	
		var clock;
		if( effRoutines.at(effNum).isNil,{  //  if not already playing
			effRoutinesToggle.put(effNum,1);
			effNum+1.post; " started".postln;
			clock = TempoClock.new(effRoutTempi[effNum]/60);
			clock.play(Library.at(\consoleRoutines,(effRoutNames.at(effNum)).asSymbol)
				.routine.value(this,effNum));
			effRoutines.put(effNum,clock);
			if( curEffNum == effNum,{ { window.effRoutToggleView.value_(1) }.defer });
		});
	}
	
	effRoutineOff { arg effNum; 
		effRoutinesToggle.put(effNum,0); 
		effNum+1.post; " stopped ".postln;
		effRoutines.at(effNum).stop; 
		effRoutines.put(effNum,nil);
		if( curEffNum == effNum,{ { window.effRoutToggleView.value_(0) }.defer });
		if( effRoutWindows.at(effNum) != "nil",{ effRoutWindows.at(effNum).close });
	}
	
	setEffRoutTempo { arg effNum, tempo;
		var wind;
		wind = this.effRoutWindows.at(effNum);
		if( wind.class == PulseBox,{ wind.setTempo(tempo) });
		this.effRoutTempi.put(effNum,tempo)
	}
	
	effectOn { arg effNum;
		var effSynth,synthName,controlNames,maxParams,recBuf,fftBuf,bufIdx,buf,bufNum;
		synthName = SynthDescLib.global.synthDescs
					.at((curEffects.at(effNum)).asSymbol).name;
		controlNames = SynthDescLib.global.synthDescs.at(synthName.asSymbol).controlNames;
		if( synthName.notNil,{
			effToggle.put(effNum,1);
			effSynth = Synth.basicNew(synthName.asSymbol,s);
			if( effNum == curEffNum,{ { window.effToggleView.value_(1) }.defer });
			controlNames.detect({ |item, i| 
				maxParams = i; item == "gate" }); if(maxParams.isNil,{ maxParams = numParams });
			if( controlNames.detect({ |item| item.containsi("buf") }).notNil,{
				if( controlNames.includesEqual("recBuf"),{
					controlNames.detect({ |item,i| bufIdx = i; item == "recBuf"});
					buf = effCtlBusValues.at(effNum).at(bufIdx)
						.clip(1,numBuffers).asInt-1;
					bufNum = recBuffers.at(buf).bufnum  
				},{ 
					if( controlNames.includesEqual("fftBuf"),{
						controlNames.detect({ |item,i| bufIdx = i; item == "fftBuf"});
						buf = effCtlBusValues.at(effNum).at(bufIdx.min(numParams-1))
							.clip(1,numFFTBuffers).asInt-1;
						bufNum = FFTBufs.buffers[buf].bufnum
					},{ controlNames.detect({ |item,i| bufIdx = i; item == "wvsBuf"});
							buf = effCtlBusValues[effNum][bufIdx]
								.clip(1,ChebyBufs.wavetables.size).asInt-1;
							bufNum = ChebyBufs.buffers[buf].bufnum
			})})});
			s.sendBundle(s.latency,
				effSynth.newMsg(genProcGroup,
					[\effIn,effMixBus.index,
						\bufNum, bufNum],\addToTail),
					[\n_mapn,effSynth.nodeID,0,effCtlBus.at(effNum).index,maxParams],
					[\n_map,effSynth.nodeID,\vol,volBus.index]
			);
			prevEffect.put(effNum,effSynth);
			if( effRoutinesToggle == 1,{
				this.effRoutineOn(effNum)
			});
		});
	}
	
	effectOff { arg effNum;
		var prevGroup, synth;
		effToggle.put(effNum,0);
		if( effNum == curEffNum,{ { window.effToggleView.value_(0) }.defer });
		if( (prevEffect.at(effNum)).class == Synth,{
			synth = prevEffect.at(effNum);
			synth.release(rlsTime); // release the effect synth
			prevEffect.put(effNum,nil);
			if( effRoutines.at(effNum).notNil,{ effRoutines.at(effNum).stop;
			effRoutines.put(effNum,nil) 
		 	});
		});
	}
	
	sigCtlChange	{ arg sig, vc, ctl, val;
		s.sendMsg ("/c_set", 	
			((sigCtlBus.at(sig).at(vc)).index+(ctl)),val);
		sigCtlBusValues.at(sig).at(vc).put(ctl,val);
	}
	
	effCtlChange { arg eff, ctl, val;
		s.sendMsg ("/c_set", ((effCtlBus.at(eff)).index+(ctl)),val);
		effCtlBusValues.at(eff).put(ctl,val);
	}

	midiInit { arg funcArray;	//		[ noteFunc, ctlFunc, bendFunc] 
		funcArray.do({ |item| item.value(this) });
	}
	
	clearMIDIInFuncs {
		MIDIIn.noteOn = nil; MIDIIn.noteOff=nil; MIDIIn.polytouch = nil;
		MIDIIn.control = nil; MIDIIn.program = nil;			MIDIIn.touch = nil; MIDIIn.bend = nil;
	}

	shutDown	{
		if( MIDIClient.sources.size > 0,{ MIDIIn.disconnect; });
		this.clearMIDIInFuncs;
		if( prRout.notNil,{ prRout.stop });
		sigRoutinesToggle.do({ arg rout, i; if( rout == 1,{ sigRoutines[i].stop }) });
		effRoutinesToggle.do({ arg rout, i; if( rout == 1,{ effRoutines[i].stop }) });
		sigCtlBus.do({ arg item; 
			item.do({ arg bus; bus.free; }); 		// free control Buses
		});
		effCtlBus.do({ arg item; item.free; }); 
		gateBus.do({ arg item, i; item.free; });
		volBus.do({ arg item, i; item.free; });
		effMixBus.do({ arg item, i; item.free; });
		Server.freeAll; 						// stop all sounds on local servers
		sampBufs.do({ arg item; item.free });   	// free sample buffers
		recBuffers.do({ arg item; item.free });
		fftBuffers.do({ arg item; item.free });   	// free fft  buffers
		SystemClock.clear; AppClock.clear;
		Server.resumeThreads;
		s.latency = 0.2;
	}

}
