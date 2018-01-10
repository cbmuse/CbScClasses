DbauPlayer {
	var <>tracker, mstrVol, density, tempo, modParam, octs,velAmps,noteVol,
	phrsSpd, <lastNote, susNotes,susPed,limiter,limiterThreshold, limiterSlope,
	makePhraseEnd, ostinato,w,<>phrsDataGUIs,<>psetGUIs;
	var s,<dbauGranSynths, <dbauProcSynths, <rvbSynth;
	var <harms,  <durs, <dweights, <freqFunc,<psets,<defs, <sets;

	*new {
		^super.new.init
	}

	init {
		mstrVol=0.1;
		density= Array.fill(12,{1});
		tempo=Array.fill(12,{1});
		modParam=Array.fill(12,{0});
		octs=Array.fill(12,{4});
		velAmps=Array.fill(12,{1});
		noteVol=Array.fill(12,{1});
		phrsSpd=Array.fill(12,{1});
		lastNote=0;
		susNotes=[];
		susPed=false;
		limiterThreshold = 0.5;
		limiterSlope=0.5;
		makePhraseEnd=false;
		ostinato=false;
	}

	startPlayer  {
		s=Server.local.waitForBoot({
			DbauSynthDefs.init;

			tracker = DbauTracker.new(this).makeGUI;
			// play analyze audio input
			tracker.analyzeInput(0);
			tracker.collectPhraseData;  // start collecting data from input

			this.makeGUIs;
			dbauGranSynths = Group.new;
			dbauProcSynths = Group.new(dbauGranSynths,'addAfter');
			limiter = Synth(\dbau_limiter,[\inbus,17],target: dbauProcSynths);
			rvbSynth = Synth(\dbau_reverb,[\inbus,17],target: dbauProcSynths);
			this.defsInit;
			this.setsInit;
			sets[0].value;     // start with fast 1 harmonic
			this.defsOnPause; // start them all with default value, then pause

			this.loadMIDIResponders;
			this.loadOSCdefs
		})
	}

	makeGUIs {
		w=Window.new("danbauCtls",Rect(400, 515, 1040, 350));
		w.addFlowLayout(10@10,10@5);
		12.collect({StaticText(w,75@10).font_("Times",9).string=" t d m s l h f a" });
		w.view.decorator.nextLine;
		phrsDataGUIs= 12.collect {|i|
			var m= MultiSliderView(w.view,75@256);
			m.valueThumbSize=4; m.indexThumbSize=8;
			m.value=
			([tempo,density,modParam,phrsSpd,tracker.loBounds,tracker.hiBounds,tracker.freqs,tracker.amps]
				.collect{|array| array[i]})
		};
		w.view.decorator.nextLine;
		psetGUIs= 12.collect {|i|
			EZNumber(w,75@20,"set",[0,5,\linear,1].asSpec,{|ez|
				psets.put(i,ez.value) }, initVal:0,labelWidth:20)};
		w.view.decorator.nextLine;
		Button(w.view,60@20).states_([["RESET",Color.red,Color.black]])
		.action_({ this.startDefs });
		w.front;
	}
	// start and pause defs, so they resume from place stopped instead of beginning
	// maybe instead, use pause resume on tracking routines,storing ref to each routine rather than an on/off flag
	defsOnPause {
		defs.do {|def| def.play; def.pause };
	}

	turnOffDefs {
		defs.do {|def,i| if(def.isPlaying,{
			def.pause; tracker.phrsIsPlaying.put(i,false) })}
	}

	// midi controls of pdefs
	loadMIDIResponders {
		// remove default midi
		CCResponder.removeAll; NoteOnResponder.removeAll;
		NoteOffResponder.removeAll; BendResponder.removeAll; TouchResponder.removeAll;
		MIDIIn.clear;
		MIDIIn.connectAll;

		// E1 controls tempo
		MIDIdef.cc(\ctlTempo,{|val|
			var spec = [0.25,4,\exp,0.125].asSpec;
			var tempo = spec.map(val/127);
			if(tempo>1,{ tempo = tempo.ceil });
			tempo.put(lastNote,tempo);
			{ phrsDataGUIs[lastNote].index_(0).currentvalue_(val/127) }.defer
		},30).fix;
		// E2 controls density
		MIDIdef.cc(\ctlDensity,{|val| density.put(lastNote,[0.25,4].asSpec.map(val/127));
			{ phrsDataGUIs[lastNote].index_(1).currentvalue_(val/127)}.defer },31).fix;
		// E3 controls limiter Threshold
		MIDIdef.cc(\ctlLimThresh,{|val|"limiter threshold= ".post;
			limiter.set(\thresh,limiterThreshold=[0.001,0.5,\exp].asSpec.map(val/127).postln)
		},32).fix;
		//  E4 controls playback smoothing threshold
		MIDIdef.cc(\smoothThreshold,{|val| "playbackThreshold= ".post;
			tracker.smoothThreshold.put(lastNote,[0.001,0.1,\exp].asSpec.map(val/127).postln) },33).fix;
		// E5 controls phraseData sequence speed
		MIDIdef.cc(\phrsSpd,{|val| phrsSpd.put(lastNote,[0.125,4,\exp,0.125].asSpec.map(val/127));
			{ phrsDataGUIs[lastNote].index_(3).currentvalue_(val/127) }.defer },34).fix;
		// E6 and 7 control lo/hi phrase bounds
		MIDIdef.cc(\loBound,{|val| tracker.loBounds.put(lastNote,val/127);
			{ phrsDataGUIs[lastNote].index_(4).currentvalue_(val/127) }.defer },35).fix;
		MIDIdef.cc(\hiBound,{|val| tracker.hiBounds.put(lastNote,val/127);
			{ phrsDataGUIs[lastNote].index_(5).currentvalue_(val/127) }.defer },36).fix;
		// E8 sets note volume multiplier
		MIDIdef.cc(\noteVolume,{|val| noteVol.put(lastNote,[1,4,\amp].asSpec.map(val/127))},27).fix;
		// footpedalcontrols master volume
		MIDIdef.cc(\ctlVolume,{|val| mstrVol=(val/127);
			limiter.set(\amp,mstrVol); rvbSynth.set(\amp,mstrVol)},37).fix;
		// modwheel sets modulation parameter
		MIDIdef.cc(\ctlModulation,{|val|
			modParam.put(lastNote,(val/127));
			{ phrsDataGUIs[lastNote].index_(2).currentvalue_(val/127) }.defer
		},26).fix;
		// pitchbend sets rvbFB level
		MIDIdef.cc(\rvbFB,{|val|
			val = [0.05,1,\exp,0.025].asSpec.map(val/127);
			rvbSynth.set(\fb,val);
			// val.postln;
		}).fix;
		// start arrow turns on ostinato playback mode
		MIDIdef.cc(\ostOn,{|val| if(val>0,{ostinato=true;
			{w.background_( Color(1.0))}.defer },{ ostinato=false;
			{w.background_(Color.grey(0.75))}.defer } )
		},80).fix;
		// stop button turns off ostinato, phrsSpd controls playback speed
		/*		MIDIdef.cc(\ostOff,{|val| ostinato=false;
		{w.background_(Color.grey(0.75))}.defer },116).fix;*/

		// P1-6 selects new pdef sets for current note
		MIDIdef.cc(\setsLo,{|val,cc|sets[(cc-81)].value;{psetGUIs[lastNote].value_(cc-81)}.defer },(81..85)).fix;
		MIDIdef.cc(\setsLoLast,{|val,cc|sets[(5)].value;{psetGUIs[lastNote].value_(5)}.defer },(94)).fix;
		/*MIDIdef.cc(\setsHi,{|val,cc|sets[(cc-82)].value;{psetGUIs[lastNote].value_(cc-82)}.defer },[86,87]).fix;*/
		// P7 kills hanging notes
		MIDIdef.cc(\killNotes,{|val,num| dbauGranSynths.freeAll;
			dbauGranSynths = Group.before(dbauProcSynths); },88).fix;
		// P8 forces phrsEnd
		MIDIdef.cc(\phrsEnd,{|val,num| makePhraseEnd=true },89).fix;

		MIDIdef.noteOn(\dbauOn,{|vel,note|
			var def,oct;
			oct = (note/12).floor;
			note = note.mod(12);
			{ if(tracker.phrsWindows[note].notNil,{ tracker.phrsWindows[note].front })}.defer;
			octs.put(note,oct);
			lastNote = note; velAmps.put(note,vel/127);
			def = defs[note];
			// "on ".post; note.postln;
			if(tracker.phrases[note].notNil,{ // if phrase exists
				if(def.isPlaying.not,{ // AND def not already playing
					tracker.playPhraseData(note,true); // start feeding def w/ phrase data
					def.postln.resume;  // resume playing def
			})})
		}).fix;

		MIDIdef.noteOff(\dbauOff,{|vel,note|
			var def;
			note = note.mod(12);
			def = defs[note];
			// "off ".post; note.postln;
			if(susPed.not,{
				if(def.isPlaying,{ def.pause; tracker.phrsIsPlaying.put(note,false) })
			},{ susNotes = susNotes.add(note)});
		}).fix;

		// sustain pedal
		MIDIdef.cc(\sustain,{|val,ccNum|
			if(val>0,{ susPed = true
			},{ susNotes.do {|num|
				defs[num].pause; tracker.phrsIsPlaying.put(num,false) };
			susNotes=[]; susPed = false;
			})
		},64);

		MIDIdef.touch(\dbauTouch,{|touch,chan|
			limiter.set(\amp,((touch/127)+1)*mstrVol)
		}).fix;
	}

	loadOSCdefs {
		// open height(ypos) controls tempo
		OSCdef(\leap_lclY,{|msg,time,addr,recvPort|
			var temp = [0.125,4,\exp,0.125].asSpec.map(msg[1]);
			if(temp>1,{ temp = temp.ceil });
			tempo.put(lastNote,temp);
			{ phrsDataGUIs[lastNote].index_(0).currentvalue_(msg[1]) }.defer },  '/left/closed/posy');

		// open horiz (xpos) controls phrsSpd
		OSCdef(\leap_lclX,{|msg,time,addr,recvPort|
			var spd = [0.125,4,\exp,0.125].asSpec.map(1-(msg[1]*0.5+0.5));
			if(spd>1,{ spd = spd.ceil });
			phrsSpd.put(lastNote,spd);  // phrase-reader tracks speed of player
			{ phrsDataGUIs[lastNote].index_(3).currentvalue_(1-(msg[1]*0.5+0.5)) }.defer
		},'/left/closed/posx');

		// posz depth (posz) controls density
		OSCdef(\leap_lclZ,{|msg,time,addr,recvPort|
			density.put(lastNote,[0.25,4].asSpec.map(msg[1]*0.5+0.5));
			{ phrsDataGUIs[lastNote].index_(1).currentvalue_(msg[1]*0.5+0.5) }.defer
		},'/left/closed/posz');
	}

	// pdefs -- each plays one of 12 phrases, using fixed synths, and choice from six sets that vary harmonics, durations, and duration-weights for playback

	defsInit {
		harms = Array.fill(12,{[1]});
		durs = Array.fill(12,{[0.4,0.6,0.8]});
		dweights = Array.fill(12,{[0.8,0.15,0.05]});
		freqFunc = {|pclass| tracker.freqs[pclass]
			*([0.0625,0.125,0.25,0.5,1.0,2.0,4.0,8.0,16.0][octs[pclass]])*harms[pclass]};
		defs=[
			Pdef(\dbau0,
				Pbind(
					\instrument,\granSin,
					\group,dbauGranSynths,\out,17,
					\freq,Pfunc({freqFunc.(0)}),
					\lfreq,Pfunc({exprand(0.1,20)}),
					\mod,Pfunc({rrand(modParam[0]*0.5,modParam[0])}),
					\dur,Pfunc({if(ostinato,{~dur0=(tempo[0]*4).reciprocal
					},{~dur0=(tracker.phrDurs[0]*tempo[0].reciprocal).max(0.025)})}),
					\gdur,Pfunc({((~dur0+rrand(0.2,0.8))*density[0]).max(0.025)}),
					\pan,Pfunc({rrand(-0.25,0.25)}),
					\amp,Pfunc({Array.rand(harms[0].size,0,tracker.amps[0]*velAmps[0]*noteVol[0]
						*(harms[0].size).reciprocal)*(density[4].reciprocal)
					}),
			)),

			Pdef(\dbau1,
				Pbind(
					\instrument,\granPinkRingz,
					\group,dbauGranSynths,\out,17,
					\freq,Pfunc{freqFunc.(1)},
					\lfreq,Pfunc({exprand(0.1,20)}),
					\mod,Pfunc({rrand(modParam[1]*0.5,modParam[1])}),
					\dur,Pfunc({if(ostinato,{~dur1=(tempo[1]*4).reciprocal
					},{~dur1=(tracker.phrDurs[1]*(tempo[1].reciprocal)).max(0.025)})}),
					\gdur,Pfunc({((~dur1+rrand(0.0,0.1))*density[1]*2).max(0.025)}),
					\pan,Pfunc({rrand(-0.25,0.25)}),
					\amp,Pfunc({Array.rand(harms[1].size,0,tracker.amps[1]*velAmps[1]*noteVol[1]
						*(harms[1].size).reciprocal)*(density[4].reciprocal)})
			)),

			Pdef(\dbau2,
				Pbind(
					\instrument,\granRingz,
					\group,dbauGranSynths,\out,17,
					\freq,Pfunc({freqFunc.(2)}),
					\lfreq,Pfunc({exprand(0.1,20)}),
					\mod,Pfunc({rrand(modParam[2]*0.5,modParam[2])}),
					\dur,Pfunc({if(ostinato,{~dur2=(tempo[2]*4).reciprocal
					},{~dur2=(tracker.phrDurs[2]*(tempo[2].reciprocal)).max(0.025)})}),
					\gdur,Pfunc({((~dur2+rrand(0.0,0.1))*density[2]*2).max(0.025)}),
					\pan,Pfunc({rrand(-0.25,0.25)}),
					\amp,Pfunc({Array.rand(harms[2].size,0,tracker.amps[2]*velAmps[2]*noteVol[2]
						*(harms[2].size).reciprocal)*(density[4].reciprocal)})
			)),

			Pdef(\dbau3,
				Pbind(
					\instrument,\granNse,
					\group,dbauGranSynths,\out,17,
					\freq,Pfunc{freqFunc.(3)},
					\lfreq,Pfunc({exprand(0.1,20)}),
					\mod,Pfunc({rrand(modParam[3]*0.5,modParam[3])}),
					\dur,Pfunc({if(ostinato,{~dur3=(tempo[3]*4).reciprocal
					},{~dur3=(tracker.phrDurs[3]*(tempo[3].reciprocal)).max(0.025)})}),
					\gdur,Pfunc({((~dur3+rrand(0,0.25))*density[3]).max(0.025)}),
					\pan,Pfunc({rrand(-0.25,0.25)}),
					\amp,Pfunc({Array.rand(harms[3].size,0,(tracker.amps[3]*velAmps[3])*noteVol[3]
						*(harms[3].size).reciprocal)*(density[3].reciprocal)}),
			)),

			Pdef(\dbau4,
				Pbind(
					\instrument,\granFM,
					\group,dbauGranSynths,\out,17,
					\freq,Pfunc({freqFunc.(4)}),
					\rtio,Pfunc({harms[4].last}),
					\idx,Pfunc({((modParam[4]*8).round(1))+1}),
					\lfreq,Pfunc({exprand(0.1,20)}),
					\mod,Pfunc({modParam[4]*0.1}),
					\dur,Pfunc({if(ostinato,{~dur4=(tempo[4]*4).reciprocal
					},{~dur4=(tracker.phrDurs[4]*(tempo[4].reciprocal)).max(0.025)})}),
					\gdur,Pfunc(({~dur4*density[4]*4}).max(0.025)),
					\pan,Pfunc({rrand(-0.25,0.25)}),
					\amp,Pfunc({Array.rand(harms[4].size,0,tracker.amps[4]*velAmps[4]*noteVol[4]*0.25
						*(harms[4].size).reciprocal)*(density[4].reciprocal)
					}),
			)),

			Pdef(\dbau5,
				Pbind(
					\instrument,\granFmDrum,
					\group,dbauGranSynths,\out,17,
					\freq,Pfunc({freqFunc.(5)}),
					\rtio,Pfunc({harms[5].last}),
					\idx,Pfunc({((modParam[5]*4).round(1))+1}),
					\dur,Pfunc({if(ostinato,{~dur5=(tempo[5]*4).reciprocal
					},{~dur5=(tracker.phrDurs[5]*(tempo[5].reciprocal)).max(0.025)})}),
					\gdur,Pfunc({(~dur5+rrand(0.0,0.1)*density[5]*2).max(0.025)}),
					\pan,Pfunc({rrand(-0.25,0.25)}),
					\amp,Pfunc({Array.rand(harms[5].size,0,tracker.amps[5]*velAmps[5]*noteVol[5]
						*(harms[5].size).reciprocal) }),
			)),

			Pdef(\dbau6,
				Pbind(
					\instrument,\granEnvIdxFM,
					\group,dbauGranSynths,\out,17,
					\freq,Pfunc({freqFunc.(6)}),
					\rtio,Pfunc({harms[6].last}),
					\idx,Pfunc({((modParam[6]*4).round(1))+1}),
					\idxmin,Pfunc({rrand(0.0,0.1)}),
					\crv,Pfunc({rrand(-8,8)}),
					\dur,Pfunc({if(ostinato,{~dur6=(tempo[6]*4).reciprocal
					},{~dur6=(tracker.phrDurs[6]*(tempo[6].reciprocal)).max(0.025)})}),
					\gdur,Pfunc({((~dur6+rrand(0.0,0.1))*density[6]*2).max(0.025)}),
					\pan,Pfunc({rrand(-0.25,0.25)}),
					\amp,Pfunc({Array.rand(harms[6].size,0,tracker.amps[6]*velAmps[6]*0.125*noteVol[6]
						*(harms[6].size).reciprocal)*(density[6].reciprocal) }),
			)),

			Pdef(\dbau7,
				Pbind(
					\instrument,\granChime,
					\group,dbauGranSynths,\out,17,
					\freq,Pfunc({freqFunc.(7)}),
					\rtio,Pfunc({harms[7].last}),
					\lfreq,Pfunc({exprand(0.1,10)}),
					\mod,Pfunc({modParam[7]}),
					\dur,Pfunc({
						if(ostinato,{~dur7=((tempo[7]*4).reciprocal)
					},{~dur7=(tracker.phrDurs[7]*(tempo[7].reciprocal)).max(0.025)})}),
					\gdur,Pfunc({((~dur7+rrand(0.0,0.1))*density[7]).max(0.025)}),
					\pan,Pfunc({rrand(-0.25,0.25)}),
					\amp,Pfunc({Array.rand(harms[7].size,0,tracker.amps[7]*velAmps[7]*noteVol[7]
						*(harms[7].size.reciprocal)*(density[7].reciprocal)) })
			)),

			Pdef(\dbau8,
				Pbind(
					\instrument,\granPluck,
					\group,dbauGranSynths,\out,17,
					\freq,Pfunc{freqFunc.(8)},
					\coef,Pfunc({(rrand(0.25,0.75)*modParam[8]).min(0.75)}),
					\dur,Pfunc({if(ostinato,{~dur8=(tempo[8]*4).reciprocal
					},{~dur8=(tracker.phrDurs[8]*(tempo[8].reciprocal)).max(0.025)})}),
					\gdur,Pfunc({((~dur8+rrand(0.01,0.1))*density[8]*2).max(0.025)}),
					\pan,Pfunc({rrand(-0.25,0.25)}),
					\amp,Pfunc({Array.rand(harms[8].size,0,tracker.amps[8]*velAmps[8]*noteVol[8]
						*(harms[8].size).reciprocal)}),
			)),

			Pdef(\dbau9,
				Pbind(
					\instrument,\granClave,
					\group,dbauGranSynths,\out,17,
					\freq,Pfunc({freqFunc.(9)}),
					\rtio,Pfunc({harms[9].first}),
					\idx,Pfunc({4*modParam[9]+1}),
					\dur,Pfunc({if(ostinato,{~dur9=(tempo[9]*4).reciprocal
					},{~dur9=(tracker.phrDurs[9]*(tempo[9].reciprocal)).max(0.025)})}),
					\gdur,Pfunc({((~dur9+rrand(0.01,0.1))*density[9]*2).max(0.025)}),
					\pan,Pfunc({rrand(-0.25,0.25)}),
					\amp,Pfunc({Array.rand(harms[9].size,0,tracker.amps[9]*velAmps[9]*noteVol[9]
						*(harms[9].size).reciprocal)}),
			)),

			Pdef(\dbau10,
				Pbind(
					\instrument,\granSnare,
					\group,dbauGranSynths,\out,17,
					\freq,Pfunc({freqFunc.(10)}),
					\lfreq,Pfunc({exprand(0.001,10)}),
					\mod,Pfunc({modParam[10]}),
					\dur,Pfunc({if(ostinato,{~dur10=(tempo[10]*4).reciprocal
					},{~dur10=(tracker.phrDurs[10]*(tempo[10].reciprocal)).max(0.025)})}),
					\gdur,Pfunc({((~dur10+rrand(0.0,0.2))*density[10]).max(0.025)}),
					\pan,Pfunc({rrand(-0.25,0.25)}),
					\amp,Pfunc({Array.rand(harms[10].size,0,tracker.amps[10]*velAmps[10]*noteVol[10]
						*(harms[10].size).reciprocal*(density[10].reciprocal))}),
			)),

			Pdef(\dbau11,
				Pbind(
					\instrument,\granWhistle,
					\group,dbauGranSynths,\out,17,
					\freq,Pfunc({freqFunc.(11)}),
					\sinePhs,Pfunc({rrand(-pi,pi)}),
					\rq,Pfunc({rrand(0.001,0.05)}),
					\lfreq,Pfunc({exprand(2,100)}),
					\mod,Pfunc({modParam[11]}),
					\dur,Pfunc({if(ostinato,{~dur11=(tempo[11]*4).reciprocal
					},{~dur11=(tracker.phrDurs[11]*(tempo[11].reciprocal)).max(0.025)})}),
					\gdur,Pfunc({((~dur11+rrand(0.0,0.2))*density[11]).max(0.025)}),
					\pan,Pfunc({rrand(-0.25,0.25)}),
					\amp,Pfunc({Array.rand(harms[11].size,0,tracker.amps[11]*velAmps[11]*noteVol[11]
						*(harms[11].size).reciprocal)}),
			))
		];

		defs.do {|def| def.quant_(0) };
	}

	setsInit {
		psets = Array.fill(12,{0});
		sets = [
			{   psets.put(lastNote,0);
				harms = harms.put(lastNote,[1]);
				durs = durs.put(lastNote,[0.2]);
				dweights = dweights.put(lastNote,[1.0]) },

			{   psets.put(lastNote,1);
				harms = harms.put(lastNote,[1,2,3]);
				durs = durs.put(lastNote,[ 0.2,0.3]);
				dweights = dweights.put(lastNote,[0.8,0.2]) },

			{   psets.put(lastNote,2);
				harms = harms.put(lastNote,[2,3,5,7]);
				durs = durs.put(lastNote,[0.1,0.2,0.3]);
				dweights = dweights.put(lastNote,[0.6,0.25,0.15]) },

			{   psets.put(lastNote,3);
				harms = harms.put(lastNote,[3,5,7,11]);
				durs = durs.put(lastNote,[0.1,0.2,0.4]);
				dweights = dweights.put(lastNote,[0.33,0.44,0.33])},

			{   psets.put(lastNote,4);
				harms = harms.put(lastNote,[5,7,11,13]);
				durs = durs.put(lastNote,[0.2,0.4,0.8]);
				dweights = dweights.put(lastNote,[0.8,0.15,0.05]) },

			{   psets.put(lastNote,5);
				harms = harms.put(lastNote,[1,2,3,5,7,11,13]);
				durs = durs.put(lastNote,[0.2,0.4,0.6,0.8]);
				dweights = dweights.put(lastNote,[0.4,0.3,0.2,0.1]) }
		];
	}

}

/*

a = DbauPlayer.new.startPlayer
a.lastNote

*/