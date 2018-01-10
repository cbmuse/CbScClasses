FFTBufs  {
	classvar <>server,<>buffers,lastBufUsed= -1;
	
	*initClass { |thisServer |
		server = thisServer ? Server.default;
	}
	
	*makeBufs { | numBufs=1, numFrames=2048, numChannels=1 |
		buffers=Array(numBufs);
		numBufs.do({ |i|
			buffers.add(Buffer.alloc(server,numFrames,numChannels));
		});
	}
	
	*freeBufs {
		buffers.do({ |buf| buf.free }); buffers=nil
	}
	
	*useBuf {
		lastBufUsed=(lastBufUsed+1);
		^buffers[lastBufUsed%(buffers.size)].bufnum;
	}
	
}
			 