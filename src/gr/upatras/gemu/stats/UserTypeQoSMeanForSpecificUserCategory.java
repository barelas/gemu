package gr.upatras.gemu.stats;

import gr.upatras.gemu.node.Node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author George Barelas
 *
 */
public class UserTypeQoSMeanForSpecificUserCategory extends UserQoSMeanStatsAggregator {
	
	static final long serialVersionUID = 1L;
	static transient Log log = LogFactory.getLog(UserTypeQoSMeanForSpecificUserCategory.class);
	double userCategoryToStat = 1;
	protected String yname = "userQoS-mean-userOfType:";
	
	public String getYname() {
		return yname + userCategoryToStat;
	}
	
	protected boolean getStatForNodeAllowed(Node node) {
//		if (node.getUserCategory()==1) {
//			log.info("got one!!!");
//		}
		if (node.getUserCategory()==userCategoryToStat) return true;
		else return false;
	}

	public void setUserCategoryToStat(double userCategoryToStat) {
		this.userCategoryToStat = userCategoryToStat;
	}
}
