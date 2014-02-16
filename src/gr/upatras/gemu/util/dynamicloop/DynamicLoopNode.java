package gr.upatras.gemu.util.dynamicloop;

import java.util.LinkedList;

/**
 * (Experimental multi-dimentional simulation)
 * @author George Barelas
 */
public interface DynamicLoopNode {
	
	/**
	 * Reinit internal loop state. If it's not a tail,
	 * the implementing class must reinit() the next DynamicLoopNode.
	 *
	 */
	public void init();
	
	/**
	 * Returns the next object in the internal loop. If the loop ends,
	 * the implementing class must reinit() and throw a DoneMyLoop object.
	 * @return
	 * @throws DoneMyLoop
	 */
	public LinkedList<Object> next() throws DoneMyLoop;
	
	public void setNext(DynamicLoopNode next);

}
