package gr.upatras.gemu.grid;

import gr.upatras.gemu.node.Node;
import gr.upatras.gemu.node.departure.DepartureSchema;
import gr.upatras.gemu.node.departure.FixedDepartTimeSchema;
import gr.upatras.gemu.node.link.producer.NodeLinkProducer;
import gr.upatras.gemu.node.link.producer.NormalLinkProducer;
import gr.upatras.gemu.node.producer.NodeProducer;
import gr.upatras.gemu.node.producer.SimpleNodeProducer;
import gr.upatras.gemu.scheduler.Scheduler;
import gr.upatras.gemu.scheduler.SimpleFCFSqueueECTassignScheduler;
import gr.upatras.gemu.stats.SimpleStatsAggregator;
import gr.upatras.gemu.stats.StatsAggregator;
import gr.upatras.gemu.task.Task;
import gr.upatras.gemu.task.Task.TaskStatus;
import gr.upatras.gemu.task.producer.NormalTaskProducer;
import gr.upatras.gemu.task.producer.TaskProducer;
import gr.upatras.gemu.util.StatisticalCharacteristics;

import java.io.FileOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents a single simulation.
 * @author George Barelas
 */
public class Grid {
	
	LinkedList<Task> tasksWaitingToBeScheduled;
	LinkedList<Task> completedTasks;
	LinkedList<Task> allTasks;
	LinkedList<Task> failedTasks;
	LinkedList<Node> nodesOnline;
	LinkedList<Node> nodesDeparted;
	Set<Task> tasksInTheGrid;//ta tasks pou einai energa: exoun ginei submit pros ypologismo, alla den exoun oloklhrw8ei h' apotyxei.
	private double currentTime;
	private double finishTime;
	TaskProducer taskProducer;
	NodeProducer nodeProducer;
	Scheduler scheduler;
	LinkedHashMap<String,StatsAggregator> statsAggregators;
	static Log log = LogFactory.getLog(Grid.class);
	public final static String RESULT_FILE_EXTENSION = ".res";
	
	public Grid() {
		nodesOnline = new LinkedList<Node>();
		nodesDeparted = new LinkedList<Node>();
		tasksWaitingToBeScheduled = new LinkedList<Task>();
		completedTasks = new LinkedList<Task>();
		allTasks = new LinkedList<Task>();
		failedTasks = new LinkedList<Task>();
		tasksInTheGrid = new LinkedHashSet<Task>();
		statsAggregators = new LinkedHashMap<String,StatsAggregator>();
		currentTime = 0;
	}
	
	public static void main(String[] args) throws Exception {
		Grid grid = new Grid();
		grid.init();
		grid.run();
	}
	
	public String getSchedulerName() {
		return scheduler.getSchedulerName();
	}
	
	/**
	 * Kaleitai otan anaxwrei enas kombos.
	 * @param node
	 */
	public void nodeDeparture(Node node) throws Exception {
		//Prwta pare ta tasks twn opoion ta data dexetai.
		for (Task task:node.getTasksReceiving()) {
			task.getNodeTransmitingFrom().cancelSendingTask(task);
			if (task.getNodeOfOrigin()==node) {
				task.markNodeOfOriginDeparted();
				node.removePendingTask(task);
				addFailedTask(task);
			} else {
				task.reinit();
				tasksWaitingToBeScheduled.add(task);
			}
		}
		//auta pou exei pros ypologismo:
		LinkedList<Task> tmp = node.getUnfinishedTasks();
		for (Iterator<Task> iter = tmp.iterator();iter.hasNext();) {
			Task task = iter.next();
			if (task.getNodeOfOrigin()==node) {
				task.markNodeOfOriginDeparted();
				node.removePendingTask(task);
				addFailedTask(task);
				iter.remove();
			}
		}
		tasksWaitingToBeScheduled.addAll(reinitTasks(tmp));
		node.removeTransmitingTasks();
		node.cancelPendingTasks();
		node.markDepartureTime();
		nodesDeparted.add(node);
	}
	
	public static LinkedList<Task> reinitTasks(LinkedList<Task> tasks) {
		for (Task task:tasks) {
			task.reinit();
		}
		return tasks;
	}

	public double getCurrentTime() {
		return currentTime;
	}
	
	public LinkedList<Node> getNodesOnline() {
		return nodesOnline;
	}
	
