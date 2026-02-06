package org.github.SilverNoir4434;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.Console;
import java.io.FileReader;
import java.sql.*;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    static Connection memoryDB;
    static BufferedReader br;
    private static String BOT_TOKEN;
    public static void main(String[] args) throws Exception {
        ScheduledExecutorService schedule = Executors.newScheduledThreadPool(1);

        try {
            memoryDB = DriverManager.getConnection("jdbc:sqlite:memories_database");
            br = new BufferedReader(new FileReader("token.txt"));
            BOT_TOKEN = br.readLine();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
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

        JDA api = JDABuilder.createDefault(BOT_TOKEN).addEventListeners(new MyListener(memoryDB)).build();


        api.updateCommands().addCommands(
                Commands.slash("addchannel", "Adds channels for the bot to watch."),
                Commands.slash("removechannel","Tells the bot to stop watching a channel."),
                Commands.slash("checkformemory", "Checks for a memory."),
                Commands.slash("getpins", "Checks watched channels for new pins.")
        ).queue();
        memoryDB.close();

        /*
        TODO: Finish implementing scheduled execution of checkforMemory.
         */
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                schedule.shutdown();
            }
        });
    }

    public static void checkforMemory(JDA api) throws SQLException {
        /*
        TODO: Implement this method, make it check if there was a memory from a year ago today and post about it if there was.
         */
        memoryDB = DriverManager.getConnection("jdbc:sqlite:memories_database");

        String getMemories = "SELECT * FROM MEMORY " +
                "WHERE DATE(MEMORY_DATE)=DATE('now');";

        PreparedStatement stmt = memoryDB.prepareStatement(getMemories);
        ArrayList<Message> memories = new ArrayList<>();
        ResultSet results = stmt.executeQuery();

        if (!results.next()) {
            System.out.println("No memories today.");
            memoryDB.close();
            return;
        }

        while (results.next()) {
            MessageChannel memChannel = api.getTextChannelById(results.getLong("CHANNEL_ID"));
            Message memory = memChannel.retrieveMessageById(results.getLong("MEMORY_ID")).complete();
            EmbedBuilder embed = new EmbedBuilder();


            embed.setTitle("Memory from <t:" + memory.getTimeCreated().toInstant().getEpochSecond() + ":f>");
            embed.addField("Sent by:", memory.getAuthor().getAsMention(), true);
            if (!memory.getContentRaw().isEmpty()) {
                embed.setDescription(">>> " + memory.getContentRaw());
                embed.addField("Original message link:", memory.getJumpUrl(), true);
            } else {
                embed.setDescription("No text in original memory.");
                embed.addField("Here's the message:", memory.getJumpUrl(), true);
            }

            MessageChannel destChan = api.getTextChannelsByName("memories-channel",true).getFirst();
            destChan.sendMessageEmbeds(embed.build()).queue();
        }

        memoryDB.close();
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


        for (Guild g : api.getGuilds()) {
            List<TextChannel> channels = g.getTextChannels();
            for (TextChannel tc : channels) {
                for (Long l : memChannels) {
                    if (tc.getIdLong() == l) {
                        List<Message> interim = tc.retrievePinnedMessages().complete();
                        memories.addAll(interim);
                    }
                }
            }
        }

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
}

