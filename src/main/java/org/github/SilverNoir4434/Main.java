package org.github.SilverNoir4434;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.pagination.PinnedMessagePaginationAction;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.quartz.CronScheduleBuilder.dailyAtHourAndMinute;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import io.github.cdimascio.dotenv.Dotenv;

public class Main {
    static Connection memoryDB;
    private static String BOT_TOKEN;
    static Logger log = LoggerFactory.getLogger(Main.class);
    static boolean noChannelsAdded = false;
    public static void main(String[] args) throws Exception {
            if (System.getenv("DISCORD_TOKEN") != null) {
                BOT_TOKEN = System.getenv("DISCORD_TOKEN");
            } else {
                Dotenv dotenv = Dotenv.load();
                BOT_TOKEN = dotenv.get("DISCORD_TOKEN");
            }

            if (BOT_TOKEN.isEmpty()) {
                log.error("No token provided! Check the environment variables.", new InvalidTokenException());
                return;
            } else {
                URL url = new URL("https://discord.com/api/users/@me");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("Authorization", "Bot " + BOT_TOKEN);
                con.connect();
                int code = con.getResponseCode();
                if (code == 401) {
                    log.error("Token is invalid! Please ensure you have copied a valid Discord bot token.", new InvalidTokenException());
                    return;
                } else {
                    con.disconnect();
                }
            }
            JDA api;
            memoryDB = DriverManager.getConnection("jdbc:sqlite:memories_database");
        // Creating database in case it doesn't exist
        String createGuild = "CREATE TABLE IF NOT EXISTS GUILD" +
                "(GUILD_ID INT PRIMARY KEY," +
                "GUILD_NAME TEXT NOT NULL);";
        String createChannel = "CREATE TABLE IF NOT EXISTS CHANNEL" +
                "(CHANNEL_ID INT PRIMARY KEY," +
                "CHANNEL_NAME TEXT NOT NULL," +
                "GUILD_ID INT NOT NULL," +
                "FOREIGN KEY(GUILD_ID) REFERENCES GUILD(GUILD_ID));";
        String createMemories = "CREATE TABLE IF NOT EXISTS MEMORY" +
                "(MEMORY_ID INT PRIMARY KEY," +
                "MEMORY_LINK TEXT NOT NULL," +
                "MEMORY_AUTHOR INT NOT NULL," +
                "MEMORY_DATE DATE NOT NULL," +
                "GUILD_ID INT NOT NULL," +
                "CHANNEL_ID INT NOT NULL," +
                "FOREIGN KEY(GUILD_ID) REFERENCES GUILD(GUILD_ID)," +
                "FOREIGN KEY(CHANNEL_ID) REFERENCES CHANNEL(CHANNEL_ID));";
        Statement stmt = memoryDB.createStatement();
        Statement stmt2 = memoryDB.createStatement();
        Statement stmt3 = memoryDB.createStatement();
        stmt.execute(createGuild);
        stmt2.execute(createChannel);
        stmt3.execute(createMemories);

        ResultSet res = memoryDB.prepareStatement("SELECT * FROM CHANNEL").executeQuery();

        if (!res.next()) noChannelsAdded = true;



        //Create bot instance, add slash commands to bot
        api = JDABuilder.createDefault(BOT_TOKEN).addEventListeners(new MyListener(memoryDB)).build();

        api.updateCommands().addCommands(
                Commands.slash("addchannel", "Adds channels for the bot to watch.")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL)),
                Commands.slash("removechannel", "Tells the bot to stop watching a channel.")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL)),
                Commands.slash("checkformemory", "Checks for a memory.")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL)),
                Commands.slash("getmemories", "Checks watched channels for new pins.")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL)),
                Commands.slash("randommemory", "Grabs a random pin from watched channels."),
                Commands.slash("checkday", "Checks a certain day for a memory.")
                        .addOption(OptionType.STRING, "day", "Day to check for memory (Use format MM/DD/YYYY)", true),
                Commands.slash("listchannels", "Lists channels the bot is currently watching.")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL))
        ).queue();
        memoryDB.close();


        // Creates and sets scheduler to check for memory every day
        JobDetail job = newJob(MyScheduler.class)
                .withIdentity("MemoryJob", "group1").build();

        Trigger trigger = newTrigger()
                .withIdentity("TimeTrigger", "group1")
                .startNow()
                .withSchedule(dailyAtHourAndMinute(12, 0))
                .build();

        Scheduler schedule = StdSchedulerFactory.getDefaultScheduler();
        schedule.start();
        schedule.scheduleJob(job, trigger);
    }

    public static Boolean checkForMemory(JDA api) throws SQLException {
        memoryDB = DriverManager.getConnection("jdbc:sqlite:memories_database");
        // Queries database for message pinned a year ago, posts embed to channel if any are found.

        LocalDate today = LocalDate.now();

        String getMemories = "SELECT * FROM MEMORY " +
                "WHERE DATE(MEMORY_DATE) LIKE '%-" + today.getMonth() + "-" + today.getDayOfMonth() + "';";

        PreparedStatement stmt = memoryDB.prepareStatement(getMemories);
        ResultSet results = stmt.executeQuery();

        if (!results.next()) {
            log.info("Checked for memory at {}, no memory found.", LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
            memoryDB.close();
            return false;
        }

        while (results.next()) {
            postEmbed(api, results, api.getTextChannelsByName("memories-channel",true).get(0));
        }

        memoryDB.close();
        return true;
    }
    public static int getMemories(JDA api) throws SQLException {
        memoryDB = DriverManager.getConnection("jdbc:sqlite:memories_database");
        boolean foundMemory = false;
        String getChannels = "SELECT * FROM CHANNEL;";
        PreparedStatement stmt = memoryDB.prepareStatement(getChannels);
        ArrayList<Long> memChannels = new ArrayList<>();
        ArrayList<Message> memories = new ArrayList<>();
        ResultSet results = stmt.executeQuery();

        while (results.next()) {
            memChannels.add(results.getLong(1));
        }

        // Iterates through channels, adding all pinned messages to the memories ArrayList
        for (Guild g : api.getGuilds()) {
            List<TextChannel> channels = g.getTextChannels();
            for (TextChannel tc : channels) {
                for (Long l : memChannels) {
                    if (tc.getIdLong() == l) {
                        List<PinnedMessagePaginationAction.PinnedMessage> interim = tc.retrievePinnedMessages().stream().toList();
                        for (PinnedMessagePaginationAction.PinnedMessage p : interim) {
                            Message msg = p.getMessage();
                            memories.add(msg);
                        }
                    }
                }
            }
        }
        // Iterates through memories, querying database to check if memory is in database, adding it if it isn't.
        for (Message m : memories) {
            String checkIfMemoryCaptured = "SELECT * FROM MEMORY WHERE MEMORY_ID = " + m.getIdLong();
            Statement stmt2 = memoryDB.createStatement();
            ResultSet result = stmt2.executeQuery(checkIfMemoryCaptured);

            if (!result.next()) {
                foundMemory = true;
                String addMemory = "INSERT OR IGNORE INTO MEMORY(MEMORY_ID, MEMORY_LINK, MEMORY_AUTHOR, MEMORY_DATE, GUILD_ID, CHANNEL_ID) VALUES" +
                        "(" + m.getIdLong() + ", '" + m.getJumpUrl() +"', '" + m.getAuthor().getId() +"', '" + m.getTimeCreated().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) +
                        "', " + m.getGuildIdLong() + ", " + m.getChannelIdLong() + ");";
                Statement stmt3 = memoryDB.createStatement();
                stmt3.execute(addMemory);
            }
        }
        memoryDB.close();
        if (foundMemory) {
            return 1;
        } else {
            return 0;
        }
    }
    
    public static void postEmbed(JDA api, ResultSet res, MessageChannel destChan) throws SQLException {
        MessageChannel memChannel = api.getTextChannelById(res.getLong("CHANNEL_ID"));
        Message memory = memChannel.retrieveMessageById(res.getLong("MEMORY_ID")).complete();
        EmbedBuilder embed = new EmbedBuilder();

        // Builds embed from database result, posts in memories-channel
        embed.setTitle("Memory from <t:" + memory.getTimeCreated().toInstant().getEpochSecond() + ":f>");
        embed.addField("Sent by:", memory.getAuthor().getAsMention(), true);
        if (!memory.getContentRaw().isEmpty()) {
            embed.setDescription(">>> " + memory.getContentRaw());
            embed.addField("Original message link:", memory.getJumpUrl(), true);
        } else {
            embed.setDescription("No text in original memory.");
            embed.addField("Here's the message:", memory.getJumpUrl(), true);
        }

        destChan.sendMessageEmbeds(embed.build()).queue();
    }
}

