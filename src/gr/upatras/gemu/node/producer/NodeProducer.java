package gr.upatras.gemu.node.producer;

import gr.upatras.gemu.grid.Grid;
import gr.upatras.gemu.node.Node;
import gr.upatras.gemu.node.departure.DepartureSchema;
import gr.upatras.gemu.node.link.producer.NodeLinkProducer;
import gr.upatras.gemu.node.weight.producer.WeightProducer;

import java.util.LinkedList;

/**
 * Interface that Node producers-generators must implement.
 * @author George Barelas
 */
public interface NodeProducer {
	
	/**
	 * Kaleitai mia fora se ka8e epoch. Kanei add komvous sth lista me tous yparxontes.
	 * @param existingNodes
	 */
	public void generateNodes(LinkedList<Node> existingNodes);
	
	/**
	 * Kaleitai kata to init, 8etei to Grid object to opoio ekteleitai.
	 * Gia na pairnei to currentTime kai na 8etei to Grid object stous paragomenous komvous.
	 * @param grid
	 */
	public void setGrid(Grid grid);
	
	/**
	 * 8etei to NodeLinkProducer object pou 8a xrhsimopoihsei gia th dhmiourgia network links.
	 * @param linkProducer
	 */
	public void setLinkProducer(NodeLinkProducer linkProducer);
	
	/**
	 * 8etei to departureSchema object pou 8a xrhsimopoih8ei gia na paragei ta departureSchema
	 * pou 8a 8etontai se ka8e kombo.
	 * @param departureSchema
	 */
	public void setDepartureSchema(DepartureSchema departureSchema);
	
	/**
	 * 8etei to weightProducer object pou 8a xrhsimopoih8ei gia thn paragwgh
	 * barwn twn xrhtwn-kombwn se allous kombous.
	 * @param weightProducer
	 */
	public void setWeightProducer(WeightProducer weightProducer);
}
