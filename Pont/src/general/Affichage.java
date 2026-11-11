package general;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Affichage console animé avec séquences ANSI (PARTIE B).
 * Un thread rafraîchit l'écran toutes les 100 ms en LISANT l'état (pont + gestionnaire) :
 * aucune autre classe n'a besoin d'appeler l'affichage, sauf pour écrire dans le journal avec log().
 * À lancer dans un vrai terminal (la console d'Eclipse n'interprète pas les séquences ANSI).
 */
public class Affichage {

    private static final int HAUTEUR_CANAL = 7;   // lignes du canal où naviguent les bateaux
    private static final int LIGNE_PONT = 3;      // ligne (0..6) où passe la route du pont
    private static final int NB_LIGNES_JOURNAL = 6;
    private static final LinkedList<String> journal = new LinkedList<>();

    // Verrou propre au journal. IMPORTANT : on ne tient JAMAIS un verrou de l'Affichage pendant qu'on lit
    // le pont ou le gestionnaire, sinon deadlock (log() est appelé depuis des méthodes synchronized
    // du pont et du gestionnaire, qui prennent leur verrou avant celui du journal).
    private static final Object VERROU_JOURNAL = new Object();

    /** Ajoute un message au journal affiché en bas de l'écran (thread-safe, ne prend que le verrou du journal). */
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
        // 1) Lecture de l'état : chaque getter prend puis relâche son propre verrou, sans verrou d'Affichage
        EtatPont etat = pont.getEtat();
        Bateau enCours = g.getBateauEnTraversee();
        long debut = g.getDebutTraversee();
        List<Bateau> file = g.getFile();
        boolean autorisee = g.isOuvertureAutorisee();
        long heureProchaine = g.getHeureProchaineOuverture();
        int n = pont.getNbSegments();
        List<String> lignesJournal;
        synchronized (VERROU_JOURNAL) { lignesJournal = new ArrayList<>(journal); }

        StringBuilder sb = new StringBuilder("\033[H"); // curseur en haut à gauche
        ligne(sb, "=== SIMULATION PONT MOBILE ===");
        ligne(sb, "Etat du pont : " + couleur(etat) + etat + "\033[0m");
        ligne(sb, "");

        // Position du bateau dans le canal (0 = nord, HAUTEUR-1 = sud)
        int rangBateau = -1;
        if (enCours != null) {
            double avancement = (System.currentTimeMillis() - debut)
                    / (double) Bateau.DUREE_TRAVERSEE_MS;
            avancement = Math.max(0, Math.min(1, avancement));
            int r = (int) Math.round(avancement * (HAUTEUR_CANAL - 1));
            rangBateau = (enCours.getDirection() == DirectionBateau.NORD_SUD) ? r : HAUTEUR_CANAL - 1 - r;
        }

        // Route : ===[S1]...[Sn/2] <trou> [S..]...[Sn]===
        StringBuilder gauche = new StringBuilder("===");
        for (int i = 1; i <= n / 2; i++) gauche.append("[S").append(i).append("]");
        StringBuilder droite = new StringBuilder();
        for (int i = n / 2 + 1; i <= n; i++) droite.append("[S").append(i).append("]");
        droite.append("===");
        int decalage = gauche.length();
        String etiquette = (enCours != null) ? "B" + enCours.getBateauId() : "  ";
        String trou;
        switch (etat) {
            case OUVERTURE: trou = "/ "; break;
            case OUVERT:    trou = "/" + (rangBateau == LIGNE_PONT ? " " + etiquette + " " : "    ") + "\\"; break;
            case FERMETURE: trou = " \\"; break;
            default:        trou = ""; break;
        }

        ligne(sb, " ".repeat(decalage) + "NORD");
        for (int r = 0; r < HAUTEUR_CANAL; r++) {
            if (r == LIGNE_PONT) {
                ligne(sb, gauche + trou + droite);
            } else if (r == rangBateau) {
                ligne(sb, " ".repeat(decalage) + "<" + etiquette + ">");
            } else {
                ligne(sb, " ".repeat(decalage) + " | ");
            }
        }
        ligne(sb, " ".repeat(decalage) + "SUD");
        ligne(sb, "");

        // Informations
        ligne(sb, "Bateau en traversee : " + (enCours == null ? "aucun"
                : "#" + enCours.getBateauId() + " (" + enCours.getDirection() + ")"));
        StringBuilder f = new StringBuilder();
        for (Bateau b : file) f.append('#').append(b.getBateauId()).append(' ');
        ligne(sb, "File d'attente      : " + (file.isEmpty() ? "vide" : f));
        long reste = heureProchaine - System.currentTimeMillis();
        String prochaine;
        if (autorisee) prochaine = "autorisee";
        else if (etat != EtatPont.FERME) prochaine = "apres la fermeture du pont";
        else prochaine = "dans " + Math.max(0, reste / 1000 + 1) + " s";
        ligne(sb, "Prochaine ouverture : " + prochaine);
        ligne(sb, "");

        // TODO (avec la partie A) : afficher ici les véhicules sur les segments S1..Sn

        ligne(sb, "-- Journal --");
        for (int i = 0; i < NB_LIGNES_JOURNAL; i++) {
            ligne(sb, i < lignesJournal.size() ? lignesJournal.get(i) : "");
        }
        // 2) Écriture : un seul print, sans tenir d'autre verrou
        synchronized (Affichage.class) {
            System.out.print(sb);
            System.out.flush();
        }
    }

    private static void ligne(StringBuilder sb, String s) {
        sb.append(s).append("\033[K\n"); // \033[K : efface la fin de la ligne (reste d'un ancien affichage)
    }

    private static String couleur(EtatPont e) {
        switch (e) {
            case FERME:     return "\033[32m"; // vert
            case OUVERT:    return "\033[31m"; // rouge
            default:        return "\033[33m"; // jaune (transition)
        }
    }
}