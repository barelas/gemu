package gr.upatras.gemu.util.dynamicloop;

import java.util.LinkedList;

/**
 * (Experimental multi-dimentional simulation)
 * @author George Barelas
 */
public class DynamicObjectLoopNode implements DynamicLoopNode {
	
	private LinkedList<Object> list;
	private int i;
	private DynamicLoopNode next;
	
	public DynamicObjectLoopNode(LinkedList<Object> list) {
		this.list = list;
		this.i = 0;
		this.next = null;
	}

	public void init() {
		i = 0;
		if (next!=null) {
			next.init();
		} 
	}
	
	public LinkedList<Object> next() throws DoneMyLoop {
		if (next==null) {
			if (i >= list.size()) {
				init();
				throw new DoneMyLoop();
			} else {
				LinkedList<Object> result = new LinkedList<Object>();
				result.addFirst(list.get(i++));
				return result;
			}
		} else {
			if (i >= list.size()) {
				init();
				throw new DoneMyLoop();
			} else {
				try {
					LinkedList<Object> result = next.next();
					result.addFirst(list.get(i));
					return result;
				} catch (DoneMyLoop done) {
					++i;
					return next();
				}
			}
		}
	}

	public void setNext(DynamicLoopNode next) {
		this.next = next;
	}

}
