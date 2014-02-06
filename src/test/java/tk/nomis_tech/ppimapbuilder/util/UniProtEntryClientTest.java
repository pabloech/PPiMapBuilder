package tk.nomis_tech.ppimapbuilder.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import tk.nomis_tech.ppimapbuilder.networkbuilder.data.UniProtProtein;

public class UniProtEntryClientTest {

	private static List<String> prots;

	@BeforeClass
	public static void init() {
		prots = Arrays.asList(new String[] { "P04040", "P61106", "Q70EK8", "Q96RU2", "Q92621", "Q9BRV8", "Q92624", "Q9NV70", "P13804", "Q9UMJ3", "Q8ZAB3", "P13807", "Q5Y7A7", "O00629", "O00628", "Q96S59", "O00623", "P22455", "P04637", "Q8TDI0", "Q92633", "P04632", "Q8WVM8", "Q5NH30", "Q8ZAC2" });
		System.out.println("Testing UniProtEntryClient on "+prots.size()+" proteins");
	}
	
	@Test
	public void test() throws IOException {
		HashMap<String, UniProtProtein> results = new HashMap<String, UniProtProtein>();
		
		for(String prot: prots) {			
			UniProtProtein retrieveProteinData = UniProtEntryClient.getInstance().retrieveProteinData(prot);
			if(retrieveProteinData != null) {
				results.put(prot, retrieveProteinData);
			}
		}
		System.out.println(results);
	}

	@Test
	public void test1() throws IOException {
		HashMap<String,UniProtProtein> retrieveProteinsData = UniProtEntryClient.getInstance().retrieveProteinsData(prots);
		System.out.println(retrieveProteinsData);
	}
}
