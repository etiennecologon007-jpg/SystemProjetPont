package general;

/** Sens de circulation des véhicules terrestres. */
public enum Direction {
    GAUCHE_DROITE("G>D"), DROITE_GAUCHE("D>G");

    private final String libelle;
    Direction(String libelle) { this.libelle = libelle; }

    /** @return le sens opposé. */
    public Direction oppose() {
        return this == GAUCHE_DROITE ? DROITE_GAUCHE : GAUCHE_DROITE;
    }

    /** @return un libellé court pour le journal. */
    public String libelle() { return libelle; }
}
