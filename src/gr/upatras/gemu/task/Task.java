package gr.upatras.gemu.task;

import gr.upatras.gemu.grid.Grid;
import gr.upatras.gemu.node.Node;
import gr.upatras.gemu.task.TaskHistory.EventType;

import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents a task (or job) that must be executed.
 * @author George Barelas
 */
public class Task implements Cloneable {
	
	double taskNumber = 0;
	double initialWorkload = 0;
	double workloadLeft = 0;
	double inputData = 0;
	double outputData = 0;
	double dataLeftToTrasmit = 0;
	double desirableCompletionTime = 0;
	double absolutCompletionTime = 0;
	double creationTime = 0;
	double completionTime = 0;
	Node nodeOfOrigin;
	Node nodeRunningOn;
	Node nodeTransmitingFrom;
	Node nodeTransmitingTo;
	LinkedList<TaskHistory> history;
	Grid grid;
	boolean readyToTransmit = false;
	static Log log = LogFactory.getLog(Task.class);
	TaskStatus status = TaskStatus.WAITING_TO_BE_SCHEDULED;
	double taskCategory = 1;
	
	public enum TaskStatus {WAITING_TO_BE_SCHEDULED,SCHEDULED,RUNNING,DONE_CALC,DONE,FAILED}
	
	public Task(double taskNumber, double initialWorkload, double inputData, double outputData, double desirableCompletionTime, double absolutCompletionTime,Grid grid, Node nodeOfOrigin) {
		this.taskNumber = taskNumber;
		this.initialWorkload = initialWorkload;
		this.workloadLeft = initialWorkload;
		this.inputData = inputData;
		this.outputData = outputData;
		this.dataLeftToTrasmit = inputData;
		this.desirableCompletionTime = desirableCompletionTime;
		this.absolutCompletionTime = absolutCompletionTime;
		this.creationTime = grid.getCurrentTime();
		this.nodeOfOrigin = nodeOfOrigin;
		this.nodeTransmitingFrom = nodeOfOrigin;
		this.nodeRunningOn = null;
		this.nodeTransmitingTo = null;
		this.history = new LinkedList<TaskHistory>();
		this.grid = grid;
		this.history.add(new TaskHistory(creationTime,EventType.CREATION,nodeOfOrigin));
		nodeOfOrigin.addTaskPending(this);
	}

	/**
	 * 
	 * @return true an teleiwse to task.
	 */
	public boolean makeComputation() {
		if ((workloadLeft-=nodeRunningOn.getSharedComputationalCapacity(this))<=0) {
			status = TaskStatus.DONE_CALC;
			history.add(new TaskHistory(grid.getCurrentTime(),EventType.END_CALC,nodeRunningOn));
			nodeTransmitingFrom = nodeRunningOn;
			nodeTransmitingTo = nodeOfOrigin;
			dataLeftToTrasmit = outputData;
			readyToTransmit = true;
			return true;
		} else {
			return false;
		}
	}
	
	public boolean makeComputation(LinkedList<Task> tasksToCompute) {
		if ((workloadLeft-=nodeRunningOn.getSharedComputationalCapacity(this,tasksToCompute))<=0) {
			status = TaskStatus.DONE_CALC;
			history.add(new TaskHistory(grid.getCurrentTime(),EventType.END_CALC,nodeRunningOn));
			nodeTransmitingFrom = nodeRunningOn;
			nodeTransmitingTo = nodeOfOrigin;
			dataLeftToTrasmit = outputData;
			readyToTransmit = true;
			return true;
		} else {
			return false;
		}
	}

	public Node getNodeRunningOn() {
		return nodeRunningOn;
	}

	public void setNodeRunningOn(Node nodeRunningOn) {
		this.nodeRunningOn = nodeRunningOn;
	}
	
	/**
	 * O nodeOfOrigin 8ewreitai kai o xrhsths pou ypeballe to task.
	 * @return
	 */
	public Node getNodeOfOrigin() {
		return nodeOfOrigin;
	}

