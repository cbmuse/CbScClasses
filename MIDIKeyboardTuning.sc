
MIDIKeyboardTuning {

	var <>keybd, server;
	
	*new {|server|
		^super.new.init(server)
	}
	
	init {
		var k;
		KeyboardTunings.initClass;
		MIDIKeysVoicer.init.gui;
		this.keybd = MIDIKeysVoicer.keyboard; k = this.keybd;
		k.keyDownAction_({|note| var num = (note - k.keys[0].note);
				if(k.keys[num].inscale.not,{
					MIDIKeysVoicer.chans[0].socket.noteOn(note,127);
					k.keys[num].inscale_(true);
					k.setColor(note,Color.red)
				},{
					MIDIKeysVoicer.chans[0].socket.noteOff(note,127);
					k.keys[num].inscale_(false);
					k.removeColor(note)
				})	
			})
	}
	
	loadTunings {
		var file;
		File.openDialog("browse for tuning file, or cancel to use defaults",{|path| 
			if(path.exists,{
				file = File.new(path,"r"); 	
				KeyboardTunings.tunings = file.readAllString.interpret; file.close; 
			},{
				 KeyboardTunings.initClass; 
			});
		})
	}
	
	
}





