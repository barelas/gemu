package gr.upatras.gemu.node;

import gr.upatras.gemu.grid.Grid;
import gr.upatras.gemu.node.TimeInterval.NodeStatus;
import gr.upatras.gemu.node.departure.DepartureSchema;
import gr.upatras.gemu.node.link.NodeLink;
import gr.upatras.gemu.node.weight.Weight;
import gr.upatras.gemu.task.Task;
import gr.upatras.gemu.task.Task.TaskStatus;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents a computational node.
 * Assumption: Node==User. Enas user taytizetai me ton Node.
 * @author George Barelas
 */
public class Node {
	
	double computationalCapacity = 0;
	double nodeNumber = 0;
	double creationTime = 0;
	double departTime = 0;
	DepartureSchema departureSchema;
	Task runningTask = null;
	LinkedList<Task> tasksToCompute;
	LinkedList<Task> tasksReceiving;
	LinkedList<Task> tasksPending;
	Grid grid;
	double numberOfEpochsNotUsed = 0;
	HashMap<Node,NodeLink> connectedNodes;
	HashMap<Node,LinkedList<Task>> transmitLists;
	NodeStatus nodeStatus = NodeStatus.IDLE;
	LinkedList<TimeInterval> future;
	double computatedTasks = 0;
	boolean toDepart = false;
	static Log log = LogFactory.getLog(Node.class);
	boolean sharedCpu = true;//activate if u want shared CPU model. If false, this node calcs the tasks one-by-one.
	HashMap<Node,Weight> weights;
	double cpuhunger = 1;
	double requestedComputanionalCapacity = 0;
	double grantedComputanionalCapacity = 0;
	double userCategory = 1;
	
	public Node(double creationTime,DepartureSchema departureSchema,double nodeNumber,double computationalCapacity,Grid grid) {
		this.creationTime = creationTime;
		this.departureSchema = departureSchema;
		this.tasksToCompute = new LinkedList<Task>();
		this.tasksReceiving = new LinkedList<Task>();
		this.tasksPending = new LinkedList<Task>();
		this.connectedNodes = new HashMap<Node,NodeLink>();
		this.transmitLists = new HashMap<Node,LinkedList<Task>>();
		this.weights = new HashMap<Node,Weight>();
		this.nodeNumber = nodeNumber;
		this.computationalCapacity = computationalCapacity;
		this.grid = grid;
	}
	
	/**
	 * Basikes leitourgies. Kaleitai mia fora se ka8e epoch kai ektelei 3 leitourgies:
	 * 1. Apofasizetai an o kombos anaxwrei, xrhsimopoiwntas to departureSchema.
	 * 2. Ektelei to computation.
	 * 3. Apostelei data mesw tou diktyou.
	 */
	public void nextEpoch() throws Exception {
		if (log.isDebugEnabled()) {
			calculateFuture();
			log.debug(getFutureString());
		}
		
		//anaxwrei?:
		if (!departureSchema.nextEpoch()) {
			toDepart = true;
			grid.nodeDeparture(this);
			return;
		}
		
		if (sharedCpu) {
			//diamoirazomenh CPU:
			if (tasksToCompute.isEmpty()) {
				numberOfEpochsNotUsed++;
			} else {
				for (Iterator<Task> it = tasksToCompute.iterator();it.hasNext();) {
					Task task = it.next();
					if (task.makeComputation()) {
						computatedTasks++;
						addTaskToTransmit(task,null);//epistrofh sto node of origin.
						it.remove();
					}
				}
			}
		} else {
			//olh h CPU se ena task.
			//compute apo to trexwn task mono:
			if (runningTask==null) {
				runningTask = tasksToCompute.poll();
				if (runningTask!=null) runningTask.markStartCalc();
			}
			if (runningTask==null) {
				numberOfEpochsNotUsed++;
			} else {
				if (runningTask.makeComputation()) {
					computatedTasks++;
					addTaskToTransmit(runningTask,null);//epistrofh sto node of origin.
					runningTask = null;
				}
			}
		}
		
		//send data apo tasks pou exeis pros apostolh:
		for (Iterator<Node> iter = transmitLists.keySet().iterator();iter.hasNext();) {
			Node nodeToTransmitTo = iter.next();
			LinkedList<Task> listOfTasksToBeTransmitted = transmitLists.get(nodeToTransmitTo);
			if (nodeToTransmitTo==this) {
				for (Task task:listOfTasksToBeTransmitted) {
					addTaskToCompute(task);
				}
				iter.remove();
				continue;
			}
			Task task = listOfTasksToBeTransmitted.peek();
			if (task.trasmit(connectedNodes.get(nodeToTransmitTo).getBandwidth())) {
				nodeToTransmitTo.addTaskToCompute(task);
				listOfTasksToBeTransmitted.poll();
				if (listOfTasksToBeTransmitted.isEmpty()) {
					iter.remove();
				}
			}
		}
	}
	
