package gr.upatras.gemu.task.producer;

import gr.upatras.gemu.grid.Grid;
import gr.upatras.gemu.task.Task;

import java.util.LinkedList;

/**
 * Generates few {@link Task}s for testing.
 * @author barelas
 */
public class TestTaskProducer implements TaskProducer {
	
	Grid grid;
	private double taskNumber = 0;
	
	public LinkedList<Task> generateTasks() {
		LinkedList<Task> result = null;
		switch (Math.round(Math.round(grid.getCurrentTime()))) {
		case 11111:
			if (result==null) result = new LinkedList<Task>();
			//result.add(new Task(++taskNumber,initialWorkload,inputData,outputData,desirableCompletionTime,absolutCompletionTime,grid,nodeOfOrigin));
			result.add(new Task(++taskNumber,1e6,0,0,2000,3000,grid,grid.getNodesOnline().element()));
			break;
		case 900000:
			if (result==null) result = new LinkedList<Task>();
			//result.add(new Task(++taskNumber,initialWorkload,inputData,outputData,desirableCompletionTime,absolutCompletionTime,grid,nodeOfOrigin));
			result.add(new Task(++taskNumber,500,1e5,0,grid.getCurrentTime()+2000,grid.getCurrentTime()+3000,grid,grid.getNodesOnline().element()));
			break;
		case 10:
			if (result==null) result = new LinkedList<Task>();
			//result.add(new Task(++taskNumber,initialWorkload,inputData,outputData,desirableCompletionTime,absolutCompletionTime,grid,nodeOfOrigin));
			result.add(new Task(++taskNumber,1000,300,300,grid.getCurrentTime()+2000,grid.getCurrentTime()+3000,grid,grid.getNodesOnline().getLast()));
			//result.add(new Task(++taskNumber,500,1e5,0,grid.getCurrentTime()+2000,grid.getCurrentTime()+3000,grid,grid.getNodesOnline().element()));
			break;
		default:
			break;
		}
		return result;
	}
	
	public void setGrid(Grid grid) {
		this.grid = grid;
	}
}
