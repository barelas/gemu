package gr.upatras.gemu.stats;

import gr.upatras.gemu.task.Task;

/**
 * @author George Barelas
 *
 */
public class FinishedBeforeDeadlineRatioForSpecificTaskAndUserCategory extends FinishedBeforeDeadlineRatioStatsAggregator {
	static final long serialVersionUID = 1L;
	String yname = "finishedBeforeDeadlineRatio-TaskCategory:";
	double taskCategoryToStat = 1;
	double userCategoryToStat = 1;
	
	public String getYname() {
		return yname + taskCategoryToStat + "userCategory:" + userCategoryToStat;
	}
	
	protected boolean taskTypeOk(Task task) {
		if (task.getTaskCategory()==taskCategoryToStat && task.getNodeOfOrigin().getUserCategory()==userCategoryToStat) return true;
		else return false;
	}

	public void setTaskCategoryToStat(double taskCategoryToStat) {
		this.taskCategoryToStat = taskCategoryToStat;
	}

	public void setUserCategoryToStat(double userCategoryToStat) {
		this.userCategoryToStat = userCategoryToStat;
	}
}
