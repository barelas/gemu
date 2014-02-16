package gr.upatras.gemu.scheduler;

import gr.upatras.gemu.grid.Grid;
import gr.upatras.gemu.node.Node;
import gr.upatras.gemu.task.Task;

import java.util.LinkedList;

/**
 * All schedulers must implement this interface.
 * @author George Barelas
 */
public interface Scheduler {
	
	/**
	 * Called every simulation iteration.
	 * @param tasksToBeScheduled List with new tasks to be scheduled. May be null or empty.
	 * @param availableNodes All online nodes.
	 * @param currentTime Grid current epoch.
	 */
	public void schedule(LinkedList<Task> tasksToBeScheduled,LinkedList<Node> availableNodes,double currentTime);
	
	/**
	 * Returns this Scheduler's name.
	 * @return This Scheduler's name.
	 */
	public String getSchedulerName();
	
	/**
	 * Sets the Grid object, whose tasks this Scheduler is going to schedule. Needs the Grid reference,
	 * so that the Scheduler gets neede information from the Grid (e.g. currentTime)
	 * @param grid The Grid object, whose tasks this Scheduler is going to schedule.
	 */
	public void setGrid(Grid grid);
	
	/**
	 * If the Grid desides that a Task must fail (e.g. time has exceeded its absolut time limit), this is the method to call,
	 * in order to inform the Scheduler that the task must fail. The Scheduler must delete the task from its data structures that it may be stored.
	 * @param task The task that must be failed.
	 */
	public void failTask(Task task);
}
