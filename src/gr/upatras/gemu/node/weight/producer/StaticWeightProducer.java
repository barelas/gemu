package gr.upatras.gemu.node.weight.producer;

import gr.upatras.gemu.node.Node;
import gr.upatras.gemu.node.weight.Weight;

/**
 * Generates {@link Weight}s with a fixed value.
 * @author George Barelas
 */
public class StaticWeightProducer implements WeightProducer {
	
	double weight = 1;

	public void establishNewWeight(Node user, Node node) {
		node.addWeight(new Weight(user,node,weight));
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}
}
