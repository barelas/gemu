package gr.upatras.gemu.experiment;

import gr.upatras.gemu.experiment.xml.ExperimentXMLReader;
import gr.upatras.gemu.experiment.xml.ResultCombinerXMLReader;
import gr.upatras.gemu.result.CombinedResult;
import gr.upatras.gemu.result.Result;
import gr.upatras.gemu.stats.StatsAggregator;
import gr.upatras.gemu.util.MathUtil;
import gr.upatras.gemu.util.dynamicloop.DynamicDoubleLoopNode;
import gr.upatras.gemu.util.dynamicloop.DynamicFor;
import gr.upatras.gemu.util.dynamicloop.DynamicObjectLoopNode;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;

import jcckit.Graphics2DPlotCanvas;
import jcckit.data.DataPlot;
import jcckit.util.ConfigParameters;
import jcckit.util.PropertiesBasedConfigData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Main class that loads and executes experiments. It is also responsible for converting results to charts.
 * @author George Barelas
 */
public class ExperimentsRunner {
	
	LinkedList<Experiment> experiments;
	LinkedList<Result> results;
	ExperimentXMLReader experimentXMLReader;
	Properties properties;
	Properties chartProperties;
	private Log log = LogFactory.getLog(ExperimentsRunner.class);
	double maxX = Double.NEGATIVE_INFINITY;
	double maxY = Double.NEGATIVE_INFINITY;
	double minX = Double.POSITIVE_INFINITY;
	double minY = Double.POSITIVE_INFINITY;
	/**
	 * This is the number of concurrent threads the simulator is going to spawn.
	 * Setting it equal to the number of cpus, will speed up the simulation.
	 */
	static int numberOfCpusToUse = 1;
	
	private static String[] symbolFactoryClassnames = {
		"jcckit.plot.CircleSymbolFactory",
		"jcckit.plot.SquareSymbolFactory"
		};
	private static int symbolFactoryClassnameIndex = symbolFactoryClassnames.length;
	private static boolean fillSymbol = false;
	
	public ExperimentsRunner() {
		this.experimentXMLReader = new ExperimentXMLReader();
	}
	
