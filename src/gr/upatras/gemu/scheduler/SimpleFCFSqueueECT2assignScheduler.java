package gr.upatras.gemu.scheduler;

import gr.upatras.gemu.grid.Grid;
import gr.upatras.gemu.node.Node;
import gr.upatras.gemu.node.Node.FinishTimes;
import gr.upatras.gemu.task.Task;
import gr.upatras.gemu.task.Task.TaskStatus;

import java.util.LinkedList;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * ECT2: ECT, but no running task misses deadline.
 * @author George Barelas
 *
 */
public class SimpleFCFSqueueECT2assignScheduler implements Scheduler {
	static Log log = LogFactory.getLog(SimpleFCFSqueueECT2assignScheduler.class);
	static final public String NAME = "ECT'";
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
			if (node==null) {
				grid.addFailedTask(task);
				continue;
			}
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
