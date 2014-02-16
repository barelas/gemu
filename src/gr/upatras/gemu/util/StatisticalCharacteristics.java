package gr.upatras.gemu.util;

/**
 * A set of statistical attributes.
 * @author George Barelas
 */
public class StatisticalCharacteristics {
	
	double epochMean = -1;
	double epochDeviation = -1;
	double ccMean = -1;
	double ccDeviation = -1;
	double initialWorkloadMean = -1;
	double initialWorkloadDeviation = -1;
	double inputDataMean = -1;
	double inputDataDeviation = -1;
	double outputDataMean = -1;
	double outputDataDeviation = -1;
	double desirableCompletionTimeMean = -1;
	double desirableCompletionTimeDeviation = -1;
	double absolutCompletionTimeMean = -1;
	double absolutCompletionTimeDeviation = -1;
	double bandwidthMean = -1;
	double bandwidthDeviation = -1;
	
	public double getBandwidthDeviation() {
		return bandwidthDeviation;
	}
	public void setBandwidthDeviation(double bandwidthDeviation) {
		this.bandwidthDeviation = bandwidthDeviation;
	}
	public double getBandwidthMean() {
		return bandwidthMean;
	}
	public void setBandwidthMean(double bandwidthMean) {
		this.bandwidthMean = bandwidthMean;
	}
	public double getAbsolutCompletionTimeDeviation() {
		return absolutCompletionTimeDeviation;
	}
	public void setAbsolutCompletionTimeDeviation(
			double absolutCompletionTimeDeviation) {
		this.absolutCompletionTimeDeviation = absolutCompletionTimeDeviation;
	}
	public double getAbsolutCompletionTimeMean() {
		return absolutCompletionTimeMean;
	}
	public void setAbsolutCompletionTimeMean(double absolutCompletionTimeMean) {
		this.absolutCompletionTimeMean = absolutCompletionTimeMean;
	}
	public double getDesirableCompletionTimeDeviation() {
		return desirableCompletionTimeDeviation;
	}
	public void setDesirableCompletionTimeDeviation(
			double desirableCompletionTimeDeviation) {
		this.desirableCompletionTimeDeviation = desirableCompletionTimeDeviation;
	}
	public double getDesirableCompletionTimeMean() {
		return desirableCompletionTimeMean;
	}
	public void setDesirableCompletionTimeMean(double desirableCompletionTimeMean) {
		this.desirableCompletionTimeMean = desirableCompletionTimeMean;
	}
	public double getInitialWorkloadDeviation() {
		return initialWorkloadDeviation;
	}
	public void setInitialWorkloadDeviation(double initialWorkloadDeviation) {
		this.initialWorkloadDeviation = initialWorkloadDeviation;
	}
	public double getInitialWorkloadMean() {
		return initialWorkloadMean;
	}
	public void setInitialWorkloadMean(double initialWorkloadMean) {
		this.initialWorkloadMean = initialWorkloadMean;
	}
	public double getInputDataDeviation() {
		return inputDataDeviation;
	}
	public void setInputDataDeviation(double inputDataDeviation) {
		this.inputDataDeviation = inputDataDeviation;
	}
	public double getInputDataMean() {
		return inputDataMean;
	}
	public void setInputDataMean(double inputDataMean) {
		this.inputDataMean = inputDataMean;
	}
	public double getOutputDataDeviation() {
		return outputDataDeviation;
	}
	public void setOutputDataDeviation(double outputDataDeviation) {
		this.outputDataDeviation = outputDataDeviation;
	}
	public double getOutputDataMean() {
		return outputDataMean;
	}
	public void setOutputDataMean(double outputDataMean) {
		this.outputDataMean = outputDataMean;
	}
	public double getCcDeviation() {
		return ccDeviation;
	}
	public void setCcDeviation(double ccDeviation) {
		this.ccDeviation = ccDeviation;
	}
	public double getCcMean() {
		return ccMean;
	}
	public void setCcMean(double ccMean) {
		this.ccMean = ccMean;
	}
	public double getEpochDeviation() {
		return epochDeviation;
	}
	public void setEpochDeviation(double epochDeviation) {
		this.epochDeviation = epochDeviation;
	}
	public double getEpochMean() {
		return epochMean;
	}
	public void setEpochMean(double epochMean) {
		this.epochMean = epochMean;
	}
}
