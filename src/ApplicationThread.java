
public class ApplicationThread implements Runnable {
	private ElectionThread gestionnaire;
	private boolean running;
	
	
	public ApplicationThread(ElectionThread gestionnaire){
		this.gestionnaire = gestionnaire;
		running = true;
	}

	@Override
	public void run() {
		Site elu;
		while(running){
			try {
				// on cherche a atteindre l'elu toute les 10 secondes
				Thread.sleep(10000);
				elu = gestionnaire.getElu();
				// deux cas, soit il existe un elu
				if (elu != null){
					// on debute la communication avec l'elu avec timeout en cas de panne de l'elu
					communicationAvecElu();
				} // soit aucun elu existant
				else {
					// election deja en cours
					if (gestionnaire.isElectionEnCours()){
						// timeout en cas de panne durant l'election
					} // on débute une élection 
					else {
						// tiemout en cas de panne durant l'election
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private boolean communicationAvecElu(){
		return true;
	}
	
	public void setRunning(boolean running){
		this.running = running;
	}
	
	
}
