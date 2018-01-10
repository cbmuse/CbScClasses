
HilbertCB {
	*ar { arg in;
		var poles=#[[
				5.41,
				41.11,
				167.35,
				671.37,
				2694.36,
				11976.86
			],[
				18.78,
				83.50,
				335.13,
				1344.40,
				5471.87,
				41551.67
			]], sr;
		    sr = Server.local.sampleRate;
			^poles.collect({arg poles;
				var out;
				out = in;
				poles.do({arg polefreq;
						var alpha, beta;
						alpha = (polefreq*pi)/sr;
						beta = (1-alpha)/(1+alpha);
						out = FOS.ar( out, beta.neg, 1, beta );
				});
				out
			})
		}
}
