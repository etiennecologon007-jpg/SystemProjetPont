package general;
/** Bateau : thread qui s'enregistre dans la file FIFO puis traverse sous le pont (PARTIE B). */
public class Bateau extends Thread {

	private final int id;
	private final DirectionBateau direction;
	private final GestionnaireBateaux gestionnaire;

	public Bateau(int id, DirectionBateau direction, GestionnaireBateaux gestionnaire) {
		this.id = id;
		this.direction = direction;
		this.gestionnaire = gestionnaire;
	}

	/** Cycle de vie : s'enregistrer, attendre son tour (FIFO + pont OUVERT), traverser, signaler la fin. */
	@Override
	public void run() {
		try {
			gestionnaire.enregistrer(this);
			gestionnaire.attendreTour(this);
			System.out.println(">>> Bateau " + id + " (" + direction + ") traverse");
			Thread.sleep(1500); // durée de traversée, hors de tout bloc synchronized
			gestionnaire.finTraversee(this);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}