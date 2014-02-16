package gr.upatras.gemu.task.producer;

import gr.upatras.gemu.grid.Grid;
import gr.upatras.gemu.node.Node;
import gr.upatras.gemu.task.Task;
import gr.upatras.gemu.util.MathUtil;
import gr.upatras.gemu.util.StatisticalCharacteristics;

import java.util.LinkedList;
import java.util.Random;

/**
 * Generates {@link Task}s using a Gaussian distibution for their attributes and arrivals.
 * @author George Barelas
 */
public class NormalTaskProducer implements TaskProducer {
	
	double countdown = 0;
	StatisticalCharacteristics statChars;
	private double taskNumber = 0;
	Random random;
	Grid grid;
	
	public NormalTaskProducer() {
		this.random = new Random();
	}
	
	public NormalTaskProducer(StatisticalCharacteristics statChars,Grid grid) {
		this.statChars = statChars;
		this.random = new Random();
		this.grid = grid;
	}

	public LinkedList<Task> generateTasks() {
		//don't generate new tasks, if there are no noline nodes:
		if (grid.getNodesOnline().isEmpty()) return null;
		if (--countdown<=0) {
			getNewCountdown();
			if (grid.getNodesOnline().isEmpty()) return null;
			LinkedList<Task> newTasks = new LinkedList<Task>();
			Task task = getNewTask();
			newTasks.add(task);
			return newTasks;
		}
		return null;
	}
	
	private void getNewCountdown() {
		countdown = MathUtil.getNextNumber(statChars.getEpochMean(),statChars.getEpochDeviation(),random.nextGaussian(),1D);
	}
	
	private Task getNewTask() {
		double initialWorkload = MathUtil.getNextNumber(statChars.getInitialWorkloadMean(),statChars.getInitialWorkloadDeviation(),random.nextGaussian(),1D);
		double inputData = MathUtil.getNextNumber(statChars.getInputDataMean(),statChars.getInputDataDeviation(),random.nextGaussian(),1D);
		double outputData = MathUtil.getNextNumber(statChars.getOutputDataMean(),statChars.getOutputDataDeviation(),random.nextGaussian(),1D);
		double desirableCompletionTime = MathUtil.getNextNumber(statChars.getDesirableCompletionTimeMean()+grid.getCurrentTime(),statChars.getDesirableCompletionTimeDeviation(),random.nextGaussian(),1D+grid.getCurrentTime());
		double absolutCompletionTime = MathUtil.getNextNumber(statChars.getAbsolutCompletionTimeMean()+grid.getCurrentTime(),statChars.getAbsolutCompletionTimeDeviation(),random.nextGaussian(),desirableCompletionTime);
		Node nodeOfOrigin = selectRandomNode();
		return new Task(++taskNumber,initialWorkload,inputData,outputData,desirableCompletionTime,absolutCompletionTime,grid,nodeOfOrigin);
	}
	
	private Node selectRandomNode() {
		return grid.getNodesOnline().get(random.nextInt(grid.getNodesOnline().size()));
	}

	public void setStatChars(StatisticalCharacteristics statChars) {
		this.statChars = statChars;
	}

	public void setGrid(Grid grid) {
		this.grid = grid;
	}
}
