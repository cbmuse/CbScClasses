AmbiGenCtl {
	classvar <numRoutines=0;
	var >s, <>ambDims, <>gens, <>genSpace, 
			<>spatRout, <>window, <>az, <>el, <>rho, <>vol,
			volCtl,azCtl,elCtl,distCtl,frqCtl,loFrqCtl,modCtl,
			idxCtl,rtioCtl, 
			azLo=0,azHi=2pi,elLo=0,elHi=pi,
			rhoLo=0,rhoHi=1.0,durLo=0.1,durHi=1.0,
			frqLo=20.0,frqHi=2000.0,loFrqLo=0.05,loFrqHi=100.0,
			modLo=0,modHi=1.0,idxLo=0.1,idxHi=2pi,
			rtioLo=0.1,rtioHi=20.0;
	
	*new { arg s,ambDims,gens;
		^super.newCopyArgs(s, ambDims,gens).initCtl
	}
	
	initCtl { 
		"...initializing....".postln;
		
		genSpace = GenericSpace.push(s);
		~n_cbout = NodeProxy.audio(s,6);		// create multichan output proxy before using
		~n_cbout.play;
		~f_genFuncs = IdentityDictionary.new;		// a 'Maybe' object, will hold current genFuncs
		~n_cbgen = NodeProxy.audio(s,1);		// create single chan output proxy before using
		~n_cbaz =0;  ~n_cbel = 0; ~n_cbrho = 1;
		~n_cbdur = 4; ~n_cbazSpd = 2;
		~n_cbout = { 
			var w, x, y, z;
			#w, x, y, z = BFEncode1.ar(~n_cbgen.ar,~n_cbaz.kr, ~n_cbel.kr, ~n_cbrho.kr); 
			BFDecode1.ar(w, x, y, z, 
				ambDims[0],ambDims[1]
			)};
			
		gens ?? { this.defaultGens };
		window = GUI.window.new("AmbiGenCtl",Rect(128,64,500,500));
		this.makeControls;
		window.onClose_({ this.clear });
		window.front;
		genSpace.pop;
	}
	
	defaultGens {
		
		gens = IdentityDictionary.new;
		
		gens.add(\motoRev -> { arg lfrq=15,fmul=1,mod=0.1,frq=200,clip=0.1,vol=1;
			var env,x,attRelTime=0.05;
			env = EnvGen.kr(Env.asr(attackTime: attRelTime/2, releaseTime: attRelTime/2));
			x = RLPF.ar(LFPulse.ar(
					SinOsc.kr(~n_cblfo.kr,0,fmul,lfrq) // freq
						,0,mod), 	// iphase, width
					frq,0.1).clip2(clip);  // filt freq, phase, clip
			x*env*vol
		});
		
		gens.add(\fmDrum->
		{ arg frq=400,rtio=0.5,idx=5,mod=0.1,lfrq=15,vol=1;
			var env,sig,attRelTime=0.05,modSig;
			env = EnvGen.kr(Env.asr(attackTime: attRelTime/2, releaseTime: attRelTime/2));
			modSig = SinOsc.kr(~n_cblfo,0,mod*frq*0.5);
			sig = Decay2.ar(Impulse.ar(lfrq),0.05,0.1,
				PMOsc.ar(frq+modSig,frq*rtio,idx)
			);
			sig*env*vol
		});
		
		gens.add(\pmTone->
		{ arg frq=400,rtio=0.5,idx=5,mod=0.1,lfrq=15,vol=1;
			var sig, modSig, env, modFreq;
			env = EnvGen.kr(Env.asr(0.05, 1, 0.2));
			modFreq = frq*rtio;
			sig = PMOsc.ar(frq+SinOsc.kr(lfrq,0,mod*frq*0.1),modFreq,idx,0,0.1);
			sig*env*vol
		});
		
		gens.add(\klank->
		{ arg frq=400,rtio=0.5,idx=5,mod=0.1,lfrq=15,vol=1;
			var sig, modSig, env, modFreq;
			DynKlank.ar(`[[frq*mod, frq, frq*idx, frq*rtio*4], nil, [0.1, 0.1, 0.1, 0.1]],  
			Decay.ar(Impulse.ar(lfrq),0.03, ClipNoise.ar(0.01)))*vol  
		});
		
		gens.add(\nseGran ->
		{ arg frq,mod,lfrq,rtio,rq=0.1,vol=1;
			var sig,env,trig,dur,frqmod;
			env = EnvGen.kr(Env.asr(0.1,1.0,0.2));
			dur = (lfrq.reciprocal);
			frqmod=((frq*mod*0.5)-(frq*mod))*2;
			sig = Mix.fill(8,{|i|
				RLPF.ar(WhiteNoise.ar(EnvGen.ar(Env.sine(dur*rtio,rtio.reciprocal*0.25),
						gate: Impulse.kr(lfrq*0.125,i*0.125))),
					LFNoise0.kr(lfrq,frqmod,frq),rq)
			});
			Compander.ar(sig,sig,0.25,1,0.5,0.01,0.01)*env*vol;
		});
		
		gens.add(\lofmGrn ->
		{ arg frq,mod,lfrq,rtio,vol;
			var sig,env,trig,dur,frqmod,mmod,mfrq2;
			mfrq2 = Rand(0,20);
			mmod = Rand(0.0,1.0);
			env = EnvGen.kr(Env.asr(0.05,1.0, 0.05));
			dur = (lfrq.reciprocal);
			mmod=((mfrq2*mmod*0.5)-(mfrq2*mmod))*2;
			frqmod=((frq*mod*0.5)-(frq*mod))*2;
			sig = Mix.fill(8,{|i|
				PMOsc.ar(LFNoise2.kr(lfrq,frqmod,frq),
					LFNoise2.kr(lfrq,mmod,mfrq2),
					LFNoise2.kr(lfrq*0.1,20.0,0.1),0,
					EnvGen.ar(Env.sine(dur*rtio,(rtio.max(1.0).dbamp.reciprocal)*0.5),
						gate: Impulse.kr(lfrq*0.125,i*0.125)))});
			sig*env*vol;
		});
		
		gens.add(\sinGran ->
		{ arg frq,mod,lfrq,rtio,rq=0.1,vol=1;
			var sig,env,trig,dur,frqmod;
			env = EnvGen.kr(Env.asr(0.1,1.0,0.2));
			dur = (lfrq.reciprocal);
			frqmod=((frq*mod*0.5)-(frq*mod))*2;
			sig = Mix.fill(8,{|i|
				SinOsc.ar(LFNoise2.kr(lfrq,frqmod,frq),0,
				EnvGen.ar(Env.sine(dur*rtio,(rtio.max(1.0).dbamp.reciprocal)*0.5),
						gate: Impulse.kr(lfrq*0.125,i*0.125)))
			});
			Compander.ar(sig,sig,0.25,1,0.5,0.01,0.01)*env*vol;
		});
		
		gens.add(\pluckedString ->
		{ arg frq,lfrq,brit=1000,mod,rtio,rq=0.1,vol=1;
			var exciter,modSig,sig,bright,mfrq,plDcy;
			mfrq=Rand(0,2.0);
			plDcy=lfrq.reciprocal*0.7;
			bright = brit+frq;
			exciter = PinkNoise.ar(Decay.kr(Impulse.kr(lfrq), 0.01));
			modSig = SinOsc.kr(mfrq,0.0,mod*(bright*0.5));
			sig = RLPF.ar(CombC.ar(exciter,0.1,frq.reciprocal,plDcy),
						bright+modSig,rq,
					EnvGen.kr(Env.asr(0.0025,1,0.05)))
					.clip2(rtio)*vol;
			sig*vol
		});
		
		gens.add(\silence ->{ SinOsc.ar(0) });

}
	makeControls {
		var sel;
		window.view.decorator = FlowLayout(window.view.bounds);
		window.view.background = Color.rand;
		volCtl = GUI.ezSlider.new(window,400@18,"Vol",Spec.specs[\amp],
			{|ez| genSpace.push(s); ~n_cbgen.set(\vol,ez.value); genSpace.pop; },0.1,true);
		window.view.decorator.nextLine;
		azCtl = GUI.ezSlider.new(window,400@18,"Az",ControlSpec(-pi,pi,\lin,0,0),
			{|ez| genSpace.push(s); ~n_cbaz = ez.value; genSpace.pop; },0,true);
		window.view.decorator.nextLine;
		elCtl = GUI.ezSlider.new(window,400@18,"Elev",ControlSpec(-0.5pi,0.5pi,\lin,0,0),
			{|ez| genSpace.push(s); ~n_cbel = ez.value; genSpace.pop; },0,true);
		window.view.decorator.nextLine;
		distCtl = GUI.ezSlider.new(window,400@18,"Dist",ControlSpec(0.0,1.0,\lin,0,1.0),
			{|ez| genSpace.push(s); ~n_cbrho = ez.value; genSpace.pop; },1.0,true);
		window.view.decorator.nextLine;
		window.view.decorator.nextLine;
		frqCtl = GUI.ezSlider.new(window,400@18,"frq",Spec.specs[\frq],
			{|ez| genSpace.push(s); ~n_cbgen.set(\frq,ez.value); genSpace.pop; },400,true);
		window.view.decorator.nextLine;
		loFrqCtl = GUI.ezSlider.new(window,400@18,"lofrq",Spec.specs[\lofreq],
			{|ez| genSpace.push(s); ~n_cbgen.set(\lfrq,ez.value); genSpace.pop; },8,true);
		window.view.decorator.nextLine;
		modCtl = GUI.ezSlider.new(window,400@18,"mod",nil,
			{|ez| genSpace.push(s); ~n_cbgen.set(\mod,ez.value); genSpace.pop; },0.2,true);
		window.view.decorator.nextLine;
		idxCtl = GUI.ezSlider.new(window,400@18,"index",ControlSpec(0,2pi,\linear),
			{|ez| genSpace.push(s); ~n_cbgen.set(\idx,ez.value); genSpace.pop; },1.0,true);
		window.view.decorator.nextLine;
		rtioCtl = GUI.ezSlider.new(window,400@18,"ratio",ControlSpec(0,20.0,\linear),
			{|ez| genSpace.push(s); ~n_cbgen.set(\rtio,ez.value); genSpace.pop; },0.1,true);
		window.view.decorator.nextLine;window.view.decorator.nextLine;
		GUI.staticText.new(window,Rect(10,10,80,20)).string_("choose sound");
		sel = GUI.popUpMenu.new(window,Rect(10,10,200,20));
		sel.items_(gens.keys.asArray); sel.background_(Color.white);
		sel.action_({|view| genSpace.push(s); 
			~n_cbgen = gens[view.items[view.value].asSymbol]; genSpace.pop });
		GUI.button.new(window,Rect(10,10,80,20))
			.states_([["static",Color.black,Color.white],["changing",Color.red,Color.white]])
			.action_({|butt| if(butt.value == 1,{ this.playRout;  },{ spatRout.stop; })});
		GUI.staticText.new(window,Rect(10,10,80,20)).string_("auto on/off");
		window.view.decorator.nextLine;window.view.decorator.nextLine;
		GUI.staticText.new(window,Rect(10,10,80,20)).string_("azimuth bnds");
		GUI.rangeSlider.new(window,Rect(10,10,220,20)).lo_(0.4).range_(0.2)
			.action_({|sl| azLo = [-pi,pi,\lin].asSpec.map(sl.lo.value); 
				[-pi,pi,\lin].asSpec.map(sl.hi.value);});
		window.view.decorator.nextLine;window.view.decorator.nextLine;
		GUI.staticText.new(window,Rect(10,10,80,20)).string_("elev bnds");
		GUI.rangeSlider.new(window,Rect(10,10,220,20)).lo_(0.4).range_(0.2)
			.action_({|sl| elLo = [-0.5pi,0.5pi,\lin].asSpec.map(sl.lo.value); 
				elHi = [-0.5pi,0.5pi,\lin].asSpec.map(sl.hi.value) });
		window.view.decorator.nextLine;window.view.decorator.nextLine;
		GUI.staticText.new(window,Rect(10,10,80,20)).string_("dist bnds");
		GUI.rangeSlider.new(window,Rect(10,10,220,20)).lo_(0.4).range_(0.2)
			.action_({|sl| rhoLo = sl.lo.value; rhoHi = sl.hi.value });
		window.view.decorator.nextLine;window.view.decorator.nextLine;
		GUI.staticText.new(window,Rect(10,10,80,20)).string_("duration");
		GUI.rangeSlider.new(window,Rect(10,10,220,20)).lo_(0.4).range_(0.2)
			.action_({|sl| durLo = sl.lo.value; durHi = sl.hi.value });
		window.view.decorator.nextLine;window.view.decorator.nextLine;
		GUI.staticText.new(window,Rect(10,10,80,20)).string_("frq bnds");
		GUI.rangeSlider.new(window,Rect(10,10,220,20)).lo_(0.4).range_(0.2)
			.action_({|sl| frqLo = Spec.specs[\freq].map(sl.lo.value); 
					frqHi = Spec.specs[\freq].map(sl.hi.value) });
		window.view.decorator.nextLine;window.view.decorator.nextLine;
		GUI.staticText.new(window,Rect(10,10,80,20)).string_("lofrq bnds");
		GUI.rangeSlider.new(window,Rect(10,10,220,20)).lo_(0.4).range_(0.2)
			.action_({|sl| loFrqLo = Spec.specs[\lofreq].map(sl.lo.value); 
					loFrqHi = Spec.specs[\lofreq].map(sl.hi.value) });
		window.view.decorator.nextLine;window.view.decorator.nextLine;
		GUI.staticText.new(window,Rect(10,10,80,20)).string_("mod bnds");
		GUI.rangeSlider.new(window,Rect(10,10,220,20)).lo_(0.4).range_(0.2)
			.action_({|sl| modLo = sl.lo.value; modHi = sl.hi.value });
		window.view.decorator.nextLine;window.view.decorator.nextLine;
		GUI.staticText.new(window,Rect(10,10,80,20)).string_("index bnds");
		GUI.rangeSlider.new(window,Rect(10,10,220,20)).lo_(0.4).range_(0.2)
			.action_({|sl| idxLo = Spec.specs[\idx].map(sl.lo.value); 
				idxHi = Spec.specs[\idx].map(sl.hi.value) });
		window.view.decorator.nextLine;window.view.decorator.nextLine;
		GUI.staticText.new(window,Rect(10,10,80,20)).string_("ratio bnds");
		GUI.rangeSlider.new(window,Rect(10,10,220,20)).lo_(0.4).range_(0.2)
			.action_({|sl| rtioLo = Spec.specs[\rtio].map(sl.lo.value); 
				rtioHi = Spec.specs[\rtio].map(sl.hi.value) });
	
	}
	
	playGen {
		genSpace.push(s);
		~n_cblfo=1.0; ~n_cbfrq=200;
		~n_cbgen= { arg lfrq=15,idx=0.1,mod=0.1,frq=200,rtio=0.1,vol=1;
			var env,x,attRelTime=0.05;
			env = EnvGen.kr(Env.asr(attackTime: attRelTime/2, releaseTime: attRelTime/2));
			x = RLPF.ar(LFPulse.ar(
			SinOsc.kr(~n_cblfo.kr,0,idx,lfrq) // freq
				,0,mod), 	// iphase, width
				frq,0.1).clip2(rtio);  // filt freq, phase, clip
			x*env*vol
		};
		genSpace.pop;
	}
	
	playRout { var name;
		numRoutines = numRoutines+1;
		name = ("randMoves"++numRoutines).asSymbol;
		spatRout = Tdef(name,{	// edit cbgen params for above sound func
			var dur=1,az,elev,dist,frq,lofrq,mod,idx,rtio; 
			loop({
				genSpace.push(s);
				dur = rrand(durLo,durHi)*8.0.max(0.1);
				frq = rrand(frqLo,frqHi); { frqCtl.value = frq; }.defer;
				~n_cbgen.set(\frq,frq); 
				lofrq = rrand(loFrqLo,loFrqHi); { loFrqCtl.value = lofrq; }.defer;
				~n_cbgen.set(\lfrq,lofrq); 
				mod = rrand(modLo,modHi); { modCtl.value = mod; }.defer;
				~n_cbgen.set(\mod,mod); 
				idx = rrand(idxLo,idxHi); { idxCtl.value = idx; }.defer;
				~n_cbgen.set(\idx,idx);
				rtio = rrand(rtioLo,rtioHi); { rtioCtl.value = rtio; }.defer;
				~n_cbgen.set(\rtio,rtio);
				~n_cblfo =  rrand(0.1,20); 
				~n_cbel = { EnvGen.kr(
						Env.new([0,0,el = rrand(elLo,elHi),
					rrand(elLo,elHi),0,0],releaseNode:4, loopNode: 1),
						doneAction: 2,timeScale: dur*(rrand(1,8.reciprocal)));
				};  
				//	{ elCtl.value = el }.defer;
				~n_cbaz = { EnvGen.kr(
						Env.new([0,0,az = rrand(azLo,azHi),
					rrand(azLo,azHi),0,0],releaseNode:4, loopNode: 1),
						doneAction: 2,timeScale: dur*(rrand(1,8.reciprocal)));
				};
				//	{ azCtl.value = az }.defer;
				genSpace.pop;
				dur.wait;
			})
		});
		spatRout.play;
	}
	
	clear {
		spatRout.stop;
		genSpace.pop;
		genSpace.clear;
	}

}