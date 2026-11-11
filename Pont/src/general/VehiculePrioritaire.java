package general;

/**
 * Ambulance ou pompiers : prioritaire sur tous les autres véhicules (PARTIE A).
 * Il ne réserve pas de segments : pendant sa traversée, les véhicules de son sens sont à l'arrêt
 * sur la bande d'urgence (ils gardent leurs segments) et reprennent à sa sortie.
 */
public class VehiculePrioritaire extends Vehicule {

    public enum Type { AMBULANCE, POMPIERS }

    private final Type type;

    public VehiculePrioritaire(int id, Direction direction, Pont pont, Type type) {
        super(id, direction, pont, 1);
        this.type = type;
    }

    public Type getType() { return type; }

    @Override public char getSymbole() { return type == Type.AMBULANCE ? 'A' : 'P'; }

    /** Annonce son arrivée, traverse rapidement le pont, puis rend la main aux autres véhicules. */
    @Override
    public void run() {
        try {
            pont.debutPriorite(this);
            for (int i = 0; i < pont.getNbSegments(); i++) {
                tete = i; // position affichée sur la bande d'urgence
                Thread.sleep((long) (pasMs * 0.5));
            }
            pont.finPriorite(this);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
