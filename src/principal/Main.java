package principal;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.background.JavaScriptJobManager;

class Main {

	static {
		// disable HtmlUnit logging
		LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log",
				"org.apache.commons.logging.impl.NoOpLog");
	}

	public static void main(String args[]) throws IOException {

		String URL = "http://www.skyscanner.it/trasporti/voli/bgy/mad/150522/150529/tariffe-aeree-da-milano-bergamo-orio-al-serio-per-madrid-a-maggio-2015.html?adults=1&children=0&infants=0&cabinclass=economy&preferdirects=false&outboundaltsenabled=false&inboundaltsenabled=false&rtn=1";
		WebClient webClient = new WebClient(BrowserVersion.FIREFOX_31);
		webClient.getOptions().setJavaScriptEnabled(true);
		webClient.getOptions().setRedirectEnabled(true);
		webClient.getOptions().setCssEnabled(true);
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webClient.getOptions().setThrowExceptionOnScriptError(true);
		webClient.getOptions().setUseInsecureSSL(true);
		webClient.setAjaxController(new NicelyResynchronizingAjaxController());
		webClient.waitForBackgroundJavaScriptStartingBefore(10000);
		webClient.setJavaScriptTimeout(2147483647);
		webClient.setCssErrorHandler(new SilentCssErrorHandler());
		webClient.waitForBackgroundJavaScript(5000);
		webClient.setRefreshHandler(new ThreadedRefreshHandler());
		webClient.getCookieManager().setCookiesEnabled(true);
		
		Elements otheragencies = new Elements();
		Elements bestagenciesprices = new Elements();
		Elements bestagencies = new Elements();
		Elements flights = new Elements();
		Elements departuretime = new Elements();
		Elements arrivaltime = new Elements();
		Date date = new Date();
		
		HtmlPage page = null;
		try {
			page = webClient.getPage(URL);
		} catch (Exception e) {
			System.out.println("Get page error");
		}
		JavaScriptJobManager manager = page.getEnclosingWindow()
				.getJobManager();
		int timeToWait = 20;
		while (manager.getJobCount() > 0) {
			timeToWait--;
			System.out.println(timeToWait + " seconds left... ("
					+ manager.getJobCount() + " jobs left)\n");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (timeToWait <= 0)
				break;
		}
		
		// save all contents
		PrintWriter wr = new PrintWriter("Page1AsXml.html");
		wr.println(page.asXml());
		wr.close();
		
		PrintWriter wr1 = new PrintWriter("Page1AsText.txt");
		wr1.println(page.asText());
		wr1.close();
		//ATTENZIONE! qui il percorso cambia a seconda di dove vado a salvare in locale il file html preso da internet
		File input = new File("/Users/Eric/Documents/workspace/ProvaSkyScrape/Page1AsXml.html");
		Document doc = Jsoup.parse(input, "UTF-8", "http://skyscanner.it/");		
		
		PrintWriter writer = new PrintWriter("agencies.txt");
		
		//salvo gli elementi di cui abbiamo bisogno da cui estrarre le informazioni
		flights = doc.getElementsByClass("airline");
		departuretime = doc.select("div.depart");
		arrivaltime = doc.select("div.arrive");
		bestagencies = doc.select("div.mainquote-wrapper.clearfix").select("a.ticketing-agent.mainquote-agent");
		bestagenciesprices = doc.select("div.mainquote-wrapper.clearfix").select("a.mainquote-price.big");
		otheragencies = doc.select("div.details-group.clearfix");
		
		//dichiaro le liste che servono per salvarvi all'interno le informazioni necessarie poi alla stampa a schermo
		ArrayList<String> timelistgo = new ArrayList<String>();
		ArrayList<String> timelistback = new ArrayList<String>();
		ArrayList<String> bestagencieslist = new ArrayList<String>();
		ArrayList<String> bestagenciespriceslist = new ArrayList<String>();
		
		
		int b=0;
		int i;
		int j;
		int t;
		
//		//creo una lista di elementi che sono i voli (non serve posso stampare direttamente)
//		ArrayList<String> flightslist = new ArrayList<String>();
//		for (Element el : flights) {
//			flightslist.add(el.text());
//		}
		
		//creo una lista con tutte le partenze di tutti i voli
		for (Element el : departuretime) {
				timelistgo.add(el.text());
				if (timelistgo.get(b).contains("Andata")){
					timelistgo.remove(b);
					}
				else{
				b++;
				}
			}
		
		//creo una lista con tutti gli arrivi di tutti i voli
		for (Element el : arrivaltime) {
			timelistback.add(el.text());
			}
		
		//creo una lista delle agenzie con i migliori prezzi per ciascuno volo (solo nome agenzia)
		for (Element el : bestagencies) {
			bestagencieslist.add(el.text());
			}
			
		//creo una lista delle agenzie con i migliori prezzi per ciascuno volo (solo prezzo agenzia)
		for (Element el : bestagenciesprices) {
			bestagenciespriceslist.add(el.text());
			}

		//estraggo le informazioni dagli elementi(o dalle liste che ho creato prima) e le stampo a schermo
		
		//scrive data e ora della query al sito
		writer.println("Research time: " + date.toString());
		writer.println();
		//scrive i voli disponibili per la tratta
		writer.println("Flights with their information:");
		writer.println();
		for(i=0; i<flights.size(); i++){
			writer.println(i+1 + ". " + flights.get(i).text());
			//scrive orari partenza di andata e ritorno allineati (con lista)
			for (t=0; t<2; t++) {
				writer.println(timelistgo.get(t).toString() + " -> " + timelistback.get(t));
			}
			writer.println();
			// scrive di ogni possibile volo l'agenzia che offre il prezzo migliore
			writer.println("Agencies selling this flight are:");
			writer.println("> " + bestagencieslist.get(i) + " " + bestagenciespriceslist.get(i));
			//di ogni volo scrive le altre agenzie con prezzi peggiori rispetto alla migliore
			//!PROBLEMA! devo fare un controllo perchè a volte non esistono ota che offrano lo stesso volo a un prezzo maggiore rispetto alla migliore !ATTENZIONE!
			if(otheragencies.get(i).select("a").size()!=0){
				for (j = 0; j < otheragencies.get(i).select("a").size(); j++) {
//					String[] words = otheragencies.get(i).select("a").get(j).text().split(" ");
//					writer.println("> " + words[0] + " " + words[1] + " " + words[2]);
					writer.println("> " + otheragencies.get(i).select("a").get(j).text());
				}
			}
			
			writer.println();
		}
		
	//salva le altre pagine successive alla prima in formato html di cui dovrò fare lo scraping
		   List<HtmlElement> elements = (List<HtmlElement>) page.getByXPath("//*[@id=\"cbp-pagination\"]/div[2]/ul/li/button[@title=\"Pagina successiva\"]");
		   int u=2;
		   while( elements.isEmpty()==false){
              HtmlElement element1 = elements.get(0);
              page = element1.click();
              timeToWait = 3;
              while (manager.getJobCount() > 0) {
      			timeToWait--;
      			System.out.println(timeToWait + " seconds left... ("
      					+ manager.getJobCount() + " jobs left)\n");
      			try {
      				Thread.sleep(1000);
      			} catch (InterruptedException e) {
      				e.printStackTrace();
      				}
      			if (timeToWait <= 0)
      				break;
      		  }
              
            PrintWriter wr3 = new PrintWriter("Page" + u + "AsXml.html");
      		wr3.println(page.asXml());
      		wr3.close();
      		elements = (List<HtmlElement>) page.getByXPath("//*[@id=\"cbp-pagination\"]/div[2]/ul/li/button[@title=\"Pagina successiva\"]");
      		u++;
		   }
		writer.close();
		
//		// save just the div "content-main"
//		HtmlDivision div = page.getHtmlElementById("content-main");
//		div.asText();
//
//		wr = new PrintWriter("DivAsXml.txt");
//		wr.println(div.asXml());
//		wr.close();
//
//		wr = new PrintWriter("DivAsText.txt");
//		wr.println(div.asText());
//		wr.close();

		webClient.close();

		System.out.println("Done.");

	}
}

