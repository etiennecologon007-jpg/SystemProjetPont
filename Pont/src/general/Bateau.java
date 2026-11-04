package general;
/** Bateau : thread qui s'enregistre dans la file FIFO puis traverse sous le pont (PARTIE B). */
public class Bateau extends Thread {

    private final int id;
    private final DirectionBateau direction;
    private final GestionnaireBateaux gestionnaire;

    public Bateau(int id, DirectionBateau direction, GestionnaireBateaux gestionnaire) {
        this.id = id;
        this.direction = direction;
        this.gestionnaire = gestionnaire;
    }

    /** Cycle de vie : s'enregistrer, attendre son tour (FIFO + pont OUVERT), traverser, signaler la fin. */
    @Override
    public void run() {
        // TODO : gestionnaire.enregistrer(this); gestionnaire.attendreTour(this); traverser; gestionnaire.finTraversee(this);
    }

    public int getBateauId() { return id; }
    public DirectionBateau getDirection() { return direction; }
}
