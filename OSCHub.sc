OSCHub : Server  {
	var <>name, <>netAddr, <>yourName, nameText, message, sendButton, pathName, logFile, keepAlive;
	var <>messageName, <>defKey, <>hCommFunc, <>startPieceFunc;
	var  window, turnOn, display,vol, volSpc, volSlider;

	*new {|name,netAddr,yourName="me"|
		if(name.isNil,{ name = \OSCHub });
		if(netAddr.isNil,{ netAddr=NetAddr("oschub.asia",57120) });
		^super.newCopyArgs(name,netAddr).init;
	}

	init {
		// all the osc command name SC can use
		messageName = [
			"/quit", "/notify", "/status", "/cmd", "/dumpOSC", "/sync", "/clearSched", "/error",
			"/d_recv", "/d_load", "/d_loadDir", "/d_free",
			"/n_free", "/n_run", "/n_set", "/n_setn", "/n_fill", "/n_map", "/n_mapn", "/n_mapa", "/n_mapan", "/n_before", "/n_after", "/n_query", "/n_trace", "/n_order",
			"/s_new", "/s_get", "/s_getn", "/s_noid",
			"/g_new", "/p_new", "/g_head", "/g_tail", "/g_freeAll", "/g_deepFree", "/g_dumpTree", "/g_queryTree",
			"/u_cmd",
			"/b_alloc", "/b_allocRead", "/b_allocReadChannel", "/b_read", "/b_readChannel", "/b_write", "/b_free", "/b_zero", "/b_set", "/b_setn", "/b_fill", "/b_gen", "/b_close", "/b_query", "/b_get", "/b_getn",
			"/c_set", "/c_setn", "/c_fill", "/c_get", "/c_getn",
			"/nrt_end",
			"/h_comm"
			// "/done, "/fail", "/late",
			// "/n_go", "/n_end", "/n_off", "/n_on", "/n_move", "/n_info",
			// "/tr"
		];
		hCommFunc = {|msg| "h_comm msg = ".post; msg.postln };
		startPieceFunc = { "no piece linked".postln };

		defKey = Array.new;

		for (0, messageName.size-1, {|i|
			var name;
			name = messageName[i].copyRange(1, messageName[i].size-1);
			defKey = defKey.add(name.asSymbol);
		});

		// prepare OSCdefs for all the commands
		for (0, defKey.size-1, {|i|
			OSCdef.newMatching(
				defKey[i],
				{|msg, time, addr, recvPort|
					msg.postln;
					if (msg[0] != '/h_comm', {
						Server.local.listSendMsg(msg);
					},{ msg.removeAt(0); // strip '/h_comm';
						hCommFunc[msg[0]].(msg)  // respond to msg type
					});
				},
				messageName[i]
			);
		});
	}

	initChat {
		// chat is written on a file named "sessionlog", which is saved in the same directory as this file.
		pathName = PathName.new(thisProcess.nowExecutingPath).pathOnly++"sessionlog";
//		logFile = File("~/CB-SC3/network_music/OscAsiaRadio_sessionlog.txt".standardizePath,"a");
		window = Window.new("o s c   r a d i o", Rect(50, 20, 500, 570));
		window.view.decorator = FlowLayout(window.view.bounds);
		window.view.decorator.gap = 2@2;

		turnOn = Button.new(window, 490@24);
		turnOn.states = [
			["Please click here to turn me on!", Color.black, Color.grey],
			["Now we're listening to the noise...", Color.black, Color.white]
		];
		turnOn.action = {arg butt;
			if (butt.value==1, {
				Server.local.waitForBoot({
					SynthDef.new(\pong, {|freq|
						Out.ar(
							0,
							EnvGen.kr(Env.perc, doneAction: 2)
							*SinOsc.ar(exprand(1, 2.0)*freq, mul: 0.2).dup
						);
					}).doSend(this);
				});
				butt.remove;
				window.view.decorator.shift(0, -555);
				turnOn = StaticText.new(window, 490@24);
				turnOn.string = "          Tuning in...";
				turnOn.stringColor = Color.yellow;
				window.refresh;
				{
					if (Server.local.serverRunning.not, {
						turnOn.string = "          Having some problem. Sorry for the inconvenience.";
						turnOn.stringColor = Color.red;
					}, {
						turnOn.string = "          Now you're listening to the noise...";
						turnOn.stringColor = Color.black;
						startPieceFunc.()
					});
				}.defer(6);
				keepAlive = Routine{
					loop{
						this.sendMsg(\keepAlive);
						10.wait;
					}
				};
				keepAlive.play;
			})
		};

		window.view.decorator.nextLine;
		display = TextView.new(window, 490@400);
		display.string = "(Messages are coming here...)";

		window.view.decorator.nextLine.shift(25, 10);
		yourName = TextField.new(window, 80@22);

		nameText = StaticText.new(window, 190@22);
		nameText.string = " <-- Fill in your name.";


		window.view.decorator.nextLine.shift(25, 10);
		sendButton = Button.new(window, 440@24);
		sendButton.states = [
			["Type your message below and click here or press return key.", Color.black, Color.grey],
			["Type your message below and click here or press return key.", Color.black, Color.yellow]
		];

		window.view.decorator.nextLine.shift(25, 0);
		message = TextField.new(window, 440@26);

		sendButton.action = {arg butt;
			var sendingMessage, displayingMessage;
			if (butt.value==1, {
				if (yourName.value!="", {
					"sending message...".postln;
					sendingMessage = yourName.value ++ ": " ++ message.value;
					this.sendMsg("/chat", sendingMessage);
					displayingMessage = display.string ++ "\n" ++ sendingMessage;
					if (displayingMessage.findAll("\n").size>23, {
						displayingMessage=displayingMessage.copyRange(
							displayingMessage.find("\n")+1,
							displayingMessage.size-1
						);
					});
					this.sendMsg("/s_new", \pong, 20001, 0, 1, \freq, 440);
					//display.valueAction_(displayingMessage);
					if (nameText.value!="", {nameText.string = "";});
				}, {
					nameText.stringColor = Color.red;
				});
				butt.value = 0;
			})
		};

		message.action = {arg field;
			sendButton.valueAction_(1)
		};

		OSCdef.newMatching(
			"speakandspell",
			{|msg, time, addr, recvPort|
				var receivedMessage;
				"received!".postln;
				{receivedMessage = display.string ++ "\n" ++ msg[1].asString}.defer;
			// 	logFile.write(msg[1].asString ++ " (" ++ addr.ip ++ ")\n");
				{
					if (receivedMessage.findAll("\n").size>23, {
						receivedMessage=receivedMessage.copyRange(
							receivedMessage.find("\n")+1,
							receivedMessage.size-1
						);
					});
					display.string_(receivedMessage);
					//	receivedMessage.size.postln;
				}.defer;
			},
			"/chat"//,
			// "oschub.asia"
		);
		//	Document.closeAll(false);

		window.view.decorator.nextLine.shift(0, 10);
		vol = Volume.new(Server.local, persist: true);
		volSpc = [vol.min, vol.max, \db].asSpec;

		volSlider = EZSlider.new(
			window,
			490@20,
			"volume",
			volSpc,
			{arg ez;
				var newdb;
				newdb = volSpc.map(ez.value).round(0.1);
				Server.local.volume_(newdb);
			},
			volSpc.unmap(0)
		);

		window.front;
		window.onClose_({
	//		logFile.close;
			keepAlive.stop;
			for (0, defKey.size-1, {|i|
				OSCdef(defKey[i]).free;
			});
			OSCdef("speakandspell").free;
			thisProcess.recompile; // somehow, File error occurs without this
			Server.killAll;
			thisProcess.shutdown;
			0.exit;
		});
	}
}
/*
x.defKey
OSCdef.all
*/