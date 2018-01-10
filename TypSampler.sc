
TypSampler {

 	// make generic gui Controls for loopSampler
var soundfiles, sampBufs, bufnum0, keyMap, vcMap, ctlBus, ctlBusValues, gateBus, numVoices = 8, numParams=6, paramLabels, paramSpecs, myWind, sliderView, ctlSliders, numLayoutView, numViews, curVoiceNum=0, curVoiceView, paramView, curVoiceUpdate,  keyTrgFunc, samplerFunc, <>keyQueue, thisSample, notesPlaying,p,l,d,s;


*new {|soundfiles|
	^super.newCopyArgs(soundfiles).initSampler 
}

initSampler {
// define a sampler
SynthDef("granSampler",{ arg ctlBus=0, gateBus=9, bufnum=0;
	var sampbuf, rate,startPos,endPos, loop=1, dir, loopFrq, thisRate, thisDur, env,gateEnv,vol;
	sampbuf = In.kr(ctlBus)-1+bufnum;
	SendTrig.kr(Impulse.kr(1),0,sampbuf);
	rate = In.kr(ctlBus+1);
	startPos = In.kr(ctlBus+2);
	endPos = In.kr(ctlBus+3);
	dir = (endPos-startPos).sign; // +1 or -1
	thisRate = BufRateScale.kr(sampbuf)*rate*dir;
	thisDur = ((endPos-startPos).abs*BufDur.kr(sampbuf));
	loopFrq = (thisDur.reciprocal).min(100);
	gateEnv = EnvGen.kr(	
				// an envelope w/ sustain
				Env.asr(attackTime: 0.02, releaseTime: 0.02),  				In.kr(gateBus), 	// gate
				doneAction: 2 
			); 
	env = Env.new([0,1,1,0],[0.0125,1.0-0.025,0.0125],\welch);
	vol = In.kr(ctlBus+5);
	Out.ar(0,Pan2.ar(
		PlayBuf.ar(1,			// numChannels
		sampbuf,				// which bufnum to play
		thisRate,   			// rate
		Impulse.kr(1.0/thisDur),		// Impulse Ugen retriggers sample
		startPos*BufFrames.kr(sampbuf),  // startPos
		loop)*EnvGen.ar(env,Impulse.kr(loopFrq),levelScale: vol,
					timeScale: thisDur.max(BufRateScale.kr(sampbuf)*0.01)), In.kr(ctlBus+4),vol*gateEnv
		// trigger an envelope on the sample "grain" in synch with loop
		
	))
}).load(Server.local);	
	s = Server.local;
	
if(soundfiles.isNil,{
	d = "";
	p = Pipe.new("ls sounds", "r");					// list directory contents in long format
	l = p.getLine;								// get the first line
	while({l.notNil}, {d = d ++ l ++ ",";  l = p.getLine; });	// post until l =nil
d = d.split($,);
d.removeAt(d.size-1);
p.close;										// close the pipe to avoid that nasty buildup
soundfiles = d;
});

sampBufs = List[];
soundfiles.do({ arg filename,i;
	var buf, file;
	filename = "sounds/" ++ filename;
	buf = Buffer.read(s,filename);
	sampBufs = sampBufs.add(buf);
	file = SoundFile.new; file.openRead(filename);
});
bufnum0 = sampBufs.at(0).bufnum;
keyMap = IdentityDictionary[
	$a -> 0, $s -> 1, $d -> 2, $f -> 3, $g -> 4, $h -> 5, $j -> 6, $k -> 7
];
keyQueue = List[];		// will contain records on notes playing
vcMap = IdentityDictionary[
	$! -> 1, $@ -> 2, $# -> 3, $$ -> 4, $% -> 5, $^ -> 6, $& -> 7, $* -> 8
];
ctlBus = 
	Array.fill(numVoices,{ Bus.control(s,numParams) });
ctlBusValues =  Array.fill(numVoices, 									{|i|[i+1,1,0,1,0,0.5].value});
s.performList(\sendMsg,\c_setn,ctlBus.at(0).index, 		numParams*numVoices, ctlBusValues.flatten);
gateBus = Bus.control(s,numVoices);
gateBus.index;
paramLabels = "smp  rate strt end  pan  vol";
paramSpecs = [
	ControlSpec(1,soundfiles.size,\lin,1),  // sample
	ControlSpec(0.125,8,\exp),nil,nil,
	ControlSpec(-1,1,\lin),			    // pan
	ControlSpec(0,1,\amp)			 // volume
];
myWind =  GUI.window.new("sampler",Rect(128, 64, 300, 440));
myWind.front;
sliderView = GUI.hLayoutView.new(myWind,Rect(10,0,450,300));
sliderView.setProperty(\spacing,10);
numLayoutView = GUI.hLayoutView.new(myWind,Rect(5,305,460,23));
numLayoutView.setProperty(\spacing,0);
numViews = Array.fill(numParams,{ arg i;
	var box;
	box = GUI.numberBox.new(numLayoutView, Rect(0,0,30,20));
	box.setProperty(\align,\center);
	box.font = Font("Arial", 9)	
});
paramView = GUI.staticText.new(myWind,Rect(10,335,500,20));
	paramView.string_(paramLabels);
	paramView.font = Font("Monaco", 9);
	paramView.stringColor = Color.blue;
	paramView.align = \left;
thisSample = GUI.staticText.new(myWind,Rect(20,390,400,20));
notesPlaying = 	GUI.staticText.new(myWind,Rect(20,360,200,20))
								.string_("playing: ");
curVoiceView = GUI.numberBox.new(myWind,Rect(210,10,30,30));
curVoiceView.setProperty(\align,\center); curVoiceView.font_(Font("Monaco", 12));
SCStaticText(myWind,Rect(210,40,50,20)).string_("curVoice");
ctlSliders = Array.fill(numParams,{ arg i;
	GUI.slider.new(sliderView,Rect(0,0,20,75))
		.action_({ arg item; 
			var value, spec;
			spec = paramSpecs.at(i);
			if( spec.notNil,{
				value = spec.map(item.value) },{ value = item.value });
			s.sendMsg ("/c_set", 	
		((ctlBus.at(curVoiceNum)).index+i),value);
				ctlBusValues.at(curVoiceNum).put(i,value);
			if( value < 1000,{ 
				numViews.at(i).value_(value.round(0.01)) 
			},{ numViews.at(i).value_(value.trunc(1.0)) });
			if( (i == 0),{ 	
				thisSample.string_(sampBufs.at(value.asInt-1).path); });
		})
		.keyDownAction_(keyTrgFunc );
});
curVoiceUpdate = { 
	ctlSliders.do({ arg item, i; 
		var val, spec;
		spec = paramSpecs.at(i); 
		if( spec.notNil,{
			val = spec.unmap(
				ctlBusValues.at(curVoiceNum).at(i)) },{ 
				val = ctlBusValues.at(curVoiceNum).at(i) });
		item.value_(val); item.action.value(item);
	});
}; 	
curVoiceView.action_({ arg item; 
		curVoiceNum = (item.value-1).asInt.clip(0, 												numVoices-1);
		curVoiceView.value_(curVoiceNum+1);
		curVoiceUpdate.value; 
	});
curVoiceView.valueAction_(1); 
keyTrgFunc = {  arg view, key, modifiers, unicode;
	var susNote, voiceNum;
//	" key = ".post; key.post; " mod = ".post; modifiers.post; 
//	" uni = ".post; unicode.postln;
	voiceNum = keyMap.at(key);
	if( voiceNum.notNil,{ 		// if this is a trigger key
		// then set curVoice thru curVoiceView
		curVoiceView.valueAction_(voiceNum.mod(numVoices)+1); 		susNote = keyQueue.detect({ arg item, i; 
				(item.at(0)) == key });
		if( susNote.isNil,{	  // if note is not already playing, then play it 
			keyQueue = keyQueue.add([key,voiceNum]);
			notesPlaying.string_(keyQueue.collect({ arg item; 									item.at(0) }).asString);
			samplerFunc.value(voiceNum);
		},{		// if the note is playing, then turn gate off
			s.sendMsg("/c_set", (gateBus.index+(susNote.at(1))),-1.1); 
			// purge note from queue
			keyQueue = keyQueue.flatten.removeAll(susNote).clump(2);  
			notesPlaying.string_(keyQueue.collect({ arg item; item.at(0) }).asString);
		});
	},{ if( vcMap.at(key).notNil,{  // number key?
				curVoiceNum = vcMap.at(key);	// # sets curVoiceNum
				curVoiceView.valueAction_(curVoiceNum);
	})});
};
myWind.view.keyDownAction_(keyTrgFunc);
samplerFunc = { arg voiceNum;
	var ctlIdx, gateIdx, vcCtlValues, sampNum;
	vcCtlValues = ctlBusValues.at(curVoiceNum);
	ctlIdx = (ctlBus.at(curVoiceNum)).index;
	gateIdx = (gateBus.index+curVoiceNum);
	//	"send new sampler note, buffer = ".post; 
	Synth.head(RootNode(s),"granSampler",[
	 	\ctlBus, ctlIdx,\gateBus, gateIdx,
	 	\bufnum, bufnum0 ]); 
	s.sendMsg("/c_set", gateIdx,1);    // gate on
};


myWind.onClose_({
	sampBufs.do {|buf| buf.free };
	ctlBus.do({ arg item; 
		item.do({ arg bus; bus.free })});
	gateBus.do({ arg bus; bus.free });
	Server.freeAll;
});

}

}

