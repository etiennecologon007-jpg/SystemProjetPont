package general;

import java.util.Random;

/**
 * Point d'entrée de la simulation : pont à 8 segments, véhicules et bateaux générés au hasard.
 * Usage : java -cp bin general.Main [delai_entre_ouvertures_en_secondes]   (défaut : 20 s ; sujet : 60 s)
 * Ctrl+C pour arrêter.
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        long delaiMs = (args.length > 0 ? Long.parseLong(args[0]) : 20) * 1000;

        Pont pont = new Pont(8);
        GestionnaireBateaux gestionnaire = new GestionnaireBateaux(pont, delaiMs);
        Affichage.demarrer(pont, gestionnaire);

        Random rnd = new Random();
        int idVehicule = 0, idBateau = 0;
        long prochainBateau = System.currentTimeMillis() + 4000;

        while (true) {
            Direction d = rnd.nextBoolean() ? Direction.GAUCHE_DROITE : Direction.DROITE_GAUCHE;
            int tirage = rnd.nextInt(100);
            Vehicule v;
            if (tirage < 55)      v = new Voiture(++idVehicule, d, pont);
            else if (tirage < 80) v = new Camion(++idVehicule, d, pont);
            else if (tirage < 90) v = new ConvoiExceptionnel(++idVehicule, d, pont);
            else v = new VehiculePrioritaire(++idVehicule, d, pont,
                    rnd.nextBoolean() ? VehiculePrioritaire.Type.AMBULANCE : VehiculePrioritaire.Type.POMPIERS);
            v.setDaemon(true);
            v.start();

            if (System.currentTimeMillis() >= prochainBateau) {
                DirectionBateau db = rnd.nextBoolean() ? DirectionBateau.NORD_SUD : DirectionBateau.SUD_NORD;
                Bateau b = new Bateau(++idBateau, db, gestionnaire);
                b.setDaemon(true);
                b.start();
                prochainBateau = System.currentTimeMillis() + 6000 + rnd.nextInt(8000);
            }
            Thread.sleep(600 + rnd.nextInt(1800));
        }
    }
}