	/**
	 * (for departure)
	 * @return Ta tasks pou exoun anate8ei ston kombo, exoun kai ta data tous edw, alla den exoun oloklhrw8ei.
	 */
	public LinkedList<Task> getUnfinishedTasks() {
		if (runningTask!=null) {
			tasksToCompute.addFirst(runningTask);
		}
		return tasksToCompute;
	}
	
	public void addConnectedNode(Node node,NodeLink link) {
		connectedNodes.put(node,link);
	}
	
	public void addTaskToCompute(Task task) {
		if (!tasksReceiving.remove(task)) {
			log.error("Tried to add task to tasksToCompute, that wasn't on the tasksReceiving list.");
			log.error("Offending task:" + task);
			log.error("tasksReceiving" + tasksReceiving);
			return;
		}
		if (task.isFinishedCalculation() && task.getNodeOfOrigin()==this) {//epistrefei ston kombo proeleysews, telos gia to task auto!
			task.setStatus(TaskStatus.DONE);
			grantedComputanionalCapacity += task.getInitialWorkload();
			tasksPending.remove(task);
			grid.addFinishedTask(task);
			return;
		}
		tasksToCompute.add(task);
		if (sharedCpu) task.markStartCalc();
		task.setNodeRunningOn(this);
	}
	
	/**
	 * Pros8etei ena task sthn lista pros metadosh. An h listra gia ton komvo pou 8a
	 * paei den yparxei, dhmiourgeitai. Pros8etei kai to task sthn lista me ta receivingTasks
	 * tou komvou-stoxou.
	 * @param task The task to add for transmition.
	 * @param transmitLists If null, then use Node's transmitLists. Use non-null when calculating future.
	 */
	public void addTaskToTransmit(Task task,HashMap<Node,LinkedList<Task>> transmitLists) {
		if (transmitLists==null) transmitLists = this.transmitLists;
		if (!transmitLists.containsKey(task.getNodeTransmitingTo())) {
			transmitLists.put(task.getNodeTransmitingTo(),new LinkedList<Task>());
		}
		transmitLists.get(task.getNodeTransmitingTo()).add(task);
		if (transmitLists==this.transmitLists) task.getNodeTransmitingTo().addTaskReceiving(task);
	}
	
	public void addTaskReceiving(Task task) {
		tasksReceiving.add(task);
		if (log.isDebugEnabled()) {
			log.debug("addTaskReceiving() called for " + task);
			log.debug("tasksReceiving:" + tasksReceiving);
		}
	}

	public double getComputationalCapacity() {
		return computationalCapacity;
	}
	
	/**
	 * An einai shared h CPU, tote epistrefei to pososto ths computationalCapacity
	 * pou analogei se ka8e task.
	 * @return
	 */
	public double getSharedComputationalCapacity(Task task) {
		if (sharedCpu) {
			if (tasksToCompute.size()==1 || tasksToCompute.isEmpty()) {
				return computationalCapacity;
			} else {
				return computationalCapacity * (weights.get(task.getNodeOfOrigin()).getWeight()/getSumOfUserWeightsRunningTasks());
			}
		} else {
			return computationalCapacity;
		}
	}
	
	/**
	 * Gia future calculation.
	 * @param task
	 * @param tasksToCompute
	 * @return
	 */
	public double getSharedComputationalCapacity(Task task,LinkedList<Task> tasksToCompute) {
		if (sharedCpu) {
			if (tasksToCompute.size()==1 || tasksToCompute.isEmpty()) {
				return computationalCapacity;
			} else {
				return computationalCapacity * (weights.get(task.getNodeOfOrigin()).getWeight()/getSumOfUserWeightsRunningTasks(tasksToCompute));
			}
		} else {
			return computationalCapacity;
		}
	}
	
