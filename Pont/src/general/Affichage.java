package general;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Affichage console animé avec séquences ANSI.
 * Un thread rafraîchit l'écran toutes les 100 ms en LISANT l'état du pont et du gestionnaire de bateaux.
 * Les autres classes n'écrivent que dans le journal, avec log().
 * À lancer dans un vrai terminal (la console d'Eclipse n'interprète pas les séquences ANSI) de 30 lignes minimum.
 */
public class Affichage {

    private static final int HAUTEUR_CANAL = 9;    // lignes du canal où naviguent les bateaux (0 = nord)
    private static final int LIGNE_VOIE_GD = 3;    // voie gauche -> droite
    private static final int LIGNE_VOIE_DG = 4;    // voie droite -> gauche
    private static final int LIGNE_URGENCE = 5;    // bande d'urgence (prioritaires)
    private static final int NB_LIGNES_JOURNAL = 5;
    private static final String VIDE = "[ . ]";    // une cellule fait toujours 5 caractères

    private static final LinkedList<String> journal = new LinkedList<>();

    // Verrou propre au journal. IMPORTANT : on ne tient JAMAIS un verrou de l'Affichage pendant qu'on lit
    // le pont ou le gestionnaire, sinon deadlock (log() est appelé depuis des méthodes synchronized
    // du pont et du gestionnaire, qui prennent leur verrou avant celui du journal).
    private static final Object VERROU_JOURNAL = new Object();

    /** Ajoute un message au journal affiché en bas de l'écran (thread-safe). */
    public static void log(String message) {
        synchronized (VERROU_JOURNAL) {
            journal.add(message);
            while (journal.size() > NB_LIGNES_JOURNAL) journal.removeFirst();
        }
    }

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

    /** Démarre le thread de rafraîchissement (daemon). */
    public static void demarrer(Pont pont, GestionnaireBateaux gestionnaire) {
        effacerEcran();
        Thread t = new Thread(() -> {
            while (true) {
                rafraichir(pont, gestionnaire);
                try { Thread.sleep(100); } catch (InterruptedException e) { return; }
            }
        }, "affichage");
        t.setDaemon(true);
        t.start();
    }

    /** Construit tout l'écran dans un StringBuilder puis l'écrit d'un seul coup (évite le scintillement). */
    private static void rafraichir(Pont pont, GestionnaireBateaux g) {
        // 1) Lecture de l'état : chaque lecture prend puis relâche son propre verrou, sans verrou d'Affichage
        Pont.Instantane s = pont.getInstantane();
        EtatPont etat = s.etat;
        int n = pont.getNbSegments();
        Bateau enCours = g.getBateauEnTraversee();
        long debut = g.getDebutTraversee();
        List<Bateau> file = g.getFile();
        boolean autorisee = g.isOuvertureAutorisee();
        long heureProchaine = g.getHeureProchaineOuverture();
        List<String> lignesJournal;
        synchronized (VERROU_JOURNAL) { lignesJournal = new ArrayList<>(journal); }

        StringBuilder sb = new StringBuilder("\033[H");
        ligne(sb, "=== SIMULATION PONT MOBILE ===");
        ligne(sb, "Etat du pont : " + couleur(etat) + etat + "\033[0m");
        ligne(sb, "");

        // Position du bateau dans le canal (0 = nord, HAUTEUR-1 = sud)
        int rangBateau = -1;
        if (enCours != null) {
            double avancement = (System.currentTimeMillis() - debut) / (double) Bateau.DUREE_TRAVERSEE_MS;
            avancement = Math.max(0, Math.min(1, avancement));
            int r = (int) Math.round(avancement * (HAUTEUR_CANAL - 1));
            rangBateau = (enCours.getDirection() == DirectionBateau.NORD_SUD) ? r : HAUTEUR_CANAL - 1 - r;
        }
        String etiquette = (enCours != null) ? "B" + (enCours.getBateauId() % 10) : "  ";
        int decalage = 3 + (n / 2) * VIDE.length(); // colonne du canal (au milieu du pont)
        int largeurTrou = trou(etat, null).length();

        ligne(sb, " ".repeat(decalage) + "NORD");
        for (int r = 0; r < HAUTEUR_CANAL; r++) {
            String t = trou(etat, r == rangBateau ? etiquette : null);
            if (r == LIGNE_VOIE_GD) {
                ligne(sb, ligneVoie(s, 0, n, t));
            } else if (r == LIGNE_VOIE_DG) {
                ligne(sb, ligneVoie(s, 1, n, t));
            } else if (r == LIGNE_URGENCE) {
                ligne(sb, ligneUrgence(s, n, t, largeurTrou));
            } else if (r == rangBateau) {
                ligne(sb, " ".repeat(decalage) + "<" + etiquette + ">");
            } else {
                ligne(sb, " ".repeat(decalage) + " | ");
            }
        }
        ligne(sb, " ".repeat(decalage) + "SUD");
        ligne(sb, "");

        ligne(sb, "Entree en attente   : G>D " + s.enAttente[0] + " vehicule(s)   D>G " + s.enAttente[1]
                + " vehicule(s)   (prioritaires annonces : " + (s.prioEnAttente[0] + s.prioEnAttente[1]) + ")");
        ligne(sb, "Bateau en traversee : " + (enCours == null ? "aucun"
                : "#" + enCours.getBateauId() + " (" + enCours.getDirection() + ")"));
        StringBuilder f = new StringBuilder();
        for (Bateau b : file) f.append('#').append(b.getBateauId()).append(' ');
        ligne(sb, "File des bateaux    : " + (file.isEmpty() ? "vide" : f));
        String prochaine;
        if (autorisee) prochaine = "autorisee";
        else if (etat != EtatPont.FERME) prochaine = "apres la fermeture du pont";
        else prochaine = "dans " + Math.max(0, (heureProchaine - System.currentTimeMillis()) / 1000 + 1) + " s";
        ligne(sb, "Prochaine ouverture : " + prochaine);
        ligne(sb, "C=voiture T=camion X=convoi A=ambulance P=pompiers   \033[33mjaune\033[0m = a l'arret (urgence)");
        ligne(sb, "-- Journal --");
        for (int i = 0; i < NB_LIGNES_JOURNAL; i++) {
            ligne(sb, i < lignesJournal.size() ? lignesJournal.get(i) : "");
        }

        // 2) Écriture : un seul print
        synchronized (Affichage.class) {
            System.out.print(sb);
            System.out.flush();
        }
    }

