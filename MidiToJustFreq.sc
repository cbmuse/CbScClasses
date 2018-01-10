
MidiToJustFreq {
	var <>tonic, <>scale, <>pitch, octBounds;
	*new { arg tonic=60;
		^super.newCopyArgs(tonic).initScale.setOctaves
	}
	
	initScale {
		//	scale = [1,16/15,9/8,6/5,5/4,4/3,11/8,3/2,8/5,5/3,7/4,15/8];
		scale = [1,16/15,9/8,6/5,5/4,4/3,45/32,3/2,8/5,5/3,9/5,15/8];

	}
	
	setOctaves {
		octBounds = [1,2,3,4,5,6,7,8].collect({|num,i| (num*12+(tonic%12)).midicps });
		octBounds.postln;
	}
	
	toJust { arg pitch;
		var oct,tfreq,pclass;
		oct = octBounds.indexInBetween(pitch.midicps).trunc;
		tfreq = octBounds[oct];
		pclass = ((pitch%12) - (tonic%12));
		if(pclass.isNegative,{ pclass = pclass+12 });
		// " ".postln; "oct = ".post; oct.postln; "pclass ".post; pclass.postln;
		// "freq = ".post;
		 ^(tfreq*(scale[pclass])); 
	}
	
	changeTonic { arg newTonic;
		tonic = newTonic;
		this.setOctaves
	}
	
	changeScale { arg newScale;
		scale = newScale;
	}
}