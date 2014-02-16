package gr.upatras.gemu.experiment.xml;

import gr.upatras.gemu.experiment.Experiment;
import gr.upatras.gemu.experiment.IterateParameter;

import java.io.File;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class is responsible for creating and returning {@link Experiment} objects from an XML file. 
 * @author George Barelas
 */
public class ExperimentXMLReader {
	
	static Log log = LogFactory.getLog(ExperimentXMLReader.class);
	static XPathFactory xpathFactory = XPathFactory.newInstance();
	
	static public Document getDocumentFromXMLFile(File file) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			return builder.parse(file);
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e);
		}
	}
	
	public LinkedList<Experiment> getExperiment(File file) throws Exception {
		LinkedList<Experiment> result = new LinkedList<Experiment>();
		Document document = getDocumentFromXMLFile(file);
		if (!document.hasChildNodes()) {
			throw new Exception("no child nodes found!");
		}
		NodeList experimentTags = document.getElementsByTagName("experiment");
		for (int i=0;i<experimentTags.getLength();i++) {
			//String resultFilename = file.getName().substring(0,file.getName().length()-4) + "." + i + ".res";
			String resultFilename = file.getName().substring(0,file.getName().length()-4) + "." + i;
			result.add(getExperimentFromExperimentNode(experimentTags.item(i),resultFilename));
		}
		
		return result;
	}
	
	private void setIterateParameters(Experiment experiment,Node expNode) throws Exception {
		XPath xpath = xpathFactory.newXPath();
		XPathExpression xpathExpression = xpath.compile("iterateOn");
		NodeList iterateOnNodes = (NodeList) xpathExpression.evaluate(expNode,XPathConstants.NODESET);
		if (log.isDebugEnabled()) {
			log.debug("iterateOnNodes.size():"+iterateOnNodes.getLength());
		}
		for (int i=0;i<iterateOnNodes.getLength();i++) {
			Node n = iterateOnNodes.item(i);
			experiment.addIterateParameter(IterateParameter.getIterateParameterFromNode(n));
			expNode.removeChild(n);
			if (log.isDebugEnabled()) {
				log.debug("xml node removed:"+n);
			}
		}
	}
	
	private Experiment getExperimentFromExperimentNode(Node expNode,String resultFilename) throws Exception {
		Experiment experiment = new Experiment();
		experiment.setResultFilename(resultFilename);
		setIterateParameters(experiment,expNode);
		experiment.setExpNode(expNode);
		return experiment;
	}
}