    /** Une cellule de 5 caractères : [ . ] si vide, [C12] sinon (en jaune si le véhicule est à l'arrêt). */
    private static String cellule(Vehicule v, boolean pause) {
        if (v == null) return VIDE;
        String c = "[" + v.getSymbole() + String.format("%-2d", v.getVehiculeId() % 100) + "]";
        return pause ? "\033[33m" + c + "\033[0m" : c;
    }

    /**
     * Ligne d'une voie. Les colonnes vont de gauche à droite ; pour le sens D>G, la colonne c correspond
     * à l'indice de parcours n-1-c.
     */
    private static String ligneVoie(Pont.Instantane s, int d, int n, String trou) {
        String fleche = (d == 0) ? ">>>" : "<<<";
        StringBuilder sb = new StringBuilder(fleche);
        for (int c = 0; c < n; c++) {
            if (c == n / 2) sb.append(trou);
            int i = (d == 0) ? c : n - 1 - c;
            sb.append(cellule(s.voies[d][i], s.enPause[d][i]));
        }
        return sb.append(fleche).toString();
    }

    /** Bande d'urgence : montre les prioritaires qui traversent. */
    private static String ligneUrgence(Pont.Instantane s, int n, String trou, int largeurTrou) {
        String[] cases = new String[n];
        java.util.Arrays.fill(cases, " ".repeat(VIDE.length()));
        for (int k = 0; k < s.prioritaires.size(); k++) {
            VehiculePrioritaire p = s.prioritaires.get(k);
            int t = s.positionsPrio[k];
            if (t < 0 || t >= n) continue;
            int d = p.getDirection().ordinal();
            int col = (d == 0) ? t : n - 1 - t;
            cases[col] = "\033[31m[" + p.getSymbole() + String.format("%-2d", p.getVehiculeId() % 100) + "]\033[0m";
        }
        StringBuilder sb = new StringBuilder("URG");
        for (int c = 0; c < n; c++) {
            if (c == n / 2) sb.append(trou.trim().isEmpty() || largeurTrou == 0 ? " ".repeat(largeurTrou) : trou);
            sb.append(cases[c]);
        }
        return sb.toString();
    }

    /** Le "trou" au milieu du pont selon son état ; un bateau peut apparaître dedans quand le pont est ouvert. */
    private static String trou(EtatPont etat, String etiquetteBateau) {
        switch (etat) {
            case OUVERTURE: return "/ ";
            case OUVERT:    return "/" + (etiquetteBateau != null ? " " + etiquetteBateau + " " : "    ") + "\\";
            case FERMETURE: return " \\";
            default:        return "";
        }
    }

    private static void ligne(StringBuilder sb, String s) {
        sb.append(s).append("\033[K\n"); // \033[K : efface la fin de la ligne (reste d'un ancien affichage)
    }

    private static String couleur(EtatPont e) {
        switch (e) {
            case FERME:  return "\033[32m"; // vert
            case OUVERT: return "\033[31m"; // rouge
            default:     return "\033[33m"; // jaune (transition)
        }
    }
}
