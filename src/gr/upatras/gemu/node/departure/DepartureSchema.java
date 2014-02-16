package gr.upatras.gemu.node.departure;

/**
 * Interface that DepartureSchema classes must implement.
 * @author George Barelas
 */
public interface DepartureSchema {
	
	/**
	 * 
	 * @return true an o kombos parameinei, false gia na anaxwrhsei apo to Grid.
	 */
	public boolean nextEpoch();
	
	/**
	 * 8a kaleitai gia na epistrejei ena instance DepartureSchema pou 8a enswmatw8ei se enan neo kombo.
	 * @return
	 */
	public DepartureSchema getNewInstance();
}
