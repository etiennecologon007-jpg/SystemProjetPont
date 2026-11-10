package general;

/** Pont de test pour la partie B : simule ouvrir()/fermer() sans la partie A. À retirer à l'intégration. */
public class PontFactice extends Pont {

    private static final long DEBUT = System.currentTimeMillis();
    private EtatPont etat = EtatPont.FERME;

    public PontFactice() { super(8); }

    private synchronized void setEtat(EtatPont e) {
        etat = e;
        System.out.printf("*** [t=%4.1fs] PONT : %s%n", (System.currentTimeMillis() - DEBUT) / 1000.0, e);
    }

    @Override public synchronized EtatPont getEtat() { return etat; }

    @Override public void ouvrir() throws InterruptedException {
        setEtat(EtatPont.OUVERTURE);
        Thread.sleep(1000);
        setEtat(EtatPont.OUVERT);
    }

    @Override public void fermer() throws InterruptedException {
        setEtat(EtatPont.FERMETURE);
        Thread.sleep(1000);
        setEtat(EtatPont.FERME);
    }
}