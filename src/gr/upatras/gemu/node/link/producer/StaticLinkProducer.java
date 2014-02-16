package gr.upatras.gemu.node.link.producer;

import gr.upatras.gemu.node.Node;
import gr.upatras.gemu.node.link.NodeLink;

/**
 * Generates {@link NodeLink}s using a fixed value for the link bandwidth.
 * @author George Barelas
 */
public class StaticLinkProducer implements NodeLinkProducer {
	
	double bandwidth = 0;
	
	public StaticLinkProducer() {
		
	}
	
	public void establishNewLink(Node node1, Node node2) {
		NodeLink link = new NodeLink(bandwidth,node1,node2);
		node1.addConnectedNode(node2,link);
		node2.addConnectedNode(node1,link);
	}

	public void setBandwidth(double bandwidth) {
		this.bandwidth = bandwidth;
	}
}
