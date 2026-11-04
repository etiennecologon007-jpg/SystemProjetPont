package general;
/** Point d'entrée : crée le pont et lance la simulation (PARTIE B). */
public class Main {
    public static void main(String[] args) {
        Pont pont = new Pont(8); // N segments
        GestionnaireBateaux gestionnaire = new GestionnaireBateaux(pont, 60_000);

        // TODO : générer des véhicules (voitures, camions, convois, prioritaires) et des bateaux
        //        avec des délais d'arrivée aléatoires, puis les démarrer (start()).
    }
}
