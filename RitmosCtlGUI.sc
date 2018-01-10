
RitmosCtlGUI {
	classvar <>skin, colors;

	var <>player, <>numVoices=4, <>voices;

	var <>win, <>layout, <>presetView, <>intGoalView, <>presetNameView, <intNameView, <>tempoView;
	var <>limitView, <>limitVoiceView, <>limitFuncView, <>countFlagView, <>countLimitFuncs,
	 	<>prstRoutView, <>syncStartView;
	var <>masterVolView, <>intSliderView;
	var <tempoIntView, <countLimIntView;
	var <pButtons, <cButtons, voiceHeight = 70;

	var <>globalData, <>globalLink, <>data;

	*initClass {
		colors = (
			yellow: Color(1.0, 0.8, 0.2, 1.0),
			gold: Color(0.6, 0.6, 0.2, 1.0),
			green: Color(0.2, 0.4, 0.2, 1.0),
			red: Color(0.6, 0.2, 0.0, 1.0),
			blue: Color(0.2, 0.4, 0.6, 1.0),
			brown: Color(0.4, 0.4, 0.2, 1.0)
		);

		skin = (
			fontSpecs:  ["Arial", 10],
			fontColor: 	Color.black,
			background: 	Color(0.8, 0.85, 0.7, 0.5),
			foreground:	Color.grey(0.95),
			onColor:		Color(0.5, 1, 0.5),
			offColor:		Color.clear,
			gap:			0 @ 0,
			margin: 		2@2,
			buttonHeight:	16
		);

	}

	*new { arg argPlayer;
		^super.new.init(argPlayer);
	}

	init { arg argPlayer;
		if(argPlayer.notNil,{
			player = argPlayer;
			numVoices = player.numVoices;
			data = player.data;
			countLimitFuncs = player.countLimitFuncs.keys.asArray;
		});

		this.makeGUI;
		^this;
	}

	// sets up entire window, including global controls and voices
	makeGUI { arg argBounds;
		var bounds = argBounds ?? {
			Rect(25, 25, 800, (60+(voiceHeight*(numVoices+1))).min(800)) };
		win = GUI.window.new("RitmosCtl", bounds).front;
		win.onClose_({ player.stop; player.seqGui.win.close; player.free;
			player.voices.do {|vc| vc.listener !? {
			//	"stop listener ".postln;
				vc.listener.stop;
			}};
		});

		layout = FlowView.new(win, win.view.bounds)
			.resize_(5);
		layout.decorator.gap = skin.gap;
		layout.decorator.margin = skin.margin;

		this.makePresetControls;

		voices = IdentityDictionary.new(numVoices);
		player.data.voices.keys.asArray.sort.do {|k|
			voices.put(k,RitmosVcCtlGUI.new(this,player,k));
			layout.hr(height: 5);
			layout.startRow
		};

		voices.do {|vc| vc.update }
	}

	makePresetControls {
		var globalHeight = 50;
		var globalLayout = PeekView.new(layout, (layout.innerBounds.width)@50, "presets", true);
		var presetWidth = 350;
		var pLay = FlowView.new(globalLayout, Rect(2, 0, presetWidth, globalHeight));
		var cLay = FlowView.new(globalLayout,
			Rect(presetWidth+2,0,layout.innerBounds.width-presetWidth-2, globalHeight));

		var buttonWidth = 20;
		var buttonHeight = 20;

		var presetTitles = ["<",">","St","Rp","Rm","Ld","Sv","rL","rS"];
		var presetActions = [
			{|view| if(player.isRunning.not,{ player.prevPreset
				},{ player.changePresetNum = (presetView.value.asInt-1)
							.clip(1,player.sequence.value.size) })},
			{|view| if(player.isRunning.not,{ player.nextPreset
				},{ player.changePresetNum =  (presetView.value.asInt+1)
							.clip(1,player.sequence.value.size) })},
			{|view| player.storePreset(player.presetName.value) },
			{|view| player.replacePreset(presetView.value.asInt) },
			{|view| player.deletePreset(presetView.value.asInt)  },
			{|view| player.loadPresetsDialog },
			{|view| player.savePresetsDialog },
			{|view| player.reloadPreset },
			{|view| player.resavePreset }
		];

		var controlStates = [[["play"],["stop"]],[["pause"],["resume"]], [["mute"], ["unmute"]]];
		var controlActions = [
			{|view| if(view.value==1) { player.play } { player.stop } },
			{|view| if(view.value==1) { player.pause } { player.resume } },
			{|view| if(view.value==1) { player.mute } { player.unmute } }
		];

		presetNameView = TextField.new(pLay, Rect(0,0,80,20))
			.align_(\center)
			.string_(\pre0).font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
			.action_({|view|
				if(player.sequence.value.includes(view.value.asSymbol),{
					ModalDialog.new({ arg layout;
					ActionButton(layout, "Name already exists .. load Preset?")
					}, name: "Preset Name Change",
					okFunc: { player.loadPreset(view.value) },
					cancelFunc: { view.value_(player.presetName.value) })
				},{
					if(view.value.copyRange(0,2) != "pre",{
						player.presetNameChange(view.value)
					},{ "can't rename starting with 'pre' ".postln;
						view.value_("")
					})
				})
			});

		presetView = NumberBox.new(pLay, Rect(0, 0, 30, 20) )
			.step_(1)
			.align_(\center)
			.value_(1)
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
		.action_({|vw| if(player.isRunning.not,{
			player.loadPreset(((vw.value.asInt).clip(1,player.sequence.value.size)))
			},{ player.changePresetNum=(vw.value.asInt).clip(1,player.sequence.value.size) } )
		});

		pButtons = presetTitles.collect {|name, i|
			var res = GUI.button.new(pLay, Rect(0, 0, buttonWidth, buttonHeight) )
				.states_([[name]])
				.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
				.action_(presetActions[i]);
			res;
		};

		pLay.startRow;

		intNameView = GUI.popUpMenu.new(pLay, Rect(0, 0, 80, 20) )
			.items_([\pre1])
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
			.action_({|menu|
				var goal;
				goal = player.sequence.value.indexOf(menu.items[menu.value]);
				if(goal.notNil,{
					data.global.ctl.intGoal.value_(player.sequence.value[goal])
				})
			});

		intGoalView = NumberBox.new(pLay, Rect(0, 0, 30, 20) )
			.step_(1)
			.align_(\center)
			.value_(1)
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
			.action_({|vw| data.global.ctl.intGoal
			.value_(player.sequence.value[(vw.value.asInt-1)
				.clip(0,player.sequence.value.size-1)])
			});

		GUI.button.new(pLay, Rect(0, 0, buttonWidth, buttonHeight) )
			.states_([["<"]])
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
			.action_({|view| 	player.prevGoal
			});

		GUI.button.new(pLay, Rect(0, 0, buttonWidth, buttonHeight) )
			.states_([[">"]])
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
			.action_({|view|  player.nextGoal
			});

		intSliderView = GUI.slider.new(pLay, Rect(0, 0, 124, 20) )
			.action_({|view|
				player.interpValue.value_(view.value);
			});

		syncStartView = Button.new(pLay, Rect(0,0,35,20))
			.states_([["free",Color.black, Color.grey(0.95)],
				["sync",Color.black, Color.red(1.0,0.5)]])
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
			.action_({|view|
				if(view.value==0,{ data.global.ctl.syncStart.value_(false)
				},{ data.global.ctl.syncStart.value_(true) })
			});

		tempoView = RitmosCtlGUI.makeEasyNumBox(cLay, Rect(0, 0, 100, 20), "tempo",
			{|view| 	player.changeTempo(view.value) },
			60, 0, inf, 1
			, envir: data.global.interp
		);
		tempoIntView = tempoView[\switch];

		cButtons = controlStates.collect {|state, i|
			var res = GUI.button.new(cLay, Rect(0, 0, buttonWidth*2, buttonHeight) )
				.states_(state)
				.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
				.action_(controlActions[i]);
			res;
		};

		masterVolView = EZSlider.new(cLay, 180@18, "masterVol", [0,3,\amp,0],
			{|me| player.masterVol.value_(me.value) },
			0.5, false, 55, 30
		);  masterVolView.numberView.align_(\center);
			masterVolView.labelView.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]));

		cLay.startRow;

		GUI.staticText.new(cLay, Rect(0, 0, 20, buttonHeight) )
			.string_("seq ")
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]));
		countFlagView = GUI.button.new(cLay, Rect(0, 0, buttonWidth, buttonHeight) )
				.states_([[" "],["X"]])
				.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
				.action_({|view|
					player.countFlg.value_(view.value>0)
				})
				.value_(if(player.countFlg.value.postln,{1},{0}));

		limitVoiceView = GUI.popUpMenu.new(cLay, Rect(0, 0, 50, buttonHeight) )
			.items_(player.voices.keys.asArray.sort)
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
			.action_({|view|
				data.global.ctl.countVc.value_(view.items[view.value]);
			});

		limitView = RitmosCtlGUI.makeEasyNumBox(cLay, Rect(0, 0, 80, 20), "rpt ",
			{|view|
				data.global.ctl.countLimit.value_(view.value)
			},
			100, 1, inf, 1, 0.4, 0.5
			, envir: data.global.interp
		);
		countLimIntView = limitView[\switch];

		limitFuncView = GUI.popUpMenu.new(cLay, Rect(0, 0, 80, buttonHeight) )
			.items_(countLimitFuncs)
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
			.action_({|view|
				data.global.ctl.limitFunc.value_(view.items[view.value])
			});

		GUI.staticText.new(cLay, Rect(0, 0, 50, buttonHeight) )
			.string_(" routine ")
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]));
		prstRoutView = GUI.popUpMenu.new(cLay, Rect(0, 0, 80, buttonHeight) )
			.items_(player.presetRoutines.keys.asArray.sort)
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
			.action_({|view|
				data.global.ctl.presetRout.value_(view.items[view.value])
			});


		layout.startRow;

		globalLink = [];
		globalLink = globalLink.add(player.masterVol.action_({|cv|
						{ masterVolView.value_(cv.value) }.defer }) );
		globalLink = globalLink.add(data.global.ctl.tempo.action_({|cv|
						{ tempoView.view.value_(cv.value) }.defer }) );
		globalLink = globalLink.add(data.global.ctl.countLimit.action_({|cv|
						{ limitView.view.value_(cv.value) }.defer }) );
		globalLink = globalLink.add(data.global.ctl.limitFunc.action_({|ov|
			{ limitFuncView.value_(limitFuncView.items.indexOf(ov.value)) }.defer
						}));
		globalLink = globalLink.add(data.global.ctl.presetRout.action_({|ov|
			if(ov.value.notNil,{
				{ prstRoutView.value_(prstRoutView.items.indexOf(ov.value)) }.defer  })
						}));
		globalLink = globalLink.add(player.presetNum.action_({|pnum|
			{ presetView.value_(pnum.value) }.defer
						}));
		globalLink = globalLink.add(player.presetName.action_({|name|
			{ presetNameView.value_(name.value) }.defer
						}));
		globalLink = globalLink.add(player.countFlg.action_({|ov|
			{ countFlagView.value_(if(ov.value,{1},{0})) }.defer
						}));
		globalLink = globalLink.add(data.global.ctl.countVc.action_({|pnum|
			{ limitVoiceView.value_(limitVoiceView.items.indexOf(pnum.value)) }.defer
						}));
		globalLink = globalLink.add(player.sequence.action_({|list|
			{ intNameView.items_(player.sequence.value.asArray) }.defer
						}));
		globalLink = globalLink.add(data.global.ctl.intGoal.action_({|name|
			if( player.sequence.value.indexOf(name.value).notNil,{
				{ 	if(intNameView.items.includes(name.value).not,{
					 	intNameView.items.add(name.view);					});
					intNameView.value_(intNameView.items.indexOf(name.value));
					intGoalView.value_(player.sequence.value.indexOf(name.value)+1)
				}.defer
			},{ data.global.ctl.intGoal.value_(player.sequence.value
										[player.presetNum.value.asInt])
			})
		}));
		globalLink = globalLink.add(player.interpValue.action_({|pnum|
			player.interpolate;
			{ intSliderView.value_(pnum.value) }.defer
						}));
		globalLink = globalLink.add(data.global.interp.tempo.action_({|bool|
			if(bool.value == false,{ { tempoIntView.value_(0)}.defer },
				{ {tempoIntView.value_(1)}.defer  })
						}) );
		globalLink = globalLink.add(data.global.interp.countLimit.action_({|bool|
			if(bool.value == false,{ { countLimIntView.value_(0)}.defer },
				{ {countLimIntView.value_(1)}.defer  })
						}) );
		globalLink = globalLink.add(data.global.ctl.syncStart.action_({|bool|
			if(bool.value == false,{ { syncStartView.value_(0)}.defer
			},{ {syncStartView.value_(1)}.defer  })
						}) );

		layout.onClose_({ globalLink.do(_.remove) });

	}

	// make small button that mutes or activates interpolation
	*makeInterpolateSwitch { arg lay, name, bounds, envir;
		var res;

		res = GUI.button.new(lay, bounds ?? { Rect(0,0,10,16) })
			.states_([
				["", Color.white, Color.red(1.0, 0.5)],
				["", Color.white, Color.blue(1.0, 0.5)]
			])
			.canFocus_(false)
			.value_(0)
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
			.action_({ |me|
				if(me.value==0) {
					"interpolating % muted\n".postf(name);
					envir !? { envir[name.asSymbol].value_(false) };
				} {
					"interpolating % activated\n".postf(name);
					envir !? { envir[name.asSymbol].value_(true) };
				}
			});
		^res;
	}

	// make NumberBox with label and interpolate switch
	// assumes FlowView as parent
	*makeEasyNumBox { arg lay, bounds, name, action,
					value=0, clipLo=0, clipHi=1, step=0.05,
					labelRatio=0.5, numberRatio=0.4, envir;
		var res, width, height;

		width = ((bounds.asRect.width-12) - (skin.gap.x));
		height = bounds.asRect.height;
		res = (
// TODO figure out good fixed sizes, don't use labelRatio
			text: GUI.staticText.new(lay, Rect(0,0,width*labelRatio, height))
				.string_(name)
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
				.align_(\right),

			view: NumberBox.new(lay, Rect(0,0,width*numberRatio, height))
				.action_(action)
				.value_(value)
				.clipLo_(clipLo)
				.clipHi_(clipHi)
				.step_(step)
				.align_(\center)
				.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]));
		);
		envir !? {		// if no envir, no switch!
			res.put(\switch,RitmosCtlGUI.makeInterpolateSwitch(lay, name,
								Rect(0,0,10,height), envir: envir))
		};

		^res;
	}
}

