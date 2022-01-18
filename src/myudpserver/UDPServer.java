/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myudpserver;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.time.LocalDate;
import java.util.Base64;

/**
 *
 * @author Wilson
 */
public class UDPServer {

    private DatagramSocket socket;
    private boolean running;
    private Connection DB;

    private byte[] send_buf = new byte[256];
    final String version = "0.1.8";
    final String updateLink = "https://gamejolt.com/games/geofighter/436528";
    final String discordLink = "https://discord.gg/DKrk3fD";
    private ArrayList<Player> players = new ArrayList<Player>();
    private ArrayList<Game> games = new ArrayList<Game>();
    private ArrayList<Code> codes = new ArrayList<Code>();
    protected ArrayList<BannedIP> banned_ips = new ArrayList<BannedIP>();
    private int server_port = 42000;
    private int max_server_port = 43000;
    private int avail_port = server_port + 1;
    public boolean debugLog = true;

    // Ranks
    private Thread calculate_rank;

    // Challenge
    private Thread generate_challenge;

    public UDPServer() throws AWTException {

        try {

            DB = new ConnectDB().getConnection();
            socket = new DatagramSocket(server_port);
            print("Server Created on port " + server_port + " [version " + version + "]");
            // notification("Geo Fighter Server Started!");
            calculate_rank = new Thread(new CalculateRanks());
            calculate_rank.start();

            generate_challenge = new Thread(new Challenge());
            generate_challenge.start();

        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() throws AWTException {
        try {
            running = true;

            while (running) {
                autoRemovePlayers();

                // cap the port
                if (avail_port >= max_server_port)
                    avail_port = server_port + 1;

                byte[] buf = new byte[256];
                // print("waiting...");
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                socket.setSoTimeout(1000);

                try {
                    socket.receive(packet);

                    InetAddress address = packet.getAddress();
                    int port = packet.getPort();

                    log("Incoming from " + address + ":" + port);
                    // packet = new DatagramPacket(buf, buf.length, address, port);

                    String received = new String(packet.getData(), 0, buf.length).trim();

                    log(">> " + received);
                    processData(received, address, port);
                } catch (SocketTimeoutException e) {
                    // System.out.println("Stopped listening");
                }

            }

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processData(String msg, InetAddress address, int port) throws AWTException {

        String command = "", cmdArgs = "";
        log("processing: " + msg);

        if (msg.contains(" ")) {
            command = msg.substring(0, msg.indexOf(" "));
            log("command: " + command);
            cmdArgs = msg.substring(msg.indexOf(" ") + 1);
        }

        String value;

        // check if it is from a banned IP
        boolean banned = false;
        for (BannedIP b : banned_ips) {

            if (b.ip.equals(address)) {

                banned = true;
                log("Message From Banned IP Recieved. [" + address + "]");
                // Say nothing
                // sendMessage("banned",address,port);
                break;
            }

        }

        if (banned)
            return;
        /** NO COMMANDS BEFORE BAN CHECK */
        /** NO COMMANDS BEFORE BAN CHECK */
        /** NO COMMANDS BEFORE BAN CHECK */

        // someone is checking if the server is online
        if (command.equals("server_online")) {

            sendMessage("online", address, port);

            return;
        }

        // online player list
        if (msg.equals("onlinelist")) {

            String playerList = "";
            for (Player i : players) {

                playerList = playerList + i.name + "<!>";
            }

            if (!playerList.equals("")) {
                log("Player is :[" + playerList + "]");
                sendMessage("playerlist" + playerList, address, port);
            }
            return;
        }

        // Checking Version
        if (command.equals("vrs")) {
            value = cmdArgs;

            if (value.equals(version)) {
                sendMessage("goodvrs", address, port);
            } else {
                sendMessage("udl" + updateLink, address, port);
                sendMessage("oud" + version, address, port);
            }

            return;
        }

        // Chat Message
        if (msg.startsWith("msg")) {

            String sender_name = "";
            Player sender = null;

            // Which player was this from...
            for (Player i : players) {

                if (i.address.equals(address) && i.port == port) {
                    sender_name = i.name;
                    sender = i;
                    break;
                }
            }

            passChatToAllPlayers("msg" + sender_name + ": " + msg.substring(3), sender);
            print(sender_name + ": " + msg.substring(3));
            return;
        }

        // Login
        if (command.equals("login")) {
            String name = cmdArgs.substring(0, cmdArgs.indexOf("!"));
            String password = cmdArgs.substring(cmdArgs.indexOf("!") + 1);

            sendMessage(loginAccount(name, password), address, port);

            return;
        }

        // Get list of player's characters
        if (command.equals("characterlist")) {

            String list = "";

            list = getCharacterList(cmdArgs);

            list = list.replace("[", "").replace("]", "").trim();

            sendMessage(list, address, port);

            return;

        }

        // Get players tokens on
        if (command.equals("loadtokens")) {

            sendMessage(getCharacterTokens(cmdArgs), address, port);

            return;
        }

        // Get player quest
        if (command.equals("loadquest")) {

            sendMessage(getCharacterQuest(cmdArgs), address, port);

            return;
        }

        // Get player badges
        if (command.equals("loadbadges")) {

            sendMessage(getCharacterBadges(cmdArgs), address, port);

            return;
        }

        // Get player stats
        if (command.equals("loadstats")) {

            sendMessage(getCharacterStats(cmdArgs), address, port);

            return;
        }

        //Get RP stats
        if(command.equals("loadrp")){

            sendMessage(getCharacterRankStats(cmdArgs), address, port);

            return;
        }

        if(command.equals("loadstory")){

            sendMessage(getCharacterStory(cmdArgs), address, port);
            return;
        }

        if(command.equals("loadequip")){

            sendMessage(getCharacterEquip(cmdArgs), address, port);
            return;
        }

        if(command.equals("loadachievements")){

            sendMessage(getCharacterAchievements(cmdArgs), address, port);
            return;
        }

        if(command.equals("loadlifetimes")){

            sendMessage(getCharacterLifeTimes(cmdArgs), address, port);
            return;
        }

        if(msg.equals("loadchallenge")){

            sendMessage(getChallenge(), address, port);
            return;
        }

        // Player Logged Out
        if (msg.startsWith("plo")) {
            value = msg.substring(3);

            sendMessageFromServer("rpl" + value);
            removePlayer(value);

            print("** " + value + " has logged out **");
            return;
        }

        // Player making a new game (one was not found)
        /*
         * if(msg.substring(0,3).equals("str")){
         *
         *
         * sendMessage("prt"+avail_port,address,port);
         *
         * value = msg.substring(3);
         * Game new_game = new Game(this,value+"' Story Game",avail_port++,"Story");
         * games.add(new_game);
         * Thread new_story_game = new Thread(new_game);
         * new_story_game.start();
         *
         * //removePlayer(address,port);
         * passChatToAllPlayers("msg*"+value+"* has left to face a boss!",null);
         *
         * print("*"+msg.substring(3)+"* has left to face a boss!");
         *
         * return;
         * }
         */

        // In Game [Port]
        if (msg.startsWith("ing")) {

            value = msg.substring(3);

            for (Player i : players) {

                if (i.address.equals(address) && i.port == port) {

                    i.inGame = Integer.parseInt(value);
                    print(i.name + " is in game: " + value);
                    break;
                }
            }

            return;
        }

        // Status of the player
        if (msg.startsWith("sta")) {

            value = msg.substring(3);

            for (Player i : players) {

                if (i.address.equals(address) && i.port == port) {

                    i.status = value;
                    log(i.name + "'s status is: " + value);

                    // Check If Player Was Already in a game prior
                    if (!(i.inGame == 0) && i.status.equals("menu")) {

                        print("Sending " + i.name + " Back To Game!");
                        // return player to game [Old Host Status]
                        if (i.isHost)
                            send_buf = ("ohs" + "yes").getBytes();
                        else
                            send_buf = ("ohs" + "no").getBytes();

                        i.message(socket, send_buf);

                        send_buf = ("orl" + i.RPtoLose).getBytes();
                        i.message(socket, send_buf);

                        send_buf = ("ore" + i.RPtoEarn).getBytes();
                        i.message(socket, send_buf);

                        send_buf = ("ost" + i.stage).getBytes();
                        i.message(socket, send_buf);

                        send_buf = ("otm" + i.team).getBytes();
                        i.message(socket, send_buf);

                        send_buf = ("rtg" + i.inGame).getBytes();
                        i.message(socket, send_buf);
                    }
                    break;
                }
            }

            return;
        }

        if (msg.startsWith("hst")) {

            value = msg.substring(3);

            for (Player i : players) {

                if (i.address.equals(address) && i.port == port) {

                    if (value.equals("yes"))
                        i.isHost = true;
                    else
                        i.isHost = false;

                    log(i.name + " is host?: " + value);
                    break;
                }
            }

            return;
        }

        // rp to lose
        if (msg.startsWith("rtl")) {

            String RPtoLose = msg.substring(3);

            // Which player was this from...
            for (Player i : players) {

                if (i.address.equals(address) && i.port == port) {
                    i.RPtoLose = Integer.parseInt(RPtoLose);
                    break;
                }
            }

            return;
        }

        // rp to earn
        if (msg.startsWith("rte")) {

            String RPtoEarn = msg.substring(3);

            // Which player was this from...
            for (Player i : players) {

                if (i.address.equals(address) && i.port == port) {
                    i.RPtoEarn = Integer.parseInt(RPtoEarn);
                    break;
                }
            }

            return;
        }

        if (msg.startsWith("stg")) {

            String stageNumber = msg.substring(3);

            // Which player was this from...
            for (Player i : players) {

                if (i.address.equals(address) && i.port == port) {
                    i.stage = Integer.parseInt(stageNumber);
                    break;
                }
            }

            return;
        }

        // On team
        if (msg.startsWith("team")) {

            String onTeam = msg.substring(4);

            // Which player was this from...
            for (Player i : players) {

                if (i.address.equals(address) && i.port == port) {
                    i.team = onTeam;
                    break;
                }
            }

            return;
        }

        // Player making a new game (one was note found)
        if (msg.startsWith("pvp")) {

            sendMessage("prt" + avail_port, address, port);

            value = msg.substring(3);
            Game new_game = new Game(this, value + "' PvP Game", avail_port++, "PvP");
            games.add(new_game);
            Thread new_pvp_game = new Thread(new_game);
            new_pvp_game.start();

            // removePlayer(address,port);

            return;
        }

        // player is requesting a practice match
        if (msg.startsWith("rpm")) {

            value = msg.substring(3);
            String challenger = "";

            for (Player i : players) {
                // System.out.println("Comparing Add ["+address+"/"+i.address+"] and Port:
                // ["+port+"/"+i.port+"]");
                if (i.address.equals(address) && i.port == port) {
                    challenger = i.name;
                    break;
                }
            }

            if (!challenger.equals("")) {
                log(challenger + " issued a challenge to " + value);
                for (Player i : players) {

                    if (i.name.equals(value)) {
                        if (i.status.equals("menu")) {
                            send_buf = ("apm" + challenger).getBytes();
                            i.message(socket, send_buf);
                        } else {
                            sendMessage("xpm" + value, address, port);
                        }
                        break;
                    }
                }
            }

            return;
        }

        if (msg.startsWith("npm")) {

            value = msg.substring(3);

            String challengee = "";

            for (Player i : players) {

                if (i.address.equals(address) && i.port == port) {
                    challengee = i.name;
                    break;
                }
            }

            if (!challengee.equals("")) {
                log(challengee + " declined the challenge from " + value);
                for (Player i : players) {

                    if (i.name.equals(value)) {
                        send_buf = ("declined" + challengee).getBytes();
                        i.message(socket, send_buf);
                        break;
                    }
                }
            }

            return;
        }

        if (msg.startsWith("ypm")) {

            value = msg.substring(3);

            String challengee = "";

            for (Player i : players) {

                if (i.address.equals(address) && i.port == port) {
                    challengee = i.name;
                    break;
                }
            }

            if (!challengee.equals("")) {
                log(challengee + " accepted the challenge from " + value);

                // message the challenger
                for (Player i : players) {

                    if (i.name.equals(value)) {
                        sendMessage("g2pm" + avail_port + "A", i.address, i.port);
                        break;
                    }
                }

                // message the challengee
                sendMessage("g2pm" + avail_port + "B", address, port);

                value = msg.substring(3);
                Game new_game = new Game(this, value + "' Practice Game", avail_port++, "Practice");
                games.add(new_game);
                Thread new_practice_game = new Thread(new_game);
                new_practice_game.start();
                new_game.game_started = true;

                /*
                 * for(Player i: players){
                 *
                 * if(i.name.equals(value)){
                 * send_buf = ("accepted"+challengee).getBytes();
                 * i.message(socket, send_buf);
                 * break;
                 * }
                 * }
                 */
            }

            return;
        }

        /*
         * //Player making a new game (one was not found)
         * if(msg.substring(0,3).equals("cha")){
         *
         * sendMessage("prt"+avail_port,address,port);
         *
         * value = msg.substring(3);
         * Game new_game = new
         * Game(this,value+"' Challenge Game",avail_port++,"Challenge");
         * games.add(new_game);
         * Thread new_story_game = new Thread(new_game);
         * new_story_game.start();
         *
         * removePlayer(address,port);
         * passChatToAllPlayers("msg*"+value+"* has left to face a challenge!",null);
         * print(value+"* has left to face a challenge!");
         *
         * return;
         * }
         */

        // getting the date from the server
        if (msg.equals("date")) {

            sendMessage(LocalDate.now().toString(), address, port);

            return;
        }

        // discord link
        if (msg.startsWith("disc")) {
            sendMessage("disc" + discordLink, address, port);
            return;
        }

        // Ping
        if (msg.startsWith("ping")) {
            value = msg.substring(4);
            log(value + " has pinged the server..");
            sendMessage("pong", address, port);
            handlePlayers(value, address, port);
            return;
        }

        // Player trying to find a game,
        /*
         * if(msg.substring(0,4).equals("fstr")){
         *
         * boolean gameFound = false;
         *
         * for(Game g: games){
         * if(!g.game_started && g.playerCount < 1 && g.game_type.equals("Story")){
         * sendMessage("prt"+g.port,address,port);
         * gameFound = true;
         * //removePlayer(address,port);
         * break;
         * }
         * }
         *
         * if(!gameFound){
         * sendMessage("new",address,port);
         * }
         *
         * return;
         * }
         */

        // Player trying to find a game,
        if (msg.startsWith("fpvp")) {

            boolean gameFound = false;

            for (Game g : games) {
                if (!g.game_started && g.playerCount == 1 && g.game_type.equals("PvP")) {
                    sendMessage("prt" + g.port, address, port);
                    gameFound = true;
                    // removePlayer(address,port);
                    break;
                }
            }

            if (!gameFound) {
                sendMessage("new", address, port);
            }

            return;
        }

        // Player left to pvp
        if (msg.startsWith("f2pvp")) {

            passChatToAllPlayers("msg*" + msg.substring(5) + "* has left for some PvP!", null);
            print(msg.substring(5) + "* has left for some PvP!");
            // removePlayer(address,port);

            return;
        }

        // Player left to train
        if (msg.startsWith("ftrain")) {

            passChatToAllPlayers("msg*" + msg.substring(6) + "* has left to train!", null);
            print(msg.substring(6) + " has left to train!");
            // removePlayer(address,port);

            return;
        }

        // Player left to tutorial
        if (msg.startsWith("ftutorial")) {

            passChatToAllPlayers("msg*" + msg.substring(9) + "* has left to do the tutorial!", null);
            print(msg.substring(9) + " has left do tutorial!");
            // removePlayer(address,port);

            return;
        }

        // Player left to challenge mode
        if (msg.startsWith("fchalmode")) {

            passChatToAllPlayers("msg*" + msg.substring(9) + "* has left to face a challenge!", null);
            print(msg.substring(9) + " has left do challenge mode!");
            // removePlayer(address,port);

            return;
        }

        if (msg.startsWith("fstory")) {

            passChatToAllPlayers("msg*" + msg.substring(6) + "* has left do to story mode!", null);
            print(msg.substring(6) + " has left to do story mode.");

            return;
        }

        // New User
        if (command.equals("nuser")) {
            value = msg.substring(5);
            String name = value.substring(0, value.indexOf("!"));
            String password = value.substring(value.indexOf("!") + 1, value.indexOf("#"));
            String email = value.substring(value.indexOf("#") + 1);

            print("recieved nuser for :" + value);
            if (registerAccount(name, password, email)) {

                sendMessage("user created", address, port);
                print("** " + name + " has registered!");

            } else {

                sendMessage("failed registration", address, port);
                print("** Failed to register account for " + name + " **");
            }

            return;
        }

        // New Character
        if (msg.startsWith("nchar")) {
            value = msg.substring(5);
            String account = value.substring(0, value.indexOf("."));
            String character = value.substring(value.indexOf(".") + 1);

            // print("New Character ["+character+"] in Account ["+account+"]");
            // print("Value: "+value);
            // print("Account: "+account);
            // print("Character: "+character);

            // Add Dir for new character
            new File("Users/" + account + "/Character/" + character).mkdir();

            try {
                // Write Charcter to player's list of characters
                FileWriter fr = new FileWriter(new File("Users/" + account + "/Character/list.con"), true);
                fr.write(character + "\n");
                fr.close();

                // Write character to the name taken list
                fr = new FileWriter(new File("Users/allcharacters.con"), true);
                fr.write(character + "\n");
                fr.close();

                sendMessage("char created", address, port);

                print("** " + account + " has registered a new character: " + character + " **");
            } catch (IOException e) {
                e.printStackTrace();
            }

            return;
        }

        // File Transfer {Receiving a file}
        if (msg.startsWith("frecv")) {
            value = msg.substring(5);
            sendMessage("prt" + avail_port, address, port);

            log(address + " is saving a file! [" + value + "]");
            Thread frecv = new Thread(new FileReciever(avail_port++, value));
            frecv.start();

            return;
        }

        // File Transfer {Sending a file}
        if (msg.startsWith("ftran")) {
            value = msg.substring(5);

            sendMessage("prt" + avail_port, address, port);

            log(address + " requested a file! [" + value + "]");
            Thread ftran = new Thread(new FileSender(avail_port++, value));
            ftran.start();

            return;
        }

        // Check if Account Exist
        if (command.equals("accexist")) {
            value = msg.substring(8);

            try {

                ResultSet results = DB.createStatement()
                        .executeQuery("select * from accounts where username='" + value + "'");

                // System.out.print(results);

                if (results.next()) {
                    print("Result from DB: username is " + results.getString("username") + ", password is "
                            + results.getString("password"));

                    // System.out.println("Value is " + value);

                    if (results.getString("username").equals(value)) {
                        // System.out.println("this account exist!");
                        sendMessage("true", address, port);
                        print("account was found...");
                    } else {
                        // System.out.println("this account does NOT exist!");
                        sendMessage("false", address, port);
                        print("account was not found...");
                    }

                    print("...in accexist loop...");
                } else {
                    sendMessage("false", address, port);
                    print("account was not found...");
                }

            } catch (Exception e) {
                print("Error: couldn't check if account existed");
                e.printStackTrace();
            }
            // File temp_file = new File("Users/" + value);

            // if (temp_file.exists() && temp_file.isDirectory`()) {

            // sendMessage("true", address, port);
            // } else {
            // sendMessage("false", address, port);
            // }

            print("returning...");
            return;
        }

        // Check if Character Exist
        if (msg.startsWith("charexist")) {
            value = msg.substring(9);
            boolean charExists = false;
            try {

                File temp_file = new File("Users/allcharacters.con");
                BufferedReader br = new BufferedReader(new FileReader(temp_file));
                try {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.equalsIgnoreCase(value)) {
                            charExists = true;
                            sendMessage("true", address, port);
                            break;
                        }
                    }

                    br.close();

                    if (!charExists) {
                        sendMessage("false", address, port);
                    }
                } catch (IOException f) {
                    f.printStackTrace();
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            return;
        }

        // check a code
        if (msg.startsWith("checkcode")) {

            value = msg.substring(9);
            log("Code Recieved: " + value);
            try {

                File temp_file = new File("Codes.con");
                BufferedReader br = new BufferedReader(new FileReader(temp_file));

                try {

                    String code;
                    String item = "";
                    String itemName = "";
                    int usageLeft = 0;
                    boolean codeFound = false;

                    // load codes
                    while ((code = br.readLine()) != null) {

                        item = br.readLine();
                        itemName = br.readLine();
                        usageLeft = Integer.parseInt(br.readLine());

                        codes.add(new Code(code, item, itemName, usageLeft));
                    }

                    br.close();

                    // check if code exist

                    for (Code i : codes) {
                        log("comparing: " + i.code + "|" + value);
                        if (i.code.equals(value)) {

                            codeFound = true;

                            if (i.usageLeft > 0) {
                                sendMessage("validCode" + i.item + "!" + i.itemName, address, port);
                                i.usageLeft--;
                                log("Valid Code!");
                            } else {
                                sendMessage("codeUsed", address, port);
                                log("Code is used up!");
                            }

                            break;
                        }
                    }

                    if (codeFound == false) {
                        sendMessage("wrongCode", address, port);
                        log("Wrong Code!");
                    }

                    // save codes
                    try {
                        FileWriter f_writer = new FileWriter("Codes.con", false);

                        for (Code i : codes) {
                            log("writing code: " + i.code);
                            f_writer.write(i.code + "\n");
                            f_writer.write(i.item + "\n");
                            f_writer.write(i.itemName + "\n");
                            f_writer.write(i.usageLeft + "\n");

                        }
                        codes.clear();

                        f_writer.close();
                    } catch (IOException d) {
                        d.printStackTrace();
                    }
                } catch (IOException f) {
                    f.printStackTrace();
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            return;
        }

    }

    private String loginAccount(String name, String password) {

        try {

            ResultSet result = DB.createStatement()
                    .executeQuery(
                            "select id , password from accounts where username='" + name.trim().toLowerCase() + "'");

            if (result.next()) {

                if (password.equals(decode64(result.getString("password")))) {

                    // Return the ID encoded!
                    log("Logged in ID:" + result.getString("id"));
                    log("Encoded ID:" + encode64(result.getString("id")));

                    return encode64(result.getString("id"));
                } else {
                    log("Wrong password.");
                    return "";
                }

            } else {
                log("Account not found.");
                return "";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        log("Error looking while looking for account.");
        return "";
    }

    private boolean registerAccount(String name, String password, String email) {

        log("registering on DB...");
        try {
            int result = DB.createStatement().executeUpdate("insert into accounts(username,password,email) values ('"
                    + name.trim().toLowerCase() + "','" + encode64(password) + "','" + email + "')");

            log("Create account result is:" + result);
            if (result != 0)
                return true;
            else
                return false;

        } catch (SQLException e) {

            e.printStackTrace();
        }
        return false;
    }

    private String getCharacterList(String token) {

        String finalList = "";
        try {

            ResultSet result = DB.createStatement()
                    .executeQuery("select name from characters where account='" + decode64(token) + "'");

            ArrayList<String> list = new ArrayList<String>();

            while (result.next()) {
                list.add(result.getString("name"));
            }

            finalList = list.toString();

            log("List:" + finalList);

            return finalList;

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

    }

    private String getCharacterTokens(String token) {

        String playerTokens = "";

        try {
            String account = token.substring(0, token.indexOf(" "));
            String character = token.substring(token.indexOf(" ") + 1);

            ResultSet result = DB.createStatement()
                    .executeQuery("select tokens from characters where account='" + decode64(account) + "' and name='"
                            + character + "'");

            if (result.next()) {

                playerTokens = result.getString("tokens");
                log("Tokens found from server is " + playerTokens);

                return playerTokens;
            } else {

                return "0";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "0";
        }

    }

    private String getCharacterQuest(String token) {

        String playerQuest = "";

        try {
            String account = token.substring(0, token.indexOf(" "));
            String character = token.substring(token.indexOf(" ") + 1);

            ResultSet result = DB.createStatement()
                    .executeQuery("select quest , questcounter from characters where account='" + decode64(account)
                            + "' and name='" + character + "'");

            if (result.next()) {

                playerQuest = result.getString("quest") + "," + result.getString("questcounter");
                log("Quest found from server is " + playerQuest);

                return playerQuest;
            } else {

                return "1,0";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "1,0";
        }

    }

    private String getCharacterBadges(String token) {

        String playerBadges = "";

        try {
            String account = token.substring(0, token.indexOf(" "));
            String character = token.substring(token.indexOf(" ") + 1);

            ResultSet result = DB.createStatement()
                    .executeQuery("select badges from characters where account='" + decode64(account) + "' and name='"
                            + character + "'");

            if (result.next()) {

                playerBadges = result.getString("badges");
                log("Badges found from server is " + playerBadges);

                return playerBadges;
            } else {

                return "Newbie";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Newbie";
        }

    }

    private String getCharacterStats(String token) {

        String playerStats = "";

        try {
            String account = token.substring(0, token.indexOf(" "));
            String character = token.substring(token.indexOf(" ") + 1);

            ResultSet result = DB.createStatement()
                    .executeQuery(
                            "select level, health, strength, defense, critical, experience, skillpoints, skillpointsused from characters where account='"
                                    + decode64(account) + "' and name='" + character + "'");

            if(result.next()) {

                for(int i = 1; i <= 8; i++){
                    if(i < 8)
                        playerStats += result.getString(i)+",";
                    else
                        playerStats += result.getString(i);
                }
                log("Stats found from server is " + playerStats);

            }else{

                return "";
            }

            return playerStats;

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

    }

    private String getCharacterRankStats(String token) {

        String playerStats = "";

        try {
            String account = token.substring(0, token.indexOf(" "));
            String character = token.substring(token.indexOf(" ") + 1);

            ResultSet result = DB.createStatement()
                    .executeQuery(
                            "select rankpoints, wins, loses from characters where account='"
                                    + decode64(account) + "' and name='" + character + "'");

            if(result.next()) {

                for(int i = 1; i <= 3; i++){
                    if(i < 3)
                        playerStats += result.getString(i)+",";
                    else
                        playerStats += result.getString(i);
                }
                log("Rank Stats found from server is " + playerStats);

            }else{

                return "";
            }

            return playerStats;

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

    }

    private String getCharacterStory(String token) {

        String playerStory = "";

        try {
            String account = token.substring(0, token.indexOf(" "));
            String character = token.substring(token.indexOf(" ") + 1);

            ResultSet result = DB.createStatement()
                    .executeQuery("select story from characters where account='" + decode64(account) + "' and name='"
                            + character + "'");

            if (result.next()) {

                playerStory = result.getString("story");
                log("Story found from server is " + playerStory);

                return playerStory;
            } else {

                return "0";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "0";
        }

    }

    private String getCharacterEquip(String token) {

        String playerEquip = "";

        try {
            String account = token.substring(0, token.indexOf(" "));
            String character = token.substring(token.indexOf(" ") + 1);

            ResultSet result = DB.createStatement()
            .executeQuery("select hair, face, shirt, pant, sets.name as currentset, firstspecial.name as special1, secondspecial.name as special2, thirdspecial.name as special3, skintone, badge from characters join sets on characters.currentset = sets.id join specials firstspecial on characters.special1 = firstspecial.id join specials secondspecial on characters.special2 = secondspecial.id join specials thirdspecial on characters.special3 = thirdspecial.id where characters.account='"+ decode64(account) + "' and characters.name='" + character + "'");

            if(result.next()) {

                for(int i = 1; i <= 10; i++){
                    if(i < 10)
                        playerEquip += result.getString(i)+",";
                    else
                        playerEquip += result.getString(i);
                }
                log("Equip found from server is " + playerEquip);

            }else{

                return "";
            }

            return playerEquip;

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

    }

    private String getCharacterAchievements(String token) {

        String playerAchievements = "";

        try {
            String account = token.substring(0, token.indexOf(" "));
            String character = token.substring(token.indexOf(" ") + 1);

            ResultSet result = DB.createStatement()
                    .executeQuery("select achievements from characters where account='" + decode64(account) + "' and name='"
                            + character + "'");

            if (result.next()) {

                playerAchievements = result.getString("achievements");
                log("Achievements found from server is " + playerAchievements);

                return playerAchievements;
            } else {

                return "";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

    }


    private String getCharacterLifeTimes(String token) {

        String playerLifeTimes = "";

        try {
            String account = token.substring(0, token.indexOf(" "));
            String character = token.substring(token.indexOf(" ") + 1);

            ResultSet result = DB.createStatement()
                    .executeQuery(
                            "select lifetimetokens, lifetimecounters, lifetimedamage from characters where account='"
                                    + decode64(account) + "' and name='" + character + "'");

            if(result.next()) {

                for(int i = 1; i <= 3; i++){
                    if(i < 3)
                        playerLifeTimes += result.getString(i)+",";
                    else
                        playerLifeTimes += result.getString(i);
                }
                log("Life time found from server is " + playerLifeTimes);

            }else{

                return "";
            }

            return playerLifeTimes;

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

    }

    private String getChallenge(){

        try {

            log("Loading challenge...");
            BufferedReader f_reader = new BufferedReader( new FileReader("Challenge/1.txt"));

            String challenge = "";
            String data = f_reader.readLine();

            while(data!= null){

                log("Data:"+data);
                challenge += data+",";

                data = f_reader.readLine();
            }

            f_reader.close();

            log("Challenge is "+challenge);

            return challenge;

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

    }

    private void sendMessage(String s, InetAddress address, int port) {

        try {
            send_buf = new byte[256];
            send_buf = s.getBytes();
            DatagramPacket packet = new DatagramPacket(send_buf, send_buf.length, address, port);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void passChatToAllPlayers(String s, Player sender) {
        // print("message to send to all: " + s);
        // send_buf = new byte[256];
        send_buf = s.getBytes();

        // If it's from a player, don't message that player, otherwise send it to all
        if (sender != null) {
            for (Player i : players) {

                if (!i.equals(sender))
                    i.message(socket, send_buf);
            }
        } else {
            for (Player i : players) {
                i.message(socket, send_buf);
            }
        }
    }

    public void sendMessageFromServer(String s) {
        passChatToAllPlayers(s, null);
    }

    private void handlePlayers(String name, InetAddress addr, int port) throws AWTException {
        // print("NAME IS:" + name);
        boolean playerExist = false;
        if (name.equals(""))
            return;

        // print("getting list...");

        for (Player i : players) {
            // print("comparing "+i.name+" to "+name);
            if (i.name.equals(name)) {
                playerExist = true;

                if (i.address.equals(addr) && i.port == port) {
                    // i.address = addr;
                    // i.port = port;
                    i.CheckIn();
                } else {
                    // kick old player
                    sendMessageFromServer("log2" + i.name);

                    // register new address and port
                    i.address = addr;
                    i.port = port;
                }
                // print("PLAYER ALREADY EXIST!");
                break;
            }
        }

        // If player is new
        if (playerExist == false) {
            print(name + " (" + addr.toString() + ") has logged in!");
            log(name + " Has Logged in!");
            players.add(new Player(name, addr, port));
            sendMessageFromServer("apl" + name);

        }
    }

    private void autoRemovePlayers() {
        // System.out.println("auto removed players was called");
        for (Player i : players) {
            if (i.dueForLogOut()) {
                print(i.name + " has timed out.");
                sendMessageFromServer("rpl" + i.name);
                removePlayer(i.name);
                break;
            }
        }
    }
    /*
     * private String getPlayerName(InetAddress addr, int port){
     *
     * String name = "";
     * for (Player i : players){
     * System.out.println("Checking with "+i.name);
     * if(i.address == addr && i.port == port){
     * name = i.name;
     * break;
     * }
     * }
     * return name;
     * }
     */

    public void removePlayerFromGame(String s) {

        for (Player i : players) {

            if (i.name.equals(s)) {
                i.status = "menu";
                i.inGame = 0;
                break;
            }
        }

    }

    public void kickAllPlayers() {

        for (Player i : players) {
            log("Kicking " + i.name);
            send_buf = ("kick" + i.name).getBytes();
            i.message(socket, send_buf);
        }
        players.clear();
    }

    public void removePlayer(String s) {
        for (Player i : players) {
            if (i.name.equals(s)) {
                players.remove(i);
                break;
            }
        }
    }

    protected void removeGame(Game g) {
        games.remove(g);
    }

    public void print(String s) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        System.out.println("[" + timestamp + "]:" + s + "\n");
    }

    public void log(String s) {
        if (debugLog == true) {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            System.out.println("[" + timestamp + "]:" + s + "\n");
        }
    }

    public void notification(String message$) throws AWTException {
        // Obtain only one instance of the SystemTray object
        SystemTray tray = SystemTray.getSystemTray();

        // If the icon is a file
        Image image = Toolkit.getDefaultToolkit().createImage("icon.png");

        TrayIcon trayIcon = new TrayIcon(image, "Tray Demo");
        // Let the system resize the image if needed
        trayIcon.setImageAutoSize(true);
        // Set tooltip text for the tray icon
        trayIcon.setToolTip("System tray icon demo");
        tray.add(trayIcon);

        trayIcon.displayMessage("Geo Fighter Server", message$, MessageType.INFO);
    }

    public String encode64(String str) {

        // log("Starting Encoding on " + str);
        for (int i = 0; i < 3; i++) {
            // log("Encoding: " + str);
            str = Base64.getEncoder().encodeToString(str.getBytes());
        }

        // log("Final encode: " + str);
        return str;
    }

    public String decode64(String str) {

        // log("Starting Decoding on " + str);
        for (int i = 0; i < 3; i++) {
            // log("Decoding: " + str);
            str = new String(Base64.getDecoder().decode(str));
        }

        // log("Final decode: " + str);
        return str;
    }
}
