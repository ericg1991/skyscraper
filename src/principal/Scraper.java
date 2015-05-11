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
import org.quartz.JobDataMap;

public class Scraper implements Job {

	static {
		// disable HtmlUnit logging
		LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log",
				"org.apache.commons.logging.impl.NoOpLog");
	}

	public void execute(JobExecutionContext jeContext) throws JobExecutionException {
		//parte il ciclo di interrogazione a skyscanner
		System.out.println();
		System.out.println("GO!");
		System.out.println();
		
		WebClient webClient = new WebClient(BrowserVersion.FIREFOX_31);
		webClient.getOptions().setJavaScriptEnabled(true);
		webClient.getOptions().setRedirectEnabled(true);
		webClient.getOptions().setCssEnabled(true);
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		//posso mettere questo a false e non da più l'eccezione 
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setUseInsecureSSL(true);
		webClient.setAjaxController(new NicelyResynchronizingAjaxController());
		webClient.waitForBackgroundJavaScriptStartingBefore(10000);
		webClient.setJavaScriptTimeout(2147483647);
		webClient.setCssErrorHandler(new SilentCssErrorHandler());
		webClient.waitForBackgroundJavaScript(5000);
		webClient.setRefreshHandler(new ThreadedRefreshHandler());
		webClient.getCookieManager().setCookiesEnabled(true);
		
		 JobDataMap jdMap = jeContext.getJobDetail().getJobDataMap();
		 String airport_part = jdMap.get("airport_part").toString();
		 String airport_dest = jdMap.get("airport_dest").toString();
		 String datepart = jdMap.get("datepart").toString();
		 String daterit = jdMap.get("daterit").toString();
		 int pagesNumber = (int) jdMap.get("pagesNumber");

		 System.out.println("Ricerca per aeroporto di partenza: "+ airport_part);
		 System.out.println("Ricerca per aeroporto di destinazione: "+ airport_dest);
		 System.out.println("Ricerca per data di partenza: "+ datepart);
		 System.out.println("Ricerca per aeroporto di ritorno: "+ daterit);
		 System.out.println("Pagine richieste: " + pagesNumber);
		 System.out.println();
		
		String URL = ("http://www.skyscanner.it/trasporti/voli/" + airport_part + "/" + airport_dest + "/" + datepart + "/" + daterit + "/");
		
		Elements otheragencies = new Elements();
		Elements bestagenciesprices = new Elements();
		Elements bestagencies = new Elements();
		Elements flights = new Elements();
		Elements depart = new Elements();
		Elements arrive = new Elements();
		Elements stops = new Elements();
		Elements check = new Elements();
		
		Date date = new Date();
		
		HtmlPage page = null;
		try {
			page = webClient.getPage(URL);
		} catch (Exception e) {
			System.out.println(URL);
			System.out.println("Get page error");
			System.out.println(e.toString());
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
		while( elements.isEmpty()==false && u<=pagesNumber ){
			HtmlElement element1 = elements.get(0);
			
			try {
				page = element1.click();
			} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			}
			timeToWait = 7;
	        while (manager.getJobCount() > 0) {
	        	timeToWait--;
	   			System.out.println(timeToWait + " seconds left... ("
	   					+ manager.getJobCount() + " jobs left)\n");
	   			try {
	   				Thread.sleep(1000);
	   			} catch (InterruptedException e) {
	   				e.printStackTrace();
	   			}
	   			if (timeToWait <= 0) {
	   				break;
	   			}
	   				
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
		int p=u;
		while (new File("Page" + p + "AsXml.html").exists()){
			new File("Page" + p + "AsXml.html").delete();
			p++;
		}
		
		//crea file di testo dove salverò tutti i risultati dell'attuale ricerca
		PrintWriter writer = null;
		try {
			String dateToString = date.toString();
			String fileName;
			dateToString = dateToString.replaceAll(":", ".");
			dateToString = dateToString.replaceAll("/", ".");
			fileName = airport_part + "_" + airport_dest + "_" + datepart + "_" + daterit + "_" + dateToString;
			writer = new PrintWriter(fileName + ".csv");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		   
		//dichiaro le liste che servono per salvarvi all'interno le informazioni necessarie poi alla stampa a schermo
		ArrayList<String> departList = new ArrayList<String>();
		ArrayList<String> arriveList = new ArrayList<String>();
		ArrayList<String> bestagencieslist = new ArrayList<String>();
		ArrayList<String> bestagenciespriceslist = new ArrayList<String>();
		String[] departureGoWords;
		String[] arrivalGoWords;
		String[] departureBackWords;
		String[] arrivalBackWords;
		String[] stopsGoWords;
		String[] stopsBackWords;
		String departureAirportGo;
		String departureAirportBack;
		String arrivalAirportGo;
		String arrivalAirportBack;
		String departureTimeGo;
		String departureTimeBack;
		String arrivalTimeGo;
		String arrivalTimeBack;
		String stopsGo;
		String stopsBack;
		String price;
		String ota;
		String airlineCompany;
		
		//variabili necessarie per ciclare etc.
		
		int i;
		int j;
		int k;
		int x=1;
		int doublebookings = 0;
		int lenghtWords;
		
		//scrive data e ora della query al sito
		writer.println("Research time: " + date.toString());
	
		//intestazioni della tabella
		writer.println("Flight;DepartureAirportGo;StopsGo;ArrivalAirportGo;DepartureAirportBack;StopsBack;ArrivalAirportBack;DateGo;DateBack;DeparturetimeGo;ArrivalTimeGo;DeparturetimeBack;ArrivalTimeBack;OTA;Price;");
				
		//qui parte il ciclo per lo scraping, per ogni file che ho ne faccio lo scraping
		while (new File("Page" + x + "AsXml.html").exists()){
			
			File input = new File("Page" + x + "AsXml.html");
			Document doc = null;
			try {
				doc = Jsoup.parse(input, "UTF-8", "http://skyscanner.it/");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//elimino gli elementi precedentemente messi nella lista in modo da non creare problemi per la nuova scrittura
			flights.clear();
			depart.clear();
			arrive.clear();
			bestagencies.clear();
			bestagenciesprices.clear();
			otheragencies.clear();
			departList.clear();
			arriveList.clear();
			bestagencieslist.clear();
			bestagenciespriceslist.clear();
			stops.clear();
			check.clear();
			
			int b=0;
			doublebookings = 0;
			
			//salvo gli element appropriati all'interno delle liste di elements (prendo gli elementi dalla pagina html) da cui poi prenderò le informazioni che mi servono
			flights = doc.getElementsByClass("airline");
			//Il formato che ottengo con go � ORA AEROPORTO per l'andata
			depart = doc.select("div.depart");
			//Il formato che ottengo con go � ORA AEROPORTO per il ritorno
			arrive = doc.select("div.arrive");
			bestagencies = doc.select("div.mainquote-wrapper.clearfix").select("a.ticketing-agent.mainquote-agent");
			bestagenciesprices = doc.select("div.mainquote-wrapper.clearfix").select("a.mainquote-price.big");
			otheragencies = doc.select("div.details-group.clearfix");
			//Salvo tutti gli scali
			stops = doc.select("div.leg-stops");
			check = doc.select("div.mainquote-wrapper.clearfix");
			
			//creo una lista con tutte le partenze di tutti i voli
			for (Element el : depart) {
				departList.add(el.text());
				if (departList.get(b).contains("Andata")){
					departList.remove(b);
					}
				else{
				b++;
				}
			}
			
			//creo una lista con tutti gli arrivi di tutti i voli
			for (Element el : arrive) {
				arriveList.add(el.text());
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
			
			for(i=0; i<flights.size(); i++){
				
				//Orario e aeroporto di partenza del viaggio di andata
				departureGoWords = departList.get(2*i).toString().split(" ");
				//Orario e aeroporto di partenza del viaggio di ritorno
				arrivalGoWords = arriveList.get(2*i).toString().split(" ");
				//Orario e aeroporto di atterraggio del viaggio di andata
				departureBackWords = departList.get(2*i+1).toString().split(" ");
				//Orario e aeroporto di atterraggio del viaggio di ritorno
				arrivalBackWords = arriveList.get(2*i+1).toString().split(" ");
				
				//FLIGHT
				airlineCompany = flights.get(i).text();
				writer.print(airlineCompany + ";");
				
				//DEPARTURE AIRPORT GO
				departureAirportGo = departureGoWords[1];
				writer.print(departureAirportGo + ";");
				
				//SCALI
				//Il formato della stringa per gli scali pu� essere
				//In caso di nessun scalo DIRETTO
				//In caso di 1 o pi� scali # SCALO LISTA_DI_CITTA'
				stopsGoWords = stops.get(2*i).text().toString().split(" ");		
				stopsGo = "";				
				//Nel caso in cui c'� almeno uno scalo vai a prendere dalla terza parola in poi
				if (stopsGoWords.length != 1) {
					for(k=2; k<stopsGoWords.length; k++) {
						stopsGo = stopsGo + stopsGoWords[k] + " ";
					}
				}
				writer.print(stopsGo + ";");
				
				
				//ARRIVAL AIRPORT GO
				//Se la stringa che sto analizzando contiene (+1) al suo interno
				if (arrivalGoWords.length > 2) {
					arrivalAirportGo = arrivalGoWords[2];
				} else {
					arrivalAirportGo = arrivalGoWords[1];
				}
				writer.print(arrivalAirportGo + ";");
				
				//DEPA AIRPORT BACK
				departureAirportBack = departureBackWords[1];
				writer.print(departureAirportBack + ";");
				
				//SCALI
				stopsBackWords = stops.get(2*i+1).text().toString().split(" ");
				stopsBack = "";
				if (stopsBackWords.length != 1) {
					for(k=2; k<stopsBackWords.length; k++) {
						stopsBack = stopsBack + stopsBackWords[k] + " ";
					}
				}
				writer.print(stopsBack + ";");
				
				//ARRIVAL AIRPORT BACK
				//Se la stringa che sto analizzando contiene (+1) al suo interno
				if (arrivalBackWords.length == 3) {
					arrivalAirportBack = arrivalBackWords[2];
				} else {
					arrivalAirportBack = arrivalBackWords[1];
				}
				writer.print(arrivalAirportBack + ";");
				
				//DATE FROM
				writer.print(datepart + ";");
				//DATE BACK
				writer.print(daterit + ";");
				
				//DEPARTURE TIME GO
				departureTimeGo = departureGoWords[0];
				writer.print(departureTimeGo + ";");
				
				//ARRIVAL TIME GO
				arrivalTimeGo = arrivalGoWords[0];
				writer.print(arrivalTimeGo + ";");
				//DEPARTURE TIME BACK
				departureTimeBack = departureBackWords[0];
				writer.print(departureTimeBack + ";");
				//ARRIVAL TIME BACK
				arrivalTimeBack = arrivalBackWords[0];
				writer.print(arrivalTimeBack + ";");
				
				//scrive nome e prezzo bestagency per il volo
				String[] wordsA;
				if (check.get(i).text().toString().contains("prenotazioni")) {
					doublebookings++;
					writer.print(airlineCompany + ";");					
				} else {
					writer.print(bestagencieslist.get(i-doublebookings) + ";");
				}
				wordsA = check.get(i).text().toString().split(" ");
				writer.print(wordsA[0] + ";");
				writer.println();
				//di ogni volo scrive le altre agenzie con prezzi peggiori rispetto alla migliore
				//prima faccio un controllo perche' potrebbero non esserci agenzie oltre la migliore che vendono lo stesso volo
				if(otheragencies.get(i).select("a").size()!=0){
					for (j = 0; j < otheragencies.get(i).select("a").size(); j++) {
						//splitto la stringa in modo da avere il nome dell'agenzia separato dal prezzo, inoltre anche perche' 
						//a volte nel box delle agenzie c'e' anche la stessa compagnia che vende il biglietto che non haa fianco
						//segnato il prezzo, in questo modo faccio si che non venga introdotto nella lista delle ota
						String[] words = otheragencies.get(i).select("a").get(j).text().split(" ");
						lenghtWords = words.length;
						if(lenghtWords > 2){
							//scrive nome volo,aeroporta andata, destinazione, giorno andata, giorno ritorno
							writer.print(flights.get(i).text() + ";");
							writer.print(departureAirportGo + ";");
							writer.print(stopsGo + ";");
							writer.print(arrivalAirportGo + ";");
							writer.print(departureAirportBack + ";");
							writer.print(stopsBack + ";");
							writer.print(arrivalAirportBack + ";");
							writer.print(datepart + ";");
							writer.print(daterit + ";");
							writer.print(departureTimeGo + ";");
							writer.print(arrivalTimeGo + ";");
							writer.print(departureTimeBack + ";");
							writer.print(arrivalTimeBack + ";");
							//Scrive nome agenzia e prezzo (di quelle proposte come alternativa
							//Ci pu� essere il caso in cui il nome di un aagenzia sia composto da diverse parole divise da uno spazio
							//Il formato della stringa � NOME_OTA PREZZO �
							price = words[lenghtWords-2];
							ota = "";
							for (k=0; k<(lenghtWords-2); k++) {
								ota = ota + words[k];
							}
							writer.print(ota + ";");
							writer.print(price + ";");
							
							writer.println();
						}
					}
				}
			}
			
			x++;
		}
			
		writer.close();	
		
		webClient.close();

		System.out.println("Done.");

	}
}


