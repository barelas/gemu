package gr.upatras.gemu.node.cpuhunger;

import gr.upatras.gemu.node.Node;
import gr.upatras.gemu.util.Pososto;

import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.random.RandomData;
import org.apache.commons.math.random.RandomDataImpl;

/**
 * Concrete {@link CpuhungerProducer} class.
 * @author George Barelas
 */
public class SimpleCpuhungerProducer implements CpuhungerProducer {
	
	RandomData randomData;
	LinkedList<Pososto> pososta;
	double totalPososto;
	static Log log = LogFactory.getLog(SimpleCpuhungerProducer.class);
	
	public SimpleCpuhungerProducer() {
		this.randomData = new RandomDataImpl();
		this.pososta = new LinkedList<Pososto>();
		this.totalPososto = 0;
	}
	
	public void setNewCpuhunger(Node node) {
		double mark = randomData.nextUniform(0,totalPososto);
		double sum = 0;
		for (Pososto p:pososta) {
			sum += p.getPososto();
			if (sum >= mark) {
				node.setCpuhunger(p.getValue());
				return;
			}
		}
		node.setCpuhunger(pososta.getLast().getValue());
	}
	
	public void setPososto(Pososto pososto) {
		pososta.add(pososto);
		totalPososto += pososto.getPososto();
//		if (log.isInfoEnabled()) {
//			log.info("added Pososto:"+pososto.getPososto() + " for cpuhunger:"+pososto.getCpuhunger());
//		}
	}

}
