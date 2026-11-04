package general;
/** Ambulance ou pompiers : prioritaire sur tous les autres véhicules (PARTIE A). */
public class VehiculePrioritaire extends Vehicule {

    public enum Type { AMBULANCE, POMPIERS }

    private final Type type;

    public VehiculePrioritaire(int id, Direction direction, Pont pont, Type type) {
        super(id, direction, pont, 1);
        this.type = type;
    }

    public Type getType() { return type; }

    @Override
    public void run() {
        // TODO : pont.debutPriorite(this); traverser le pont; pont.finPriorite(this);
    }
}
