using DiscordMemoriesBot.commands;
using DSharpPlus;
using DSharpPlus.CommandsNext;
using DSharpPlus.Entities;
using DSharpPlus.EventArgs;
using DSharpPlus.Exceptions;
using DSharpPlus.Interactivity;
using DSharpPlus.Interactivity.Extensions;
using System.Text;

namespace DiscordMemoriesBot
{
    /* Initalizes the DiscordClient which logs in the bot, in addition to the CommandsNextExtension, which 
     * allows us to make text commands (commands which are called by writing a message in a text channel
     * starting with a prefix (ex. !help would call a help command)). We also take in the ChannelPinsUpdated
     * event, which is necessary for the bot to function without constant human intervention.
     */
    internal class Program
    {
        public static DiscordClient Client { get; private set; }
        private static CommandsNextExtension Commands { get; set; }


        static async Task Main(string[] args)
        {
            if (!File.Exists("pins.txt")) File.Create("pins.txt");
            if (!File.Exists("channels.txt")) File.Create("channels.txt");
            if (!File.Exists("roles.txt")) File.Create("roles.txt");
            if (!File.Exists("config.json"))
            {
                File.Create("config.json");
                Console.WriteLine("Input bot token.");
                var token = Console.ReadLine();
                string jsonText = "{\r\n  \"token\": \"" + token + "\",\r\n  \"prefix\": \"!\"\r\n}";
                File.WriteAllText("config.json", jsonText);
            }
            // reads bot token and command prefix from config.json
            var jsonReader = new JSONReader();
            await jsonReader.ReadJSON();

            // Sets our settings for the Discord Client the bot uses, which we pass to it below.
            var discordConfig = new DiscordConfiguration()
            {
                Intents = DiscordIntents.All,
                Token = jsonReader.token,
                TokenType = TokenType.Bot,
                AutoReconnect = true,
            };

            
            Client = new DiscordClient(discordConfig);

            // Gives the ChannelPinsUpdated and MessageDeleted events to their respective methods.
            Client.ChannelPinsUpdated += RecordPins;
            Client.MessageDeleted += CheckForDeletedPin;
            
            // Sets our settings for commands, which we pass in below.
            var commandsConfig = new CommandsNextConfiguration()
            {
                StringPrefixes = new string[] { jsonReader.prefix },
                EnableMentionPrefix = true,
                EnableDefaultHelp = true
            };

            Commands = Client.UseCommandsNext(commandsConfig);

            // Sets the default timeout for commands that take user input after the inital command is sent.
            Client.UseInteractivity(new InteractivityConfiguration()
            {
                Timeout = TimeSpan.FromSeconds(30)
            });

            // Registers our bot's commands.
            Commands.RegisterCommands<Commands>();

            // Connects our bot to Discord.
            await Client.ConnectAsync();

            // Schedules a job to call CheckForMemory once every day at noon.
            DailyMemoryScheduler memSched = new DailyMemoryScheduler();
            await memSched.ScheduleJob();

            // Sets the bot's activity
            DiscordActivity activity = new();
            activity.Name = "use !setup to get started!";
            await Client.UpdateStatusAsync(activity);

            // We make it so the Main method never closes so that the program doesn't close and our bot stays online.
            await Task.Delay(-1);
        }

        /* Whenever a message is deleted, we check if it was a pinned message. If it was, we remove it from the
         * pins file.
         */

        private static Task CheckForDeletedPin(DiscordClient sender, MessageDeleteEventArgs args)
        {
            ChannelReader cr = new ChannelReader();
            // If the deleted message isn't from a channel we have logged, exit the method.
            if (!cr.Channels.Contains(args.Channel.Id)) return Task.CompletedTask;
            StringBuilder sb = new();

            /* Removes the message from the pins file by rewriting the file with each message
             * except the removed message.
             */
            using (StreamReader sr = new StreamReader("pins.txt"))
            {
                string currentLine;
                bool hasLooped = false;
                while ((currentLine = sr.ReadLine()) != null)
                {
                    string[] pinnedMessage = currentLine.Split(',');
                    ulong messageID = ulong.Parse(pinnedMessage[0]);
                    if (!(messageID == args.Message.Id) && hasLooped == false)
                    {
                        sb.Append(currentLine);
                        hasLooped = true;
                    }
                    else if (!(messageID == args.Message.Id))
                    {
                        sb.Append("\n" + currentLine);
                    }
                }
            }
            File.WriteAllText("pins.txt", sb.ToString());
            return Task.CompletedTask;
        }