	/**
	 * 
	 * @param bandwidth
	 * @return true an ola ta data einai transmitted.
	 */
	public boolean trasmit(double bandwidth) {
		if (readyToTransmit) {
			readyToTransmit = false;
			history.add(new TaskHistory(grid.getCurrentTime(),EventType.START_TRANSMIT,nodeTransmitingFrom,nodeTransmitingTo));
		}
		if ((dataLeftToTrasmit-=bandwidth)<=0) {
			history.add(new TaskHistory(grid.getCurrentTime(),EventType.END_TRANSMIT,nodeTransmitingFrom,nodeTransmitingTo));
			return true;
		} else {
			return false;
		}
	}
	
	public double getInitialWorkload() {
		return initialWorkload;
	}

	public double getWorkloadLeft() {
		return workloadLeft;
	}

	public double getTaskNumber() {
		return taskNumber;
	}
	
	public double getDataLeftToTrasmit() {
		return dataLeftToTrasmit;
	}

	public Node getNodeTransmitingTo() {
		return nodeTransmitingTo;
	}

	public Node getNodeTransmitingFrom() {
		return nodeTransmitingFrom;
	}

	public void setNodeTransmitingFrom(Node nodeTransmitingFrom) {
		this.nodeTransmitingFrom = nodeTransmitingFrom;
	}

	public void setNodeTransmitingTo(Node nodeTransmitingTo) {
		this.nodeTransmitingTo = nodeTransmitingTo;
		readyToTransmit = true;
		history.add(new TaskHistory(grid.getCurrentTime(),EventType.SCHEDULED,nodeTransmitingTo));
	}
	
	public void markStartTransmit() {
		history.add(new TaskHistory(grid.getCurrentTime(),EventType.START_TRANSMIT,nodeTransmitingFrom,nodeTransmitingTo));
	}
	
	public void markStartCalc() {
		history.add(new TaskHistory(grid.getCurrentTime(),EventType.START_CALC,nodeRunningOn));
	}
	
	public StringBuffer getHistory(String message) {
		StringBuffer sb = new StringBuffer();
		sb.append("task ").append(taskNumber).append(' ').append(message).append(":");
		for (TaskHistory th:history) {
			sb.append("[event:").append(th.getType()).append(",time:").append(th.getTime());
			if (th.getNode1()!=null) {
				sb.append(",node1:").append(th.getNode1().getNodeNumber()).append(",comp-cap1:").append(th.getNode1().getComputationalCapacity());
			}
			if (th.getNode2()!=null) {
				sb.append(",node2:").append(th.getNode2().getNodeNumber()).append(",comp-cap2:").append(th.getNode2().getComputationalCapacity());
			}
			sb.append("]");
		}
		return sb;
	}

	public boolean isFinishedCalculation() {
		return status==TaskStatus.DONE_CALC ? true : false;
	}
	
	public void markNodeOfOriginDeparted() {
		history.add(new TaskHistory(grid.getCurrentTime(),EventType.ORIGIN_NODE_DEPARTED,nodeOfOrigin));
	}
	
	/**
	 * Epanarxikopoiei to task. Kaleitai se periptwsh pou o kombos,
	 * ston opoio exei anate8ei to task pros ypologismo, anaxwrhsei.
	 */
	public void reinit() {
		history.add(new TaskHistory(grid.getCurrentTime(),EventType.COMP_NODE_DEPARTED,nodeRunningOn));
		this.dataLeftToTrasmit = inputData;
		this.nodeTransmitingFrom = nodeOfOrigin;
		this.nodeRunningOn = null;
		this.nodeTransmitingTo = null;
		this.status = TaskStatus.WAITING_TO_BE_SCHEDULED;
	}
	
	public Task clone() throws CloneNotSupportedException {
		Task clone = (Task) super.clone();
		clone.setNewHistory();
		return clone;
	}
	
	private void setNewHistory() {
		this.history = new LinkedList<TaskHistory>();
	}
	
	public String toString() {
		return "task#" + taskNumber;
	}

	public double getCompletionTime() {
		return completionTime;
	}

	public void setCompletionTime(double completionTime) {
		this.completionTime = completionTime;
	}

	public double getAbsolutCompletionTime() {
		return absolutCompletionTime;
	}

	public double getDesirableCompletionTime() {
		return desirableCompletionTime;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public void setStatus(TaskStatus status) {
		this.status = status;
	}

	public double getCreationTime() {
		return creationTime;
	}

	public double getTaskCategory() {
		return taskCategory;
	}

	public void setTaskCategory(double taskCategory) {
		this.taskCategory = taskCategory;
	}
}
