
SampleBuffers	{
	var  <>server, <>sampFilePath, <>sampleFiles, <>buffers, <>numBuffers, <>buffersLoaded=false;

	*new { arg argSampFilePath, argServer;
		^super.new.init(argServer).load (argSampFilePath)
	}

	init { arg argServer;
		if( argServer.isNil,{ server = Server.local },{ server = argServer });
		buffers = [];
	}

	load	{ arg argSampFilePath;
		var file;
		if((argSampFilePath.notNil),{
			if( File.exists(argSampFilePath),{
				sampFilePath = argSampFilePath;
				file = File(sampFilePath,"r");
				sampleFiles = file.readAllString.interpret;
				file.close;
			},{
				"can't find sample directory, loading default !!".warn;
				sampFilePath = Platform.resourceDir +/+ "sounds";
				sampleFiles = [sampFilePath +/+ "/a11wlk01.wav"];
			})
		},{
			"no sample directory, loading default !!".warn;
			sampFilePath = Platform.resourceDir +/+ "sounds";
			sampleFiles = [sampFilePath +/+ "/a11wlk01.wav"];
		});
		numBuffers = 0;
		sampleFiles.do({ arg filename,i;
			Buffer.read(server,filename, action: {|buf|
				buffers = buffers.add(buf.postln);
				numBuffers = numBuffers+1;
				if(i==(sampleFiles.size-1),{ buffersLoaded=true; " buffers loaded".postln; })
			})
		})
	}

	free {
		buffers.do {|b| b.free; numBuffers = 0 }
	}

	setBufnumSpec {
		 Spec.specs.add(\bufnum->([0,numBuffers,\lin,1,0].asSpec))
	}

}

/*
File.exists("/Sounds/tektonics/skillz")
 a = SampleBuffers.new("/Users/chris/CB-SC3/samplefiles/ttenderloinSamps.rtf");
 a.numBuffers
Buffer.read(s,"/Sounds/tektonics/skillz", action: {|buf| b=buf;	})
b.bufnum
a.free
 */