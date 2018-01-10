
DbauTracker {
	var <>player,<>soundFiles,<>inBus=8,<>inputSynth, <>analyzerSynth,<>trkSynth,trkSynthDef,
	<>phrases,trgBtn,<>phrsIsPlaying,phraseFiles,sfDurs,lastTime;
	var <>freqs, <>amps, <>phrDurs,
	<>loBounds,<>hiBounds,<>smoothThreshold,<>phrsWindows;

	*new {|player,soundfiles|
		^super.newCopyArgs(player,soundfiles).init
	}

	init {
		phrsIsPlaying = Array.fill(12,{false});
		freqs= Array.fill(12,{60.midicps});
		amps= Array.fill(12,{0.1});
		phrDurs= Array.fill(12,{0.1});
		loBounds = Array.fill(12,{0});
		hiBounds = Array.fill(12,{1});
		smoothThreshold=Array.fill(12,{0.0});
		phrsWindows = Array.fill(12,{});
		trkSynthDef = SynthDef(\trkSynth,{|freq=440,amp=0,bus=1|
			Out.ar(bus,LFCub.ar(freq,0,amp))}).send(Server.local);
	}

	// load and play soundfiles

	loadBuf  {|fnum|
		Buffer.cueSoundFile(Server.default,soundFiles[fnum.min(soundFiles.size-1)],
			numChannels: 1
		)
	}

	getSoundFileDurs  {
		sfDurs = soundFiles.collect {|f,i|
			var sf = SoundFile.new;
			sf.openRead(f); sf.close;
			// "duration = ".post; sf.duration.postln;
			sf.duration }
	}

	//play and analyze all files
	playAllFiles  {
		soundFiles.do {|f,i|
			var b = this.loadBuf(i);
			0.2.wait;
			if(i==0,{
				inputSynth = Synth(\dbau_DiskIn,[\inbuf,b,\loop,0]);
				analyzerSynth = Synth.after(inputSynth,\dbau_track);
			},{ inputSynth.set(\inbuf,b) });
			i.post; " playing...".post; sfDurs[i].postln.wait;
		};
		"done".postln;
		inputSynth.free; analyzerSynth.free;
	}

	// play & analyze one file
	playFile  {|fnum|
		var buf = this.loadBuf(fnum);
		inputSynth = Synth(\dbau_DiskIn,[\inbuf,buf,\loop,0]);
		analyzerSynth = Synth.after(inputSynth,\dbau_track);
	}

	loadPhrase {|path|
		var file, names;
		File.exists(path).if( {
			file = File.new(path,"r");
			phrases = phrases.add(file.readAllString.interpret.postln);
			"phrase added, phraseNum = ".post; phrases.size.postln;
			file.close})
	}

	savePhrases {|path|
		var file, phrases;
		file = File.new(path, "w");
		phrases = phrases.deepCopy;
		phrases.do {|p| p.asCompileString };
		file.write(phrases.asCompileString);
		file.close
	}

	makeGUI  {|analSynth|
		var fv,av;
		var w = Window.new("audio analyzer",Rect(5,5,360,300));
		analSynth ?? { analyzerSynth=analSynth };
		w.view.decorator = FlowLayout(w.view.bounds);
		w.view.background=Color(0.6,0.8,0.8);

		EZSlider(w.view,280@20,"pitchThresh",[0.5,1.0].asSpec,initVal: 0.96,labelWidth: 80,
			action: {|v| analyzerSynth ?? { analyzerSynth.set(\pThresh,v.value) } });

		trgBtn = Button(w.view,60@20)
		.states_([["no-trg",Color.white,Color.black],
			["trg",Color.white,Color.red]]);

		av =EZSlider(w.view,280@20,"ampThresh",
			[0.001,0.05,\exp,0.001].asSpec, initVal: 0.002,
			labelWidth: 80,
			action: {|v| analyzerSynth ?? {analyzerSynth.set(\aThresh,v.value)} });
		av.numberView.step_(0.001).decimals_(4);

		Stethoscope.new(Server.default,1,inBus,view:w.view).view.bounds_(Rect(0,50,360,250));
		w.onClose_({ trgBtn=nil });
		w.front;
	}

	// play & analyze one file
	analyzeInput  {|inbus=0,outbus=16|
		inputSynth = Synth(\dbau_in,[\in,inbus,\outbus,outbus]);
		analyzerSynth = Synth.after(inputSynth,\dbau_track);
	}

	plotPhrs  {|pnum|
		var env;
		var phrs = phrases[pnum];
		var size=phrs.size;
		var times=phrs.collect {|ev| ev[2] }.differentiate;
		times.removeAt(0);
		times=times.collect {|t| t.min(0.1) }; // filter out early waits
		env = Env(phrs.collect {|ev| ev[0]},times);
		if(times.notEmpty,{
			phrsWindows= phrsWindows.put(pnum,env.plot(
				name: "Phrase#"++ pnum.asString ++ " size= " ++ size.asString ++
				" dur= " ++ times.sum.round(0.01).asString).parent);
			phrsWindows[pnum].bounds_(Rect(5,310,400,300));
		},{ pnum.post; " envelope has nil duration".postln });
		env
	}

	// recv and store a phrase pitches/amp data
	collectPhraseData  {
		^Routine {
			var func,curphrase;
			var first=true;
			var lowbounds = 38.midicps;  // D above cello C, 73.416 Hz
			var pnum=0;
			var numPhrases=12;
			var collPhrsPause=true;
			var makePhraseEnd=false;
			phrases = Array.fill(numPhrases,{nil});
			lastTime=thisThread.seconds;

			func = OSCFunc({|msg,time|
				var pitch=msg[3];
				var amp = msg[4];
				if((pitch>lowbounds),{
					if(trgBtn.notNil,{ // flash gui trgBtn if open
						{ trgBtn.valueAction_(1);
							AppClock.sched(0.1,{ trgBtn.valueAction_(0); })
						}.defer;
					});
					if(first,{
						first=false;
						collPhrsPause=false;
						"new phrase".postln;
						curphrase= [[pitch,amp,time]];
						},{
							curphrase= curphrase.add([pitch,amp,time]);
					});
					lastTime = time
				});
			},'/dbauEvent');

			loop { // store and plot a phrase
				if( // if there is either a silence of > 1 sec OR manual segment triggered
					((((thisThread.seconds - lastTime) > 1) || makePhraseEnd)
						&& (curphrase.size > 19)),{  // AND phrase is at least 20 events long
						if(collPhrsPause.not // if this is not the first event of new phrase
							&& ((lastTime - curphrase[0][2]) > 2),{ // AND phrase is at least 2 sec
								curphrase = curphrase.select {|ev,i| // remove noise gap events
									if(((curphrase[i][2] - curphrase[(i-1).max(0)][2]) < 1.0),{
										true },{i.post; " ".post; ev.postln; false })
								};
								curphrase.removeAt(0);  // trim first event
								"phrs#".post; pnum.post; " ".post;
								curphrase.post; " size= ".post;
								curphrase.size.postln;
								phrases= phrases.put(pnum,curphrase);
								{ this.plotPhrs(pnum);
									pnum=(pnum+1).mod(numPhrases)
								}.defer;
								makePhraseEnd=false;  // reset manual end trigger
								collPhrsPause=true;  // pause until next phrase has started
								first=true;  // trigger
				})});
				0.05.wait
			}
		}.play
	}

	playPhraseData  {|pnum=0,show=false|
		var rout;
		pnum=pnum.asInt;
		if(phrsIsPlaying[pnum].not,{
			if(player=="trk",{ trkSynth = Synth(\trkSynth) });
			rout = {
				var phr,pfreqs,pamps,times;
				phr = phrases[pnum.asInt];
				phr.do{|el,i| if(el[1]< smoothThreshold[pnum],{ phr.remove(el) })};
				pfreqs = phr.collect {|ev| ev[0] };
				pamps = phr.collect {|ev| ev[1] };
				times = phr.collect {|ev| ev[2] }.differentiate;
				times = times.collect {|t| t.min(0.1) }; // acoustic melody has constant freq changes, so filter out early waits
				times.removeAt(0); times.add(0);
				pamps = pamps.normalize(pamps[pamps.minIndex],1);
				pamps = [0.1,1].asSpec.map(pamps);  // expand low amplitudes
				phrsIsPlaying.put(pnum,true);
				inf.do {
					var curIdx,curDur;
					var spec= [0,pfreqs.size-0.001].asSpec;
					var hi = spec.map(hiBounds[pnum]).floor;
					var lo = spec.map(loBounds[pnum]).floor;
					if(hi==lo,{if(hi!=(spec.maxval.floor-1),{hi=hi+1},{hi=hi-1})});
					"bounds = ".post; lo.post; " ".post; hi.postln; "".postln;
					(hi-lo).abs.do {|i|
						if((hi>lo),{ curIdx=lo+i },{ curIdx=lo-i });
						freqs.put(pnum,pfreqs[curIdx]);
								amps.put(pnum,pamps[curIdx]);
						if(player.notNil,{
							if(player.class==DbauPlayer,{
								{ player.phrsDataGUIs[pnum].index_(6)
									.currentvalue_(\freq.asSpec.unmap(pfreqs[curIdx]));
									player.phrsDataGUIs[pnum].index_(7)
									.currentvalue_(pamps[curIdx])}.defer;
							},{ // for a simple synth player
								trkSynth.set(\freq,pfreqs[curIdx]);
								trkSynth.set(\amp,pamps[curIdx]);
							})
						});
						if(show,{
							("phrs#"++pnum.asString++
								" "++(curIdx.asString)++"= ").post;
							[pfreqs[i].round(0.01),pamps[i]].round(0.001).postln;
						});
						if(phrsIsPlaying[pnum],{
							if(player.class==DbauPlayer,{
								curDur = player.phrsSpd[pnum].reciprocal*times[curIdx]
							},{ curDur = times[curIdx] });
							phrDurs.put(pnum,curDur); // store duration for pdef access
							curDur.wait
						},{ phrsIsPlaying.put(pnum,false);
							trkSynth !? { trkSynth.postln.free; trkSynth=nil; };
							rout.stop;
						});
					};
					0.001.wait;
				};
			}.fork;
			^rout
		});
	}

	mutePhraseData {|pnum| phrases[pnum.asInt] ?? { phrases.put(pnum.asInt,false) }}

}
