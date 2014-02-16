package gr.upatras.gemu.scheduler;

import gr.upatras.gemu.grid.Grid;
import gr.upatras.gemu.node.Node;
import gr.upatras.gemu.node.Node.FinishTimes;
import gr.upatras.gemu.task.Task;
import gr.upatras.gemu.task.Task.TaskStatus;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * (r,s) bounded users. (r,s) common for all users-nodes.
 * @author barelas
 *
 */
public class LinearlyBoundedUserScheduler implements Scheduler {
	
	static Log log = LogFactory.getLog(LinearlyBoundedUserScheduler.class);
	static final public String NAME = "LINEAR_BOUND";
	Grid grid;
	LinkedList<Task> tasksWaitingForBoundToRise = new LinkedList<Task>();
	HashMap<Node,Double> nodesRequestedWorkloadHistory = new HashMap<Node,Double>();
	double r = 1;
	double s = 1000;
	
	public void schedule(LinkedList<Task> tasksToBeScheduled,LinkedList<Node> availableNodes,double currentTime) {
		//reasons not to run the scheduler:
		if ((tasksToBeScheduled.isEmpty() && tasksWaitingForBoundToRise.isEmpty()) //empty task list:no tasks to be scheduled.
			|| availableNodes.isEmpty() //no nodes to run tasks on. sad...
			) return;
		
		//main scheduling loops:
		
		for (Iterator<Task> it = tasksWaitingForBoundToRise.iterator();it.hasNext();) {
			Task task = it.next();
			if (nodesRequestedWorkloadIsUnderBound(task)) {
				tasksToBeScheduled.addFirst(task);
				it.remove();
			}
		}
		while (!tasksToBeScheduled.isEmpty()) {
			Task task = selectNextTask(tasksToBeScheduled);
			if (!nodesRequestedWorkloadIsUnderBound(task)) {
				addWaitingTask(task);
				continue;
			}
			Node node = selectNode(availableNodes,task,currentTime);
			if (node==null) {
				grid.addFailedTask(task);
				continue;
			}
			assign(task,node);
		}
	}
	
	private void addWaitingTask(Task task) {
		tasksWaitingForBoundToRise.addLast(task);
	}
	
	private boolean nodesRequestedWorkloadIsUnderBound(Task task) {
		double bound = r * (grid.getCurrentTime() - task.getNodeOfOrigin().getCreationTime()) + s;
		double allreadyGivenCapacity = 0;
		if (nodesRequestedWorkloadHistory.containsKey(task.getNodeOfOrigin())) {
			allreadyGivenCapacity = nodesRequestedWorkloadHistory.get(task.getNodeOfOrigin());
		}
		return allreadyGivenCapacity + task.getInitialWorkload() <= bound;
	}
	
	private Task selectNextTask(LinkedList<Task> tasks) {
		return tasks.poll();
	}
	
	/**
	 * ECT': Earliest Completion Time, arkei na mhn xasei kapoio task to deadline tou. 
	 * @param nodes
	 * @param task
	 * @param currentTime
	 * @return
	 */
	private Node selectNode(LinkedList<Node> nodes,Task task,double currentTime) {
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
				if (doesATaskMissesDeadline(finishTimes)) continue;
				result = node;
				resultCompletionTime = finishTimes.get(task.getTaskNumber()).doubleValue();
			}
		}
		return result;
	}
	
	private boolean doesATaskMissesDeadline(FinishTimes finishTimes) {
		boolean result = false;
		for (Entry<Task,Double> entry:finishTimes.getTasks().entrySet()) {
			if (entry.getKey().getAbsolutCompletionTime() < entry.getValue().doubleValue()) {
				result = true;
				break;
			}
		}
		return result;
	}
	
	private void assign(Task task,Node node) {
		task.setStatus(TaskStatus.SCHEDULED);
		task.setNodeTransmitingTo(node);
		task.getNodeTransmitingFrom().addTaskToTransmit(task,null);
		double allreadyGivenCapacity = 0;
		if (nodesRequestedWorkloadHistory.containsKey(task.getNodeOfOrigin())) {
			allreadyGivenCapacity = nodesRequestedWorkloadHistory.get(task.getNodeOfOrigin());
		}
		nodesRequestedWorkloadHistory.put(task.getNodeOfOrigin(),allreadyGivenCapacity + task.getInitialWorkload());
		if (log.isDebugEnabled()) {
			log.debug("task #"+task.getTaskNumber()+" with workload:"+task.getInitialWorkload()+" assigned to node #"+node.getNodeNumber()+" with capacity:"+node.getComputationalCapacity());
		}
	}
	
	public void failTask(Task task) {
		tasksWaitingForBoundToRise.remove(task);
	}
	
	public String getSchedulerName() {
		return NAME + " (r=" + r + ",s=" + s + ")";
	}
	
	public void setGrid(Grid grid) {
		this.grid = grid;
	}

	public void setR(double r) {
		this.r = r;
	}

	public void setS(double s) {
		this.s = s;
	}

}