	/**
	 * (just for testing reasons)
	 * @deprecated
	 */
	public void init() {
		this.currentTime = 0;
		//link producer:
		StatisticalCharacteristics linkStats = new StatisticalCharacteristics();
		linkStats.setBandwidthMean(1000);
		linkStats.setBandwidthDeviation(1000);
		NodeLinkProducer linkProducer = new NormalLinkProducer(linkStats);
		
		//node producer:
		//DepartureSchema departureSchema = new NeverDepartSchema();
		DepartureSchema departureSchema = new FixedDepartTimeSchema(1e4);
		StatisticalCharacteristics nodeStats = new StatisticalCharacteristics();
		nodeStats.setCcDeviation(1000);
		nodeStats.setCcMean(10000);
		nodeStats.setEpochMean(10000);
		nodeStats.setEpochDeviation(100);
		this.nodeProducer = new SimpleNodeProducer(nodeStats,this,departureSchema,linkProducer);
		
		//task producer:
		StatisticalCharacteristics taskStats = new StatisticalCharacteristics();
		taskStats.setEpochMean(100);
		taskStats.setEpochDeviation(100);
		taskStats.setInitialWorkloadMean(1e5D);
		taskStats.setInitialWorkloadDeviation(1e6D);
		taskStats.setDesirableCompletionTimeMean(200);
		taskStats.setDesirableCompletionTimeDeviation(100);
		taskStats.setAbsolutCompletionTimeMean(200);
		taskStats.setAbsolutCompletionTimeDeviation(100);
		taskStats.setInputDataMean(10000);
		taskStats.setInputDataDeviation(10000);
		taskStats.setOutputDataMean(10000);
		taskStats.setOutputDataDeviation(10000);
		this.taskProducer = new NormalTaskProducer(taskStats,this);
		
		this.scheduler = new SimpleFCFSqueueECTassignScheduler();
		
		this.addStatsAggregator(new SimpleStatsAggregator(this));
	}
	
	private double calculateLogEpochs() {
		return Math.floor(finishTime / 10);
	}
	
	/**
	 * Main loop.
	 *
	 */
	public void run() throws Exception {
		double lastTime = 0;
		double logEpochs = calculateLogEpochs();
		long startTime = 0;
		if (log.isInfoEnabled()) {
			startTime = System.currentTimeMillis();
		}
		setThisGridToAll();
		for (;;) {
			++currentTime;
			if (currentTime>=finishTime) break;
			if (log.isInfoEnabled() && currentTime >= lastTime + logEpochs) {
				log.info("epoch:" + currentTime + " scheduler:" + scheduler.getSchedulerName() + " grid:"+this);
				lastTime += logEpochs;
				/*for (Task task:allTasks) {
					task.debug();
				}*/
				/*for (Node node:nodesOnline) {
					node.debugFuture();
				}*/
			}
			nodeProducer.generateNodes(nodesOnline);
			LinkedList<Task> newTasks = taskProducer.generateTasks();
			if (newTasks!=null) {
				for (Task task:newTasks) {
					allTasks.add(task);
					tasksWaitingToBeScheduled.add(task);
					tasksInTheGrid.add(task);
				}
			}
			//log.debug("# of nodes:"+nodesOnline.size());
			//log.debug("# of tasks to be scheduled:"+tasksWaitingToBeScheduled.size());
			//log.debug("# of completed tasks:"+completedTasks.size());
			scheduler.schedule(tasksWaitingToBeScheduled,nodesOnline,currentTime);
			for (Iterator<Node> iter = nodesOnline.iterator();iter.hasNext();) {
				Node node = iter.next();
				node.nextEpoch();
				if (node.isToDepart()) iter.remove();
			}
			
			failTasksThatExceededAbsolutCompletionTime();
			
			//let the aggregators get stats, if they want to:
			for (Entry<String,StatsAggregator> entry:statsAggregators.entrySet()) {
				entry.getValue().aggregate();
			}
			
		}
		if (log.isInfoEnabled()) {
			log.info("duration of grid simulation (seconds):" + (System.currentTimeMillis()-startTime)/1000);
		}
	}
	
