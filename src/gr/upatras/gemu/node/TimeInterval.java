package gr.upatras.gemu.node;

import java.util.LinkedList;

/**
 * Represents a time interval, throughout which the node is in a state:
 * 1. Computing (could be receiving too, oi komboi exoun network frontend)
 * 2. Receiving
 * 3. Idle.
 * (Se opoiadhpote katastash mporei na stelnei dedomena me to network frontend, alla den mas endiaferei.)
 * Etsi mia lista apo synexomena TimeInterval mporei na perigrapsei plhrws to mellon tou kombou.
 * @author George Barelas
 */
public class TimeInterval {
	
	public enum NodeStatus {COMPUTING,RECEIVING,IDLE;
	
		public boolean greaterEqualThan(NodeStatus tmp) {
			boolean result = false;
			if (this==COMPUTING || this==tmp) result = true;
			else if (tmp==COMPUTING) result = false;
			else if (this==RECEIVING && tmp==IDLE) result = true;
			return result;
		}
	
	}
	
	double startTime = 0;
	double endTime = 0;
	NodeStatus status;
	
	public TimeInterval(double startTime, double endTime, NodeStatus status) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.status = status;
	}

	public LinkedList<TimeInterval> add(TimeInterval anotherInterval) {
		if (startTime>anotherInterval.getStartTime()) return anotherInterval.add(this);
		LinkedList<TimeInterval> result = new LinkedList<TimeInterval>();
		if (anotherInterval.getStartTime()>=endTime) {
			result.add(this);
			if (anotherInterval.getStartTime()>endTime) result.add(new TimeInterval(endTime,anotherInterval.getStartTime(),NodeStatus.IDLE));
			result.add(anotherInterval);
		} else if (startTime==anotherInterval.getStartTime()) {
			if (endTime>anotherInterval.getEndTime()) {
				anotherInterval.setStatus(getStrongerStatus(status,anotherInterval.getStatus()));
				result.add(anotherInterval);
				startTime = anotherInterval.getEndTime();
				result.add(this);
			} else if (endTime<anotherInterval.getEndTime()) {
				status = getStrongerStatus(status,anotherInterval.getStatus());
				result.add(this);
				anotherInterval.setStartTime(endTime);
				result.add(anotherInterval);
			} else {
				status = getStrongerStatus(status,anotherInterval.getStatus());
				result.add(this);
			}
		} else {
			if (status==anotherInterval.getStatus()) {
				if (status==NodeStatus.COMPUTING) {
					endTime += (anotherInterval.getEndTime()-anotherInterval.getStartTime());
					result.add(this);
				} else {
					endTime = Math.max(endTime,anotherInterval.getEndTime());
					result.add(this);
				}
			} else {
				result.add(new TimeInterval(startTime,anotherInterval.getStartTime(),status));
				startTime = anotherInterval.getStartTime();
				result.addAll(this.add(anotherInterval));
			}
		}
		return result;
	}
	
	public NodeStatus getStrongerStatus(NodeStatus a,NodeStatus b) {
		if (a.greaterEqualThan(b)) return a;
		else return b;
	}
	
	public boolean interleaves(TimeInterval a) {
		if (startTime<a.getStartTime() && endTime>a.getStartTime()) {
			return true;
		} else if (startTime>a.getStartTime() && startTime<a.getEndTime()) {
			return true;
		} else {
			return false;
		}
	}
	
	public double getEndTime() {
		return endTime;
	}
	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}
	public double getStartTime() {
		return startTime;
	}
	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}
	public NodeStatus getStatus() {
		return status;
	}
	public void setStatus(NodeStatus status) {
		this.status = status;
	}
}
