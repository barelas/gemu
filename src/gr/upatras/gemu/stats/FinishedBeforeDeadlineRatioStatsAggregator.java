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
public class FinishedBeforeDeadlineRatioStatsAggregator implements StatsAggregator,Serializable {
	
	static final long serialVersionUID = 1L;
	static transient Log log = LogFactory.getLog(FinishedBeforeDeadlineRatioStatsAggregator.class);
	transient Grid grid;
	double lastAggregationTime = 0;
	double countdown = 2;
	double aggregationInterval;
	Map<Double,Statistic> stats;
	String xname = "time";
	String yname = "finishedBeforeDeadlineRatio";//this is the name of the metric.
	double lastValue = 0;
	double consecutiveTimesWithSameValue = 0;
	double maxConsecutiveTimesWithSameValue = 100;
	double tolerance = 0.001;
	
	public FinishedBeforeDeadlineRatioStatsAggregator() {
		this.stats = Collections.synchronizedMap(new LinkedHashMap<Double,Statistic>());
	}

	public FinishedBeforeDeadlineRatioStatsAggregator(Grid grid) {
		this.grid = grid;
		this.stats = Collections.synchronizedMap(new LinkedHashMap<Double,Statistic>());
	}
	
	public void aggregate() {
		if (isTimeToAggregate()) forcedAggregation();
	}
	
	public void forcedAggregation() {
		lastAggregationTime = grid.getCurrentTime();
		Statistic stat = new Statistic(grid.getCurrentTime());
		if (setPosostoTaskFinishedBeforeDeadline(stat)) stats.put(grid.getCurrentTime(),stat);
	}
	
	/**
	 * 
	 * @param stat
	 * @return true gia na ginei h katagrafh. false gia na agnohsei thn katagrafh ayth.
	 */
	private boolean setPosostoTaskFinishedBeforeDeadline(Statistic stat) {
		if (grid.getCompletedTasks().isEmpty() && grid.getFailedTasks().isEmpty()) return false;
		double finishedOnTime = 0;
		for (Task task:grid.getCompletedTasks()) {
			if (!taskTypeOk(task)) continue;
			if (task.getCompletionTime()<=task.getAbsolutCompletionTime()) {
				finishedOnTime++;
			}
		}
		double allTasksOfThisCategory = countTasksOfValidCategory(grid.getCompletedTasks()) + countTasksOfValidCategory(grid.getFailedTasks());
		if (allTasksOfThisCategory==0) {
			//log.warn("no tasks of category found");
			return false;
		}
//		else {
//			log.info("tasks of category found!!! "+allTasksOfThisCategory);
//		}
		double posostoTaskFinishedOnTime = finishedOnTime / allTasksOfThisCategory;
//		if (log.isInfoEnabled()) {
//			log.info("posostoTaskFinishedOnTime:"+posostoTaskFinishedOnTime + " lastvalue="+lastValue);
//		}
		
//		if (isLikeLastValue(posostoTaskFinishedOnTime)) {
//			if (++consecutiveTimesWithSameValue >= maxConsecutiveTimesWithSameValue) {
//				grid.endSimulation();
//				log.info("same value calced "+consecutiveTimesWithSameValue+" times. Ending this simulation.");
//			}
//		} else {
//			consecutiveTimesWithSameValue = 0;
//			lastValue = posostoTaskFinishedOnTime;
//		}
//		log.info(getYname()+"="+posostoTaskFinishedOnTime);
		stat.setValue(getYname(),posostoTaskFinishedOnTime);
		return true;
	}
	
	private int countTasksOfValidCategory(LinkedList<Task> tasks) {
		int res = 0;
		for (Task task:tasks) {
			if (taskTypeOk(task)) res++;
		}
		return res;
	}
	
	protected boolean taskTypeOk(Task task) {
		return true;
	}
	
	private boolean isLikeLastValue(double newValue) {
		if (lastValue*(1D-tolerance) < newValue && newValue < lastValue*(1D+tolerance)) return true;
		else return false;
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
