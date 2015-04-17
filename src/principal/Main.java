package principal;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.PrintWriter;


class Main {
		
	public static void main(String args[]) throws IOException {
		
		String URL = "http://www.skyscanner.it/trasporti/voli/bgy/mad/150610/150615/tariffe-aeree-da-milano-bergamo-orio-al-serio-per-madrid-a-giugno-2015.html?adults=1&children=0&infants=0&cabinclass=economy&preferdirects=false&outboundaltsenabled=false&inboundaltsenabled=false&rtn=1&PageSpeed=noscript";
		
		
		//A list of Elements, with methods that act on every element in the list.
		//An HTML element consists of a tag name, attributes, and child nodes (including text nodes and other elements).
		//From an Element, you can extract data, traverse the node graph, and manipulate the HTML.
		Elements agencies = new Elements();
		
		//A HTML Document.
		Document doc = new Document("");
		
		int i = 1;
		//String URL = "http://www.skyscanner.it/trasporti/voli/bgy/mad/150610/150615/tariffe-aeree-da-milano-bergamo-orio-al-serio-per-madrid-a-giugno-2015.html?adults=1&children=0&infants=0&cabinclass=economy&preferdirects=false&outboundaltsenabled=false&inboundaltsenabled=false&rtn=1";
		System.out.println(i + "; " + URL);
		
		
		
		try{
			PrintWriter writer = new PrintWriter("agencies.txt");
			// Carica la pagina web
			try{
				//userAgent --> Request configuration can be made using the shortcut methods in Connection
				//Timeout -->  Set the request timeouts (connect and read).
				doc = Jsoup.connect(URL).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").timeout(20000).get();
				}
			catch (IOException e) {
				System.err.println("Errore di lettura della pagina da internet");
				e.printStackTrace();
			}
			//stampa ciò che è contenuto in doc ovvero la pagina html scaricata (questo è per provare e vedere se prende la pagina giusta, non andrà nel programma finale)
			writer.println(doc.toString());
			
			
			//Public Elements select(String cssQuery)
			//Find elements that match the Selector CSS query,
			//with this element as the starting context. Matched elements may include this element, or any of its children
			agencies = doc.select("a.ticketing-agent.mainquote-agent");
			
			
			
			//.text() -> Prende l'elemento di testo presente tra i tag
			for (Element el : agencies) {
				writer.println(el.text());
			}
			
			
			// Chiudi il file di output prima di uscire 
			writer.close();

			
			}catch(Exception FileNotFoundException){}
	}
}