	public static void main(String[] args) {
		ExperimentsRunner expRunner = new ExperimentsRunner();
		try {
			expRunner.init();
			if (args.length==0) {
				expRunner.loadExperiments();
				//expRunner.runAll();
				expRunner.runAllConcurrent(numberOfCpusToUse);
				expRunner.loadResults();
				expRunner.printResults();
				expRunner.convertResultsToCharts();
			} else if (args[0].equals("run")) {
				expRunner.loadExperiments();
				expRunner.runAllConcurrent(numberOfCpusToUse);
				//expRunner.runAll();
			} else if (args[0].equals("make-charts")) {
				expRunner.loadResults();
				expRunner.printResults();
				expRunner.convertResultsToCharts();
			} else if (args[0].equals("combine-results")) {
				expRunner.combineResultsToCharts();
			} else if (args[0].equals("test1")) {
				expRunner.test1();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void init() throws Exception {
		properties = new Properties();
		properties.load(new FileInputStream("conf/config.properties"));
		String resultsDirectory = properties.getProperty("results.directory");
		if (resultsDirectory==null) {
			throw new Exception("Must define results.directory in config file!");
		} else if (!(new File(resultsDirectory)).isDirectory()) {
			throw new Exception("'results.directory' is not a directory!");
		}
		numberOfCpusToUse = Integer.parseInt(properties.getProperty("numberOfCpusToUse","1"));
	}
	
	public void loadExperiments() throws Exception {
		experiments = new LinkedList<Experiment>();
		File experimentsDirectory = new File(properties.getProperty("experiments.directory","conf/experiments"));
		if (!experimentsDirectory.isDirectory()) {
			throw new Exception("experiments.directory is not a directory!");
		}
		File[] filelist = experimentsDirectory.listFiles();
		for (int i=0;i<filelist.length;i++) {
			File file = filelist[i];
			if (file.getPath().toLowerCase().endsWith(".xml")) {
				experiments.addAll(experimentXMLReader.getExperiment(file));
			}
		}
	}
	
	/**
	 * It runs all the experiments. It unlinks every experiment, when it is finished and has
	 * saved it's results, then calls the garbage collector to free up memory for the next run.
	 * If you want to rerun experiments, you have to reload them from their XML descriptions.
	 */
	public void runAll() throws Exception {
		while (!experiments.isEmpty()) {
			Experiment exp = experiments.poll();
			exp.run();
			exp.saveResults(properties.getProperty("results.directory"));
			exp = null;
			System.gc();
		}
	}
	
	/**
	 * Experimental... but quite stable.
	 * @throws Exception
	 */
	public void runAllConcurrent(int numberOfThreads) throws Exception {
		int numberOfRunningExperiments = 0;
		HashMap<Experiment,Future> futures = new HashMap<Experiment,Future>();
		ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
		while (!experiments.isEmpty() || numberOfRunningExperiments>0) {
			//fill execution queue:
			while (numberOfRunningExperiments<numberOfThreads && !experiments.isEmpty()) {
				Experiment exp = experiments.poll();
				futures.put(exp,executorService.submit(exp));
				++numberOfRunningExperiments;
				log.warn("numberOfRunningExperiments inced:"+numberOfRunningExperiments);
			}
			//give them some time to execute:
			Thread.sleep(5000);
			//check if someone has finished
			for (Iterator<Entry<Experiment,Future>> it = futures.entrySet().iterator();it.hasNext();) {
				Entry<Experiment,Future> entry = it.next();
				if (entry.getValue().isDone()) {
					entry.getKey().saveResults(properties.getProperty("results.directory"));
					it.remove();
					--numberOfRunningExperiments;
					log.warn("numberOfRunningExperiments decr:"+numberOfRunningExperiments);
					entry = null;
					System.gc();
				}
			}
		}
		executorService.shutdown();
	}
	
	public void loadResults() throws Exception {
		results = new LinkedList<Result>();
		File[] resultFilenames = (new File(properties.getProperty("results.directory"))).listFiles();
		for (int i=0;i<resultFilenames.length;i++) {
			if (log.isDebugEnabled()) {
				log.debug("filepath:"+resultFilenames[i].getAbsolutePath());
			}
			if (!resultFilenames[i].getName().toLowerCase().endsWith(".res")) continue;
			results.add(getResultFromFilename(resultFilenames[i]));
		}
	}
	
	static public Result getResultFromFilename(File file) throws Exception {
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
		return new Result((StatsAggregator)ois.readObject(),file);
	}
	
	public void printResults() {
		for (Result result:results) {
			result.info();
		}
	}
	
	public void convertResultsToCharts() throws Exception {
		initChartProperties();
		for (Result result:results) {
			makeChartFile(result);
		}
	}
	
	private void initChartProperties() throws Exception {
		String chartsDirectory = properties.getProperty("charts.directory");
		if (chartsDirectory==null) {
			throw new Exception("Must set 'charts.directory' property to a directory where u have write permissions!");
		}
		File chartsDir = new File(chartsDirectory);
		if (!chartsDir.isDirectory()) {
			throw new Exception("'charts.directory' is not a directory!");
		}
		chartProperties = new Properties();
		chartProperties.load(new FileInputStream(properties.getProperty("chart.properties.file")));
	}
	
	public void combineResultsToCharts() throws Exception {
		String combineResultFilename = "combine-results.xml";
		initChartProperties();
		ResultCombinerXMLReader resultCombinerXMLReader = new ResultCombinerXMLReader();
		String resDir = properties.getProperty("results.directory");
		File combineResultsFile = new File(resDir + File.separator + combineResultFilename);
		if (!combineResultsFile.exists()) {
			log.error("File does not exist:"+combineResultFilename);
			return;
		}
		resultCombinerXMLReader.loadXMLFile(combineResultsFile,resDir);
		//pare apo to arxeio, ena set apo lists, pou h ka8emia 8a exei ena synolo apo results pou prepei na syndyastoun.
		for (int i=0;i<resultCombinerXMLReader.getCombines();i++) {
			//gia ka8e mia apo tis listes, syndiase:
			makeChartFile(combineResults(resultCombinerXMLReader.getResultList(i),resultCombinerXMLReader.getValueNames(i),resultCombinerXMLReader.getChartFileName(i)));
		}
	}
	
	private Result combineResults(LinkedList<Result> list,HashMap<Result,HashSet<String>> valueNames,String chartFileName) {
		CombinedResult combinedResult = new CombinedResult();
		for (Result result:list) {
			combinedResult.addResult(result,valueNames.get(result));
		}
		combinedResult.setChartFileName(chartFileName);
		return combinedResult;
	}
	
	private void makeChartFile(Result result) throws Exception {
		maxX = 0;
		maxY = 0;
		int chartLineColor = 0x0000f0;
		System.getProperties().setProperty("java.awt.headless", "true");
		Properties props = new Properties(chartProperties);
		StringBuffer curves = new StringBuffer();
		StringBuffer curves2 = new StringBuffer();
		int curveNumber = 0;
		HashMap<String,LinkedList<Double>> data = result.getMapOfStatValues();
		result.setXValues(data.get(result.getXname()));
		for (Entry<String,LinkedList<Double>> entry:data.entrySet()) {
			if (!entry.getKey().equals(result.getXname())) {
				if (log.isDebugEnabled()) log.debug("chartLineColor="+chartLineColor);
				props.put("data/"+entry.getKey().replace(' ','_') + "/x",getStringFromDoubles(result.getXValues(entry.getKey()),true));
				props.put("data/"+entry.getKey().replace(' ','_') + "/y",getStringFromDoubles(entry.getValue(),false));
				props.put("data/"+entry.getKey().replace(' ','_') + "/title",entry.getKey());
				curves.append(entry.getKey().replace(' ','_')).append(" ");
				curves2.append("curve_").append(curveNumber).append(" ");
				props.put("plot/curveFactory/curve_"+curveNumber+"/","defaultCurve/");
				props.put("plot/curveFactory/curve_"+curveNumber+"/lineAttributes/lineColor",String.valueOf(chartLineColor));
				props.put("plot/curveFactory/curve_"+curveNumber+"/symbolFactory/className",getNextSymbolFactoryClassname());
				props.put("plot/curveFactory/curve_"+curveNumber+"/symbolFactory/attributes/lineColor",String.valueOf(chartLineColor));
				if (fillSymbol) props.put("plot/curveFactory/curve_"+curveNumber+"/symbolFactory/attributes/fillColor",String.valueOf(chartLineColor));
				curveNumber++;
				chartLineColor = MathUtil.rotateLeft(chartLineColor,8);
				//log.info("chartLineColor="+Integer.toString(chartLineColor,16) + "  10value="+Integer.toString(chartLineColor,10));
			}
		}
		props.put("data/curves",curves.toString());
		props.put("plot/coordinateSystem/xAxis/maximum",String.valueOf(maxX + (Math.abs(maxX-minX) * 0.025D)));
		props.put("plot/coordinateSystem/xAxis/minimum",String.valueOf(minX - (Math.abs(maxX-minX) * 0.025D)));
		props.put("plot/coordinateSystem/yAxis/maximum",String.valueOf(maxY + (Math.abs(maxY-minY) * 0.1D)));
		props.put("plot/coordinateSystem/yAxis/minimum",String.valueOf(minY - (Math.abs(maxY-minY) * 0.1D)));
		props.put("plot/coordinateSystem/xAxis/axisLabel",result.getXname());
		props.put("plot/coordinateSystem/yAxis/axisLabel",result.getYname());
		props.put("plot/curveFactory/definitions",curves2.toString());
		
		if (log.isDebugEnabled()) {
			log.debug(props);
			log.debug("data/curves:" + props.getProperty("data/curves"));
			log.debug("maxX="+maxX + " maxY="+maxY+ " minX="+minX+ " minY="+minY);
			log.debug("");
			for (Entry<String,LinkedList<Double>> entry:data.entrySet()) {
				log.debug("data/"+entry.getKey()+"/x:" + props.get("data/"+entry.getKey()+"/x"));
				log.debug("data/"+entry.getKey()+"/y:" + props.get("data/"+entry.getKey()+"/y"));
			}
		}
		
		ConfigParameters config = new ConfigParameters(new PropertiesBasedConfigData(props));
		Graphics2DPlotCanvas plotCanvas = new Graphics2DPlotCanvas(config);
		plotCanvas.connect(DataPlot.create(config));
		plotCanvas.setDoubleBuffering(false);
		BufferedImage image = new BufferedImage(1024,768,BufferedImage.TYPE_INT_RGB);
		plotCanvas.draw2DInto(image);
		ImageIO.write(image,"png",result.getChartFile(properties.getProperty("charts.directory"),".png"));
		reinitGraphicAttrs();
		reinitMaxMin();
	}
	
	private String getStringFromDoubles(LinkedList<Double> list,boolean forX) {
		double tmpMax = Double.NEGATIVE_INFINITY;
		double tmpMin = Double.POSITIVE_INFINITY;
		StringBuffer sb = new StringBuffer();
		for (Double d:list) {
			sb.append(d.doubleValue()).append(" ");
			if (d.doubleValue()>tmpMax) tmpMax = d.doubleValue();
			if (d.doubleValue()<tmpMin) tmpMin = d.doubleValue();
		}
		if (forX) {
			if (tmpMax>maxX) maxX = tmpMax;
			if (tmpMin<minX) minX = tmpMin;
		} else {
			if (tmpMax>maxY) maxY = tmpMax;
			if (tmpMin<minY) minY = tmpMin;
		}
		return sb.toString();
	}
	
	public void test1() {
		LinkedList<Object> list1 = new LinkedList<Object>();
		list1.add(1);
		list1.add(2);
		list1.add(3);
		
		
		LinkedList<Object> list2 = new LinkedList<Object>();
		list2.add("a");
		list2.add("b");
		list2.add("c");
		list2.add("d");
		
		LinkedList<Object> list3 = new LinkedList<Object>();
		list3.add(1);
		list3.add(2);
		list3.add(3);
		list3.add(4);
		
		
		//DynamicObjectLoopNode node1 = new DynamicObjectLoopNode(list1);
		DynamicObjectLoopNode node2 = new DynamicObjectLoopNode(list2);
		DynamicObjectLoopNode node3 = new DynamicObjectLoopNode(list3);
		DynamicDoubleLoopNode node4 = new DynamicDoubleLoopNode(1,3,1);
		
		node2.setNext(node4);
		//node2.setNext(node3);
		node4.setNext(node3);
		
		int counter = 0;
		
		DynamicFor dFor = new DynamicFor(node4);
		for (LinkedList<Object> tmp = dFor.next();tmp!=null;tmp = dFor.next()) {
			System.out.print(++counter + " ");
			for (Object obj:tmp) {
				System.out.print(obj.toString() + " ");
			}
			System.out.println();
		}
	}
	
	private static String getNextSymbolFactoryClassname() {
		if (symbolFactoryClassnameIndex >= symbolFactoryClassnames.length - 1) {
			symbolFactoryClassnameIndex = -1;
			fillSymbol = !fillSymbol;
		}
		return symbolFactoryClassnames[++symbolFactoryClassnameIndex];
	}
	
	private static void reinitGraphicAttrs() {
		symbolFactoryClassnameIndex = symbolFactoryClassnames.length;
		fillSymbol = false;
	}
	
	private void reinitMaxMin() {
		maxX = Double.NEGATIVE_INFINITY;
		maxY = Double.NEGATIVE_INFINITY;
		minX = Double.POSITIVE_INFINITY;
		minY = Double.POSITIVE_INFINITY;
	}
}
