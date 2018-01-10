// a network for any number of analyzer/players -- each client can have multiple voices (analyzer-players) -- the network receives data from all local+remote analyzers, displays it, and makes it available to any player within the network

KnowNoNetClient :  NetHub {
	var <myName,<>numVoices,<size,<view,<>analyzers,<synths,synthsFlg;
	var <oscGroupClient,<networkSize,<localClients,<localNumVcs,netDisp,<plDispStarts,
	<plMarkers,<netPlayers,netData,<myNetIndex,<netUpdateRoutine,<netTimbr,<netFrq,<netAmp,
	<paramData,<paramCtlViews,<synthViews;

	*new { arg name=\knowNo,numVcs=1,size=20,remote,synthsFlg=true;
		^super.new.knInit(name,numVcs,size,remote,synthsFlg)
	}

	knInit { arg argName,argNumVcs,argSize,argRemote,argsynthsFlg;
		myName=argName.asSymbol;
		numVoices=argNumVcs;
		size=argSize;  // max size of sum of all netPlayer voices (analyzer-player combos)
		oscGroupClient=argRemote;
		synthsFlg=argsynthsFlg;  // flag normally true creates KnowNoSynthPlayer

		netPlayers= ();  // stores client ip and numVoices, keys are clientNames
		localClients=[];
		this.broadcastNumVoices; //  already broadcast by super.new, but call again to publish numVoices
		if(oscGroupClient.notNil,{  // if this is an OSC remote server
			this.setupRemoteResponders;  //  to receive data from remote clients
			oscGroupClient.join;
			{ oscGroupClient.sendMsg('/remHostip',
				NetAddr.myIP,NetAddr.langPort,myName,numVoices);
				"'/remHostip' message sent ".postln
			}.defer(4)
		});

		this.setmyNetIndex;  // start updating this client's network index
		paramData= (   // holds current data, for filtering data jitter
			freq:Array.fill(numVoices,{0}),
			timbre:Array.fill(numVoices,{0}),
			amp:Array.fill(numVoices,{0})
		);

		analyzers= FBanalyzer.new(numVoices,this);  // includes 'Server.local.waitForBoot'
		{ while({ analyzers.fbGroup.isNil },{ 0.1.wait }); // wait for boot and analyzer load
			{  // wait for configuration of networks
				this.makeGui;
				this.setupLocalParamResponders;
			}.defer(1)
		}.fork
	}

	broadcastNumVoices {  // to local NetHub
		var broadcastAddress,
		hostIPAddress = NetAddr.myIP;
		var broadcastAddressSplit = hostIPAddress.split($\.);
		broadcastAddressSplit.put(3,"255");
		// Concatenate the results together
		broadcastAddressSplit.do({|item, i|
			if(i > 0, { broadcastAddress = broadcastAddress ++  "." ++ item;
				}, { // first member of IP address doesn't need to have a dot attached to it
					broadcastAddress = item;
			})
		});
		NetAddr.broadcastFlag = true;
		NetAddr(broadcastAddress, NetAddr.langPort).sendMsg('/hostip', hostIPAddress, NetAddr.langPort, hostName, numVoices);  // added numVoices of this host to this message
		NetAddr.broadcastFlag = false;
		("NetHub: IP broadcast to network on " ++ broadcastAddress).postln;
	}

	setupNetwork {
		// respond to IP-Voice message from a client on the local network
		OSCdef(\clientip, {| msg,time,addr,recvPort|
			var clientIP = msg[1], clientName = msg[3], clientNumVcs = msg[4];
			msg.postln;
			clients = clients.add(clientName.asSymbol -> NetAddr(clientIP.asString, msg[2].asInteger));
			("NetHub: Received IP from " ++ clientName ).post;
			(" " ++ clients[clientName].asString).post;
			(" numVoices = " ++ clientNumVcs.asString).postln;
			this.addToNetPlayers(clientName.asSymbol,clientIP,clientNumVcs);
		},'/clientip');

		// respond to hostIP-Voice message from host on the local network
		OSCdef(\hostip, {| msg,time,addr,recvPort|
			var rcvdIP = msg[1].asString,
			rcvdPort = msg[2].asInteger,
			rcvdName = msg[3].asString,
			rcvdNumVcs = msg[4];
			msg.postln;
			clients = clients.add(rcvdName.asSymbol -> NetAddr(rcvdIP, rcvdPort));
			("NetHub: IP received from " ++ rcvdName ++ " = " ++ rcvdIP ++":" ++ rcvdPort).postln;
			// send my ip and vcs back to the client sender on local network
			NetAddr(rcvdIP, rcvdPort)
			.sendMsg('/clientip', NetAddr.myIP, NetAddr.langPort, hostName, numVoices);
			rcvdNumVcs = if(rcvdNumVcs.notNil,{ rcvdNumVcs.asInteger },{0});
			this.addToNetPlayers(rcvdName.asSymbol,rcvdIP,rcvdNumVcs);
			if(oscGroupClient.notNil,{  // update numVoices in this NetHub to remote network
				localClients=netPlayers.copy;
				localClients.removeAt(\knowNo);  // removeAt(myName?) localClients.removeAt(hostName.asSymbol);
				localNumVcs= localClients.asArray.collect{|n|n[1]}.sum;
				oscGroupClient.sendMsg('/remClientip', NetAddr.myIP, NetAddr.langPort,
					myName,localNumVcs)
			})
		},'/hostip');

		OSCdef(\removeClient,{|msg,time,addr,recvPort|
			var ipOrder,index;
			"removeClient ".post; clients.removeAt(msg[1].asSymbol.postln);
			netPlayers.removeAt(msg[1].asSymbol);
			ipOrder = netPlayers.asArray.sort({|a,b|
				(a[0].asString.split($.)[3].asInt) < (b[0].asString.split($.)[3].asInt) });
			//  unique IPs w vcs
			plDispStarts=ipOrder.copyRange(0,ipOrder.size-1).collect{|n| n[1]}
			.integrate.insert(0,0);
		},'/removeClient');
	}

	setupRemoteResponders {  // merge OscGroup clients with local network clients
		"...loading remote network responders".postln;
		OSCdef(\remClientip, {| msg,time,addr,recvPort|
			var oscGrpIp=oscGroupClient.netAddr.ip;
			var clientIP = msg[1].asString, clientPort = msg[2].asInteger,
			clientName = msg[3].asString, clientNumVcs=msg[4].asInteger;
			msg.postln;
			clients = clients.add(
				clientName.asSymbol -> NetAddr(msg[1].asString, msg[2].asInteger));
			("OscGroup: Received IP from " ++ clientName ++ " ").post;
			clients[clientName].postln;
			this.addToNetPlayers(clientName.asSymbol,oscGrpIp,clientNumVcs);
			// echo this msg to local network clients
			localClients=clients.copy;
			localClients.removeAt(clientName.asSymbol); localClients.removeAt(hostName.asSymbol);
			localClients.asArray.do {|na|
				na.sendMsg('/clientip', oscGrpIp, NetAddr.langPort, clientName,clientNumVcs)
			}
		},'/remClientip');

		OSCdef(\remHostip, {| msg,time,addr,recvPort|
			var oscGrpIp=oscGroupClient.netAddr.ip;
			var receivedIP = msg[1].asString,
			receivedPort = msg[2].asInteger,
			receivedName = msg[3].asString,
			receivedNumVcs = msg[4].asInteger;
			msg.postln;
			("OscGroup: IP received from " ++ receivedName ++ " = " ++ receivedIP ++":" ++
				receivedPort).postln;
			clients = clients.add(
				receivedName.asSymbol -> NetAddr(oscGrpIp, receivedPort));
			this.addToNetPlayers(receivedName.asSymbol,oscGrpIp,receivedNumVcs);
			// reply with my ip back to other OscGroup network
			oscGroupClient.sendMsg('/remClientip',
				NetAddr.myIP, NetAddr.langPort, myName, plDispStarts.last-receivedNumVcs);
			// echo this msg to local NetHub clients
			localClients=clients.copy;
			localClients.removeAt(\knowNo); localClients.removeAt(hostName.asSymbol);
			localClients.asArray.do {|na|
				na.sendMsg('/clientip',na.ip,NetAddr.langPort,receivedName,receivedNumVcs)
			}
		},'/remHostip');

		OSCdef(\remoteParam,{|msg|  // recv and respond, then echo remoteParams to local netHub
			var pl = msg[1].asString.split($_)[1].asInt;
			var param=msg[2], vc=msg[3], data=msg[4];
			msg.postln;
			// show all received param data on netDisp
			this.displayNetData(pl,vc,param.asSymbol,data);
			if(pl == myNetIndex,{ // just post if it comes from me
				"my ".post; param.post; " = ".post; data.postln
				// route to paramCtlView if from a selected netPlayer
				},{ paramCtlViews.do {|view,i|
					if(view.netPlayer.value == (plDispStarts[pl]+vc),{
						switch(param,
							\freq,{ paramCtlViews[i].netFrq.value_(data)},
							\timbre,{ paramCtlViews[i].netTimbr.value_(data)},
							\amp,{ paramCtlViews[i].netAmp.value_(data)})
					})}
			});
			// echo this msg to local NetHub clients
			localClients.asArray.do {|na|
				na.sendMsg('/param',msg[1],msg[2],msg[3],msg[4])
			}
		},'/remoteParam')
	}

	setupLocalParamResponders {
		"...loading local network responders".postln;
		this.setAllResponses({|msg|  // local network responder
			var pl=msg[1], param=msg[2], vc=msg[3], data=msg[4];
			msg.postln;
			// show all received param data on netDisp
			this.displayNetData(pl,vc,param.asSymbol,data);
			if(pl == myNetIndex,{ // just post if it comes from me
				"my ".post; param.post; " = ".post; data.postln
				// route to paramCtlView if from a selected netPlayer
				},{ paramCtlViews.do {|view,i|
					if(view.netPlayer.value == (plDispStarts[pl]+vc),{
						switch(param,
							\freq,{ paramCtlViews[i].netFrq.value_(data)},
							\timbre,{ paramCtlViews[i].netTimbr.value_(data)},
							\amp,{ paramCtlViews[i].netAmp.value_(data)})
					})}
			});
		},'/param');
	}

	// Possible BUG: netPlayers will include both a local and remote player -- plDispStarts will be incorrect if a player is both in local and remote networks!!
	addToNetPlayers {|name,ip,vcs|
		var ipOrder,index,starts;
		netPlayers.add(name->[ip,vcs]);
		ipOrder = netPlayers.asArray.sort({|a,b| (a[0].asString.split($.)[3].asInt) < (b[0].asString.split($.)[3].asInt) }); //  unique IPs w vcs
		plDispStarts=ipOrder.copyRange(0,ipOrder.size-1).collect{|n| n[1]}
		.integrate.insert(0,0);  // start pos in netDisp for each netPlayer, + total voices (next start pos)
	}

	setmyNetIndex {
		netUpdateRoutine =
		Routine({
			inf.do({
				var ips = clients.values.collect {|n| n.ip }.asSet; // save only unique IPs
				// myNetIndex is this client's position in network as sorted by ip-addresses
				myNetIndex = ips.asArray.sort.detectIndex {|n| n == NetAddr.myIP }; // index low to high
				networkSize=ips.size;
				1.wait; })
		}).play
	}

	sendParamToAll {|param,vc,val|
		if(((paramData[param.asSymbol][vc]-val).abs > 0.01),{  // don't send repeating values
			paramData[param.asSymbol].put(vc,val);
			this.toAll(["/param",myNetIndex,param,vc,val]);  // local network broadcast
			if(oscGroupClient.notNil,{  // send also to clients on remote network
				oscGroupClient.sendMsg('/remoteParam',
					(myName.asString++"_"++(myNetIndex.asString)).asSymbol,param,vc,val)
			})
		})
	}

	displayNetData {|idx=0,vc=0,param=\freq,value=0.5|
		{ netDisp.index_((plDispStarts[idx]+vc)*6+3+(\freq:0,\timbre:1,\amp:2)[param] );
			netDisp.currentvalue_(value) }.defer
	}

	makeGui {
		view = FlowView.new(nil,Rect(8,60,460,150+(160*numVoices)),8@8,2@2,"knowNo");
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
		view.parent.onClose_({this.stopAll });
		paramCtlViews = [];
		if(synthsFlg,{ synthViews = FlowView.new(nil,Rect(8,60,460,150+(160*numVoices)),
			8@8,2@2,"knowNoSynths")},{
				"... no SynthDefs loaded with this network...supply your own for now! ".postln
		});
		synths = [];
		numVoices.do {|i|
			paramCtlViews = paramCtlViews.add(KnParamsCtlView.new(this,i,view,size));
			if(synthsFlg,{
				synths = synths.add(KnowNoSynthPlayer.new(this,i,synthViews)) });
		};
		view.parent.onClose_({ "exiting...".postln; this.stopAll })
	}

	removeFromNetworks {
		clients.do {|addr|  addr.post.sendMsg('/removeClient',hostName.postln.asSymbol) };
		if(oscGroupClient.notNil,{oscGroupClient.sendMsg('/removeClient','knowNo')})
	}

	stopAll {
		netUpdateRoutine.stop;
		OSCdef.freeAll;
		this.removeFromNetworks
	}

}


