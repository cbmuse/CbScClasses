RitmosXfrm	{
	classvar <>basicFuncs;	// basic algorithms used in specific funcs
	classvar <>funcs;
	classvar <>voiceMutes;	// funcs need to keep track of which voices are playing
	classvar <kick, <snare, <hihat, <tom;
	// kick=F#2, snare = F5, hihat=Bb2, tom=D2
	*initClass	{
		kick=[42,51,49,55];
		snare=[77,39,37,36];
		hihat=[46,50,47,43];
		tom=[69,54,40,38];
		voiceMutes = ();
		basicFuncs = (

			folFrqSus: 	{| rhyth, vc |
				rhyth.do {|bt,i| // change freq to follow midinote input
					// vc.input is [velocity, note, swing, dur, phrasenum]
					var freq, dur, indur;
					if((bt > vc.voice.ctl.gateLev.value) && (vc.input[i][0] > 0),{
						if(vc.tuning.notNil,{ freq = vc.tuning.cps((vc.input[i][1]));
							// default to ET pitchclass within 2 octaves above c60
						},{ freq = ((vc.input[i][1])%24+60).midicps });
						dur = (rhyth.rotate((i+1).neg)
							.detectIndex {|it| it > vc.voice.ctl.gateLev.value }+1)
						*vc.clock.tempo.reciprocal;
						indur = vc.input[i][3]; if((indur.isNil) || (indur==0),{ indur = 0.99 });
						vc.voice.clv[\sus].valuePut(i,indur/dur);
						vc.voice.clv[\freq].valuePut(i,freq)
				})};
				rhyth
			},

			folAbsFrqSus: 	{| rhyth, vc |
				rhyth.do {|bt,i| // change freq to follow midinote input
					// vc.input is [velocity, note, swing, dur, phrasenum]
					var freq, dur, indur;
					if((bt > vc.voice.ctl.gateLev.value) && (vc.input[i][0] > 0),{
						if(vc.tuning.notNil,{ freq = vc.tuning.cps((vc.input[i][1]));
						},{ freq = ((vc.input[i][1])).midicps });
						dur = (rhyth.rotate((i+1).neg)
							.detectIndex {|it| it > vc.voice.ctl.gateLev.value }+1)
						*vc.clock.tempo.reciprocal;
						indur = vc.input[i][3]; if((indur.isNil) || (indur==0),{ indur = 0.99 });
						vc.voice.clv[\sus].valuePut(i,indur/dur);
						vc.voice.clv[\freq].valuePut(i,freq)
				})};
				rhyth
			},

			wchooseFrqSus: 	{| rhyth, vc |
				rhyth.do {|bt,i| // weighted random choice of freq from last phrase
					var freq, dur, indur;
					if((bt > vc.voice.ctl.gateLev.value) && (vc.input[i][0] > 0),{
						freq = (Array.series(12,0,1).wchoose(vc.listener.octaves(0))*12) +
						Array.series(12,0,1).wchoose(vc.listener.pitches(0));
						if(vc.tuning.notNil,{
							freq = vc.tuning.cps(freq);
						},{ freq = freq.midicps });
						dur = (rhyth.rotate((i+1).neg)
							.detectIndex {|it| it > vc.voice.ctl.gateLev.value }+1)
						*vc.clock.tempo.reciprocal;
						indur = vc.input[i][3]; if((indur.isNil) || (indur==0),{ indur = 0.99 });
						vc.voice.clv[\sus].valuePut(i,indur/dur);
						vc.voice.clv[\freq].valuePut(i,freq)
				})};
				rhyth
			},

			folPitchSus: 	{| rhyth, vc, lowFreq, hiFreq |
				rhyth.do {|bt,i| // follow pitchlass of midinote input, octave is based on vc#
					var freq, dur, indur;
					if((bt > vc.voice.ctl.gateLev.value) && (vc.input[i][0] > 0),{
						if(vc.tuning.notNil,{ freq = vc.tuning.cps((vc.input[i][1]));
							// default to ET pitchclass within 2 octaves above c60
						},{ freq = ((vc.input[i][1])%24+60).midicps });
						if(freq < lowFreq,{ while({freq < lowFreq},{freq = freq*2 })
						},{
							if(freq > hiFreq,{ while({freq>hiFreq},{freq=freq/2}) })
						});
						dur = (rhyth.rotate((i+1).neg)
							.detectIndex {|it| it > vc.voice.ctl.gateLev.value }+1)
						*vc.clock.tempo.reciprocal;
						indur = vc.input[i][3]; if((indur.isNil) || (indur==0),{ indur = 0.99 });
						vc.voice.clv[\sus].valuePut(i,indur/dur);
						vc.voice.clv[\freq].valuePut(i,freq)
				})};
				rhyth
			},

			melToPhrs: { |mel, rhyth, vc|
				rhyth.do {|bt,i| // change freq to follow midinote input
					var freq, dur, indur;
					var freqs = mel.collect {|note| note[1] };
					if((bt > vc.voice.ctl.gateLev.value),{
						if(vc.tuning.notNil,{ freq = vc.tuning.cps(freqs[i]);
						},{ freq = ((freqs[i])).midicps });
						dur = (rhyth.rotate((i+1).neg)
							.detectIndex {|it| it > vc.voice.ctl.gateLev.value }+1)
						*vc.clock.tempo.reciprocal;
						indur = vc.input[i][3];
						if((indur.isNil) || (indur==0),{ indur = 0.99 });
						vc.voice.clv[\sus].valuePut(i,indur/dur);
						vc.voice.clv[\freq].valuePut(i,freq)
					})
				};
				rhyth = rhyth*vc.voice.ctl.weights.value[0]    // scale output by input weights
			},

			rhythToPhrs: { |mel, rhyth, vc|
				rhyth.do {|bt,i|
/*					var freq, dur, indur;
					if((bt > vc.voice.ctl.gateLev.value),{
/*						dur = (rhyth.rotate((i+1).neg)
							.detectIndex {|it| it > vc.voice.ctl.gateLev.value }+1)
						*vc.clock.tempo.reciprocal;*/
						indur = vc.input[i][3];
						if((indur.isNil) || (indur==0),{ indur = 0.99 });
						vc.voice.clv[\sus].valuePut(i,indur);
					})*/
				};
				rhyth = rhyth*vc.voice.ctl.weights.value[0]    // scale output by input weights
			},

			modPhrs:	{|vc| var durProcFunc, noteProcFunc;
				// randomly choose between 2 levels of expansion, contraction, or normal time
				durProcFunc = [{|v|v*(3.rand+1)},{|v|v/(3.rand+1)}].choose;
				// randomly choose between scrambling or randomly rotating pitches
				noteProcFunc = [{|n|n.scramble},{|n|n.rotate(n.size.rand)}].choose;
				vc.playPhrase.put(\phrsDurs,durProcFunc.(vc.playPhrase[\phrsDurs]));
				vc.playPhrase.put(\phrsDur,durProcFunc.(vc.playPhrase[\phrsDur]));
				vc.playPhrase.put(\phrsNotes,noteProcFunc.(vc.playPhrase[\phrsNotes]));
			},

			muteVoice:	{|rhyth,vc|
				//	vc.mute;
				Array.fill(rhyth.size,{0})		// return Array w no events
			}

		);

		funcs = (

			echo: {| rhyth, vc | rhyth },
			// input influence from notes: A4 F#3 E2 D2

			complement: {|rhyth, vc |
				var beats, lastBt;
				var thresh = vc.voice.ctl.gateLev.value;
				var idx = rhyth.detectIndex {|bt| bt>thresh };
				if( idx.notNil,{
					beats = rhyth.rotate(idx.neg).collect {|bt,i|
						if(bt>thresh,{ lastBt = bt; 0 },{ lastBt }) }
					.rotate(idx)
				},{ rhyth });	// if no beats, then silence,
			},

			scramble: {|rhyth, vc |
				rhyth.scramble
			},

			clmp4Scrmb: {|rhyth, vc |
				rhyth.clump((rhyth.size/4).floor).collect {|l| l.scramble }.flatten
			},

			pfolRate: {|rhyth, vc | rhyth.do {|bt,i| // change sample playback rate to follow midinote input
				if((bt > vc.voice.ctl.gateLev.value) && (vc.input[i][0] > 0),{
					vc.voice.clv[\rate].valuePut(i,((vc.input[i][1])
						%24+48).midicps*0.00383142)});
				rhyth }},

			pfolFreq: {|rhyth, vc | var freq, dur;
				rhyth.do {|bt,i| // change freq to follow midinote input
					if((bt > vc.voice.ctl.gateLev.value) && (vc.input[i][0] > 0),{
						if(vc.tuning.notNil,{ freq = vc.tuning.cps((vc.input[i][1]));
						},{ freq = ((vc.input[i][1])%24+60).midicps });
						vc.voice.clv[\freq].valuePut(i,freq)
				})};
				rhyth
			},

			pfOctFrq: {|rhyth, vc | var freq, dur;
				rhyth.do {|bt,i| // change freq to follow midinote input
					if((bt > vc.voice.ctl.gateLev.value) && (vc.input[i][0] > 0),{
						if(vc.tuning.notNil,{ freq = vc.tuning.cps((vc.input[i][1]));
						},{ freq = ((vc.input[i][1])%24+60).midicps });
						vc.voice.clv[\freq].valuePut(i,freq*2)
				})};
				rhyth
			},

			pfHiFrq: {|rhyth, vc | var freq, dur;
				rhyth.do {|bt,i| // change freq to follow midinote input
					if((bt > vc.voice.ctl.gateLev.value) && (vc.input[i][0] > 0),{
						vc.voice.clv[\freq].valuePut(i,((vc.input[i][1])%24+96).midicps)
				})};
				rhyth
			},

			rvsFolFrqSus: {|rhyth, vc |
				rhyth = RitmosXfrm.basicFuncs[\folFrqSus].value(rhyth,vc);
				rhyth.reverse;
			},

			folFrqSus: {|rhyth, vc |
				rhyth = RitmosXfrm.basicFuncs[\folFrqSus].value(rhyth,vc);
			},

			folAbsFrqSus: {|rhyth, vc |
				rhyth = RitmosXfrm.basicFuncs[\folFrqSus].value(rhyth,vc);
			},

			// play a phrase, chosen randomly, weighted to recent ones
			phrsFol: {|rhyth, vc | var mel;
				if( vc.playPhrase[\continue].not,{ // start playing a new Phrase
					if(((1.0.rand < vc.listener.activityLevels[1])
						&& (vc.listener.phrases.isEmpty.not)),{
						"activityLevels = ".post; vc.listener.activityLevels.postln;
						//	vc.unmute;
						// choose a new phrase
						vc.makePlayPhrase(0.1.exprand(vc.listener.phrases.size).asInt);
						vc.voiceName.post; " start playing phrase ".post;
						vc.playPhrase[\phrsNum].postln;
						mel = vc.getCycleFromPhrase;
						rhyth = mel.collect {|note| note[0] };
						rhyth = RitmosXfrm.basicFuncs[\melToPhrs].value(mel,rhyth,vc);
					},{
						"mute phrsFol".postln;
						rhyth = RitmosXfrm.basicFuncs[\muteVoice].(rhyth,vc)
					})
				},{
					/*						vc.voiceName.post; " continue playing phrase ".post;
					vc.playPhrase[\phrsNotes].postln; "".postln;*/
					mel = vc.getCycleFromPhrase;
					rhyth = mel.collect {|note| note[0] };
					rhyth = RitmosXfrm.basicFuncs[\melToPhrs].value(mel,rhyth,vc);
				});
				rhyth
			},

			// phrase to be played can be time-stretched or contracted, pitches rotated or scrambled
			phrsMod: {|rhyth, vc | var mel, freq, dur;
				if(vc.playPhrase[\continue].not,{  // start playing a new phrase
					if( (1.0.rand < vc.listener.activityLevels[1])
						&& (vc.listener.phrases.isEmpty.not),{
							"activityLevels = ".post; vc.listener.activityLevels.postln;
							"start phrsMod".postln;
							//	vc.unmute;
							// make a Phrase, then modify it
							vc.makePlayPhrase(0.5.exprand(vc.listener.phrases.size).asInt);
							RitmosXfrm.basicFuncs[\modPhrs].(vc);
							vc.voiceName.post; " phrsMod start playing phrase ".post;
							mel = vc.getCycleFromPhrase;
							rhyth = mel.collect {|note| note[0] };
							rhyth = RitmosXfrm.basicFuncs[\melToPhrs].value(mel,rhyth,vc);
						},{
							"mute phrsMod".postln;
							rhyth = RitmosXfrm.basicFuncs[\muteVoice].(rhyth,vc)
					})
				},{	// vc.voiceName.post; " continue playing phrase ".post; vc.playPhrase[\phrsNum].postln;
					mel = vc.getCycleFromPhrase;
					rhyth = mel.collect {|note| note[0] };
					rhyth = RitmosXfrm.basicFuncs[\melToPhrs].value(mel,rhyth,vc);
				});
				rhyth
			},

			// phrase to be played can be time-stretched or contracted, pitches rotated or scrambled
			phrsRhyth: {|rhyth, vc | var mel, freq, dur;
				if(vc.playPhrase[\continue].not,{
					if( (1.0.rand < vc.listener.activityLevels[1])
						&& (vc.listener.phrases.isEmpty.not),{
							"activityLevels = ".post; vc.listener.activityLevels.postln;
							// choose a new phrase
							vc.makePlayPhrase(0.1.exprand(vc.listener.phrases.size).asInt);
							vc.voiceName.post; " start playing phrase ".post;
							vc.playPhrase[\phrsNum].postln;
							mel = vc.getCycleFromPhrase;
							rhyth = mel.collect {|note| note[0] };
							rhyth = RitmosXfrm.basicFuncs[\rhythToPhrs].value(mel,rhyth,vc);
					})
				},{	// vc.voiceName.post; " continue playing phrase ".post; vc.playPhrase[\phrsNum].postln;
					mel = vc.getCycleFromPhrase;
					rhyth = mel.collect {|note| note[0] };
					rhyth = RitmosXfrm.basicFuncs[\rhythToPhrs].value(mel,rhyth,vc);
				});
				rhyth.postln
			}
	)}
}

/*

change the usage of vc.listener.activityLevels to vary responsiveness -- now it always inversely tracks input level -- it could randomly switch between direct and inverse tracking
use a variable to subsitute for this:  1.0.rand > vc.listener.activityLevels[1]

vc.listener.phrases - use a variable for 0.5 in this:  0.5.exprand(vc.listener.phrases.size).asInt
Synth(\default,[\freq,38.midicps])
~ritmos.voices[\vc0].input
[i][3];
*/

		