// a network for any number of analyzer/players -- each client can have multiple voices (analyzer-players) -- the network receives data from all local+remote analyzers, displays it, and makes it available to any player within the network

KnowNoNetwork {
	var <myName,<>numVoices,<size,<view,<>analyzers,<synths,synthsFlg;
	var <>network,<networkSize,<>oscGroupClient,
	netDisp,<plDispStarts,<plMarkers,<netPlayers,netData,
	<paramData,<paramCtlViews,<synthViews,<presets;

	*new { arg name=\knowNo,numVcs=1,oscAddr="pauline.mills.edu",size=20,
		synthsFlg=true,presets=true,bus=true;
		^super.new.init(name,numVcs,oscAddr,size,synthsFlg,presets,bus)
	}

	init { arg argName,argNumVcs,argOscAddr,argSize,argsynthsFlg,argPresets;
		myName=argName.asSymbol;
		numVoices=argNumVcs;
		size=argSize;  // max size of sum of all netPlayer voices (analyzer-player combos)
		oscGroupClient=OscGroupClient.new(
			argOscAddr,myName,myName.asString++"pwd","KnowNo","knowpwd").join;
		synthsFlg=argsynthsFlg;  // flag normally true creates KnowNoSynthPlayer
		argPresets ?? { presets = KnowNoConsolePresets(this) };
		netPlayers= ();  // stores client ip and numVoices, keys are clientNames
		this.setupOscGroupNetwork;
		{ oscGroupClient.sendMsg('/host',myName,numVoices);
			"sent '/host' message ".postln
		}.defer(4);

		paramData= (   // holds current data, for filtering data jitter
			freq:Array.fill(numVoices,{0}),
			timbre:Array.fill(numVoices,{0}),
			amp:Array.fill(numVoices,{0})
		);

		analyzers= FBanalyzer.new(numVoices,this);  // includes 'Server.local.waitForBoot'
		{ while({ analyzers.fbGroup.isNil },{ 0.1.wait }); // wait for boot and analyzer load
			{  // wait for configuration of networks
				this.makeGui;
				this.addToNetPlayers(myName,numVoices);
			}.defer(1)
		}.fork;
		ShutDown.add({this.stopAll});
	}

	setupOscGroupNetwork {
		"...loading OscGroup network responders".postln;
		OSCdef(\client, {| msg,time,addr,recvPort|
			var clientName = msg[1].asSymbol, clientNumVcs=msg[2].asInteger;
			msg.postln;
			("OscGroup: Joined by " ++ clientName ++
				" with " ++ clientNumVcs.asString ++ " voices").postln;
			this.addToNetPlayers(clientName,clientNumVcs);
		},'/client');

		OSCdef(\host, {| msg,time,addr,recvPort|
			var hostName = msg[1].asSymbol,
			hostNumVcs = msg[2].asInteger;
			msg.postln;
			(hostName.asString ++ " joined NoKnowNetwork with " ++ hostNumVcs.asString ++ "voices").postln;
			this.addToNetPlayers(hostName,hostNumVcs);
			// reply to OscGroup with my name and numVoices
			oscGroupClient.sendMsg('/client',myName,numVoices);
		},'/host');

		OSCdef(\param,{|msg|  // recv and respond
			var plName = msg[1].asSymbol,
			vc=msg[2].asInteger,param=msg[3].asSymbol,data=msg[4];
			// msg.postln;
			if(netPlayers.keys.includes(plName),{
				// show all received param data on netDisp
				this.displayNetData(plName,vc,param,data);
				paramCtlViews.do {|view,i|
					if(view.netPlayer.value == (plDispStarts[plName]+vc),{
						switch(param,
							\freq,{ paramCtlViews[i].netFrq.value_(data)},
							\timbre,{ paramCtlViews[i].netTimbr.value_(data)},
							\amp,{ paramCtlViews[i].netAmp.value_(data)})
			})}})
		},'/param');

		OSCdef(\removeClient,{|msg,time,addr,recvPort|
			"removed client ".post;
			netPlayers.removeAt(msg[1].asSymbol.postln);
			this.sortNetDispPlayers
		},'/removeClient');

		OSCdef(\numVoices,{|msg,time,addr,recvPort|
			var rcvdName=msg[1].asSymbol,rcvdIP=msg[2].asString,rcvdNumVcs=msg[3].asInteger;
			this.addToNetPlayers(rcvdName.asSymbol,rcvdNumVcs);
		},'/numVoices')
	}

	addToNetPlayers {|name,vcs|
		// protect against same client name
		if(netPlayers.includes(name),{name = (name.asString++(100.rand.asString)).asSymbol});
		netPlayers.add(name -> vcs);
		// sort for display of netPlayers data
		this.sortNetDispPlayers;
		"KnowNo Network: ".post; netPlayers.postln;
	}

	sortNetDispPlayers {
		var plOrder,plStarts,starts;
		plOrder = netPlayers.keys.asArray.sort;
		plStarts = plOrder.collect {|k| netPlayers[k] }.integrate.insert(0,0);  // start pos in netDisp for each netPlayer, + total voices (next start pos)
		starts = (); plOrder.do {|name,i| starts.add(name->plStarts[i])};
		plDispStarts = starts.copy;
		numVoices.do {|i| paramCtlViews[i].myPl.value_((plDispStarts[myName])+i+1) };
	}

	sendParamToAll {|param,vc,val|
		var netInCV;
		if(((paramData[param.asSymbol][vc]-val).abs > 0.01),{  // don't send repeating values
			paramData[param.asSymbol].put(vc,val);
			this.displayNetData(myName,vc,param,val);  // display my Data
			oscGroupClient.sendMsg('/param',myName,vc,param,val); 	// send to everyone else
			// feedback to my voices, if not coming from same voice
			paramCtlViews.do {|view,i|
				if(((view.netPlayer.value) != (view.myPl.value-1)),{
					if(((plDispStarts[myName]+vc) == (view.netPlayer.value)),{
						netInCV= switch(param,\freq,{view.netFrq},
							\timbre,{view.netTimbr},\amp,{view.netAmp});
						netInCV.value_(val)})})};
		})
	}

	displayNetData {|name,vc=0,param=\freq,value=0.5|
		{ netDisp.index_((plDispStarts[name]+vc)*6+3+(\freq:0,\timbre:1,\amp:2)[param] );
			netDisp.currentvalue_(value) }.defer
	}

	makeGui {
		view = FlowView.new(nil,Rect(8,60,460,150+(180*numVoices)),8@8,2@2,"knowNo");
		netData=CV.new(Array.fill(size*6,{0},0));
		// net Data display

		view.startRow;
		netDisp = MultiSliderView.new(view,435@100).size_(120).elasticMode_(1);
		netDisp.strokeColor_(Color(1,alpha:0.5)).fillColor_(Color.blue).background_(Color.grey);
		netDisp.value_(Array.fill(size*6,{0})).isFilled_(true).startIndex_(3);
		view.startRow;
		StaticText.new(view,4@20).visible_(false);
		plMarkers = size.do {|i|
			StaticText.new(view,20@20).string_((i+1).asString).font_(Font("Times",9)) };
		view.parent.onClose_({ this.stopAll });
		paramCtlViews = [];
		if(synthsFlg,{ synthViews = FlowView.new(nil,Rect(474,60,460,(180*numVoices)),
			8@8,2@2,"knowNoSynths")},{
				"... no SynthDefs loaded with this network...supply your own for now! ".postln
		});
		synths = [];
		numVoices.do {|i|
			paramCtlViews = paramCtlViews.add(KnParamsCtlView.new(this,i,view,size));
			if(synthsFlg,{
				synths = synths.add(KnowNoSynthPlayer.new(this,i,synthViews)) });
		};
		Button(view.parent,Rect(385,140,50,20)).states_([["login"]])
		.action_({ oscGroupClient.sendMsg('/host',myName,numVoices);
			"sent '/host' message ".postln });
		view.parent.onClose_({ "exiting...".postln; this.stopAll })
	}

	removeFromNetwork {
		"....removing ".post;
		oscGroupClient.sendMsg('/removeClient',myName.postln.asSymbol)
	}

	stopAll {
		this.removeFromNetwork;
		OSCdef.freeAll;
	}

}


