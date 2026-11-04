package general;

import java.util.LinkedList;
import java.util.Queue;

/**
 * File FIFO commune des bateaux (PARTIE B) - ETAPE 1 : file + traversée, sans pont ni timer.
 * Règle : un bateau ne traverse que s'il est en tête de file et si aucun autre bateau ne traverse.
 */
public class GestionnaireBateaux {

    private final Pont pont; // inutilisé à l'étape 1 (peut être null)
    private final long delaiEntreOuverturesMs;

    private final Queue<Bateau> file = new LinkedList<>();
    private boolean traversee = false; // true si un bateau est en train de passer

    public GestionnaireBateaux(Pont pont, long delaiEntreOuverturesMs) {
        this.pont = pont;
        this.delaiEntreOuverturesMs = delaiEntreOuverturesMs;
    }

    /** Ajoute le bateau en fin de file (ordre d'arrivée = ordre de passage). */
    public synchronized void enregistrer(Bateau b) {
        file.add(b);
        System.out.println("[file] Bateau " + b.getBateauId() + " (" + b.getDirection() + ") arrive. File : " + ids());
    }

    /** Bloque jusqu'à ce que le bateau soit en tête de file ET que la voie soit libre. */
    public synchronized void attendreTour(Bateau b) throws InterruptedException {
        // while (et non if) : on revérifie la condition après chaque réveil
        while (file.peek() != b || traversee) {
            wait();
        }
        file.poll();       // il quitte la file...
        traversee = true;  // ...et occupe la voie
    }

    /** Le bateau a fini de traverser : on libère la voie et on réveille les bateaux en attente. */
    public synchronized void finTraversee(Bateau b) {
        traversee = false;
        System.out.println("[file] Bateau " + b.getBateauId() + " a fini. File : " + ids());
        notifyAll();
    }

    /** Aide au débogage : liste des identifiants dans la file. */
    private String ids() {
        StringBuilder sb = new StringBuilder("[");
        for (Bateau b : file) sb.append(b.getBateauId()).append(' ');
        return sb.append(']').toString();
    }
}