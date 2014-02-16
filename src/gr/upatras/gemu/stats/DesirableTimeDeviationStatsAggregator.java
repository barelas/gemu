package gr.upatras.gemu.stats;

import gr.upatras.gemu.grid.Grid;
import gr.upatras.gemu.task.Task;

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
public class DesirableTimeDeviationStatsAggregator implements StatsAggregator,Serializable {

	static final long serialVersionUID = 1L;
	static transient Log log = LogFactory.getLog(DesirableTimeDeviationStatsAggregator.class);
	transient Grid grid;
	double lastAggregationTime = 0;
	double countdown = 2;
	double aggregationInterval;
	Map<Double,Statistic> stats;
	String xname = "time";
	String yname = "deviationFromDesirableTime-allTaskTypes";//this is the name of the metric.
	double lastValue = 0;
	double consecutiveTimesWithSameValue = 0;
	double maxConsecutiveTimesWithSameValue = 100;
	double tolerance = 0.001;
	
	public DesirableTimeDeviationStatsAggregator() {
		this.stats = Collections.synchronizedMap(new LinkedHashMap<Double,Statistic>());
	}

	public DesirableTimeDeviationStatsAggregator(Grid grid) {
		this.grid = grid;
		this.stats = Collections.synchronizedMap(new LinkedHashMap<Double,Statistic>());
	}
	
	public void aggregate() {
		if (isTimeToAggregate()) forcedAggregation();
	}
	
	public void forcedAggregation() {
		lastAggregationTime = grid.getCurrentTime();
		Statistic stat = new Statistic(grid.getCurrentTime());
		if (setDesirableTimeDeviation(stat)) stats.put(grid.getCurrentTime(),stat);
	}
	
	private double getDeviationFromDesirableTime(Task task) {
		double des = task.getDesirableCompletionTime();
		return (task.getCompletionTime() - des) / (des - task.getCreationTime());
	}
	
	/**
	 * 
	 * @param stat
	 * @return true gia na ginei h katagrafh. false gia na agnohsei thn katagrafh ayth.
	 */
	private boolean setDesirableTimeDeviation(Statistic stat) {
		if (grid.getCompletedTasks().isEmpty()) return false;
		double deviation = 0;
		double n = 0;
		boolean foundAtLeastOneOfCategory = false;
		for (Task task:grid.getCompletedTasks()) {
			if (!taskTypeOk(task)) continue;
			foundAtLeastOneOfCategory = true;
			deviation = (n++*deviation+getDeviationFromDesirableTime(task))/n;
		}
		//log.info(getYname() + "=" + deviation);
		if (!foundAtLeastOneOfCategory) return false;
		stat.setValue(getYname(),deviation);
		return true;
	}
	
	/**
	 * Always true for this class. Gets overriden from the subclasses, so that
	 * they collect stats for specific task categories.
	 * @param task
	 * @return
	 */
	protected boolean taskTypeOk(Task task) {
		return true;
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
