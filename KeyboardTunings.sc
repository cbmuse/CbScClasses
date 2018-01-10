KeyboardTunings {
	classvar <>tunings,<centsDevs;

	*initClass	{
		"compiling tunings".postln;
		tunings = (
			// name -> TuningRatios
			lim13: [1, 13/12, 9/8, 7/6, 5/4, 4/3, 11/8, 3/2, 13/8, 5/3, 7/4, 11/6],
// A=440  [ 0, +38.6, +03.9, -43.1, -13.7, -2, -48.7, +2, +40.5, -15.6, -31.2, -50.6 ]
			lim7:  [1/1, 21/20, 9/8, 6/5, 21/16, 4/3, 7/5, 3/2, 8/5, 27/16, 9/5, 28/15],
			lim11: [1/1, 16/15, 9/8, 6/5, 11/9, 4/3, 7/5, 22/15, 8/5, 33/20, 16/9, 28/15],
			lim5: [1/1, 16/15, 9/8, 6/5, 5/4, 4/3, 45/32, 3/2, 8/5, 5/3, 9/5, 15/8 ],
			ez7lim: [1/1, 21/20, 9/8, 7/6, 5/4, 4/3, 7/5, 3/2, 14/9, 5/3, 7/4, 15/8],
			ez5lim:  [1/1, 16/15, 9/8, 6/5, 5/4, 4/3, 64/45, 3/2, 8/5, 27/16, 9/5, 15/8 ],
			pyth: [ 1, 2187/2048, 9/8, 1.2013549804688, 81/64, 1.3515243530273, 729/512, 3/2, 6561/4096, 27/16, 1.8020324707031, 243/128 ],
			triley: [ 1, 1.0666666666667, 1.125, 1.2, 1.25, 1.333, 1.422, 1.5, 1.6, 1.667, 1.778, 1.875 ],
			lyoung: [1,567/512, 9/8, 147/128, 21/16, 1323/1024, 189/128, 3/2, 49/32, 7/4, 441/256, 63/32 ],
			millsgamelan: [1, 133/128, 17/16, 9/8, 19/16, 21/16, 133/96, 3/2, 399/256, 13/8, 7/4, 57/32 ],
			obelisk: [1,28/27,10/9,7/6,5/4,21/16,45/32,3/2,14/9,5/3,7/4,15/8],
			ragamala1:  [1,15/14,9/8,6/5,9/7,9/7,9/7,3/2,8/5,12/7,9/5,15/8],

			ragamala2:  [1,16/15,10/9,7/6,5/4,4/3,4/3,3/2,14/9,8/5,5/3,16/9],

			ragamala3:  [1,21/20,15/14,8/7,6/5,21/16,7/5,10/7,3/2,8/5,12/7,7/4]
		);

		centsDevs= ();
		tunings.keysValuesDo {|name,t|
			centsDevs.put(name,KeyboardTunings.tuningCentsDev(name))
		}
	}

	*loadTunings	{
		File.openDialog("browse for tuning file, or cancel to use defaults",{|path|
			var file = File.new(path,"r");
			tunings = file.readAllString.interpret; file.close;
		},{ KeyboardTunings.initClass })
	}

	*loadTuningsFile	{|path|
		var file = File.new(path,"r");
		if(file.exists,{ file.open;
			tunings = file.readAllString.interpret; file.close;
		},{ KeyboardTunings.initClass })
	}

	*cpsmidi {|name,pitch=220|
		^(KeyboardTunings.tunings[name.asSymbol]*pitch).cpsmidi.round(0.001)
	}

	*tuningCentsDev {|name|
		//	^[0,-0.27,-0.18,-0.33,-0.14,-0.3,-0.1,0.02,-0.35,-0.16,-0.31,-0.12];
		^KeyboardTunings.tunings[name.asSymbol].collect {|rtio,i| if(rtio!=1,{
			if((rtio.cpsmidi - (1.cpsmidi)) > i,{
				(rtio.cpsmidi-1.cpsmidi).frac.round(0.001)},{
				(1-((rtio.cpsmidi-1.cpsmidi).frac.round(0.001))).neg
			})
		},{0})}
	}

}

/*

t = TuningRatios(12,tunings: KeyboardTunings.tunings[\lim13])

v = KeyboardTunings.tunings[\lim13].collect {|r| 220*r }

[ 220, 238.33333333333, 247.5, 256.66666666667, 275, 293.33333333333, 302.5, 330, 357.5, 366.66666666667, 385, 403.33333333333 ]

KeyboardTunings.tunings[\lim13].collect {|rtio,i| if(rtio*(0.midicps) >= (i.midicps),{ rtio*(i.midicps).frac.round(0.001) },{ rtio*(i.midicps).frac.round(0.001).neg })}

*/