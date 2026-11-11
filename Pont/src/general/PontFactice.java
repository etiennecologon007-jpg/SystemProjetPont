package general;

/** Pont de test pour la partie B : simule ouvrir()/fermer() sans la partie A. À retirer à l'intégration. */
public class PontFactice extends Pont {

    private EtatPont etat = EtatPont.FERME;

    public PontFactice() { super(8); }

    private synchronized void setEtat(EtatPont e) {
        etat = e;
        Affichage.log("*** PONT : " + e);
    }

    @Override public synchronized EtatPont getEtat() { return etat; }

    @Override public void ouvrir() throws InterruptedException {
        setEtat(EtatPont.OUVERTURE);
        Thread.sleep(1500);
        setEtat(EtatPont.OUVERT);
    }

    @Override public void fermer() throws InterruptedException {
        setEtat(EtatPont.FERMETURE);
        Thread.sleep(1500);
        setEtat(EtatPont.FERME);
    }
}