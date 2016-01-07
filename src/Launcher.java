/**
 * Auteurs	: Marc Pellet et David Villa
 * Labo		: 03 - PRR
 * File		: Launcher.java
 * 
 * DESCRIPTION :
 * 
 * Classe principale du programme
 * 
 * Cette dernière s'occupe de créer l'environnement contenant nos sites et de demander la création 
 * de 4 sites au sein de cet environnement.
 * 
 * Après 20 secondes, la classe demande à l'environnement de simuler la panne du site elu.
 * Encore 20 secondes plus tard, la classe demande à l'environnement de simuler la reprise du site en panne.
 * Finalement 20 secondes plus tard, la classe demande à l'environnement d'arrêter les sites.
 * 
 */

import java.net.UnknownHostException;


public class Launcher {

	public static void main(String[] args) {
		System.out.println("Debut programme");

		boolean panneEnCours = false;
		String[] addrNames = {"localhost", "localhost", "localhost", "localhost"};
		Environnement env = new Environnement();

		try {
			int result = env.init(4, addrNames);

			if (result == 1){
				// simuler la panne du site elu (id = 3) apres 20 secondes
				try {
					Thread.sleep(20000);
					panneEnCours = env.simulerPanne(3);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// simuler la reprise du site en panne après 10 secondes
				Thread.sleep(20000);
				if (panneEnCours){
					env.reprisePanne(3);
				}

				Thread.sleep(20000);
				env.arretTotal();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}	

		System.out.println("Fin programme");
	}

}
