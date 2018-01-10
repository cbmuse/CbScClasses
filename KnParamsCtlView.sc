// KnowNothing paramData in and out control and display

KnParamsCtlView {
	var network,<vcNum,parentView,networkSize,labels,<view,<netPlayer,<netPlayerSel,responders,local,
	<inFrq,<netFrq,<frqProb,<frqScale,<frqOffset,<frqBlend,outFrq,
	<inTimbr,<netTimbr,<timbrProb,<timbrScale,<timbrOffset,<timbrBlend,outTimbr,
	<inAmp,<netAmp,<ampProb,<ampScale,<ampOffset,<ampBlend,outAmp,
	<frqCtlDest,<timbrCtlDest,<ampCtlDest,<>ctlPlayerActions,
	<guiCtls,<>myPl,mySynthView;

	*new {|network,vcNum,parentView,networkSize|
		^super.newCopyArgs(network,vcNum,parentView,networkSize).init
	}

	init {
		local =  NetAddr("127.0.0.1", 57110); // local server
		labels = ["freq","timbre","amp"];
		view = FlowView.new(parentView,Rect(8,60,480,180),8@8,2@2,"knowNoParams");
		netPlayer=SV.new.sp((0..networkSize),0).items_(Array.fill(networkSize,{|i| (i+1).asString }));
		inFrq = CV(nil,0); frqProb= CV(nil,0); frqScale= CV(nil,0); frqOffset=CV(nil,0);
		outFrq=CV(nil,0); netFrq= CV(nil,0); frqBlend= CV(nil,0);
		inTimbr= CV(nil,0); timbrProb= CV(nil,0); timbrScale= CV(nil,0); timbrOffset= CV(nil,0);
		outTimbr=CV(nil,0); netTimbr= CV(nil,0); timbrBlend= CV(nil,0);
		inAmp= CV(nil,0); ampProb= CV(nil,0); ampScale= CV(nil,0); ampOffset= CV(nil,0);
		outAmp=CV(nil,0); netAmp= CV(nil,0); ampBlend= CV(nil,0);
		frqCtlDest=SV.new((1..8),1); timbrCtlDest=SV.new((1..8),2); ampCtlDest=SV.new((1..8),0);
		ctlPlayerActions=  // set these functions to control custom synths
			[{|this,frqval,frqDest| frqval.post; " to ".post; frqDest.postln },
			{|this,timbrval,timbrDest| timbrval.post; " to ".post; timbrDest.postln },
			{|this,ampval,ampDest| ampval.post; " to ".post; ampDest.postln }];
		myPl=CV([1,networkSize,\lin,1].asSpec,vcNum+1);
		myPl.action_({|cv| {mySynthView.string_(" my Synth is #" ++ cv.value.asString)}.defer });
		this.makeGuiCtls;
		this.connectToActions;
		this.loadAnalyzerResponders
	}

	loadAnalyzerResponders {
		responders = [
			OSCdef(("fbFreq"++(vcNum.asString)).asSymbol,{|msg,time,addr,port|
				var vc = msg[3];
				var val = msg[4]; msg.postln;
				if(vcNum==vc,{val = \freq.asSpec.unmap(val); inFrq.value_(val)})
			},'/freq',local).fix,
			OSCdef(("fbTimbre"++(vcNum.asString)).asSymbol,{|msg,time,addr,port|
				var vc = msg[3];
				var val = msg[4]; msg.postln;
				if(vcNum==vc,{val = \freq.asSpec.unmap(val); inTimbr.value_(val)})
			},'/specCentroid',local).fix,
			OSCdef(("fbAmp"++(vcNum.asString)).asSymbol, {|msg,time,addr,port|
				var vc = msg[3];
				var val = msg[4]; msg.postln;
			//	msg.postln; //  example: [ /freq, 1001, 1204, 440, 1 ]
				if(vcNum==vc,{val = \amp.asSpec.unmap(val); inAmp.value_(val) })
			},'/peak',local).fix
		];
	}

	makeGuiCtls {
		view.startRow;
		StaticText(view,65@20).string_("netPlayer ").align_(\left);
		netPlayerSel = PopUpMenu.new(view,50@15)
		.action_({|m| (m.value+1) }); netPlayer.connect(netPlayerSel);
		mySynthView = StaticText(view,100@20)
		.string_("  mySynth is #"++myPl.value).font_(Font("Monaco",10));
		view.startRow; StaticText.new (view,360@15)
		.string_("               input          probability    scale               out");
		guiCtls = 3.collect {|i|
			view.startRow;
			StaticText(view,60@15).string_(labels[i]);
			[Slider(view,72@15),   // data input
				Slider(view,72@15),    // filterweight
				Slider(view,72@15),   // scale
				Knob.new(view,25@20).value_(0.5), // offset
				NumberBox(view,Rect(0,0,50,15)),
				view.startRow;
				StaticText(view,60@12).string_("net-"++labels[i]).font_(Font("Monaco",10));
				Slider(view,72@12).knobColor_(Color.red),
				StaticText(view,40@12).string_("blend").font_(Font("Monaco",12));
				Knob.new(view,25@20).value_(0.5), // blend
				StaticText.new(view,70@15).visible_(false);
				StaticText.new(view,40@15).string_("ctl->");
				PopUpMenu.new(view,40@15).items_([1,2,3]).font_(Font("Times",10)).value_(i)
			]
		}
	}

	connectToActions {
		netPlayer.connect(netPlayerSel);
		inFrq.action_({|cv| if(frqProb.value.coin,{ // rand filter gate
			var val = (cv.value*(1-frqBlend.value))+(netFrq.value*frqBlend.value);
			outFrq.value_(((val*frqScale.value)+frqOffset.value).wrap(0,1.001))})});
		inTimbr.action_({|cv| if(timbrProb.value.coin,{ // rand filter gate
			var val = (cv.value*(1-timbrBlend.value))+(netTimbr.value*timbrBlend.value);
			outTimbr.value_(((val*timbrScale.value)+timbrOffset.value).wrap(0,1.001))})});
		inAmp.action_({|cv| if(ampProb.value.coin,{ // rand filter gate
			var val = (cv.value*(1-ampBlend.value))+(netAmp.value*ampBlend.value);
			outAmp.value_(((val*ampScale.value)+ampOffset.value).wrap(0,1.001))})});
		netFrq.action_({|cv| if(frqProb.value.coin,{ // rand filter gate
			var val = (cv.value*frqBlend.value)+(inFrq.value*(1-frqBlend.value));
			outFrq.value_(((val*frqScale.value)+frqOffset.value).wrap(0,1.001))})});
		netTimbr.action_({|cv| if(timbrProb.value.coin,{ // rand filter gate
			var val = (cv.value*timbrBlend.value)+(inTimbr.value*(1-timbrBlend.value));
			outTimbr.value_(((val*timbrScale.value)+timbrOffset.value).wrap(0,1.001))})});
		netAmp.action_({|cv| if(ampProb.value.coin,{ // rand filter gate
			var val = (cv.value*ampBlend.value)+(inAmp.value*(1-ampBlend.value));
			outAmp.value_(((val*ampScale.value)+ampOffset.value).wrap(0,1.001))})});
		frqBlend.action_({|cv|
			var val = (inFrq.value*(1-cv.value))+(netFrq.value*cv.value);
			outFrq.value_(((val*frqScale.value)+frqOffset.value).wrap(0,1.001))});
		timbrBlend.action_({|cv|
			var val = (inTimbr.value*(1-cv.value))+(netTimbr.value*cv.value);
			outTimbr.value_(((val*timbrScale.value)+timbrOffset.value).wrap(0,1.001))});
		ampBlend.action_({|cv|
			var val = (inAmp.value*(1-cv.value))+(netAmp.value*cv.value);
			outAmp.value_(((val*ampScale.value)+ampOffset.value).wrap(0,1.001))});
		frqOffset.action_({|cv|
			var val = (inFrq.value*(1-frqBlend.value))+(netFrq.value*frqBlend.value);
			outFrq.value_(((val*frqScale.value)+cv.value).wrap(0,1.001))});
		timbrOffset.action_({|cv|
			var val = (inTimbr.value*timbrBlend.value)+(netTimbr.value*(1-timbrBlend.value));
			outTimbr.value_(((val*timbrScale.value)+cv.value).wrap(0,1.001))});
		ampOffset.action_({|cv|
			var val = (inAmp.value*(1-ampBlend.value))+(netFrq.value*ampBlend.value);
			outAmp.value_(((val*ampScale.value)+cv.value).wrap(0,1.001))});
		// should params be sent to all from inParam CVs instea of outParamCVs?
		outFrq.action_({|cv|
			network.sendParamToAll(\freq,vcNum,inFrq.value); ctlPlayerActions[0].(this,frqCtlDest.value,cv.value) });
		outTimbr.action_({|cv|
			network.sendParamToAll(\timbre,vcNum,inTimbr.value); ctlPlayerActions[1].(this,timbrCtlDest.value,cv.value)});
		outAmp.action_({|cv|
			network.sendParamToAll(\amp,vcNum,inAmp.value); ctlPlayerActions[2].(this,ampCtlDest.value,cv.value) });

		[inFrq,frqProb,frqScale,frqOffset,outFrq,netFrq,frqBlend].do {|cv,i|
			if(cv.notNil,{ cv.connect(guiCtls[0][i]) })};
		[inTimbr,timbrProb,timbrScale,timbrOffset,outTimbr,netTimbr,timbrBlend].do {|cv,i|
			if(cv.notNil,{ cv.connect(guiCtls[1][i]) })};
		[inAmp,ampProb,ampScale,ampOffset,outAmp,netAmp,ampBlend].do {|cv,i|
			if(cv.notNil,{ cv.connect(guiCtls[2][i]) })};
		[frqCtlDest,timbrCtlDest,ampCtlDest].do {|sv,i| sv.connect(guiCtls[i][7]) };
	}

}