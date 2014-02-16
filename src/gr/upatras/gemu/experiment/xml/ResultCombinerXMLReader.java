package gr.upatras.gemu.experiment.xml;

import gr.upatras.gemu.experiment.ExperimentsRunner;
import gr.upatras.gemu.result.Result;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class combines Result files to create new charts.
 * @author George Barelas
 */
public class ResultCombinerXMLReader {
	
	private HashMap<Integer,LinkedList<Result>> set;
	private HashMap<Integer,HashMap<Result,HashSet<String>>> yNames;
	private HashMap<Integer,String> chartFileName;
	private int combines;
	
	public int getCombines() {
		return combines;
	}
	
	public LinkedList<Result> getResultList(int i) {
		return set.get(i);
	}
	
	public HashMap<Result,HashSet<String>> getValueNames(int i) {
		return yNames.get(i);
	}
	
	public String getChartFileName(int i) {
		return chartFileName.get(i);
	}

	public void loadXMLFile(File file,String resultsDirectory) throws Exception {
		set = new HashMap<Integer,LinkedList<Result>>();
		yNames = new HashMap<Integer,HashMap<Result,HashSet<String>>>();
		chartFileName = new HashMap<Integer,String>();
		Document document = ExperimentXMLReader.getDocumentFromXMLFile(file);
		if (!document.hasChildNodes()) {
			throw new Exception("no child nodes found!");
		}
		NodeList combineTags = document.getElementsByTagName("combine-results");
		combines = combineTags.getLength();
		for (int i=0;i<combineTags.getLength();i++) {
			Element combineResultsElement = (Element) combineTags.item(i);
			chartFileName.put(i,combineResultsElement.getAttribute("chartfilename"));
			NodeList resultFileTags = combineResultsElement.getChildNodes();
			LinkedList<Result> list = new LinkedList<Result>();
			HashMap<Result,HashSet<String>> charts = new HashMap<Result,HashSet<String>>();
			for (int j=0;j<resultFileTags.getLength();j++) {
				Node node = resultFileTags.item(j);
				if (node.getNodeType()==Node.ELEMENT_NODE && node.getNodeName().equals("result-file")) {
					Element resultFileElement = (Element) node;
					Result result = ExperimentsRunner.getResultFromFilename(new File(resultsDirectory + File.separator + resultFileElement.getAttribute("filename")));
					list.add(result);
					HashSet<String> ynameSetForThisResult = new HashSet<String>();
					NodeList valueToUseElements = resultFileElement.getElementsByTagName("value-to-use");
					if (valueToUseElements==null || valueToUseElements.getLength()==0) {
						ynameSetForThisResult.add("all");
					} else {
						for (int k=0;k<valueToUseElements.getLength();k++) {
							Element valueToUseElement = (Element) valueToUseElements.item(k);
							ynameSetForThisResult.add(valueToUseElement.getAttribute("name"));
						}
					}
					charts.put(result,ynameSetForThisResult);
				}
			}
			set.put(i,list);
			yNames.put(i,charts);
		}
	}

}
