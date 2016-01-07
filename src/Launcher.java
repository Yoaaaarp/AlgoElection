import java.net.UnknownHostException;


public class Launcher {

	public static void main(String[] args) {
		System.out.println("Debut programme");
		
		boolean panneEnCours = false;
		String[] addrNames = {"localhost", "localhost", "localhost", "localhost"};
		Environnement env = new Environnement();
		
		try {
			env.init(4, addrNames);
			// simuler la panne du site elu (id = 3) apres 20 secondes
			try {
				Thread.sleep(20000);
				env.simulerPanne(3);
				panneEnCours = true;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// simuler la reprise du site en panne après 10 secondes
			try {
				Thread.sleep(10000);
				if (panneEnCours){
					env.reprisePanne(3);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}		
		
		env.arretTotal();
		System.out.println("Fin programme");
	}

}
