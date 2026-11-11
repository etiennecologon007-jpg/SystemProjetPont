package general;

import java.util.Random;

/** Démo de la partie B : le pont (factice), l'affichage animé et des bateaux qui arrivent au hasard. */
public class MainBateaux {
    public static void main(String[] args) throws InterruptedException {
        PontFactice pont = new PontFactice();
        GestionnaireBateaux g = new GestionnaireBateaux(pont, 10_000); // 60_000 pour le rendu final
        Affichage.demarrer(pont, g);

        Random rnd = new Random();
        for (int i = 1; i <= 8; i++) {
            DirectionBateau d = rnd.nextBoolean() ? DirectionBateau.NORD_SUD : DirectionBateau.SUD_NORD;
            new Bateau(i, d, g).start();
            Thread.sleep(1000 + rnd.nextInt(3000)); // arrivées espacées de 1 à 4 s
        }
        Thread.sleep(40_000); // laisse la simulation se terminer
    }
}