
// this listener allows multiple phraseAnalyzers to operate on the same MIDIListener2 -- unused!!

MIDIListener2 {
	classvar <>listeners, <>idCount=0;
	var <>midiChan, <>bounds, susNotes, sost=false, showInput=true,<>play=false,
	<>noteOnResponder,<>noteOffResponder,<>ccResponder,
	<>startTime,<>history, <isRunning=false,<>pauseTime,
	<>pauseSum=0, <>pauseFlg=false, <>break=1.0, <>synthDef;

	*initClass { listeners = Array.fill(16,{[]}) }

	*new { arg midiChan=1, bounds=[21,108];
		var listenList, match;
		// if this is first on this channel, make a new one
		if( MIDIListener2.listeners[midiChan-1].isEmpty,{
			^super.newCopyArgs(midiChan,bounds).initHistory.start
		},{ 	// if midiChan and bounds are same, use previous
			listenList = MIDIListener2.listeners[midiChan-1];
			match = listenList.detectIndex {|list| (list.bounds == bounds) };
			if(match.notNil,{ ^MIDIListener2.listeners[midiChan-1][match] },{
				^super.newCopyArgs(midiChan,bounds).initHistory.start
			})
		})
	}

	initHistory {
		MIDIListener2.listeners.put(midiChan-1,MIDIListener2.listeners[midiChan-1].add(this));
		synthDef = SynthDef("sinTest",{|frq=400,amp=0.1,gate=1|
			var env;
			env=EnvGen.kr(Env.asr(0.05,1,0.1),gate,doneAction: 2);
			Out.ar([0,1],SinOsc.ar(frq,0,amp*env))
		}).send(Server.default);
		idCount=idCount+1;
		^this
	}

	start {
		noteOnResponder = NoteOnResponder({ arg src, chan, num, vel;
			if(bounds.isNil,{
				this.storeNoteOn(chan,num,vel)
			},{ if((num >= bounds[0]) && (num <= bounds[1]),{
					this.storeNoteOn(chan,num,vel)})
			})
		},nil,midiChan-1);
		noteOffResponder = NoteOffResponder({ arg src, chan, num, vel;
			if(bounds.isNil,{
				if( sost!=true,{ this.storeNoteOff(chan,num, vel) },{
						susNotes = susNotes.add([chan,num,vel])})
			},{ if((num >= bounds[0]) && (num <= bounds[1]),{
					if( sost!=true,{ this.storeNoteOff(chan,num, vel) },{
						susNotes = susNotes.add([chan,num,vel])})})
			})
		},nil,midiChan-1);
		susNotes = [];
		ccResponder = CCResponder({|src,chan,num,val|
			if(num == 64,{ if((val==127),{sost=true},{sost=false;
				susNotes.do({|item|this.storeNoteOff(item[0],item[1])});
				susNotes = []
			})})
		},nil,midiChan-1);
		startTime = thisThread.seconds;
		history = List.new(1000);
		history=history.add([0,0,0,0,0,-1,0,0,nil,nil,nil,nil]);  // dummy start record
		isRunning = true;
	}

	stop {
		var which;
		isRunning = false;
		noteOnResponder.remove;
		noteOffResponder.remove;
		ccResponder.remove;
		which = MIDIListener2.listeners[midiChan-1].detectIndex {|list| list.bounds == bounds };
		if( which.notNil,{
			MIDIListener2.listeners[midiChan-1].removeAt(which);
	//		if(window.notNil,{ window.close })
		})
	}

	reset {
		history = List.new(1000);
		history=history.add([0,0,0,0,0,-1,0,0,nil,nil,nil,nil]);  // dummy start record
		startTime = thisThread.seconds;
		pauseTime=startTime; pauseSum=0;
	}

	pause {
		pauseFlg = true;
		pauseTime = thisThread.seconds;
	}

	unpause {
		pauseFlg = false;
		pauseSum = pauseSum + (thisThread.seconds - pauseTime)
	}

	// get note parameters
	phraseNum {|idx| idx=idx.min(this.history.size-1); ^this.history[idx][0] }
	eventNum {|idx| idx=idx.min(this.history.size-1); ^this.history[idx][1] }
	noteNum {|idx| idx=idx.min(this.history.size-1); ^this.history[idx][2] }
	velNum {|idx| idx=idx.min(this.history.size-1); ^this.history[idx][3] }
	onTime {|idx| idx=idx.min(this.history.size-1); ^this.history[idx][4] }
	offTime {|idx| idx=idx.min(this.history.size-1); ^this.history[idx][5] }
	duration {|idx| idx=idx.min(this.history.size-1); ^this.history[idx][6] }
	speed {|idx| idx=idx.min(this.history.size-1); ^this.history[idx][7] }
	synth {|idx| idx=idx.min(this.history.size-1); ^this.history[idx][8] }
	eventDensity {|idx| idx=idx.min(this.history.size-1); ^this.history[idx][9] }
	eventSpacing {|idx| idx=idx.min(this.history.size-1); ^this.history[idx][10] }
	eventChord {|idx| idx=idx.min(this.history.size-1); ^this.history[idx][11] }

	// stuffs notes in history, evaluates if part of last or new phrase
	//	history items are:
	// phraseNum,eventNum,noteNum,velNum,onTime,offTime,duration,speed,synth,
	//	eventDensity,eventSpacing,evChord
	storeNoteOn { arg chan,num, vel;
		var thisSynth,curPhrase,lastOff,lastDur,evNum,prevEventIndex,chord,now;
		if((isRunning && pauseFlg.not),{
			if( showInput,{ num.post; " ".post; vel.postln; });
			if(play&&(synthDef.notNil),{
					thisSynth = Synth(synthDef.name.asSymbol,
							[\frq,num.midicps,\amp,(vel/127)*0.125])
			});
			now = thisThread.seconds-startTime;
			lastOff = history.detect({|note,i|
				(note[5] == 0) && (note[0] == this.phraseNum(0)) });
			if(((lastOff.notNil) || ((this.offTime(0)+break) > now)),{
				curPhrase = this.phraseNum(0); // part of previous phrase
			//	"part of current phrase ".post;
			},{
				curPhrase=this.phraseNum(0)+1;  // part of next phrase
				evNum = 1;		// start of 1st event in this phrase
			//	"1st event in new phrase ".post;
			});
			//	speed completed by next NoteOn
			lastDur= now-this.onTime(0);
			history[0].put(7,lastDur);  // store speed as time since prev note
			// is this a new event, or part of a chord?
			if(evNum.isNil,{ // if this is not the first event, then test if it's part of chord
				if((lastDur<(0.05)),{
					evNum=this.eventNum(0);
			//		"part of chord, ".post; evNum.post; " event in phrase".postln;
				},{ //	"start new chord ".post;
					evNum=this.eventNum(0)+1;
			//		evNum.post; " event in phrase".postln;
				});
			});
			// leave offtime and duration to be completed by storeNoteOff,
//			"new note at ".post; now.postln;
			history.addFirst([curPhrase,evNum,num,vel/127,now,0,0,0,thisSynth,nil,nil,nil]);
		})
	}

	storeNoteOff { arg chan,num, vel;
		var sameNote, thisOne,now;
		//	"noteOff = ".post; num.postln;
		now = thisThread.seconds;
		sameNote = history.detect({ arg list, i; thisOne = i;
			(list.at(2) == num) && (list[8] != "nil") });
		if( sameNote.notNil,{
			sameNote[8].release; // release synth
			sameNote.put(8,"nil"); // set synth to nil;
			sameNote = sameNote.put(5, now-(startTime)); // store offTime
			sameNote = sameNote.put(6,sameNote.at(5)-sameNote.at(4));   // compute, store duration
			history.put(thisOne,sameNote);
		})
	}
}

