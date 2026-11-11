package general;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test de robustesse (sans affichage) : simulation accélérée avec un thread vérificateur qui contrôle
 * les invariants du pont toutes les millisecondes, puis vérifie que tous les threads terminent
 * (pas d'interblocage ni de famine).
 * Usage : java -cp bin general.VerifPont [duree_en_secondes]   (défaut : 25)
 */
public class VerifPont {

    public static void main(String[] args) throws Exception {
        int dureeS = args.length > 0 ? Integer.parseInt(args[0]) : 25;
        Vehicule.pasMs = 12;
        Pont.dureeTransitionMs = 80;
        Bateau.DUREE_TRAVERSEE_MS = 80;

        Pont pont = new Pont(8);
        GestionnaireBateaux g = new GestionnaireBateaux(pont, 500);

        AtomicReference<String> erreur = new AtomicReference<>();
        Set<EtatPont> etatsVus = ConcurrentHashMap.newKeySet();
        AtomicInteger maxSurPont = new AtomicInteger();
        AtomicInteger pausesVues = new AtomicInteger();

        Thread verificateur = new Thread(() -> {
            while (true) {
                String msg = pont.verifierInvariants();
                if (msg != null) erreur.compareAndSet(null, msg);
                Pont.Instantane s = pont.getInstantane();
                etatsVus.add(s.etat);
                Set<Vehicule> distincts = new java.util.HashSet<>();
                for (int d = 0; d < 2; d++) for (int i = 0; i < s.voies[d].length; i++) {
                    if (s.voies[d][i] != null) distincts.add(s.voies[d][i]);
                    if (s.enPause[d][i]) pausesVues.incrementAndGet();
                }
                maxSurPont.accumulateAndGet(distincts.size(), Math::max);
                try { Thread.sleep(1); } catch (InterruptedException e) { return; }
            }
        });
        verificateur.setDaemon(true);
        verificateur.start();

        Random rnd = new Random();
        List<Thread> threads = new ArrayList<>();
        int[] nb = new int[5]; // voitures, camions, convois, prioritaires, bateaux
        int id = 0, idB = 0;
        long fin = System.currentTimeMillis() + dureeS * 1000L;
        long prochainBateau = 0;
        while (System.currentTimeMillis() < fin && erreur.get() == null) {
            Direction d = rnd.nextBoolean() ? Direction.GAUCHE_DROITE : Direction.DROITE_GAUCHE;
            int t = rnd.nextInt(100);
            Thread v;
            if (t < 45)      { v = new Voiture(++id, d, pont); nb[0]++; }
            else if (t < 70) { v = new Camion(++id, d, pont); nb[1]++; }
            else if (t < 84) { v = new ConvoiExceptionnel(++id, d, pont); nb[2]++; }
            else             { v = new VehiculePrioritaire(++id, d, pont,
                                   rnd.nextBoolean() ? VehiculePrioritaire.Type.AMBULANCE : VehiculePrioritaire.Type.POMPIERS); nb[3]++; }
            v.start();
            threads.add(v);
            if (System.currentTimeMillis() >= prochainBateau) {
                Bateau b = new Bateau(++idB, rnd.nextBoolean() ? DirectionBateau.NORD_SUD : DirectionBateau.SUD_NORD, g);
                b.start(); threads.add(b); nb[4]++;
                prochainBateau = System.currentTimeMillis() + 300 + rnd.nextInt(700);
            }
            Thread.sleep(5 + rnd.nextInt(35));
        }

        // Tous les threads doivent finir : sinon interblocage ou famine
        long limite = System.currentTimeMillis() + 40_000;
        int bloques = 0;
        for (Thread th : threads) {
            th.join(Math.max(1, limite - System.currentTimeMillis()));
            if (th.isAlive()) bloques++;
        }

        System.out.println("Voitures : " + nb[0] + " | Camions : " + nb[1] + " | Convois : " + nb[2]
                + " | Prioritaires : " + nb[3] + " | Bateaux : " + nb[4]);
        System.out.println("Etats du pont observes : " + etatsVus);
        System.out.println("Max de vehicules simultanes sur le pont : " + maxSurPont.get()
                + " | observations d'arret sur bande d'urgence : " + pausesVues.get());
        if (erreur.get() != null) {
            System.out.println("ECHEC invariant : " + erreur.get());
            System.exit(1);
        }
        if (bloques > 0) {
            System.out.println("ECHEC : " + bloques + " thread(s) bloque(s) (interblocage ou famine)");
            for (Thread th : threads) if (th.isAlive()) System.out.println("  " + th.getClass().getSimpleName() + " " + th.getState());
            System.exit(1);
        }
        System.out.println("OK : invariants respectes, tous les threads ont termine.");
    }
}
