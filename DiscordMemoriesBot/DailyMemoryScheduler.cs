using Quartz;

namespace DiscordMemoriesBot
{
    internal class DailyMemoryScheduler
    {

        /* Sets up the job using DailyMemoryJob.cs, then builds a trigger for that job to run and call
         * Program.CheckForMemory once each day at noon. The scheduler takes the trigger and schedules
         * the job for the appropriate time.
        */
        public async Task ScheduleJob()
        {
            IJobDetail job = JobBuilder.Create<DailyMemoryJob>()
                .WithIdentity("memoryjob", "discordmemoriesbot")
                .Build();

            ITrigger trigger = TriggerBuilder.Create()
                .WithIdentity("memorytrigger", "discmembot")
                .StartNow()
                .WithSchedule(CronScheduleBuilder.DailyAtHourAndMinute(12, 00)
                    .WithMisfireHandlingInstructionFireAndProceed()
                )
                .Build();

            var scheduleFactory = SchedulerBuilder.Create()
                .WithMisfireThreshold(TimeSpan.FromSeconds(5))
                .WithId("memjobsched")
                .WithName("Memory Job Scheduler")
                .WithMaxBatchSize(2)
                .WithInterruptJobsOnShutdown(true)
                .WithInterruptJobsOnShutdownWithWait(true)
                .Build();

            var scheduler = await scheduleFactory.GetScheduler();

            await scheduler.ScheduleJob(job, trigger);
            await scheduler.Start();
        }
    }
}
