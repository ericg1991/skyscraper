package principal;

import java.io.File;
import java.io.FileNotFoundException;
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
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.background.JavaScriptJobManager;

public class Main implements Job {

	static {
		// disable HtmlUnit logging
		LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log",
				"org.apache.commons.logging.impl.NoOpLog");
	}

	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		//parte il ciclo di interrogazione a skyscanner
		System.out.println();
		System.out.println("VIA!");
		System.out.println();
		
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
		

		String datepart = "150526";
		String daterit = "150729";
		String airport_part = "bgy";
		String airport_dest = "mad";
		String URL = ("http://www.skyscanner.it/trasporti/voli/" + airport_part + "/" + airport_dest + "/" + datepart + "/" + daterit + "/");
		
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
		int timeToWait = 25;
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
		
		//salvo la prima pagina in formato html
		
		PrintWriter wr = null;
		try {
			wr = new PrintWriter("Page1AsXml.html");
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		wr.println(page.asXml());
		wr.close();
		
		
		// salvo tutte le pagine della ricerca in formato html, dopodichè in uno step successivo ne farò lo scraping
		  
		List<HtmlElement> elements = (List<HtmlElement>) page.getByXPath("//*[@id=\"cbp-pagination\"]/div[2]/ul/li/button[@title=\"Pagina successiva\"]");
		   int u=2;
		   while( elements.isEmpty()==false){
           HtmlElement element1 = elements.get(0);
           try {
			page = element1.click();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
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
           
           PrintWriter wr3 = null;
		try {
			wr3 = new PrintWriter("Page" + u + "AsXml.html");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
           wr3.println(page.asXml());
   		   wr3.close();
   		   elements = (List<HtmlElement>) page.getByXPath("//*[@id=\"cbp-pagination\"]/div[2]/ul/li/button[@title=\"Pagina successiva\"]");
   		   u++;
		   }
			
		//crea file di testo dove salverò tutti i risultati dell'attuale ricerca
		PrintWriter writer = null;
		try {
			writer = new PrintWriter("agencies.txt");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		   
		//(queste probabilmente non sono necessarie e andranno tolte): dichiaro le liste che servono per salvarvi all'interno le informazioni necessarie poi alla stampa a schermo
		ArrayList<String> timelistgo = new ArrayList<String>();
		ArrayList<String> timelistback = new ArrayList<String>();
		ArrayList<String> bestagencieslist = new ArrayList<String>();
		ArrayList<String> bestagenciespriceslist = new ArrayList<String>();
		
		//variabili necessarie per ciclare etc.
		
		int i;
		int j;
		int t;
		int x=1;
		int z=0;
		
		//scrive data e ora della query al sito
				writer.println("Research time: " + date.toString());
				writer.println();
	
		//qui parte il ciclo per lo scraping, per ogni file che ho ne faccio lo scraping
		while (new File("Page" + x + "AsXml.html").exists()){
			
			//ATTENZIONE! qui il percorso cambia a seconda di dove vado a salvare in locale il file html preso da internet
			File input = new File("/Users/Eric/Documents/workspace/ProvaSkyScrape/Page" + x + "AsXml.html");
			Document doc = null;
			try {
				doc = Jsoup.parse(input, "UTF-8", "http://skyscanner.it/");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//elimino gli elementi precedentemente messi nella lista in modo da non creare problemi per la nuova scrittura
			flights.clear();
			departuretime.clear();
			arrivaltime.clear();
			bestagencies.clear();
			bestagenciesprices.clear();
			otheragencies.clear();
			
			timelistgo.clear();
			timelistback.clear();
			bestagencieslist.clear();
			bestagenciespriceslist.clear();
			
			int b=0;
			
			//salvo gli element appropriati all'interno delle liste di elements (prendo gli elementi dalla pagina html) da cui poi prenderò le informazioni che mi servono
			flights = doc.getElementsByClass("airline");
			departuretime = doc.select("div.depart");
			arrivaltime = doc.select("div.arrive");
			bestagencies = doc.select("div.mainquote-wrapper.clearfix").select("a.ticketing-agent.mainquote-agent");
			bestagenciesprices = doc.select("div.mainquote-wrapper.clearfix").select("a.mainquote-price.big");
			otheragencies = doc.select("div.details-group.clearfix");
			
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
			
			//scrive i voli disponibili per la tratta
			writer.println("Flights with their information:");
			writer.println();
			for(i=0; i<flights.size(); i++){
				z++;
				writer.println(z + ". " + flights.get(i).text());
				//scrive orari partenza di andata e ritorno allineati (con lista)
				for (t=0; t<2; t++) {
					writer.println(timelistgo.get(t).toString() + " -> " + timelistback.get(t));
				}
				writer.println();
				// scrive di ogni possibile volo l'agenzia che offre il prezzo migliore
				writer.println("Agencies selling this flight are:");
				writer.println("> " + bestagencieslist.get(i) + " " + bestagenciespriceslist.get(i));
				//di ogni volo scrive le altre agenzie con prezzi peggiori rispetto alla migliore
				//prima faccio un controllo perchè potrebbero non esserci agenzie oltre la migliore che vendono lo stesso volo
				if(otheragencies.get(i).select("a").size()!=0){
					for (j = 0; j < otheragencies.get(i).select("a").size(); j++) {
						//splitto la stringa in modo da avere il nome dell'agenzia separato dal prezzo, inoltre anche perchè a volte nel box delle agenzie c'è anche un lufthansa senza prezzo, in questo modo faccio si che non venga introdotto nella lista delle ota
						String[] words = otheragencies.get(i).select("a").get(j).text().split(" ");
						if(words.length > 1){
							//!!!stampa nome e prezzo..ma se volessi stampare anche il simbolo € non lo fa perchè dice che non esiste quell'elemento words[2]..forse non riconosce il simbolo €
						writer.println("> " + words[0] + " " + words[1]);
//						writer.println("> " + otheragencies.get(i).select("a").get(j).text());
						}
					}
				}
				
				writer.println();
			}
			
			x++;
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