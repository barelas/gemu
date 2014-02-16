package gr.upatras.gemu.node.producer;

import gr.upatras.gemu.grid.Grid;
import gr.upatras.gemu.node.Node;
import gr.upatras.gemu.node.departure.DepartureSchema;
import gr.upatras.gemu.node.link.producer.NodeLinkProducer;
import gr.upatras.gemu.node.weight.producer.WeightProducer;

import java.util.LinkedList;

/**
 * Generates few {@link Node}s for testing.
 * @author George Barelas
 */
public class TestNodeProducer implements NodeProducer {
	
	double nodeNumber = 0;
	Grid grid;
	DepartureSchema departureSchema;
	NodeLinkProducer linkProducer;
	WeightProducer weightProducer;
	
	public void generateNodes(LinkedList<Node> existingNodes) {
		switch (Math.round(Math.round(grid.getCurrentTime()))) {
		case 1:
			Node newNode = new Node(grid.getCurrentTime(),departureSchema.getNewInstance(),++nodeNumber,500D,grid);
			for (Node node:grid.getNodesOnline()) {
				linkProducer.establishNewLink(newNode,node);
				weightProducer.establishNewWeight(node,newNode);
				weightProducer.establishNewWeight(newNode,node);
			}
			weightProducer.establishNewWeight(newNode,newNode);
			existingNodes.add(newNode);
			break;
		case 2:
			newNode = new Node(grid.getCurrentTime(),departureSchema.getNewInstance(),++nodeNumber,500D,grid);
			for (Node node:grid.getNodesOnline()) {
				linkProducer.establishNewLink(newNode,node);
				weightProducer.establishNewWeight(node,newNode);
				weightProducer.establishNewWeight(newNode,node);
			}
			weightProducer.establishNewWeight(newNode,newNode);
			existingNodes.add(newNode);
			break;
		default:
			break;
		}
	}
	
	public void setGrid(Grid grid) {
		this.grid = grid;
	}

	public void setDepartureSchema(DepartureSchema departureSchema) {
		this.departureSchema = departureSchema;
	}

	public void setLinkProducer(NodeLinkProducer linkProducer) {
		this.linkProducer = linkProducer;
	}

	public void setWeightProducer(WeightProducer weightProducer) {
		this.weightProducer = weightProducer;
	}
}
