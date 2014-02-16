package gr.upatras.gemu.util.category;

import gr.upatras.gemu.node.Node;
import gr.upatras.gemu.task.Task;

/**
 * All category generators must implement this interface.
 * @author George Barelas
 */
public interface CategoryProducer {
	
	public void setNewUserCategory(Node node);
	
	public void setNewTaskCategory(Task task);

}
