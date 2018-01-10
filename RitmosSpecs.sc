RitmosSpecs {
	classvar <>specs;

	*initClass	{
		StartUp.add({
			specs = [
				\bufnum->[0,100,\lin,0].asSpec,
				\rate->[0.125, 8, 'exp', 0, 1.0].asSpec,
				\mod->'unipolar'.asSpec,
				\fMod->'unipolar'.asSpec,
				\endRt->[0.125,8,\exp,0,1.0].asSpec,
				\sus->ControlSpec(0,0.999,default:1),
				\sinFreq->Spec.specs.at(\freq),
				\sinAmp->ControlSpec(warp:\amp),
				\sawFreq->Spec.specs.at(\freq),
				\sawPhs->ControlSpec(0,2),
				\sawAmp->ControlSpec(warp:\amp),
				\plsFreq->Spec.specs.at(\freq),
				\plsWdth->ControlSpec(default: 0.1),
				\plsAmp->ControlSpec(warp:\amp),
				\sinfrq->Spec.specs.at(\lofreq),
				\sawfrq->Spec.specs.at(\lofreq),
				\plsfrq->Spec.specs.at(\lofreq),
				\sigAmp->ControlSpec(warp:\amp),
				\gate->ControlSpec(default:1, step:1),
				\vol->ControlSpec(warp:\amp),
				\effOut->[8,12,\lin,1,8].asSpec,
				\effBus->[16,24,\lin,1,16].asSpec,
				\effAmp->Spec.specs[\amp],
				\out->[0,15,\lin,1,0].asSpec,
				\start->ControlSpec(default:0),
				\end->ControlSpec(default:1),
				\adjTime->ControlSpec(-1.0,1.0),
				\spd->ControlSpec(0.125,12,\exp,0,4),
				\speed->ControlSpec(0.125,12,\exp,0,4),
				\spdQ->ControlSpec(0.125,12,\exp,0.1,4),
				\pSpd->ControlSpec(-8.0,8.0,\lin,0.01,0),
				\trgf->ControlSpec(0.25,80,\exp,default:1),
				\gdur->ControlSpec(0.01,1.0,\exp,0,0.1),
				\sdur->ControlSpec(0.01,5.0,\exp,0,0.2),
				\dnse->ControlSpec(0.0625,8,\exp,default:2),
				\lfrq->[0.01,200,\exp,0,0.2].asSpec,
				\fvar->ControlSpec(0.01,5,\exp),
				\fmul->ControlSpec(1,20,default:1),
				\wdth->ControlSpec(default:0.1),
				\width1->ControlSpec(default:0.1),
				\width2->ControlSpec(default:0.1),
				\width3->ControlSpec(default:0.1),
				\int1->[-12,12,\lin,1,0].asSpec,
				\int2->[-12,12,\lin,1,0].asSpec,
				\int3->[-12,12,\lin,1,0].asSpec,
				\ffrqInt->[1,48,\lin,1,1].asSpec,
				\ffrq->ControlSpec(80,8000,\exp,0,1200),
				\ffreq->ControlSpec(80,8000,\exp,0,1200),
				\clip->ControlSpec(0.05,1,default:0.5),
				\rtio->ControlSpec(0.01,20,\lin,0.1,1),
				\lrtio->ControlSpec(0.01,4,\lin,0.025,1),
				\idx->ControlSpec(0,2pi,\linear),
				\ffrq1->ControlSpec(80,8000,\exp,0,1200),
				\ffrq2->ControlSpec(80,8000,\exp,0,1200),
				\dcy1->[0.001,0.1,\lin].asSpec,
				\dcy2->[0.001,0.1,\lin].asSpec,
				\res->[0.001,0.1,\lin,0,0.005].asSpec,
				\dcyt->[0,16,\lin,0,1.0].asSpec,
				\mfrq1->Spec.specs.at(\lofreq),
				\mDpt1->[0.001,4000,\exp].asSpec,
				\aDpt1->[0.001,4000,\exp].asSpec,
				\mfrq2->Spec.specs.at(\lofreq),
				\mfrq->Spec.specs.at(\lofreq),
				\mDpt2->[0.001,4000,\exp].asSpec,
				\aDpt2->[0.001,4000,\exp].asSpec,
				\frqShift->[0.1,4000,\exp,0,4].asSpec,
				\fDpth->[0.001,4000,\exp].asSpec,
				\dly->[0,8,\lin,0,0.2].asSpec,
				\dlyt->[0,8,\lin,0,0.2].asSpec,
				\dly1->[0,8,\lin,0,0.2].asSpec,
				\dly2->[0,8,\lin,0,0.2].asSpec,
				\gfrq->[0.1,80,\exp,0,12.0].asSpec,
				\amp1->ControlSpec(warp:\amp),
				\amp2->ControlSpec(warp:\amp),
				\pShft->[0.25,4.0,\exp,0,1.0].asSpec,
				\wsize->[0.01,0.5,\lin,0.01,0.05].asSpec,
				\pShft1->[0.25,4.0,\exp,0,1.0].asSpec,
				\wsize1->[0.01,0.5,\lin,0.01,0.05].asSpec,
				\pShft2->[0.25,4.0,\exp,0,1.0].asSpec,
				\wsize2->[0.01,0.5,\lin,0.01,0.05].asSpec,
				\bShft->[-32,96,\lin,1,16].asSpec,
				\stch->[0,8,\lin,0.0,1].asSpec,
				\bShft1->[-32,96,\lin,1,16].asSpec,
				\stch1->[0,8,\lin,0.0,1].asSpec,
				\bShft2->[-32,96,\lin,1,16].asSpec,
				\stch2->[0,8,\lin,0.0,1].asSpec,
				\thrsh->[0.01,1,\exp,0,0.1].asSpec,
				\thrsh1->[0.01,1,\exp,0,0.1].asSpec,
				\thrsh2->[0.01,1,\exp,0,0.1].asSpec,
				\slp1->[0,10,\lin,0.0,1].asSpec,
				\slp2->[0,10,\lin,0.0,1].asSpec,
				\slpa->[0,10,\lin,0.0,1].asSpec,
				\slpb->[0,10,\lin,0.0,1].asSpec,
				\wvsBuf->[1,16,\lin,1,1].asSpec,
				\voscBuf->[1,7,\lin,0,1].asSpec,
				\coef->[-1,1,\lin,0.0,0.0].asSpec,
				\brit->[100,18000,\exp,0,1000].asSpec,
				\plDcy->[0.1,8,\lin,0,2].asSpec,
				\plSpd->[0.025,25.0,\exp,0,0.1].asSpec,
				\tblBuf->[1,8,\lin,0,1].asSpec,
				\swpFrq->Spec.specs.at(\lofreq),
				\vib->Spec.specs.at(\lofreq),
				\in->[1,8,\lin,1,1].asSpec,
				\att->[0,8.0,\lin,0,0.01].asSpec,
				\decay->[0,8.0,\lin,0,0.01].asSpec,
				\rls->[0,8.0,\lin,0,0.2].asSpec,
				\peak->'amp'.asSpec,
				\bpm->[20,480,\exp,1.0,120].asSpec,
				\lvl->[0.5,4.0,\amp,0,1.0].asSpec,
				\hWdth->[0.001,0.5,\exp,0,0.1].asSpec,
				\pInt->[0.25,2,\exp,1,0.05].asSpec,
				\frq->[20,4186,\exp,0,512].asSpec,
				\duty->[0,1,\lin,0,0.5].asSpec,
				\ring->[0.05,10,\exp,0,0.4].asSpec,
				\ffrq3->ControlSpec(80,8000,\exp,0,1200),
				\loAmp->ControlSpec(warp:\amp),
				\hiAmp->ControlSpec(warp:\amp),
				\loPan->Spec.specs[\pan],
				\hiPan->Spec.specs[\pan],
				\clipAmp->[0,20,\lin,0,1].asSpec,
				\noiseAmp->ControlSpec(warp:\amp, default: 0.5),
				\ampNoise->ControlSpec(warp:\amp, default: 0.5),
				\sinePhs->[0,2pi,\lin].asSpec,
				\phs->[0,2pi,\lin].asSpec,
				\plsPhs->[0,1,\lin].asSpec,
				\ratio->ControlSpec(0.01,20,\lin,0.1,1),
				\ratio2->ControlSpec(0.01,20,\lin,0.1,2.6775),
				\ratio3->ControlSpec(0.01,20,\lin,0.1,3.3825),
				\ratio4->ControlSpec(0.01,20,\lin,0.1,4.3075),
				\index->ControlSpec(0,2pi,\linear),
				\loop->ControlSpec(0,1,\lin,1,1),
				\numTeeth->ControlSpec(0,32,\lin,0,8),
				\elev->[0,0.5pi,\lin,0,0].asSpec,
				\bw->[1.0,40.0,\exp,0,4].asSpec,
				\rq->[0.001,1,\exp,0,0.1].asSpec,
				\chor->[0,1,\lin,0,0.1].asSpec,
				\level->[0,3.0,\amp,0,0.5].asSpec,
				\curve->[-12,12,\lin,0,-4].asSpec,
				\fold->[0,1,\lin,0,1].asSpec
			];

			Spec.specs.addAll(specs)
		})

		}

	}
