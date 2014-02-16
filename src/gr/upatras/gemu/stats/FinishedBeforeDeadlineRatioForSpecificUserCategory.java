package gr.upatras.gemu.stats;

import gr.upatras.gemu.task.Task;

/**
 * @author George Barelas
 *
 */
public class FinishedBeforeDeadlineRatioForSpecificUserCategory extends FinishedBeforeDeadlineRatioStatsAggregator {
	
	static final long serialVersionUID = 1L;
	String yname = "finishedBeforeDeadlineRatio-UserCategory:";
	double categoryToStat = 1;
	
	public String getYname() {
		return yname + categoryToStat;
	}
	
	protected boolean taskTypeOk(Task task) {
		if (task.getNodeOfOrigin().getUserCategory()==categoryToStat) return true;
		else return false;
	}

	public void setCategoryToStat(double categoryToStat) {
		this.categoryToStat = categoryToStat;
	}

}
