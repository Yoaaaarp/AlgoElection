/**
 * Auteurs	: Marc Pellet et David Villa
 * Labo		: 03 - PRR
 * File		: ApplicationThread.java
 * 
 * DESCRIPTION :
 * 
 * Cette classe correspond � la couche applicative de notre site. Elle s'occupe de g�rer les t�ches
 * � effectuer ainsi que la communication avec l'�lu lorsque les t�ches � effectuer requi�re l'intervention
 * de ce dernier.
 * 
 * Dans le cadre de notre laboratoire, la couche applicative poss�de une seule t�che qui s'effectue toutes les
 * 10 secondes apr�s la terminaison de la t�che pr�c�dente. 
 * 
 * La t�che en question est un simple �change de message avec l'�lu afin de savoir si ce dernier est toujours
 * actif ou non.
 * 
 * La couche applicative utilise le gestionnaire d'�lection afin de r�cup�rer l'�lu courant ou demander une �lection.
 * 
 * CAS POSSIBLE LORS DE L'EXECUTION D'UNE TACHE :
 * 
 * Lorsque la couche applicative a une t�che n�cessitant une communication avec l'�lu plusieurs cas peuvent se
 * pr�senter tels que :
 * 1. Aucun �lu n'est connu par le gestionnaire d'�lection
 * 2. L'�lu est connu et est actif
 * 3. L'�lu est connu et est inactif
 * 4. Une �lection est en cours
 * 
 * CAS 1 :
 * 
 * Dans le cas o� aucun �lu n'est connu par notre gestionnaire, la couche applicative demande �
 * ce dernier de commencer une nouvelle �lection.
 * 
 * CAS 2 :
 * 
 * Dans ce cas l�, aucun probl�me n'est a signaler.
 * 
 * CAS 3 :
 * 
 * Dans ce cas l�, le gestionnaire nous a renseign� l'�lu courant et lors de la communication avec
 * ce dernier, le timeout sur la r�ception de la r�ponse est atteint. Nous consid�rons donc l'�lu 
 * comme �tant en panne et une nouvelle �lection est d�marr�e.
 * 
 * CAS 4 :
 * 
 * Dans le cas o� le gestionnaire nous signale qu'une �lection est en court la couche applicative se 
 * met en attente du r�sultat de l'�lection. Une s�curit�, sous la forme d'un timeout, est mise en 
 * place pour palier � un probl�me durant l'�lection. Si le timeout est atteint, on renouvelle la 
 * demande d'�lection.
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
	 * le port utiliser afin de pouvoir communiquer avec l'�lu.
	 * 
	 * De plus, elle a besoin de connaitre le gestionnaire d'�lection afin de pouvoir r�cup�rer les informations
	 * sur l'�lu courant ou demander une nouvelle �lection.
	 * 
	 * @param gestionnaire, gstionnaire d'�lection
	 * @param site, information sur l'InetAddress et le port du site
	 * @param T, temps max d�fini pour la transmission d'un message 
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
	 * Lorsque la couche applicative est activ�e, elle s'occupe de prendre contacte avec l'�lu
	 * ou de g�rer les demandes d'�lection en cas d'�chec de communication ou d'�lu non-connu.
	 * (CF description de la classe pour plus d'information concernant les cas possibles)
	 */
	@Override
	public void run() {
		sync = new Object();
		gestionnaire.setSync(sync);
		boolean success = false;
		
		while(running){
			try {
				// on boucle tant que l'op�ration n'a pas r�ussie
				while(!success && running){
					// si une election est en cours, on att qu'elle se termine
					if (gestionnaire.isElectionEnCours()){
						synchronized (sync){
							wait(nbSite*T);
						}
					}
					// on recup�re l'elu courant
					elu = gestionnaire.getElu();
					// deux cas, soit aucun elu courant
					if (elu == null){
						// alors on demande une �lection
						gestionnaire.trouverNouvelElu();
					} // soit un elu est connu
					else {
						// on tente de communiquer avec l'elu
						success = communicationAvecElu();
						// si la communication n'a pas aboutie, elu en panne et on demande une nouvelle election
						if (!success){
							gestionnaire.trouverNouvelElu();
						} // en cas de communication r�ussie
						else {
							// on attend 10 sec avant la prochaine op�ration
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
	 * M�thode g�rant la transmission d'un message vers l'�lu et la r�ception de sa r�ponse.
	 * 
	 * @return true si la communication a r�ussie, false si le timeout a �t� atteint.
	 * @throws IOException
	 */
	private boolean communicationAvecElu() throws IOException {
		// ouverture du socket
		if (serverSocket == null){
			serverSocket = new DatagramSocket(site.getValue());
		}
		// pr�paration du message a envoyer
		byte[] tampon = intToBytes(Message.BONJOUR.ordinal());
		DatagramPacket message = new DatagramPacket(tampon, tampon.length, elu.getKey(), elu.getValue());
		// envoi du message
		serverSocket.send(message);
		// placement d'un timeout sur la reponse
		serverSocket.setSoTimeout(2*T);
		// creation r�ponse
		DatagramPacket response = new DatagramPacket(tampon, tampon.length);
		try{
			// attente de la r�ponse de l'�lu
			serverSocket.receive(response);
			// la reponse en soit est neglig�e, ce qui nous int�resse �tant la r�ussite de la communication
		} catch(SocketException e){
			// le timeout s'est d�clench�, elu probablement en panne
			return false;
		}
		return true;
	}

	/**
	 * M�thode permettant de modifier l'�tat running de la couche applicative. Modifier running
	 * � false permet un arr�t s�curis� du thread.
	 * 
	 * @param running, boolean d�finissant l'�tat de la couche applicative
	 */
	public void setRunning(boolean running){
		this.running = running;
	}

	/**
	 * M�thode permettant de convertir un entier en tableau de byte.
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
