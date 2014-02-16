package gr.upatras.gemu.result;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents a combined Result from different Result files.
 * @author George Barelas
 */
public class CombinedResult extends Result {
	
	LinkedList<Result> results = new LinkedList<Result>();
	static Log log = LogFactory.getLog(CombinedResult.class);
	private String xname = null;
	private String yname = null;
	HashMap<String,LinkedList<Double>> yValues = new HashMap<String,LinkedList<Double>>();
	HashMap<String,LinkedList<Double>> xValues = new HashMap<String,LinkedList<Double>>();
	private String chartFileName = null;
	boolean addingDifferentMetrics = false;
	HashMap<String,String> fullYNames = new HashMap<String,String>();
	
	public void addResult(Result result,HashSet<String> valueNames) {
		if (xname==null) {
			xname = result.getXname();
			yname = result.getYname();
		} else if (!result.getYname().equals(yname)) {
			if (!addingDifferentMetrics) {
				log.warn("Creating a Result from different Y metrics!");
				fixYNames();
				addingDifferentMetrics = true;
				yname = "y axis";
			}
		}
		if (!result.getXname().equals(xname)) {
			log.error("Creating a Result from different X metrics not allowed.");
			return;
		}
		addAll(result,valueNames);
		results.add(result);
	}
	
	private void fixYNames() {
		for (String yname:fullYNames.keySet()) {
			yValues.put(fullYNames.get(yname),yValues.get(yname));
			yValues.remove(yname);
			xValues.put(fullYNames.get(yname),xValues.get(yname));
			xValues.remove(yname);
		}
		fullYNames = null;
	}
	
	private void addAll(Result result,HashSet<String> valueNames) {
		HashMap<String,LinkedList<Double>> map = result.getMapOfStatValues();
		LinkedList<Double> xValuesForTheseYValues = map.get(xname);
		boolean addAll = false;
		if (valueNames.contains("all")) addAll = true;
		for (Entry<String,LinkedList<Double>> entry:map.entrySet()) {
			if (entry.getKey().equals(xname)) continue;
			if ( !(addAll || valueNames.contains(entry.getKey())) ) continue;
			String yname = entry.getKey();
			String fullname = yname + " - " + result.getYname();
			if (addingDifferentMetrics) {
				yname = fullname;
			} else {
				fullYNames.put(yname,fullname);
			}
			yValues.put(yname,entry.getValue());
			xValues.put(yname,xValuesForTheseYValues);
		}
	}
	
	/**
	 * just for testing...
	 *
	 */
	private LinkedList<Double> tamperValues(LinkedList<Double> l) {
		LinkedList<Double> res = new LinkedList<Double>();
		for (Double d:l) {
			res.add(d+1.5);
		}
		return res;
	}
	
	public HashMap<String,LinkedList<Double>> getMapOfStatValues() {
		return yValues;
	}
	
	
	public File getChartFile(String chartDir,String extension) {
		
		return new File(chartDir + File.separator + chartFileName + extension);
	}
	
	public void setXValues(String values) {
		
	}
	
	public LinkedList<Double> getXValues(String yName) {
		return xValues.get(yName);
	}
	
	public String getXname() {
		return xname;
	}

	public String getYname() {
		return yname;
	}

	public String getChartFileName() {
		return chartFileName;
	}

	public void setChartFileName(String chartFileName) {
		this.chartFileName = chartFileName;
	}
}
