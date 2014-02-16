package gr.upatras.gemu.scheduler;

import gr.upatras.gemu.grid.Grid;
import gr.upatras.gemu.node.Node;
import gr.upatras.gemu.node.TimeInterval;
import gr.upatras.gemu.node.Node.FinishTimes;
import gr.upatras.gemu.node.TimeInterval.NodeStatus;
import gr.upatras.gemu.task.Task;
import gr.upatras.gemu.task.Task.TaskStatus;

import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * ECT: Earliest Completion Time first.
 * @author George Barelas
 */
public class SimpleFCFSqueueECTassignScheduler implements Scheduler {
	
	static Log log = LogFactory.getLog(SimpleFCFSqueueECTassignScheduler.class);
	static final public String NAME = "ECT";
	private Grid grid;

	public void schedule(LinkedList<Task> tasksToBeScheduled,LinkedList<Node> availableNodes,double currentTime) {
		//reasons not to run the scheduler:
		if (tasksToBeScheduled.size()==0 //called with an empty task list:no tasks to be scheduled.
			|| availableNodes.isEmpty() //no nodes to run tasks on. sad...
			) return;
		//main scheduling loop:
		while (!tasksToBeScheduled.isEmpty()) {
			Task task = selectNextTask(tasksToBeScheduled);
			Node node = selectNode(availableNodes,task,currentTime);
			assign(task,node);
		}
	}
	
	/**
	 * FCFS
	 * @param tasks
	 * @return To prwto sth lista.
	 */
	private Task selectNextTask(LinkedList<Task> tasks) {
		return tasks.poll();
	}
	
	/**
	 * ECT: Earliest Completion Time
	 * @param nodes
	 * @param task
	 * @param currentTime
	 * @return
	 */
	private Node selectNode(LinkedList<Node> nodes,Task task,double currentTime) {
		/*Node result = nodes.peek();
		result.calculateFuture();
		double resultCompletionTime = calcCompletionTime(task,result,currentTime);
		for (Node node:nodes) {
			if (node==result) continue;
			node.calculateFuture();
			double nodeCompletionTime = calcCompletionTime(task,node,currentTime);
			if (nodeCompletionTime<resultCompletionTime) {
				resultCompletionTime = nodeCompletionTime;
				result = node;
			}
		}
		return result;*/
		if (log.isDebugEnabled()) {
			log.debug("task to be scheduled:" + task.getTaskNumber());
		}
		Node result = null;
		double resultCompletionTime = Double.POSITIVE_INFINITY;
		for (Node node:nodes) {
			FinishTimes finishTimes = node.getFinishTimesOfAllTasks(task);
			if (log.isDebugEnabled()) {
				//logFinishTimes(finishTimes);
				log.debug("finishTimes:"+finishTimes);
				//log.debug("finishTimes.get(task.getTaskNumber()):" + finishTimes.get(task.getTaskNumber()));
			}
			if (finishTimes.get(task.getTaskNumber()).doubleValue() < resultCompletionTime) {
				result = node;
				resultCompletionTime = finishTimes.get(task.getTaskNumber()).doubleValue();
			}
		}
		return result;
	}
	
	private void logFinishTimes(Map<Double,Double> finishTimes) {
		if (log.isDebugEnabled()) {
			StringBuffer sb = new StringBuffer();
			sb.append("fTimes:");
			for (Entry<Double,Double> entry:finishTimes.entrySet()) {
				sb.append("task#:").append(entry.getKey().doubleValue()).append(",finishTime:").append(entry.getValue().doubleValue());
			}
			log.debug(sb);
		}
	}
	
	/**
	 * @deprecated
	 * @param task
	 * @param node
	 * @param currentTime
	 * @return
	 */
	private double calcCompletionTime(Task task,Node node,double currentTime) {
		LinkedList<TimeInterval> future = node.getFuture();
		LinkedList<TimeInterval> workingIntervals = new LinkedList<TimeInterval>();
		double receiveTime = currentTime + calculateReceiveTime(task,node);
		double computationalTime = task.getInitialWorkload()/node.getComputationalCapacity();
		for (TimeInterval interval:future) {
			if (interval.getStatus()==NodeStatus.COMPUTING) {
				workingIntervals.add(interval);
			}
		}
		for (int i=0;i<workingIntervals.size()-1;i++) {
			TimeInterval i1 = workingIntervals.get(i);
			TimeInterval i2 = workingIntervals.get(i+1);
			if ((computationalTime <= i1.getEndTime() - i2.getStartTime()) && (receiveTime+computationalTime<=i2.getStartTime())) {
				if (receiveTime<=i1.getEndTime()) {
					return i1.getEndTime() + computationalTime;
				} else {
					return receiveTime + computationalTime;
				}
			}
		}
		//den bre8hke ena arketa megalo keno, paei sto telos ths listas:
		double endOfLastTask = 0;
		if (!workingIntervals.isEmpty()) {
			endOfLastTask = workingIntervals.getLast().getEndTime();
		}
		if (receiveTime<=endOfLastTask) {
			return workingIntervals.getLast().getEndTime() + computationalTime;
		} else {
			return receiveTime + computationalTime;
		}
	}
	
	private double calculateReceiveTime(Task task,Node node) {
		if (task.getNodeTransmitingFrom()==node) {
			return 0;
		}
		LinkedList<Task> tasksToSend = task.getNodeTransmitingFrom().getListOfTasksSendingTo(node);
		double bandwidth = node.getLinkToNode(task.getNodeTransmitingFrom()).getBandwidth();
		double result = task.getDataLeftToTrasmit()/bandwidth;
		if (tasksToSend!=null) {
			for (Task taskToSend:tasksToSend) {
				result += taskToSend.getDataLeftToTrasmit()/bandwidth;
			}
		}
		return result;
	}
	
	private void assign(Task task,Node node) {
		task.setStatus(TaskStatus.SCHEDULED);
		task.setNodeTransmitingTo(node);
		task.getNodeTransmitingFrom().addTaskToTransmit(task,null);
		if (log.isDebugEnabled()) {
			log.debug("task #"+task.getTaskNumber()+" with workload:"+task.getInitialWorkload()+" assigned to node #"+node.getNodeNumber()+" with capacity:"+node.getComputationalCapacity());
		}
	}
	
	public String getSchedulerName() {
		return NAME;
	}
	
	public void setGrid(Grid grid) {
		this.grid = grid;
	}
	
	public void failTask(Task task) {
		
	}
}
