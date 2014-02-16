package gr.upatras.gemu.scheduler;

import gr.upatras.gemu.grid.Grid;
import gr.upatras.gemu.node.Node;
import gr.upatras.gemu.task.Task;
import gr.upatras.gemu.util.Pososto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Team scheduler. (r,s) bounded inside every team.
 * @author George Barelas
 */
public class TeamRSBoundedScheduler implements Scheduler {
	
	static Log log = LogFactory.getLog(TeamRSBoundedScheduler.class);
	static final public String NAME = "TEAM_RS_BOUND";
	static public String NAME_FULL = NAME;
	Grid grid;
	LinkedHashMap<Integer,Double> taskCategories = new LinkedHashMap<Integer,Double>();
	HashMap<Integer,LinkedList<Node>> categoryNodes = new HashMap<Integer,LinkedList<Node>>();
	HashMap<Integer,Scheduler> categoryScheduler = new HashMap<Integer,Scheduler>();
	HashMap<Integer,LinkedList<Task>> categoryTasksLists = new HashMap<Integer,LinkedList<Task>>();
	HashMap<Integer,Double> categoryComputationalCapacity = new HashMap<Integer,Double>();
	HashMap<Node,Integer> allOnlineNodes = new HashMap<Node,Integer>();//Integer: category assigned too: null: no category assigned yet.
	HashMap<Integer,Integer> categoryNumberOfUsers = new HashMap<Integer,Integer>();
	double totalGridCapacity = 0;
	boolean firstRun = true;
	double s = 1000;
	
	public void failTask(Task task) {
		for (Scheduler scheduler:categoryScheduler.values()) {
			scheduler.failTask(task);
		}
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
		countCategoryUsers(availableNodes);
		try {
			teamNodes(availableNodes);
			for (Entry<Integer,LinkedList<Task>> entry:divideTasksIntoCategories(tasksToBeScheduled).entrySet()) {
				
				Scheduler sched = categoryScheduler.get(entry.getKey());
				BeanUtils.setProperty(sched,"numberOfUsers",categoryNumberOfUsers.get(entry.getKey()));
				sched.schedule(entry.getValue(),categoryNodes.get(entry.getKey()),currentTime);
			}
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			System.exit(-1);
		}
	}
	
	private void countCategoryUsers(LinkedList<Node> availableNodes) {
		for (Integer category:categoryScheduler.keySet()) {
			categoryNumberOfUsers.put(category,0);
		}
		for (Node node:availableNodes) {
			int userCategory = (int) Math.round(node.getUserCategory());
			categoryNumberOfUsers.put(userCategory,categoryNumberOfUsers.get(userCategory) + 1);
		}
	}
	
	private HashMap<Integer,LinkedList<Task>> divideTasksIntoCategories(LinkedList<Task> tasks) throws Exception {
		//first, clear lists:
		for (LinkedList<Task> list:categoryTasksLists.values()) {
			list.clear();
		}
		//then, divide tasks per category:
		for (Task task:tasks) {
			categoryTasksLists.get(decideCategory(task)).add(task);
		}
		tasks.clear();
		return categoryTasksLists;
	}
	
	/**
	 * also inits RS Schedulers
	 * also, inits list objects for tasks
	 */
	private void fixCategoriesPososta() {
		double sum = 0;
		for (Entry<Integer,Double> categoryEntry:taskCategories.entrySet()) {
			sum += categoryEntry.getValue();
			categoryScheduler.put(categoryEntry.getKey(),getNewRSBoundedScheduler());
			categoryTasksLists.put(categoryEntry.getKey(),new LinkedList<Task>());
		}
		for (Entry<Integer,Double> categoryEntry:taskCategories.entrySet()) {
			categoryEntry.setValue(categoryEntry.getValue().doubleValue() / sum);
		}
	}
	
	private Scheduler getNewRSBoundedScheduler() {
		LinearlyBoundedUserCalcingRScheduler sched = new LinearlyBoundedUserCalcingRScheduler();
		sched.setS(s);
		sched.setGrid(grid);
		return sched;
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
	
	private double categoryComputationalRatio(int category) {
		if (totalGridCapacity==0) return 0;
		return categoryComputationalCapacity.get(category) / totalGridCapacity;
	}
	
	private void setNodeCategory(Node node,int category) {
		allOnlineNodes.put(node,category);
		incCategoryCapacity(category,node.getComputationalCapacity());
		if (!categoryNodes.get(category).contains(node)) categoryNodes.get(category).add(node);
	}
	
	public void setCategoryPososto(Pososto pososto) throws Exception {
		int category = (int) Math.round(pososto.getValue());
		if (category==0) throw new Exception("Task category can't be zero!!!");
		taskCategories.put(category,pososto.getPososto());
		categoryNodes.put(category,new LinkedList<Node>());
		categoryComputationalCapacity.put(category,0D);
	}
	
	/**
	 * updates total capacity too.
	 * @param category
	 * @param capacity
	 */
	private void incCategoryCapacity(int category,double capacity) {
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
		sb.append("s=").append(s);
		return sb.toString();
	}
	
	public String getSchedulerName() {
		return NAME_FULL;
	}

	public double getS() {
		return s;
	}

	public void setS(double s) {
		this.s = s;
	}
}
