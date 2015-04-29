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



public class quartzlauncher {
	
	public quartzlauncher() throws Exception{
		
		SchedulerFactory sf = new StdSchedulerFactory();
		Scheduler sched = sf.getScheduler();
		JobDetail jd = newJob(Main.class)
			    .withIdentity("job1", "group1")
			    .build();
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

	public static void main(String[] Mel){
		try{
			new quartzlauncher();
		}
		catch(Exception e){e.getStackTrace();}
	}
	
}
