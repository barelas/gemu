package gr.upatras.gemu.task;

import gr.upatras.gemu.node.Node;

/**
 * Represents an event at some epoch for the task.
 * @author George Barelas
 */
public class TaskHistory {
	
	public enum EventType {CREATION,SCHEDULED,START_TRANSMIT,END_TRANSMIT,START_CALC,END_CALC,ORIGIN_NODE_DEPARTED,COMP_NODE_DEPARTED}
	
	double time;
	EventType type;
	Node node1;
	Node node2;
	
	public TaskHistory(double time, EventType type, Node node1) {
		this.time = time;
		this.type = type;
		this.node1 = node1;
		this.node2 = null;
	}

	public TaskHistory(double time, EventType type, Node node1, Node node2) {
		this.time = time;
		this.type = type;
		this.node1 = node1;
		this.node2 = node2;
	}

	public double getTime() {
		return time;
	}

	public EventType getType() {
		return type;
	}

	public Node getNode1() {
		return node1;
	}

	public Node getNode2() {
		return node2;
	}
}
