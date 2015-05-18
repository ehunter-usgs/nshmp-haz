package org.opensha2.calc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import org.opensha2.data.DataUtils;
import org.opensha2.eq.Magnitudes;
import org.opensha2.eq.model.Rupture;
import org.opensha2.eq.model.SourceType;
import org.opensha2.gmm.Gmm;
import org.opensha2.gmm.Imt;

import com.google.common.base.Function;
import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;

/**
 * Add comments here
 *
 * 
 * @author Peter Powers
 */
class Deagg {

//	 private final DataModel model;
//	 private final double[][][] data;
//	 private final double mBar, rBar, εBar;

	/*
	 * Many deagg bins have no data so we will usually be returning a sparse
	 * matrix
	 */

	// do we want to track the relative location in each distance bin:
	// i.e. the bin plots at the contribution weighted distance
	// private Comparator<ContributingRupture> comparator = Ordering.natural();

	private Queue<Contribution> contribQueue = MinMaxPriorityQueue.orderedBy(Ordering.natural())
		.maximumSize(20).create();

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// TreeMap<Double, String> test = Maps.newTreeMap();

		// int dd = Data.Builder.size(5.0, 5.2, 0.1);
		// System.out.println(dd);
	}


	/* Wrapper class for a Rupture and it's contribution to hazard. */
	static class Contribution implements Comparable<Contribution> {

		final Rupture rupture = null;
		final Double value = null;

		@Override public int compareTo(Contribution o) {
			return value.compareTo(o.value);
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	static class Builder {

		private DataModel model;
		private HazardResult hazard;
		private double iml;
		
		private double[][][] data; // [M][R][e]
		
		private double mBar, rBar, εBar; // these are total
		private double totalRate; // TODO compare to orignal PoE
		private double totalRateWithinRange;
		
		// track mBar and rBar for each M R bin, for positioning?
		private double[][] mPos; //??
		private double[][] rPos; //??
		
		// should we use a general Deagg.Result container to store total results and
		// then use a Map<Gmm, Result> for gmm specific values


		// TODO pass in executor??

		Builder withModel(DataModel model) {
			this.model = checkNotNull(model);
			return this;
		}

		Builder forHazard(HazardResult hazard) {
			this.hazard = checkNotNull(hazard);
			return this;
		}

		// // TODO need PoE enum
		// Builder targetExceedance(double exceedance) {
		// // TODO convert to iml
		// }
		//
		// Builder targetRate(double rate) {
		// // TODO convert to iml
		// }
		//
		// Builder targetIml(double iml) {
		// // TODO validate iml against curve range
		// // this
		// }

		Deagg build() {
			// run deaggregation
			// build final statistics
			return null;
		}

		private void process() {
			
			
			for (SourceType type : hazard.sourceSetMap.keySet()) {
				Set<HazardCurveSet> hazardCurveSets = hazard.sourceSetMap.get(type);
				switch (type) {
					case FAULT:
//						processFaultSources(hazardCurveSets);
				}
			}
		}

		/*
		 * There are a variety of things that we can keep track of here. - the
		 * gross contribution of each source set - the gross contribution of
		 * each Gmm (could be further subdivision of above) -
		 */
//		private void processFaultSources(Set<HazardCurveSet> curveSets) {
//			for (HazardCurveSet curveSet : curveSets) {
//				for (HazardGroundMotions groundMotions : curveSet.hazardGroundMotionsList) {
//					for (Gmm gmm : groundMotions.means.keySet()) {
//
//					}
//				}
//			}
//		}
		
		// do we want to farm SourceSets out onto different threads?
		
		// TODO get us from CalcConfig
		private static final ExceedanceModel SIGMA = ExceedanceModel.TRUNCATION_UPPER_ONLY;
		private static final double trunc = 3.0;
		
		
		
		private void processFaultSource(HazardCurveSet curveSet, Imt imt, double iml) {
			for (HazardGroundMotions groundMotions : curveSet.hazardGroundMotionsList) {
				
				int inputCount = groundMotions.inputs.size();
				
				Map<Gmm, List<Double>> gmmMeans = groundMotions.means.get(imt);
				Map<Gmm, List<Double>> gmmSigmas = groundMotions.sigmas.get(imt);

				double sourceRateAtIml = 0.0;
				
				
				
				for (Gmm gmm : gmmMeans.keySet()) {
					List<Double> means = gmmMeans.get(gmm);
					List<Double> sigmas = gmmSigmas.get(gmm);
					
					
					for (int i=0; i<inputCount; i++) {
						HazardInput input = groundMotions.inputs.get(i);
						double μ = means.get(i);
						double σ = sigmas.get(i);
						
						double ε = epsilon(μ, σ, iml);
						double rateAtIml = SIGMA.exceedance(μ, σ, trunc, imt, iml);
						
						
						
					}
					
				}
			}
		}
		
		private void addRupture(double m, double r, double ε, double rate) {
			
			mBar += m * rate;
			rBar += r * rate;
			εBar += ε * rate;
			totalRate += rate;
			
//			int im = index(model.mMin, model.Δm, )
//			int ir = 
		}
		
//		private void processSource()
		
		// HazardGroundMotions inputs list is same length and order as means and sigmas

	}
	
	
	private static int index(double min, double binWidth, double value) {
		return (int) Math.floor((value - min) / binWidth);
	}

	
	// should all be in log space
	private static double epsilon(double μ, double σ, double iml) {
		return (iml - μ) / σ;
	}

	/*
	 * TODO move to transforms
	 * 
	 * Transforms HazardGroundMotions to a rate for a target Iml. This is the
	 * same as computing a hazard curve with a single point.
	 */
	private static class GroundMotionsToRate implements Function<HazardGroundMotions, Double> {

		private final double targetIml;

		GroundMotionsToRate(double targetIml) {
			this.targetIml = targetIml;
		}

		@Override public Double apply(HazardGroundMotions groundMotions) {

			// HazardCurves.Builder curveBuilder =
			// HazardCurves.builder(groundMotions);
			// ArrayXY_Sequence utilCurve = ArrayXY_Sequence.copyOf(modelCurve);
			//
			// double sourceRate = 0.0;
			// for (Gmm gmm : groundMotions.means.keySet()) {
			//
			// // ArrayXY_Sequence gmmCurve =
			// ArrayXY_Sequence.copyOf(modelCurve);
			//
			// List<Double> means = groundMotions.means.get(gmm);
			// List<Double> sigmas = groundMotions.sigmas.get(gmm);
			//
			// for (int i = 0; i < means.size(); i++) {
			// setExceedProbabilities(utilCurve, means.get(i), sigmas.get(i),
			// false, NaN);
			// utilCurve.multiply(groundMotions.inputs.get(i).rate);
			// gmmCurve.add(utilCurve);
			// }
			// curveBuilder.addCurve(gmm, gmmCurve);
			// }
			// return curveBuilder.build();
			return null;
		}
	}

	private static final Range<Double> rRange = Range.closed(0.0, 1000.0);
	private static final Range<Double> εRange = Range.closed(-3.0, 3.0);

	private static int size(double min, double max, double Δ) {
		return (int) Math.rint((max - min) / Δ);
	}

	public static class DataModel {

		private final double mMin, mMax, Δm;
		private final double rMin, rMax, Δr;
		private final double εMin, εMax, Δε;

		private final int mSize, rSize, εSize;

		private DataModel(double mMin, double mMax, double Δm, double rMin, double rMax, double Δr,
			double εMin, double εMax, double Δε) {

			this.mMin = mMin;
			this.mMax = mMax;
			this.Δm = Δm;
			this.rMin = rMin;
			this.rMax = rMax;
			this.Δr = Δr;
			this.εMin = εMin;
			this.εMax = εMax;
			this.Δε = Δε;

			mSize = size(mMin, mMax, Δm);
			rSize = size(rMin, rMax, Δr);
			εSize = size(εMin, εMax, Δε);
		}
		
//		public int mIndex(double m) {
//			return 
//		}

		/**
		 * Create a deaggregation data model. Deaggregation data bins are
		 * anchored on the {@code min} values supplied. {@code max} values may
		 * not correspond to final upper edge of uppermost bins if
		 * {@code max - min} is not evenly divisible by {@code Δ}.
		 * 
		 * @param mMin lower edge of lowest magnitude bin
		 * @param mMax maximum magnitude
		 * @param Δm
		 * @param rMin lower edge of lowest distance bin
		 * @param rMax
		 * @param Δr
		 * @param εMin lower edge of lowest epsilon bin
		 * @param εMax
		 * @param Δε
		 */
		public static DataModel create(double mMin, double mMax, double Δm, double rMin,
				double rMax, double Δr, double εMin, double εMax, double Δε) {

			// @formatter:off
			return new Builder()
				.magnitudeDiscretization(mMin, mMax, Δm)
				.distanceDiscretization(rMin, rMax, Δr)
				.epsilonDiscretization(εMin, εMax, Δε)
				.build();
			// @formatter:on
		}

		/**
		 * Create a deaggregation data model from the supplied
		 * {@code CalcConfig}.
		 * @param c {@code CalcConfig} to process
		 */
		public static DataModel fromConfig(CalcConfig c) {
			return create(c.deagg.mMin, c.deagg.mMax, c.deagg.Δm, c.deagg.rMin, c.deagg.rMax,
				c.deagg.Δr, c.deagg.εMin, c.deagg.εMax, c.deagg.Δε);
		}

		private static class Builder {

			private static final String ID = "Deagg.DataModel.Builder";
			private boolean built = false;

			private Double mMin, mMax, Δm;
			private Double rMin, rMax, Δr;
			private Double εMin, εMax, Δε;

			private Builder magnitudeDiscretization(double min, double max, double Δ) {
				mMin = Magnitudes.validateMag(min);
				mMax = Magnitudes.validateMag(max);
				Δm = DataUtils.validateDelta(min, max, Δ);
				return this;
			}

			private Builder distanceDiscretization(double min, double max, double Δ) {
				rMin = DataUtils.validate(rRange, "Min distance", min);
				rMax = DataUtils.validate(rRange, "Max distance", max);
				Δr = DataUtils.validateDelta(min, max, Δ);
				return this;
			}

			private Builder epsilonDiscretization(double min, double max, double Δ) {
				εMin = DataUtils.validate(εRange, "Min epsilon", min);
				εMax = DataUtils.validate(εRange, "Max epsilon", max);
				Δε = DataUtils.validateDelta(min, max, Δ);
				return this;
			}

			private DataModel build() {
				validateState(ID);
				return new DataModel(mMin, mMax, Δm, rMin, rMax, Δr, εMin, εMax, Δε);
			}

			private void validateState(String id) {
				checkState(!built, "This %s instance as already been used", id);
				checkState(mMin != null, "%s magnitude discretization not set", id);
				checkState(rMin != null, "%s distance discretization not set", id);
				checkState(εMin != null, "%s epsilon discretization not set", id);
				built = true;
			}
		}
	}
}