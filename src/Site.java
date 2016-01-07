/**
 * Auteurs	: Marc Pellet et David Villa
 * Labo 	: 03 - PRR
 * File		: Site.java
 * 
 * DESCRIPTION
 * 
 * Classe d�finissant un site. 
 * 
 * Cette derni�re contient la liste des autres sites contenu dans l'environnement afin de pouvoir
 * communiquer avec ces derniers.
 * 
 * Un site poss�de une couche applicative (ApplicationThread) qui s'occupe des t�ches du site.
 * 
 * Un site poss�de un gestionnaire d'�lection (ElectionThread) qui s'occupe de g�rer les �lections
 * au sein des sites de l'environnement et contient les informations sur l'�lu courant.
 * 
 * Chaque site poss�de une aptitude qui lui est propre et qui est calcul�e de la mani�re suivante :
 * aptitude site i = dernier byte de son adresse IP + port utilis�
 * 
 * Lorsqu'un site est d�marr�, il instancie sa couche applicative et son gestionnaire d'�lection et 
 * les d�marre.
 */

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javafx.util.Pair;


public class Site extends Thread {
	private ApplicationThread app;	 // couche applicative executant les taches du site
	private Thread threadApp;
	private ElectionThread election; // gestionnaire d'�lection
	private Thread threadElection;
	private int id; 
	private int port; 
	private InetAddress addr;
	private List< Pair<InetAddress, Integer>> sites; // liste des sites de l'environnement, l'id du site correspond � son indice dans la liste
	private int aptitude;
	private final int T; // temps max d�fini pour transmettre un message
	
	/**
	 * Constructeur permettant d'instancier un site. Un site est d�finit par une id qui lui 
	 * est propre.
	 * 
	 * @param id, identifiant unique du site
	 * @param port, port d'�coute utilis�
	 * @param addr, InetAddress associ�e au site
	 * @param T, temps max d�fini pour la transmission d'un message
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
	 * M�thode permettant d'initialiser un site. Pour qu'un site puisse �tre actif,
	 * il est n�cessaire qu'il ait connaissance d'autres autres sites de l'environnement.
	 * 
	 * L'ID d'un site correspond � son indice dans la liste. 
	 * 
	 * @param sites, liste de Pair permettant de communiquer avec les autres sites.
	 */
	public void init(List<Pair<InetAddress, Integer>> sites){
		this.sites = sites;
	}
	
	/**
	 * M�thode retournant l'aptitude du site.
	 * 
	 * @return l'aptitude du site
	 */
	public int getAptitude() {
		int aptitude = addr.getAddress()[3] + port;
		return aptitude;
	}

	/**
	 * M�thode permettant de d�marrer un site. Lorsqu'un site d�marre, il instancie et 
	 * d�marre un thread contenant la couche applicative ainsi que pour le gestionnaire 
	 * d'�lection.
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
	 * M�thode permettant de faire tomber en panne un site. Cette derni�re s'occupe de kill
	 * les threads internes (couche applicative et gestionnaire).
	 */
	public void panne() {
		// on se permet d'utiliser cette methode d�pr�ci� car c'est le propre d'une panne
		// que de ne pas se fermer de mani�re s�curitaire
		threadApp.stop();
		threadElection.stop();
	}

	/**
	 * M�thode permettant d'arr�ter un site de mani�re s�curitaire.
	 */
	public void safeStop() {
		app.setRunning(false);
		election.setRunning(false);
	}
}
