
ConsoleRoutine  {
	var	<>name, <>routine;
	
	*new { arg name, routine;
		^super.newCopyArgs(name, routine)
		.initRoutine(name)
	}
	
	initRoutine { arg name;
		Library.put(\consoleRoutines,name.asSymbol,this);
	}
	
	*initRoutineLib {
		ConsoleRoutine(\sDefault,{ arg console, sigNum;  // default sigRoutine
			var seq;
			seq = Pseq(#[0.1, 0.2, 0.3, 0.2], inf).asStream;
			Routine({
				loop({ console.voiceOn(sigNum); 
					{ console.voiceOff(sigNum) }.defer(0.3);
					(seq.next*4).wait;
				})
			});
		});
		
		ConsoleRoutine(\eDefault,{ arg console, effNum;  // default effRoutine
			var seq;
			seq = Prand(#[0.1,0.2,0.4,0.8], inf).asStream;
			Routine({
				loop({ console.effCtlChange(effNum,0,(seq.next));
					1.wait
				})
			});
		});
		
	}

}