package gr.upatras.gemu.stats;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Statistic implements Serializable {
	
	static final long serialVersionUID = 1L;
	double time;
	HashMap<String,Double> values;
	static Log log = LogFactory.getLog(Statistic.class);

	public Statistic(double time) {
		this.time = time;
		this.values = new HashMap<String,Double>();
	}
	
	public void setValue(String name,double value) {
		values.put(name,value);
		if (log.isDebugEnabled()) log.debug("values:"+values);
	}
	
	public double getValue(String valueName) {
		if (log.isDebugEnabled()) log.debug("values:"+values);
		return values.get(valueName);
	}

	public double getTime() {
		return time;
	}

	public HashMap<String,Double> getValues() {
		return values;
	}
}
