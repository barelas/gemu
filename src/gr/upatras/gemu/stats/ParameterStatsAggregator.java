package gr.upatras.gemu.stats;

import gr.upatras.gemu.grid.Grid;

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

/**
 * @author George Barelas
 *
 */
public class ParameterStatsAggregator implements StatsAggregator,Serializable {
	
	static final long serialVersionUID = 1L;
	static transient Log log = LogFactory.getLog(ParameterStatsAggregator.class);
	Map<Double,Statistic> stats;
	String xname = "time";
	String yname = null;
	String schedulerName;
	
	public ParameterStatsAggregator(String xname,String yname,String schedulerName) {
		this.xname = xname;
		this.yname = yname;
		this.schedulerName = schedulerName;
		this.stats = Collections.synchronizedMap(new LinkedHashMap<Double,Statistic>());
	}

	public void aggregate() {
		
	}

	public void forcedAggregation() {
		
	}

	public HashMap<String, LinkedList<Double>> getMapOfStatValues() {
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

	public Map<Double, Statistic> getStats() {
		return stats;
	}

	/* (non-Javadoc)
	 * @see gr.upatras.gemu.stats.StatsAggregator#saveToFile(java.lang.String)
	 */
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
	
	public void addStatistic(Statistic statistic,Double xvalue) {
		stats.put(xvalue,statistic);
	}

	public void setGrid(Grid grid) {
		
	}
	public String getXname() {
		return this.xname;
	}
	public String getYname() {
		return yname;
	}
	public String getSchedulerName() {
		return schedulerName;
	}
}
