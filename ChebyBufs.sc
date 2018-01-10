ChebyBufs {
	classvar <>wavetables, <>buffers, <>server;
	*initClass { |thisServer|
		server = thisServer ? Server.default;
		wavetables = [[1],[0,1],[0,0,1],[0,0,0,1],[0,0,0,0,1],[0,0,0,0,0,1],
			[0,0,0,0,0,0,1],[0,0,0,0,0,0,0,1],[0,0,0,0,0,0,0,0,1],
			[0,0,0,0,0,0,0,0,0,1],[0,0,0,0,0,0,0,0,0,0,1],[0,0,0,0,0,0,0,0,0,0,0,1],
			[0,0,0,0,0,0,0,0,0,0,0,0,1],[0,0,0,0,0,0,0,0,0,0,0,0,0,1],
			[0,0,0,0,0,0,0,0,0,0,0,0,0,0,1],[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1],[1,0,1,1,0,1]
		];
	}
	

	*makeBufs {
		buffers=Array(wavetables.size);
		wavetables.do({ arg tbl,i;
			buffers.add(Buffer.alloc(server,512,1,{ arg buf; buf.chebyMsg(tbl) }));
		});
	}
	
	*freeBufs {
		buffers.do({ |buf| buf.free });
		buffers=nil;
	}
	
}
			