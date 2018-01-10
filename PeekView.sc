/*
	w = FlowView.new;
	a = PeekView.new(w, Rect(0, 0, 300, 300), "hallo");
	d = FlowView.new(a, a.view.bounds);
	b = PeekView.new(d, Rect(0, 0, 200, 100), "yo");
	e = FlowView.new(b, b.view.bounds);
	c = SCSlider.new(e, Rect(0, 0, 40, 60));
c.remove
Pen
a.bounds
a.view.background_(Color.white)
b.view.background_(Color.blue(0.9))
b.button.background_(Color.red)
c.
	a.view.bounds
	a.view.hasBorder_(true);
	a.normalBounds
	a.buttonBounds
	a.view.bounds = a.buttonBounds
*/

PeekView : SCViewHolder {
	var <>button;
	var <>normalBounds;
	var <buttonBounds;
	var <>parent;

	// var <view -- in SCViewHolder
	
	*new { arg parent, bounds, label=" - ", makeTwoTiered=false, buttonBounds;
		^super.new.init(parent, bounds, label, makeTwoTiered, buttonBounds);
	}

	init { arg argParent, argBounds, argLabel, makeTwoTiered, argButtonBounds;
		// deal with variables
		parent = argParent.asFlowView;

		normalBounds = argBounds.asRect;
		buttonBounds = if(argButtonBounds.notNil) { argButtonBounds.asRect } { Rect(0, 0, 50, 20) };

		// make views
		button = GUI.button.new(parent, buttonBounds)
			.canFocus_(false)
			.states_([[argLabel, Color.black, Color.clear], [argLabel, Color.black, Color.clear]])
			.action_({|me|
				// if it's clicked make the view shrink to the button or show
				//	THIS DOESN'T WORK CORRECTLY -- normalBounds changes on action -- WHY?
				if(me.value==1) {
					view.bounds = Rect(0,0,0,0);
					parent.reflowAll;
					parent.resizeToFit(true, true);
					
				} {
					view.bounds = normalBounds;
					this.resizeToFit(true, true);
//					parent.resizeToFit(true, true);
					parent.reflowAll;
				}
			});
		makeTwoTiered !? { if(makeTwoTiered) { parent.startRow } };
		this.view = GUI.scrollView.new(parent, normalBounds)
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false);
//			.setProperty(\relativeOrigin,true);
		
		if(parent.children[parent.children.size-1] === view,{
			parent.children[parent.children.size-1] = this;
		},{
			Error("PeekView unexpected result : parent's last child is not my view").throw;
		});

//		view.decorator = FlowLayout.new(view.bounds, 0@0, 2@2);
		^this;
	}

	bounds { ^view.bounds }
	bounds_ { arg b; normalBounds = b; view.bounds_(b) }
	
	buttonBounds_ { arg m; buttonBounds = m; button.bounds_(m) }
	
	display { arg bool; button.valueAction_(bool.not.binaryValue) }
	
	relativeOrigin { ^true }
	
	resizeToFit { arg reflow = false,tryParent = false;
		var r;

//		r = Rect.aboutPoint(this.bounds.origin, 0, 0);
		r = Rect(0, 0, 0, 0);
		this.children.do({ |kid|
//			r = r.union(kid.tryPerform(\resizeToFit, true).bounds)
			r = r.union(kid.tryPerform(\resizeToFit, true).bounds.extent.asRect);
		});
[this.bounds, r].postln;
		this.bounds = r;
		this.view.bounds = r;
		this.view.resizeToFit;
		
		if(tryParent,{
			this.parent.tryPerform(\resizeToFit,reflow,tryParent);
		});

		^this;
	}
	
}
