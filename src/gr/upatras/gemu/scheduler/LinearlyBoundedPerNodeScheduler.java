package gr.upatras.gemu.scheduler;

import gr.upatras.gemu.grid.Grid;
import gr.upatras.gemu.node.Node;
import gr.upatras.gemu.node.Node.FinishTimes;
import gr.upatras.gemu.task.Task;
import gr.upatras.gemu.task.Task.TaskStatus;

import java.util.HashMap;
import java.util.HashSet;
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
public class LinearlyBoundedPerNodeScheduler implements Scheduler {
	
	static Log log = LogFactory.getLog(LinearlyBoundedPerNodeScheduler.class);
	static final public String NAME = "LINEAR_BOUND_PER_NODE";
	Grid grid;
	LinkedList<Task> tasksWaitingForBoundToRise = new LinkedList<Task>();
	HashMap<Node,HashMap<Node,Double>> submittedWorkloadPerNode = new HashMap<Node,HashMap<Node,Double>>();
	double r = 1;
	double s = 1000;
	
	public void schedule(LinkedList<Task> tasksToBeScheduled,LinkedList<Node> availableNodes,double currentTime) {
		//log.info("entering schedule()...");long milis = System.currentTimeMillis();
		//reasons not to run the scheduler:
		if ((tasksToBeScheduled.isEmpty() && tasksWaitingForBoundToRise.isEmpty()) //empty task list:no tasks to be scheduled.
			|| availableNodes.isEmpty() //no nodes to run tasks on. sad...
			) return;
		
		//main scheduling loops:
		
		for (Iterator<Task> it = tasksWaitingForBoundToRise.iterator();it.hasNext();) {
			tasksToBeScheduled.addFirst(it.next());
			it.remove();
		}
		while (!tasksToBeScheduled.isEmpty()) {
			Task task = selectNextTask(tasksToBeScheduled);
			
			Node node = null;
			boolean waitingForBoundToRise = false;
			//HashSet<Node> nodesNotToBeSelected = new HashSet<Node>();
			HashSet<Node> nodesNotToBeSelected = getNodesNotToBeSelectedSet(availableNodes,task);
			if (nodesNotToBeSelected.size() >= availableNodes.size()) {
				addWaitingTask(task);
				continue;
			}
			while (nodesNotToBeSelected.size() < availableNodes.size()) {
				node = selectNode(availableNodes,task,currentTime,nodesNotToBeSelected);
				if (node==null) {//kanenas kombos de mporei na to dextei.
					break;
				}
				if (nodesRequestedWorkloadIsUnderBound(task,node)) {
					waitingForBoundToRise = false;
					break;
				} else {
					/*
					 * ston ypojhfio kombo, mporei na paei apo thn ECT' epilogh (den xalaei to deadline kanenos allou task),
					 * alla den mporei logw tou (r,s) oriou.
					 */
					nodesNotToBeSelected.add(node);
					waitingForBoundToRise = true;
				}
			}
			if (waitingForBoundToRise) {
				addWaitingTask(task);
				continue;
			}
			if (node==null) {
				grid.addFailedTask(task);
				continue;
			}
			assign(task,node);
		}
		//log.info("schedule() done. milis:" + (System.currentTimeMillis()-milis));
	}
	
	/**
	 * Returns a Set with all the nodes that must be excluded from selection due to linear bound.
	 * @param availableNodes
	 * @param task
	 * @return
	 */
	private HashSet<Node> getNodesNotToBeSelectedSet(LinkedList<Node> availableNodes,Task task) {
		HashSet<Node> set = new HashSet<Node>();
		for (Node node:availableNodes) {
			if (!nodesRequestedWorkloadIsUnderBound(task,node)) {
				set.add(node);
			}
		}
		return set;
	}
	
	private void addWaitingTask(Task task) {
		tasksWaitingForBoundToRise.addLast(task);
	}
	
	private boolean nodesRequestedWorkloadIsUnderBound(Task task,Node nodeToBeScheduledTo) {
		double bound = r * (grid.getCurrentTime() - task.getNodeOfOrigin().getCreationTime()) + s;
		double allreadyGivenCapacity = 0;
		if (submittedWorkloadPerNode.containsKey(nodeToBeScheduledTo) && submittedWorkloadPerNode.get(nodeToBeScheduledTo).containsKey(task.getNodeOfOrigin())) {
			allreadyGivenCapacity = submittedWorkloadPerNode.get(nodeToBeScheduledTo).get(task.getNodeOfOrigin());
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
	private Node selectNode(LinkedList<Node> nodes,Task task,double currentTime,HashSet<Node> nodesNotToBeSelected) {
		if (log.isDebugEnabled()) {
			log.debug("task to be scheduled:" + task.getTaskNumber());
		}
		Node result = null;
		double resultCompletionTime = Double.POSITIVE_INFINITY;
		for (Node node:nodes) {
			if (nodesNotToBeSelected.contains(node)) continue;
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
		if (!submittedWorkloadPerNode.containsKey(node)) {
			submittedWorkloadPerNode.put(node,new HashMap<Node,Double>());
		}
		HashMap<Node,Double> workloadsSubmittedToThisNode = submittedWorkloadPerNode.get(node);
		if (workloadsSubmittedToThisNode.containsKey(task.getNodeOfOrigin())) {
			allreadyGivenCapacity = workloadsSubmittedToThisNode.get(task.getNodeOfOrigin());
		}
		workloadsSubmittedToThisNode.put(task.getNodeOfOrigin(),allreadyGivenCapacity + task.getInitialWorkload());
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
