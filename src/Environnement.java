/**
 * Auteurs	: Marc Pellet et David Villa
 * Labo		: 03 - PRR
 * File 	: Environnement.java
 * 
 * DESCRIPTION :
 * 
 * Cette classe a pour rôle de simuler un environnement. L'environnement contient les différents sites
 * lui appartenant.
 * 
 * Lorsque l'on fait appel à la méthode d'initialisation, l'environnement s'occupe de créer le nombre
 * de site requis et de leur fournir la liste de leur voisin afin qu'ils puissent communiquer entre eux.
 * 
 * De plus l'environnement permet de simuler des pannes ou des reprises de ses différents sites à l'aide
 * de l'id les identifiant. 
 * 
 * Finalement l'environnement propose une méthode permettant de stopper ses sites de manières sécuritaire.
 */

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javafx.util.Pair;


public class Environnement {
	private final int NB_MAX_SITE = 4; // hypothèse 1
	private List< Pair<InetAddress, Integer>> sites; // tableau liant un site (InetAddress/port) à l'ID (indice)
	private int idSite;
	private int port;
	private List<Site> vm; // chaque site est un thread permettant ainsi de simuler une VM

	/**
	 * Constructeur vide de notre environnement. 
	 * Il défini une valeur de port de départ par défaut ainsi que l'id de départ
	 * des sites
	 */
	public Environnement(){
		idSite = 0;
		port = 5335;
	};

	/**
	 * Méthode permettant d'initialiser un environnement contenant le nombre de site
	 * renseigné. Chaque site sera lié au nom qui lui correspond dans le tableau renseigné selon 
	 * la règle suivante :
	 * - site 1 -> indice 0 du tableau
	 * - site 2 -> indice 1 du tableau
	 * - etc...
	 * 
	 * @param nbSite, nombre de site requis
	 * @param addressNames, tableau de string contenant le nom que l'on souhaite associer aux sites
	 * @return -1 en cas de paramètres invalides, 1 en cas de réussite.
	 * @throws UnknownHostException
	 */
	public int init(int nbSite, String[] addressNames) throws UnknownHostException {
		// on s'assure que l'on ne crée pas plus de 4 sites
		if (nbSite > NB_MAX_SITE){
			return -1;
		} // on s'assure que l'on fournit un nombre de nom d'InetAddress suffisant
		else if (addressNames.length != nbSite){
			return -1;
		}

		Site courant;
		InetAddress addr;
		List<Site> liste = new ArrayList<Site>();
		sites = new ArrayList<Pair<InetAddress, Integer>>();
		// creation des sites ainsi qu'une entré correspondante dans la liste de Pair
		for (int i = 0; i < nbSite; i++){
			addr = InetAddress.getByName(addressNames[i]);
			sites.add(new Pair<InetAddress, Integer>(addr, new Integer(port)));
			courant = new Site(idSite, port, addr, 2000);
			liste.add(courant);
			idSite++;
			port++;
		}

		vm = new ArrayList<Site>();
		// initialisation des sites avec la liste de Pair complete et demarrage de ces derniers (simulation VM)
		for (Site s : liste){
			s.init(sites);
			s.start();
			vm.add(s);
		}
		return 1;
	}

	/**
	 * Methode permettant de faire tomber en panne le site dont l'id correspond à l'id renseignée.
	 * 
	 * @param siteID, id correspondant au site que l'on souhaite faire tomber en panne
	 * @return true si un site correspondait à l'id, false autrement
	 */
	public boolean simulerPanne(int siteID){
		// on verifie que l'id du site existe
		if (siteID < sites.size()){
			// on kill les threads internes
			vm.get(siteID).panne();
			return true;
		}
		return false;
	}

	/**
	 * Methode permettant d'initier une reprise du site correspondant à l'id renseignée.
	 * 
	 * @param siteID, id correspondant au site que l'on souhaite faire tomber en panne
	 */
	public void reprisePanne(int siteID){
		// on verifie que l'id du site existe
		if (siteID < sites.size()){
			// on restart le thread
			vm.get(siteID).start();
		}
	}

	/**
	 * Méthode permettant d'initier l'arret total des différents sites contenus dans l'environnement
	 */
	public void arretTotal(){
		for(Site s: vm){
			// on kill les threads internes
			s.safeStop();
			// puis on kill le thread principale
			//s.stop(); // normalement pas nécessaire, se termine automatiquement si les threads internes se terminent
		}
	}
}