	public double getSumOfUserWeightsRunningTasks() {
		double result = 0;
		for (Task task:tasksToCompute) {
			result += weights.get(task.getNodeOfOrigin()).getWeight();
		}
		return result;
	}
	
	/**
	 * Gia future calculation.
	 * @param tasksToCompute
	 * @return
	 */
	public double getSumOfUserWeightsRunningTasks(LinkedList<Task> tasksToCompute) {
		double result = 0;
		for (Task task:tasksToCompute) {
			result += weights.get(task.getNodeOfOrigin()).getWeight();
		}
		return result;
	}
	
	public void calculateFuture() {
		LinkedList<TimeInterval> all = getAllTimeIntervals();
		for (;;) {
			TimeInterval[] twoIntervals = getTwoInterleavingIntervals(all);
			if (twoIntervals==null) break;
			all.addAll(twoIntervals[0].add(twoIntervals[1]));
		}
		this.future = all;
		sortFuture();
	}
	
	private void sortFuture() {
		boolean swapped = true;
		int top = future.size();
		while (swapped) {
			swapped = false;
			--top;
			for (int i=0;i<top;i++) {
				TimeInterval thisInterval = future.get(i);
				TimeInterval nextInterval = future.get(i+1);
				if (thisInterval.getStartTime()>nextInterval.getStartTime()) {
					swapped = true;
					future.remove(i);
					future.remove(i);
					future.add(i,thisInterval);
					future.add(i,nextInterval);
				}
			}
		}
	}
	
	private TimeInterval[] getTwoInterleavingIntervals(LinkedList<TimeInterval> all) {
		TimeInterval[] result = null;
		TimeInterval tmp1 = null;
		TimeInterval tmp2 = null;
		boolean found = false;
		for (TimeInterval a:all) {
			for (TimeInterval b:all) {
				if (a==b) continue;
				if (a.interleaves(b)) {
					found = true;
					tmp1 = a;
					tmp2 = b;
					break;
				}
			}
			if (found) break;
		}
		if (found) {
			result = new TimeInterval[2];
			all.remove(tmp1);
			all.remove(tmp2);
			if (tmp1.getStartTime()<=tmp2.getStartTime()) {
				result[0] = tmp1;
				result[1] = tmp2;
			} else {
				result[0] = tmp2;
				result[1] = tmp1;
			}
		}
		return result;
	}
	
	/**
	 * Epistrefei mia lista apo timeintervals pou antistoixoun sta tasks pou exei o kombos pros
	 * ypologismo kai sta tasks pou tou apostelloun ta dedomena. Ta intervals einai arketa pi8ano na epikalyptontai. 
	 * @return
	 */
	private LinkedList<TimeInterval> getAllTimeIntervals() {
		LinkedList<TimeInterval> result = new LinkedList<TimeInterval>();
		
		//get already to be computed:
		if (runningTask!=null || !tasksToCompute.isEmpty()) {
			result.add(new TimeInterval(grid.getCurrentTime(),getComputingFinishTime(),NodeStatus.COMPUTING));
		}
		
		//get tasks receiving:
		for (Task task:tasksReceiving) {
			double receiveTime = grid.getCurrentTime();
			double receiveTimeOfPreviousTask = receiveTime;
			for (Task task2:task.getNodeTransmitingFrom().getListOfTasksSendingTo(this)) {
				if (task.isFinishedCalculation()) continue;//den einai pros ypologismo, epistrefei ta output data.
				double trasmitTime = 0;
				if (task.getNodeTransmitingFrom()!=this) trasmitTime = task2.getDataLeftToTrasmit()/connectedNodes.get(task2.getNodeTransmitingFrom()).getBandwidth();
				receiveTime = receiveTimeOfPreviousTask + trasmitTime;
				result.add(new TimeInterval(receiveTimeOfPreviousTask,receiveTime,NodeStatus.RECEIVING));
				result.add(new TimeInterval(receiveTime,receiveTime+task2.getInitialWorkload()/computationalCapacity,NodeStatus.COMPUTING));
				receiveTimeOfPreviousTask = receiveTime;
			}
		}
		return result;
	}
	