        /* Whenever a message is pinned in a channel, the bot gets the latest pin from the channel and appends it
         * to the end of the pins file.
         */
        private static async Task RecordPins(DiscordClient sender, ChannelPinsUpdateEventArgs args)
        {
            /* Checks to make sure that the channel the message was pinned in is a channel we're supposed to 
             * take pins from. If it's not, exits the method.
             */
            ChannelReader cr = new ChannelReader();
            if (!cr.Channels.Contains(args.Channel.Id)) return;

            /* Gets the channel and its pins, converts the pins to a list, gets the most recent entry in that list,
             * then writes it to file.
             */
            var channel = args.Channel;
            var channelPins = await channel.GetPinnedMessagesAsync();
            var pinsList = channelPins.ToList();
            var pinnedMessage = pinsList.ElementAt(0);
            StringBuilder sb = new StringBuilder();
            sb.Append("\n" + pinnedMessage.Id.ToString() + ", " + 
                pinnedMessage.Timestamp.ToString() + ", " + pinnedMessage.Author.Id.ToString()
                + ", " + pinnedMessage.ChannelId.ToString());
            File.AppendAllText("pins.txt", sb.ToString());
        }


        /* Method that takes in a list of DiscordMessages and a character that tells the method whether
         * to write text to the file (that is, replace everything in the file with the string in 
         * StringBuilder) or to append to it (place text in StringBuilder at the end of the file).
         */
        public static void ModifyPinsFile(List<DiscordMessage> pinsList, char writeOrAppend) 
        {
            StringBuilder sb = new StringBuilder();
            // Enter this loop if writing to the file.
            if (writeOrAppend == 'w')
            {
                Console.WriteLine("Writing to pins.txt...");
                bool hasLooped = false;
                foreach (var pin in pinsList)
                {
                    /* Writes the pinID (the MessageID of the pinned message), the Timestamp (when the message was
                     * sent), the AuthorID (the UserID of the author of the message), and the ChannelID (ID of the
                     * channel where the message is pinned) of the pin to the StringBuilder.
                     */
                    if (!hasLooped)
                    {
                        sb.Append(pin.Id.ToString() + ", " + pin.Timestamp.ToString() + ", " + pin.Author.Id.ToString() + ", " + pin.ChannelId.ToString());
                        hasLooped = true;
                    }
                    /* If the loop has iterated once already, add a line break before writing more data to the 
                     * StringBuilder. We skip the first line break since the above line of data is the first 
                     * line of the text document.
                     */
                    else
                    {
                        sb.Append("\n" + pin.Id.ToString() + ", " + pin.Timestamp.ToString() + ", " + pin.Author.Id.ToString() + ", " + pin.ChannelId.ToString());
                    }
                }
                // Writes contents of the StringBuilder to pins.txt.
                File.WriteAllText("pins.txt", sb.ToString());
            }

            else if (writeOrAppend == 'a')
            {
                Console.WriteLine("Appending to pins.txt...");
                // Same as above loop, except we don't use hasLooped since we don't need to skip a line break.
                foreach (var pin in pinsList)
                {
                    sb.Append("\n" + pin.Id.ToString() + ", " + pin.Timestamp.ToString() + ", " + pin.Author.Id.ToString() + ", " + pin.ChannelId.ToString());
                }
                // Appends contents of the StringBuilder to pins.txt.
                File.AppendAllText("pins.txt", sb.ToString());
            }

            else
            {
                Console.WriteLine("You must specify whether the method is supposed to append text or write text!");
            }
            Console.WriteLine("Done!");
        }

