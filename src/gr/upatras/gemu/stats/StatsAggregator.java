package gr.upatras.gemu.stats;

import gr.upatras.gemu.grid.Grid;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * @author barelas
 *
 */
public interface StatsAggregator {
	
	/**
	 * Mazeyei statistika gia to grid thn stigmh pou kaleitai, dhmiourgei kai apo8hkeyei
	 * ena Statistic object. Kaleitai se ka8e epoch kai etsi mporei na mhn 8elei na sylle3ei stats.
	 */
	public void aggregate();
	
	/**
	 * Aggregate stats ane3arthta apo ton xrono.
	 */
	public void forcedAggregation();
	
	public void saveToFile(String filename);
	
	public void setGrid(Grid grid);
	
	public Map<Double,Statistic> getStats();
	
	public HashMap<String,LinkedList<Double>> getMapOfStatValues();
	
	public String getXname();
	
	public String getYname();
}