RitmosVcCtlGUI {
	var <>parent, <>player,<>name, vcData;
	var <>skin, <>layout, <>controlLayout;
	var <>paramKeys, <>paramColors;
	var voiceHeight = 70, <row=0, <>numCells;
	var <>ctlData, <>link;


	var <>paramSelectView, <paramIntView, <>xfrmFuncSelectView;
	var <>weightView, <>numGenView, <>mutRateView, <>gateView, <>breedFlgView, <>inChanView;
	var <>countView, <>levelView, <>susDurView, <>cycleView, <>beatDivView, <>instSelectView;
	var <>tonicSelectView, <>tuningSelectView,<>muteView;
	var <wIntView, <genIntView, <mutIntView, <gateIntView, <cycleIntView,
			<beatDivIntView, <levelIntView;

	*new { arg parent, player, name;
		^super.newCopyArgs(parent, player, name).init;
	}

	init {

		vcData = player.data.voices[name];

		// provide default data for testing
		if(player.isNil,{
			ctlData = (
				weights: OV([0.5,0,0.5],{|v| v = v.asArray;
					if(v.size!=3,{if(v.size<3,{v=v.wrapExtend(3)},{v=v.copyRange(0,2)})});
					 v=v.normalizeSum}),
				numGen: CV([0,100,\lin,1],4),
				mutRate: CV([0,1.0,\lin,0],0.25),
				gateLev: CV([0,1.0,\lin,0],0.25),
				xfrmFunc: OV(\echo,(_.asSymbol)),
				inst: OV(\pluckR, (_.asSymbol)),
				level: CV([0.001,3.0,\amp,0],0.5),
				mute: CV([0,1,\lin,1],1),
				cycCtr: CV([1,1000,\lin,1],1),
				cycle: CV([1,64,\lin,1],8),
				beatDiv: CV([1,32,\lin,1],4)
				, param: OV(\amp, (_.asSymbol))
			)
		// otherwise normally get it from RitmosPlay
		},{ ctlData = vcData.ctl });

		skin = (
			// rhythm colors
			colors: (
				yellow: Color(1.0, 0.8, 0.2, 1.0),
				gold: Color(0.6, 0.6, 0.2, 1.0),
				green: Color(0.2, 0.4, 0.2, 1.0),
				red: Color(0.6, 0.2, 0.0, 1.0),
				blue: Color(0.2, 0.4, 0.6, 1.0),
				brown: Color(0.4, 0.4, 0.2, 1.0),
				rest: Color.grey(0.0, 0.8),
				wait: Color.yellow(1.0, 0.5),
				active: Color.green(1.0, 0.5),
				event: Color.red(0.5, 0.5),
				cellText: Color.white
			),
			fontSpecs:  ["Arial", 10],
			box: 20@20,
			gap: 2@2,
			margin: 2@2
		);

		paramKeys = [\amp, \sus, \freq];
		numCells = player.numCells;

		this.makeGUI;

		^this
	}

	makeGUI {

		layout = FlowView.new(parent.layout, (parent.layout.bounds.width)@(voiceHeight) );
		layout.decorator.gap = this.skin.gap;
		layout.decorator.margin = this.skin.margin;
		layout.resize_(1);		//	.relativeOrigin_(true);
		layout.startRow;

		controlLayout = PeekView.new(layout,
			Rect(0, 0, layout.innerBounds.width,voiceHeight), name.asString,
				true, Rect(0, 0, 36, 16) );
		controlLayout.hasBorder_(false).resize_(1);

		weightView = GUI.multiSliderView.new(controlLayout, Rect(0, 0, 120, 40) )
			.canFocus_(false)
			.value_([0.5, 0.0, 0.5])
			.indexIsHorizontal_(false )
			.gap_(4)
			.indexThumbSize_(10)
			.valueThumbSize_(20)
			.isFilled_(true)
			.colors_(Color.clear, Color.green(1.0, 0.5))
			.background_(Color.black)
			.action_({|me|
				var newValues;
				newValues = me.value.normalizeSum;
				if(newValues[me.index]>=0.99) { newValues = newValues.normalize(0.01, 0.99) };
				if( newValues.includes(0/0).not ) {
					me.value_(newValues.normalizeSum);
				} {
					me.value_([0.333, 0.333, 0.333]);
				};
				ctlData.weights.input_(newValues.copy);
			});

		Array.fill(3, {|i|
			GUI.staticText.new(controlLayout, Rect(2, (14*i), 20,12))
				.string_(["in","cur","clv"][i])
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
				.stringColor_(Color.gray(1.0,0.5));
		});

			wIntView = RitmosCtlGUI.makeInterpolateSwitch(controlLayout,
			"weights", Rect(120, 0, 10, 39)
				, envir: vcData.interp
			);

		controlLayout.flow({ arg gaValLayout;

			numGenView = RitmosCtlGUI.makeEasyNumBox(gaValLayout, Rect(0, 0, 110, 12), "numGen",
				{|me|
					ctlData.numGen.value_(me.value)
				},
				4, 1, 64, 1
				,envir: vcData.interp
			);
			genIntView = numGenView[\switch];

			gaValLayout.startRow;

			mutRateView = RitmosCtlGUI.makeEasyNumBox(gaValLayout, Rect(0, 0, 110, 12), "mutRate",
				{|me|
					ctlData.mutRate.value_(me.value)
				},
				0.25, 0, 1, 0.05
				,envir: vcData.interp
			);
			mutIntView = mutRateView[\switch];

			gaValLayout.startRow;

			gateView = RitmosCtlGUI.makeEasyNumBox(gaValLayout, Rect(0, 0, 110, 12), "gateLev",
				{|me|
					ctlData.gateLev.value_(me.value)
				},
				0.25, 0, 1, 0.05
				,envir: vcData.interp
			);
			gateIntView = gateView[\switch];

		}, Rect(128, 0, 128, 66) );

		controlLayout.flow({ arg gaValLayout;

			breedFlgView =  GUI.button.new(gaValLayout, Rect(0, 0, 20, 20) )
				.states_([["", Color.white, Color.black],
					["ch#", Color.black, Color.green(0.5)]])
				.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
				.canFocus_(false)
				.value_(0)
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
				.action_({|view|
					if(view.value>0,{
						ctlData.breedFlg.value_(true);
						if(player.isRunning,{player.voices[name].startBreeder});
					},{ ctlData.breedFlg.value_(false);
						player.voices[name].stopBreeder
					});
				});

			gaValLayout.startRow;

			inChanView = NumberBox.new(gaValLayout, Rect(0,0,20,20)).align_(\center)
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
				.action_({|me| ctlData.inChan.value_(me.value) })
				.value_(1).clipLo_(1).clipHi_(16).step_(1);

		}, Rect(240,0, 30, 66) );

		controlLayout.flow({ arg claveLayout;

			GUI.staticText.new(claveLayout, Rect(0, 0, 40, 18) ).string_("xfrm ")
			.align_(\center).font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]));
			xfrmFuncSelectView = GUI.popUpMenu.new(claveLayout, (70@18))
				.items_(RitmosXfrm.funcs.keys.asArray.sort)
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
				.action_({|me| player.voices[name].unmute; ctlData.xfrmFunc.value_(me.item) });

			GUI.staticText.new(claveLayout, Rect(0, 0, 40, 18) ).string_("inst ")
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1])).align_(\center);
			instSelectView = PopUpMenu.new(claveLayout, (100@18)).items_(player.instrumentList)
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
				.action_({|me|
					var newParams;
					ctlData.inst.value_(me.item);
					newParams = SynthDescLib.global.synthDescs[me.item]
								.controlNames.collect {|it| it.asSymbol }.add(\sus);
					newParams.remove(\gate); newParams.remove(\susTime);
					paramSelectView.items_(newParams);
					newParams.do {|param| vcData.clv[param] };  // activate CVOrders for each param
					player.seqGui.voices[name].paramSelView.items_(newParams)
				});

			claveLayout.startRow;

			countView = RitmosCtlGUI.makeEasyNumBox(claveLayout, Rect(0, 0, 100, 18), "count ",
				{|me| ctlData.cycCtr.value_(me.value) },
				1, 1, inf, 1, 0.5, 0.4
	//			no envir, no interpolation of count values
			);

			cycleView = RitmosCtlGUI.makeEasyNumBox(claveLayout,
				Rect(0, 0, 90, 18), "cycle ",
					{|me| ctlData.cycle.value_(me.value) },
				ctlData.cycle.value, 1, numCells, 1, 0.5, 0.4
					, envir: vcData.interp
			);
			cycleIntView = cycleView[\switch];

			beatDivView = RitmosCtlGUI.makeEasyNumBox(claveLayout, Rect(0, 0, 100, 18), "beatDiv",
				{|me| ctlData.beatDiv.value_(me.value);
				},
				4, 1, 32, 1, 0.5, 0.4
				, envir: vcData.interp
			);
