
SFGrain	{
	var <>lastval=0.0, >window, >slider,  
		>synth, >rout, <>sfilePath, <>buffer;
	var prop, timestart, timeend, s;

	*new {|lastval| ^super.new.init }
	
	init {
		this.lastval_(lastval); 
		s = Server.default;
		this.gui;
		this.loadSynthdef;
		this.loadBuffer; 
	}
	
	loadBuffer {|path|
		buffer = if(path.isNil,{ 
			Buffer.read(s,"sounds/a11wlk01.wav")
		},{ Buffer.read(s,sfilePath) });
	}
	
	loadSynthdef {
		SynthDef(\sfgrain,{arg bufnum=0, pan=0.0,
			 startPos=0.0, amp=0.1, dur=0.04; 
		var grain; 
		grain= PlayBuf.ar(1,bufnum, 
			BufRateScale.kr(bufnum), 1,
			BufFrames.ir(bufnum)*startPos,0)
			*(EnvGen.kr(Env.perc(0.01,dur),
				doneAction:2)-0.001);
		Out.ar(0,Pan2.ar(grain, pan))}).send(s)
	} 

	gui {
		window = Window("My Window", Rect(100,500,200,200)); 
		slider= Slider(window,Rect(10,10,150,40)); 
		slider.action_({lastval= slider.value });
		window.onClose_({ rout.stop });
		window.front
	}
		
	play	{
		rout = {
			inf.do{ arg i; 
				prop= (i%300)/300;
				timestart= prop*0.8;
				timeend= prop*(0.8+(0.1*lastval));

				synth = Synth(\sfgrain,
					[\bufnum, buffer.bufnum, 		
					\startPos,rrand(timestart,timeend),
					\amp, exprand(0.005,0.1), 
					\pan, lastval.rand2, 
					\dur, 0.1+(lastval*0.5)]); 
					 
		(((lastval*0.2)+0.01).max(0.01)).wait } }.fork;
	}
}
