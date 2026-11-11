package general;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Moniteur central du pont (PARTIE A).
 *
 * MODELE
 *  - Deux voies indépendantes (une par sens) de N segments. voies[d][i] = véhicule occupant le segment i,
 *    où i est l'indice dans l'ORDRE DE PARCOURS du sens d (pour D>G, l'indice 0 est le segment de droite).
 *  - Un véhicule de longueur L occupe L segments consécutifs [tete-L+1 .. tete]. Il les réserve tous ensemble
 *    à l'entrée (atomique, donc pas d'interblocage "j'en tiens un et j'attends l'autre"), puis avance d'un
 *    segment : réservation du suivant, puis libération du dernier. À la sortie il libère ses segments un par un.
 *  - Pas de dépassement : un véhicule ne peut avancer que si le segment devant lui est libre.
 *  - Entrée en FIFO par sens (tickets) : un camion ou un convoi n'est donc jamais affamé par les voitures.
 *
 * REGLES
 *  - Aucun véhicule n'entre si le pont n'est pas FERME ou si une ouverture a été demandée.
 *  - Convoi exceptionnel : dès qu'il est en tête de file, les NOUVELLES entrées du sens opposé sont bloquées ;
 *    il entre quand la voie opposée est vide. Pendant sa traversée, le sens opposé reste bloqué.
 *  - Prioritaire : tant qu'il est annoncé, les nouvelles entrées de son sens sont bloquées ; quand il traverse,
 *    les véhicules de son sens (et un convoi du sens opposé, à cause de sa largeur) s'arrêtent sur la bande
 *    d'urgence en gardant leurs segments, puis reprennent à sa sortie.
 *
 * Toutes les méthodes publiques sont thread-safe (synchronized / wait / notifyAll).
 * Les sleeps (transitions d'état) sont toujours faits HORS du verrou.
 */
public class Pont {

    /** Durée (ms) des transitions OUVERTURE et FERMETURE. Modifiable pour les tests. */
    public static long dureeTransitionMs = 1500;

    private final int nbSegments;
    private EtatPont etat = EtatPont.FERME;
    private boolean ouvertureDemandee = false;

    private final Vehicule[][] voies;                       // [sens][indice de parcours]
    private final int[] vehiculesSurVoie = new int[2];      // véhicules distincts présents par voie
    private final long[] prochainTicket = new long[2];      // FIFO d'entrée par sens
    private final long[] ticketServi = new long[2];
    private final int[] convoisEnAttente = new int[2];      // convois en tête de file qui attendent d'entrer
    private final int[] convoisSurPont = new int[2];
    private final int[] prioEnAttente = new int[2];         // prioritaires annoncés, pas encore sur le pont
    private final int[] prioSurPont = new int[2];
    private final List<VehiculePrioritaire> prioritairesSurPont = new ArrayList<>();

    /** Le pont a besoin d'au moins 4 segments (longueur d'un convoi exceptionnel). */
    public Pont(int nbSegments) {
        if (nbSegments < 4) throw new IllegalArgumentException("Il faut au moins 4 segments");
        this.nbSegments = nbSegments;
        this.voies = new Vehicule[2][nbSegments];
    }

    public int getNbSegments() { return nbSegments; }

    // =====================================================================
    //  Véhicules terrestres
    // =====================================================================

    /** Bloque tant que l'entrée n'est pas autorisée, puis réserve atomiquement les premiers segments. */
    public synchronized void entrer(Vehicule v) throws InterruptedException {
        int d = v.getDirection().ordinal();
        boolean convoi = v instanceof ConvoiExceptionnel;
        long ticket = prochainTicket[d]++;
        boolean annonce = false;

        while (!peutEntrer(v, ticket)) {
            // Un convoi "arrive à l'entrée" quand il est en tête de sa file : il bloque alors le sens opposé.
            if (convoi && !annonce && ticket == ticketServi[d]) {
                annonce = true;
                convoisEnAttente[d]++;
                Affichage.log("Convoi " + v.getVehiculeId() + " (" + v.getDirection().libelle()
                        + ") en attente : sens oppose bloque");
                notifyAll();
            }
            wait();
        }
        if (annonce) convoisEnAttente[d]--;

        int longueur = v.getNbSegmentsOccupes();
        for (int i = 0; i < longueur; i++) voies[d][i] = v;
        v.tete = longueur - 1;
        v.occupes = longueur;
        vehiculesSurVoie[d]++;
        ticketServi[d]++;
        if (convoi) {
            convoisSurPont[d]++;
            Affichage.log("Convoi " + v.getVehiculeId() + " (" + v.getDirection().libelle() + ") s'engage");
        }
        notifyAll();
    }

    /** Conditions d'entrée (appelée sous verrou). */
    private boolean peutEntrer(Vehicule v, long ticket) {
        int d = v.getDirection().ordinal();
        int q = 1 - d; // sens opposé
        if (etat != EtatPont.FERME || ouvertureDemandee) return false;
        if (ticket != ticketServi[d]) return false;                // FIFO dans le sens
        if (prioEnAttente[d] + prioSurPont[d] > 0) return false;   // un prioritaire passe avant
        for (int i = 0; i < v.getNbSegmentsOccupes(); i++) {
            if (voies[d][i] != null) return false;                 // tous les segments nécessaires libres
        }
        if (v instanceof ConvoiExceptionnel) {
            if (convoisSurPont[q] > 0) return false;               // un convoi opposé est déjà engagé
            if (vehiculesSurVoie[q] > 0) return false;             // attendre que le sens opposé soit vidé
            if (prioSurPont[q] > 0) return false;
        } else {
            if (convoisEnAttente[q] > 0 || convoisSurPont[q] > 0) return false; // sens opposé bloqué par un convoi
        }
        return true;
    }

    /**
     * Avance d'un cran. Bloque si un prioritaire traverse (arrêt sur la bande d'urgence) ou si le segment
     * suivant est occupé.
     * @return true si le véhicule est encore sur le pont, false s'il vient de le quitter complètement.
     */
    public synchronized boolean avancer(Vehicule v) throws InterruptedException {
        int d = v.getDirection().ordinal();
        while (estEnPause(v) || (v.tete < nbSegments - 1 && voies[d][v.tete + 1] != null)) {
            wait();
        }
        int queue = v.tete - v.occupes + 1; // indice du segment le plus en arrière
        if (v.tete < nbSegments - 1) {
            voies[d][v.tete + 1] = v;       // 1) réserver le segment suivant...
            voies[d][queue] = null;         // 2) ...puis libérer le dernier
            v.tete++;
        } else {
            voies[d][queue] = null;         // le véhicule sort : il libère ses segments un par un
            v.occupes--;
        }
        boolean toujoursSurPont = v.occupes > 0;
        if (!toujoursSurPont) {
            vehiculesSurVoie[d]--;
            if (v instanceof ConvoiExceptionnel) {
                convoisSurPont[d]--;
                Affichage.log("Convoi " + v.getVehiculeId() + " a quitte le pont");
            }
        }
        notifyAll();
        return toujoursSurPont;
    }

    /** Un véhicule est à l'arrêt si un prioritaire traverse dans son sens (ou, pour un convoi, dans le sens opposé). */
    private boolean estEnPause(Vehicule v) {
        int d = v.getDirection().ordinal();
        return prioSurPont[d] > 0 || (v instanceof ConvoiExceptionnel && prioSurPont[1 - d] > 0);
    }

    // =====================================================================
    //  Véhicules prioritaires
    // =====================================================================

    /** Annonce l'arrivée, attend que le pont soit FERME, puis commence la traversée (les autres s'arrêtent). */
    public synchronized void debutPriorite(VehiculePrioritaire p) throws InterruptedException {
        int d = p.getDirection().ordinal();
        prioEnAttente[d]++;
        Affichage.log("ALERTE " + p.getSymbole() + p.getVehiculeId() + " (" + p.getDirection().libelle() + ") arrive");
        notifyAll();
        while (etat != EtatPont.FERME || ouvertureDemandee) wait();
        prioEnAttente[d]--;
        prioSurPont[d]++;
        prioritairesSurPont.add(p);
        Affichage.log(p.getSymbole() + p.getVehiculeId() + " traverse : vehicules a l'arret");
        notifyAll();
    }

    /** Le prioritaire a quitté le pont : les véhicules interrompus reprennent là où ils s'étaient arrêtés. */
    public synchronized void finPriorite(VehiculePrioritaire p) {
        prioSurPont[p.getDirection().ordinal()]--;
        prioritairesSurPont.remove(p);
        p.tete = -1;
        Affichage.log(p.getSymbole() + p.getVehiculeId() + " a quitte le pont : reprise");
        notifyAll();
    }

    // =====================================================================
    //  Interface avec les bateaux (utilisée par GestionnaireBateaux)
    // =====================================================================

    /** @return l'état courant du pont. */
    public synchronized EtatPont getEtat() { return etat; }

    /** @return true si aucun véhicule (ni prioritaire) n'occupe le pont. */
    public synchronized boolean estVide() {
        return vehiculesSurVoie[0] == 0 && vehiculesSurVoie[1] == 0 && prioSurPont[0] == 0 && prioSurPont[1] == 0;
    }

    /**
     * Bloque les nouvelles entrées, attend que les véhicules engagés aient quitté le pont,
     * puis FERME -> OUVERTURE -> OUVERT. Retourne quand le pont est OUVERT.
     */
    public void ouvrir() throws InterruptedException {
        synchronized (this) {
            ouvertureDemandee = true;
            while (!estVide()) wait();
            etat = EtatPont.OUVERTURE;
            Affichage.log("*** PONT : OUVERTURE");
            notifyAll();
        }
        Thread.sleep(dureeTransitionMs); // hors verrou : le pont reste consultable pendant la transition
        synchronized (this) {
            etat = EtatPont.OUVERT;
            Affichage.log("*** PONT : OUVERT");
            notifyAll();
        }
    }

    /** OUVERT -> FERMETURE -> FERME, puis réautorise les entrées. */
    public void fermer() throws InterruptedException {
        synchronized (this) {
            etat = EtatPont.FERMETURE;
            Affichage.log("*** PONT : FERMETURE");
            notifyAll();
        }
        Thread.sleep(dureeTransitionMs);
        synchronized (this) {
            etat = EtatPont.FERME;
            ouvertureDemandee = false;
            Affichage.log("*** PONT : FERME");
            notifyAll();
        }
    }

    // =====================================================================
    //  Lecture de l'état (affichage) et vérification des invariants (tests)
    // =====================================================================

    /** Photo cohérente de l'état du pont, pour l'affichage. */
    public static class Instantane {
        public final EtatPont etat;
        public final Vehicule[][] voies;      // [sens][indice de parcours]
        public final boolean[][] enPause;     // véhicule à l'arrêt sur la bande d'urgence
        public final List<VehiculePrioritaire> prioritaires;
        public final int[] positionsPrio;     // position (indice de parcours) de chaque prioritaire
        public final int[] enAttente;         // véhicules arrivés mais pas encore engagés, par sens
        public final int[] prioEnAttente;

        Instantane(EtatPont etat, Vehicule[][] voies, boolean[][] enPause, List<VehiculePrioritaire> prioritaires,
                   int[] positionsPrio, int[] enAttente, int[] prioEnAttente) {
            this.etat = etat; this.voies = voies; this.enPause = enPause; this.prioritaires = prioritaires;
            this.positionsPrio = positionsPrio; this.enAttente = enAttente; this.prioEnAttente = prioEnAttente;
        }
    }

    public synchronized Instantane getInstantane() {
        Vehicule[][] v = new Vehicule[2][];
        boolean[][] p = new boolean[2][nbSegments];
        for (int d = 0; d < 2; d++) {
            v[d] = voies[d].clone();
            for (int i = 0; i < nbSegments; i++) p[d][i] = v[d][i] != null && estEnPause(v[d][i]);
        }
        List<VehiculePrioritaire> pr = new ArrayList<>(prioritairesSurPont);
        int[] pos = new int[pr.size()];
        for (int k = 0; k < pos.length; k++) pos[k] = pr.get(k).tete;
        int[] att = { (int) (prochainTicket[0] - ticketServi[0]), (int) (prochainTicket[1] - ticketServi[1]) };
        return new Instantane(etat, v, p, pr, pos, att, new int[] { prioEnAttente[0], prioEnAttente[1] });
    }

    /** @return null si tous les invariants sont respectés, sinon un message décrivant la violation. */
    public synchronized String verifierInvariants() {
        for (int d = 0; d < 2; d++) {
            Set<Vehicule> vus = new HashSet<>();
            int segments = 0;
            for (int i = 0; i < nbSegments; i++) {
                Vehicule v = voies[d][i];
                if (v == null) continue;
                segments++;
                vus.add(v);
                if (v.getDirection().ordinal() != d) return "vehicule " + v.getVehiculeId() + " sur la voie de l'autre sens";
                if (i > v.tete || i < v.tete - v.occupes + 1) return "vehicule " + v.getVehiculeId() + " hors de sa plage de segments";
            }
            int attendu = 0;
            for (Vehicule v : vus) attendu += v.occupes;
            if (segments != attendu) return "nombre de segments occupes incoherent (voie " + d + ")";
            if (vus.size() != vehiculesSurVoie[d]) return "compteur de vehicules incoherent (voie " + d + ")";
            if (convoisSurPont[d] > 0 && vehiculesSurVoie[1 - d] > 0) return "convoi present en meme temps qu'un vehicule du sens oppose";
        }
        if (etat != EtatPont.FERME && !estVide()) return "vehicule sur le pont alors que l'etat est " + etat;
        return null;
    }
}
