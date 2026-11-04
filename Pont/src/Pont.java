/**
 * Moniteur central du pont (PARTIE A).
 * Gère l'état du pont, les N segments et l'accès des véhicules terrestres.
 * Toutes les méthodes publiques sont thread-safe (synchronized/wait/notifyAll
 * ou ReentrantLock + Condition).
 */
public class Pont {

    private final int nbSegments;
    private EtatPont etat = EtatPont.FERME;

    // TODO : occupation des segments, dans chaque sens (ex: Vehicule[] gd, dg)
    // TODO : entrées bloquées par sens (convoi exceptionnel / ouverture)
    // TODO : indicateur de priorité en cours (véhicule prioritaire)

    public Pont(int nbSegments) {
        this.nbSegments = nbSegments;
    }

    // ---------- Véhicules terrestres (A) ----------

    /** Bloque tant que l'entrée n'est pas autorisée, puis réserve les premiers segments. */
    public void entrer(Vehicule v) throws InterruptedException {
        // TODO
    }

    /** Avance d'un cran : réserve les segments suivants (atomiquement), puis libère les anciens. */
    public void avancer(Vehicule v) throws InterruptedException {
        // TODO
    }

    /** Libère les derniers segments : le véhicule a complètement quitté le pont. */
    public void sortir(Vehicule v) {
        // TODO
    }

    // ---------- Véhicules prioritaires (A) ----------

    /** Un prioritaire arrive : les autres véhicules se rangent sur la bande d'urgence. */
    public void debutPriorite(VehiculePrioritaire p) throws InterruptedException {
        // TODO
    }

    /** Le prioritaire a quitté le pont : les véhicules interrompus reprennent. */
    public void finPriorite(VehiculePrioritaire p) {
        // TODO
    }

    /** Appelé par un véhicule normal entre deux déplacements : se met en pause si un prioritaire est actif. */
    public void attendreFinPriorite(Vehicule v) throws InterruptedException {
        // TODO
    }

    // ---------- Interface avec les bateaux (A fournit, B appelle) ----------

    /** @return l'état courant du pont. */
    public synchronized EtatPont getEtat() {
        return etat;
    }

    /** @return true si aucun véhicule n'occupe le pont. */
    public synchronized boolean estVide() {
        // TODO
        return true;
    }

    /**
     * Bloque les entrées, attend que le pont soit vide, puis passe
     * FERME -> OUVERTURE -> OUVERT. Retourne quand le pont est OUVERT.
     */
    public void ouvrir() throws InterruptedException {
        // TODO
    }

    /** OUVERT -> FERMETURE -> FERME, puis réautorise les entrées. */
    public void fermer() throws InterruptedException {
        // TODO
    }

    public int getNbSegments() {
        return nbSegments;
    }
}
