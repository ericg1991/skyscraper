package principal;

import java.util.Date;

import javax.xml.crypto.Data;

import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.CronTriggerImpl;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;



public class QuartzMain {
	
	public QuartzMain(String a, String b, String c, String d, int e, int f, String endate) throws Exception{
		
		SchedulerFactory sf = new StdSchedulerFactory();
		Scheduler sched = sf.getScheduler();
		JobDetail jd = newJob(Scraper.class)
			    .withIdentity("job1", "group1")
			    .build();
		//per passare i parametri alla classe Scraper si fa così
		jd.getJobDataMap().put("airport_part", a);
		jd.getJobDataMap().put("airport_dest", b);
		jd.getJobDataMap().put("datepart",c);
		jd.getJobDataMap().put("daterit", d);
		jd.getJobDataMap().put("pagesNumber", e);
	
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
			            .withIntervalInSeconds(f)
			            .repeatForever())
			    .endAt(endTime)
			    .build();
		sched.scheduleJob(jd,ct);
		sched.start();
	}

	public static void main(String[] info){
		//passo i parametri al metodo quartzmain che poi a sua volta li passa alla classe Scraper dove servono
		String from = info[0];
		String to = info[1];
		String departure = info[2];
		String arrival = info[3];
		int pagesNumber = Integer.parseInt(info[4]);
		int time = Integer.parseInt(info[5]);
		String endate = info[6];
	
		
		try{
			new QuartzMain(from,to,departure,arrival,pagesNumber,time,endate);
		}
		catch(Exception e){e.getStackTrace();}
	}
	
}