/*

Local-Remote Design:

OscGroups maintains a network of local networks, where one remote player per each local network logs into it via OscGroupClient.  This player must echo every parameter message from his local network to the OscGroup network, with each local network player on his network being represented as one or more of his voices.  So the OscGroup player has his own n local voices, and n + networkSize OscGroup voices.  When a \hostip message is received by this remote player from the local network, the OscGroup player must send a new remClientip message that updates his number of voices to the OscGroup network.  Each OscGroup player receiving it must then  echo that message to the local net as a hostip message, so that everyone in the network of networks maintains the same number of total voices.  Local net players only message directly to other local net players, but OscGroup remote players must message both locally and echo to other remote players.

Know-No feedback Instructions:

Sound producing synth(s) play to 1 audio rate Bus which is analyzed for pitch, timbre, and amplitude.
Analysis data is mixed individually by each player with data received from 1 other player,
then published to the network.  Players choose which stream they are currently mixing with their own analysis.
Synth(s) have 3 input control rate buses that are connected to the published 3 feedback parameters of their sound
While playing, choose  different netPlayers to patch their analysis data to your input controls. Manipulate the probability control to change how often the feedback is applied to the synth; manipulate scale control to change its range, and offset to tune that range of change to different values.  Tune the blend knob to mix your feedback with that of your chosen netPlayer.  The result of this feedback data matrix mix process is shown in the numberBox, and in the three sliders in the Synth gui section.  The
last two sliders are entirely manually controlled, allowing you control the frequency range of the synth independent of the feedback, and to control the volume of your local sound output.

INSTRUCTIONS FOR INSTALLING AND STARTING THE NEW remote KnowNoNetClient CLASS :

1. Put a copy of the OscGroupClient binary executable into your /Applications/SuperCollider directory.

2. Put OscGroup.sc in your /Users/Username/Library/Application Support/SuperCollider/quarks/OscGroupClient
folder, or if you don't have this installed as a Quark, put it directly here:

Users/Username/Library/Application Support/SuperCollider/Extensions/classes

3. Add KnowNoNetClient.sc to the same directory:
Users/Username/Library/Application Support/SuperCollider/Extensions/classes

4. Start SuperCollider and execute these two lines:

OscGroupClient.program_("./Applications/OscGroupClient")
File.exists("/Applications/OscGroupClient")

a=OscGroupClient("pauline.mills.edu","chris","chrispwd","hub","hubpwd");
b=KnowNoNetClient(remote:a);

****** TESTS:

a=OscGroupClient("pauline.mills.edu","chris","chrispwd","hub","hubpwd");
b=KnowNoNetClient.new(numVcs:2,remote:a);

b=KnowNoNetClient.new(numVcs:2,remote:a,synthsFlg: false);

b.oscGroupClient.sendMsg('/remHostip',NetAddr.myIP,NetAddr.langPort,b.myName,b.numVoices)

b.netPlayers.keys
b.clients
b.oscGroupClient
b.plDispStarts
b.myNetIndex
b.hostname
b.removeFromNetworks
Window
b.sendParamToAll(\freq,0,0.5.rand)
OSCFunc.trace(true)
b.oscGroupClient.netAddr.ip
~oscgrp = NetAddr.new("localhost",22244);
~oscgrp.sendMsg('/remHostip',NetAddr.myIP,NetAddr.langPort,b.myName,b.numVoices);
a.sendMsg('/removeClient','knowNo'.asSymbol)
*/

