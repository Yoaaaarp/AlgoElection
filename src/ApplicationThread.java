/**
 * Auteurs	: Marc Pellet et David Villa
 * Labo		: 03 - PRR
 * File		: ApplicationThread.java
 * 
 * DESCRIPTION :
 * 
 * Cette classe correspond à la couche applicative de notre site. Elle s'occupe de gérer les tâches
 * à effectuer ainsi que la communication avec l'élu lorsque les tâches à effectuer requière l'intervention
 * de ce dernier.
 * 
 * Dans le cadre de notre laboratoire, la couche applicative possède une seule tâche qui s'effectue toutes les
 * 10 secondes après la terminaison de la tâche précédente. 
 * 
 * La tâche en question est un simple échange de message avec l'élu afin de savoir si ce dernier est toujours
 * actif ou non.
 * 
 * La couche applicative utilise le gestionnaire d'élection afin de récupérer l'élu courant ou demander une élection.
 * 
 * CAS POSSIBLE LORS DE L'EXECUTION D'UNE TACHE :
 * 
 * Lorsque la couche applicative a une tâche nécessitant une communication avec l'élu plusieurs cas peuvent se
 * présenter tels que :
 * 1. Aucun élu n'est connu par le gestionnaire d'élection
 * 2. L'élu est connu et est actif
 * 3. L'élu est connu et est inactif
 * 4. Une élection est en cours
 * 
 * CAS 1 :
 * 
 * Dans le cas où aucun élu n'est connu par notre gestionnaire, la couche applicative demande à
 * ce dernier de commencer une nouvelle élection.
 * 
 * CAS 2 :
 * 
 * Dans ce cas là, aucun problème n'est a signaler.
 * 
 * CAS 3 :
 * 
 * Dans ce cas là, le gestionnaire nous a renseigné l'élu courant et lors de la communication avec
 * ce dernier, le timeout sur la réception de la réponse est atteint. Nous considérons donc l'élu 
 * comme étant en panne et une nouvelle élection est démarrée.
 * 
 * CAS 4 :
 * 
 * Dans le cas où le gestionnaire nous signale qu'une élection est en court la couche applicative se 
 * met en attente du résultat de l'élection. Une sécurité, sous la forme d'un timeout, est mise en 
 * place pour palier à un problème durant l'élection. Si le timeout est atteint, on renouvelle la 
 * demande d'élection.
 * 
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import javafx.util.Pair;


public class ApplicationThread implements Runnable {
	private ElectionThread gestionnaire;
	private Pair<InetAddress, Integer> site;
	private Pair<InetAddress, Integer> elu;
	private DatagramSocket serverSocket;
	private Object sync;
	private boolean running;
	private int nbSite;
	private final int T;
	
	/**
	 * Constructeur de la couche applicative d'un site. 
	 * 
	 * La couche applicative a besoin de connaitre les informations du site concernant son InetAddress ainsi que
	 * le port utiliser afin de pouvoir communiquer avec l'élu.
	 * 
	 * De plus, elle a besoin de connaitre le gestionnaire d'élection afin de pouvoir récupérer les informations
	 * sur l'élu courant ou demander une nouvelle élection.
	 * 
	 * @param gestionnaire, gstionnaire d'élection
	 * @param site, information sur l'InetAddress et le port du site
	 * @param T, temps max défini pour la transmission d'un message 
	 * @param nbSite, nombre de site contenu dans l'environnement
	 */
	public ApplicationThread(ElectionThread gestionnaire, Pair<InetAddress, Integer> site, int T, int nbSite){
		this.T = T;
		this.nbSite = nbSite;
		this.gestionnaire = gestionnaire;
		this.site = site;
		serverSocket = null;
		running = true;
	}

	/**
	 * Lorsque la couche applicative est activée, elle s'occupe de prendre contacte avec l'élu
	 * ou de gérer les demandes d'élection en cas d'échec de communication ou d'élu non-connu.
	 * (CF description de la classe pour plus d'information concernant les cas possibles)
	 */
	@Override
	public void run() {
		sync = new Object();
		gestionnaire.setSync(sync);
		boolean success = false;
		
		while(running){
			try {
				// on boucle tant que l'opération n'a pas réussie
				while(!success && running){
					// si une election est en cours, on att qu'elle se termine
					if (gestionnaire.isElectionEnCours()){
						synchronized (sync){
							wait(nbSite*T);
						}
					}
					// on recupère l'elu courant
					elu = gestionnaire.getElu();
					// deux cas, soit aucun elu courant
					if (elu == null){
						// alors on demande une élection
						gestionnaire.trouverNouvelElu();
					} // soit un elu est connu
					else {
						// on tente de communiquer avec l'elu
						success = communicationAvecElu();
						// si la communication n'a pas aboutie, elu en panne et on demande une nouvelle election
						if (!success){
							gestionnaire.trouverNouvelElu();
						} // en cas de communication réussie
						else {
							// on attend 10 sec avant la prochaine opération
							Thread.sleep(10000);
						}
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e){
				e.printStackTrace();
			}
		}
		if(!serverSocket.isClosed()){
			serverSocket.close();
		}
	}

	/**
	 * Méthode gérant la transmission d'un message vers l'élu et la réception de sa réponse.
	 * 
	 * @return true si la communication a réussie, false si le timeout a été atteint.
	 * @throws IOException
	 */
	private boolean communicationAvecElu() throws IOException {
		// ouverture du socket
		if (serverSocket == null){
			serverSocket = new DatagramSocket(site.getValue());
		}
		// préparation du message a envoyer
		byte[] tampon = intToBytes(Message.BONJOUR.ordinal());
		DatagramPacket message = new DatagramPacket(tampon, tampon.length, elu.getKey(), elu.getValue());
		// envoi du message
		serverSocket.send(message);
		// placement d'un timeout sur la reponse
		serverSocket.setSoTimeout(2*T);
		// creation réponse
		DatagramPacket response = new DatagramPacket(tampon, tampon.length);
		try{
			// attente de la réponse de l'élu
			serverSocket.receive(response);
			// la reponse en soit est negligée, ce qui nous intéresse étant la réussite de la communication
		} catch(SocketException e){
			// le timeout s'est déclenché, elu probablement en panne
			return false;
		}
		return true;
	}

	/**
	 * Méthode permettant de modifier l'état running de la couche applicative. Modifier running
	 * à false permet un arrêt sécurisé du thread.
	 * 
	 * @param running, boolean définissant l'état de la couche applicative
	 */
	public void setRunning(boolean running){
		this.running = running;
	}

	/**
	 * Méthode permettant de convertir un entier en tableau de byte.
	 * 
	 * @param my_int, entier a convertir en byte
	 * @return le tableau de byte contenant l'entier convertit
	 * @throws IOException
	 */
	private byte[] intToBytes(int my_int) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = new ObjectOutputStream(bos);
		out.writeInt(my_int);
		out.close();
		byte[] int_bytes = bos.toByteArray();
		bos.close();
		return int_bytes;
	}

}