//			beatDivView.view.align_(\left);
			beatDivIntView = beatDivView[\switch];

		}, Rect(260, 0, 350, 66) );

		controlLayout.flow({|paramLayout|

			tonicSelectView = GUI.popUpMenu.new(paramLayout, Rect(0, 0, 40, 18) )
				.items_([\C,\Db,\D,\Eb,\E,\F,\Gb,\G,\Ab,\A,\Bb,\B])
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
				.value_(0)
				.action_({|view|
					ctlData.tonic.value_(view.item);
					player.voices[name].tuning_(ctlData.tuning.value);  // create a new TuningRatios
				});

			tuningSelectView = GUI.popUpMenu.new(paramLayout, Rect(0, 0, 55, 18) )
				.items_([\none,\eq12] ++(KeyboardTunings.tunings.keys.asArray))
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
				.value_(0)
				.action_({|view|
					ctlData.tuning.value_(view.item);
					player.voices[name].tuning_(view.item);  // create a new TuningRatios
				});

			GUI.staticText.new(paramLayout, Rect(0, 0, 40, 18) )
				.string_(" param ").align_(\center).font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]));
			paramSelectView = GUI.popUpMenu.new(paramLayout, Rect(0, 0, 65, 18) )
				.items_(paramKeys).font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
				.value_(0)
				.action_({|view|
					var intVal;
					ctlData.param.value_(view.item);
					player.seqGui.voices[name].editParam_(view.item);
					intVal = vcData.interp[view.item].value;
					if(intVal.isNil,{ paramIntView.value_(0) },{
						if(intVal,{paramIntView.value_(1)},{paramIntView.value_(0)})
					});
				});

			paramIntView = GUI.button.new(paramLayout, Rect(0,0,10,18))
				.states_([
					["", Color.white, Color.red (1.0, 0.5)],
					["", Color.white, Color.blue (1.0, 0.5)]
				])
				.canFocus_(false).font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
				.value_(0)
				.action_({ |me|
					var envir = vcData.interp;
					if(me.value==0) {
						"interpolating % muted\n".postf(paramSelectView.item);
						if(envir.notNil,{
							if( envir[paramSelectView.item.asSymbol].isNil,{
								envir.put(paramSelectView.item.asSymbol,
												OV.new(false,(_.asSymbol)))
							},{ envir[paramSelectView.item.asSymbol].value_(false)})});
					} {
						"interpolating % activated\n".postf(paramSelectView.item);
						if(envir.notNil,{
							if( envir[paramSelectView.item.asSymbol].isNil,{
								envir.put(paramSelectView.item.asSymbol,
												OV.new(true,(_.asSymbol)))
							},{ envir[paramSelectView.item.asSymbol].value_(true)})});
					};
				});

			muteView = Button.new(paramLayout, 16@18)
			.states_([
				["",Color.white,Color.green(1.0,0.25) ],
				["m", Color.white,Color.red(1.0,0.25)]
				]).font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
			.value_(0)
			.action_({|b|
				if(b.value==0,{ctlData.mute.value_(false)},{ctlData.mute.value_(true)})});

			susDurView = Button.new(paramLayout, 18@18)
				.states_([
					["sus",Color.black],["dur",Color.white,Color.blue]
				]).font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
				.action_({|b|
					switch(b.value,
						0,{ctlData.susDur.value_(false)},
						1,{ctlData.susDur.value_(true)});
				});

			levelView = EZSlider.new(paramLayout, 170@18, "level ", [0,3,\amp,0],
				{|me| ctlData.level.value_(me.value) },
				0.5, false, 25, 35
			);
			levelView.numberView.align_(\center);
			levelView.labelView.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]));
			levelIntView = RitmosCtlGUI.makeInterpolateSwitch(paramLayout, "level", Rect(0,0,10,18)
				,envir: vcData.interp
			);

		}, Rect(550, 0, 230, 66));

		// adjust gen font sizes
		numGenView[\view].font_(Font("Times",9));
		mutRateView[\view].font_(Font("Times",9));
		gateView[\view].font_(Font("Times",9));


		//	add links from ctlData CVs to guis

		link = [];
		link = link.add(ctlData.numGen.action_({|cv|
						{ numGenView.view.value_(cv.value) }.defer }) );
		link = link.add(ctlData.mutRate.action_({|cv|
						{ mutRateView.view.value_(cv.value) }.defer }) );
		link = link.add(ctlData.gateLev.action_({|cv|
						{ gateView.view.value_(cv.value) }.defer }) );
		link = link.add(ctlData.breedFlg.action_({|cv|
					{ if(cv.value == true,{ breedFlgView.value_(1) },
								{ breedFlgView.value_(0) }) }.defer }) );
		link = link.add(ctlData.inChan.action_({|cv|
						{ inChanView.value_(cv.value) }.defer }) );
		link = link.add(ctlData.level.action_({|cv|
						{ levelView.value_(cv.value) }.defer }) );
		link = link.add(ctlData.cycCtr.action_({|cv|
						{ countView.view.value_(cv.value) }.defer }) );
		link = link.add(ctlData.cycle.action_({|cv|
			{ cycleView.view.value_(cv.value);
				player.seqGui.voices[name].selSizeView.valueAction_(cv.value) }.defer }) );
		link = link.add(ctlData.beatDiv.action_({|cv|
						{ beatDivView.view.value_(cv.value.asFloat.asStringPrec(4).asFloat) }.defer }) );
		link = link.add(ctlData.inst.action_({|ov|
				{ instSelectView.value_(instSelectView.items.indexOf(ov.value)) }.defer
						}) );
