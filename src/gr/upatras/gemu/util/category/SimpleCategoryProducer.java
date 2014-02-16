package gr.upatras.gemu.util.category;

import gr.upatras.gemu.node.Node;
import gr.upatras.gemu.task.Task;
import gr.upatras.gemu.util.Pososto;

import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.random.RandomData;
import org.apache.commons.math.random.RandomDataImpl;

/**
 * Assigns categories using a uniform distribution.
 * @author George Barelas
 */
public class SimpleCategoryProducer implements CategoryProducer {
	
	RandomData randomDataForUsers;
	RandomData randomDataForTasks;
	LinkedList<Pososto> pososta;
	double totalPososto;
	static Log log = LogFactory.getLog(SimpleCategoryProducer.class);
	
	public SimpleCategoryProducer() {
		this.randomDataForUsers = new RandomDataImpl();
		this.randomDataForTasks = new RandomDataImpl();
		this.pososta = new LinkedList<Pososto>();
		this.totalPososto = 0;
	}
	
	public void setNewUserCategory(Node node) {
		double mark = randomDataForUsers.nextUniform(0,totalPososto);
		double sum = 0;
		for (Pososto p:pososta) {
			sum += p.getPososto();
			if (sum >= mark) {
				node.setUserCategory(p.getValue());
				return;
			}
		}
		node.setUserCategory(pososta.getLast().getValue());
	}
	
	public void setNewTaskCategory(Task task) {
		double mark = randomDataForTasks.nextUniform(0,totalPososto);
		double sum = 0;
		for (Pososto p:pososta) {
			sum += p.getPososto();
			if (sum >= mark) {
				task.setTaskCategory(p.getValue());
				return;
			}
		}
		task.setTaskCategory(pososta.getLast().getValue());
	}
	
	public void setPososto(Pososto pososto) {
		pososta.add(pososto);
		totalPososto += pososto.getPososto();
	}

}
