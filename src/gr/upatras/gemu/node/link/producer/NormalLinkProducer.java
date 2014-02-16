package gr.upatras.gemu.node.link.producer;

import gr.upatras.gemu.node.Node;
import gr.upatras.gemu.node.link.NodeLink;
import gr.upatras.gemu.util.MathUtil;
import gr.upatras.gemu.util.StatisticalCharacteristics;

import java.util.Random;

/**
 * Generates {@link NodeLink}s using a Gaussian distribution for the link bandwidth.
 * @author George Barelas
 */
public class NormalLinkProducer implements NodeLinkProducer {
	
	StatisticalCharacteristics statsChars;
	Random random;
	
	public NormalLinkProducer() {
		this.random = new Random();
	}
	
	public NormalLinkProducer(StatisticalCharacteristics statsChars) {
		this.statsChars = statsChars;
		this.random = new Random();
	}

	public void establishNewLink(Node node1,Node node2) {
		NodeLink link = new NodeLink(MathUtil.getNextNumber(statsChars.getBandwidthMean(),statsChars.getBandwidthDeviation(),random.nextGaussian(),1D),node1,node2);
		node1.addConnectedNode(node2,link);
		node2.addConnectedNode(node1,link);
	}

	public void setStatsChars(StatisticalCharacteristics statsChars) {
		this.statsChars = statsChars;
	}

}
