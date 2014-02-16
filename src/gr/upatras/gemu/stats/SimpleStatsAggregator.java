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
 * -pososto task pou oloklhrw8hkan ston epi8ymhto xrono.
 * -pososto task pou oloklhrw8hkan ston austhrh xrono.
 * -ratio (xronos oloklhrwshs - epi8ymhtos xronos oloklhrwshs)/xronos
 *  ypologismou => dinei mia ektimhsh QoS pisteuw.
 * -pososto task pou den kataferan na oloklhrw8oun.
 * -node utilization ratio.
 * 
 * @author George Barelas
 *
 * @deprecated Use one of the special aggregators.
 */
public class SimpleStatsAggregator implements StatsAggregator,Serializable {
	
	static final long serialVersionUID = 1L;
	static transient Log log = LogFactory.getLog(SimpleStatsAggregator.class);
	transient Grid grid;
	double lastAggregationTime = 0;
	double countdown = 2;
	double aggregationInterval;
	Map<Double,Statistic> stats;
	String xname = "time";
	String yname;//this is the name of the metric.
	double lastValue = 0;
	double consecutiveTimesWithSameValue = 0;
	double maxConsecutiveTimesWithSameValue = 100;
	double tolerance = 0.001;
	
	public SimpleStatsAggregator() {
		this.stats = Collections.synchronizedMap(new LinkedHashMap<Double,Statistic>());
		yname = "deviationFromDesirableTime";
		//yname = "posostoTaskFinishedOnTime";
	}

	public SimpleStatsAggregator(Grid grid) {
		this.grid = grid;
		this.stats = Collections.synchronizedMap(new LinkedHashMap<Double,Statistic>());
	}
	
	public void aggregate() {
		if (isTimeToAggregate()) forcedAggregation();
	}
	
	public void forcedAggregation() {
		lastAggregationTime = grid.getCurrentTime();
		Statistic stat = new Statistic(grid.getCurrentTime());
		
		
		//do stuff and calc various stats:
		
		//numberOfOnlineNodes
		//stat.setValue("numberOfOnlineNodes",grid.getNodesOnline().size());
		
		//meanTaskComputationalWorkload:
		//setTaskMeanComputationalWorkload(stat);
		
		if (setDesirableTimeDeviation(stat)) stats.put(grid.getCurrentTime(),stat);
		//if (setPosostoTaskFinishedBeforeDeadline(stat)) stats.put(grid.getCurrentTime(),stat);
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
		for (Task task:grid.getCompletedTasks()) {
			deviation = (n++*deviation+getDeviationFromDesirableTime(task))/n;
		}
		log.info("deviation:"+deviation);
		stat.setValue(yname,deviation);
		return true;
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
			if (task.getCompletionTime()<=task.getAbsolutCompletionTime()) {
				finishedOnTime++;
			}
		}
		double posostoTaskFinishedOnTime = finishedOnTime / (grid.getCompletedTasks().size()+grid.getFailedTasks().size());
//		if (log.isInfoEnabled()) {
//			log.info("posostoTaskFinishedOnTime:"+posostoTaskFinishedOnTime + " lastvalue="+lastValue);
//		}
		
		if (isLikeLastValue(posostoTaskFinishedOnTime)) {
			if (++consecutiveTimesWithSameValue >= maxConsecutiveTimesWithSameValue) {
				grid.endSimulation();
				log.info("same value calced "+consecutiveTimesWithSameValue+" times. Ending this simulation.");
			}
		} else {
			consecutiveTimesWithSameValue = 0;
			lastValue = posostoTaskFinishedOnTime;
		}
		
		stat.setValue(yname,posostoTaskFinishedOnTime);
		return true;
	}
	
	private boolean isLikeLastValue(double newValue) {
		if (lastValue*(1D-tolerance) < newValue && newValue < lastValue*(1D+tolerance)) return true;
		else return false;
	}

	private void setTaskMeanComputationalWorkload(Statistic stat) {
		double mean = 0;
		double n = 0;
		for (Task task:grid.getAllTasks()) {
			mean = (n++*mean+task.getInitialWorkload())/n;
		}
		stat.setValue("meanTaskComputationalWorkload",mean);
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
