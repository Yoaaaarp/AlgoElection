
public class ApplicationThread {
	private final int NB_MAX_SITE = 4; // hypothèse
	
	public ApplicationThread(){
		
	};
	
	public int init(int nbSite){
		// on s'assure que l'on ne crée pas plus de 4 sites
		if (nbSite > NB_MAX_SITE){
			return -1;
		}
		//TODO
		return 0;
	}
	
}
