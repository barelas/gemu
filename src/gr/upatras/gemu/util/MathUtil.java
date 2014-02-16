package gr.upatras.gemu.util;

/**
 * Math utilities.
 * @author George Barelas
 */
public class MathUtil {
	
	static public double getNextNumber(double mean,double deviation,double random,double min) {
		return Math.max(deviation*random + mean,min);
	}
	
	static public int rotateLeft(int i,int bits) {
		bits &= 0x1F;
		return (i << bits) | (i >>> (32-bits));
	}

}
