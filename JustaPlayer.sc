JustaPlayer {
	var <>name, <>synth, <>scale, <>durations, <>noteFunc, <>phraseLengths, <>vols, <>timbres, 
	<>fund=261.62556530114, <>ictus=0.125,  thisRoutine, thisSynth, phraseDone=true;
	
	*new	{ arg name, synth, scale, durations, noteFunc, phraseLengths, vols, timbres;
		^super.newCopyArgs(name, synth, scale, durations, noteFunc,
			phraseLengths, vols, timbres).initPlayer;
	}
	
	initPlayer {
		if( synth.isNil,{ 
			synth = SynthDef("tpulse", { arg out=0,freq=700,sawFreq=440.0, gate=1; 
				Out.ar(out, SyncSaw.ar(freq, sawFreq,0.1*EnvGen.kr(
					Env.asr, gate, doneAction:2))) 
			})
		});
		synth.send(Server.local);
		if( scale.isNil,{ 
			scale = [ [ [ 4,3], [1,1], [3,2], [9,8] ], [ 0.1, 0.25, 0.25, 0.4] ] });
		if( durations.isNil,{ durations = [ [ 4, 3, 2, 1],[0.3, 0.2, 0.25, 0.25 ] ] });
		if( phraseLengths.isNil,{ phraseLengths = [[3,8,12],[0.2,0.3,0.5]] });
		if( vols.isNil,{ vols = [[0.05,0.07,0.1],[0.2,0.7,0.1]] });
		if( timbres.isNil,{ timbres = [[1.0,3.0,6.0],[0.5,0.3,0.2]] });
		if( noteFunc.isNil,{ noteFunc = { Synth(synth.name,["freq",this.nextFreq]) }});
	}

	nextFreq { 
		var ratio;
		ratio = scale.at(0).wchoose(scale.at(1));
		^(fund*(ratio.at(0))/(ratio.at(1)))
	}
	
	nextDur {
		^(durations.at(0).wchoose(durations.at(1))*ictus)
	}
	
	nextVol {
		^vols.at(0).wchoose(vols.at(1))
	}
	
	nextTimbre {
		^timbres.at(0).wchoose(timbres.at(1))
	}
	
	play { 
		thisRoutine = Routine({ 
			loop({ 
				if( thisSynth.notNil,{ thisSynth.release });
					thisSynth = noteFunc.value;
					this.nextDur.wait;
				});
			}).play;
	}
	
	playPhrase {
		"starting phrase ".post; name.postln;
		this.stop;	// stop previous phrase, if playing
		thisRoutine = Routine({ 
			phraseDone = false;
			phraseLengths.at(0).wchoose(phraseLengths.at(1)).do({
			if( thisSynth.notNil,{ thisSynth.release });
					thisSynth = noteFunc.value;
					this.nextDur.wait;
			}); 
			name.post; " ending phrase".postln; 
			phraseDone = true;
			thisSynth.release(0.1); thisSynth = nil;
			nil.alwaysYield;
		}).play;
		
	}
	
	newPhrase { arg deltaTime=0, argScale, argDurations, argPhraseLengths, argVols, argTimbres, 			argFund, argIctus;
		SystemClock.sched(deltaTime,{ 
			if( argScale.notNil,{ this.scale_(argScale) });
			if( argDurations.notNil,{ this.durations_(argDurations) });
			if( argPhraseLengths.notNil,{ this.phraseLengths_(argPhraseLengths) });
			if( argVols.notNil,{ this.vols_(argVols) });
			if( argTimbres.notNil,{ this.timbres_(argTimbres) });
			if( argFund.notNil,{ this.fund_(argFund) });
			if( argIctus.notNil,{ this.ictus_(argIctus) });
			this.playPhrase 
		});
	}
	
	newPhrases { arg phrasesDur,deltaTime=0, argScale, argDurations, argPhraseLengths, argVols, 					argTimbres, argFund, argIctus, argMaxPause=4;
		var endTime;
		SystemClock.sched(deltaTime,{ 
			endTime = thisThread.seconds+phrasesDur;
			"starting phrase group ".post; name.postln; thisThread.seconds.postln; "".postln;
			if( argScale.notNil,{ this.scale_(argScale) });
			if( argDurations.notNil,{ this.durations_(argDurations) });
			if( argPhraseLengths.notNil,{ this.phraseLengths_(argPhraseLengths) });
			if( argVols.notNil,{ this.vols_(argVols) });
			if( argTimbres.notNil,{ this.timbres_(argTimbres) });
			if( argFund.notNil,{ this.fund_(argFund) });
			if( argIctus.notNil,{ this.ictus_(argIctus) });
			this.playPhrase;
			Routine({ 
				loop({ 
					if( phraseDone,{ phraseDone = false;
						this.newPhrase(this.nextDur*(argMaxPause.rand)) });
					if( thisThread.seconds > endTime,{ nil.alwaysYield });
					ictus.wait
				})
			}).play;
		});
	}

					
	stop { thisRoutine.stop; thisRoutine = nil; phraseDone = true;
		thisSynth.release(0.1); thisSynth = nil;
	}
					
}

/*

a = JustaPlayer.new;
a = JustaPlayer.new(scale: [ [ [8,5],[1,1],[6,5],[3,2],[9,5],[9,8] ], [0.2,0.1,0.2,0.1,0.2,0.2] ]);
a.play;
b = JustaPlayer.new(scale: [[[16,15],[4,3],[3,2],[8,5],[15,8],[1,1]],[0.2,0.1,0.2,0.1,0.2,0.2] ]);
b.play;
c = JustaPlayer.new(scale: [[[9,8],[6,5],[45,32],[3,2],[15,8],[1,1]],[0.2,0.1,0.2,0.1,0.2,0.2] ]);	c.play;

[a,b,c].do({ arg pl; pl.stop });

*/
