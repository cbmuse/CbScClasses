+ Server {
	runningSynths {
			var probe,probing,resp,nodes,server,report,indent = 0,order=0;
		//	nodes = IdentityDictionary.new;
			nodes = Set.new;
			probing = List.new;
			
			probe = { arg nodeID;
				probing.add(nodeID);
				this.sendMsg("/n_query",nodeID);
			};
			("\nnodes on" + name ++ ":").postln;
				
			resp = OSCresponder(this.addr,'/n_info',{ arg a,b,c;
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
							nodes.remove(nodeID);	// don't list groups
						});
						probing.remove(nodeID);
						if(probing.size == 0,{
							resp.remove;
						});
					}).add;
					
			probe.value(0);
			{ nodes.postln }.defer(0.1)
			
	}
}

/*
 { SinOsc.ar(200) }.play
 s.runningNodes
*/