        /* Method that runs once every day (see DailyMemoryScheduler.cs) to check if there is a pin that was sent
         * a year ago today.
         */
        public async static void CheckForMemory()
        {
            Console.WriteLine("Checking for memory...");
            // Checking to make sure setup command has been run first
            FileInfo channelFile = new("channels.txt");
            FileInfo pinsFile = new("pins.txt");
            FileInfo roleFile = new("roles.txt");
            if (channelFile.Length == 0 || pinsFile.Length == 0)
            {
                Console.WriteLine("Setup command must be run before doing that!");
                return;
            }
            var guilds = Client.Guilds;
            bool memoryFound = false;
            bool authorIsDeletedAccount = false;
            DiscordMember? author = null;
            foreach (var guild in guilds)
            {
                // gets the current guild
                var currentGuild = await Client.GetGuildAsync(guild.Key); 
                using (StreamReader sr = new StreamReader("pins.txt"))
                {
                    /* We read pins.txt and get the messageID, timestamp, authorID, and author of the pinned message.
                     * Using the timestamp, we get the unix timestamp and the amount of time that has passed since
                     * the message has been posted.
                     */
                    string currentLine;
                    while ((currentLine = sr.ReadLine()) != null)
                    {
                        string[] pinnedMessage = currentLine.Split(',');
                        ulong messageID = ulong.Parse(pinnedMessage[0]);
                        DateTime timestamp = DateTime.Parse(pinnedMessage[1]);
                        long unixTimestamp = ((DateTimeOffset)timestamp).ToUnixTimeSeconds();
                        ulong authorID = ulong.Parse(pinnedMessage[2]);
                        try
                        {
                            author = await currentGuild.GetMemberAsync(authorID);
                        } 
                        catch (NotFoundException)
                        {
                            authorIsDeletedAccount = true;
                        }
                        
                        ulong channelID = ulong.Parse(pinnedMessage[3]);
                        TimeSpan timeSinceMessagePosted = timestamp - DateTime.Now;
                        Console.WriteLine(timeSinceMessagePosted.Days * -1);

                        /* If it is the anniversary of when the pinned message was posted, we get the pinned message
                         * and begin building our embed message. We use the unix timestamp to make the title of the
                         * embed have a Discord timestamp for the date & time the message was posted (so that it'll
                         * be accurate to the user's local time), mention the user that sent the message, and put a
                         * link to the pinned message in the embed. If we were given a role to mention in setup (see
                         * setup in Commands.cs for more on that), get that role from the roles file using the current
                         * guild's ID and mention it. Then, we build the embed and send it in the memories channel.
                         */
                        if (((timeSinceMessagePosted.Days * -1) % 365) == 0 && timeSinceMessagePosted.Days != 0)
                        {
                            memoryFound = true;
                            var channel = currentGuild.GetChannel(channelID);
                            if (channel == null)
                            {
                                continue;
                            }
                            var message = await channel.GetMessageAsync(messageID);
                            var memoryChannel = currentGuild.GetChannel(currentGuild.Channels.SingleOrDefault
                                (channels => channels.Value.Name == "memories-channel").Key);

                            DiscordEmbedBuilder embedBuilder = new DiscordEmbedBuilder();
                            embedBuilder.Title = $"Memory from <t:{unixTimestamp}:f>";
                            if (authorIsDeletedAccount)
                            {
                                embedBuilder.AddField("Sent by: " , "Deleted Account");
                            }
                            else
                            {
                                embedBuilder.AddField("Sent by: ", author.Mention);
                            }
                            
                            embedBuilder.AddField("Here's the message: ", message.JumpLink.ToString());
                            var embed = embedBuilder.Build();

                            using (StreamReader rolesSR = new StreamReader("roles.txt"))
                            {
                                string currentLineRoleFile;
                                if (!(roleFile.Length == 0))
                                {
                                    while ((currentLineRoleFile = rolesSR.ReadLine()) != null)
                                    {
                                        string[] role = currentLineRoleFile.Split(',');
                                        if (currentGuild.Roles.ContainsKey(ulong.Parse(role[1])))
                                        {
                                            var memoryRole = currentGuild.Roles[ulong.Parse(role[0])];
                                            await memoryChannel.SendMessageAsync(memoryRole.Mention);
                                        }
                                    }
                                }
                            }

                            Console.WriteLine("Memory found, and embed built! Sending...");
                            await memoryChannel.SendMessageAsync(embed);
                        }
                    }
                    if (!memoryFound)
                    {
                        Console.WriteLine("No memory found for today.");
                    }
                    sr.Close();
                }
            }
        }        
    }
}