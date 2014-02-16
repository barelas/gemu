package gr.upatras.gemu.node.link;

import gr.upatras.gemu.node.Node;

/**
 * Represents a network link between two {@link Node}s.
 * @author George Barelas
 */
public class NodeLink {
	
	Node node1;
	Node node2;
	
	private double bandwidth = 0;
	
	public NodeLink(double bandwidth,Node node1,Node node2) {
		this.bandwidth = bandwidth;
		this.node1 = node1;
		this.node2 = node2;
	}

	public double getBandwidth() {
		return bandwidth;
	}
}