/*		link = link.add(ctlData.startBeat.action_({|cv|
				{ player.seqGui.voices[name].startBeatView.value_((cv.value+1)) }.defer
						}) );*/
		link = link.add(ctlData.tuning.action_({|cv|
				{ tuningSelectView.value_(tuningSelectView.items.indexOf(cv.value))}.defer;
					player.voices[name].tuning_(cv.value);
						}) );
		link = link.add(ctlData.tonic.action_({|cv|
				{ tonicSelectView.value_(tonicSelectView.items.indexOf(cv.value))}.defer;
					player.voices[name].tuning_(ctlData.tuning.value);
						}) );
		link = link.add(ctlData.param.action_({|ov|
			var bool;
			if(paramSelectView.items.includes(ov.value),{
				{ paramSelectView.value_(paramSelectView.items.indexOf(ov.value));
				  player.seqGui.voices[name].editParam_(ov.value);
				  paramIntView.value_(0);	// first turn off paramIntView btn
					// but turn it on again if curParam is on in interp envir
				  if( vcData.interp[ov.value].notNil,{ 						bool = vcData.interp[ov.value].value;
						if(bool,{bool=1},{bool=0});
						paramIntView.value_(bool)}) }.defer
				  })
			}) );
		link = link.add(ctlData.susDur.action_({|ov|
				{ susDurView.value_(if(ov.value,{1},{0}) ) }.defer
			}));
		link = link.add(ctlData.xfrmFunc.action_({|ov|
				{ xfrmFuncSelectView.value_(xfrmFuncSelectView.items.indexOf(ov.value)) }.defer
						}) );
		link = link.add(ctlData.weights.action_({|cv|
				{ weightView.value_(cv.value) }.defer
						}) );

		link = link.add(vcData.interp.cycle.action_({|bool|
			if(bool.value == false,{ { cycleIntView.value_(0)}.defer },
				{ {cycleIntView.value_(1)}.defer  })
						}) );

		link = link.add(vcData.interp.beatDiv.action_({|bool|
			if(bool.value == false,{ { beatDivIntView.value_(0)}.defer },
				{ {beatDivIntView.value_(1)}.defer  })
						}) );

		link = link.add(vcData.interp.level.action_({|bool|
			if(bool.value == false,{ { levelIntView.value_(0)}.defer },
				{ {levelIntView.value_(1)}.defer  })
						}) );

		link = link.add(vcData.interp.numGen.action_({|bool|
			if(bool.value == false,{ { genIntView.value_(0)}.defer },
				{ {genIntView.value_(1)}.defer  })
						}) );

		link = link.add(vcData.interp.mutRate.action_({|bool|
			if(bool.value == false,{ { mutIntView.value_(0)}.defer },
				{ {mutIntView.value_(1)}.defer  })
						}) );

		link = link.add(vcData.interp.gateLev.action_({|bool|
			if(bool.value == false,{ { gateIntView.value_(0)}.defer },
				{ { gateIntView.value_(1)}.defer  })
						}) );

		link = link.add(vcData.interp.weights.action_({|bool|
			if(bool.value == false,{ { wIntView.value_(0)}.defer },
				{ { wIntView.value_(1)}.defer   })
						}) );

		link = link.add(vcData.interp.param.action_({|bool|
			if(bool.value == false,{ { paramIntView.value_(0)}.defer },
				{ { paramIntView.value_(1)}.defer  })
						}) );

		link = link.add(vcData.ctl.mute.action_({|bool|
			if(bool.value == false,{ { muteView.value_(0)}.defer },
				{ { muteView.value_(1)}.defer  })
						}) );

		layout.onClose = { link.do(_.remove) };

	}

	//	rewrite data to update gui
	update	{
		ctlData.keys.do {|key| ctlData[key].value_(ctlData[key].value) }
	}


}
