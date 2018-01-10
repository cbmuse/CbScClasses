// hub input display
HubInputHandler {
	classvar <>hData,<>hGuis,<>verbose=false;
	classvar <>senders,params,receivers,<>window,<responders;
	classvar defaultHub;

	*init	{| ...args|  // player names, as symbols
		defaultHub = ['chris','scot','phil','john','tim','matt'];
		if(args.isEmpty,{
			senders = defaultHub // default senders
		},{ senders = args });
		receivers = senders.collect {|snd|
			snd.asString.addFirst("/").asSymbol }.addFirst('/hub');
		params = [\from, \command, \args];
		HubInputHandler.initGui;
		HubInputHandler.initResp;
	}

	*initGui	{
		window = Window.new("hubInput",Rect(0, 636, 700,220));
		window.view.decorator = FlowLayout(window.view.bounds);
		window.view.background = Color(0.6,0.8,0.8);
		StaticText.new(window,Rect(0,0,60,20)).string_("");
		// create param labels
		params.do({|item,i|
			StaticText.new(window,Rect(0,0,80,20)).string_(item);
		});
		window.view.decorator.nextLine;
		// create display fields per each sender
		hGuis = ( ); hData = ( );
		receivers.do {|rcvr,i|
			StaticText.new(window,Rect(0,0,60,20);)
				.string_(rcvr.asString.split($/)[1]).align_(\right);
			hGuis.put(rcvr,
				( \senderGui: TextField.new(window,Rect(0, 0, 80,20))
				.background_(Color.grey).stringColor_(Color.white).align_(\center),
					\cmdGui: TextField.new(window,Rect(0, 0, 80,20))
				.background_(Color.grey).stringColor_(Color.white).align_(\center),
					\argsGui: TextField.new(window,Rect(0, 0, 400,20))
				.background_(Color.grey).stringColor_(Color.white).align_(\left))
			);
			// storage for latest data per receiver
			hData.put(rcvr, (\sndr: nil, \cmd: nil,\args: nil));
			window.view.decorator.nextLine;
		};
		window.front;
		window.onClose_({ responders.do {|resp| resp.remove }; responders=[] })
	}

	*initResp	{
		receivers.do {|rcvr|
			responders= responders.add(
			OSCresponderNode(nil, rcvr, { arg time, responder, msg;
				var sender, command, data, args, argsDisp;
				sender = msg[1];
				command = msg[2];
				args = msg.copy;
				args.removeAll([rcvr,sender,command]);
				if(verbose,{
					sender.post; " ".post; command.post; " ".post; args.postln });
				{ hGuis[rcvr][\senderGui].string_(sender.asString);
				hGuis[rcvr][\cmdGui].string_(command.asString);
				argsDisp = args.asString;
				argsDisp.removeAt(0); argsDisp.removeAt(argsDisp.size-1);
				hGuis[rcvr][\argsGui].string_(argsDisp) }.defer;
				hData[rcvr].put(\sndr,sender);
				hData[rcvr].put(\cmd,command);
				hData[rcvr].put(\args,args);
			}).add);
		}
	}

	showIncoming {
		thisProcess.recvOSCfunc = { |time, addr, msg|
			if(msg[0] != 'status.reply') {
				"time: % sender: %\nmessage: %\n".postf(time, addr, msg)
			}
		}
	}

	hideIncoming	{
		// stop posting.
		thisProcess.recvOSCfunc = nil;
	}


}

// create OSCresponders with replaceable response functions
HubOSCResponder {
	var <>function, <>responder;

	*new { |func|
		^super.new.init(func)
	}

	init {|func|
		this.function = func;
		this.responder = OSCresponderNode.new(nil, '/hub', { arg time, responder, msg;
			this.function.value(msg)
		}).add
	}

	newFunc	{|newFunc|
		this.function = newFunc
	}

	release { responder.remove }
}

ChrisOSCResponder {
	var <>function, <>responder;

	*new { |func|
		^super.new.init(func)
	}

	init {|func|
		this.function = func;
		this.responder = OSCresponderNode(nil, '/chris',{ arg time, responder, msg;
			this.function.value(msg)
		}).add
	}

	newFunc	{|newFunc|
		this.function = newFunc
	}

	release { responder.remove }
}

/*
HubInputHandler.init
HubInputHandler.hGuis['/hub'][\senderGui].string_(\barbera.asString)
h = HubOSCResponder.new({ "gotcha!!!!!!".postln })
x = {| ...args| args.postln };
x.value.isEmpty
h.newFunc({ "me too !!!!".postln })
~senders = ['chris','scot','phil','john','tim','mark']
~rcvs = ~senders.collect {|snd| snd.asString.addFirst("/").asSymbol.postln }.addFirst('/hub')
*/