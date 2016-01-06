
public class ApplicationThread implements Runnable {
	
	ElectionThread gestionnaire;
	
	
	public ApplicationThread(ElectionThread gestionnaire){
		this.gestionnaire = gestionnaire;
	}

	@Override
	public void run() {
		if(gestionnaire.isElectionEnCours()){
			//TODO signal.wait;
		}
		Site elu;
		while(true){
			elu = gestionnaire.getElu();
			if(!communiquerAvecElu()){
				gestionnaire.trouverNouvelElu();
			}
		}
		
	}
	
	private boolean communiquerAvecElu(){
		return true;
	}
	
	
}
