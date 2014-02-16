package gr.upatras.gemu.node.weight.producer;

import gr.upatras.gemu.node.Node;

/**
 * All weight generators must implement this interface.
 * @author George Barelas
 */
public interface WeightProducer {
	
	/**
	 * Establishes a weight that user has on a node.
	 * Must be called for a node and self too.
	 * Note that it usually must be called for a user-node and a node
	 * and vice-versa.
	 * @param user
	 * @param node
	 */
	public void establishNewWeight(Node user,Node node);

}
