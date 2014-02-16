package gr.upatras.gemu.task.producer;

import gr.upatras.gemu.grid.Grid;
import gr.upatras.gemu.task.Task;

import java.util.LinkedList;

/**
 * All task generators must implement this interface.
 * @author George Barelas
 */
public interface TaskProducer {
	
	/**
	 * Kaleitai se ka8e epoch. Epistrefei mia lista me ta nea tasks. Mporei na epistrejei null h'
	 * adeia lista.
	 * @return
	 */
	public LinkedList<Task> generateTasks();
	
	public void setGrid(Grid grid);
}
