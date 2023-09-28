namespace DiscordMemoriesBot
{
    /* This class is used to make a list of channels from the channelIDs we pull
     * from the channels.txt file.
     */
    internal class ChannelReader
    {
        List<ulong> channels;

        public List<ulong> Channels
        {
            get { return channels; }
            set { channels = value; }
        }

        public ChannelReader()
        {
            // Instantiates the list, and adds all channels in channel.txt to it
            channels = new List<ulong> { };
            using (var sr = new StreamReader("../../../channels.txt"))
            {
                string currentLine;
                while ((currentLine = sr.ReadLine()) != null)
                {
                    channels.Add(ulong.Parse(currentLine));
                }
                sr.Close();
            }
        }

        public void ReadChannels()
        {
            // prints all channels in list to console, used for debugging
            foreach (var channel in channels)
            {
                Console.WriteLine(channel.ToString());
            }
        }
    }
}
