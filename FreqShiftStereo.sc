FreqShiftStereo {
	*ar {
		arg in,			// input signal
		freq = 0.0,		// shift, in cps
		phase = 0.0,	// phase of SSB
		mul = 1.0,
		add = 0.0;

		// multiply by quadrature
		// and add together. . .
		^(Hilbert.ar(in) * SinOsc.ar(freq, (phase + [ 0.5*pi, 0.0 ]))).madd(mul,add)
	}
}