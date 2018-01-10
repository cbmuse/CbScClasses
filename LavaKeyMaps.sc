LavaKeyMaps {
	classvar <>sectionMaps,<>cueMap,<>clicks, <>curSec;
	
	*initClass {
		curSec = 0;
		sectionMaps = [
			// lava1-1
			Dictionary[\36-> 9,\37->9,\38->2,\39->3,\40->2,\41->5,\42->49,\43->50,\44->43,			\45->46,\46->47,\47->45,\48->51,\49->51,\50->27,\51->32,\52->31,\53->15,\54->35,			\55->14,\56->14,\57->27,\58->227,\59->226,\60->233,\61->233,\62->225,\63->225,
			\64->214,	\65->214,\66->90,\67->85,\68->83,\69->58,\70->80,\71->81,\72->58,\73->80,
			\74->142,\75->150,\76->139,\77->156,\78->158,\79->144,\80->156,\81->144,\82->144,
			\83->181,\84->199,\85->171,\86->189,\87->191,\88->187,\89->203,\90->203,\91->203,
			\92->191,\93->119,\94->128,\95->135,\96->115,\97->115,\98->102,\99->102,\100->124],
			// lava1-2
			Dictionary[\36->8,\37->8,\38->0,\39->2,\40->11,\41->5,\42->49,\43->50,\44->49,
			\45->50,\46->49,\47->50,\48->46,\49->46,\50->32,\51->32,\52->15,\53->15,\54->35,
			\55->35,\56->34,\57->21,\58->228,\59->228,\60->226,\61->226,\62->226,\63->225,
			\64->225,\65->225,\66->85,\67->87,\68->88,\69->83,\70->70,\71->67,\72->75,\73->73,
			\74->195,\75->149,\76->139,\77->157,\78->156,\79->144,\80->154,\81->141,\82->197,
			\83->138,\84->192,\85->163,\86->109,\87->170,\88->203,\89->170,\90->135,\91->126,
			\92->136,\93->116,\94->96,\95->115,\96->117,\97->108,\98->109,\99->120,\100->104,
			\101->97,\103->97,\104->115,\105->108],
			// lava2-1
			Dictionary[\48->88,\50->89,\52->57,\53->82,\55->79,\56->78,\57->66,\59->68,\60->0,
			\61->2,\62->5,\64->10,\65->11,\66->4,\67->3,\69->12,\70->8,\71->7,\72->35,\73->33,
			\74->19,\75->22,\76->15,\77->23,\78->24,\79->34,\81->136,\82->125,\83->94,\84->121,
			\86->127,\88->131,\89->130,\91->132,\93->99,\95->133,\96->118],
			// lava2-2
			Dictionary[\24->212,\25->193,\26->171,\27->197,\28->177,\29->155,\31->161,\33->162,
			\35->207,\36->186,\38->194,\39->213,\40->190,\41->188,\43->168,\44->166,\45->165,
			\47->169,\48->88,\50->89,\52->57,\53->82,\55->78,\57->66,\59->68,\60->0,\61->2,
			\62->5,\65->11,\67->3,\69->12,\71->8,\72->35,\74->19,\75->22,\76->15,\77->23,
			\79->34,\88->136,\89->132,\84->121,\86->127,\91->130,\93->99,\95->133,\96->53,\98->46,
			\100->42,\101->43,\103->49,\105->50,\107->44,\108->235,\110->232,\112->229,\113->230,
			\115->223,\117->231,\119->224,\120->234],
			// lava3-1
			Dictionary[\48->35,\50->32,\52->15,\53->16,\55->17,\57->27,\59->29,\60->26,\62->216,			\64->221,\65->222,\67->218,\69->219,\71->220,\72->0,\74->2,\76->5,\77->11,\79->13,			\81->1,\83->53,\84->46,\86->49,\88->50,\91->52,\93->47,\95->48,\97->41],
			// lava3-2
			Dictionary[\36->90,\38->81,\40->72,\41->71,\43->91,\45->134,\47->107,\48->122,
			\50->114,\52->113,\53->35,\55->32,\57->15,\59->26,\60->0,\62->2,\64->5,\65->11,
			\67->53,\69->46,\71->49,\72->50,\74->47,\76->226,\77->223,\79->229,\81->230,\83->231,
			\84->183,\86->179,\88->182,\89->184,\91->198,\93->196,\95->213,\96->211,\98->210],
			// lava3-3
			Dictionary[\24->90,\26->76,\28->84,\29->62,\31->63,\33->107,\35->95,\36->134,\38->111,			\40->112,\41->98,\43->99,\45->35,\47->32,\48->15,\50->0,\52->2,\53->5,\55->11,\57->53,			\59->46,\60->49,\62->226,\64->223,\65->229,\67->230,\69->231,\71->195,\72->196,
			\74->151,\76->152,\77->146,\79->148,\81->146,\83->166,\84->153,\86->154,\88->140,
			\89->141,\91->184,\93->206,\95->182,\96->200,\98->201,\100->207,\101->208,\103->167,
			\105->169,\107->185,\108->165,\110->202,\112->204],
			// lava4-1
			Dictionary[\24->90,\26->76,\28->77,\29->73,\31->61,\33->60,\35->135,\36->134,
			\38->136,\40->95,\41->133,\43->127,\45->107,\47->130,\48->132,\50->106,\52->99,
			\53->101,\55->137,\57->110,\59->40,\60->38,\62->36,\64->20,\65->19,\67->22,\69->23,
			\71->17,\72->0,\74->2,\76->6,\77->5,\79->11,\81->3,\83->235,\84->229,\86->230,
			\88->226,\89->215,\91->222,\93->217,\95->218,\96->56,\98->55,\100->54,\101->44,
			\103->195,\105->196,\107->143,\108->152,\110->148,\112->166,\113->154,\115->141,
			\117->187,\119->206,\120->182,\122->174,\124->209,\125->169,\127->164],
			// lava5-1
			Dictionary[\24->90,\26->93,\28->76,\29->74,\31->59,\33->92,\35->78,\36->66,\38->135,
			\40->136,\41->95,\43->133,\45->107,\47->105,\48->100,\50->129,\52->98,\53->19,\55->22,
			\57->35,\59->32,\60->15,\62->25,\64->18,\65->28,\67->0,\69->2,\71->5,\72->11,\74->3,
			\76->235,\77->229,\79->230,\81->223,\83->224,\84->56,\86->55,\88->54,\89->45,\91->195,
			\93->196,\95->143,\96->159,\98->145,\100->146,\101->140,\103->187,\105->206,\107->182,
			\108->173,\110->167,\112->185,\113->160],
			// lava5-2
			Dictionary[\24->90,\26->93,\28->76,\29->69,\31->70,\33->75,\35->73,\36->67,\38->135,
			\40->136,\41->95,\43->133,\45->127,\47->105,\48->100,\50->129,\52->98,\53->19,\55->22,
			\57->35,\59->32,\60->15,\62->25,\64->18,\65->28,\67->0,\69->2,\71->5,\72->11,\74->3,
			\75->8,\76->235,\77->229,\79->230,\81->223,\83->224,\84->56,\86->55,\88->54,\89->44,
			\91->195,\93->196,\95->143,\96->159,\98->145,\100->146,\101->140,\103->205,\105->178,
			\107->175,\108->174,\110->166,\112->165,\113->211],
			// lava5-3
			Dictionary[\24->90,\26->85,\28->86,\29->69,\31->70,\33->75,\35->73,\36->64,\38->135,
			\40->136,\41->127,\43->123,\45->103,\47->105,\48->100,\50->22,\52->35,\53->32,\55->15,
			\57->30,\59->37,\60->39,\62->0,\64->2,\65->5,\67->11,\69->7,\71->235,\72->229,\74->231,
			\76->223,\77->224,\79->234,\81->56,\83->49,\84->46,\86->42,\88->44,\89->195,\91->196,
			\93->143,\95->159,\96->145,\98->146,\100->140,\101->180,\103->176,\105->178,\107->174,
			\108->201,\110->169,\112->164]
					
		];
		
		clicks = Dictionary[	// 4 measure downbeats,1 count-click
			\70->5,\77->1,\75->2,\79->3,\84->0
		];
		 		
		cueMap =	Dictionary[	// letter announcements, A-Z
			\48->11,\49->12,\50->13,\51->14,\52->15,\53->16,\54->17,\55->18,\56->19,\57->20,
			\58->21,\59->22,\60->23,\61->24,\62->25,\63->26,\64->27,\65->28,\66->29,\67->30,
			\68->31,\69->32,\70->33,\71->34,\72->35,\73->36
		];
	}
	
	*map {|keyNum|
		^sectionMaps[curSec][keyNum.asSymbol]
	}
}
		