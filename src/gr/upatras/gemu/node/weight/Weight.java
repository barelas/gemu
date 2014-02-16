package gr.upatras.gemu.node.weight;

import gr.upatras.gemu.node.Node;

/**
 * Represents a weight that a user has on a node.
 * @author George Barelas
 */
public class Weight {
	
	private Node user;
	private Node node;
	private double weight;
	
	public Weight(Node user, Node node, double weight) {
		this.user = user;
		this.node = node;
		this.weight = weight;
	}

	public Node getNode() {
		return node;
	}

	public Node getUser() {
		return user;
	}

	public double getWeight() {
		return weight;
	}
}
