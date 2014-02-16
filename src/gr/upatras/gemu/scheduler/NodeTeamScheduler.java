package gr.upatras.gemu.scheduler;

import gr.upatras.gemu.grid.Grid;
import gr.upatras.gemu.node.Node;
import gr.upatras.gemu.node.Node.FinishTimes;
import gr.upatras.gemu.task.Task;
import gr.upatras.gemu.task.Task.TaskStatus;
import gr.upatras.gemu.util.Pososto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Team scheduler.
 * @author George Barelas
 */
public class NodeTeamScheduler implements Scheduler {
	
	static Log log = LogFactory.getLog(NodeTeamScheduler.class);
	static final public String NAME = "NODE_TEAM";
	static public String NAME_FULL = NAME;
	Grid grid;
	LinkedHashMap<Integer,Double> taskCategories = new LinkedHashMap<Integer,Double>();
	HashMap<Integer,HashSet<Node>> categoryNodes = new HashMap<Integer,HashSet<Node>>();
	HashMap<Integer,Double> categoryComputationalCapacity = new HashMap<Integer,Double>();
	HashMap<Node,Integer> allOnlineNodes = new HashMap<Node,Integer>();//Integer: category assigned too: null: no category assigned yet.
	double totalGridCapacity = 0;
	boolean firstRun = true;
	
	public void failTask(Task task) {
		
	}
	
	public void schedule(LinkedList<Task> tasksToBeScheduled,LinkedList<Node> availableNodes, double currentTime) {
		if (firstRun) {
			fixCategoriesPososta();
			NAME_FULL = getSchedulerNameFull();
			firstRun = false;
		}
		if (
				tasksToBeScheduled.isEmpty() //empty task list:no tasks to be scheduled.
				|| availableNodes.isEmpty() //no nodes to run tasks on. sad...
				) return;
		try {
			teamNodes(availableNodes);
			while (!tasksToBeScheduled.isEmpty()) {
				Task task = selectNextTask(tasksToBeScheduled);
				Node node = selectNode(categoryNodes.get(decideCategory(task)),task);
				if (node==null) {
					grid.addFailedTask(task);
					continue;
				}
				assign(task,node);
			}
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			System.exit(-1);
		}
	}
	
	private void fixCategoriesPososta() {
		double sum = 0;
		for (Entry<Integer,Double> categoryEntry:taskCategories.entrySet()) {
			sum += categoryEntry.getValue();
		}
		for (Entry<Integer,Double> categoryEntry:taskCategories.entrySet()) {
			categoryEntry.setValue(categoryEntry.getValue().doubleValue() / sum);
		}
	}
	
	/**
	 * ECT': Earliest Completion Time, arkei na mhn xasei kapoio task to deadline tou. 
	 * @param nodes
	 * @param task
	 * @return
	 */
	private Node selectNode(Set<Node> nodes,Task task) {
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
	
	private Task selectNextTask(LinkedList<Task> tasks) {
		return tasks.poll();
	}
	
	/**
	 * Omadopoiei tous kombous.
	 * @param allNodes
	 * @throws Exception
	 */
	private void teamNodes(LinkedList<Node> allNodes) throws Exception {
		HashSet<Node> allNodesSet = new HashSet<Node>();
		allNodesSet.addAll(allNodes);
		//first, for nodes that went offline:
		for (Iterator<Entry<Node,Integer>> it = allOnlineNodes.entrySet().iterator();it.hasNext();) {
			Entry<Node,Integer> entry = it.next();
			if (!allNodesSet.contains(entry.getKey())) {//node went offline...
				incCategoryCapacity(entry.getValue(),-entry.getKey().getComputationalCapacity());
				categoryNodes.get(entry.getValue()).remove(entry.getKey());
				it.remove();
			}
		}
		
		//then decide category for new nodes:
		for (Node node:allNodes) {
			if (!allOnlineNodes.containsKey(node)) {
				setNodeCategory(node,decideCategory(node));
			}
		}
	}
	
	protected int decideCategory(Task task) throws Exception {
		return (int) Math.round(task.getNodeOfOrigin().getUserCategory());
	}
	
	/**
	 * Choose categories that are below their ratio.
	 * From them, choose the relevantly lowest one.
	 * @param node This implementation does not use the node param,
	 * but needed for overloading and code clarity reasons.
	 * @return
	 */
	private int decideCategory(Node node) throws Exception {
		int result = 0;
		LinkedHashMap<Integer,Double> candidateCategories = new LinkedHashMap<Integer,Double>();
		for (Entry<Integer,Double> category:taskCategories.entrySet()) {
			if (category.getValue() >= categoryComputationalRatio(category.getKey())) {
				candidateCategories.put(category.getKey(),category.getValue());
			}
		}
		if (candidateCategories.size()==0) {
			throw new Exception("candidateCategories.size()==0!!! No way, something else is wrong!!!");
		} else if (candidateCategories.size()==1) {
			return candidateCategories.keySet().iterator().next();
		}
		double biggestDistanceFromDesirable = 0;
		for (Entry<Integer,Double> category:candidateCategories.entrySet()) {
			double distanceFromDesirable = (category.getValue()-categoryComputationalRatio(category.getKey())) / category.getValue();
			if (distanceFromDesirable >= biggestDistanceFromDesirable) {
				biggestDistanceFromDesirable = distanceFromDesirable;
				result = category.getKey();
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
		try {
			if (decideCategory(task) != allOnlineNodes.get(node)) {
				log.error("task category="+decideCategory(task) + " nodeCategory="+allOnlineNodes.get(node));
			}
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
	}
	
	private double categoryComputationalRatio(int category) {
		if (totalGridCapacity==0) return 0;
		return categoryComputationalCapacity.get(category) / totalGridCapacity;
	}
	
	private void setNodeCategory(Node node,int category) {
		allOnlineNodes.put(node,category);
		incCategoryCapacity(category,node.getComputationalCapacity());
		categoryNodes.get(category).add(node);
	}
	
	public void setCategoryPososto(Pososto pososto) throws Exception {
		int category = (int) Math.round(pososto.getValue());
		if (category==0) throw new Exception("Task category can't be zero!!!");
		taskCategories.put(category,pososto.getPososto());
		categoryNodes.put(category,new HashSet<Node>());
		categoryComputationalCapacity.put(category,0D);
	}
	
	/**
	 * updates total capacity too.
	 * @param category
	 * @param capacity
	 */
	private void incCategoryCapacity(int category,double capacity) {
//		log.info("category="+category);
//		log.info("categoryComputationalCapacity="+categoryComputationalCapacity);
//		log.info("categoryComputationalCapacity.get(category)="+categoryComputationalCapacity.get(category));
		double newCategoryCapacity = categoryComputationalCapacity.get(category) + capacity;
		if (newCategoryCapacity<0) newCategoryCapacity = 0;
		categoryComputationalCapacity.put(category,newCategoryCapacity);
		totalGridCapacity += capacity;
	}
	
	public void setGrid(Grid grid) {
		this.grid = grid;
	}
	
	private String getSchedulerNameFull() {
		StringBuffer sb = new StringBuffer();
		sb.append(NAME).append(' ');
		for (Entry<Integer,Double> categoryEntry:taskCategories.entrySet()) {
			sb.append(categoryEntry.getKey()).append(':').append(Math.round(categoryEntry.getValue().doubleValue()*100D)).append("%,");
		}
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}
	
	public String getSchedulerName() {
		return NAME_FULL;
	}
}