PhraseAnalyzer {
	var <>listener, <>phrases,<>show=false,<>history,
	<>showAnal=false,<>showPlay=false,<>showInput=false,<>lastRestDur=0,
	phraseDursSum,<pieceTime,<startTime,<activityLevels,monitorRout,
	phrAnalRout,<thisPhrase=1,<>phrNumView,<window,<>dispBtn,actLevView,<phrsPause=0;

	*new { arg listener;
		^super.newCopyArgs(listener).initPhrases
	}

	initPhrases {
		phrases = List.new(100);
		history = listener.history;   // this fails because MIDIListener2.history is an array of arrays
	}

	start {
		activityLevels = [0,0,0,0];	// [sinceLastPhr,cubeRootPhrSize,sqrRootPhrSize,allPhrases]
		monitorRout ?? { monitorRout.reset };
		phrAnalRout ?? { phrAnalRout.reset };
		startTime = thisThread.seconds;
		phrAnalRout = Routine({ loop({ this.analyzePhrases; listener.break.wait })}).play;
		this.monitorActivityLevels;
	}

	stop {
		phrAnalRout.stop;
		monitorRout.stop;
	}

	// get phrase parameters
	phraseStartTime {|idx=0| phrases.notEmpty.if({
		idx=idx.min(this.phrases.size-1); ^phrases[idx][0] },{ ^nil })}
	phraseDur {|idx=0| phrases.notEmpty.if({
		idx=idx.min(this.phrases.size-1); ^phrases[idx][1]},{ ^nil })}
	numNotes {|idx=0| phrases.notEmpty.if({
		idx=idx.min(this.phrases.size-1); ^phrases[idx][2]},{ ^nil })}
	pitches {|idx=0| phrases.notEmpty.if({	  // the next params are probabilities per pitch-class
		idx=idx.min(this.phrases.size-1); ^phrases[idx][3]},{ ^nil })}
	octaves {|idx=0| phrases.notEmpty.if({
		idx=idx.min(this.phrases.size-1); ^phrases[idx][4]},{ ^nil })}
	vels {|idx=0| phrases.notEmpty.if({
		idx=idx.min(this.phrases.size-1); ^phrases[idx][5]},{ ^nil })}
	durs {|idx=0| phrases.notEmpty.if({
		idx=idx.min(this.phrases.size-1); ^phrases[idx][6]},{ ^nil })}
	avgVel {|idx=0| phrases.notEmpty.if({	// the next 2 params are averages for all notes in phrase
		idx=idx.min(this.phrases.size-1); ^phrases[idx][7]},{ ^nil })}
	avgDur {|idx=0| phrases.notEmpty.if({
		idx=idx.min(this.phrases.size-1); ^phrases[idx][8]},{ ^nil })}
	avgEvDens {|idx=0| phrases.notEmpty.if({ // avg length per event in phrase
		idx=idx.min(this.phrases.size-1); ^phrases[idx][9]},{ ^nil })}
	numEvents {|idx=0| phrases.notEmpty.if({   // these params group chords into single events
		idx=idx.min(this.phrases.size-1); ^phrases[idx][10]},{ ^nil })}
	evRhythm {|idx=0| phrases.notEmpty.if({
		idx=idx.min(this.phrases.size-1); ^phrases[idx][11]},{ ^nil })}
	evMelody {|idx=0| phrases.notEmpty.if({
		idx=idx.min(this.phrases.size-1); ^phrases[idx][12]},{ ^nil })}
	evChord {|idx=0| phrases.notEmpty.if({
		idx=idx.min(this.phrases.size-1); ^phrases[idx][13]},{ ^nil })}
	phrasePauseSum {|idx=0| phrases.notEmpty.if({
		idx=idx.min(this.phrases.size-1); ^phrases[idx][14]},{ ^nil })}


	cubeRoot { |num| num = num.sqrt.sqrt; num = num*num.sqrt.sqrt;
		num = num*num.sqrt.sqrt.sqrt.sqrt;
		num = num*num.sqrt.sqrt.sqrt.sqrt.sqrt.sqrt.sqrt.sqrt;
		num = num.round(0.001); ^num
	}

		/* analyze new phrases, storing:
	[startTime, duration, pitchWeights, octaveWeights, avgVels/note, avgDur/note,
	avgVel/phrase, avgDur/phrase, numNotes,avgEvDens,numEvents,
	rhythm,melody,chordnotes, pauseSum ]
	*/

	analyzePhrases {
		var recent,hiBound,hiBoundPtr,lastAnalyzed,lastPhrase,phrase,numPhr,pitchWeights,
		octWeights,thisVel,vels,thisDur,durs,avgDur,avgVel,avgEvDens,analysis,now,
		rhythm,melody,melNotes,chdNotes,last,numEvents,evNumIdx,chord;
		if(this.history.isEmpty.not,{
			now = thisThread.seconds-listener.startTime;
			lastAnalyzed = phrases.size; // number of phrases analyzed before now
			hiBound = history.detectIndex({|note| note[0] == lastAnalyzed });
			if(hiBound.notNil,{
				hiBoundPtr = history.size-hiBound;  // save ptr from end of list
				lastPhrase = listener.phraseNum(0);  // phraseNum of last note recorded
				recent = history.copyRange(0,hiBound-1);  // work on copy, in case new events interrupt
		//		"analyze these number of phrases
				numPhr = (lastPhrase-lastAnalyzed);
		//		"number of phrases = ".post; numPhr.postln;
				numPhr.do({|i|  // do this for each new phrase
					var unFinished;
					phrase = recent.select({|note| note[0] == (lastPhrase-numPhr+1+i) });
		//		"phrase = ".postln; phrase.do({|note| note[0].postln; });
		//  	analyze phrase as long as it is NOT the most recent one AND hasn't finished yet
					unFinished = phrase.detect({|note,i| note[5] == 0 });
					if(((phrase[0][0] == lastPhrase) &&
						((phrase[0][5]+listener.break > now) || (unFinished.notNil))).not,{
						if(show.not,{
							"new phrase = ".post; phrase[0][0].post;
							" numEvents = ".post; numEvents = phrase[0][1].postln;
						});
						// parse the chords of the last phrase and store in history
						Array.fill(numEvents,{|k| numEvents-k}).do ({|j|
							evNumIdx = phrase.reverse.detectIndex({|note| note[1] == j });
							chord = phrase.select({|note| note[1] == j })
												.collect({|note|note[2]}).sort;
							history[history.size-hiBoundPtr-evNumIdx-1].put(9,chord.size);
							history[history.size-hiBoundPtr-evNumIdx-1].put(10,chord.last-chord.first);
							history[history.size-hiBoundPtr-evNumIdx-1].put(11,chord);
						});
				//		phrase.do ({|note| note.postln });

						pitchWeights = Array.new(12); octWeights = Array.new(12);
						vels = Array.new(12); durs = Array.new(12);
						12.do({ arg j;
							var notes, octs;
							notes = phrase.select({|note| (note[2]%12) == j });
							pitchWeights.add(notes.size);
							octs = phrase.select({|note| (note[2]/12).asInt == j });
							octWeights.add(octs.size);
							thisVel = notes.collect({|note| note[3] });
							if(thisVel[0].notNil,{ vels.add(thisVel.sum/notes.size)
								},{ vels.add(0) });
							thisDur = notes.collect({|note| note[6] }); // durations
							if(thisDur[0].notNil,{ durs.add(thisDur.sum/notes.size)
								},{ durs.add(0) });
						});
						pitchWeights = pitchWeights.collect({ arg item;
									item/pitchWeights.sum });
						octWeights = octWeights.collect({ arg item; item/octWeights.sum });
						avgVel = phrase.collect({|note| note[3] }).sum/(phrase.size);
						avgDur = phrase.collect({|note| note[6] }).sum/(phrase.size);
						numEvents = phrase[0][1];
						 // avgEvDens = phraseDur/numEvents
						avgEvDens = ((phrase.first[5]-phrase.last[4])/phrase.first[1]);
						phrase=phrase.reverse;
						rhythm = phrase.select({|note,i|
							if((phrase.size > 1) && (i != (phrase.size-1)),{
								 phrase[i][1] != (phrase[i+1][1])
								},{ false })
						});
						// rhythm is difference between ontimes of adjacent events
						rhythm=rhythm.collect({|note,i|
							if(i<(rhythm.size-1),{rhythm[i+1][4]- note[4]},{ note[7]})});
						// if phrase is one note, then rhythm is its duration
						rhythm.isEmpty.if({rhythm=[phrase[0][6]]});
						last=0;
						// find first note within event
						melNotes=phrase.select({|note,i|
							if(last != phrase[i][1],{ last = phrase[i][1]; true },{false})});
						// choose top note in chord
						melody=melNotes.collect({|note|
							if(note[11].notNil,{note[11].last},{note[2]}) });
						// chdNotes keeps numNotes, hi->lo note interval, notes
						chdNotes=melNotes.collect({|note,i|
							[note[9],note[10],note[11]]});
/* [startTime, duration, pitchWeights, octaveWeights, avgVels/note, avgDur/note, avgVel/phrase, avgDur/phrase, numNotes,avgEvDens,numEvents,rhythm,melody, chdNotes, pauseSum ] */
						analysis = [
							phrase.first[4], // phraseStartTime is onTime of first note
							phrase.last[5]-phrase.first[4], // phraseDur is diff of last offTime & 1st onTime
							phrase.size,	// number of notes in phrase
							pitchWeights, octWeights, vels, durs,
							avgVel, avgDur, avgEvDens,
							numEvents, rhythm, melody, chdNotes, listener.pauseSum ];
						phrases.addFirst(analysis); // last phrase, first in list
						if((show && showAnal),{
							{phrNumView.valueAction_(phrases.size)}.defer});
					})
				})
			})
		})
	}

	/* compute 4 levels of input event density:
	1) phrase-in-progress, 2) recent-phrases, 3) recent-section, 4) since-start
	*/
	monitorActivityLevels {
		var lastPhrEndTime,secStart,recentOnDurs, idx,recentAct;
		monitorRout = Routine({
			loop({
				phraseDursSum = phrases.sum({|phr| phr[1] });
				pieceTime = thisThread.seconds - startTime;
				if(listener.pauseFlg,{ pieceTime = pieceTime - (thisThread.seconds-listener.pauseTime)});
				// activityLev 3: onDur-of-all-phrases/duration since start
				activityLevels.put(3,phraseDursSum/pieceTime);
				if(phrases.size > 0,{
	// activityLevel 1: recent phrases section begins at phrase = cubeRoot of numphrases
	//  resulting in analysis of last 2 (2 total) , 3 (9 total), 4 (28 total), 5 (65),  or 6 (126) phrases
					secStart = this.cubeRoot(phrases.size).ceil.asInt;
					//  recentOnDurs = sum of onDur/duration of this section
					recentOnDurs = phrases.copyFromStart(secStart).sum({|phr| phr[1] });
					// actLev1 = ratio of recentOnDurs to recent pauses
					activityLevels.put(1,recentOnDurs/
						(pieceTime - (this.phraseStartTime(secStart)-this.phrasePauseSum(secStart))));
	// activityLevel 2: section is square of cubeRoot of phrases size of previous section
					secStart = secStart.squared;
					recentOnDurs = phrases.copyFromStart(secStart).sum({|phr| phr[1] });
					activityLevels.put(2,
						recentOnDurs/(pieceTime -
							(this.phraseStartTime(secStart)-this.phrasePauseSum(secStart))));
		//  activitylevel 0 is onDur proportion since the last recorded phrase (phrase in progress)
					lastPhrEndTime = this.phraseStartTime(0)+this.phraseDur(0);
					lastRestDur = thisThread.seconds - listener.startTime - lastPhrEndTime;
					idx = 0; recentOnDurs = 0;
					while({(listener.phraseNum(idx) > phrases.size) &&
						listener.duration(idx).notNil },{
						recentOnDurs = recentOnDurs + listener.duration(idx);
						idx = idx+1
					});
					recentAct = recentOnDurs/lastRestDur;
					if((recentAct == 0) && (activityLevels[0] == 0),{
						phrsPause = phrsPause+1 },{
							if(recentAct == 0,{ phrsPause = 0 })});
					activityLevels.put(0,recentAct);
					{ actLevView.value_(activityLevels.round(0.001).asString); }.defer;
				});
				listener.break.wait
			})
		}).play;
	}


	savePhrases { var f;
		File.saveDialog("savePhrases","",{ arg savePath;
			f = File(savePath,"w");
			f.write(phrases.asCompileString++"  // phrases "++"\n");
			f.close;
		},{ "cancelled".postln })
	}

	dialogLoadPhrases { |loadPath|
		var file;
		File.openDialog("load phrases",{ arg path;
			file = File.new(path,"r");
			phrases = file.readAllString.interpret;
			file.close;
			dispBtn.valueAction_(1); phrNumView.valueAction_(1);
		},{ "cancelled".postln })
	}

	loadPhrases {|loadPath|
		var file;
		loadPath.notNil.if({
			File.exists(loadPath).if({
				file = File.new(loadPath,"r");
				phrases = file.readAllString.interpret;
				file.close;
				{ dispBtn.valueAction_(1); phrNumView.valueAction_(1) }.defer;
			},{ "file not found".postln });
		})
	}

	view {
		var views, lo, bview;
		show=true; showAnal=true;
		if(window.isNil,{
			if(listener.bounds.notNil,{
				bview = " notes "++listener.bounds[0].asString ++ " "++ listener.bounds[1].asString
			},{ bview = "" });
			window = GUI.window.new("channel "++listener.midiChan.asString++bview,
				Rect(128+(MIDIListener2.idCount*15),64-(MIDIListener2.idCount*25),400,400));
			window.view.decorator = FlowLayout(window.view.bounds);
			window.view.background = Color.new(0.7,0.9,0.67);
			views = [];
			views = views.add(
				phrNumView = GUI.ezNumber.new(window,120@20,"phraseNum ",[1,400,\lin,1,1].asSpec,numberWidth: 30, labelWidth: 90));
			GUI.button.new(window,Rect(0,0,70,20))
				.states_([["load",Color.black,Color.white]])
				.action_({ this.dialogLoadPhrases });
			GUI.staticText.new(window,Rect(0,0,60,20)).string_("display ").align_(\right);
			dispBtn = GUI.button.new(window,Rect(0,0,100,20))
				.states_([["analyze",Color.red, Color.white],
				["play",Color.blue,Color.white],["static",Color.black,Color.white]])
				.action_({|vw| switch(vw.value,
					0,{ showAnal = true; showPlay = false; },
					1,{ showAnal = false; showPlay = true; },
					2,{ showAnal = false; showPlay = false; })});
			window.view.decorator.nextLine; window.view.decorator.nextLine;
			views = views.add(GUI.ezNumber.new(window,160@20,
				"phraseDur ",[0,400].asSpec,numberWidth: 70, labelWidth: 90));
			views = views.add(GUI.ezNumber.new(window,160@20,
				"numNotes ",[1,400].asSpec,numberWidth: 70, labelWidth: 90));
			window.view.decorator.nextLine;
			views = views.add(GUI.ezNumber.new(window,160@20,
				"avgVel ",numberWidth: 70, labelWidth: 90));
			views = views.add(GUI.ezNumber.new(window,160@20,
				"avgDur ",[0,400].asSpec,numberWidth: 70, labelWidth: 90));
			window.view.decorator.nextLine;
			views = views.add(GUI.ezNumber.new(window,160@20,
				"numEvents ",[1,400].asSpec,numberWidth: 70, labelWidth: 90));
			views = views.add(GUI.ezNumber.new(window,160@20,
				"avgEvDensity ",[0,40].asSpec,numberWidth: 70, labelWidth: 90));
			window.view.decorator.nextLine;
			GUI.staticText.new(window,Rect(0,0,80,25)).string_("   pitch_prob");
			views=views.add(GUI.multiSliderView.new(window,Rect(0,0,200,25))
				.readOnly_(true).valueThumbSize_(1).indexThumbSize_((200/12).floor));
			window.view.decorator.nextLine;
			GUI.staticText.new(window,Rect(0,0,80,25)).string_("    oct_prob");
			views=views.add(GUI.multiSliderView.new(window,Rect(0,0,200,25))
				.readOnly_(true).valueThumbSize_(1).indexThumbSize_((200/12).floor));
			window.view.decorator.nextLine;
			GUI.staticText.new(window,Rect(0,0,80,25)).string_("    vel_avg");
			views=views.add(GUI.multiSliderView.new(window,Rect(0,0,200,25))
				.readOnly_(true).valueThumbSize_(1).indexThumbSize_((200/12).floor));
			window.view.decorator.nextLine;
			GUI.staticText.new(window,Rect(0,0,80,25)).string_("    dur_avg");
			views=views.add(GUI.multiSliderView.new(window,Rect(0,0,200,25))
				.readOnly_(true).valueThumbSize_(1).indexThumbSize_((200/12).floor));
			window.view.decorator.nextLine;
			GUI.staticText.new(window,Rect(0,0,80,25)).string_("    evMelody");
			views = views.add(GUI.envelopeView.new(window,Rect(0,0,200,128))
				.thumbWidth_(3.0).thumbHeight_(1.0).drawLines_(true).drawRects_(true));
			window.view.decorator.nextLine;
			GUI.staticText.new(window,Rect(0,0,80,25)).string_("activityLevels");
			actLevView = GUI.textField.new(window,Rect(0,0,200,25))
				.string_("[0,0,0,0]").align_(\center);
			views[0].action_({|view|
				var rhythm,lastDur=0,melody;
				thisPhrase = phrases.size-(view.value.asInt.min(phrases.size));
				views[1].value_(this.phraseDur(thisPhrase));
				views[2].value_(this.numNotes(thisPhrase));
				views[3].value_(this.avgVel(thisPhrase));
				views[4].value_(this.avgDur(thisPhrase));
				views[5].value_(this.numEvents(thisPhrase));
				views[6].value_(this.avgEvDens(thisPhrase));
				views[7].reference_(this.pitches(thisPhrase));
				views[8].reference_(this.octaves(thisPhrase));
				views[9].reference_(this.vels(thisPhrase));
				views[10].reference_(this.durs(thisPhrase));
				rhythm = this.evRhythm(thisPhrase).collect({|dur| lastDur=lastDur+dur});
				melody = this.evMelody(thisPhrase).collect({|val|val/127});
				if(melody.size == 1,{melody=melody.add(melody[0])});
				views[11].value_([rhythm.addFirst(0)
					.collect({|dur| dur/this.phraseDur(thisPhrase)}),melody])
			});
			window.front;
			window.onClose_({ this.stop; views.do({|vw|vw.free});
				window=nil; MIDIListener2.idCount=MIDIListener2.idCount-1;  })
		});
	}
}

/*
m=MIDIListener2.new(1);
p=PhraseAnalyzer.new(m)
p.start.view
p.analyzePhrases


*/