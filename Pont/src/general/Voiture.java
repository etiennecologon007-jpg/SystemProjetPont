package general;

/** Voiture : occupe 1 segment à la fois (PARTIE A). */
public class Voiture extends Vehicule {
    public Voiture(int id, Direction direction, Pont pont) {
        super(id, direction, pont, 1);
    }
    @Override public char getSymbole() { return 'C'; }
}
