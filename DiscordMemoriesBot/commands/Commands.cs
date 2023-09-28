using DSharpPlus;
using DSharpPlus.CommandsNext;
using DSharpPlus.CommandsNext.Attributes;
using DSharpPlus.Entities;
using DSharpPlus.Interactivity.Extensions;

namespace DiscordMemoriesBot.commands
{
    public class Commands : BaseCommandModule
    {
        [Command("setup"), Description("Command used to initally setup the Memories bot."), RequirePermissions(Permissions.ManageChannels)]
        public async Task Setup(CommandContext ctx)
        {
            Console.WriteLine("Executing setup command...");
            /* Checks to see if this command has already been used. If so, warns the user that data will be
             * wiped if this command is used again and lets them decide if they want to do so. If they do,
             * wipe data from all three files and continue the command. If not, exits the command. 
             * Command also exits if the user is inactive for 30 seconds.
             */
            FileInfo channelFile = new FileInfo("../../../channels.txt");
            FileInfo pinsFile = new FileInfo("../../../pins.txt");
            FileInfo roleFile = new("../../../roles.txt");
            if (channelFile.Length > 0 || pinsFile.Length > 0 || roleFile.Length > 0)
            {
                await ctx.RespondAsync("I already have channels and memories logged! Are you sure you want to continue setup? If you " +
                    "do, all logged channels and memories will be wiped! Type yes if you're sure, or type anything else to cancel.");
                var responseMessage = await ctx.Message.GetNextMessageAsync();
                if (responseMessage.TimedOut)
                {
                    await ctx.RespondAsync("Command timed out.");
                    return;
                }
                if (responseMessage.Result.Content.ToLower() == "yes")
                {
                    await ctx.RespondAsync("Alright! Continuing with setup...");
                    File.WriteAllText("../../../pins.txt", "");
                    File.WriteAllText("../../../channels.txt", "");
                    File.WriteAllText("../../../roles.txt", "");
                    Console.WriteLine("Files reset!");
                }
                else
                {
                    await ctx.RespondAsync("Setup cancelled.");
                    return;
                }
            }
            await ctx.RespondAsync("Thank you for using the Memories Bot! Give me the channelID of the first channel you wish to pull " +
                "pins from, or type cancel to cancel setup.");
            while(true)
            {
                /* Waits for user to provide channelID for channel to pull pins from. Checks for timeout as 
                 * above. If the response was "cancel", exits the method with a message to the user.
                 */
                var responseMessage = await ctx.Message.GetNextMessageAsync();
                if (responseMessage.TimedOut)
                {
                    await ctx.RespondAsync("Command timed out.");
                    return;
                }
                ulong channelID;
                
                try
                {
                    if (responseMessage.Result.Content.ToLower() == "cancel")
                    {
                        await responseMessage.Result.RespondAsync("Cancelled setup! This bot will not work properly until you complete setup.");
                        return;
                    }
                    /* If the response is "done" and we have been given a channel already, we ask the user if 
                     * they wish to ping a role when a memory is found. If the response is no, we send a final 
                     * message and exit the method. If the response is yes, we ask the user to ping the role 
                     * that they wish to be pinged. We have some error checking to make sure the user pings a 
                     * singular role in their next message, and if they do, we take that role and write it to 
                     * the roles.txt file with the serverID of the server the role is in.
                     */
                    else if (responseMessage.Result.Content.ToLower() == "done" && channelFile.Length > 0)
                    {
                        await responseMessage.Result.RespondAsync("All channels logged! Would you like to ping a role when the memory embed is " +
                            "sent? Reply yes to set a role, or anything else to finish setup.");
                        responseMessage = await ctx.Message.GetNextMessageAsync();
                        if (responseMessage.TimedOut)
                        {
                            await ctx.RespondAsync("Command timed out.");
                            return;
                        }
                        if (responseMessage.Result.Content.ToLower() == "yes")
                        {
                            await responseMessage.Result.RespondAsync("Ping the role you wish to be pinged when the memory embed is sent.");
                            while (true)
                            {
                                responseMessage = await ctx.Message.GetNextMessageAsync();
                                if (responseMessage.Result.MentionedRoles == null || responseMessage.Result.MentionedRoles.Count > 1)
                                {
                                    await responseMessage.Result.RespondAsync("You need to send a ping to a single role!");
                                }
                                else
                                {
                                    var role = responseMessage.Result.MentionedRoles[0];
                                    
                                    if (roleFile.Length > 0)
                                    {
                                        File.AppendAllText("../../../roles.txt","\n" + role.Id.ToString() + ", " + ctx.Guild.Id.ToString());
                                    }
                                    else
                                    {
                                        File.WriteAllText("../../../roles.txt", role.Id.ToString() + ", " + ctx.Guild.Id.ToString());
                                    }
                                    await responseMessage.Result.RespondAsync("Setup complete!");
                                }
                            }
                        }
                        else
                        {
                            await responseMessage.Result.RespondAsync("Setup complete!");
                        }
                        return;
                    }
                    /* We use the given input to get a channelID and write that channelID to the channels
                     * file. Then, we pull all of the pins from the channel and write them to the pins file.
                     * We refresh the channelFile so that its length is always accurate when we use it to check
                     * if the file has been written to. We do the same for the pins file.
                     */
                    channelID = ulong.Parse(responseMessage.Result.Content);
                    ctx.Guild.GetChannel(channelID);
                    channelFile.Refresh();
                    if (channelFile.Length == 0)
                    {
                        File.WriteAllText("../../../channels.txt", channelID.ToString());
                        channelFile.Refresh();
                    }
                    else
                    {
                        File.AppendAllText("../../../channels.txt", "\n" + channelID.ToString());
                    }
                    var channel = ctx.Guild.GetChannel(channelID);
                    var channelPins = await channel.GetPinnedMessagesAsync();
                    var pinsList = channelPins.ToList();
                    pinsFile.Refresh();
                    if (pinsFile.Length == 0)
                    {
                        Program.ModifyPinsFile(pinsList, 'w');
                    }
                    else
                    {
                        Program.ModifyPinsFile(pinsList, 'a');
                    }
                    await ctx.Channel.SendMessageAsync("Pins from this channel written to file!");
                    await responseMessage.Result.RespondAsync("Channel logged! Provide another channelID or type done to finish.");
                }
                catch(FormatException) // Catches if the user tries to enter something that's not a channelID.
                {
                    await responseMessage.Result.RespondAsync("Please provide a channelID, or type 'cancel' to cancel.");
                    continue;
                }
                catch(NullReferenceException) // Catches if the user enters an invalid channelID.
                {
                    await responseMessage.Result.RespondAsync("I can't find that channel! Are you sure you gave the right channel ID?");
                    continue;
                }
            }
        }

