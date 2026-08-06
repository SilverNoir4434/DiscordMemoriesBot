package org.github.SilverNoir4434;

import net.dv8tion.jda.api.JDA;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import static org.github.SilverNoir4434.Main.log;


public class MyScheduler implements Job {
    JDA api;

    @Override
    public void execute(JobExecutionContext content) {
        try {
            if (Main.noChannelsAdded) {
                log.warn("Not currently watching any channels, skipping check.");
                return;
            }
            log.info("Checking for memory on {} at {}", LocalDate.now(), LocalTime.now());
            Main.checkForMemory(api);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
