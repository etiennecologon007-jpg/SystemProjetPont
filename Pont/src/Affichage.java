/**
 * Affichage console animé avec séquences ANSI (PARTIE B).
 * Méthodes synchronisées pour éviter que les threads mélangent leurs écritures.
 */
public class Affichage {

    /** Efface l'écran et replace le curseur en haut à gauche. */
    public static synchronized void effacerEcran() {
        System.out.print("\033[2J\033[H");
        System.out.flush();
    }

    /** Écrit un texte à la ligne/colonne donnée. */
    public static synchronized void ecrireA(int ligne, int colonne, String texte) {
        System.out.print("\033[" + ligne + ";" + colonne + "H" + texte);
        System.out.flush();
    }

    // TODO : dessiner le pont et ses segments, afficher l'état, les véhicules, les bateaux, la file d'attente
}