        [Command("addchannel"), Description("Adds a new channel to get pins from."), RequirePermissions(Permissions.ManageChannels)]
        public async Task AddChannel(CommandContext ctx, [Description("ChannelID of the channel to add.")] ulong channelID)
        {
            Console.WriteLine("Executing addchannel command...");
            ChannelReader cr = new();
            DiscordChannel channel;
            // Checks to make sure we haven't already added the provided channel to the file
            if(cr.Channels.Contains(channelID))
            {
                await ctx.Channel.SendMessageAsync("I already have that channel!");
            }
            // Check to make sure the provided channel is in the server where this command was called
            try
            {
                channel = ctx.Guild.GetChannel(channelID);
            }
            catch(NullReferenceException)
            {
                await ctx.Channel.SendMessageAsync("I can't find that channel! Are you sure you gave the right channel ID?");
                return;
            }
            /* Gets the channels file, then writes the channel into it. If the channels file already has a channel in it, appends
             * the new channel to the file. Also records the pins from the provided channel.
             */
            FileInfo channelFile = new FileInfo("../../../channels.txt");
            if (channelFile.Length == 0) {
                File.WriteAllText("../../../channels.txt", channelID.ToString());
            } 
            else
            {
                File.AppendAllText("../../../channels.txt", "\n" + channelID.ToString());
            }
            var channelPins = await channel.GetPinnedMessagesAsync();
            var pinsList = channelPins.ToList();
            Program.ModifyPinsFile(pinsList, 'w');
            await ctx.Channel.SendMessageAsync("Pins from this channel written to file!");
        }

        [Command("checkformemory"), Description("Command that manually checks for a message pinned today."), RequirePermissions(Permissions.ManageChannels)]
        public Task CheckForMemory(CommandContext ctx)
        {
            Program.CheckForMemory();
            return Task.CompletedTask;
        }

        [Command("getpins"), Description("Reacquires pinned messages from channels."), RequirePermissions(Permissions.ManageChannels)]
        public async Task GetPins(CommandContext ctx)
        {
            Console.WriteLine("Getting pins from logged channels...");
            File.WriteAllText("../../../pins.txt", "");
            ChannelReader cr = new();
            foreach (DiscordGuild guild in Program.Client.Guilds.Values)
            {
                foreach (ulong channel in cr.Channels)
                {
                    if (guild.Channels.ContainsKey(channel))
                    {
                        var channelPins = await guild.Channels[channel].GetPinnedMessagesAsync();
                        var pinsList = channelPins.ToList();
                        FileInfo pinsFile = new FileInfo("../../../pins.txt");
                        if (pinsFile.Length == 0)
                        {
                            Program.ModifyPinsFile(pinsList, 'w');
                        }
                        else
                        {
                            Program.ModifyPinsFile(pinsList, 'a');
                        }
                    }
                }
            }
        }

        [Command("changerole"), Description("Changes the role to ping when this bot sends an embed message."), RequirePermissions(Permissions.ManageChannels)]
        public async Task ChangeRole(CommandContext ctx, [Description("The role to mention when an embed is sent.")]DiscordRole role)
        {
            Console.WriteLine("Changing role mentioned in embed...");
            if (ctx.Message.MentionedRoles == null || ctx.Message.MentionedRoles.Count > 1)
            {
                return;
            }
            else
            {
                File.WriteAllText("../../../roles.txt", role.Id.ToString() + ", " + ctx.Guild.Id.ToString());
                await ctx.Channel.SendMessageAsync("Role changed!");
            }
        }
    }
}
