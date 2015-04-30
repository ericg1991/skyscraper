package principal;



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
	
	public QuartzMain(String a, String b, String c, String d) throws Exception{
		
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
		
		Trigger ct = newTrigger()
			    .withIdentity("trigger1", "group1")
			    .startNow()
			    .withSchedule(simpleSchedule()
			            .withIntervalInSeconds(180)
			            .repeatForever())
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
		
		try{
			new QuartzMain(from,to,departure,arrival);
		}
		catch(Exception e){e.getStackTrace();}
	}
	
}
