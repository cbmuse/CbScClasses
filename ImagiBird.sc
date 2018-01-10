ImagBird {
	// convert ProxyBird into SynthDef-based code
	classvar <>idCount=0;
	var name,<server,<>chan,<numChans,<>ambDims,
	envsPath,phrsPath,<>tempo,start,>show,<specs,<>outBus,
	<birdSynthDefs,<birdSynthDef,<>curSynth,
	<birdArgs,<birdFrqEnvs,<spatEnvs,
	<birdVol,<>lastVol=0.25,<birdSpat,lastAz=0,lastElev=0,
	<window,envDur=1,<>argsLists,guis,durSlider,ampGUI,pitchGUI,trgBtn,
	<envGUIs,<envSize=400,vcGUI,plFuncGUI,pauseFuncGUI,
	<plPhrsBtn,<plInfBtn,<>phrsTask,<gateGUI,pFacGUI,crvGUI,melBhvGUI,rytBhvGUI,
	<>listener,<>rout,<>infRout,playFuncs,<curPlayFunc,pauseFuncs,curPauseFunc,
	rhythmXfrmFuncs,melXfrmFuncs,<>xfrmWeights,<>muted=true,muteDispFlg=true,
	<behaviors,defaultArgs;

	*new { arg name,server,chan,numChans=2,ambDims,envsPath,phrsPath,tempo=60,
			start=true,show=true,specs,outBus=0;
		^super.newCopyArgs(name,server,chan,numChans,ambDims,envsPath,phrsPath,tempo,
			start,show,specs,outBus).init
	}

	init {
		if( VOscBufs.buffers.isNil,{ VOscBufs.initClass(server).makeBufs });
		ambDims = ambDims ?? switch(numChans,
			2,{[[-0.25pi,0.25pi],[0,0],[1,1]]},
			4,{[[-0.25pi, 0.25pi, 0.75pi, 1.25pi],[0,0,0,0],[1,1,1,1]]},
			6,{[[-0.25pi, 0.25pi, 0.75pi, 1.25pi,-0.5pi, 0.5pi],
				[-0.5pi,-0.5pi,-0.5pi,-0.5pi, 0.25pi,0.25pi],[1,1,1,1,1,1,1,1]]},
			8,{[[-0.454,0.454,0,0,-1.047,1.047,-2.39,2.39], // angle
				[0.088,0.088,1.22,0.35,0.35,0.524,0.524], // elev
				[1,1,0,0,0.56,0.5,0.8,0.8]]},  // littlefield (3 not used)
			{[[-0.25pi,0.25pi],[0,0],[1,1]]}		// defaults to 2 channels
		);
		// args are volEnv,fltEnv, frqDev,modFrq,modDepth,rq,timbre,ampModFrq,ampModDpth
		defaultArgs = [0.0,0.5,1.0,2000,1.0,0.0,0.2,0,1.0,0.0];
		this.loadBirdSynthDefs;  // can this have a completion function??
		specs = specs ?? [\amp.asSpec,\amp.asSpec,// volEnv,fltEnv
			[0.5,2.0,\lin,0,1.0].asSpec, // frqDev
			[80, 8000, 'lin', 0, 1200].asSpec,
			[0.01, 100, 'lin', 0, 0.1].asSpec,Spec.specs[\idx],Spec.specs[\rq],
			nil,[0.01, 100, 'lin', 0, 0.1].asSpec,\amp.asSpec];
		// [muteGate,pauseFactor,nowCrv,xfrmWeights]
		behaviors = [0.2,1.0,2.neg,[[1,0,0,0,0],[1,0,0,0,0]]];
		xfrmWeights = [behaviors[3][0].normalizeSum,behaviors[3][1].normalizeSum];
				// [muteGate,pauseFactor,recPhrsWgt,xfrmWeights]
		this.newListener;
		this.startBirds;
		if(show,{{this.makeControls}.defer});
		// loadEnvData changes size of argList -- interpolate envData to envSize
		{this.loadEnvData(envsPath)}.defer;
		if(start,{{plInfBtn.valueAction_(1)}.defer});
		idCount = idCount+1;
	}

	muteGate { ^behaviors[0] }  // bird muted when activity level is below gate
	pauseFactor { ^behaviors[1] }  // used by 'timeRatioPause' pauseFunc
	recPhrsWgt { ^behaviors[2] }  // exp curve weights more recent for rand choice of next phrase
	xfrmMode {^behaviors[3] }  // stores melody and rhythm xfrmWeights

	newListener {
		listener= MIDIListener.new(chan,unique: true); // each ImagiBird has own listen-history
		{ listener.start.view; listener.window.name_(name++" phrases");
			listener.loadPhrases(phrsPath);
			listener.startPhraseAnalysis;
		}.defer
	}

	startBirds {
		birdSynthDef = \bird1;
		birdArgs = Bus.control(server,10);
		birdArgs.setn(defaultArgs);
		birdFrqEnvs = Bus.control(server,2); birdFrqEnvs.setn([0,0]);
		birdSpat = Bus.control(server,2); birdSpat.set(0);
		birdVol = Bus.control(server,1); birdVol.set(0.25);

		this.loadPlayFuncs;
		curPlayFunc = 'playProbNotes';
		this.loadPauseFuncs;
		curPauseFunc = 'nextPhrsPause';
		this.loadXfrmFuncs;
	}

	// trigger envelopes and spatialization to play one phrase
	trg {
		curSynth = Synth(birdSynthDef,
			[\birdArgs,birdArgs,\birdVol,birdVol,\birdFrqEnvs,birdFrqEnvs,
				\birdSpat,birdSpat]);
		SystemClock.sched(envDur,{curSynth.release});
		argsLists.do {|list,i|
			{ var env = Env.new(list,
				Array.fill(envSize-1,{(envSize-1).reciprocal}));
			var envgen = EnvGen.kr(env,timeScale: envDur, doneAction: 2);
			Out.kr(birdArgs.index+i,envgen)
			}.play;
		};
		spatEnvs = {
			var birdAz = EnvGen.kr(Env.new([lastAz,lastAz=rrand(0,2pi)],
				[envDur]),doneAction:2);
			var birdElev = EnvGen.kr(Env.new([lastElev,lastElev=rrand(0,pi)]
				,[envDur]),doneAction:2);
			Out.kr(birdSpat,[birdAz,birdElev])
		}.play
	}


	makeControls {
		window = GUI.window.new(name++"Bird",Rect(468+(idCount*10),64-(idCount*25),1000,850));
		argsLists = Array.fill(10,{|i|Array.fill(envSize,{defaultArgs[i]})});
		window.view.decorator = FlowLayout(window.view.bounds);
		window.view.background = Color(0.6,0.8,0.8);
		trgBtn = GUI.button.new(window,Rect(0,0,60,20))
			.states_([["trg",Color.black,Color.white]])
			.action_({|vw|
				this.trg
			});

		durSlider = GUI.ezSlider.new(window,260@20,"dur",[0.2,20,\lin,0,0],
			{|vw| envDur=vw.value},1.0,labelWidth: 40,numberWidth:40);
		pitchGUI = EZNumber.new(window,100@20,"pitch",\freq,{|gui| curSynth.set(\frq,gui.value.round(0.01))},440,
			labelWidth:40,numberWidth:60);
		ampGUI = GUI.ezNumber.new(window,100@20,"amp",nil,{|gui| curSynth.set(\amp,gui.value)},0.5,
			labelWidth:40,numberWidth:40);
		window.view.decorator.nextLine;
		StaticText.new(window,Rect(0,0,80,25)).string_("  phrsArgs");
		GUI.button.new(window,Rect(0,0,80,20)).states_([["save",Color.black,Color.white]])
			.action_({ var f,all,voice,plFunc,psFunc;
				File.saveDialog("saveSettings","",{ arg savePath;
					f = File(savePath,"w");
					voice = vcGUI.items[vcGUI.value.asInt] ++ "\n";
					f.write(voice);
					plFunc = plFuncGUI.items[plFuncGUI.value.asInt] ++ "\n";
					f.write(plFunc);
					psFunc = pauseFuncGUI.items[pauseFuncGUI.value.asInt] ++ "\n";
					f.write(psFunc);
					f.write(behaviors.asCompileString++ "\n");
					all=envGUIs.collect({|vw| vw.value });
					f.write(all.asCompileString);
					f.close;
				},{ "cancelled".postln })
			});
		GUI.button.new(window,Rect(0,0,80,20)).states_([["load",Color.black,Color.white]])
			.action_({ this.dialogLoadEnvData });
		GUI.button.new(window,Rect(0,0,60,20))
			.states_([["refresh",Color.black,Color.white]])
			.action_({|vw|
				this.refreshArgs
			});
		StaticText.new(window,Rect(0,0,100,25)).string_("   melody Xfrm");
		melBhvGUI = GUI.multiSliderView.new(window,Rect(0,0,56,25))
			.thumbSize_(10.0).showIndex_(true).gap_(1).valueThumbSize_(3)
			.value_([1,0,0,0,0])
			.action_({|sl|
					behaviors[3][0].put(sl.index,sl.currentvalue);
					xfrmWeights.put(0,(behaviors[3][0]).normalizeSum);
			});
		StaticText.new(window,Rect(0,0,100,25)).string_("    rhythm Xfrm");
		rytBhvGUI = GUI.multiSliderView.new(window,Rect(0,0,56,25))
			.thumbSize_(10.0).showIndex_(true).gap_(1).valueThumbSize_(3)
			.value_([1,0,0,0,0])
			.action_({|sl|
				behaviors[3][1].put(sl.index,sl.currentvalue);
				xfrmWeights.put(1,(behaviors[3][1]).normalizeSum);
			});
		window.view.decorator.nextLine;
		StaticText.new(window,Rect(0,0,280,20));
		StaticText.new(window,Rect(0,0,320,20))
			.string_("none,reverse,invert,scramble,rrotate  none,reverse,scramble,rrotate,quantize")
			.font = Font("Arial",9);
		window.view.decorator.nextLine;
		StaticText.new(window,Rect(0,0,100,25)).string_("  voice");
		vcGUI = GUI.popUpMenu.new(window,Rect(0,0,80,25))
			.items_(birdSynthDefs.keys.asArray)
			.action_({|sl| this.changebirdSynth(birdSynthDefs.keys.asArray[sl.value])});
		StaticText.new(window,Rect(0,0,100,25)).string_("  play function");
		plFuncGUI = GUI.popUpMenu.new(window,Rect(0,0,100,25))
			.items_(playFuncs.keys.asArray)
			.action_({|sl| curPlayFunc = playFuncs.keys.asArray[sl.value]});
		StaticText.new(window,Rect(0,0,100,25)).string_("  pause func");
		pauseFuncGUI = GUI.popUpMenu.new(window,Rect(0,0,100,25))
			.items_(pauseFuncs.keys.asArray)
			.action_({|sl|
				curPauseFunc = pauseFuncs.keys.asArray[sl.value]});
		gateGUI = GUI.ezNumber.new(window,100@20,"gate",nil,{|num|behaviors.put(0,num.value)},0.1,
			labelWidth:40,numberWidth:40);
		pFacGUI = GUI.ezNumber.new(window,100@20,"pseFac",[0.01,20.0],
			{|num|behaviors.put(1,num.value)},1.0,labelWidth:60,numberWidth:40);
		crvGUI = GUI.ezNumber.new(window,100@20,"recPhrsWgt",[-20,-1],
			{|num|behaviors.put(2,num.value)},-2,labelWidth:60,numberWidth:30);
		window.view.decorator.nextLine;


		// change multiSliderViews to Plotters, so values of shapes are shown
		envGUIs = [
			StaticText.new(window,Rect(0,0,40,25)).string_("volEnv"),
			Plotter.new("volEnv",Rect(0,0,envSize+50,128),window),
			StaticText.new(window,Rect(0,0,40,25)).string_("fltEnv"),
			Plotter.new("fltEnv",Rect(0,0,envSize+50,128),window),
			window.view.decorator.nextLine,
			StaticText.new(window,Rect(0,0,40,25)).string_("frqDev"),
			Plotter.new("frqDev",Rect(0,0,envSize+50,128),window),
			StaticText.new(window,Rect(0,0,40,25)).string_("fltFrq"),
			Plotter.new("fltFrq",Rect(0,0,envSize+50,128),window),
			window.view.decorator.nextLine,
			StaticText.new(window,Rect(0,0,40,25)).string_("modFrq"),
			Plotter.new("modFrq",Rect(0,0,envSize+50,128),window),
			StaticText.new(window,Rect(0,0,40,25)).string_("modDpth"),
			Plotter.new("modDpt",Rect(0,0,envSize+50,128),window),
			window.view.decorator.nextLine,
			StaticText.new(window,Rect(0,0,40,25)).string_("rq");
			Plotter.new("rq",Rect(0,0,envSize+50,128),window),
			StaticText.new(window,Rect(0,0,40,25)).string_("timbre");
			Plotter.new("timbre",Rect(0,0,envSize+50,128),window),
			window.view.decorator.nextLine,
			StaticText.new(window,Rect(0,0,40,25)).string_("AMfrq");
			Plotter.new("amFrq",Rect(0,0,envSize+50,128),window),
			StaticText.new(window,Rect(0,0,40,25)).string_("AMdpth");
			Plotter.new("amDpt",Rect(0,0,envSize+50,128),window)
		];
		envGUIs= envGUIs.select({|gui| gui.class == Plotter });
		envGUIs.do {|gui,i|
			gui.value_(Array.fill(envSize,{0}));
			gui.specs = specs[i];
			gui.value_(Array.fill(envSize,{gui.specs[0].map(1.0.rand)}));
			gui.editMode = true;
			gui.editFunc = { |plotter, plotIndex,index,val|
				index.post; " ".post;
				//	argsLists[i].put(index,val.postln)  // editIndexLine skips
				argsLists.put(i,gui.value) // update whole list each editFunc
			}
		};
		window.view.decorator.nextLine;

		plInfBtn = GUI.button.new(window,Rect(0,0,200,40))
				.states_([["startPhrases",Color.black,Color.white],
						["stopPhrases",Color.black,Color.white]])
				.action_({|btn| switch(btn.value,
					1,{ this.playInfPhrases },
					0,{ infRout.stop })
				 });
		plPhrsBtn = GUI.button.new(window,Rect(0,0,200,40))
				.states_([["playPhrase",Color.black,Color.white],
						["stopPhrase",Color.black,Color.white]])
				.action_({|btn| switch(btn.value,
					1,{ this.playNumPhrases },
					0,{ rout.stop })
				 });
		this.refreshArgs;
		window.onClose_({ this.quit; listener.show.if({ listener.window.close });
		});
		window.front;
	}
	// only do this to convert loaded data from old 0-> format files !!
	refreshArgs {		 // refresh argsLists from graphic data
		envGUIs.do({|vw,i| var spec; spec = specs[i];
			argsLists[i] = vw.value.collect({|val| val }) })
	}

	changebirdSynth {|name|
		if(birdSynthDefs[name.asSymbol].notNil,{
			birdSynthDef = name.asSymbol;
			{this.refreshArgs}.defer })
	}

	playBirdPhrase { |phrsNum|
		var thisPhraseDur,rhythm,melody,size,thisPhraseNum;
		size = listener.phrases.size;
		// choose weighted recent rand choice phrase to play
		phrsNum.isNil.if({phrsNum = ([1,size,this.recPhrsWgt,1].asSpec.map(1.0.rand))});
		// display phrase chosen in Listener window
		if((listener.show && listener.showPlay),{
			{ listener.phrNumView.valueAction_(phrsNum) }.defer
		});
		thisPhraseNum = size-phrsNum;	// invert because display is opposite of phrases order
		thisPhraseDur = listener.phraseDur(thisPhraseNum);
		if(thisPhraseDur.notNil,{ envDur = thisPhraseDur; });
		{ durSlider.value = envDur }.defer;
		this.trg; 		// start envelopes
		playFuncs[curPlayFunc].value(thisPhraseNum,thisPhraseDur); // start playing phrase notes and amps
		thisPhraseDur.wait;		// wait during phrase performance
		"pause = ".post;
		pauseFuncs[curPauseFunc].value(thisPhraseNum).max(listener.break).postln.wait // wait until next phrase
	}

	playBirdPhraseMt { |phrsNum|
		var thisPhraseDur,rhythm,melody,size,thisPhraseNum;
		// activityLevel[0] + activityLevel[1] must exceed muteGate to play
		if(((listener.activityLevels[0]*0.1 + listener.activityLevels[1]) > this.muteGate),{
			if(muted,{ muted=false; muteDispFlg=true; "playing...".postln});
			size = listener.phrases.size;
			// choose weighted recent rand choice phrase to play
			phrsNum.isNil.if({phrsNum = ([1,size,this.recPhrsWgt,1].asSpec.map(1.0.rand))});
			if((listener.show && listener.showPlay),{
				{ listener.phrNumView.valueAction_(phrsNum) }.defer
			});
			thisPhraseNum = size-phrsNum;	// invert because display is opposite of phrases order
			thisPhraseDur = listener.phraseDur(thisPhraseNum);
			envDur = thisPhraseDur;
			{ durSlider.value = envDur }.defer;
			this.trg; 		// start envelopes
			"phraseNum = ".post; phrsNum.postln;
			playFuncs[curPlayFunc].value(thisPhraseNum,thisPhraseDur);
			thisPhraseDur.wait;		// wait during phrase performance
			"pause = ".post;
			pauseFuncs[curPauseFunc].value(thisPhraseNum)
			    .max(listener.break).min(15).postln.wait
		},{ if((muted==false),{ muted=true; "set to mute".postln
				},{ if(muteDispFlg,{ "muted".postln; muteDispFlg=false});
			listener.break.wait
		 	})
		})
	}

	playInfPhrases {
		infRout = Routine {
			loop({
		//		" ... inf loop running ".postln;
				if((listener.phrases.size < 2).not,{ this.playBirdPhraseMt },{ 0.1.wait });
			})
		}.play
	}

	playNumPhrases {|numPlays=1|
		rout = Routine {
			numPlays.do({
				this.playBirdPhrase(listener.phrases.size - listener.thisPhrase)
			});
			{ plPhrsBtn.value_(0) }.defer
		}.play
	}

	loadBirdSynthDefs {

		birdSynthDefs = IdentityDictionary[
			// revise synthdef args to read from birdArgs-bus using In.kr
			'bird1' -> SynthDef(\bird1,{
				arg frq=440,amp=0.5,birdArgs=0,birdVol=10,birdFrqEnvs=11,birdSpat=13,
				gate=1;
				var envArgs=In.kr(birdArgs,10),spatArgs=In.kr(birdSpat,2),
				frqEnvs=In.kr(birdFrqEnvs,2);
				var volEnv=envArgs[0],fltEnv=envArgs[1],frqDev=envArgs[2],
				fltFrq=envArgs[3],frqModFrq=envArgs[4],frqModDpth=envArgs[5],
				rq=envArgs[6],timbre=envArgs[7],ampModFrq=envArgs[8],
				ampModDpth=envArgs[9],frqEnv=frqEnvs[0],fltFrqEnv=frqEnvs[1];
				var tblBuf,bufPtr,sig,flt,swpLFO,numHrms=7,rng,w,x,y,z;
				var env= EnvGen.ar(Env.asr(releaseTime:0.1),gate,doneAction: 2);
				tblBuf = VOscBufs.buffers[0].bufnum;
				bufPtr = tblBuf+(Lag.kr(timbre*numHrms,0.05))
				.clip(tblBuf,(tblBuf+numHrms)-0.01);
				rng = frqModDpth.min(1.0)*bufPtr;
				swpLFO = LFTri.kr(frqModFrq,0,rng*0.5,(rng*0.5).neg);
				sig = VOsc.ar((bufPtr+swpLFO),frq+(frq*frqDev)+frqEnv,0,amp);
				sig = sig*(LFPar.ar(ampModFrq,0,ampModDpth*0.5,0.5));
				flt = RLPF.ar(sig,fltFrq,rq,fltEnv);
				flt=(flt+(sig*(1-fltEnv)))*env*volEnv;
				#w, x, y, z = BFEncode1.ar(flt*(In.kr(birdVol))
					,spatArgs[0],spatArgs[1]);
				Out.ar(0,BFDecode1.ar(w, x, y, z, ambDims[0],ambDims[1]))
			}).add,
			'bird2' -> SynthDef(\bird2,{
				arg frq=440,amp=0.5,birdArgs=0,birdVol=10,birdFrqEnvs=11,birdSpat=13,
				gate=1;
				var envArgs=In.kr(birdArgs,10),spatArgs=In.kr(birdSpat,2),
				frqEnvs=In.kr(birdFrqEnvs,2);
				var volEnv=envArgs[0],fltEnv=envArgs[1],frqDev=envArgs[2],
				fltFrq=envArgs[3],frqModFrq=envArgs[4],frqModDpth=envArgs[5],
				rq=envArgs[6],timbre=envArgs[7],ampModFrq=envArgs[8],
				ampModDpth=envArgs[9],frqEnv=frqEnvs[0],fltFrqEnv=frqEnvs[1];
				var tblBuf,bufPtr,sig,flt,swpLFO,numHrms=7,rng,fModLFO,w,x,y,z;
				var env= EnvGen.ar(Env.asr(releaseTime:0.1),gate,doneAction: 2);
				tblBuf = VOscBufs.buffers[0].bufnum;
				bufPtr = tblBuf+(Lag.kr(timbre*numHrms,0.05)).clip(tblBuf,(tblBuf+numHrms)-0.01);
				fModLFO = SinOsc.kr(frqModFrq,0,frqModDpth*2pi);
				sig = VOsc.ar(bufPtr,frq+(frq*frqDev)+frqEnv,fModLFO,amp);
				sig = sig*(LFPar.ar(ampModFrq,0,ampModDpth*0.5,0.5));
				flt = RLPF.ar(sig,fltFrq+fltFrqEnv,rq,fltEnv);
				flt=(flt+(sig*(1-fltEnv)))*env*volEnv;
				#w, x, y, z = BFEncode1.ar(flt*(In.kr(birdVol))
					,spatArgs[0],spatArgs[1]);
				Out.ar(0,BFDecode1.ar(w, x, y, z, ambDims[0],ambDims[1]))
			}).add,
			'hiBird' -> SynthDef(\hiBird,{
				arg frq=440,amp=0.5,birdArgs=0,birdVol=10,birdFrqEnvs=11,birdSpat=13,
				gate=1;
				var envArgs=In.kr(birdArgs,10),spatArgs=In.kr(birdSpat,2),
				frqEnvs=In.kr(birdFrqEnvs,2);
				var volEnv=envArgs[0],fltEnv=envArgs[1],frqDev=envArgs[2],
				fltFrq=envArgs[3],frqModFrq=envArgs[4],frqModDpth=envArgs[5],
				rq=envArgs[6],timbre=envArgs[7],ampModFrq=envArgs[8],
				ampModDpth=envArgs[9],frqEnv=frqEnvs[0],fltFrqEnv=frqEnvs[1];
				var tblBuf,bufPtr,sig,flt,swpLFO,numHrms=7,rng,fModLFO,w,x,y,z;
				var env= EnvGen.ar(Env.asr(releaseTime:0.1),gate,doneAction: 2);
				tblBuf = VOscBufs.buffers[0].bufnum;
				bufPtr = tblBuf+(Lag.kr(timbre*numHrms,0.05))
				.clip(tblBuf,(tblBuf+numHrms)-0.01);
				fModLFO = SinOsc.kr(frqModFrq,0,frqModDpth*2pi);
				sig = VOsc.ar(bufPtr,frq+(frq*frqDev*8)+frqEnv,fModLFO,amp);
				sig = sig*(LFPar.ar(ampModFrq,0,ampModDpth*0.5,0.5));
				flt = RLPF.ar(sig,fltFrq+fltFrqEnv,rq,fltEnv);
				flt=(flt+(sig*(1-fltEnv)))*env*volEnv;
				#w, x, y, z = BFEncode1.ar(flt*(In.kr(birdVol))
					,spatArgs[0],spatArgs[1]);
				Out.ar(0,BFDecode1.ar(w, x, y, z, ambDims[0],ambDims[1]))
			}).add,
			'chirpBird' -> SynthDef(\chirpBird,{
				arg frq=440,amp=0.5,birdArgs=0,birdVol=10,birdFrqEnvs=11,birdSpat=13,
				gate=1;
				var envArgs=In.kr(birdArgs,10),spatArgs=In.kr(birdSpat,2),
				frqEnvs=In.kr(birdFrqEnvs,2);
				var volEnv=envArgs[0].poll(10,"vol"),
				fltEnv=envArgs[1],frqDev=envArgs[2],
				fltFrq=envArgs[3],frqModFrq=envArgs[4],frqModDpth=envArgs[5],
				rq=envArgs[6],timbre=envArgs[7],ampModFrq=envArgs[8],
				ampModDpth=envArgs[9],frqEnv=frqEnvs[0],fltFrqEnv=frqEnvs[1];
				var tblBuf,bufPtr,sig,flt,chirp,numHrms=7,rng,fModLFO,w,x,y,z;
				var env= EnvGen.ar(Env.asr(releaseTime:0.1),gate,doneAction: 2);
				tblBuf = VOscBufs.buffers[0].bufnum;
				bufPtr = tblBuf+(Lag.kr(timbre*numHrms,0.05))
				.clip(tblBuf,(tblBuf+numHrms)-0.01);
				fModLFO = SinOsc.kr(frqModFrq,0,frqModDpth*2pi);
				sig = VOsc.ar(bufPtr,(frq*frqDev)+frqEnv,fModLFO,amp);
				sig = sig*(LFPar.ar(ampModFrq,0,ampModDpth*0.5,0.5));
				chirp = Decay.ar(Trig.ar(amp,0.001),0.05,fltFrq*0.5);
				flt = RLPF.ar(sig,(fltFrq+chirp+fltFrqEnv),rq,fltEnv).poll(10,"flt");
				flt=(flt+(sig*(1-fltEnv).poll(10,"sig")))*env*volEnv;
				#w, x, y, z = BFEncode1.ar(flt*(In.kr(birdVol))
					,spatArgs[0],spatArgs[1]);
				Out.ar(0,BFDecode1.ar(w, x, y, z, ambDims[0],ambDims[1]))
			}).add,
			'dblOscBird' -> SynthDef(\dblOscBird,{
				arg frq=440,frq2=880,amp=0.5,birdArgs=0,birdVol=10,
				birdFrqEnvs=11,birdSpat=13,gate=1;
				var envArgs=In.kr(birdArgs,10),spatArgs=In.kr(birdSpat,2),
				frqEnvs=In.kr(birdFrqEnvs,2);
				var volEnv=envArgs[0],fltEnv=envArgs[1],frqDev=envArgs[2],
				fltFrq=envArgs[3],frqModFrq=envArgs[4],frqModDpth=envArgs[5],
				rq=envArgs[6],timbre=envArgs[7],ampModFrq=envArgs[8],
				ampModDpth=envArgs[9],frqEnv=frqEnvs[0],fltFrqEnv=frqEnvs[1];
				var tblBuf,bufPtr,sig,flt,swpLFO,numHrms=7,rng,fModLFO,w,x,y,z;
				var env= EnvGen.ar(Env.asr(releaseTime:0.1),gate,doneAction: 2);
				tblBuf = VOscBufs.buffers[0].bufnum;
				bufPtr = tblBuf+(Lag.kr(timbre*numHrms,0.05))
				.clip(tblBuf,(tblBuf+numHrms)-0.01);
				fModLFO = SinOsc.kr(frqModFrq,0,frqModDpth*2pi);
				sig = VOsc.ar(bufPtr,frq+(frq*frqDev)+frqEnv,fModLFO,amp) +
				sig = VOsc.ar(bufPtr,frq2+(frq2*frqDev),fModLFO,amp)*0.5;
				sig = sig*(LFPar.ar(ampModFrq,0,ampModDpth*0.5,0.5));
				flt = RLPF.ar(sig,fltFrq+fltFrqEnv,rq,fltEnv);
				(flt+(sig*(1-fltEnv)))*env*volEnv;
				#w, x, y, z = BFEncode1.ar(flt*(In.kr(birdVol))
					,spatArgs[0],spatArgs[1]);
				Out.ar(0,BFDecode1.ar(w, x, y, z, ambDims[0],ambDims[1]))
			}).add,
			'noiseBird' -> SynthDef(\noiseBird,{
				arg frq=440,amp=0.5,birdArgs=0,birdVol=10,birdFrqEnvs=11,birdSpat=13,
				gate=1;
				var envArgs=In.kr(birdArgs,10),spatArgs=In.kr(birdSpat,2),
				frqEnvs=In.kr(birdFrqEnvs,2);
				var volEnv=envArgs[0],fltEnv=envArgs[1],frqDev=envArgs[2],
				fltFrq=envArgs[3],frqModFrq=envArgs[4],frqModDpth=envArgs[5],
				rq=envArgs[6],timbre=envArgs[7],ampModFrq=envArgs[8],
				ampModDpth=envArgs[9],frqEnv=frqEnvs[0],fltFrqEnv=frqEnvs[1];
				var tblBuf,bufPtr,sig,flt,swpLFO,numHrms=7,rng,fModLFO,noise,w,x,y,z;
				var env= EnvGen.ar(Env.asr(releaseTime:0.1),gate,doneAction: 2);
				tblBuf = VOscBufs.buffers[0].bufnum;
				bufPtr = tblBuf+(Lag.kr(timbre*numHrms,0.05)).clip(tblBuf,(tblBuf+numHrms)-0.01);
				fModLFO = SinOsc.kr(frqModFrq,0,frqModDpth*2pi);
				sig = VOsc.ar(bufPtr,frq+(frq*frqDev)+frqEnv,fModLFO,amp);
				noise = WhiteNoise.ar(amp*volEnv*(1-timbre));
				sig = sig+noise;
				sig = sig*(LFPar.ar(ampModFrq,0,ampModDpth*0.5,0.5));
				flt = RLPF.ar(sig,fltFrq+fltFrqEnv,rq,fltEnv);
				(flt+(sig*(1-fltEnv)))*env;
				#w, x, y, z = BFEncode1.ar(flt*(In.kr(birdVol))
					,spatArgs[0],spatArgs[1]);
				Out.ar(0,BFDecode1.ar(w, x, y, z, ambDims[0],ambDims[1]))
			}).add,
			/*		'prairieChick' -> {
			arg frq=440,amp=0.5,volEnv,fltEnv,
			frqDev,fltFrq,frqModFrq,frqModDpth,rq,timbre,ampModFrq,ampModDpth;
			var fund = frq*frqDev;
			var formFrq = LFNoise1.kr(ampModFrq, ampModDpth, fund+(frqModFrq*frqModDpth));
			var bwFrq = fund+(fund*timbre);
			var sig = Formant.ar(fund,  // fundamental
			formFrq,   // formant
			LFNoise1.kr(ampModFrq,ampModDpth),volEnv);
			sig*amp
			}*/
			// irrational, but sounds better?
			'prairieChick' -> SynthDef(\prairieChick,{
				arg frq=440,amp=0.5,birdArgs=0,birdVol=10,birdFrqEnvs=11,
				birdSpat=13,gate=1;
				var envArgs = In.kr(birdArgs,10), spatArgs=In.kr(birdSpat,2),
				frqEnvs=In.kr(birdFrqEnvs,2);
				var volEnv=envArgs[0],fltEnv=envArgs[1],frqDev=envArgs[2],
				fltFrq=envArgs[3],frqModFrq=envArgs[4],frqModDpth=envArgs[5],
				rq=envArgs[6],timbre=envArgs[7],ampModFrq=envArgs[8],
				ampModDpth=envArgs[9],frqEnv=frqEnvs[0],fltFrqEnv=frqEnvs[1];
				var w,x,y,z;
				var env= EnvGen.ar(Env.asr(releaseTime:0.1),gate,doneAction: 2);
				var fundFrq=frq*frqDev+frqEnv;
				var formFrq = (fundFrq+fltFrq+fltFrqEnv)
				+SinOsc.kr(frqModFrq,0,frqModDpth,fltFrq);
				var bwFrq = (formFrq/2)*rq;
				var sig = Formant.ar(fundFrq,formFrq,bwFrq,
					SinOsc.kr(ampModFrq,0,ampModDpth*0.5,ampModDpth*0.5)+amp);
				#w, x, y, z = BFEncode1.ar((sig*env*volEnv)*(In.kr(birdVol))
					,spatArgs[0],spatArgs[1]);
				Out.ar(0,BFDecode1.ar(w, x, y, z, ambDims[0],ambDims[1]))
			}).add
		];
	}

	// different ways to play phrases
	loadPlayFuncs {
		playFuncs = IdentityDictionary[
			// stochastic choice based on probabilities stored with each phrase
			'playProbNotes' -> {|thisPhraseNum,thisPhraseDur|
				var thisNote,thisPitch,thisAmp,thisOct,thisDur,rhythm;
				birdFrqEnvs.setn([0,0]);
				rhythm = Pseq(listener.evRhythm(thisPhraseNum),inf).asStream;
				phrsTask = Task({
					listener.evRhythm(thisPhraseNum).size.do {
						thisNote = Array.series(12,0,1).wchoose(listener.pitches(thisPhraseNum));
						thisOct = Array.series(12,0,1).wchoose(listener.octaves(thisPhraseNum));
						curSynth.set(\frq,thisPitch=((thisNote+(thisOct*12)).midicps));
						{ pitchGUI.value_(thisPitch)}.defer;
						curSynth.set(\amp,thisAmp=(listener.vels(thisPhraseNum)[thisNote]));
						{ ampGUI.value_(thisAmp)}.defer;
						SystemClock.sched((listener.durs(thisPhraseNum)[thisNote]),
							{ curSynth.set(\amp,0); nil });
						thisDur = rhythm.next.max(0.05);
						thisDur.wait;	// wait dur until next note
					}
				}).play
			},
			// play the recorded melody, with xfrms of pitch and duration
			'playMelody' -> {|thisPhraseNum,thisPhraseDur|
				var thisNote,thisPitch,thisAmp,thisOct,thisDur,rhythm,numNotes,melody,inputMelody, melXfrm,rhyXfrm;
				"playMelody".postln;
				birdFrqEnvs.setn([0,0]);
				inputMelody = listener.evMelody(thisPhraseNum);
				melXfrm= melXfrmFuncs.wchoose(xfrmWeights[0]);
				if(melXfrm.notNil,{melody = melXfrm.value(inputMelody);
					"mel = ".post; melody.postln;
				},{
					melody = inputMelody
				});
				melody = Pseq(melody,inf).asStream;
				rhyXfrm=rhythmXfrmFuncs.wchoose(xfrmWeights[1]);
				if(rhyXfrm.notNil,{rhythm = rhyXfrm.value(listener.evRhythm(thisPhraseNum));
					"ryt = ".post; rhythm.postln;
				},{
					rhythm = listener.evRhythm(thisPhraseNum)
				});
				numNotes=rhythm.size;
				rhythm = Pseq(rhythm,inf).asStream;
				phrsTask = Task({
					numNotes.postln.do {|i|
						"pitch = ".post;
						thisNote = melody.next.postln;
						curSynth.set(\frq,thisPitch=(thisNote.midicps));
						{ pitchGUI.value_(thisPitch)}.defer;
						"amp = ".post;
						curSynth.set(\amp,thisAmp=(listener.vels(thisPhraseNum)[inputMelody[i]%12]).postln);
						{ ampGUI.value_(thisAmp)}.defer;
						"duration = ".post;
						SystemClock.sched((listener.durs(thisPhraseNum)[inputMelody[i]%12]).postln,
							{ curSynth.set(\amp,0); nil }); " ".postln;
						thisDur = rhythm.next.max(0.05);
						thisDur.wait;	// wait dur until next note
					}
				}).play
			},
			// like playMelody, but adds frq2 dyad from recorded melody
			'playChord' -> {|thisPhraseNum,thisPhraseDur|
				var thisPitch,thisAmp,thisChord,frq2,thisOct,thisDur,rhythm,melody,inputMelody,
				numNotes,chord,melXfrm,rhyXfrm;
				birdFrqEnvs.setn([0,0]);
				inputMelody = listener.evMelody(thisPhraseNum);
				melXfrm= melXfrmFuncs.wchoose(xfrmWeights[0]);
				if(melXfrm.notNil,{
					melody = melXfrm.value(listener.evMelody(thisPhraseNum))
				},{
					melody = listener.evMelody(thisPhraseNum)
				});
				chord = Pseq(melody,inf).asStream;
				rhyXfrm=rhythmXfrmFuncs.wchoose(xfrmWeights[1]);
				if(rhyXfrm.notNil,{rhythm = rhyXfrm.value(listener.evRhythm(thisPhraseNum))},{
					rhythm = listener.evRhythm(thisPhraseNum)});
				numNotes=rhythm.size;
				rhythm = Pseq(rhythm,inf).asStream;
				phrsTask = Task({
					numNotes.do {|i|
						thisPitch = chord.next;
						thisChord = listener.evChord(thisPhraseNum)[i];
						// freq2 derived from chords present in listener, if any
						frq2 = if(thisChord.notNil,{
							if( thisChord.last.notNil && thisChord[1].notNil,{
								[thisPitch,thisPitch+(thisChord.last-thisChord[1])]
							},{ [thisPitch,thisPitch+12] })
						},{ [thisPitch,thisPitch+12] });
						curSynth.set(\frq,thisPitch.midicps);
						curSynth.set(\frq2,(frq2.midicps));
						{ pitchGUI.value_(thisPitch)}.defer;
						curSynth.set(\amp,thisAmp = (listener.vels(thisPhraseNum)[inputMelody[i]%12]));
						{ ampGUI.value_(thisAmp)}.defer;
						thisDur = (rhythm.next.max(0.05));
						SystemClock.sched((listener.durs(thisPhraseNum)[inputMelody[i]%12]),
							{ curSynth.set(\amp,0); nil });
						thisDur = rhythm.next.max(0.05);
						thisDur.wait;	// wait dur until next note
					}
				}).play
			},
			// same as playMelody, but FrqEnv and FltFrqEnv follow Melody with Env.perc
			'playMel+Envs' -> {|thisPhraseNum,thisPhraseDur|
				var thisPitch,thisAmp,thisOct,thisDur,rhythm,melody,
				inputMelody, numNotes,melXfrm,rhyXfrm;
				inputMelody = listener.evMelody(thisPhraseNum);
				melXfrm= melXfrmFuncs.wchoose(xfrmWeights[0]);
				if(melXfrm.notNil,{
					melody = melXfrm.value(listener.evMelody(thisPhraseNum))
				},{
					melody = listener.evMelody(thisPhraseNum)});
				melody = Pseq(melody,inf).asStream;
				rhyXfrm=rhythmXfrmFuncs.wchoose(xfrmWeights[1]);
				if(rhyXfrm.notNil,{
					rhythm = rhyXfrm.value(listener.evRhythm(thisPhraseNum))
				},{
					rhythm = listener.evRhythm(thisPhraseNum)});
				numNotes=rhythm.size;
				rhythm = Pseq(rhythm,inf).asStream;
				phrsTask = Task({
					numNotes.do {|i|
						thisPitch = melody.next;
						curSynth.set(\frq,(thisPitch.midicps));
						{ pitchGUI.value_(thisPitch)}.defer;
						curSynth.set(\amp,
							thisAmp=(listener.vels(thisPhraseNum)[inputMelody[i]%12]));
						{ ampGUI.value_(thisAmp)}.defer;
						thisDur = (rhythm.next.max(0.05));
						// generate birdFrqEnvs with each melody note
						{Out.kr(birdFrqEnvs.index,
							EnvGen.kr(Env.perc(0.025,thisDur
								,[thisPitch*2,thisPitch*4],-12),
								doneAction:2))
						}.play;
						SystemClock.sched((
							inputMelody = listener.evMelody(thisPhraseNum)),
						{ curSynth.set(\amp,0); nil });
						thisDur = rhythm.next.max(0.05);
						thisDur.wait;	// wait dur until next note
					};
				}).play
			}
		]
	}

	loadPauseFuncs {
		pauseFuncs = IdentityDictionary[
			// pause for the duration of the last recorded pause
		'lastPhrsPause' -> {|thisPhraseNum|
			(listener.phraseStartTime(thisPhraseNum) -
				(listener.phraseStartTime(thisPhraseNum+1) +
						(listener.phraseDur(thisPhraseNum+1))))},
		//  pauseTime = duration of this phrase
		'thisPhrsDur' -> {|thisPhraseNum|
			listener.phraseDur(thisPhraseNum) },
		// pause for the duration of a randomly chosen phrase
		'randPrevPause' -> { var thisPhrase;
			thisPhrase = listener.phrases.size.rand;
			listener.phraseStartTime(thisPhrase) -
				(listenerphraseStartTime(thisPhrase+1) + listener.phraseDur(thisPhrase+1)) },
		// pause is a proportion of the phraselength times pauseFactor
		// cur phrase dur * activityLevels0+1 * pauseFactor
		'timeRatioPause' -> {|thisPhraseNum| var phrsTime;
			phrsTime = listener.phraseDur(thisPhraseNum);
			phrsTime*((listener.activityLevels[1]+listener.activityLevels[0])*this.pauseFactor) },
		// random pause length between 1 and 5 beats
		'randBeatsPause' -> {|thisPhraseNum|
			(4.rand+1)*((60/tempo)/2) }
		]
	}

	loadXfrmFuncs {
		melXfrmFuncs = [
			nil,		// 'none'
			{|notes|"reverse melody".postln;  notes.reverse},   // 'reverse'
			{|notes| var p,oct; 	"invert melody".postln;   // 'invert'
				notes.collect({|note| p =note%12; oct=(note/12).floor; p = 12-p; (oct*12+p)})},
			{|notes| "scramble melody".postln; notes.scramble },   // 'scramble'
			{|notes| "rotate melody".postln; notes.rotate(notes.size.rand)}  // 'rrotate'
		];
		rhythmXfrmFuncs = [
			nil,		// 'none'
			{|durs| "reverse rhythm".postln; durs.reverse},    // 'reverse'
			{|durs| "scramble rhythm".postln; durs.scramble },   // 'scramble'
			{|durs| "rotate rhythm".postln; durs.rotate(durs.size.rand)}, //	'rrotate'
			{|durs| "quantize rhythm".postln;
				durs.collect({|dur|dur.round((60/tempo)/4).max(60/tempo/8)})}    // 'qtize'
		];
	}

	// use this to translate old env data in 0->1 format to actual parameter data
	dialogLoadOldEnvData {
		var file,all;
		File.openDialog("load envData",{ arg path;
			file = File.new(path,"r");
			vcGUI.valueAction_(
				vcGUI.items.indexOf(file.getLine.asSymbol;));
			plFuncGUI.valueAction_(
				plFuncGUI.items.indexOf(file.getLine.asSymbol));
			pauseFuncGUI.valueAction_(
				pauseFuncGUI.items.indexOf(file.getLine.asSymbol));
			behaviors = file.getLine.interpret;
			gateGUI.valueAction_(behaviors[0]);
			pFacGUI.valueAction_(behaviors[1]);
			crvGUI.valueAction_(behaviors[2]);
			melBhvGUI.value_(behaviors[3][0]);
			xfrmWeights.put(0,(behaviors[3][0]).normalizeSum);
			rytBhvGUI.value_(behaviors[3][1]);
			xfrmWeights.put(1,(behaviors[3][1]).normalizeSum);
			all = file.readAllString.interpret;
			envGUIs.do({|vw,i|
				vw.value_(all[i].collect({|val|
					if(specs[i].notNil,{specs[i].map(val.max(0.001)).postln},{val}) }))});
			file.close;  this.refreshArgs;
			envGUIs.do {|gui,i| gui.specs_(specs[i]); }; // reset x-specs
		},{ "cancelled".postln });
	}

		// use this to translate old env data in 0->1 format to actual parameter data
	dialogLoadEnvData {
		var file,all;
		File.openDialog("load envData",{ arg path;
			file = File.new(path,"r");
			vcGUI.valueAction_(
				vcGUI.items.indexOf(file.getLine.asSymbol;));
			plFuncGUI.valueAction_(
				plFuncGUI.items.indexOf(file.getLine.asSymbol));
			pauseFuncGUI.valueAction_(
				pauseFuncGUI.items.indexOf(file.getLine.asSymbol));
			behaviors = file.getLine.interpret;
			gateGUI.valueAction_(behaviors[0]);
			pFacGUI.valueAction_(behaviors[1]);
			crvGUI.valueAction_(behaviors[2]);
			melBhvGUI.value_(behaviors[3][0]);
			xfrmWeights.put(0,(behaviors[3][0]).normalizeSum);
			rytBhvGUI.value_(behaviors[3][1]);
			xfrmWeights.put(1,(behaviors[3][1]).normalizeSum);
			all = file.readAllString.interpret;
			envGUIs.do({|vw,i| vw.value_(all[i])});
			file.close;  this.refreshArgs;
			envGUIs.do {|gui,i| gui.specs_(specs[i]); }; // reset x-specs
		},{ "cancelled".postln });
	}

	loadEnvData { |loadPath|
		var file,all;
		loadPath.notNil.if({
			File.exists(loadPath).if({
				file = File.new(loadPath,"r");
				vcGUI.valueAction_(vcGUI.items.indexOf(file.getLine.asSymbol));
				plFuncGUI.valueAction_(
					plFuncGUI.items.indexOf(file.getLine.asSymbol));
				pauseFuncGUI.valueAction_(
					pauseFuncGUI.items.indexOf(file.getLine.asSymbol));
				behaviors = file.getLine.interpret;
				gateGUI.valueAction_(behaviors[0]);
				pFacGUI.valueAction_(behaviors[1]);
				crvGUI.valueAction_(behaviors[2]);
				melBhvGUI.value_(behaviors[3][0]);
				xfrmWeights.put(0,(behaviors[3][0]).normalizeSum);
				rytBhvGUI.value_(behaviors[3][1]);
				xfrmWeights.put(1,(behaviors[3][1]).normalizeSum);
				all = file.readAllString.interpret;
				envGUIs.do({|vw,i| var list=all[i];
	/*				if(list.size == 200,{list = list.stutter(2)});*/
					if(list.size < envSize,{
						list = list.stutter((envSize/list.size).asInt)
						.wrapExtend(envSize)});
					vw.value_(list)});
				file.close; this.refreshArgs;
				envGUIs.do {|gui,i| gui.specs_(specs[i]); }; // reset x-specs
			},{ "file not found".postln });
		});
	}

	quit {
		if(rout.notNil,{ rout.stop });
		if(infRout.notNil,{ infRout.stop });
		if(phrsTask.notNil,{ phrsTask.stop });
		birdVol.set(0);
		[ birdArgs,birdSpat,birdVol].do {|bus| bus.free };
		listener.stop; listener.free;
		idCount = idCount-1
	}


}
/*
b = ImagBird.new(\b1,s,1,2)
Synth(b.birdSynthDef.name)
b.curSynth=Synth(\bird1)
c = 10.do {|i|
		{var env = Env.new(b.argsLists[i],
				Array.fill(199,{199.reciprocal}));
			var envgen = EnvGen.kr(env,timeScale: 1, doneAction: 2);
			Out.kr(b.birdArgs.index+i,envgen)
		}.play;
		}
WARNING: SynthDef temp__2 too big for sending. Retrying via synthdef file

s.sendMsg('/free',1001)
b.envGUIs[0].findSpecs
[3].specs
b.specs[3]
*/