	public NodeLink getLinkToNode(Node node) {
		return connectedNodes.get(node);
	}
	
	public LinkedList<Task> getListOfTasksSendingTo(Node node) {
		return transmitLists.get(node);
	}
	
	private double getComputingFinishTime() {
		double result = grid.getCurrentTime();
		if (runningTask!=null) {
			result += runningTask.getWorkloadLeft()/computationalCapacity;
		}
		for (Task task:tasksToCompute) {
			result += task.getInitialWorkload()/computationalCapacity;
		}
		return result;
	}

	public double getNodeNumber() {
		return nodeNumber;
	}
	
	public StringBuffer getFutureString() {
		StringBuffer sb = new StringBuffer();
		sb.append("node ").append(nodeNumber).append(":");
		for (TimeInterval ti:future) {
			sb.append("[status:").append(ti.getStatus()).append(",start:").append(ti.getStartTime()).append(",end:").append(ti.getEndTime()).append("]");
		}
		return sb;
	}
	
	public void markDepartureTime() {
		departTime = grid.getCurrentTime();
	}
	
	public boolean cancelTask(Task task) {
		if (runningTask==task) {
			runningTask = null;
			return true;
		} else if (tasksToCompute.remove(task)) {
			return true;
		} else if (tasksReceiving.remove(task)){
			return true;
		} else {
			return false;
		}
	}
	
	public void cancelSendingTask(Task task) {
		for (Iterator<Entry<Node,LinkedList<Task>>> iter = transmitLists.entrySet().iterator();iter.hasNext();) {
			Entry<Node,LinkedList<Task>> entry = iter.next();
			if (entry.getValue().remove(task)) {
				if (entry.getValue().isEmpty()) iter.remove();
				return;
			}
		}
	}
	
	public void removeTransmitingTasks() {
		for (Entry<Node,LinkedList<Task>> list:transmitLists.entrySet()) {
			for (Task task:list.getValue()) {
				list.getKey().cancelTask(task);
				if (task.getNodeOfOrigin()==this) {
					task.markNodeOfOriginDeparted();
					removePendingTask(task);
					grid.addFailedTask(task);
				} else {
					task.reinit();
					grid.addTaskForScheduling(task);
				}
			}
		}
	}
	
	public void addWeight(Weight weight) {
		weights.put(weight.getUser(),weight);
	}
	
	public void cancelPendingTasks() {
		if (tasksPending.isEmpty()) return;
		for (Task task:tasksPending) {
			if (task.getStatus()==TaskStatus.WAITING_TO_BE_SCHEDULED) {
				continue;
			}
			if (!task.getNodeRunningOn().cancelTask(task)) {
				log.error("pending task could not be canceled! task #:"+task.getTaskNumber());
			}
		}
	}
	
