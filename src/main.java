import java.net.InetAddress;
import java.net.UnknownHostException;


public class main {

	public static void main(String[] args) {
		System.out.println("Debut programme");
		
		String[] addrNames = {"localhost", "localhost", "localhost", "localhost"};
		Environnement env = new Environnement();
		
		try {
			env.init(4, addrNames);
			// TODO simuler la panne du site elu apres X secondes
			// TODO simuler la reprise du site en panne après Y secondes
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}		
		
		env.arretTotal();
		System.out.println("Fin programme");
	}

}
