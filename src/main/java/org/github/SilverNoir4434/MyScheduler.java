package org.github.SilverNoir4434;

import net.dv8tion.jda.api.JDA;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

public class MyScheduler implements Job {
    JDA api;

    @Override
    public void execute(JobExecutionContext content) throws JobExecutionException {
        try {
            System.out.println("Checking for memory on " + LocalDate.now() + " at " + LocalTime.now());
            Main.checkForMemory(api);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
