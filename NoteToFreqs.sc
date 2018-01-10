
NoteToFreq {
	classvar <noteFreqArray, <notes;
	*initClass {
		noteFreqArray = Array.new(100);
		for(12,108,{|i| noteFreqArray.add(i.midicps.round(0.001)) });
		noteFreqArray = noteFreqArray.clump(12);
		notes = ["C","C#","D","D#","E","F","F#","G","G#","A","A#","B"]
	}
	
	*do {|string|
		var pclass, oct; 
		if( string.size > 2,{ 
			pclass = notes.detectIndex({|str| str == ((string[0]++string[1]).asString) });
			oct = string[2]
		},{
			pclass = notes.detectIndex({|str| str == string[0].asString });
			oct = string[1];
//			pclass.postln; oct.postln;
//			pclass.class.postln; oct.class.postln; " ".postln;
		});
		if( (pclass.notNil && oct.notNil),{
			^noteFreqArray[oct.digit][pclass]
		},{ 
			"note not valid !!".postln
		})
	}
		
}
		