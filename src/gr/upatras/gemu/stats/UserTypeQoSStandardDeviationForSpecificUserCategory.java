package gr.upatras.gemu.stats;

import gr.upatras.gemu.node.Node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author George Barelas
 *
 */
public class UserTypeQoSStandardDeviationForSpecificUserCategory extends UserQoSStandardDeviationStatsAggregator {

	static final long serialVersionUID = 1L;
	static transient Log log = LogFactory.getLog(UserTypeQoSStandardDeviationForSpecificUserCategory.class);
	double userCategoryToStat = 1;
	protected String yname = "userQoS-standardDeviation-userOfType:";
	
	public String getYname() {
		return yname + userCategoryToStat;
	}
	
	protected boolean getStatForNodeAllowed(Node node) {
		if (node.getUserCategory()==userCategoryToStat) return true;
		else return false;
	}

	public void setUserCategoryToStat(double userCategoryToStat) {
		this.userCategoryToStat = userCategoryToStat;
	}
}
