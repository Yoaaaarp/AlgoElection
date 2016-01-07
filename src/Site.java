/**
 * Auteurs	: Marc Pellet et David Villa
 * Labo 	: 03 - PRR
 * File		: Site.java
 * 
 * DESCRIPTION
 * 
 * Classe définissant un site. 
 * 
 * Cette dernière contient la liste des autres sites contenu dans l'environnement afin de pouvoir
 * communiquer avec ces derniers.
 * 
 * Un site possède une couche applicative (ApplicationThread) qui s'occupe des tâches du site.
 * 
 * Un site possède un gestionnaire d'élection (ElectionThread) qui s'occupe de gérer les élections
 * au sein des sites de l'environnement et contient les informations sur l'élu courant.
 * 
 * Chaque site possède une aptitude qui lui est propre et qui est calculée de la manière suivante :
 * aptitude site i = dernier byte de son adresse IP + port utilisé
 * 
 * Lorsqu'un site est démarré, il instancie sa couche applicative et son gestionnaire d'élection et 
 * les démarre.
 */

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javafx.util.Pair;


public class Site extends Thread {
	private ApplicationThread app;	 // couche applicative executant les taches du site
	private Thread threadApp;
	private ElectionThread election; // gestionnaire d'élection
	private Thread threadElection;
	private int id; 
	private int port; 
	private InetAddress addr;
	private List< Pair<InetAddress, Integer>> sites; // liste des sites de l'environnement, l'id du site correspond à son indice dans la liste
	private int aptitude;
	private final int T; // temps max défini pour transmettre un message
	
	/**
	 * Constructeur permettant d'instancier un site. Un site est définit par une id qui lui 
	 * est propre.
	 * 
	 * @param id, identifiant unique du site
	 * @param port, port d'écoute utilisé
	 * @param addr, InetAddress associée au site
	 * @param T, temps max défini pour la transmission d'un message
	 */
	public Site(int id, int port, InetAddress addr, int T){
		this.T = T;
		this.id = id;
		this.port = port;
		this.addr = addr;
		aptitude = 0;
		for(byte b : addr.getAddress()){
			aptitude *= 10;
			aptitude += (int)b;
		}
		aptitude += port;
	}
	
	/**
	 * Méthode permettant d'initialiser un site. Pour qu'un site puisse être actif,
	 * il est nécessaire qu'il ait connaissance d'autres autres sites de l'environnement.
	 * 
	 * L'ID d'un site correspond à son indice dans la liste. 
	 * 
	 * @param sites, liste de Pair permettant de communiquer avec les autres sites.
	 */
	public void init(List<Pair<InetAddress, Integer>> sites){
		this.sites = sites;
	}
	
	/**
	 * Méthode retournant l'aptitude du site.
	 * 
	 * @return l'aptitude du site
	 */
	public int getAptitude() {
		int aptitude = addr.getAddress()[3] + port;
		return aptitude;
	}

	/**
	 * Méthode permettant de démarrer un site. Lorsqu'un site démarre, il instancie et 
	 * démarre un thread contenant la couche applicative ainsi que pour le gestionnaire 
	 * d'élection.
	 */
	@Override
	public void run() {
		election = new ElectionThread(id, aptitude, sites);
		app = new ApplicationThread(election, sites.get(id), T, sites.size());
		threadApp = new Thread(app);
		threadApp.start();
		threadElection = new Thread(election);
		threadElection.start();
	}
	
	/**
	 * Méthode permettant de faire tomber en panne un site. Cette dernière s'occupe de kill
	 * les threads internes (couche applicative et gestionnaire).
	 */
	public void panne() {
		// on se permet d'utiliser cette methode déprécié car c'est le propre d'une panne
		// que de ne pas se fermer de manière sécuritaire
		threadApp.stop();
		threadElection.stop();
	}

	/**
	 * Méthode permettant d'arrêter un site de manière sécuritaire.
	 */
	public void safeStop() {
		app.setRunning(false);
		election.setRunning(false);
	}
}
