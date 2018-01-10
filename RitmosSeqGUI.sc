
RitmosSeqGUI {

	var <>win; // window
	var <>layout;

	var <>player;
	var <>numVoices;
	var <>voices;

	*new { arg argPlayer;
		^super.new.init(argPlayer);
	}

	init { arg argPlayer;
		if(argPlayer.notNil,{
			player = argPlayer;
			numVoices = player.numVoices;
		},{ numVoices = 4 });
		this.makeGUI;
		^this;



	}

	makeGUI {
		win = GUI.window.new("Ritmos", Rect(50, 450, 800, 100*numVoices)).front;
		layout = CompositeView.new(win, Rect(2,2,800,(100*numVoices)+150)).resize_(2);

		voices = IdentityDictionary.new(numVoices);
		player.data.voices.keys.asArray.sort.do {|k|
			voices.put(k,RitmosSeqVcGUI.new(this,k));
		};
	}

}

RitmosSeqVcGUI {
	classvar <>skin, <>colors, <>paramColors;
	var <>parent, <>name;
	var <>numCells=64, vcNum;
	var <>lay1,<>lay2, <>cLay;
	var <>cells, <>edits, <>idx, <>sel;
	var <>link, boxFunc;
	var <>zoom, <>slider;
	var <>paramData, <>ctlData, <>curParam=\amp, <>rhythmLength=8;
	var <>paramSelView, <>selSizeView, <>startBeatView;
	var <defaultEditView, defaultParamsButton, <grphEditButton, <grphWindow;

	*initClass {

		skin = (
			fontSpecs:  ["Arial", 10],
			fontColor: 	Color.black,
			background: 	Color(0.8, 0.85, 0.7, 0.5),
			foreground:	Color.grey(0.95),
			onColor:		Color(0.5, 1, 0.5),
			offColor:		Color.clear,
			gap:			0 @ 0,
			margin: 		2@2,
			buttonHeight:	16,
			box: 20@20
		);

		colors = (
			yellow: Color(1.0, 0.8, 0.2, 1.0),
			gold: Color(0.6, 0.6, 0.2, 1.0),
			green: Color(0.2, 0.4, 0.2, 1.0),
			red: Color(0.6, 0.2, 0.0, 1.0),
			blue: Color(0.2, 0.4, 0.6, 1.0),
			brown: Color(0.4, 0.4, 0.2, 1.0)
		);

		paramColors = (
			amp: [Color(1.0, 0.953, 0.943, 1.0), Color(0.934, 0.481, 0.472, 1.0)],
			sus: [Color(0.953, 1.0, 0.954, 1.0), Color(0.519, 0.821, 0.604, 1.0)],
			freq: [Color(1.0, 0.951, 1.0, 1.0), Color(0.604, 0.481, 0.83, 1.0)],
			default: [Color(1.0, 1.0, 0.95, 1.0), Color(0.8, 0.8, 0.4, 1.0)]
		);

	}

	*new { arg parent, name;
		^super.newCopyArgs(parent, name).init;
	}

	init {
		numCells = parent.player.numCells;
		vcNum = parent.voices.size;
		if(parent.player.data.notNil,{
			 paramData = parent.player.data.voices[name].clv;
			 ctlData = parent.player.data.voices[name].ctl;
//		},{
//			paramData = ClaveEnvir.new(numCells);
//			paramData[\freq];	// create an array for MultiSlider display
		});
		rhythmLength = CV([1,numCells-1,\lin,1],8);
		idx = CV([0,numCells-1,\lin,1],0);
		sel = CV([3,numCells,\lin,1],0);

		this.makeGUI;
	}

	makeGUI {
		var row=0;
		cLay = CompositeView.new(parent.layout, Rect(0,vcNum*100,80,100))
			.background_(Color(0.2, 0.4, 0.6, 1.0));
		cLay.addFlowLayout;
		cLay.decorator.margin_(Point(4,0));
		StaticText.new(cLay,Rect(0,0,25,14)).string_("selSize")
		.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1])).stringColor_(colors.yellow);

		selSizeView = NumberBox.new(cLay,Rect(0,0,35,14)).font_(Font("Monaco",10));
		selSizeView.value_(1)
			.action_({|vw| sel.value_(vw.value.asInt) })
			.clipLo_(1)
			.clipHi_(numCells)
			.step_(1)
			.align_(\center);

		paramSelView = GUI.popUpMenu.new(cLay, Rect(0, 0, 60, 18) )
				.items_([\amp,\sus,\freq]).font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
				.value_(0)
				.action_({|view|
					var intVal;
					ctlData.param.value_(view.item);
				});

		StaticText.new(cLay,Rect(0,0,25,14)).string_("def ")
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
		.stringColor_(colors.yellow);
		defaultEditView = NumberBox.new(cLay,Rect(0,0,35,14))
			.font_(Font("Monaco",10))
			.align_(\center)
			.value_(paramData[curParam].default)
			.action_({|vw| this.curParamDefault_(vw.value)});

		defaultParamsButton = Button.new(cLay,33@14)
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
			.states_([["clone",colors.yellow,colors.brown]])
			.action_({
				var model = paramData[\amp].detectIndex {|amp| amp > ctlData.gateLev.value };
				var clones = paramData[\amp].copyRange(model,ctlData.cycle.value)
					.collect {|amp,i| if(amp > ctlData.gateLev.value,{i},{-1}) }
					.select {|n| (n>0) && (n != model) };
				clones.do {|n|
					paramSelView.items.do {|param|
						paramData[param].valuePut(n,paramData[param].valueAt(model));
						this.refreshCurParam
				}}
			});

		grphEditButton = Button.new(cLay,Rect(0,0,33,14))
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1]))
			.states_([["draw",colors.yellow,colors.brown]])
			.action_({|vw| var pbounds = parent.win.bounds;
				var palette, btn;
				grphWindow = Window.new("shape vc"++vcNum,
					Rect(pbounds.left+80,
						pbounds.top+((parent.voices.size-vcNum-1)*100),150,160)).front;
						// add MultiSliderView to edit paramData
				palette =  MultiSliderView.new(grphWindow,Rect(4,4,128,128))
			// unmap the value of paramData[curParam] first
			.value_(paramData[curParam].input.copyRange(0,
					ctlData.cycle.value.asInt-1))
						.elasticMode_(1)
						.drawLines_(true)
						.drawRects_(false)
						.action_({|vw|vw.currentvalue });
				btn = Button.new(grphWindow,Rect(16,138,100,20))
					.states_([[" map to param",colors.yellow,colors.brown]])
					.action_({ paramData[curParam].input_(palette.value);
						if(curParam == \freq,{ parent.player.voices[name].retuneFlg = true;
						});
						this.refreshCurParam
					})
			});
		StaticText.new(cLay,Rect(0,0,25,15)).string_("start")
			.font_(Font(skin.fontSpecs[0],skin.fontSpecs[1])).stringColor_(colors.yellow);
		startBeatView = NumberBox.new(cLay,Rect(0,0,35,14)).font_(Font("Monaco",10));
		startBeatView.value_(1)
			.action_({|vw| ctlData.startBeat.value_(vw.value-1) })
			.clipLo_(1).clipHi_(numCells).step_(1)
			.align_(\center);

		lay1 = CompositeView.new(parent.layout, Rect(80,vcNum*100,800,150)).resize_(2);
		lay2 = ScrollView.new(lay1, Rect(0, 0, 800,80))
				.hasBorder_(false)
				.hasHorizontalScroller_(false).resize_(2);
		cells = [
				Array.fill(numCells, {|i|
					NumberBox.new(lay2, Rect(i*(skin.box.x+skin.gap.x),
						0,skin.box.x,skin.box.y))
						.value_(i+1)
						.background_(colors.brown)
						.stringColor_(Color.white)
						.normalColor_(Color.gray(1.0,0.9))
						.scroll_(false)
						.align_(\center)
				}),

				Array.fill(numCells, {|i|
					NumberBox.new(lay2, Rect(i*(skin.box.x+skin.gap.x),
						(row+1)*(skin.box.y+skin.gap.y),skin.box.x,skin.box.y*0.5))
						.value_(0)
						.background_(colors.brown)
						.font_(Font("Times",7))
						.stringColor_(Color.white)
						.normalColor_(Color.gray(0.9,0.9))
						.scroll_(false)
						.align_(\center)
				}),
				Array.fill(numCells, {|i|
					NumberBox.new(lay2,
						Rect(i*(skin.box.x+skin.gap.x),(row+1.5)*(skin.box.y+skin.gap.y),
								skin.box.x,skin.box.y*0.5))
					.value_(0)
					.background_(colors.brown)
					.stringColor_(Color.white)
					.font_(Font("Times",7))
					.normalColor_(Color.gray(0.9,0.9))
					.scroll_(false)
					.align_(\center)
				})
			];
			// multislider for showing current data index
			slider = MultiSliderView(lay2, Rect(0,0,(skin.box.x+skin.gap.x)*numCells, skin.box.y))
				.value_({0}!(numCells))
				.elasticMode_(1)
				.drawLines_(false)
				.drawRects_(false)
				.isFilled_(false)
				.indexThumbSize_(skin.box.x+skin.gap.x)
				.valueThumbSize_(0)
				.isFilled_(false)
				.showIndex_(true)
				.editable_(false)
				.strokeColor_(Color.clear)
				.fillColor_(Color.gray(0.9,0.25))
				.background_(Color.clear);

			// edits are MinBoxes for scrolling or setting values
			row = row+2;
			edits = Array.fill(numCells, {|i|
				MinBox.new(lay2, Rect(i*(skin.box.x+skin.gap.x),
					row*(skin.box.y+skin.gap.y),skin.box.x,skin.box.y+10))
					.spec_(\amp)
			.round_(0.01)
					.mode_(\vert)
					.value_(paramData.at(curParam,i))
					.align_(\center)
			.step_(0.01)
			//		.relativeOrigin_(true)
			//		.clearOnRefresh_(false)
			});
			edits.do {|x| x.skin.font = Font("Arial",12) };
			parent.win.refresh;

			// zoom slider selects portion of edits to work on
			row = row+1;
			zoom = MultiSliderView(lay1, Rect(0, 80, 800, 20))
				.value_(paramData[curParam].input)
				.elasticMode_(1)
				.drawLines_(false)
				.isFilled_(true)
				.indexThumbSize_(8)
				.valueThumbSize_(0)
				.resize_(2)
				.isFilled_(true)
				.showIndex_(true)
				.strokeColor_(Color.gray(0.1,0.9))
				.fillColor_(colors.red)
				.background_(Color.gray(0.9,0.9));

			row = row+1;
			boxFunc = {|arr|
			var box = (lay2.bounds.width-cLay.bounds.width)/(sel.value);
				arr.do {|el,i|
					var pos = i*(box+skin.gap.x);
					el.bounds = el.bounds.left_(pos).width_(box);
				};
			};

			zoom.mouseDownAction = {|v,x,y,mod,btn,click|
				if(click >1) { sel.input_(1);
					idx.value_(0); v.index = 0; v.selectionSize = sel.value }
				{ 	idx.value = zoom.index; };
			};
			zoom.mouseUpAction = {|v,x,y,mod|
				if( (idx.input==0)&&{sel.input==1.0} ) { { v.index = 0; v.selectionSize = 					sel.value }.defer(0.2) };
			};
			zoom.editable_(false);
			zoom.mouseMoveAction = nil;
			zoom.mouseMoveAction = {|me,x,y,mod|
			// if exceeding end of view don't change selection size
				if(131072&mod>0) { 		// shift key
					sel.value = zoom.selectionSize.postln;
					zoom.index = idx.value;
					idx.value = idx.value;
				} {
					zoom.selectionSize = sel.value;
					idx.value = zoom.index;
				}
			};

			// set multislider zoom to proper point
			link = link.add(idx.action_({|cv| { zoom.index_(idx.value); nil }.defer }));
			// set selection size of zoom mslider
			link = link.add(sel.action_({|cv| { zoom.selectionSize_(sel.value); nil }.defer }));

			// scrollview
			// TODO fix this -- equation wrong
			link = link.add(idx.action_({|cv|
				{ lay2.visibleOrigin = ((lay2.innerBounds.width)*(idx.input))@0; nil }.defer
			}));
			// boxes -- add on for more box arrays
			link = link.add(sel.action_({|cv| { cells.do{|r| boxFunc.(r) }; nil }.defer }));
			link = link.add(sel.action_({|cv| { boxFunc.(edits); nil }.defer }));
			link = link.add(sel.action_({|cv| {
				var box = (lay2.bounds.width-cLay.bounds.width)/sel.value;
				slider.bounds = slider.bounds.width_(numCells*(box+skin.gap.x));
				slider.thumbSize_(box+skin.gap.x);
				nil
			}.defer }));

			link = link.add(rhythmLength.action_({|cv| {
				cells.do{|row|
					row.do {|cell, i|
						if(i<cv.value) {
							cell.background_(colors.gold);
						} {
							cell.background_(colors.brown);
						}
					}
				};
				nil
			}.defer }));

			lay2.onClose = { link.do(_.remove) };

			paramData.envir.keys.do {|key|
				link = link.add(paramData[key].action_({|changer,what,cv,index|
					{
						if(key == curParam,{
							if(index.notNil,{
								edits[index].value = paramData[key].valueAt(index)
							},{	// if entire CVOrder is being changed, there is no index
								cv.value.do {|val,i| edits[i].value_(val) }
							});
							zoom.value = paramData[key].input });
						nil
					}.defer
				})
			)};

			edits.do {|view,i|
				// zoom connections to data?
				view.action = {|me| paramData[curParam].valuePut(i,me.value) };
			};
		}

	index_ {|i| { slider.index_(i) }.defer }

	putEditValues_ {|idx,vals |
		{ vals.do {|val,i| edits[idx+i].value_(val) } }.defer
	}

	editValue_ {|idx, val|
		{  edits[idx].value_(val)  }.defer
	}

	editParam_ {|param|
		paramSelView.value_(paramSelView.items.indexOf(param));
		edits.do {|m| m.spec_(param) };
		curParam = param;
		{ this.refreshCurParam }.defer
	}

	refreshCurParam {
		edits.do {|box,i| box.value_(paramData[curParam][i].value) };
		zoom.value = paramData[curParam].input
	}

	curParamDefault_ {|val|
		paramData[curParam].default_(val);
		paramData[curParam].value_(val);
		this.refreshCurParam
	}

	showEvent {|cell|
		{ cells[2][cell].background_(colors.red) }.defer
	}

	showNoEvent {|cell|
		{ cells[2][cell].background_(colors.brown) }.defer
	}

}

	