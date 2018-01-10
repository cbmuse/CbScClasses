// a network for any number of analyzer/players -- each client can have multiple voices (analyzer-players) -- the network receives data from all local+remote analyzers, displays it, and makes it available to any player within the network

KnowNoOscHub {
	var <myName,<>numVoices,<size,<view,<>analyzers,<synths,synthsFlg;
	var <>network,<networkSize,<>oscHub,<>hCommFunc,
	netDisp,<plDispStarts,<plMarkers,<netPlayers,netData,
	<paramData,<paramCtlViews,<synthViews;

	*new { arg name=\knowNo,numVcs=1,oscAddr="oschub.asia",size=20,synthsFlg=true;
		^super.new.init(name,numVcs,oscAddr,size,synthsFlg)
	}

	init { arg argName,argNumVcs,argOscAddr,argSize,argsynthsFlg;
		myName=argName.asSymbol;
		numVoices=argNumVcs;
		size=argSize;  // max size of sum of all netPlayer voices (analyzer-player combos)
		oscHub = OSCHub(NetAddr.new("oschub.asia", 57120)).initChat;
		synthsFlg=argsynthsFlg;  // flag normally true creates KnowNoSynthPlayer
		netPlayers= ();  // stores client ip and numVoices, keys are clientNames
		hCommFunc = ();
		this.setupOscHubNetwork;
		paramData= (   // holds current data, for filtering data jitter
			freq:Array.fill(numVoices,{0}),
			timbre:Array.fill(numVoices,{0}),
			amp:Array.fill(numVoices,{0})
		);
		oscHub.startPieceFunc_({this.buildPlayerGui}); //buildGui&Synths after oscHub starts
		ShutDown.add({this.stopAll});
	}

	buildPlayerGui {
		this.makeGui;
		oscHub.sendMsg("/h_comm","host",myName,numVoices);
		{ // wait for /client replies
			analyzers= FBanalyzerOSCHub.new(numVoices,this).startAnalyzer;
			this.addToNetPlayers(myName,numVoices);  // add self
			if(synthsFlg,{
				numVoices.do {|i| synths = synths.add(
					KnowNoSynthPlayerOSCHub.new(this,i,synthViews)) }});
		}.defer(2);
	}

	setupOscHubNetwork {
		"...loading OscHub network responders".postln;

		hCommFunc.put(\client, {| msg|
			var clientName = msg[1].asSymbol, clientNumVcs=msg[2].asInteger;
			msg.postln;
			("OscHub: Joined by " ++ clientName ++
				" with " ++ clientNumVcs.asString ++ " voices").postln;
			this.addToNetPlayers(clientName,clientNumVcs);
		});

		hCommFunc.put(\host, {| msg,time,addr,recvPort|
			var hostName = msg[1].asSymbol,
			hostNumVcs = msg[2].asInteger;
			msg.postln;
			(hostName.asString ++ " joined NoKnowNetwork with " ++ hostNumVcs.asString ++ "voices").postln;
			this.addToNetPlayers(hostName,hostNumVcs);
			// reply to OscHub with my name and numVoices
			oscHub.sendMsg("/h_comm","client",myName,numVoices);
		});

		hCommFunc.put(\param,{|msg|  // recv and respond
			var plName = msg[1].asSymbol,
			vc=msg[2].asInteger,param=msg[3].asSymbol,data=msg[4];
			msg.postln;
			if(netPlayers.keys.includes(plName),{
				// show all received param data on netDisp
				this.displayNetData(plName,vc,param,data);
				// update netParams except if coming from self
				paramCtlViews.do {|view,i|
					if((view.netPlayer.value != (view.myPl.value-1)),{  // not coming from self
						if(view.netPlayer.value == (plDispStarts[plName]+vc),{
							switch(param,
								\freq,{ paramCtlViews[i].netFrq.value_(data)},
								\timbre,{ paramCtlViews[i].netTimbr.value_(data)},
								\amp,{ paramCtlViews[i].netAmp.value_(data)})
			})})}})
		});

		hCommFunc.put(\removeClient,{|msg|
			"removed client ".post;
			netPlayers.removeAt(msg[1].asSymbol.postln);
			this.sortNetDispPlayers
		});

		hCommFunc.put(\numVoices,{|msg,time,addr,recvPort|
			var rcvdName=msg[1].asSymbol,rcvdIP=msg[2].asString,rcvdNumVcs=msg[3].asInteger;
			this.addToNetPlayers(rcvdName.asSymbol,rcvdNumVcs);
		});

		oscHub.hCommFunc_(hCommFunc)
	}

	addToNetPlayers {|name,vcs|
		// protect against same client name
		if(netPlayers.includes(name),{name = (name.asString++(100.rand.asString)).asSymbol});
		netPlayers.add(name -> vcs);
		// sort for display of netPlayers data
		this.sortNetDispPlayers;
		"KnowNo Network: ".post; netPlayers.postln;
//		analyzers.fbBus = 64+((netPlayers.size-1)*4); // reassign fbBus for net config?
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
				oscHub.sendMsg("/h_comm","param",myName,vc,param,val); 	// send to everyone else
			// feedback to my voices, if not coming from same voice
/*			paramCtlViews.do {|view,i|
				if(((view.netPlayer.value) != (view.myPl.value)),{
					if(((plDispStarts[myName]+vc) == (view.netPlayer.value)),{
						netInCV= switch(param,\freq,{view.netFrq},
							\timbre,{view.netTimbr},\amp,{view.netAmp});
						netInCV.value_(val)})})};*/
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
			8@8,2@2,"knowNoSynths")
		},{
				"... no SynthDefs loaded with this network...supply your own for now! ".postln
		});
		synths = [];
		numVoices.do {|i|
			paramCtlViews = paramCtlViews.add(KnParamsCtlView.new(this,i,view,size));
		};
		Button(view.parent,Rect(385,140,50,20)).states_([["login"]])
			.action_({ oscHub.sendMsg("/h_comm","host",myName,numVoices);
			"sent '/host' message ".postln });
		view.parent.onClose_({ "exiting...".postln; this.stopAll })
	}

	removeFromNetwork {
		"....removing ".post;
		oscHub.sendMsg("/h_comm","removeClient",myName.postln.asSymbol)
	}

	stopAll {
		this.removeFromNetwork;
		OSCdef.freeAll;
	}

}