	/**
	 * Remove from all lists and queues: running, sending, etc... 
	 * @param task Task to be removed for the Grid.
	 */
	private void failTask(Task task) throws Exception {
		switch (task.getStatus()) {
		case WAITING_TO_BE_SCHEDULED:
			tasksWaitingToBeScheduled.remove(task);
			task.getNodeOfOrigin().removePendingTask(task);
			scheduler.failTask(task);
			break;
		case SCHEDULED:
			task.getNodeTransmitingFrom().cancelSendingTask(task);
			task.getNodeOfOrigin().removePendingTask(task);
			task.getNodeTransmitingTo().cancelTask(task);
			break;
		case DONE_CALC:
			task.getNodeTransmitingFrom().cancelSendingTask(task);
			task.getNodeOfOrigin().removePendingTask(task);
			task.getNodeTransmitingTo().cancelTask(task);
			break;
		case FAILED:
		case DONE:
			
			break;
		case RUNNING:
			task.getNodeOfOrigin().removePendingTask(task);
			task.getNodeRunningOn().cancelTask(task);
			break;
		default:
			String errorString = "Uknown TaskStatus encundered:" + task.getStatus();
			log.error(errorString);
			throw new Exception(errorString);
			//break;
		}
		
		task.setStatus(TaskStatus.FAILED);
	}
	
	/**
	 * (mporei na einai scheduler feature kai oxi basic grid infrastructure feature.)
	 *
	 */
	private void failTasksThatExceededAbsolutCompletionTime() throws Exception {
		for (Iterator<Task> it = tasksInTheGrid.iterator();it.hasNext();) {
			Task task = it.next();
			if (task.getAbsolutCompletionTime() < currentTime) {
				it.remove();
				failedTasks.add(task);
				failTask(task);
			}
		}
	}
	
	public void setThisGridToAll() {
		taskProducer.setGrid(this);
		nodeProducer.setGrid(this);
		for (Entry<String,StatsAggregator> entry:statsAggregators.entrySet()) {
			entry.getValue().setGrid(this);
		}
		scheduler.setGrid(this);
	}
	
	public void addFinishedTask(Task task) {
		task.setCompletionTime(currentTime);
		this.completedTasks.add(task);
		tasksInTheGrid.remove(task);
	}
	
	public void saveAggregationsToFiles(String filename) {
		if (statsAggregators.isEmpty()) return;
		for (Entry<String,StatsAggregator> entry:statsAggregators.entrySet()) {
			saveToFile(filename + "." + entry.getValue().getYname() + RESULT_FILE_EXTENSION,entry.getValue());
		}
	}
	
	private void saveToFile(String filename,Object obj) {
		try {
			FileOutputStream f = new FileOutputStream(filename);
			ObjectOutput s = new ObjectOutputStream(f);
			s.writeObject(obj);
			s.flush();
			f.close();
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			return;
		}
	}
	
	public double getTotalOnlineCapacity() {
		double C = 0;
		for (Node node:nodesOnline) {
			C += node.getComputationalCapacity();
		}
		return C;
	}
	
	/**
	 * Prepei na kaleitai gia ka8e failed task.
	 * @param task
	 */
	public void addFailedTask(Task task) {
		failedTasks.add(task);
		tasksInTheGrid.remove(task);
	}
	
	/**
	 * Will end simulation at the start of the next simulation circle.
	 */
	public void endSimulation() {
		currentTime = Double.MAX_VALUE - 1;
	}
	
	public void addTaskForScheduling(Task task) {
		tasksWaitingToBeScheduled.add(task);
	}

	public void setNodeProducer(NodeProducer nodeProducer) {
		this.nodeProducer = nodeProducer;
	}

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	public void addStatsAggregator(StatsAggregator statsAggregator) {
		statsAggregators.put(statsAggregator.getYname(),statsAggregator);
	}

	public void setTaskProducer(TaskProducer taskProducer) {
		this.taskProducer = taskProducer;
	}
	
	public void setFinishTime(double finishTime) {
		this.finishTime = finishTime;
	}

	public LinkedList<Task> getAllTasks() {
		return allTasks;
	}

	public LinkedList<Task> getCompletedTasks() {
		return completedTasks;
	}

	public LinkedList<Task> getFailedTasks() {
		return failedTasks;
	}

	public LinkedList<Node> getNodesDeparted() {
		return nodesDeparted;
	}

	public LinkedHashMap<String,StatsAggregator> getStatsAggregators() {
		return statsAggregators;
	}
}
