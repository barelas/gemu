package gr.upatras.gemu.util;

/**
 * Represents a value pair, commonly used for percentage values.
 * @author George Barelas
 */
public class Pososto {
	
	double pososto;
	double value;
	
	public double getValue() {
		return value;
	}
	public void setValue(double value) {
		this.value = value;
	}
	public double getPososto() {
		return pososto;
	}
	public void setPososto(double pososto) {
		this.pososto = pososto;
	}
}
