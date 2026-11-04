package general;
/**
 * Convoi exceptionnel : 4 segments, aucun véhicule du sens opposé pendant sa traversée (PARTIE A).
 * À l'arrivée : bloque l'entrée opposée, attend que le sens opposé soit vidé.
 */
public class ConvoiExceptionnel extends Vehicule {
    public ConvoiExceptionnel(int id, Direction direction, Pont pont) {
        super(id, direction, pont, 4);
    }
}
