package gr.upatras.gemu.util.dynamicloop;

import java.util.LinkedList;

/**
 * (Experimental multi-dimentional simulation)
 * @author George Barelas
 */
public class DynamicFor {
	
	private DynamicLoopNode head;
	
	public DynamicFor(DynamicLoopNode head) {
		this.head = head;
	}
	
	public LinkedList<Object> next() {
		LinkedList<Object> tmp = null;
		try {
			tmp = head.next();
		} catch (DoneMyLoop done) {
			
		}
		return tmp;
	}

}
