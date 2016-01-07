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
	private boolean running;
	private Pair<InetAddress, Integer> site;
	private DatagramSocket serverSocket;
	private Pair<InetAddress, Integer> elu;
	private final int T;
	private int nbSite;
	private Object sync;


	public ApplicationThread(ElectionThread gestionnaire, Pair<InetAddress, Integer> site, int T, int nbSite){
		this.T = T;
		this.nbSite = nbSite;
		this.gestionnaire = gestionnaire;
		this.site = site;
		serverSocket = null;
		running = true;
	}

	@Override
	public void run() {
		sync = new Object();
		gestionnaire.setSync(sync);
		boolean success = false;
		
		while(running){
			try {
				// on boucle tant que l'op�ration n'a pas r�ussie
				while(!success){
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
	}

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

	public void setRunning(boolean running){
		this.running = running;
	}

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
