package gr.upatras.gemu.stats;

import gr.upatras.gemu.grid.Grid;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;

/**
 * @author George Barelas
 *
 */
public class UserQoSStandardDeviationStatsAggregator extends UserQoSMeanStatsAggregator implements StatsAggregator,Serializable {
	
	static final long serialVersionUID = 1L;
	static transient Log log = LogFactory.getLog(UserQoSStandardDeviationStatsAggregator.class);
	
	String yname = "userQoS-StandardDeviation-allUserTypes";//this is the name of the metric.
	
	public UserQoSStandardDeviationStatsAggregator() {
		this.stats = Collections.synchronizedMap(new LinkedHashMap<Double,Statistic>());
	}

	public UserQoSStandardDeviationStatsAggregator(Grid grid) {
		this.grid = grid;
		this.stats = Collections.synchronizedMap(new LinkedHashMap<Double,Statistic>());
	}
	
	double getValueFromSummaryStatistics(SummaryStatistics summaryStatistics) {
		return summaryStatistics.getN()==0 ? 0 : summaryStatistics.getStandardDeviation();
	}
	
	public String getYname() {
		return this.yname;
	}

}
