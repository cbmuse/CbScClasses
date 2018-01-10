/**
	NoSpec
	Useful for when you need a spec, but don't want it to actually do anything.
	t = NoSpec.new
	t.constrain(33902)
	t.unmap(\sym)
**/
NilSpec : ControlSpec {
	*new { ^super.new.default_(nil) }
	constrain {|v| ^v }
	map {|v| ^v }
	unmap {|v| ^v }
}