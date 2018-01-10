
//+ VoicerMIDIController {
//	
//	set { arg value, divisor;
//		(destination.notNil and: { destination != defaultDest }).if({
//			destination.set(destination.spec.map(value/(divisor?127)), resync:false);
//		});
//		this.changed(this.name,value/(divisor?127))  // cb added
//	}
//
//}

+ VoicerGCProxy {
	set { arg value, updateGUI = true, latency, resync = true;
		gc.notNil.if({ gc.set(value, false, latency, resync) });
		this.changed((what: \value, val: value, param: this.name, updateGUI: updateGUI, resync: resync));
	}

}