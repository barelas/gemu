package gr.upatras.gemu.node.link.producer;

import gr.upatras.gemu.node.Node;

/**
 * Interface that NodeLinkProducers must implement.
 * @author George Barelas
 */
public interface NodeLinkProducer {
	
	public void establishNewLink(Node node1,Node node2);

}
