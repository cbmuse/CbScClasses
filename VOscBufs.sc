VOscBufs  {
	classvar <>server,<>buffers, <>diamondHarms;
	
	*initClass { |thisServer |
		server = thisServer ? Server.default;
	}
	
	*makeBufs { | numBufs=8, numFrames=1024, numChannels=1 |
		buffers=Buffer.allocConsecutive(numBufs,server,numFrames,numChannels,
			{ |buf,i| var n,a; 
				n = (i+1)**2;
				a = Array.fill(n,{ arg j; (((n-j)/n).squared.round(0.001)) });
				buf.sine1Msg(a,true,true,true);
			}
		);
	}
	
	*makeDiamondBufs { | numFrames=1024 |
		var numHarms=7;
		diamondHarms = [[4,8,9,10,11,12,14],
			[4,10,11,12,14,16,18],
			[4,6,7,8,9,10,11],
			[4,7,8,9,10,11,12],
			[4,9,10,11,12,14,16],
			[4,11,12,14,16,18,20]
		];
		buffers=Buffer.allocConsecutive(diamondHarms.size*numHarms,server,numFrames,1);
		diamondHarms.flatten.size.do({|i|
			var a,harms;
			harms=diamondHarms[i/numHarms];
			a = Array.fill(i%numHarms+1,{ arg k; [harms[k%numHarms],(k+1).squared]});
			a = a.flatten;
			server.performList(\sendMsg, \b_gen, i, \sine2, 7, a.postln)
		});
	}
	
	*freeBufs {
		buffers.do({ |buf| buf.free }); buffers=nil
	}
	
}
			 