package principal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;


public class QuartzMain {	
	
	public QuartzMain(int pages, int frequency, String endate) throws Exception{
		
		SchedulerFactory sf = new StdSchedulerFactory();
		Scheduler sched = sf.getScheduler();
		JobDetail jd = newJob(Scraper.class)
			    .withIdentity("job1", "group1")
			    .build();
		
		jd.getJobDataMap().put("pagesNumber", pages);
	
		//settiamo la data di interruzione dello scraper
		//prima splitto la stringa che ho ricevuto come data che sarà del tipo 150529
		String[] endatesplitted = endate.split("(?<=\\G.{2})");
		//primo elemento lo metto negli anni,poi mesi, poi giorni
		int yy = Integer.parseInt(endatesplitted[0]);
		int mm = Integer.parseInt(endatesplitted[1]);
		int dd = Integer.parseInt(endatesplitted[2]);
		
		//creo la data aggiungo all'anno 2000 così da avere 15+2000=2015 e tolgo uno al mese perchè la data è nel formato 0-11 quindi se siamo a maggio dovrò avere 4 come numero e non 5
		java.util.Calendar cal = new java.util.GregorianCalendar(yy + 2000, mm-1, dd);
		//prendo la data del calendario e la metto in formato Date che è quello riconosciuto dal trigger
		Date endTime = cal.getTime();
		
		System.out.println("The scraper will end on " +  endTime);
		// definisco il trigger con il suo nome, gruppo, data partenza, ogni quanto fa l'interrogazione e fine
		Trigger ct = newTrigger()
			    .withIdentity("trigger1", "group1")
			    .startNow()
			    .withSchedule(simpleSchedule()
			            .withIntervalInSeconds(frequency)
			            .repeatForever())
			    .endAt(endTime)
			    .build();
		sched.scheduleJob(jd,ct);
		sched.start();
		
		if( sched.isShutdown() ) {
			sendMail("Tesi: il programma � terminato", "Congratulazioni! Lo scraper ha completato la ricerca.");
		}
	}

	public static void main(String[] info){
		
		ArrayList<String> specifications = new ArrayList<String>();
		int pages;
		int time;
		String endate;
		
		//Legge i parametri come tratte, numero di passeggeri e date
		specifications = readParameters();
		
		pages = Integer.parseInt(specifications.get(0));
		time = Integer.parseInt(specifications.get(1));
		endate = specifications.get(2);
		
		try{
			new QuartzMain(pages, time, endate);
		}
		catch(Exception e){
			e.getStackTrace();
			sendMail("Errore nell'avvio di QuartzMain", "C'� stata un'eccezione dell'avviare il programma.");
		}
	}
	
	//Legge i parametri della ricerca e mi ritorna quelli che dobbiamo settare nel programma
	private static ArrayList<String> readParameters() {
		ArrayList<String> dates = new ArrayList<String>();
		ArrayList<String> passengers = new ArrayList<String>();
		ArrayList<String> routes = new ArrayList<String>();
		ArrayList<String> specifications = new ArrayList<String>();
		File datesFile = new File("dates.txt");
		File passengersFile = new File("passengers.txt");
		File routesFile = new File("routes.txt");
		File specificationsFile = new File("specifications.txt");
		PrintWriter writer = null;
		String dateGo;
		String dateBack;
		String airportGo;
		String airportBack;
		String numberPass;
		String[] datesSplitted;
		String[] airporSplitted;
		
		try {
			writer = new PrintWriter("dataFile.csv", "UTF-8");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		if(datesFile.exists() && !datesFile.isDirectory()) {
			readFiles(datesFile, dates);
		}
		
		if(passengersFile.exists() && !passengersFile.isDirectory()) {
			readFiles(passengersFile, passengers);
		}
		
		if(routesFile.exists() && !routesFile.isDirectory()) {
			readFiles(routesFile, routes);
		}
		
		if(specificationsFile.exists() && !specificationsFile.isDirectory()) {
			readFiles(specificationsFile, specifications);
		}
		
		//writer.println("dateGo;dateBack;airportGo;airportBack;numberPass");
		
		for(String date : dates) {
			for(String passenger : passengers) {
				for(String route : routes) {
					datesSplitted = date.split(" ");
					dateGo = datesSplitted[0];
					dateBack = datesSplitted[1];
					airporSplitted = route.split(" ");
					airportGo = airporSplitted[0];
					airportBack = airporSplitted[1];
					numberPass = passenger;
					writer.println(dateGo +";" + dateBack + ";" + airportGo + ";" + airportBack + ";" + numberPass);
				}
			}
		}
		writer.close();
		
		return specifications;
	}
	
	private static void readFiles(File file, ArrayList<String> array) {
		try {
			
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;
			
			while ((line = bufferedReader.readLine()) != null) {
				array.add(line);
			}
			
			fileReader.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
private static void sendMail(String subject, String body) {
		
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
        } catch (MessagingException me) {
        	me.printStackTrace();
        	
        }
    }
	
}