package gr.upatras.gemu.node.producer;

import gr.upatras.gemu.grid.Grid;
import gr.upatras.gemu.node.Node;
import gr.upatras.gemu.node.departure.DepartureSchema;
import gr.upatras.gemu.node.link.producer.NodeLinkProducer;
import gr.upatras.gemu.node.weight.producer.WeightProducer;
import gr.upatras.gemu.util.MathUtil;
import gr.upatras.gemu.util.StatisticalCharacteristics;

import java.util.LinkedList;
import java.util.Random;

/**
 * Generates {@link Node}s using a Gaussian distribution for their arrival times and computational capacity.
 * @author George Barelas
 */
public class SimpleNodeProducer implements NodeProducer {
	
	double countdown = 0;
	StatisticalCharacteristics statChars;
	private double nodeNumber = 0;
	Random random;
	Grid grid;
	DepartureSchema departureSchema;
	NodeLinkProducer linkProducer;
	WeightProducer weightProducer;
	
	public SimpleNodeProducer() {
		this.random = new Random();
	}
	
	public SimpleNodeProducer(StatisticalCharacteristics statChars,Grid grid,DepartureSchema departureSchema,NodeLinkProducer linkProducer) {
		this.grid = grid;
		this.statChars = statChars;
		this.random = new Random();
		this.departureSchema = departureSchema;
		this.linkProducer = linkProducer;
	}

	/* (non-Javadoc)
	 * @see gr.upatras.gemu.node.producer.NodeProducer#generateNodes(java.util.LinkedList)
	 */
	public void generateNodes(LinkedList<Node> existingNodes) {
		if (--countdown<=0) {
			getNewCountdown();
			existingNodes.add(getNewNode());
		}
	}
	
	private void getNewCountdown() {
		countdown = MathUtil.getNextNumber(statChars.getEpochMean(),statChars.getEpochDeviation(),random.nextGaussian(),1D);
	}
	
	private Node getNewNode() {
		Node newNode = new Node(grid.getCurrentTime(),departureSchema.getNewInstance(),++nodeNumber,MathUtil.getNextNumber(statChars.getCcMean(),statChars.getCcDeviation(), random.nextGaussian(),1D),grid);
		for (Node node:grid.getNodesOnline()) {
			linkProducer.establishNewLink(newNode,node);
			weightProducer.establishNewWeight(node,newNode);
			weightProducer.establishNewWeight(newNode,node);
		}
		weightProducer.establishNewWeight(newNode,newNode);
		return newNode;
	}
	
	public void setDepartureSchema(DepartureSchema departureSchema) {
		this.departureSchema = departureSchema;
	}

	public void setLinkProducer(NodeLinkProducer linkProducer) {
		this.linkProducer = linkProducer;
	}

	public void setStatChars(StatisticalCharacteristics statChars) {
		this.statChars = statChars;
	}

	public void setGrid(Grid grid) {
		this.grid = grid;
	}

	public void setWeightProducer(WeightProducer weightProducer) {
		this.weightProducer = weightProducer;
	}
}
