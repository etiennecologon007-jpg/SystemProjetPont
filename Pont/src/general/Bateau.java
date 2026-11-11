package general;

/** Bateau : thread qui s'enregistre dans la file FIFO puis traverse sous le pont (PARTIE B). */
public class Bateau extends Thread {

    /** Durée de la traversée (ms), utilisée aussi par l'affichage pour animer le bateau. */
    public static final long DUREE_TRAVERSEE_MS = 3000;

    private final int id;
    private final DirectionBateau direction;
    private final GestionnaireBateaux gestionnaire;

    public Bateau(int id, DirectionBateau direction, GestionnaireBateaux gestionnaire) {
        this.id = id;
        this.direction = direction;
        this.gestionnaire = gestionnaire;
    }

    /** Cycle de vie : s'enregistrer, attendre son tour, traverser, signaler la fin. */
    @Override
    public void run() {
        try {
            gestionnaire.enregistrer(this);
            gestionnaire.attendreTour(this);
            Affichage.log(">>> Bateau " + id + " (" + direction + ") traverse");
            Thread.sleep(DUREE_TRAVERSEE_MS); // hors de tout bloc synchronized
            gestionnaire.finTraversee(this);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getBateauId() { return id; }
    public DirectionBateau getDirection() { return direction; }
}