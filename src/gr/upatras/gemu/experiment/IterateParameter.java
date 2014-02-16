package gr.upatras.gemu.experiment;

import org.apache.commons.beanutils.BeanUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Represents a parameter that experiments iterate on.
 * @author George Barelas
 */
public class IterateParameter {
	
	String name;
	String xpathExpr;
	double startValue;
	double stopValue;
	double step;
	
	/**
	 * Dhmiourgei ena IterateParameter object apo enan XML komvo.
	 * @param node
	 * @return
	 */
	static public IterateParameter getIterateParameterFromNode(Node node) throws Exception {
		IterateParameter iterateParameter = new IterateParameter();
		NamedNodeMap attrs = node.getAttributes();
		for (int i=0;i<attrs.getLength();i++) {
			Node attr = attrs.item(i);
			BeanUtils.setProperty(iterateParameter,attr.getNodeName(),attr.getNodeValue());
		}
		return iterateParameter;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getStartValue() {
		return startValue;
	}

	public void setStartValue(double startValue) {
		this.startValue = startValue;
	}

	public double getStep() {
		return step;
	}

	public void setStep(double step) {
		this.step = step;
	}

	public double getStopValue() {
		return stopValue;
	}

	public void setStopValue(double stopValue) {
		this.stopValue = stopValue;
	}

	public String getXpathExpr() {
		return xpathExpr;
	}

	public void setXpathExpr(String xpathExpr) {
		this.xpathExpr = xpathExpr;
	}

}