/*

KnowNoNetwork Instructions:

Sound producing synth(s) play to an audio Bus which is analyzed for pitch, timbre, and amplitude.
Analysis data is mixed individually by each player with data received from 1 other player,
then published to the network.  Players choose which stream they are currently mixing with their own analysis.  Synth(s) have 3 input controls that are connected can be connected to 3 feedback parameters of their sound.  While playing, select  different netPlayers and patch their analysis data to your input controls. Manipulate the probability control to change how often the feedback is applied to the synth; manipulate scale control to change its range, and offset to tune that range of change to different values.  Tune the blend knob to mix your feedback with that of your chosen netPlayer.  The result of this feedback data matrix mix process is shown in the numberBox, and in the three sliders in the Synth gui section.  The bottom two sliders for each synth are entirely manually controlled, allowing you control one parameter of the synth independent of the feedback, and to control the volume of your local sound output.

INSTRUCTIONS FOR INSTALLING AND STARTING THE NEW remote KnowNoOscGroup CLASS:

1. Put the OscGroupClient binary executable file into your /Applications/SuperCollider directory.

2. Put the other .sc files into this directory:

Users/Username/Library/Application Support/SuperCollider/Extensions/classes

3. Launch SuperCollider and execute these two lines, then
start the KnowNoNetwork program by executing the following line, substituting your own name started with a forward-slash, followed by the number of voices (1-4) you would like to play:

b=KnowNoNetwork.new(\yourname,4)

4.  Two windows should have opened:  "knowNo" shows the network
data traffic, and "knowNoSynths" shows controls for your own
synths.  Select a synth from the menu of one of these, and press
the "SynthOn" button, and you should hear a steady sound, whose
volume you can change with the "vol" slider.  You can change its
pitch by moving the "freq" slider.  These are your two manual
controls over your own sound.  Look at the "knowNo" window and
notice that the "freq", "timbre", and "amp" sliders should have
moved and may keep moving, depending on the synth that you chose.
Next, create a feedback path for your this sound to change itself by moving the 2nd and 3rd sliders for each of these parameters all, or most of the way to the right.  You should immediately notice changes to the
sound corresponding to changes of the positions of the sliders 1, 2,
and 3 in the "knowNoSynth" window. You can move these sliders manually, but the feedback always controls them too.  Now,
************
The following commands are for diagnostics only:

b.netPlayers
b.oscGroupClient.sendMsg('/host',\abel,2);
b.plDispStarts
b.sortNetDispPlayers

*/






