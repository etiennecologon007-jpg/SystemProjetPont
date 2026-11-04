/**
 * Véhicule terrestre générique (PARTIE A). Chaque véhicule est un thread.
 * Les sous-classes précisent le nombre de segments occupés.
 */
public abstract class Vehicule extends Thread {

    protected final int id;
    protected final Direction direction;
    protected final Pont pont;
    protected final int nbSegmentsOccupes; // 1 voiture, 2 camion, 4 convoi

    protected Vehicule(int id, Direction direction, Pont pont, int nbSegmentsOccupes) {
        this.id = id;
        this.direction = direction;
        this.pont = pont;
        this.nbSegmentsOccupes = nbSegmentsOccupes;
    }

    /** Cycle de vie : entrer, avancer segment par segment, sortir. */
    @Override
    public void run() {
        // TODO : pont.entrer(this); boucle { pont.attendreFinPriorite(this); pont.avancer(this); sleep } ; pont.sortir(this);
    }

    public int getVehiculeId() { return id; }
    public Direction getDirection() { return direction; }
    public int getNbSegmentsOccupes() { return nbSegmentsOccupes; }
}
