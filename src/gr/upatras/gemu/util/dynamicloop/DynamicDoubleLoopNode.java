package gr.upatras.gemu.util.dynamicloop;

import java.util.LinkedList;

/**
 * (Experimental multi-dimentional simulation)
 * @author George Barelas
 */
public class DynamicDoubleLoopNode implements DynamicLoopNode {
	
	private double start;
	private double stop;
	private double step;
	private double now;
	DynamicLoopNode next;
	
	public DynamicDoubleLoopNode(double start, double stop, double step) {
		this.start = start;
		this.stop = stop;
		this.step = step;
		now = start;
	}

	public void init() {
		now = start;
		if (next!=null) {
			next.init();
		}
	}
	
	public LinkedList<Object> next() throws DoneMyLoop {
		if (next==null) {
			if (now > stop) {
				init();
				throw new DoneMyLoop();
			} else {
				LinkedList<Object> result = new LinkedList<Object>();
				result.addFirst(new Double(now));
				now += step;
				return result;
			}
		} else {
			if (now > stop) {
				init();
				throw new DoneMyLoop();
			} else {
				try {
					LinkedList<Object> result = next.next();
					result.addFirst(new Double(now));
					return result;
				} catch (DoneMyLoop dml) {
					now += step;
					return next();
				}
			}
		}
	}
	
	public void setNext(DynamicLoopNode next) {
		this.next = next;
	}

}