/*

KnowNoOscHub Instructions:

Synths play on discrete audio buses which are each analyzed for their pitch, timbre, and amplitude.
Analysis data controls the 'input' sliders on the left of the 'KnowNo' window. The smaller 'net' sliders show data received from 1 other synth played by another synth in the network (including your own), selected by the 'netPlayer' menu.  The synth and net data are mixed using the 'blend' knob to create control feedback for one synth parameter, as well as being published to the network for anyone to use.  Feedback data is represented by a 0.0->1.0 float shown in the number boxes on the right side of the window, and also as vertical bars in the top display of the 'KnowNo' window.

While playing, select  different netPlayers to use their analysis data to mix with your's. Move the 'probability' slider to select how often new feedback values are applied to the synth; move the 'scale' slider to change the data's maximum range, and move the 'offset' knob to make that range begin at different values - when values reach 1.0, they wrap to start again at 0.0, and this big change can have interesting affects on the synths. The output of each mixed feedback data stream controls one of three synth parameters chosen with the 'ctl' menu; these parameter controls are shown and can be moved manually with the narrow, numbered sliders in the 'KnowNoSynths' window. The bottom two sliders for each synth are entirely manually controlled, allowing you adjust one parameter of the synth independent of any feedback influence, and to control the volume of the synth's local sound output without affecting its 'amp' analysis output.

The musical goal in playing is to find settings of synth and feedback parameters that allows the system to play itself.  The influence of feedback parameters within and between synths, together with micro-delays inherent in the network, can be exploited to create complex sonic textures that continuously change in interesting yet unpredictable ways.  Notice how small adjustments in the feedback data matrix controls ripple out and produce change through all the synths in the network.  The synth sliders should be played sparingly to find starting points and make direct changes if the system gets too static.  Listen and patiently explore this shared instrument using any desireable prearranged forms that vary the density and rate-of-change of the music within any time-frame.


INSTRUCTIONS FOR INSTALLING AND STARTING THE NEW remote KnowNoOscHub CLASS library:

1. Put these provided Class files --

KnowNoOscHub.sc, KnowNoSynthPlayerOSCHub.sc, FBanalyzerOSCHub.sc, OSCHub.sc

into this directory:

Users/Username/Library/Application Support/SuperCollider/Extensions

2.  Also put the Conductor folder into the same Extensions directory, UNLESS it is already
installed as a Quark in your system.  Note also, if you are NOT using SC 3.7alpha, you should
replace the CV.sc file in the Conductor/classes/CV directory with the alternate version supplied;
otherwise store that version outside the Extensions folder!

3. Recompile SC3 and launch SuperCollider then execute the following line, substituting your own name started with a forward-slash, followed by the number of voices (1-4) you would like to play:

x = KnowNoOscHub.new(\yourname,4);

4.  The 'osc radio'  window should have opened.   Click the top bar to
turn it on, then wait for a few seconds until two more windows open.
"knowNo" shows the network feedback data traffic, and "knowNoSynths"
shows controls for your own synths.  Select a synth from the menu of
one of these, and press the "SynthOn" button, and you should hear a
steady sound, whose volume you can change with the "vol" slider.
You can change its pitch by moving the "freq" slider.  These are your two manual
controls over your own sound.  Look at the "knowNo" window and
notice that the "freq", "timbre", and "amp" sliders should have
moved and may keep moving, depending on the synth that you chose.
Next, create a feedback path for this sound to change itself by moving
the 2nd ('probability') and 3rd ('scale') sliders for each parameter all, or
most of the way to the right.  This should create changes of the positions
of the sliders 1, 2, and 3 in the "knowNoSynth" window that also change
the sounds. You can move these sliders manually, but the feedback always
controls them too.  Next play with the offset controls and try to get this
single synth into a feedback loop that keeps making the sound change.

5. If you are playing more than one synth, start another one, and tune its
feedback network similarly to the first one, but notice that since its netPlayer
defaults to '1', the 'net' sliders are also moving, and you can mix these streams
with your own analysis data to make the feedback more complex.  Then go back
to the first synth, and use the menu to set its netPlayer to '2', and you'll be
able to use the second synth's data to influence the first.

6.  Assuming other players join the network, start choosing their 'netPlayer'
data to influence your own.  Note that as other players join, the order of
synth numbers and network data streams may change automatically.  To
avoid this confusion, allow all players to join before starting to play!  You
should see login data in the 'post' window, and if for some reason you
are not receiving data from other players, try clicking on the 'login' button
in the 'knowNo' window.


*/






