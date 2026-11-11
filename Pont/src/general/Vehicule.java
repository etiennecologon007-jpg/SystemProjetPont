package general;

/**
 * Véhicule terrestre générique (PARTIE A). Chaque véhicule est un thread.
 * Les sous-classes précisent le nombre de segments occupés et la vitesse.
 * L'état "sur le pont" (tete, occupes) n'est modifié que par la classe Pont, sous son verrou.
 */
public abstract class Vehicule extends Thread {

    /** Durée (ms) d'un pas d'avancement d'une voiture. Modifiable pour accélérer les tests. */
    public static long pasMs = 400;

    protected final int id;
    protected final Direction direction;
    protected final Pont pont;
    protected final int nbSegmentsOccupes; // 1 voiture, 2 camion, 4 convoi

    /** Indice (dans l'ordre de parcours du sens) du segment de tête ; -1 avant l'entrée. */
    volatile int tete = -1;
    /** Nombre de segments actuellement occupés (diminue à la sortie). */
    int occupes = 0;

    protected Vehicule(int id, Direction direction, Pont pont, int nbSegmentsOccupes) {
        this.id = id;
        this.direction = direction;
        this.pont = pont;
        this.nbSegmentsOccupes = nbSegmentsOccupes;
    }

    /** @return facteur de lenteur : 1 = vitesse d'une voiture, 2 = deux fois plus lent. */
    protected double lenteur() { return 1.0; }

    /** @return la lettre affichée sur le pont. */
    public char getSymbole() { return 'V'; }

    /** Cycle de vie : entrer, avancer segment par segment, sortir. */
    @Override
    public void run() {
        try {
            pont.entrer(this); // bloque jusqu'à ce que l'entrée soit autorisée
            do {
                Thread.sleep((long) (pasMs * lenteur()));
            } while (pont.avancer(this)); // avancer() renvoie false quand le véhicule a quitté le pont
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getVehiculeId() { return id; }
    public Direction getDirection() { return direction; }
    public int getNbSegmentsOccupes() { return nbSegmentsOccupes; }
}
