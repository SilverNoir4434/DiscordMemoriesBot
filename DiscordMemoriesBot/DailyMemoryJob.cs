using Quartz;

namespace DiscordMemoriesBot
{
    internal class DailyMemoryJob : IJob
    {
        public async Task Execute(IJobExecutionContext context)
        {
            Program.CheckForMemory();
        }
    }
}
