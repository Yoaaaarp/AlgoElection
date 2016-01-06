import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;


public class ElectionThread implements Runnable {
	private Site elu;
	private boolean electionEnCours;
	private int id;
	private int aptitude;
	
	public ElectionThread(int id, int aptitude){
		this.id = id;
		this.aptitude = aptitude;
	}

	@Override
	public void run() {
		// TODO faire un truc qui correspond à la donnée...
		while(true);
	}

	/**
	 * @return the elu
	 */
	public Site getElu() {
		return elu;
	}
	
	/**
	 * @return the electionEnCours
	 */
	public boolean isElectionEnCours() {
		return electionEnCours;
	}

	public void trouverNouvelElu(){
		HashMap<Integer, Integer> aptitudes = new HashMap<Integer, Integer>();
		aptitudes.put(id, aptitude);

		
	    
	    
	    
	    
		
	}
	
	private byte[] HashMapToByteArray(HashMap<Integer, Integer> map){
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
	    ObjectOutputStream out;
		try {
			out = new ObjectOutputStream(byteOut);
			out.writeObject(map);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return byteOut.toByteArray();
	}
	
	
}
