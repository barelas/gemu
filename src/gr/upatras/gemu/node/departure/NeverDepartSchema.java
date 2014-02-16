package gr.upatras.gemu.node.departure;

/**
 * Node never departs.
 * @author George Barelas
 */
public class NeverDepartSchema implements DepartureSchema {

	/**
	 * Epistrefei panta true: o kombos den anaxwrei pote.
	 */
	public boolean nextEpoch() {
		return true;
	}
	
	/**
	 * Afou den exei idiaiteres metablhtes kai katastashs, epistrefei to monadiko
	 * instance ths klasshs.
	 */
	public DepartureSchema getNewInstance() {
		return this;
	}
}
