package general;

/**
 * Test étape 3 (délai de 5 s entre deux ouvertures) :
 * - bateaux 1 et 2 : première ouverture immédiate
 * - bateau 3 arrive pont fermé, MAIS avant la fin du délai : il doit attendre le Timer
 * - bateau 4 arrive bien plus tard : ouverture dès son arrivée (délai déjà écoulé)
 */
public class MainTestBateaux {
    public static void main(String[] args) throws InterruptedException {
        PontFactice pont = new PontFactice();
        GestionnaireBateaux g = new GestionnaireBateaux(pont, 5_000);
        DirectionBateau[] dirs = { DirectionBateau.NORD_SUD, DirectionBateau.SUD_NORD };

        new Bateau(1, dirs[0], g).start();
        Thread.sleep(300);
        new Bateau(2, dirs[1], g).start();

        Thread.sleep(6000);                 // t ~ 6,3 s : pont fermé depuis ~1 s, délai pas écoulé
        new Bateau(3, dirs[0], g).start();

        Thread.sleep(14000);                // t ~ 20 s : tout est fini, délai écoulé
        new Bateau(4, dirs[1], g).start();
        Thread.sleep(5000);
    }
}