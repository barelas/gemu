package gr.upatras.gemu.task.producer;

import gr.upatras.gemu.grid.Grid;
import gr.upatras.gemu.node.Node;
import gr.upatras.gemu.task.Task;
import gr.upatras.gemu.util.StatisticalCharacteristics;
import gr.upatras.gemu.util.category.CategoryProducer;

import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.random.RandomData;
import org.apache.commons.math.random.RandomDataImpl;

/**
 * Generates {@link Task}s using a Gaussian distibution for their attributes and a Poisson distibution for their arrivals.
 * @author George Barelas
 */
public class PoissonTaskProducer implements TaskProducer {
	
	StatisticalCharacteristics statChars;
	private double taskNumber = 0;
	RandomData randomData;
	Grid grid;
	static Log log = LogFactory.getLog(PoissonTaskProducer.class);
	
	double taskStatsType1Tasks = 0;
	double taskStatsType2Tasks = 0;
	double lastLog = 0;
	CategoryProducer categoryProducer;
	
	public PoissonTaskProducer() {
		this.randomData = new RandomDataImpl();
	}

	public LinkedList<Task> generateTasks() {
		//don't generate new tasks, if there are no noline nodes:
		if (grid.getNodesOnline().isEmpty()) return null;
		if (log.isDebugEnabled()) log.debug("taskNumber="+taskNumber);
		LinkedList<Task> newTasks = null;
		long numberOfNewTasks = randomData.nextPoisson(statChars.getEpochMean());//epochMean==lamda (λ)
		if (numberOfNewTasks>0) {
			newTasks = new LinkedList<Task>();
			for (long i=0;i<numberOfNewTasks;i++) {
				newTasks.add(getNewTask());
			}
		}
		//calcAndPrintTaskStats(newTasks);
		return newTasks;
	}
	
	/**
	 * just for testing purposes...
	 * @param list
	 */
	private void calcAndPrintTaskStats(LinkedList<Task> list) {
		if (list==null) return;
		for (Task t:list) {
			if (t.getTaskCategory()>2.5 && t.getNodeOfOrigin().getUserCategory()>2.5) {
				taskStatsType2Tasks++;
			} else {
				taskStatsType1Tasks++;
			}
		}
		if (lastLog + 500 < taskStatsType1Tasks + taskStatsType2Tasks) {
			log.info("type 1 tasks:"+taskStatsType1Tasks + " type 2 tasks:"+taskStatsType2Tasks);
			lastLog = taskStatsType1Tasks+taskStatsType2Tasks;
		}
	}
	
	private Task getNewTask() {
		long milis = System.currentTimeMillis();
		log.debug("getNewTask() begins");
		double initialWorkload = getRandomValue(statChars.getInitialWorkloadMean(),statChars.getInitialWorkloadDeviation());
		double inputData = getRandomValue(statChars.getInputDataMean(),statChars.getInputDataDeviation());
		double outputData = getRandomValue(statChars.getOutputDataMean(),statChars.getOutputDataDeviation());
		double desirableCompletionTime = getRandomValue(statChars.getDesirableCompletionTimeMean(),statChars.getDesirableCompletionTimeDeviation()) + grid.getCurrentTime();
		double absolutCompletionTime = Math.max(getRandomValue(statChars.getAbsolutCompletionTimeMean(),statChars.getAbsolutCompletionTimeDeviation()) + grid.getCurrentTime(),desirableCompletionTime);
		//Node nodeOfOrigin = selectRandomNodeUniform();
		Node nodeOfOrigin = selectRandomNodeByCpuhunger();
		if (log.isDebugEnabled()) log.debug("getNewTask() ends:"+(System.currentTimeMillis()-milis));
		Task task = new Task(++taskNumber,initialWorkload,inputData,outputData,desirableCompletionTime,absolutCompletionTime,grid,nodeOfOrigin);
		categoryProducer.setNewTaskCategory(task);
		return task;
	}
	
	private double getRandomValue(double mean,double deviation) {
		if (deviation < 0) {//an h diaspora einai arnhtikh, epestreje Poisson dist.
			return randomData.nextPoisson(mean);
		} else if (deviation==0) {//an einai 0, return the mean
			return mean;
		} else {//alliws Gaussian
			return randomData.nextGaussian(mean,deviation);
		}
	}
	
	private Node selectRandomNodeUniform() {
		if (grid.getNodesOnline().size()==1) {
			return grid.getNodesOnline().getFirst();
		}
		return grid.getNodesOnline().get(randomData.nextInt(0,grid.getNodesOnline().size()-1));
	}
	
	private Node selectRandomNodeByCpuhunger() {
		if (grid.getNodesOnline().size()==1) {
			return grid.getNodesOnline().getFirst();
		}
		double mark = randomData.nextUniform(0,getTotalCpuhunger());
		double sum = 0;
		for (Node node:grid.getNodesOnline()) {
			sum += node.getCpuhunger();
			if (sum >= mark) return node;
		}
		return grid.getNodesOnline().getLast();
	}
	
	private double getTotalCpuhunger() {
		double result = 0;
		for (Node node:grid.getNodesOnline()) {
			result += node.getCpuhunger();
		}
		return result;
	}

	public void setGrid(Grid grid) {
		this.grid = grid;
	}

	public void setStatChars(StatisticalCharacteristics statChars) {
		this.statChars = statChars;
	}

	public void setCategoryProducer(CategoryProducer categoryProducer) {
		this.categoryProducer = categoryProducer;
	}
}