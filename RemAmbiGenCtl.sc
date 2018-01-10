RemAmbiGenCtl {
	var <>server,<name,<numChans,<>ambDims, <>gens, <>client, <>genSpace, 
			<>spatRout, <>window, <>az, <>el, <>rho, <>vol;
	
	*new { arg server,name,numChans,ambDims,gens,client;
		^super.newCopyArgs(server,name,numChans,ambDims,gens,client).initCtl
	}
	
	initCtl {
		"...initializing....".postln;
		window = GUI.window.new("AmbiGenCtl",Rect(128,64,440,360));
		this.makeControls;
		window.onClose_({ this.clear });
		window.front;
		
		client = client ?? { Client(\default,NetAddr("127.0.0.1",57120)) };
		client.password_(\pass);
		
	}
	
	startGenSpace {
		var msg;
		msg = name++".genSpace = GenericSpace.push(s);
			~n_cbout = NodeProxy.audio(s,"++numChans++");
		~n_cbout.play;
		~f_genFuncs = IdentityDictionary.new;		// a 'Maybe' object, will hold current genFuncs
		~n_cbgen = NodeProxy.audio(s,1);		// create single chan output proxy before using
		~n_cbaz =0;  ~n_cbel = 0; ~n_cbrho = 1;
		~n_cbout = { 
			var w, x, y, z;
			#w, x, y, z = BFEncode1.ar(~n_cbgen.ar,~n_cbaz.kr, ~n_cbel.kr, ~n_cbrho.kr); 
			BFDecode1.ar(w, x, y, z, 
				"++ambDims[0].asString++","++ambDims[1].asString++"
			)};
		"++name++".genSpace.pop;";
		client.send(msg); client.interpret(msg)
	}
	
	makeControls {
		var sel;
		window.view.decorator = FlowLayout(window.view.bounds);
		window.view.background = Color.rand;
		GUI.ezSlider.new(window,400@18,"Vol",Spec.specs[\amp],
			{|ez| this.sendClient(
				name++".genSpace.push(s); 
				~n_cbgen.set('vol',"++ez.value.asString++"); "++name++".genSpace.pop;")
			},0.1);
		window.view.decorator.nextLine;
		GUI.ezSlider.new(window,400@18,"Az",ControlSpec(0,2pi,\lin,0,0.5pi),
			{|ez| genSpace.push(server); ~n_cbaz = ez.value; genSpace.pop; },0.5pi);
		window.view.decorator.nextLine;
		GUI.ezSlider.new(window,400@18,"Elev",ControlSpec(-0.5pi,0.5pi,\lin,0,0),
			{|ez| genSpace.push(server); ~n_cbel = ez.value; genSpace.pop; },0.5pi);
		window.view.decorator.nextLine;
		GUI.ezSlider.new(window,400@18,"Dist",ControlSpec(0.0,1.0,\lin,0,1.0),
			{|ez| genSpace.push(server); ~n_cbrho = ez.value; genSpace.pop; },0.5pi);
		window.view.decorator.nextLine;
		window.view.decorator.nextLine;
		GUI.ezSlider.new(window,400@18,"frq",Spec.specs[\frq],
			{|ez| genSpace.push(server); ~n_cbgen.set(\frq,ez.value); genSpace.pop; },400);
		window.view.decorator.nextLine;
		GUI.ezSlider.new(window,400@18,"lofrq",Spec.specs[\lofreq],
			{|ez| genSpace.push(server); ~n_cbgen.set(\lfrq,ez.value); genSpace.pop; },8);
		window.view.decorator.nextLine;
		GUI.ezSlider.new(window,400@18,"mod",nil,
			{|ez| genSpace.push(server); ~n_cbgen.set(\mod,ez.value); genSpace.pop; },0.2);
		window.view.decorator.nextLine;
		GUI.ezSlider.new(window,400@18,"index",ControlSpec(0,2pi,\linear),
			{|ez| genSpace.push(server); ~n_cbgen.set(\idx,ez.value); genSpace.pop; },1.0);
		window.view.decorator.nextLine;
		GUI.ezSlider.new(window,400@18,"rtio",ControlSpec(0,1.0,\linear),
			{|ez| genSpace.push(server); ~n_cbgen.set(\rtio,ez.value); genSpace.pop; },0.1);
		window.view.decorator.nextLine;window.view.decorator.nextLine;
		GUI.staticText.new(window,Rect(10,10,80,20)).string_("choose sound");
		sel = GUI.popUpMenu.new(window,Rect(10,10,200,20));
		sel.items_(["growl","speak"]); sel.background_(Color.white);
	}
	
	sendClient {|str|
		client.send(\rcv,"remote code: " ++ str);
		client.interpret(str)
	}
	
	playGen {
		var msg;
		msg = name++".genSpace.push(s);
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
		~n_cbdur = 5;
		"++name++".genSpace.pop;";
		client.send(msg); client.interpret(msg)
	}

	playRout {
		genSpace.push(server);
		spatRout = Tdef(\randMoves,{	// edit cbgen params for above sound func
			var dur; 
			loop({
				dur = 8.0.rand.max(0.1);
				~n_cbdur = dur;
				~n_cbgen.set(\frq,rrand(100,10000));
				~n_cbgen.set(\lfrq,20.rand); 
				~n_cbgen.set(\mod,1.0.rand); 
				~n_cbgen.set(\idx,rrand(0.1,10));
				~n_cblfo =  rrand(0.1,20); 
				~n_cbel = { Line.kr((pi.rand-(pi*0.5)),(pi.rand-(pi*0.5)),~n_cbdur) }; 
				~n_cbazimuth = { Line.kr((2pi.rand-pi), (2pi.rand-pi), ~n_cbdur) };
				dur.wait;
			})
		});
		spatRout.play;
		genSpace.pop;
	}
	
	clear {
		genSpace.pop;
		genSpace.clear;
	}

}