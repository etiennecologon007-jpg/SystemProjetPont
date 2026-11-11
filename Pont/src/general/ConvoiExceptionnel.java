package general;

/**
 * Convoi exceptionnel : 4 segments, aucun véhicule du sens opposé pendant sa traversée (PARTIE A).
 * La logique d'exclusion avec le sens opposé est dans Pont.entrer().
 */
public class ConvoiExceptionnel extends Vehicule {
    public ConvoiExceptionnel(int id, Direction direction, Pont pont) {
        super(id, direction, pont, 4);
    }
    @Override protected double lenteur() { return 2.0; }
    @Override public char getSymbole() { return 'X'; }
}
