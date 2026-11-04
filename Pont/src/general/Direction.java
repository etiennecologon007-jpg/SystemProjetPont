package general;
/** Sens de circulation des véhicules terrestres. */
public enum Direction {
    GAUCHE_DROITE, DROITE_GAUCHE;

    /** @return le sens opposé. */
    public Direction oppose() {
        return this == GAUCHE_DROITE ? DROITE_GAUCHE : GAUCHE_DROITE;
    }
}
