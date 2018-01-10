ReacTableResponder {
	var <numObjects,<numParams,fltBounds,mixBounds,ctlBounds,syncBounds,typeBounds,<latency,
	>numPlayers=4,<>synthTbl,<>synthStates,synthMsgs,runningSynths,zombieSynths,
	aliveObj,audioConnects,aConnOrder,ctlConnects,ctlGroup,seqCtl=2,
	<syncConnects,<extSyncs,<syncsList,noSyncBus,<>controlFuncs,
	s,<>bounceThreshold=1.0,showMsg=false,startParams,deltaParams,
	<grphEngineAddr,<>bgColor=1065132;

	*new { arg numObjects=38,numParams=18,fltBounds=16,mixBounds=24,ctlBounds=28,syncBounds=36,
	latency=nil;		
	^super.newCopyArgs(numObjects,numParams,fltBounds,mixBounds,ctlBounds,syncBounds,latency)
			.initReacTableResponder
	}
	
	free {
		synthTbl.do({|item| 
			item[3].free; item[4].free;
		});
	}
	
	makeSynthTbl {
		var table;
			table = Array.fill(typeBounds[1],{[
				nil,		// synth nodeID
				\rtSaw,		// SynthDef
				\generator, // reactable type
				nil,	//  input Bus, not needed for generators, 1 for filter, 2 for mixer
				Bus.control(s,numParams+1),  // busses to control all params, + 1 control input
				[	// functions that process '/set' data from reactable to control synth params
				controlFuncs[\freq],controlFuncs[\octFrq],nil,nil,nil,nil,nil,
				controlFuncs[\pan],controlFuncs[\panDelta],nil,nil,nil,nil,nil,nil,nil,
				nil,nil],
				nil			// play routine function
			]});
			table = table ++ Array.fill(typeBounds[2]-typeBounds[1],{ // flt defaults
				[ nil,\rtBpFlt,\filter, Bus.audio(s,1),Bus.control(s,numParams+1),
				[ controlFuncs[\ffreq],controlFuncs[\octFrq],nil,nil,nil,nil,nil,
					controlFuncs[\pan],controlFuncs[\panDelta],controlFuncs[\res], 					nil,nil,nil,nil,nil,nil,nil,nil],nil]});
			table = table ++ Array.fill((typeBounds[3]-typeBounds[2]),{ // mixer defaults
				[ nil,\rtMix,\mixer, Bus.audio(s,2), Bus.control(s,numParams+1),
				[controlFuncs[\amp],controlFuncs[\amp],nil,nil,nil,nil,nil,
				controlFuncs[\pan],controlFuncs[\panDelta],nil,nil,nil,nil,nil,nil,nil,
				nil,nil],nil]});
			table = table ++ Array.fill((typeBounds[4]-typeBounds[3]),{ // control defaults
				[nil,\rtSinLfo,\ctl,nil,Bus.control(s,numParams),  // no ctl arg
				[controlFuncs[\lofreq],controlFuncs[\oct3Frq],nil,nil,nil,nil,nil,
				nil,nil,controlFuncs[\mod],nil,nil,nil,nil,nil,nil,nil,nil],nil]});
			table = table ++ Array.fill((numObjects-typeBounds[4]),{ // syncTrg defaults
				[ nil,\rtSyncTrg,\sync, nil, Bus.control(s,2),  // freqStart,freqDelta=1,ctl
				[nil,nil,nil,nil,nil,nil,nil,nil,nil,nil,nil,nil,nil,nil,nil,nil,nil,nil],nil]});
			^table
	}

	
	initReacTableResponder {
		s = Server.default;
		s.latency = latency;
		s.recChannels_(4);
		grphEngineAddr = NetAddr("127.0.0.1",3500);
		Routine({ 
			loop({ grphEngineAddr.sendMsg("/bgcolor",
				bgColor.asInt.asHexString.copyRange(2,7),5); 5.wait; }) 
		}).play;
		typeBounds = [0,fltBounds,mixBounds,ctlBounds,syncBounds];
		extSyncs = 
			[syncBounds,numObjects+syncBounds,numObjects*2+syncBounds,numObjects*3+syncBounds];
		startParams = [0,7,10,14,16];
		deltaParams = [1,8,11,15,17];
		synthMsgs=List[];
		this.initControlFuncs;
		this.initOrc;
		ctlGroup = Group.new(s,\addToTail);
		noSyncBus = Bus.control(s,1);
		s.sendMsg("/c_set",noSyncBus.index,0.0);
		synthTbl = []; 
		numPlayers.do({ synthTbl = synthTbl ++ this.makeSynthTbl });
		synthTbl;		// copy for remote player
		//	create state table
		synthStates = Array.fill(numObjects*numPlayers,{[
			0,							// mute States
			Array.fill(numParams,0),	// current param values (0->1)
			Array.fill(numParams,0),	// bounced Event param values
			0,							// last stopped-time, for debounce
			true,						// new Event flag, for debounce
			[nil,nil],			// [routine playing,[patterns playing]]
			0							//  startFlag
		]});
		//  add connection sets
		audioConnects = Set[];
		ctlConnects = Set[];
		syncConnects = Set[];
		syncsList = List[];
		aConnOrder = List[];
		this.initResponders;
		aliveObj=[];
		zombieSynths=[];
		Routine({ loop({ 5.wait; this.getRunningSynths(true) })}).play; // check for zombies
	}
	
	initResponders { 
		// also add Responders for '/new' and '/on' messages
		// doesn't care who sends
		OSCresponder(nil, '/set', { arg time, resp, msg;
			var id;
			if( showMsg,{ msg.postln; });
			id = msg[1].asInt;
			if((msg[2]!=synthStates[id][2]) && (synthTbl[id][1].asString.contains("Sync").not),{
				s.sendMsg("/n_run",synthTbl[id][0],msg[2]);
				synthStates[id].put(0,msg[2].asInt);  // 0 = mute, 1 = play
			});
			// if there are new start values, then a new connection has been made
			if(msg[3] != (synthStates[id][1][0]),{
				// check if this is a new event or a "bounce" from an old event
				if( (thisThread.seconds - (synthStates[id][3])) > bounceThreshold,{ // new Event
					"new event".postln;
					synthStates[id].put(4,true);  // flag as new Event
					startParams.do({ |i| synthStates[id][2].put(i,msg[3]) });  // save starting param data
				},{ // bounce of old Event
					"bounce event -- use previous param states".postln;
					synthStates[id].put(4,false);  // flag as bounce Event
					deltaParams.do({ |i| synthStates[id][2].put(i,  // save deltaParams
						(synthStates[id][1][i]+synthStates[id][2][i])) });
				});
			});
			(msg.size-3).do({ arg i;  // for each param 
				var thisData; thisData = msg[i+3];
				synthStates[id][1].put(i,thisData); // store all data
				if( synthTbl[id][5][i].notNil,{  // if data function exists for this param
					if( synthStates[id][4],{  // new event, use values directly from rtable
						// store function that sends data to synth, to sent at \alive msg
						synthMsgs.add({ synthTbl[id][5][i].value(id,(synthTbl[id][4].index+i),thisData);
						});
					},{ // bounced new event, adj data by older start and delta values 
						if( (startParams.includes(i)),{ // if start data
							synthMsgs.add({
							  synthTbl[id][5][i].value(id,(synthTbl[id][4].index+i),
									synthStates[id][2][i]);  }); // send old start data
						},{
							if( deltaParams.includes(i),{ // if delta data
								synthMsgs.add({ synthTbl[id][5][i].value(id,(synthTbl[id][4].index+i),
									(synthStates[id][2][i]+thisData)); }); // add to last incr
							},{	  // else send normally
								synthMsgs.add({synthTbl[id][5][i].value(id,(synthTbl[id][4].index+i),thisData);});
				})})})});
			});
		}).add;
		
		OSCresponder(nil,'/aconns',{ arg time, resp, msg;
			var src, srcport, targ, targport, parms, theseConnects, new, dead;
			if( showMsg,{ msg.postln; });
			theseConnects = Set[];
			msg.removeAt(0); msg = msg.clump(4);
			(msg.size).do({ arg i;
				src = msg[i][0];  // "asrc = ".post; src.post; "  ".post; 
				srcport = msg[i][1]; // "asrcPort = ".post; srcport.post; "  ".post; 
				targ = msg[i][2];  // "atarg = ".post; targ.post; "  ".post; 
				targport = msg[i][3];  // "atargPort = ".post; targport.post; "  ".postln;  
				theseConnects = theseConnects.add([src,srcport, targ, targport]);
			});
			dead = audioConnects.difference(theseConnects);
			//	theseConnects.post; " ".post; dead.postln;
			dead.do({ arg item;	"disconnect".postln;
				audioConnects = audioConnects.remove(item);
				this.newDisconnection(item);
				aConnOrder = aConnOrder.reject({ |oItem| oItem == item.asArray});
			});
			new = theseConnects.difference(audioConnects);
			// put the new connections in correct execution order to avoid timing problems
			new = new.asArray.sort({ |item1, item2| item1[2] == item2[0] });
			new.do({ |item|
				this.newConnection(item);
				audioConnects = audioConnects.add(item);
			});
		}).add;

		OSCresponder(nil,'/cconns',{ arg time, resp, msg;
			var src, srcport, targ, targport, parms, theseConnects, new, dead;
			if( showMsg,{ msg.postln; });
			theseConnects = Set[];
			msg.removeAt(0); msg = msg.clump(4);
			(msg.size).do({ arg i;
				src = msg[i][0];  // "asrc = ".post; src.post; "  ".post; 
				srcport = msg[i][1]; // "asrcPort = ".post; srcport.post; "  ".post; 
				targ = msg[i][2];  // "atarg = ".post; targ.post; "  ".post; 
				targport = msg[i][3];  // "atargPort = ".post; targport.post; "  ".postln;  
				theseConnects = theseConnects.add([src,srcport, targ, targport]);
			});
			dead = ctlConnects.difference(theseConnects);
			//	theseConnects.post; " ".post; dead.postln;
			dead.do({ arg item;	"disconnect".postln;
				ctlConnects = ctlConnects.remove(item);
				this.ctlDisconnection(item);
			});
			new = theseConnects.difference(ctlConnects);
			new.do({ |item|
				this.ctlConnection(item);
				ctlConnects = ctlConnects.add(item);
			});
		}).add;
		
		OSCresponder(nil,'/sconns',{ arg time, resp, msg;
			var src, targ, theseConnects, dead, new, syncSynth;
			if( showMsg,{ msg.postln; });
			theseConnects = Set[];
			((msg.size-1)/2).do({ |i| 
				theseConnects.add([msg[i*2+1],msg[i*2+2]])
			});
			//	theseConnects.postln;
			dead=syncConnects.difference(theseConnects);
			dead.do({ |item,i|
				var numConnects, thisSrc; 
				thisSrc=item[0];
				// set "sync" input of synth to busNum w/ data == 1.0
				s.sendBundle(s.latency,
					["/n_set",synthTbl[item[1]][0],"sync",noSyncBus.index]);
				// if this is last connection to this source, free src
				numConnects = syncConnects.count({ |item| item[0] == thisSrc });
				if(numConnects < 2,{ 
				//	" free sync object, and remove from syncConnects".postln;
					s.sendBundle(s.latency,["/n_free",synthTbl[thisSrc][0]]);
				});
				syncConnects.remove(item);
				syncsList.remove(item);
			});
			new=theseConnects.difference(syncConnects);
			new.do({ |item,i| var numConnects, thisSrc,thisTarg;
			//	"these are the new syncConnects ".post; new.postln;
				thisSrc=item[0]; thisTarg=item[1]; thisTarg;
				// set "sync" input of synth to control busnum of sync-obj
				s.sendBundle(s.latency,
					["/n_set",synthTbl[thisTarg.postln][0],"sync",synthTbl[thisSrc][4].index]);
				// if this is first connection from this source, start sync-obj
				numConnects=syncConnects.count({ |item| item[0] == thisSrc });
				if(numConnects == 0,{
					"start sync-synth ".post; syncSynth = s.nextNodeID.post;
				 	" ".post; synthTbl[thisSrc][1].postln;
					s.sendBundle(s.latency,
						[\s_new,synthTbl[thisSrc][1],syncSynth,1,0,
							"out",synthTbl[thisSrc][4].index,"id",thisSrc]);
					synthTbl[thisSrc].put(0,syncSynth); 
				});
				syncConnects.add(item);
				syncsList.add(item);
			});
		}).add;

		OSCresponder(nil,'/alive',{ arg time, resp, msg;
			var src, srcport, targ, targport, connInt, parms;
			if( showMsg,{ msg.postln; "".postln; });
			aliveObj=Array.new(msg.size-1);
			(msg.size-1).do({ arg i;
				aliveObj=aliveObj.add(msg[i+1])
			});
			synthMsgs.do({ |item| item.value });  // send values to synths
			synthMsgs = List[];
		}).add;
		
		OSCresponder(nil,'/bpm',{ arg time, resp, msg;
			if( showMsg,{ msg.postln; });
		}).add;
		
		OSCresponder(nil,'/clear',{ arg time, resp, msg;
			if( showMsg,{ msg.postln; });
			audioConnects.do({ |item|
				this.newDisconnection(item)
			});
		}).add;
		
		OSCresponder(s.addr,'/tr',{ arg time, resp, msg;
			if( showMsg,{ msg.postln; });
			if( synthTbl[msg[2]][2] == \ctl,{
				grphEngineAddr.sendMsg("/ctr",msg[2],"freq",msg[3]);
			},{
				if( synthTbl[msg[2]][2] == \sync,{
					grphEngineAddr.sendMsg("/sync",msg[2],msg[3]);
					syncsList.do({|item|	  // set start flag for targets of this sync obj
						if(item[0] == msg[2],{ synthStates[item[1]].put(6,1) })
					});
				});
			});
		}).add;
	}
	
	initControlFuncs { controlFuncs = Dictionary.new;
		controlFuncs.add(\freq->
			{ |id,idx,val| s.sendBundle(s.latency,["/c_set" ,idx,Spec.specs.at(\freq).map(val)])}
		);
		controlFuncs.add(\lofreq->{|id,idx,val| s.sendBundle(s.latency,
			["/c_set",idx,Spec.specs.at(\lofreq).map(val)])}
		);
		controlFuncs.add(\octFrq->
			{ |id,idx,val| s.sendBundle(s.latency,["/c_set",idx,2.pow(val)]) }
		);
		controlFuncs.add(\ffreq->{|id,idx,val| s.sendBundle(s.latency,
			["/c_set",idx,Spec.specs.at(\ffreq).map(val)])}
		);
		controlFuncs.add(\oct3Frq->{ |id,idx,val| s.sendBundle(s.latency,
			["/c_set",idx,8.pow(val)]) }
		);
		controlFuncs.add(\res->
			{ |id,idx,val| s.sendBundle(s.latency,["/c_set",idx,0.1])}    // \res default
		);
		controlFuncs.add(\pan->
			{ |id,idx, val|  s.sendBundle(s.latency,["/c_set",idx,
						Spec.specs.at(\pan).map(val)]);
			}
		);
		controlFuncs.add(\panDelta->{ |id,idx, val| var pan, cBus;
			cBus = idx - synthTbl[id][4].index;
			pan = (synthStates[id][1][cBus-1]+(val.abs.frac)).frac.fold(0,0.5);
			s.sendBundle(s.latency,
				["/c_set",idx,Spec.specs.at(\pan).map(pan)])}
		);
		controlFuncs.add(\mix->
			{|id,idx, val| s.sendBundle(s.latency,["/c_setn",idx,2,
				Spec.specs.at(\amp).map(val),Spec.specs.at(\amp).map(1-val)])
			}
		)
	}
	
	initOrc {
		SynthDescLib.global.read;
		if( SynthDescLib.global.synthDescs[\rtSaw].isNil,{
			SynthDef(\rtSaw, { arg freqStart,freqDelta=1,a2,
				a3,a4,a5,a6,a7,pan=0,a9,a10,a11,a12,a13,a14,a15,a16,a17,ctl,amp=0.25,out=0,gate=1;
				var env,freq;
				freq = freqStart*freqDelta; 
				env = EnvGen.kr(Env.asr(0.05,1,0.1), gate: gate, doneAction: 2); 
				Out.ar(out, Pan2.ar(Saw.ar(freq)*env*amp,pan));
		}).store;

			SynthDef(\rtSawM, { arg freqStart,freqDelta=1,a2,
				a3,a4,a5,a6,a7,pan,a9,a10,a11,a12,a13,a14,a15,a16,a17,ctl,amp=0.25, out=0,gate=1 ;
				var env,freq;
				freq = freqStart*freqDelta; 
				env = EnvGen.kr(Env.asr(0.05,1,0.1), gate: gate, doneAction: 2); 
				Out.ar(out, Saw.ar(freq)*env*amp);
			}).store;		
		});
		 
		if( SynthDescLib.global.synthDescs[\rtBpFlt].isNil,{
			SynthDef(\rtBpFlt,{ arg freqStart,freqDelta,a2,a3,a4,a5,a6,a7,pan,res,
				a10,a11,a12,a13,a14,a15,a16,a17,amp = 0.25,out=0,in=2,gate=1;
				var env,freq;
				freq=freqStart*freqDelta;
				env = EnvGen.kr(Env.asr(0.05,1,0.1), gate: gate, doneAction: 2);
				Out.ar(out,
					Pan2.ar(Ringz.ar(In.ar(in), freq, res,env*amp),pan)
				);
			}).store;

			SynthDef(\rtBpFltM,{ arg freqStart,freqDelta,a2,a3,a4,a5,a6,a7,pan,res,
				a10,a11,a12,a13,a14,a15,a16,a17,amp = 0.25,out=0,in=2,gate=1;
				var env,freq;
				freq=freqStart*freqDelta;
				env = EnvGen.kr(Env.asr(0.05,1,0.1), gate: gate, doneAction: 2);
				Out.ar(out, Ringz.ar(In.ar(in), freq, res,env*amp))
			}).store;

		});
	
		if( SynthDescLib.global.synthDescs[\rtMix].isNil,{
			SynthDef(\rtMix,{ arg mixStart,mixDelta,a2,a3,a4,a5,a6,a7,pan,a9,
				a10,a11,a12,a13,a14,a15,a16,a17,amp=0.5, out=0, in1=2, in2=3, gate=1;
				var env,mix1,mix2;
				mix1 = (mixStart+mixDelta); mix2=(1-mix1);
				env = EnvGen.kr(Env.asr(0.05,1,0.1), gate: gate, doneAction: 2);
				Out.ar(out,Pan2.ar((In.ar(in1)*(mix1))+(In.ar(in2)*mix2),pan,amp))
			}).store;

			SynthDef(\rtMixM,{ arg mixStart,mixDelta,a2,a3,a4,a5,a6,a7,pan,a9,
				a10,a11,a12,a13,a14,a15,a16,a17,amp=0.5, out=0, in1=2, in2=3, gate=1;
				var env,mix1,mix2;
				mix1 = (mixStart+mixDelta); mix2=(1-mix1);
				env = EnvGen.kr(Env.asr(0.05,1,0.1), gate: gate, doneAction: 2);
				Out.ar(out,(In.ar(in1)*(mix1))+(In.ar(in2)*mix2)*amp)
			}).store;
		});
		
		if( SynthDescLib.global.synthDescs[\rtSinLfo].isNil,{
			SynthDef(\rtSinLfo,{ arg freqStart,freqDelta=1,a2,a3,a4,a5,a6,a7,a8,mod,
				a10,a11,a12,a13,a14,a15,a16,a17,amp=1.0,out=0;
				var freq;
				freq = freqStart*freqDelta;
				Out.kr(out,SinOsc.kr(freq,0,mod))
			}).store;
		});
		if( SynthDescLib.global.synthDescs[\rtSyncTrg].isNil,{
			SynthDef(\rtSyncTrg,{ arg bpm,out=0;
				Out.kr(out,Impulse.kr(bpm/60))
			}).store;
		});
	}
		
	newConnection {  arg connArray;
		var synth, src, trg, prev,prevIdx,trgIdx,lastSrc,busOut,synthName,bufnum,rout,syncNum;
		"".postln; "make new Connection ".post; connArray.postln;
		src = connArray[0]; trg = connArray[2];
		if( trg == -1,{ 
			busOut = 0; 
			synthName = synthTbl[src][1] 
		},{ busOut = synthTbl[trg][3].index+connArray[3];
			synthName = synthTbl[src][1] ++ "M"
		 }); 
		if( synthName.asString.contains("FFT"),{ bufnum = FFTbufs.useBuf });
		if( synthTbl[src][2] == \generator,{ 
			"start gen-synth ".post;
			synth = s.nextNodeID.post; " ".post; synthName.postln;
			s.sendBundle(s.latency,
				[\s_new,synthName,synth,0,0,  // add to head of default group
					\out,busOut,\bufNum,bufnum,\id,src],
				[\n_mapn,synth,0,synthTbl[src][4].index,numParams+1],
				[\c_set,synthTbl[src][4].index+seqCtl,0]
			);
			synthTbl[src].put(0,synth); 
			aConnOrder = aConnOrder.addFirst(connArray);
		},{ 
			if( synthTbl[src][2] == \filter,{ "start flt-synth ".post; 
				prev = audioConnects.detect({ |item| item[2] == src });  // get prev synth, if playing already
				aConnOrder.detect({ |item, i| prevIdx = i; item == prev }); // and prev exec order index
				synth = s.nextNodeID.postln; " ".post; synthName.postln;
				if(prev.notNil,{ 
					prev = prev[0];
					"previous synth is = ".post; synthTbl[prev][0].postln;
					s.sendBundle(s.latency,
						[\s_new,synthName,synth,3,synthTbl[prev][0],    // after previous
						\in,synthTbl[src][3].index,\out,busOut,\bufNum,bufnum,\id,src], 
						[\n_mapn,synth,0,synthTbl[src][4].index,numParams+1],
						[\c_set,synthTbl[src][4].index+seqCtl,0]
					);
					aConnOrder = aConnOrder.insert(prevIdx+1,connArray);
				},{ "prev synth not yet playing".postln;
					s.sendBundle(s.latency,
						[\s_new,synthName,synth,0,0,    // add to head of default group
							\in,synthTbl[src][3].index,\out,busOut,\bufNum,bufnum,\id,src], 
						[\n_mapn,synth,0,synthTbl[src][4].index,numParams+1],
						[\c_set,synthTbl[src][4].index+seqCtl,0]
					);
					aConnOrder = aConnOrder.addFirst(connArray);
				});
				synthTbl[src].put(0, synth);
			},{  "start mix-synth ".post; 
				prev = aConnOrder.select({ |item| item[2] == src }).asList.last;
				aConnOrder.detect({ |item, i| prevIdx = i; item == prev }); // and prev exec order index
				synth = s.nextNodeID.postln;  " ".post; synthName.postln;
				if(prev.notNil,{ prev = prev[0]; // if source is already playing
					"previous synth is = ".post; synthTbl[prev][0].postln;
					s.sendBundle(s.latency,
						[\s_new,synthName,synth,3,synthTbl[prev][0],    // after previous
							\in1,synthTbl[src][3].index,\in2,synthTbl[src][3].index+1,
							\out,busOut,\bufNum,bufnum,\id,src], 
						[\n_mapn,synth,0,synthTbl[src][4].index,numParams+1],
						[\c_set,synthTbl[src][4].index+seqCtl,0]
					);
					aConnOrder = aConnOrder.insert(prevIdx+1,connArray);
				},{ "prev synth not yet playing".postln;
					s.sendBundle(s.latency,
						[\s_new,synthName,synth,0,0,
							\in1,synthTbl[src][3].index,\in2,synthTbl[src][3].index+1,
							\out,busOut,\bufNum,bufnum,\id,src],
						[\n_mapn,synth,0,synthTbl[src][4].index,numParams+1],
						[\c_set,synthTbl[src][4].index+seqCtl,0]
					);
					aConnOrder = aConnOrder.addFirst(connArray);
				});
				synthTbl[src].put(0, synth);
			});
		// if trg already playing and earlier in exec order, move it to after src
			lastSrc = audioConnects.select({ |item| item[0] == trg }).asList.last;
			if(lastSrc.notNil,{
				aConnOrder.detect({ |item,i| trgIdx = i; item == lastSrc });
				if( prev.notNil && (trgIdx < (prevIdx+2)),{
					"change order!".postln;
					s.sendBundle(0.01+(s.latency ? 0.0),[\n_after,synthTbl[lastSrc[0]][0],synth]);
					aConnOrder = aConnOrder.reject({|item,i| i == trgIdx});
					aConnOrder = aConnOrder.insert(prevIdx+2,lastSrc)
				});
			}); "aConnOrder = ".post; aConnOrder.postln;
		});
		// if synth was previously synced, and recreated in place, reconnect it to syncSynth
		syncConnects.do({|item| 
			if( (item[1] == src),{ "reset sync".postln;
				s.sendBundle(s.latency,
					["/n_set",synthTbl[item[1].postln][0],"sync",synthTbl[item[0]][4].index])
			})
		});
		if( synthTbl[src][6].notNil,{
		 	if( synthStates[src][4],{ 	// if new event, reset patterns  
				if( synthStates[src][5][1].notNil,{
					synthStates[src][5][1].do({|item| 
						// find index of syncEnvirs for this Object
						extSyncs.detect({|item,i| syncNum = i; item == src });
						ReacTablePatterns.syncEnvirs[syncNum].use({item.reset })});
				});
				synthStates[src].put(6,0); // reset start flag
			});
			rout = Routine({ this.startRoutine(src,synthTbl[src][6])}).play(SystemClock); // start routine
			synthStates[src][5].put(0,rout);  // store routine
		});
		{ this.getRunningSynths }.defer(0.4)
	}
	
	newDisconnection { arg connections;
		var thisConn; thisConn=connections[0];
		connections.postln;
		"disconnect synth ".post; synthTbl[thisConn][0].postln;
		if( synthTbl[thisConn][6].notNil,{ 
			synthStates[thisConn][5][0].stop;   // stop routine
			synthStates[thisConn].put(6,0)      // reset start flag
		}); 
		s.sendBundle(s.latency,["/n_set",synthTbl[thisConn][0],\gate,0]);
		synthTbl[thisConn].put(0,nil);
		synthStates[thisConn].put(3,thisThread.seconds)
	}
	
	ctlConnection { arg connArray;
	 	var synth,src,trg,busOut,synthName;
		"".postln; "new ctl-Connection ".post; connArray.postln;
		src = connArray[0]; trg = connArray[2];
		synth = s.nextNodeID.post;  " ".post; 
		synthName = synthTbl[src][1];  " ".post; synthName.postln;
		busOut = synthTbl[trg][4].index+numParams;
		s.sendBundle(s.latency,[\s_new,synthName,synth,0,ctlGroup.nodeID,\out,busOut,\id,src],
			[\n_mapn,synth,0,synthTbl[src][4].index,numParams]);
		synthTbl[src].put(0,synth);
	}
	
	ctlDisconnection { arg connections;
		var synthID; synthID = synthTbl[connections[0]][0];
		connections.postln;
		"disconnect ctl-synth ".post; synthID.postln;
		s.sendBundle(s.latency,["/n_set",synthID,\gate,0]);
		synthTbl[connections[0]].put(0,nil)
	}
	
	loadSynths { |synthFuncArray|		// synthFuncArray = [[num,name,func],...]
		var num,name,funcs,routine,offset;
		numPlayers.do({|i|
			offset = (i*numObjects);
			synthFuncArray.do({ |item|
				num = item[0]+offset; name = item[1]; 
				funcs = item[2]; routine = item[3];
				synthTbl[num].put(1,name);
				funcs.do({|func,j| synthTbl[num][5].put(j,controlFuncs[func])});
				synthTbl[num].put(6,routine);
			});
		});
	}
	
	startRoutine	{ arg obj,patArray; // patArray =[pitchPat,rhythmPat,bpm]
		var p,r,bpm,data=0,delay,syncPair,syncNum;
		loop({ 
			syncPair = syncsList.detect({|item| 
				((item[1] == obj) && (extSyncs.includes(item[0]))) });
			if( syncPair.isNil,{   
				synthStates[obj].put(6,0); // reset sync flag
				0.01.wait 
			},{
				if( (synthStates[obj][6] != 0),{ // wait until sync flag is set
					extSyncs.detect({|item,i|
							syncNum = i; item == syncPair[0] });
					p = ReacTablePatterns.usePattern(syncNum,patArray[0]);
					r = ReacTablePatterns.usePattern(syncNum,patArray[1]);
					bpm = ReacTablePatterns.usePattern(syncNum,patArray[2]);
					synthStates[obj][5].put(1,[p,r]);  // store patterns 
					data = p.next;
					delay = (60/bpm)*r.next;
					// only first active obj controls sync tempo
					if(syncsList[0].includes(obj),{ 
						s.sendBundle(s.latency,
							["/n_set",(synthTbl[syncPair[0]][0]),"bpm",60/delay]);
					});
		 			s.sendBundle(s.latency,
		 					["/c_set",synthTbl[obj][4].index+seqCtl,data]);
					delay.max(0.01).wait; 
				},{ 0.01.wait })
			})
		 })
	}
		
	getRunningSynths { arg zombFlg=false;
		var probe,probing,resp,nodes;
		nodes = Set.new;
		probing = List.new;
		probe = { arg nodeID;
				probing.add(nodeID);
				s.sendMsg("/n_query",nodeID);
		};
		resp = OSCresponder(s.addr,'/n_info',{ arg a,b,c;
						var cmd,nodeID,parent,prev,next,isGroup,head,tail;
						# cmd,nodeID,parent,prev,next,isGroup,head,tail = c;
						
						nodes.add(nodeID);
						if(next > 0,{
							probe.value(next);
						});
						if(isGroup==1,{
							if(head > 0,{
								probe.value(head);
							});
							nodes.remove(nodeID); // don't list groups
						});
						probing.remove(nodeID);
						if(probing.size == 0,{
							resp.remove;
						});
					}).add;
					
		probe.value(0);
		{ 	
			runningSynths = nodes.asArray;
			if( zombFlg.not,{
				if( (runningSynths.isEmpty.not),{
					"nodes on localhost: ".post; 
					runningSynths.postln; "".postln;
				})
			},{ this.freeZombies
			})
		}.defer(0.1)	
	}
	
	freeZombies {
		var zombies,freeZombies,aliveSynthIDs,recNodeID;
		aliveSynthIDs= aliveObj.collect({ |obj| synthTbl[obj][0] });
		if( s.recordNode.notNil,{ recNodeID = s.recordNode.nodeID }); 
		zombies = runningSynths.reject({|obj| aliveSynthIDs.includes(obj) }); 
		zombies = zombies.reject({|obj| zombies.includes(recNodeID) });
		if( zombies.isEmpty.not,{
			"zombie synths = ".post; zombies.postln;
			freeZombies = zombies.select({|item| zombieSynths.includes(item) });
			if( freeZombies.isEmpty.not,{ 
				"freeing zombies = ".post; freeZombies.postln; 
				freeZombies.do({ |obj| 
					s.sendMsg("/n_free",obj);
					zombieSynths.remove(obj) 
				 });
			},{ zombieSynths = zombies; // save new zombies to check at next iteration
			})
		},{ zombieSynths = []
		})
	}
	
	show { |flg=1| if( flg==1,{ showMsg=true },{ showMsg=false }) }

}

/*
//	receive only from specific NetAddr, but doesn't work!!
b = NetAddr("193.145.55.193",57120)
a = OSCresponder([b], '/set', { arg time, resp, msg;
	msg.size.postln;
}).add;
*/