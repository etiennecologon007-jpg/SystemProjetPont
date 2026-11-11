package general;

/** Camion : occupe 2 segments consécutifs, réservés simultanément (PARTIE A). */
public class Camion extends Vehicule {
    public Camion(int id, Direction direction, Pont pont) {
        super(id, direction, pont, 2);
    }
    @Override protected double lenteur() { return 1.5; }
    @Override public char getSymbole() { return 'T'; }
}
