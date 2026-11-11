package general;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

/**
 * File FIFO commune des bateaux et cycle d'ouverture du pont (PARTIE B) - ETAPE 3.
 * Un thread contrôleur gère le cycle : attendre un bateau ET l'autorisation -> ouvrir -> laisser passer -> fermer.
 * Après chaque fermeture, un Timer lève l'autorisation d'ouverture après un délai minimal.
 */
public class GestionnaireBateaux {

    private final Pont pont;
    private final long delaiEntreOuverturesMs; // 60 000 ms dans le sujet (1 minute en simulation)
    private long heureProchaineOuverture = 0;  // en ms (System.currentTimeMillis)
    private boolean ouvertureAutorisee = true; // true au départ : première ouverture possible tout de suite
    private final Timer timer = new Timer(true);

    private final Queue<Bateau> file = new LinkedList<>();
    private boolean traversee = false;
    private Bateau enTraversee = null;      // bateau actuellement sous le pont (pour l'affichage)
    private long debutTraversee = 0;
    private boolean fermetureDecidee = false;

    public GestionnaireBateaux(Pont pont, long delaiEntreOuverturesMs) {
        this.pont = pont;
        this.delaiEntreOuverturesMs = delaiEntreOuverturesMs;
        Thread controleur = new Thread(this::cycleOuverture, "controleur-pont");
        controleur.setDaemon(true);
        controleur.start();
    }

    /** Ajoute le bateau en fin de file et réveille le contrôleur. */
    public synchronized void enregistrer(Bateau b) {
        file.add(b);
        Affichage.log("[file] Bateau " + b.getBateauId() + " (" + b.getDirection() + ") arrive");
        notifyAll();
    }

    /** Bloque jusqu'à : tête de file, voie libre, pont OUVERT et fermeture non décidée. */
    public synchronized void attendreTour(Bateau b) throws InterruptedException {
        while (file.peek() != b || traversee || fermetureDecidee || pont.getEtat() != EtatPont.OUVERT) {
            wait();
        }
        file.poll();
        traversee = true;
        enTraversee = b;
        debutTraversee = System.currentTimeMillis();
    }

    /** Le bateau a fini de traverser : libère la voie et réveille tout le monde. */
    public synchronized void finTraversee(Bateau b) {
        traversee = false;
        enTraversee = null;
        Affichage.log("[file] Bateau " + b.getBateauId() + " a fini");
        notifyAll();
    }


    // ---------- Lecture de l'état, pour l'affichage ----------

    /** @return une copie de la file d'attente (ordre FIFO). */
    public synchronized List<Bateau> getFile() { return new ArrayList<>(file); }

    /** @return le bateau actuellement sous le pont, ou null. */
    public synchronized Bateau getBateauEnTraversee() { return enTraversee; }

    /** @return l'heure (ms) de début de la traversée en cours. */
    public synchronized long getDebutTraversee() { return debutTraversee; }

    /** @return true si une nouvelle ouverture est déjà autorisée (délai écoulé). */
    public synchronized boolean isOuvertureAutorisee() { return ouvertureAutorisee; }

    /** @return l'heure (ms) de la prochaine ouverture autorisée (utile pour l'affichage). */
    public synchronized long getHeureProchaineOuverture() {
        return heureProchaineOuverture;
    }

    /** Boucle du thread contrôleur. ouvrir()/fermer() sont appelés HORS synchronized. */
    private void cycleOuverture() {
        try {
            while (true) {
                // 1) attendre : au moins un bateau EN FILE ET ouverture autorisée (délai écoulé)
                synchronized (this) {
                    while (file.isEmpty() || !ouvertureAutorisee) wait();
                    ouvertureAutorisee = false;
                }
                // 2) ouvrir, puis réveiller les bateaux
                pont.ouvrir();
                synchronized (this) { notifyAll(); }

                // 3) attendre file vide + voie libre, puis interdire tout passage
                synchronized (this) {
                    while (!file.isEmpty() || traversee) wait();
                    fermetureDecidee = true;
                }
                // 4) fermer
                pont.fermer();
                synchronized (this) {
                    fermetureDecidee = false;
                    notifyAll();
                }
                // 5) mémoriser l'heure de fermeture complète et armer le Timer
                programmerProchaineOuverture();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * T_prochaine_ouverture = T_fermeture_complete + délai.
     * Le TimerTask lève l'autorisation et réveille le contrôleur, qui vérifie alors s'il y a des bateaux.
     */
    private void programmerProchaineOuverture() {
        synchronized (this) {
            heureProchaineOuverture = System.currentTimeMillis() + delaiEntreOuverturesMs;
        }
        timer.schedule(new TimerTask() {
            @Override public void run() {
                synchronized (GestionnaireBateaux.this) {
                    ouvertureAutorisee = true;
                    Affichage.log("[timer] Nouvelle ouverture autorisée");
                    GestionnaireBateaux.this.notifyAll();
                }
            }
        }, delaiEntreOuverturesMs);
    }

    private String ids() {
        StringBuilder sb = new StringBuilder("[");
        for (Bateau b : file) sb.append(b.getBateauId()).append(' ');
        return sb.append(']').toString();
    }
}