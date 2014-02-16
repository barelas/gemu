package gr.upatras.gemu.result;

import gr.upatras.gemu.stats.StatsAggregator;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents a simulation result.
 * @author George Barelas
 */
public class Result {
	
	StatsAggregator statsAggregator;
	File resultFile;
	static Log log = LogFactory.getLog(Result.class);
	LinkedList<Double> xValues = null;
	
	public Result() {
		
	}
	
	public Result(StatsAggregator statsAggregator, File resultFile) {
		this.statsAggregator = statsAggregator;
		this.resultFile = resultFile;
	}
	
	public File getChartFile(String chartDir,String extension) {
		return new File(chartDir + File.separator + resultFile.getName().substring(0,resultFile.getName().length()-4) + extension);
	}
	
	public void info() {
		log.info("filename:"+resultFile.getName());
		log.info("# of stat points:"+statsAggregator.getStats().size());
	}
	
	public HashMap<String,LinkedList<Double>> getMapOfStatValues() {
		return statsAggregator.getMapOfStatValues();
	}
	
	public String getXname() {
		return statsAggregator.getXname();
	}
	
	public String getYname() {
		return statsAggregator.getYname();
	}
	
	/**
	 * 
	 * @param yName Den paizei rolo edw. Paizei mono sthn subclass {@link CombinedResult},
	 * h opoia 8a override thn me8odo auth kai 8a epistrefei to swsto xValues ana yName. 
	 * @return
	 */
	public LinkedList<Double> getXValues(String yName) {
		return xValues;
	}

	public void setXValues(LinkedList<Double> values) {
		xValues = values;
	}
}
