package general;
import java.util.Timer;
import java.util.TimerTask;

/**
 * File FIFO commune des bateaux et cycle d'ouverture du pont (PARTIE B).
 * Utilise Pont.ouvrir() / Pont.fermer() fournis par la partie A.
 */
public class GestionnaireBateaux {

    private final Pont pont;
    private final long delaiEntreOuverturesMs; // 60 000 ms (1 minute en simulation)
    private long heureProchaineOuverture = 0;
    private final Timer timer = new Timer(true);

    // TODO : file FIFO (ex: Queue<Bateau>), bateau actuellement en traversée

    public GestionnaireBateaux(Pont pont, long delaiEntreOuverturesMs) {
        this.pont = pont;
        this.delaiEntreOuverturesMs = delaiEntreOuverturesMs;
    }

    /** Ajoute le bateau en fin de file et déclenche une ouverture si elle est autorisée. */
    public synchronized void enregistrer(Bateau b) {
        // TODO
    }

    /** Bloque jusqu'à ce que le bateau soit en tête de file ET que le pont soit OUVERT. */
    public synchronized void attendreTour(Bateau b) throws InterruptedException {
        // TODO
    }

    /** Le bateau a fini de traverser : suivant, ou fermeture du pont si la file est vide. */
    public synchronized void finTraversee(Bateau b) {
        // TODO
    }

    /** Vérifie : bateaux en attente + heure autorisée atteinte -> lance le cycle d'ouverture. */
    private void verifierOuverture() {
        // TODO
    }

    /** Programme verifierOuverture() à l'heure de la prochaine ouverture autorisée (Timer/TimerTask). */
    private void programmerProchaineOuverture() {
        // TODO : heureProchaineOuverture = now + delaiEntreOuverturesMs ; timer.schedule(...)
    }
}
