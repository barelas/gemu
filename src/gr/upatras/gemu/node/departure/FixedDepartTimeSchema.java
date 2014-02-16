package gr.upatras.gemu.node.departure;

/**
 * Nodes depart in a fixed epoch count.
 * @author George Barelas
 */
public class FixedDepartTimeSchema implements DepartureSchema {
	
	double countdown;
	
	public FixedDepartTimeSchema() {
		
	}
	
	public FixedDepartTimeSchema(double countdown) {
		this.countdown = countdown;
	}

	public boolean nextEpoch() {
		if (--countdown<=0) return false;
		else return true;
	}

	public void setCountdown(double countdown) {
		this.countdown = countdown;
	}
	
	public DepartureSchema getNewInstance() {
		return new FixedDepartTimeSchema(countdown);
	}
}
