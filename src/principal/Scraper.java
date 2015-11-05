package principal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.background.JavaScriptJobManager;
import com.mchange.v2.resourcepool.ResourcePool.Manager;

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
	Document doc = null;
	WebClient webClient;
	File input;
	PrintWriter out;
	boolean checkCorrectness = true;
	
	int contatorefile = 0;

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
		
		webClient = new WebClient(BrowserVersion.CHROME);
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
		webClient.getCookieManager().setCookiesEnabled(false);
		
		jdMap = jeContext.getJobDetail().getJobDataMap();
		pagesNumber = (int) jdMap.get("pagesNumber");
		domain = "UK";
	    
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
    		try {
				startQuery(datepart, daterit, airport_part, airport_dest, numberPass, pagesNumber, domain);
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
		
		webClient.close();

		doublePrint("Done.");

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
		otheragencies = doc.select("div.details-group-altquotes");
		//Salvo tutti gli scali
		stops = doc.select("div.leg-stops");
		checkDoubleBooking = doc.select("div.mainquote-wrapper.clearfix");
	}
	
	private void writeCSVfile(String domain, int numPass, String datepart, String daterit, String dateToString, String fileName, String pathDirectory){
		
		String[] otherAgenciesWords;
		String[] departureGoWords;
		String[] arrivalGoWords;
		String[] departureBackWords;
		String[] arrivalBackWords;
		String[] stopsGoWords;
		String[] stopsBackWords;
		String[] bestAgenciesPriceWords;
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
				writer = new PrintWriter(pathDirectory + "/" + fileName + ".csv");
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
		
			
			//creo una lista con tutti gli arrivi di tutti i voli
			for (Element el : arrive) {
				arriveList.add(el.text());
				}
			
			//creo una lista delle agenzie con i migliori prezzi per ciascuno volo (solo nome agenzia)
			for (Element el : bestagencies) {
				bestagencieslist.add(el.text());
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
				
				//OTA
				//scrive nome e prezzo bestagency per il volo
				if (checkDoubleBooking.get(i).text().toString().contains("required")) {
					doublebookings++;
					writer.print(airlineCompany + ";");					
				} else {
					writer.print(bestagencieslist.get(i-doublebookings) + ";");
				}
				
				//PRICE
				//Scrive il prezzo della miglior agenzia
				
				bestAgenciesPriceWords = checkDoubleBooking.get(i).text().toString().split(" ");
				//Se ho più passeggeri, ho due diversi prezzi per le best agencies
				//Nel caso di 1 passeggero avrei PREZZO AGENZIA
				//Nel caso di più passeggeri avrei PREZZO_TOTALE totale PREZZO_SINGOLO AGENZIA
				//Nel caso contenga una doppia prenotazione abbiamo altri due casi
				//Nel caso di 1 passeggero avrei PREZZO Select 2 bookings required
				//Nel caso di più passeggeri PREZZO_TOTALE total PREZZO_SINGOLO Select 2 bookings required
				if(numPass == 1) {
					writer.print(bestAgenciesPriceWords[0] + ";");
				} else {
					writer.print(bestAgenciesPriceWords[2] + ";");
				}
			
//				writer.print(bestagenciespriceslist.get(i) + ";");
				writer.println();
				
				//faccio un controllo perche' potrebbero non esserci agenzie oltre la migliore che vendono lo stesso volo
				if(otheragencies.get(i).select("a").size()!=0){
					for (j = 0; j < otheragencies.get(i).select("a").size(); j++) {
						//splitto la stringa in modo da avere il nome dell'agenzia separato dal prezzo, inoltre
						//a volte nel box delle agenzie c'e' la stessa compagnia che vende il biglietto senza avere
						//segnato il prezzo
						
						otherAgenciesWords = otheragencies.get(i).select("a").get(j).text().split(" ");
						lenghtWords = otherAgenciesWords.length - 1; //Non tengo conto del simbolo dell'euro
						//Controllo se contiene la stringa "Caricamento in corso.."
						if(otheragencies.get(i).select("a").toString().contains("Loading")){
							checkCorrectness = false;
						} else {
							//Esistono casi in cui la OTA venga presentata senza prezzo
							//Perchè la stessa compagnia presentata come best agencies
							
							//Scrive nome agenzia e prezzo (di quelle proposte come alternativa
							//Ci puo' essere il caso in cui il nome di un aagenzia sia composto da diverse parole
							//divise da uno spazio
							//Il formato della stringa e' NOME_OTA PREZZO
							
							price = otherAgenciesWords[lenghtWords-1];
							ota = "";
							for (k=0; k<(lenghtWords-1); k++) {
								ota = ota + otherAgenciesWords[k];
							}
							
//							//Se l'ultima parola non contiene numeri, non devo tenere conto dell'ota
							if (isNumeric(price)) {
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

		String from = "tesiericmaria@gmail.com";
		String pass = "t4L2bBd3x3a5ZGrG";
		String port  = "587";
		String[] to = { "Ayero.Maria@hotmail.it", "ericg@live.it" }; // list of recipient email addresses

		Properties props = System.getProperties();
		String host = "smtp.gmail.com";
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.user", from);
		props.put("mail.smtp.password", pass);
		props.put("mail.smtp.port", port);
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.socketFactory.port", port);
		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.socketFactory.fallback", "false");


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
		} catch (MessagingException me) {
			me.printStackTrace();

		}
    }
	
	private void deleteFile(String fileName, String pathDirectory) {
		try{
    		File file = new File(pathDirectory + "/" + fileName + ".csv");
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
			File file =new File(pathDirectory + "/" + fileName + ".csv");
 
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
	
	private void startQuery(String datepart, String daterit, String airport_part, String airport_dest, int numberPass, int pagesNumber, String domain) throws IOException {
		
		String fileName;
		String pathDirectory;
		Date date = new Date();
		String newFormat;
		SimpleDateFormat oldFormat;
		String dateToString;
		String counterPath = "CountLessThanZero";
		
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
		
		doublePrint("Ricerca per aeroporto di partenza: "+ airport_part);
		doublePrint("Ricerca per aeroporto di destinazione: "+ airport_dest);
		doublePrint("Ricerca per data di partenza: "+ datepart);
		doublePrint("Ricerca per aeroporto di ritorno: "+ daterit);
		doublePrint("Ricerca per numero di passeggeri: "+ numberPass);
		doublePrint("Pagine richieste: " + pagesNumber);
		doublePrint("");
		
		//String URL = ("http://www.skyscanner.dk/trasporti/voli/" + airport_part + "/" + airport_dest + "/" + datepart + "/" + daterit + "/");
		String URL = ("http://www.skyscanner.net/transport/flights/" + airport_part + "/" + airport_dest + "/" + datepart + "/" + daterit + "/?adults=" + numberPass + "&children=0&infants=0&cabinclass=economy&rtn=1&preferdirects=false&outboundaltsenabled=false&inboundaltsenabled=false&market=" + domain + "&locale=en-GB&currency=EUR&_ga=1.48008167.1740910371.1433161688");
		
		HtmlPage page = null;
		try {
			page = webClient.getPage(URL);
		} catch (Exception e) {
			out.println("Eccenzione nel caricamento della pagina - " + e.toString() + "\n");
		}
		
		
		out.println("Pagina richiesta tramite il comando: page = webClient.getPage(URL);");
		
		fileName = dateToString + "_" + datepart + "_" + daterit + "_" + airport_part + "_" + airport_dest + "_" + "pass" + numberPass;
		pathDirectory = datepart + "_" + daterit + "_" + airport_part + "_" + airport_dest + "_" + "pass" + numberPass;
		
		JavaScriptJobManager manager = page.getEnclosingWindow().getJobManager();
		
		int timeToWait = 90;
		int attemps = 1;
		
		while (manager.getJobCount() < 0) {
			out.println("JobCount minore di zero. Prova di caricamento della pagina in corso.");
			out.println("Tentativo numero: " + attemps);
			attemps++;
			try {
				page = webClient.getPage(URL);
				webClient.getCache().clear();
			} catch (Exception e) {
				out.println("Eccenzione nel caricamento della pagina - " + e.toString() + "\n");
			}
			
			out.println("Pagina richiesta tramite il comando: page = webClient.getPage(URL);");
			
			manager = page.getEnclosingWindow().getJobManager();
			
			if(attemps >= 5)
				break;
		}
		
		if (manager.getJobCount() < 0) {
			out.println("manager.getJobCount() <= 0 --> Non entra nel while nonostante i 5 tentativi.");
			sendMail("TESI: jobCount < 0", "Nella tratta " + airport_part + " " + airport_dest+ " " + datepart + " "+ daterit+ " " + numberPass + "è stato trovato"
					+ "il jobCount minore uguale a 0 nella prima pagina, nonostante i 5 tentativi.");
			out.println("L'URL è il seguente: " + URL);
		}
		
		while (manager.getJobCount() >= 0) {
			out.println("manager.getJobCount() > 0 --> Entrato correttamente nel while per la prima pagina.");
			timeToWait--;
			doublePrint(timeToWait + " seconds left... ("
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
		out.println("Salvo la prima pagina in formato HTML.");
		wr.println(page.asXml());
		wr.close();
		
		String pathCopiaFile = "Copia file";
		createNewDirectory(pathCopiaFile);
		File source2 = new File("Page1AsXml.html");
        File dest2 = new File( pathCopiaFile + "/" + fileName + "Page1AsXml.html");
		Files.copy(source2.toPath(), dest2.toPath());
		
		if (manager.getJobCount() < 0) {
			out.println("Salvo la pagina HTML nella cartella a parte visto il jobCount minore di zero.");
			createNewDirectory(counterPath);
			File source = new File("Page1AsXml.html");
	        File dest = new File( counterPath + "/" + fileName + "Page1AsXml.html");
			Files.copy(source.toPath(), dest.toPath());
		}
		
		//Salvo tutte le pagine della ricerca in formato html, dopodichÃ¨ in uno step successivo ne farÃ² lo scraping
		out.println("Vado a salvare le pagine successive alla prima.");
		DomNodeList<DomElement> domList = page.getElementsByTagName("button");
		
		int currentPage = 2;
		
		while( !domList.isEmpty() && currentPage <= pagesNumber ){
			System.out.println("Sono entrato nel ciclo");
			
			try {
				System.out.println("Sto caricando la seconda pagina.");
				for (DomElement domElement : domList) {
					String attr = domElement.getAttribute("title");
					if(attr.equals("Next page")){
						HtmlElement nextPageButton = (HtmlElement) domElement;
						nextPageButton.click();
					} else {
						out.println("Non è stato trovato il bottone 'Next page', quindi non è stata salvata la seconda pagina");
					}
					
				}
			} catch (IOException e1) {
				e1.printStackTrace();
				out.println("Eccenzione nel cambiare il numero della pagina - " + e1.toString() + "\n");
			}
			
			manager = page.getEnclosingWindow().getJobManager();
			
			timeToWait = 60;
//			if (manager.getJobCount() < 0) {
//				out.println("manager.getJobCount() <= 0 --> Non entra nel while per la pagina " + currentPage);
//				sendMail("TESI: jobCount < 0", "Nella tratta " + airport_part + " " + airport_dest+ " " + datepart + " "+ daterit+ " " + numberPass + "è stato trovato"
//						+ "il jobCount minore uguale a 0 per la pagina n. " + currentPage + ".");
//			}
			
			System.out.println(manager.getJobCount());
			while (manager.getJobCount() >= 0) {
				out.println("manager.getJobCount() > 0 --> Entrato correttamente nel while per la pagina numero " + currentPage);
	        	timeToWait--;
	   			doublePrint(timeToWait + " seconds left... ("
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
	        out.println("Salvo la pagina n. " + currentPage + " in formato HTML.");
	        wr3.println(page.asXml());
	        wr3.close();
	        
	        source2 = new File("Page" + currentPage + "AsXml.html");
	        dest2 = new File( pathCopiaFile + "/" + fileName + "Page" + currentPage + "AsXml.html");
			Files.copy(source2.toPath(), dest2.toPath());
	        
//	        elements = (List<HtmlElement>) page.getByXPath("//*[@id=\"cbp-pagination\"]/div[2]/ul/li/button[@title=\"Next page\"]");
	        currentPage++;
		}
		
		if (manager.getJobCount() < 0) {
			out.println("Salvo la pagina n." + currentPage + " HTML nella cartella a parte visto il jobCount minore di zero.");
			
			File source = new File("Page" + currentPage + "AsXml.html");
	        File dest = new File(counterPath + "/" + fileName + "Page" + currentPage + "AsXml.html");
			Files.copy(source.toPath(), dest.toPath());
		
		}
		
		out.println("Tutte le pagine sono state salvate.");
		
		//Elimino le pagine create in una precedente interrogazione
		out.println("Elimino le pagine HTML di troppo create nella precedente interrogazioni");
		int p = currentPage;
		while (new File("Page" + p + "AsXml.html").exists()){
			new File("Page" + p + "AsXml.html").delete();
			p++;
		}
		
		//Scrive il file csv contenente tutti i voli e le ota
		out.println("Faccio il parsing delle pagine e salvo i dati in csv.");
		writeCSVfile(domain, numberPass, datepart, daterit, dateToString, fileName, pathDirectory);
		
		//Controlla se il file contiene la stringa "Caricamento in corso"
		if (!checkCorrectness) {
			out.println("File csv eliminato perchè conteneva 'Caricamento in corso'");
			deleteFile(fileName, pathDirectory);
			String emptyPath = "emptyfiles";
			out.println("Salvo file html in una cartella a parte.");
			createNewDirectory(emptyPath);
			int pagecounter;
			for(pagecounter = 1; pagecounter <= pagesNumber; pagecounter++) {
					
				File source = new File("Page" + pagecounter + "AsXml.html");
		        File dest = new File(emptyPath + "/" + fileName + "Page" + pagecounter + "AsXml.html");
				Files.copy(source.toPath(), dest.toPath());
				
			}
		}
		
		//Controlla se il file è vuoto
		int lines = readLines(fileName, pathDirectory);
		out.println("Controllo il numero delle righe del file csv salvato.");
		if(lines <= 2) {
			out.println("Il documento csv è vuoto.");
			deleteFile(fileName, pathDirectory);
			sendMail("TESI: Documento excell vuoto", "Il contenuto del file excell non è stato pervenuto. Il file è stato eliminato.");
			String emptyPath = "emptyfiles";
			out.println("Salvo file html in una cartella a parte.");
			createNewDirectory(emptyPath);
			int pagecounter;
			for(pagecounter = 1; pagecounter <= pagesNumber; pagecounter++) {
					
				File source = new File("Page" + pagecounter + "AsXml.html");
		        File dest = new File(emptyPath + "/" + fileName + "Page" + pagecounter + "AsXml.html");
				Files.copy(source.toPath(), dest.toPath());
				
			}
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
	
	private static boolean isNumeric(String str) {  		
		NumberFormat formatter = NumberFormat.getInstance();
		ParsePosition pos = new ParsePosition(0);
		formatter.parse(str, pos);
		return str.length() == pos.getIndex();
	}
	
	private void doublePrint(String string) {
		System.out.println(string);
		out.println(string);
	}
	
}