	/**
	 * Get the future. Calcs the completion time by "fast forwarding".
	 * Uses cloning to ensure data integrity.
	 * @param candidate
	 * @return Map me taskNumber, finishTime
	 */
	public FinishTimes getFinishTimesOfAllTasks(Task candidate) {
		Task candidateForAssigment = null;
		if (candidate!=null) {
			try {
				candidateForAssigment = candidate.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
				return null;
			}
		}
		double virtualTime = grid.getCurrentTime();
		Task runningTask = null;
		if (this.runningTask!=null) {
			try {
				runningTask = this.runningTask.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
		FinishTimes result = new FinishTimes();
		LinkedList<Task> tasksToCompute = new LinkedList<Task>();
		HashMap<Node,LinkedList<Task>> transmitLists = new HashMap<Node,LinkedList<Task>>();
		fillWithClones(this.tasksToCompute,tasksToCompute);
		
		TreeMap<Double,LinkedList<Task>> arrivals =  getArrivalTimesOfReceivingTasks(candidateForAssigment);
		if (log.isDebugEnabled()) log.debug("arrivals:" + arrivals);
		Set<Double> orderedArrivalTimes = arrivals.keySet();//ordered because it's backed by a TreeMap.
		
		while (!(tasksToCompute.isEmpty() && arrivals.isEmpty() && runningTask==null && transmitLists.isEmpty())) {
			if (sharedCpu) {
				//diamoirazomenh CPU:
				if (tasksToCompute.isEmpty()) {
					//numberOfEpochsNotUsed++;
				} else {
					for (Iterator<Task> it = tasksToCompute.iterator();it.hasNext();) {
						Task task = it.next();
						if (task.makeComputation(tasksToCompute)) {
							//computatedTasks++;
							addTaskToTransmit(task,transmitLists);//epistrofh sto node of origin.
							//result.put(task.getTaskNumber(),virtualTime);
							it.remove();
						}
					}
				}
			} else {
				//olh h CPU se ena task.
				//compute apo to trexwn task mono:
				if (runningTask==null) {
					runningTask = tasksToCompute.poll();
					if (runningTask!=null) runningTask.markStartCalc();
				}
				if (runningTask==null) {
					//numberOfEpochsNotUsed++;
				} else {
					if (runningTask.makeComputation(tasksToCompute)) {
						//computatedTasks++;
						addTaskToTransmit(runningTask,transmitLists);//epistrofh sto node of origin.
						//result.put(runningTask.getTaskNumber(),virtualTime);
						runningTask = null;
					}
				}
			}
			
			//send output data to node of origin:
			for (Iterator<Node> iter = transmitLists.keySet().iterator();iter.hasNext();) {
				Node nodeToTransmitTo = iter.next();
				LinkedList<Task> listOfTasksToBeTransmitted = transmitLists.get(nodeToTransmitTo);
				if (nodeToTransmitTo==this) {
					for (Task task:listOfTasksToBeTransmitted) {
						//addTaskToCompute(task);
						result.put(task,virtualTime);
					}
					iter.remove();
					continue;
				}
				Task task = listOfTasksToBeTransmitted.peek();
				if (task.trasmit(connectedNodes.get(nodeToTransmitTo).getBandwidth())) {
					//nodeToTransmitTo.addTaskToCompute(task);
					result.put(task,virtualTime);
					listOfTasksToBeTransmitted.poll();
					if (listOfTasksToBeTransmitted.isEmpty()) {
						iter.remove();
					}
				}
			}
			
			for (Iterator<Double> it = orderedArrivalTimes.iterator();it.hasNext();) {
				Double arrivalTime = it.next();
				if (arrivalTime.doubleValue() > virtualTime) {
					break;
				} else {
					for (Task task:arrivals.get(arrivalTime)) {
						task.setNodeRunningOn(this);
						tasksToCompute.add(task);
					}
					it.remove();
					continue;
				}
			}
			++virtualTime;
		}
		
		return result;
	}
	
	private TreeMap<Double,LinkedList<Task>> getArrivalTimesOfReceivingTasks(Task candidateForAssignment) {
		TreeMap<Double,LinkedList<Task>> result = new TreeMap<Double,LinkedList<Task>>();
		if (candidateForAssignment!=null) {
			
			if (candidateForAssignment.getNodeOfOrigin()==this) {
				putArrivalTime(result,grid.getCurrentTime(),candidateForAssignment);
			} else {
				double bandwidth = connectedNodes.get(candidateForAssignment.getNodeOfOrigin()).getBandwidth();
				double arrivalTime = grid.getCurrentTime() + candidateForAssignment.getDataLeftToTrasmit()/bandwidth;
				putArrivalTime(result,arrivalTime,candidateForAssignment);
			}
			
			if (candidateForAssignment.getNodeOfOrigin().transmitLists.containsKey(this)) {
				//candidatesNodeOfOrigin = candidateForAssignment.getNodeOfOrigin();
			} else {
				
			}
		}
		if (log.isDebugEnabled()) {
			log.debug("tasksReceiving.size():" + tasksReceiving.size());
		}
		for (Task task:tasksReceiving) {
			double arrivalTime = grid.getCurrentTime();
			double bandwidth;
			if (!(task.getNodeTransmitingFrom()==this)) {
				bandwidth = connectedNodes.get(task.getNodeTransmitingFrom()).getBandwidth();
				arrivalTime += task.getDataLeftToTrasmit()/bandwidth;
			}
			putArrivalTime(result,arrivalTime,task);
			
			//are there more in queue for sending to this Node?
			if (task.getNodeTransmitingFrom().transmitLists.containsKey(this)) {
				LinkedList<Task> list = task.getNodeTransmitingFrom().transmitLists.get(this);
				for (Task task2:list) {
					if (task2==task) continue;
					if (task2.getNodeTransmitingFrom()==this) {
						putArrivalTime(result,arrivalTime,task2);
						continue;
					}
					bandwidth = connectedNodes.get(task2.getNodeTransmitingFrom()).getBandwidth();
					arrivalTime += task2.getDataLeftToTrasmit()/bandwidth;
					putArrivalTime(result,arrivalTime,task2);
				}
				if (candidateForAssignment!=null && task.getNodeTransmitingFrom()==candidateForAssignment.getNodeOfOrigin() && candidateForAssignment.getNodeOfOrigin()!=this) {
					bandwidth = connectedNodes.get(candidateForAssignment.getNodeOfOrigin()).getBandwidth();
					arrivalTime += candidateForAssignment.getDataLeftToTrasmit()/bandwidth;
					putArrivalTime(result,arrivalTime,candidateForAssignment);
				}
			}
		}
		return result;
	}
	
	private void putArrivalTime(TreeMap<Double,LinkedList<Task>> result,double arrivalTime,Task task) {
		if (!result.containsKey(arrivalTime)) {
			result.put(arrivalTime,new LinkedList<Task>());
		}
		try {
			result.get(arrivalTime).add(task.clone());
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return;
		}
	}
	
	private void fillWithClones(LinkedList<Task> fromList,LinkedList<Task> toList) {
		for (Task task:fromList) {
			try {
				toList.add(task.clone());
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
				continue;
			}
		}
	}

	public LinkedList<Task> getTasksReceiving() {
		return tasksReceiving;
	}
	
	/**
	 * 
	 * @return A list with the tasks that r assigned to compute on this node.
	 */
	public LinkedList<Task> getAssignedTasks() {
		LinkedList<Task> result = new LinkedList<Task>();
		if (runningTask!=null) result.add(runningTask);
		result.addAll(tasksToCompute);
		for (Task task:tasksReceiving) {
			if (!(task.isFinishedCalculation() && task.getNodeOfOrigin()==this)) {//if not returning output data, considered assigned for compuation
				result.add(task);
			}
		}
		return result;
	}
	
	public void addTaskPending(Task task) {
		tasksPending.add(task);
		requestedComputanionalCapacity += task.getInitialWorkload();
	}
	
	public void removePendingTask(Task task) {
		tasksPending.remove(task);
	}

	public boolean isToDepart() {
		return toDepart;
	}

	public LinkedList<TimeInterval> getFuture() {
		return future;
	}
	
	public double getCreationTime() {
		return creationTime;
	}
	
	public class FinishTimes {
		HashMap<Double,Double> tasksNumbers = new HashMap<Double,Double>();
		HashMap<Task,Double> tasks = new HashMap<Task,Double>();
		
		void put(Task task,double time) {
			tasks.put(task,time);
			tasksNumbers.put(task.getTaskNumber(),time);
		}
		
		public Double get(Task task) {
			return tasks.get(task);
		}
		
		public Double get(double taskNumber) {
			return tasksNumbers.get(taskNumber);
		}
		
		public String toString() {
			return tasksNumbers.toString();
		}
		
		public HashMap<Task,Double> getTasks() {
			return tasks;
		}
	}

	public double getDepartTime() {
		return departTime;
	}

	public double getNumberOfEpochsNotUsed() {
		return numberOfEpochsNotUsed;
	}

	public double getCpuhunger() {
		return cpuhunger;
	}

	public void setCpuhunger(double cpuhunger) {
		this.cpuhunger = cpuhunger;
	}

	public double getGrantedComputanionalCapacity() {
		return grantedComputanionalCapacity;
	}

	public double getRequestedComputanionalCapacity() {
		return requestedComputanionalCapacity;
	}
	
	public double getUserQos() {
		if (requestedComputanionalCapacity<=0) return -1;
		return grantedComputanionalCapacity / requestedComputanionalCapacity;
	}

	public double getUserCategory() {
		return userCategory;
	}

	public void setUserCategory(double userCategory) {
		this.userCategory = userCategory;
	}
}