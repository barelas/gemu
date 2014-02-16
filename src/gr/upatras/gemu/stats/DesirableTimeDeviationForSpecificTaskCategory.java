package gr.upatras.gemu.stats;

import gr.upatras.gemu.task.Task;

/**
 * @author George Barelas
 *
 */
public class DesirableTimeDeviationForSpecificTaskCategory extends DesirableTimeDeviationStatsAggregator {
	static final long serialVersionUID = 1L;
	double categoryToStat = 1;
	String yname = "deviationFromDesirableTime-TaskCategory:";
	
	public String getYname() {
		return yname + categoryToStat;
	}
	protected boolean taskTypeOk(Task task) {
		if (task.getTaskCategory()==categoryToStat) return true;
		else return false;
	}
	public void setCategoryToStat(double categoryToStat) {
		this.categoryToStat = categoryToStat;
	}
}
