package principal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.quartz.Job;
import org.quartz.JobDataMap;
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

public class Scraper implements Job {

	static {
		// disable HtmlUnit logging
		LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log",
				"org.apache.commons.logging.impl.NoOpLog");
	}
	
	Elements otheragencies = new Elements();
//	Elements bestagenciesprices = new Elements();
	Elements bestagencies = new Elements();
	Elements flights = new Elements();
	Elements depart = new Elements();
	Elements arrive = new Elements();
	Elements stops = new Elements();
	Elements checkDoubleBooking = new Elements();
	ArrayList<String> departList = new ArrayList<String>();
	ArrayList<String> arriveList = new ArrayList<String>();
	ArrayList<String> bestagencieslist = new ArrayList<String>();
//	ArrayList<String> bestagenciespriceslist = new ArrayList<String>();
//	ArrayList<String> otherAgenciesNameList = new ArrayList<String>();
//	ArrayList<String> otherAgenciesPriceList = new ArrayList<String>();
	Document doc = null;
	WebClient webClient;
	File input;
	PrintWriter out;
	boolean checkCorrectness = true;
	
	public void execute(JobExecutionContext jeContext) throws JobExecutionException {
		
		JobDataMap jdMap;
		String airport_part;
		String airport_dest;
		String datepart;
		String daterit;
		String [] queryWords;
		int pagesNumber;
		int numberPass;
		String domain;
		BufferedReader dataFile = null;
	    List<String> lines = new ArrayList<>();
		
		//parte il ciclo di interrogazione a skyscanner
		System.out.println();
		System.out.println("GO!");
		System.out.println();
		
		webClient = new WebClient(BrowserVersion.FIREFOX_31);
		webClient.getOptions().setJavaScriptEnabled(true);
		webClient.getOptions().setRedirectEnabled(true);
		webClient.getOptions().setCssEnabled(true);
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		//posso mettere questo a false e non da piu' l'eccezione 
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setUseInsecureSSL(true);
		webClient.setAjaxController(new NicelyResynchronizingAjaxController());
		webClient.waitForBackgroundJavaScriptStartingBefore(10000);
		webClient.setJavaScriptTimeout(2147483647);
		webClient.setCssErrorHandler(new SilentCssErrorHandler());
		webClient.waitForBackgroundJavaScript(5000);
		webClient.setRefreshHandler(new ThreadedRefreshHandler());
		webClient.getCookieManager().setCookiesEnabled(true);
		
		jdMap = jeContext.getJobDetail().getJobDataMap();
		pagesNumber = (int) jdMap.get("pagesNumber");
		domain = "http://www.skyscanner.dk/";
		
				
	    
		try {
			dataFile = new BufferedReader(new FileReader("dataFile.csv"));
		} catch (FileNotFoundException e) {
			out.println("Errore nel caricamento del file di lettura dei dati.");
		}
    	
    	String line = null;
    	try {
			while ((line = dataFile.readLine()) != null) {
			    lines.add(line);
			}
		} catch (IOException e) {
			out.println("Errore nel caricamento del file di lettura dei dati.");
		}
    	
    	for(String query : lines) {
    		queryWords = query.split(";");
    		
    		datepart = queryWords[0];
    		daterit = queryWords[1];
    		airport_part = queryWords[2];
    		airport_dest = queryWords[3];
    		numberPass = Integer.parseInt(queryWords[4]);
    		
    		//Inizio ad interrogare le pagine html e salvo i dati su un file csv
    		startQuery(datepart, daterit, airport_part, airport_dest, numberPass, pagesNumber, domain);
    	}
		
		webClient.close();

		System.out.println("Done.");

	}
	
	private void clearAll() {
		flights.clear();
		depart.clear();
		arrive.clear();
		bestagencies.clear();
//		bestagenciesprices.clear();
		otheragencies.clear();
		departList.clear();
		arriveList.clear();
		bestagencieslist.clear();
//		bestagenciespriceslist.clear();
//		otherAgenciesNameList.clear();
//		otherAgenciesPriceList.clear();
		stops.clear();
		checkDoubleBooking.clear();
	}
	
	private void selectElements(){
		//salvo gli element appropriati all'interno delle liste di elements (prendo gli elementi dalla pagina html) da cui poi prenderÃ² le informazioni che mi servono
		flights = doc.getElementsByClass("airline");
		//Il formato che ottengo con go ï¿½ ORA AEROPORTO per l'andata
		depart = doc.select("div.depart");
		//Il formato che ottengo con go ï¿½ ORA AEROPORTO per il ritorno
		arrive = doc.select("div.arrive");
		bestagencies = doc.select("div.mainquote-wrapper.clearfix").select("a.ticketing-agent.mainquote-agent");
		//bestagenciesprices = doc.select("div.mainquote-wrapper.clearfix").select("a.mainquote-price.big");
		otheragencies = doc.select("div.details-group.clearfix");
		//Salvo tutti gli scali
		stops = doc.select("div.leg-stops");
		checkDoubleBooking = doc.select("div.mainquote-wrapper.clearfix");
	}
	
	private void writeCSVfile(String domain, String datepart, String daterit, String dateToString, String fileName, String pathDirectory){
		
		String[] otherAgenciesWords;
		String[] departureGoWords;
		String[] arrivalGoWords;
		String[] departureBackWords;
		String[] arrivalBackWords;
		String[] stopsGoWords;
		String[] stopsBackWords;
		String[] bestAgenciesWords;
		String airlineCompany;
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
//		String pathDirectory;
//		File fileTemp;
		int i;
		int j;
		int k;
		int x=1;
		int doublebookings = 0;
		int lenghtWords;
		
		//crea file di testo dove salvera' tutti i risultati dell'attuale ricerca
		PrintWriter writer = null;
		try {
			//String dateToString = date.toString();
			if( createNewDirectory(pathDirectory) ){
				writer = new PrintWriter(pathDirectory + "\\" + fileName + ".csv");
			}
		} catch (FileNotFoundException e) {
			out.println("Eccenzione nella creazione del file csv - " + e.toString() + "\n");
			e.printStackTrace();
		}  
		
		//scrive data e ora della query al sito
		
		writer.println(dateToString.replaceAll("_", "-"));
	
		//intestazioni della tabella
		writer.println("Flight;DepartureAirportGo;StopsGo;ArrivalAirportGo;DepartureAirportBack;StopsBack;ArrivalAirportBack;DateGo;DateBack;DeparturetimeGo;ArrivalTimeGo;DeparturetimeBack;ArrivalTimeBack;OTA;Price;");
				
		//qui parte il ciclo per lo scraping, per ogni file che ho ne faccio lo scraping
		while (new File("Page" + x + "AsXml.html").exists()){
			
			input = new File("Page" + x + "AsXml.html");
			doc = null;
			try {
				doc = Jsoup.parse(input, "UTF-8", domain);
			} catch (IOException e) {
				out.println("Eccenzione nel parsing della pagina - " + e.toString() + "\n");
				e.printStackTrace();
			}
			
			//elimino gli elementi precedentemente messi nella lista in modo da non creare problemi per la nuova scrittura
			clearAll();
			
			int b=0;
			doublebookings = 0;
			
			//Faccio il parsing della pagina e seleziono solo i dati che mi interessano
			selectElements();
			
			//creo una lista con tutte le partenze di tutti i voli
			for (Element el : depart) {
				departList.add(el.text());
				if (departList.get(b).contains("Outbound")){
					departList.remove(b);
					}
				else{
				b++;
				}
			}
			
//			int lengthOtherAgencyWord = 0;
//			String nameOTA;
//			int t;
//			//Creo una lista per gestire le altre agenzie
//			for (Element el : otheragencies) {
//				for (Element singleEl : el.select("a")) {
//					//Controllo se contiene la stringa "Caricamento in corso.."
//					if (singleEl.text().contains("Loading")) {
//						checkCorrectness = false;
//					} else {
//						otherAgenciesWords = singleEl.text().split(" ");
//						lengthOtherAgencyWord = otherAgenciesWords.length;
//						//Ultimo elemento è il prezzo
//						otherAgenciesPriceList.add(otherAgenciesWords[lengthOtherAgencyWord - 1].substring(1));
//						nameOTA = "";
//						for (t = 0; t < lengthOtherAgencyWord - 1; t++) {
//							nameOTA = nameOTA + otherAgenciesWords[t];
//						}
//						otherAgenciesNameList.add(nameOTA);
//					}
//					
//				}
//			}
			
			//creo una lista con tutti gli arrivi di tutti i voli
			for (Element el : arrive) {
				arriveList.add(el.text());
				}
			
			//creo una lista delle agenzie con i migliori prezzi per ciascuno volo (solo nome agenzia)
			for (Element el : bestagencies) {
				bestagencieslist.add(el.text());
				}
				
//			//creo una lista delle agenzie con i migliori prezzi per ciascuno volo (solo prezzo agenzia)
//			for (Element el : bestagenciesprices) {
//				bestagenciespriceslist.add(el.text());
//				System.out.println("prezzo: " + el.text());
//				}
			
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
				//Il formato della stringa per gli scali puï¿½ essere
				//In caso di nessun scalo DIRETTO
				//In caso di 1 o piï¿½ scali # SCALO LISTA_DI_CITTA'
				stopsGoWords = stops.get(2*i).text().toString().split(" ");		
				stopsGo = "";				
				//Nel caso in cui c'e' almeno uno scalo vai a prendere dalla terza parola in poi
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
				//Nel caso si abbia arrivalGoWords = "22:05 (+1) SYD"
				if (arrivalGoWords.length == 3) {
					arrivalTimeGo = arrivalGoWords[0] + arrivalGoWords[1];
				} else {
					arrivalTimeGo = arrivalGoWords[0];
				}
				writer.print(arrivalTimeGo + ";");
				
				//DEPARTURE TIME BACK
				departureTimeBack = departureBackWords[0];
				writer.print(departureTimeBack + ";");
				
				//ARRIVAL TIME BACK
				//Nel caso si abbia arrivalBackWords = "08:25 (+1) MXP"
				if (arrivalBackWords.length == 3) {
					arrivalTimeBack = arrivalBackWords[0] + arrivalBackWords[1];
				} else {
					arrivalTimeBack = arrivalBackWords[0];
				}
				writer.print(arrivalTimeBack + ";");
				
				//scrive nome e prezzo bestagency per il volo
				if (checkDoubleBooking.get(i).text().toString().contains("required")) {
					doublebookings++;
					writer.print(airlineCompany + ";");					
				} else {
					writer.print(bestagencieslist.get(i-doublebookings) + ";");
				}
				
				
				//Scrive il prezzo della miglior agenzia
				bestAgenciesWords = checkDoubleBooking.get(i).text().toString().split(" ");
				//Se ho più passeggeri, ho due diversi prezzi per le best agencies
				//Nel caso di 1 passeggero avrei PREZZO AGENZIA
				//Nel caso di più passeggeri avrei PREZZO_TOTALE totale PREZZO_SINGOLO AGENZIA
				if( bestAgenciesWords.length < 4) {
					writer.print(bestAgenciesWords[0].substring(1) + ";");
				} else {
					writer.print(bestAgenciesWords[2].substring(1) + ";");
				}
				
//				writer.print(bestagenciespriceslist.get(i) + ";");
				writer.println();
				//di ogni volo scrive le altre agenzie con prezzi peggiori rispetto alla migliore
				//prima faccio un controllo perche' potrebbero non esserci agenzie oltre la migliore che vendono lo stesso volo
				if(otheragencies.get(i).select("a").size()!=0){
					for (j = 0; j < otheragencies.get(i).select("a").size(); j++) {
						//splitto la stringa in modo da avere il nome dell'agenzia separato dal prezzo, inoltre
						//a volte nel box delle agenzie c'e' la stessa compagnia che vende il biglietto senza avere
						//segnato il prezzo
						otherAgenciesWords = otheragencies.get(i).select("a").get(j).text().split(" ");
						lenghtWords = otherAgenciesWords.length;
						//Controllo se contiene la stringa "Caricamento in corso.."
						if(otheragencies.get(i).select("a").toString().contains("Loading")){
							checkCorrectness = false;
						} else {
							if(lenghtWords >= 2){
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
								//Ci puo' essere il caso in cui il nome di un aagenzia sia composto da diverse parole
								//divise da uno spazio
								//Il formato della stringa e' NOME_OTA PREZZO
								price = otherAgenciesWords[lenghtWords-1].substring(1);
								ota = "";
								for (k=0; k<(lenghtWords-1); k++) {
									ota = ota + otherAgenciesWords[k];
								}
								writer.print(ota + ";");
								writer.print(price + ";");
								writer.println();
							}
						}
						
					}
				}
				
			}
			
			x++;
		}
			
		writer.close();
	}
	
	private void sendMail(String subject, String body) {
		
		//tesiericmaria@gmail.com
		//t4L2bBd3x3a5ZGrG
		
		String from = "tesiericmaria";
		String pass = "t4L2bBd3x3a5ZGrG";
		String[] to = { "Ayero.Maria@hotmail.it", "ericg@live.it" }; // list of recipient email addresses

	    Properties props = System.getProperties();
        String host = "smtp.gmail.com";
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.user", from);
        props.put("mail.smtp.password", pass);
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");

        Session session = Session.getDefaultInstance(props);
        MimeMessage message = new MimeMessage(session);

        try {
            message.setFrom(new InternetAddress(from));
            InternetAddress[] toAddress = new InternetAddress[to.length];

            // To get the array of addresses
            for( int i = 0; i < to.length; i++ ) {
                toAddress[i] = new InternetAddress(to[i]);
            }

            for( int i = 0; i < toAddress.length; i++) {
                message.addRecipient(Message.RecipientType.TO, toAddress[i]);
            }

            message.setSubject(subject);
            message.setText(body);
            Transport transport = session.getTransport("smtp");
            transport.connect(host, from, pass);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
        } catch (AddressException ae) {
        	ae.printStackTrace();
        	out.println("Eccezione nell'indirizzo mail - " + ae.toString());
        } catch (MessagingException me) {
        	me.printStackTrace();
        	out.println("Eccezione nell'invio della mail - " + me.toString());
        	
        }
    }
	
	private void deleteFile(String fileName, String pathDirectory) {
		try{
    		File file = new File(pathDirectory + "\\" + fileName + ".csv");
    		if(file.delete()){
    			out.println(file.getName() + " è stato eliminato!");
    		}else{
    			out.println("L'operazione di eliminazione del file è fallita.");
    		}
    	}catch(Exception e){
    		e.printStackTrace();
    		out.println("Eccenzione nell'eminazione del file (" + fileName + ") - " + e.toString() + "\n");
    	}
	}
	
	private int readLines(String fileName, String pathDirectory) {
		try{
			File file =new File(pathDirectory + "\\" + fileName + ".csv");
 
    		if(file.exists()){
    		    FileReader fr = new FileReader(file);
    		    LineNumberReader lnr = new LineNumberReader(fr);
    		    
    		    int linenumber = 0;
 
    		    while (lnr.readLine() != null){
    		    	linenumber++;
    	        }
    		    lnr.close();
    		    return linenumber;
    		}
 
    	}catch(IOException e){
    		e.printStackTrace();
    	}
		return 0;
 
	}
	
	private void startQuery(String datepart, String daterit, String airport_part, String airport_dest, int numberPass, int pagesNumber, String domain) {
		
		String fileName;
		String pathDirectory;
		Date date = new Date();
		String newFormat;
		SimpleDateFormat oldFormat;
		String dateToString;
		
		oldFormat = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss");
		newFormat = oldFormat.format(date);
		dateToString = newFormat.replaceAll(":", ".");
		dateToString = newFormat.replaceAll("/", ".");
		
		File log = new File("log.txt");
		out = null;
	    try{
	    	if (!log.exists()) {
	            log.createNewFile();
	    	}
		    out = new PrintWriter(new FileWriter(log, true));
		    out.println("******* " + dateToString +"******* " + "\n");
	    } catch(IOException e) {
	        System.out.println("COULD NOT LOG!!");
	    }
		
		System.out.println("Ricerca per aeroporto di partenza: "+ airport_part);
		System.out.println("Ricerca per aeroporto di destinazione: "+ airport_dest);
		System.out.println("Ricerca per data di partenza: "+ datepart);
		System.out.println("Ricerca per aeroporto di ritorno: "+ daterit);
		System.out.println("Ricerca per numero di passeggeri: "+ numberPass);
		System.out.println("Pagine richieste: " + pagesNumber);
		System.out.println();
		
		//String URL = ("http://www.skyscanner.it/trasporti/voli/" + airport_part + "/" + airport_dest + "/" + datepart + "/" + daterit + "/");
		String URL = (domain + "transport/flights/" + airport_part + "/" + airport_dest + "/" + datepart + "/" + daterit + "/?adults=" + numberPass + "&children=0&infants=0&cabinclass=economy&rtn=1&preferdirects=false&outboundaltsenabled=false&inboundaltsenabled=false&market=DE&locale=en-GB&currency=EUR&_ga=1.48008167.1740910371.1433161688");
		
		HtmlPage page = null;
		try {
			page = webClient.getPage(URL);
		} catch (Exception e) {
			out.println("Eccenzione nel caricamento della pagina - " + e.toString() + "\n");
		}
		
		JavaScriptJobManager manager = page.getEnclosingWindow().getJobManager();
		int timeToWait = 60;
		while (manager.getJobCount() > 0) {
			timeToWait--;
			System.out.println(timeToWait + " seconds left... ("
					+ manager.getJobCount() + " jobs left)\n");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				out.println("Eccenzione sleep del thread - " + e.toString() + "\n");
			}
			if (timeToWait <= 0)
				break;
		}
		
		//salvo la prima pagina in formato html
		
				PrintWriter wr = null;
				try {
					wr = new PrintWriter("Page1AsXml.html");
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
					out.println("Eccenzione nella creazione della documento XML (Page1asXML) - " + e1.toString() + "\n");
				}
				wr.println(page.asXml());
				wr.close();
				
				
				//Salvo tutte le pagine della ricerca in formato html, dopodichÃ¨ in uno step successivo ne farÃ² lo scraping
				  
				List<HtmlElement> elements = (List<HtmlElement>) page.getByXPath("//*[@id=\"cbp-pagination\"]/div[2]/ul/li/button[@title=\"Next page\"]");
				int currentPage = 2;
				while( !elements.isEmpty() && currentPage <= pagesNumber ){
					HtmlElement element1 = elements.get(0);
					
					try {
						page = element1.click();
					} catch (IOException e1) {
						e1.printStackTrace();
						out.println("Eccenzione nel cambiare il numero della pagina - " + e1.toString() + "\n");
					}
					timeToWait = 10;
			        while (manager.getJobCount() > 0) {
			        	timeToWait--;
			   			System.out.println(timeToWait + " seconds left... ("
			   					+ manager.getJobCount() + " jobs left)\n");
			   			try {
			   				Thread.sleep(1000);
			   			} catch (InterruptedException e) {
			   				e.printStackTrace();
			   				out.println("Eccenzione sleep del thread - " + e.toString() + "\n");
			   			}
			   			if (timeToWait <= 0) {
			   				break;
			   			}	
			        }
			        
			        PrintWriter wr3 = null;
			        try {
			        	wr3 = new PrintWriter("Page" + currentPage + "AsXml.html");
			        } catch (FileNotFoundException e) {
			        	e.printStackTrace();
			        	out.println("Eccenzione nella creazione del documento XML (Page" + currentPage + "AsXml) - " + e.toString() + "\n");
			        }
			        wr3.println(page.asXml());
			        wr3.close();
			        elements = (List<HtmlElement>) page.getByXPath("//*[@id=\"cbp-pagination\"]/div[2]/ul/li/button[@title=\"Next page\"]");
			        currentPage++;
				}
				
				//Elimino le pagine create in una precedente interrogazione
				int p = currentPage;
				while (new File("Page" + p + "AsXml.html").exists()){
					new File("Page" + p + "AsXml.html").delete();
					p++;
				}
				
				//Scrive il file csv contenente tutti i voli e le ota
				fileName = dateToString + "_" + datepart + "_" + daterit + "_" + airport_part + "_" + airport_dest + "_" + "pass" + numberPass;
				pathDirectory = datepart + "_" + daterit + "_" + airport_part + "_" + airport_dest + "_" + "pass" + numberPass;
				writeCSVfile(domain, datepart, daterit, dateToString, fileName, pathDirectory);
				
				//Controlla se il file contiene la stringa "Caricamento in corso"
				if (!checkCorrectness) {
					out.println("File csv eliminato perchè conteneva 'Caricamento in corso'");
					deleteFile(fileName, pathDirectory);
				}
				
				//Controlla se il file è vuoto
				int lines = readLines(fileName, pathDirectory);
				
				if(lines <= 2) {
					out.println("Il documento csv è vuoto.");
					deleteFile(fileName, pathDirectory);
					sendMail("TESI: Documento excell vuoto", "Il contenuto del file excell non è stato pervenuto. Il file è stato eliminato.");
				}
				
				out.close();
	}
	
	//Metodo che crea una nuova cartella se non esiste già
	private boolean createNewDirectory(String folderPath) {
		File file = new File( folderPath );
		
		if( !file.exists() ) {
			if( !file.mkdirs() ) {
				out.print("Problema nel creare la nuova cartella");
				return false;
			}
		}
		return true;
	}

}