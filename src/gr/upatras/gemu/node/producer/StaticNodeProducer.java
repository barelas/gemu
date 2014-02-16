package gr.upatras.gemu.node.producer;

import gr.upatras.gemu.grid.Grid;
import gr.upatras.gemu.node.Node;
import gr.upatras.gemu.node.cpuhunger.CpuhungerProducer;
import gr.upatras.gemu.node.departure.DepartureSchema;
import gr.upatras.gemu.node.link.producer.NodeLinkProducer;
import gr.upatras.gemu.node.weight.producer.WeightProducer;
import gr.upatras.gemu.util.category.CategoryProducer;

import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * On first run, generates a specific set of {@link Node}s with a fixed capacity.
 * @author George Barelas
 */
public class StaticNodeProducer implements NodeProducer {
	
	long numberOfInitialNodes = 0;
	boolean nodesGenerated = false;
	private double nodeNumber = 0;
	Grid grid;
	DepartureSchema departureSchema;
	NodeLinkProducer linkProducer;
	double computationalCapacity = 0;
	static Log log = LogFactory.getLog(StaticNodeProducer.class);
	WeightProducer weightProducer;
	CpuhungerProducer cpuhungerProducer;
	CategoryProducer categoryProducer;
	
	public StaticNodeProducer() {
		
	}

	public void generateNodes(LinkedList<Node> existingNodes) {
		if (log.isDebugEnabled()) {
			log.debug("nodesGenerated="+nodesGenerated);
			log.debug("# of existing nodes:"+existingNodes.size());
			log.debug("# of online nodes:"+grid.getNodesOnline().size());
		}
		if (nodesGenerated) return;
		for (long i=0;i<numberOfInitialNodes;i++) {
			existingNodes.add(getNewNode());
		}
		//calcAndPrintNodeStats(existingNodes);
		nodesGenerated = true;
	}
	
	/**
	 * just for testing purposes...
	 * @param list
	 */
	private void calcAndPrintNodeStats(LinkedList<Node> list) {
		double nodeStatsType1Nodes = 0;
		double nodeStatsType2Nodes = 0;
		for (Node n:list) {
			//if (n.getCpuhunger()>2.5) {
			if (n.getUserCategory()>2.5) {
				nodeStatsType2Nodes++;
			} else {
				nodeStatsType1Nodes++;
			}
		}
		log.info("type 1 nodes:"+nodeStatsType1Nodes + " type 2 nodes:"+nodeStatsType2Nodes);
	}
	
	private Node getNewNode() {
		Node newNode = new Node(grid.getCurrentTime(),departureSchema.getNewInstance(),++nodeNumber,computationalCapacity,grid);
		for (Node node:grid.getNodesOnline()) {
			linkProducer.establishNewLink(newNode,node);
			weightProducer.establishNewWeight(node,newNode);
			weightProducer.establishNewWeight(newNode,node);
		}
		weightProducer.establishNewWeight(newNode,newNode);
		cpuhungerProducer.setNewCpuhunger(newNode);
		categoryProducer.setNewUserCategory(newNode);
		return newNode;
	}

	public void setGrid(Grid grid) {
		this.grid = grid;
	}

	public void setLinkProducer(NodeLinkProducer linkProducer) {
		this.linkProducer = linkProducer;
	}
	
	public void setDepartureSchema(DepartureSchema departureSchema) {
		this.departureSchema = departureSchema;
	}

	public void setNumberOfInitialNodes(long numberOfInitialNodes) {
		this.numberOfInitialNodes = numberOfInitialNodes;
	}

	public void setComputationalCapacity(double computationalCapacity) {
		this.computationalCapacity = computationalCapacity;
	}

	public void setWeightProducer(WeightProducer weightProducer) {
		this.weightProducer = weightProducer;
	}

	public void setCpuhungerProducer(CpuhungerProducer cpuhungerProducer) {
		this.cpuhungerProducer = cpuhungerProducer;
	}

	public void setCategoryProducer(CategoryProducer categoryProducer) {
		this.categoryProducer = categoryProducer;
	}
}
