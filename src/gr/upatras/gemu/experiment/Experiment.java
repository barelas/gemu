package gr.upatras.gemu.experiment;

import gr.upatras.gemu.grid.Grid;
import gr.upatras.gemu.stats.ParameterStatsAggregator;
import gr.upatras.gemu.stats.Statistic;
import gr.upatras.gemu.stats.StatsAggregator;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Representes an experiment. May be parameterized or just a single run.
 * @author George Barelas
 */
public class Experiment implements Runnable {
	
	Grid runningGrid;
	String resultFilename;
	LinkedList<IterateParameter> iterateParameters;
	Node expNode;
	static Log log = LogFactory.getLog(Experiment.class);
	HashMap<String,StatsAggregator> statAggregatorResult;
	public static final String SCHEDULER_NODE_NAME = "scheduler";
	
	public Experiment() {
		this.iterateParameters = new LinkedList<IterateParameter>();
	}

	public void run() {
		try {
		switch (iterateParameters.size()) {
		case 0:
			runningGrid = getGridFromExpNode(expNode);
			runningGrid.run();
			break;
		case 1:
			//statAggregatorResult = runParameterizedExperiment(expNode);
			statAggregatorResult = runExperiments(expNode);
			break;
		default:
			break;
		}
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	private HashMap<String,StatsAggregator> runExperiments(Node expNode) throws Exception {
		HashMap<String,StatsAggregator> resultPerParameterPerMetric = new HashMap<String,StatsAggregator>();
		LinkedList<Node> expNodes = getExperimentNodesFromExpNode(expNode);
		LinkedList<HashMap<String,StatsAggregator>> expResultsPerScheduler = new LinkedList<HashMap<String,StatsAggregator>>();
		for (Node n:expNodes) {
			expResultsPerScheduler.add(runParameterizedExperiment(n));
		}
		
		//gia ka8e metric sxhmatise to statResult:
		for (String metric:expResultsPerScheduler.peek().keySet()) {
			ParameterStatsAggregator allResults = new ParameterStatsAggregator(iterateParameters.peek().getName(),metric,null);
			for (Double xvalue:expResultsPerScheduler.peek().get(metric).getStats().keySet()) {
				Statistic statistic = new Statistic(xvalue.doubleValue());
				for (HashMap<String,StatsAggregator> expResultMap:expResultsPerScheduler) {
					ParameterStatsAggregator expResult = (ParameterStatsAggregator) expResultMap.get(metric);
					statistic.setValue(expResult.getSchedulerName(),expResult.getStats().get(xvalue).getValue(metric));
				}
				allResults.addStatistic(statistic,xvalue);
			}
			resultPerParameterPerMetric.put(metric,allResults);
		}
		
		return resultPerParameterPerMetric;
	}
	
	/**
	 * Epistrefei ta apotelesmata gia ka8e metrikh.
	 * @param expNode
	 * @return
	 * @throws Exception
	 */
	private HashMap<String,StatsAggregator> runParameterizedExperiment(Node expNode) throws Exception {
		HashMap<String,LinkedHashMap<Double,StatsAggregator>> allMetrics = new HashMap<String,LinkedHashMap<Double,StatsAggregator>>();
		String schedulerName = null;
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();
		IterateParameter ip = iterateParameters.peek();
		for (double parameterValue = ip.getStartValue();parameterValue<=ip.getStopValue();parameterValue+=ip.getStep()) {
			XPathExpression xpathExpression = xpath.compile(ip.getXpathExpr());
			NodeList iterateOnNodes = (NodeList) xpathExpression.evaluate(expNode,XPathConstants.NODESET);
			Node attrNode = iterateOnNodes.item(0);
			attrNode.setNodeValue(String.valueOf(parameterValue));
			Grid grid = getGridFromExpNode(expNode);
			grid.run();
			schedulerName = grid.getSchedulerName();
			addGridResults2AllMetrics(allMetrics,grid.getStatsAggregators(),parameterValue);
			//free up some memory:
			grid = null;
			System.gc();
		}
		return sumResultsPerParameterValue(allMetrics,schedulerName,ip);
	}
	
	/**
	 * 
	 * @param allMetrics
	 * @param schedulerName
	 * @return
	 */
	private HashMap<String,StatsAggregator> sumResultsPerParameterValue(
			HashMap<String,LinkedHashMap<Double,StatsAggregator>> allMetrics,
			String schedulerName,
			IterateParameter ip) {
		HashMap<String,StatsAggregator> result = new HashMap<String,StatsAggregator>();
		for (Entry<String,LinkedHashMap<Double,StatsAggregator>> entry:allMetrics.entrySet()) {
			result.put(entry.getKey(),getStatsAggregatorFromSimulationResults(entry.getValue(),ip,schedulerName));
		}
		return result;
	}
	
	/**
	 * 
	 * @param allMetrics
	 * @param thisGridResult
	 */
	private void addGridResults2AllMetrics(
			HashMap<String,LinkedHashMap<Double,StatsAggregator>> allMetrics,
			LinkedHashMap<String,StatsAggregator> thisGridResult,
			double parameterValue) {
		for (Entry<String,StatsAggregator> entry:thisGridResult.entrySet()) {
			if (!allMetrics.containsKey(entry.getKey())) {
				allMetrics.put(entry.getKey(),new LinkedHashMap<Double,StatsAggregator>());
			}
			allMetrics.get(entry.getKey()).put(parameterValue,entry.getValue());
		}
	}
	
	private Set<String> getParameterNames(LinkedHashMap<Double,StatsAggregator> statsResults) {
		Iterator<StatsAggregator> it = statsResults.values().iterator();
		return it.next().getMapOfStatValues().keySet();
	}
	
	public StatsAggregator getStatsAggregatorFromSimulationResults(LinkedHashMap<Double,StatsAggregator> statsResults,IterateParameter iterateParameter,String schedulerName) {
		ParameterStatsAggregator result = new ParameterStatsAggregator(iterateParameter.getName(),statsResults.entrySet().iterator().next().getValue().getYname(),schedulerName);
		Set<String> parameters = getParameterNames(statsResults);
		parameters.remove("time");
		for (Entry<Double,StatsAggregator> entry:statsResults.entrySet()) {
			Statistic statistic = new Statistic(entry.getKey());
			HashMap<String,LinkedList<Double>> statValuesMap = entry.getValue().getMapOfStatValues();
			for (String parameter:parameters) {
				if (log.isDebugEnabled()) {
					log.debug("parameter="+parameter);
					log.debug("statValuesMap="+statValuesMap);
					log.debug("statValuesMap.get(parameter)="+statValuesMap.get(parameter));
				}
				double mean = 0;
				double n = 0;
				for (Double d:statValuesMap.get(parameter)) {
					mean = (n++*mean+d)/n;
					if (log.isDebugEnabled()) log.debug("mean="+mean);
				}
				statistic.setValue(parameter,mean);
			}
			result.addStatistic(statistic,entry.getKey());
		}
		
		return result;
	}
	
	static synchronized public Grid getGridFromExpNode(Node expNode) throws Exception {
		Grid grid = new Grid();
		NodeList nodes = expNode.getChildNodes();
		for (int i=0;i<nodes.getLength();i++) {
			if (!(nodes.item(i).getNodeType()==Node.ELEMENT_NODE)) continue;
			if (nodes.item(i).getNodeName().equals("attr")) {
				NamedNodeMap attrs = nodes.item(i).getAttributes();
				if (log.isDebugEnabled()) {
					log.debug(attrs.getNamedItem("name").getNodeValue()+" => "+attrs.getNamedItem("value").getNodeValue());
				}
				BeanUtils.setProperty(grid,attrs.getNamedItem("name").getNodeValue(),attrs.getNamedItem("value").getNodeValue());
			} else if (nodes.item(i).getNodeName().equals("statsAggregator")) {
				grid.addStatsAggregator((StatsAggregator)getObjectFromNode(nodes.item(i)));
			} else {
				BeanUtils.setProperty(grid,nodes.item(i).getNodeName(),getObjectFromNode(nodes.item(i)));
			}
		}
		return grid;
	}
	
	static synchronized public LinkedList<Node> getExperimentNodesFromExpNode(Node expNode) throws Exception {
		LinkedList<Node> grids = new LinkedList<Node>();
		LinkedList<Node> schedulerNodes = new LinkedList<Node>();
		NodeList nodes = expNode.getChildNodes();
		
		//get all the scheduler nodes:
		for (int i=0;i<nodes.getLength();i++) {
			Node node = nodes.item(i);
			if (!(node.getNodeType()==Node.ELEMENT_NODE)) continue;
			if (node.getNodeName().equals(SCHEDULER_NODE_NAME)) {
				schedulerNodes.add(node);
				expNode.removeChild(node);
			}
		}
		
		//set one-by-one the scheduler nodes and clone it each time:
		for (Node schedulerNode:schedulerNodes) {
			expNode.appendChild(schedulerNode);
			grids.add(expNode.cloneNode(true));
			expNode.removeChild(schedulerNode);
		}
		
		//put back the scheduler nodes:
		for (Node schedulerNode:schedulerNodes) {
			expNode.appendChild(schedulerNode);
		}
		return grids;
	}
	
	static private Object getObjectFromNode(Node node) throws Exception {
		if (log.isDebugEnabled()) {
			log.debug(node.getNodeType()+" : "+node.getNodeName()+"  =  "+node.getNodeValue());
		}
		String className = node.getAttributes().getNamedItem("class").getNodeValue();
		Object object = Class.forName(className).newInstance();
		NodeList children = node.getChildNodes();
		for (int i=0;i<children.getLength();i++) {
			Node child = children.item(i);
			if (!(child.getNodeType()==Node.ELEMENT_NODE)) continue;
			if (child.getNodeName().equals("attr")) {
				NamedNodeMap attrs = child.getAttributes();
				BeanUtils.setProperty(object,attrs.getNamedItem("name").getNodeValue(),attrs.getNamedItem("value").getNodeValue());
			} else {
				BeanUtils.setProperty(object,child.getNodeName(),getObjectFromNode(child));
			}
		}
		return object;
	}
	
	public void saveResults(String directory) {
		switch (iterateParameters.size()) {
		case 0:
			runningGrid.saveAggregationsToFiles(directory + File.separator + resultFilename);
			break;
		case 1:
			for (Entry<String,StatsAggregator> entry:statAggregatorResult.entrySet()) {
				entry.getValue().saveToFile(directory + File.separator + resultFilename + "." + entry.getKey() + Grid.RESULT_FILE_EXTENSION);
			}
			break;
		default:
			break;
		}
	}
	
	public void addIterateParameter(IterateParameter ip) {
		iterateParameters.add(ip);
	}

	public String getResultFilename() {
		return resultFilename;
	}

	public void setResultFilename(String resultFilename) {
		this.resultFilename = resultFilename;
	}

	public void setExpNode(Node expNode) {
		this.expNode = expNode;
	}
}
