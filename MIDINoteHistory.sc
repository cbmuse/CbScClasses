MIDINoteHistory {
	var <>midiChan, <>history, <>analysis, <>phrases, <>startTime, 
	currentTime, running=false, <>show=true,<>play=false,<>synthDef,
	noteOnResponder,noteOffResponder,ccResponder,sost=false,susNotes;
	
	*new { arg midiChan;
		^super.newCopyArgs(midiChan).initHistory
	}
	
	initHistory { 
		analysis = [0,0,0,0,0,0,0,0,0];
		synthDef= SynthDef("sinTest",{|frq=400,amp=0.1,gate=1|
			var env;
			env=EnvGen.kr(Env.asr(0.05,1,0.1),gate,doneAction: 2);
			Out.ar([0,1],SinOsc.ar(frq,0,amp*env))
		}).send(Server.default)
	}
	
	start { 
		noteOnResponder = NoteOnResponder({ arg src, chan, num, vel;
			this.storeNoteOn(chan,num,vel)
		},nil,midiChan-1);
		noteOffResponder = NoteOffResponder({ arg src, chan, num, vel; 
			if( sost!=true,{ this.storeNoteOff(chan,num, vel) },{ 
				susNotes = susNotes.add([chan,num,vel])})
		},nil,midiChan-1);
		susNotes = [];
		ccResponder = CCResponder({|src,chan,num,val|
			if(num == 64,{ if((val==127),{sost=true},{sost=false; 
				susNotes.do({|item|this.storeNoteOff(item[0],item[1])});
				susNotes = [] 
			})})
		},nil,midiChan-1);
		startTime = thisThread.seconds;
		history = List.new(0);
		running = true;
	}
	
	stop { 
		running = false;
		noteOnResponder.remove;
		noteOffResponder.remove;
		ccResponder.remove;
	}
	
	storeNoteOn { arg chan,num, vel;
		var thisSynth;
		if(running,{
			if( show,{ num.post; " ".post; vel.postln; });
			if(play,{
				if(synthDef.notNil,{ 
					thisSynth = Synth(synthDef.name.asSymbol,[\frq,num.midicps,\amp,(vel/127)*0.125]) });
			});
			//	history items are:  noteNum,velNum,onTime,offTime,duration,synth
			history.addFirst([num, vel/127, 
				thisThread.seconds-startTime, thisThread.seconds-startTime, 0,thisSynth]
			)
		})
	}
	
	storeNoteOff { arg chan,num, vel;
		var sameNote, which; 
		//	"noteOff = ".post; num.postln;
		sameNote = history.detect({ arg list, i; which = i; 
			(list.at(0) == num) && (list[5] != "nil") });
		if( sameNote.notNil,{ 
			sameNote[5].release; // release synth
			sameNote.put(5,"nil"); // set synth to nil;
			sameNote = sameNote.put(3,thisThread.seconds-(startTime));
			sameNote = sameNote.put(4,sameNote.at(3)-sameNote.at(2));   // compute, store duration
			history.put(which,sameNote);
		})
	}
	
	/* for given amount of recent time, compute avg density of events, pitchClass-weights,
		octave weights, avgDuration and avgVelocity (per pitchClass and overall), 
		and percentage of silence
	*/
	analyze { arg time;
		var recent, thisNote, pitchWeights, durs, vels, octWeights, avgDur, avgVel, avgSilence;
		recent = List.new(0); 
		history.detect({ arg item, i; 
			if(  (history.at(i).at(3)) >= (thisThread.seconds-startTime-time),{
				recent.add(history.at(i))
			});
			history.at(i).at(3) < (thisThread.seconds-startTime-time)
		});
		pitchWeights = Array.new(12); durs = Array.new(12); vels = Array.new(12);
		octWeights = Array.new(12);
		12.do({ arg i; 
			var notes, octs;
			notes = recent.select({ arg item; item.at(0).mod(12) == i });
			pitchWeights.add(notes.size);
			octs = recent.select({ arg item; item.at(0)/12 == i });
			octWeights.add(octs.size);
			thisNote = notes.collect({ arg item; item.at(1); });
			if(thisNote.at(0).notNil,{ vels.add(thisNote.sum/notes.size) },{ vels.add(0) });
			thisNote = notes.collect({ arg item; item.at(4); });
			if(thisNote.at(0).notNil,{ durs.add(thisNote.sum/notes.size) },{ durs.add(0) });
		});
		pitchWeights = pitchWeights.collect({ arg item; item/pitchWeights.sum });
		octWeights = octWeights.collect({ arg item; item/octWeights.sum });
		avgVel = recent.collect({ arg item; item.at(1) }).sum;
		avgDur = recent.collect({ arg item; item.at(4) }).sum;
		/* [time, pitchWeights, octaveWeights, 
			avg-vels/note, avg-dur/note, 
				avg-vel, avg-dur, density ] */
		analysis = [time, pitchWeights, octWeights, vels, durs, 
			avgVel/recent.size, avgDur/recent.size, avgDur/time, recent.size/time  ]
	}
	
	lastTime { ^analysis.at(0) }
	pitches { ^analysis.at(1) }
	octaves { ^analysis.at(2) }
	avgVels { ^analysis.at(3) }
	avgDurs { ^analysis.at(4) }
	avgVel { ^analysis.at(5) }
	avgDur { ^analysis.at(6) }
	onDensity { ^analysis.at(7) }
	evDensity { ^analysis.at(8) }
	
	parsePhrases { arg numPhrases=1, break=1;
		var ptr,start, end, length;
		phrases = List.new(numPhrases);
		ptr = 0;
		numPhrases.do({ arg i; 
			if( ptr < (history.size-2),{
				while({(((history.at(ptr).at(2) - history.at(ptr+1).at(3)) < break) 
						&& (ptr != (history.size-3)))},
					{ ptr = ptr+1 }); 
				end = ptr+1; ptr=ptr+1;
			},{ end = ptr });
			if( ptr < (history.size-2),{
				while({((history.at(ptr).at(2) - history.at(ptr+1).at(3) < break) 
						&& (ptr != (history.size-2)))},
					{ ptr = ptr+1 });  
				start = ptr; 
			},{  start = ptr });
			if( start != end,{
				length = (history.at(end).at(2)) - (history.at(start).at(2));
				if( length > break,{ phrases.add([start,end,length]) },{
					phrases.add(nil) })
			});
		});
		phrases.postln;
	}
}
	

/*
	MIDIIn.connect;
	b = MIDINoteHistory(1).start;
	b.analyze(20)
	each notefunction is the same -- it stores a record of the note in the list
	
	MIDIIn.connect;
	a = MIDINoteHistory.new(1).start;
	a.play=true;
	~hist.synth_("pmTone");

*/
