package gr.upatras.gemu.stats;

import gr.upatras.gemu.grid.Grid;
import gr.upatras.gemu.node.Node;

import java.io.FileOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.apache.commons.math.stat.descriptive.SummaryStatisticsImpl;

/**
 * @author George Barelas
 *
 */
public class UserQoSMeanStatsAggregator implements StatsAggregator,Serializable {
	static final long serialVersionUID = 1L;
	static transient Log log = LogFactory.getLog(UserQoSMeanStatsAggregator.class);
	protected transient Grid grid;
	protected double lastAggregationTime = 0;
	protected double countdown = 2;
	protected double aggregationInterval;
	protected Map<Double,Statistic> stats;
	protected String xname = "time";
	protected String yname = "userQoS-mean-allUserTypes";//this is the name of the metric.
	protected double lastValue = 0;
	protected double consecutiveTimesWithSameValue = 0;
	protected double maxConsecutiveTimesWithSameValue = 100;
	protected double tolerance = 0.001;
	
	public UserQoSMeanStatsAggregator() {
		this.stats = Collections.synchronizedMap(new LinkedHashMap<Double,Statistic>());
	}

	public UserQoSMeanStatsAggregator(Grid grid) {
		this.grid = grid;
		this.stats = Collections.synchronizedMap(new LinkedHashMap<Double,Statistic>());
	}
	
	public void aggregate() {
		if (isTimeToAggregate()) forcedAggregation();
	}
	
	public void forcedAggregation() {
		lastAggregationTime = grid.getCurrentTime();
		Statistic stat = new Statistic(grid.getCurrentTime());
		if (setNodeQoS(stat)) stats.put(grid.getCurrentTime(),stat);
	}
	
	/**
	 * 
	 * @param stat
	 * @return true gia na ginei h katagrafh. false gia na agnohsei thn katagrafh ayth.
	 */
	private boolean setNodeQoS(Statistic stat) {
		LinkedList<Node> nodes = grid.getNodesOnline();
		if (nodes.isEmpty()) return false;
		SummaryStatistics summaryStatistics = new SummaryStatisticsImpl();
		for (Node node:nodes) {
			if (!getStatForNodeAllowed(node)) continue;
			double nodeQoS = node.getUserQos();
			if (nodeQoS<0) continue;
			summaryStatistics.addValue(nodeQoS);
		}
		stat.setValue(getYname(),getValueFromSummaryStatistics(summaryStatistics));
		//log.info(getYname()+"="+stat.getValue(getYname()));
		return true;
	}
	
	protected boolean getStatForNodeAllowed(Node node) {
		return true;
	}
	
	double getValueFromSummaryStatistics(SummaryStatistics summaryStatistics) {
		return summaryStatistics.getN()==0 ? 0 : summaryStatistics.getMean();
	}
	
	private boolean isTimeToAggregate() {
		if (--countdown<=0) {
			countdown = aggregationInterval;
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 
	 * @return LinkedHashMap me ta Statistics pou exei sygkentrwsei.
	 */
	public Map<Double,Statistic> getStats() {
		return stats;
	}
	
	public void saveToFile(String filename) {
		try {
			FileOutputStream f = new FileOutputStream(filename);
			ObjectOutput s = new ObjectOutputStream(f);
			s.writeObject(this);
			s.flush();
			f.close();
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			return;
		}
	}

	public void setGrid(Grid grid) {
		this.grid = grid;
	}
	
	public HashMap<String,LinkedList<Double>> getMapOfStatValues() {
		HashMap<String,LinkedList<Double>> result = new HashMap<String,LinkedList<Double>>();
		for (Entry<Double,Statistic> entry:stats.entrySet()) {
			addStatValue(result,xname,entry.getKey());
			for (Entry<String,Double> statEntry:entry.getValue().getValues().entrySet()) {
				addStatValue(result,statEntry.getKey(),statEntry.getValue());
			}
		}
		return result;
	}
	
	private void addStatValue(HashMap<String,LinkedList<Double>> map,String key,Double value) {
		if (!map.containsKey(key)) {
			map.put(key,new LinkedList<Double>());
		}
		map.get(key).add(value);
	}

	public void setAggregationInterval(double aggregationInterval) {
		this.aggregationInterval = aggregationInterval;
	}
	
	public String getXname() {
		return this.xname;
	}

	public String getYname() {
		return yname;
	}
}
