/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myudpserver;

import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.time.LocalDate;
/**
 *
 * @author Wilson
 */
public class UDPServer{

    private DatagramSocket socket;
    private boolean running;

    private byte[] send_buf = new byte[256];
    final String version = "0.1.8";
    final String updateLink = "https://gamejolt.com/games/geofighter/436528";
    final String discordLink = "https://discord.gg/DKrk3fD";
    private ArrayList <Player> players = new ArrayList<Player>();
    private ArrayList <Game> games = new ArrayList<Game>();
    private ArrayList <Code> codes = new ArrayList<Code>();
    protected ArrayList <BannedIP> banned_ips = new ArrayList<BannedIP>();
    private int server_port = 42000;
    private int max_server_port = 43000;
    private int avail_port = server_port + 1;
    public boolean debugLog = true;

    //Ranks
    private Thread calculate_rank;

    //Challenge
    private Thread generate_challenge;

    public UDPServer() throws AWTException{

        try{

            socket = new DatagramSocket(server_port);
            print("Server Created on port "+server_port+" [version "+version+"]");
            //notification("Geo Fighter Server Started!");
           calculate_rank = new Thread(new CalculateRanks());
           calculate_rank.start();

            generate_challenge = new Thread(new Challenge());
            generate_challenge.start();

        }catch(SocketException e){
            e.printStackTrace();
        }
    }

    public void run() throws AWTException{
        try{
            running = true;

            while (running) {
                autoRemovePlayers();

                //cap the port
                if(avail_port >= max_server_port )
                    avail_port = server_port+1;

                byte[] buf = new byte[256];
                //print("waiting...");
                DatagramPacket packet
                  = new DatagramPacket(buf, buf.length);

                socket.setSoTimeout(1000);

                try{
                    socket.receive(packet);


                    InetAddress address = packet.getAddress();
                    int port = packet.getPort();

                    log("Incoming from " +address+":"+port);
                    //packet = new DatagramPacket(buf, buf.length, address, port);

                    String received
                        = new String(packet.getData(),0,buf.length).trim();

                    log(">> "+received);
                    processData(received,address,port);
                }catch(SocketTimeoutException e){
                   //System.out.println("Stopped listening");
                }

            }

            socket.close();
         }catch(IOException e){
            e.printStackTrace();
        }
    }

    private void processData(String msg,InetAddress address,int port) throws AWTException{


        log("processing: " + msg);
        boolean banned = false;
        String value;

        //check if it is from a banned IP
        for(BannedIP b: banned_ips){

            if(b.ip.equals(address)){

                banned = true;
                log("Message From Banned IP Recieved. ["+address+"]");
                //Say nothing
                //sendMessage("banned",address,port);
                break;
            }

        }

        if(banned)return;

        //someone is checking if the server is online
        if(msg.equals("server_online?")){

            sendMessage("online",address,port);

            return;
        }

        //online player list
        if(msg.equals("onlinelist")){

            String playerList = "";
            for(Player i:players){

                playerList = playerList+i.name+"<!>";
            }

            if(!playerList.equals("")){
                log("Player is :["+playerList+"]");
                sendMessage("playerlist"+playerList,address,port);
            }
            return;
        }

        //Checking Version
        if(msg.startsWith("vrs")){
            value = msg.substring(3);

            if(value.equals(version)){
                sendMessage("goodvrs",address,port);
            }else{
                sendMessage("udl"+updateLink,address,port);
                sendMessage("oud"+version,address,port);
            }

           return;
        }

         //Chat Message
        if(msg.startsWith("msg")){

            String sender_name = "";
            Player sender = null;

            //Which player was this from...
            for(Player i: players){

                if(i.address.equals(address) && i.port == port){
                    sender_name = i.name;
                    sender = i;
                    break;
                }
            }

            passChatToAllPlayers("msg"+sender_name+": "+msg.substring(3),sender );
            print(sender_name+": "+msg.substring(3));
            return;
        }

        //Player Logged Out
        if(msg.startsWith("plo")){
            value = msg.substring(3);

            sendMessageFromServer("rpl"+value);
            removePlayer(value);

            print("** "+value+" has logged out **");
            return;
        }

        //Player making a new game (one was not found)
       /* if(msg.substring(0,3).equals("str")){


            sendMessage("prt"+avail_port,address,port);

            value = msg.substring(3);
            Game new_game = new Game(this,value+"' Story Game",avail_port++,"Story");
            games.add(new_game);
            Thread new_story_game = new Thread(new_game);
            new_story_game.start();

            //removePlayer(address,port);
            passChatToAllPlayers("msg*"+value+"* has left to face a boss!",null);

            print("*"+msg.substring(3)+"* has left to face a boss!");

            return;
        }*/

       //In Game [Port]
       if(msg.startsWith("ing")){

           value = msg.substring(3);

           for(Player i: players){

               if(i.address.equals(address) && i.port == port){

                 i.inGame = Integer.parseInt(value);
                 print(i.name+" is in game: "+value);
                 break;
               }
           }

           return;
       }

       //Status of the player
       if(msg.startsWith("sta")){

           value = msg.substring(3);

           for(Player i: players){

               if(i.address.equals(address) && i.port == port){

                 i.status = value;
                 log(i.name+"'s status is: "+value);

                 //Check If Player Was Already in a game prior
                 if(!(i.inGame ==0) && i.status.equals("menu")){

                     print("Sending "+i.name+" Back To Game!");
                     //return player to game [Old Host Status]
                     if(i.isHost)
                        send_buf = ("ohs"+"yes").getBytes();
                     else
                         send_buf = ("ohs"+"no").getBytes();

                     i.message(socket, send_buf);

                     send_buf = ("orl"+i.RPtoLose).getBytes();
                     i.message(socket, send_buf);

                     send_buf = ("ore"+i.RPtoEarn).getBytes();
                     i.message(socket, send_buf);

                     send_buf = ("ost"+i.stage).getBytes();
                     i.message(socket, send_buf);

                     send_buf = ("otm"+i.team).getBytes();
                     i.message(socket, send_buf);

                     send_buf = ("rtg"+i.inGame).getBytes();
                     i.message(socket, send_buf);
                 }
                 break;
               }
           }

           return;
       }

        if(msg.startsWith("hst")){

           value = msg.substring(3);

           for(Player i: players){

               if(i.address.equals(address) && i.port == port){

                 if(value.equals("yes"))
                     i.isHost = true;
                 else
                     i.isHost = false;

                 log(i.name+" is host?: "+value);
                 break;
               }
           }

           return;
       }

        //rp to lose
        if(msg.startsWith("rtl")){

            String RPtoLose = msg.substring(3);

            //Which player was this from...
            for(Player i: players){

                if(i.address.equals(address) && i.port == port){
                    i.RPtoLose = Integer.parseInt(RPtoLose);
                    break;
                }
            }

            return;
        }

        //rp to earn
        if(msg.startsWith("rte")){

            String RPtoEarn = msg.substring(3);

            //Which player was this from...
            for(Player i: players){

                if(i.address.equals(address) && i.port == port){
                    i.RPtoEarn = Integer.parseInt(RPtoEarn);
                    break;
                }
            }

            return;
        }

        if(msg.startsWith("stg")){

            String stageNumber = msg.substring(3);

            //Which player was this from...
            for(Player i: players){

                if(i.address.equals(address) && i.port == port){
                    i.stage = Integer.parseInt(stageNumber);
                    break;
                }
            }

            return;
        }

        //On team
        if(msg.startsWith("team")){

            String onTeam = msg.substring(4);

            //Which player was this from...
            for(Player i: players){

                if(i.address.equals(address) && i.port == port){
                    i.team = onTeam;
                    break;
                }
            }

            return;
        }

        //Player making a new game (one was note found)
        if(msg.startsWith("pvp")){

            sendMessage("prt"+avail_port,address,port);

            value = msg.substring(3);
            Game new_game = new Game(this,value+"' PvP Game",avail_port++,"PvP");
            games.add(new_game);
            Thread new_pvp_game = new Thread(new_game);
            new_pvp_game.start();

            //removePlayer(address,port);

            return;
        }

        //player is requesting a practice match
        if(msg.startsWith("rpm")){

            value = msg.substring(3);
            String challenger = "";

            for (Player i: players){
                //System.out.println("Comparing Add ["+address+"/"+i.address+"] and Port: ["+port+"/"+i.port+"]");
                if(i.address.equals(address) && i.port == port){
                     challenger = i.name;
                    break;
                }
            }

            if(!challenger.equals("")){
                log(challenger+" issued a challenge to "+value);
                for(Player i:players){

                    if(i.name.equals(value)){
                        if(i.status.equals("menu")){
                            send_buf = ("apm"+challenger).getBytes();
                            i.message(socket, send_buf);
                        }else{
                            sendMessage("xpm"+value,address,port);
                        }
                        break;
                    }
                }
            }

            return;
        }

        if(msg.startsWith("npm")){

            value = msg.substring(3);

            String challengee = "";

            for (Player i: players){

                if(i.address.equals(address) && i.port == port){
                     challengee = i.name;
                    break;
                }
            }

            if(!challengee.equals("")){
                log(challengee+" declined the challenge from "+value);
                for(Player i: players){

                    if(i.name.equals(value)){
                        send_buf = ("declined"+challengee).getBytes();
                        i.message(socket, send_buf);
                        break;
                    }
                }
            }

            return;
        }

        if(msg.startsWith("ypm")){

            value = msg.substring(3);

            String challengee = "";

            for (Player i: players){

                if(i.address.equals(address) && i.port == port){
                     challengee = i.name;
                    break;
                }
            }

            if(!challengee.equals("")){
                log(challengee+" accepted the challenge from "+value);

                //message the challenger
                for (Player i: players){

                    if(i.name.equals(value)){
                        sendMessage("g2pm"+avail_port+"A",i.address,i.port);
                        break;
                    }
                }

                //message the challengee
                sendMessage("g2pm"+avail_port+"B",address,port);

                value = msg.substring(3);
                Game new_game = new Game(this,value+"' Practice Game",avail_port++,"Practice");
                games.add(new_game);
                Thread new_practice_game = new Thread(new_game);
                new_practice_game.start();
                new_game.game_started = true;

               /* for(Player i: players){

                    if(i.name.equals(value)){
                        send_buf = ("accepted"+challengee).getBytes();
                        i.message(socket, send_buf);
                        break;
                    }
                }*/
            }

            return;
        }

        /*//Player making a new game (one was not found)
        if(msg.substring(0,3).equals("cha")){

            sendMessage("prt"+avail_port,address,port);

            value = msg.substring(3);
            Game new_game = new Game(this,value+"' Challenge Game",avail_port++,"Challenge");
            games.add(new_game);
            Thread new_story_game = new Thread(new_game);
            new_story_game.start();

            removePlayer(address,port);
            passChatToAllPlayers("msg*"+value+"* has left to face a challenge!",null);
            print(value+"* has left to face a challenge!");

            return;
        }*/

        //getting the date from the server
        if(msg.equals("date")){

            sendMessage(LocalDate.now().toString(),address,port);

            return;
        }

        //discord link
        if(msg.startsWith("disc")){
                sendMessage("disc"+discordLink,address,port);
           return;
        }

        //Ping
        if(msg.startsWith("ping")){
            value = msg.substring(4);
            log(value+" has pinged the server..");
            sendMessage("pong",address,port);
            handlePlayers(value,address,port);
            return;
        }

        //Player trying to find a game,
        /*if(msg.substring(0,4).equals("fstr")){

            boolean gameFound = false;

            for(Game g: games){
                if(!g.game_started && g.playerCount < 1 && g.game_type.equals("Story")){
                    sendMessage("prt"+g.port,address,port);
                    gameFound = true;
                    //removePlayer(address,port);
                    break;
                }
            }

            if(!gameFound){
                sendMessage("new",address,port);
            }

            return;
        }*/

        //Player trying to find a game,
        if(msg.startsWith("fpvp")){

            boolean gameFound = false;

            for(Game g: games){
                if(!g.game_started && g.playerCount == 1 && g.game_type.equals("PvP")){
                    sendMessage("prt"+g.port,address,port);
                    gameFound = true;
                    //removePlayer(address,port);
                    break;
                }
            }

            if(!gameFound){
                sendMessage("new",address,port);
            }

            return;
        }

        //Player left to pvp
        if(msg.startsWith("f2pvp")){

            passChatToAllPlayers("msg*"+msg.substring(5)+"* has left for some PvP!",null);
            print(msg.substring(5)+"* has left for some PvP!");
            //removePlayer(address,port);

            return;
        }

        //Player left to train
        if(msg.startsWith("ftrain")){

            passChatToAllPlayers("msg*"+msg.substring(6)+"* has left to train!",null);
            print(msg.substring(6)+" has left to train!");
            //removePlayer(address,port);

            return;
        }

        //Player left to tutorial
        if(msg.startsWith("ftutorial")){

            passChatToAllPlayers("msg*"+msg.substring(9)+"* has left to do the tutorial!",null);
            print(msg.substring(9)+" has left do tutorial!");
            //removePlayer(address,port);

            return;
        }

        //Player left to challenge mode
        if(msg.startsWith("fchalmode")){

            passChatToAllPlayers("msg*"+msg.substring(9)+"* has left to face a challenge!",null);
            print(msg.substring(9)+" has left do challenge mode!");
            //removePlayer(address,port);

            return;
        }

        if(msg.startsWith("fstory")){

            passChatToAllPlayers("msg*"+msg.substring(6)+"* has left do to story mode!",null);
            print(msg.substring(6)+ " has left to do story mode.");

            return;
        }

        //New User
        if(msg.startsWith("nuser")){
            value = msg.substring(5);

            try{

                new File("Users/"+value).mkdir();
                new File("Users/"+value+"/Character").mkdir();
                new File("Users/"+value+"/Character/list.con").createNewFile();
                sendMessage("user created",address,port);

                print("** "+msg.substring(5)+" has registered!");
            }catch(IOException e){
                e.printStackTrace();
            }

            return;
        }

        //New Character
        if(msg.startsWith("nchar")){
            value = msg.substring(5);
            String account = value.substring(0,value.indexOf("."));
            String character = value.substring(value.indexOf(".")+1);

            //print("New Character ["+character+"] in Account ["+account+"]");
            //print("Value: "+value);
            //print("Account: "+account);
            //print("Character: "+character);

            //Add Dir for new character
            new File("Users/"+account+"/Character/"+character).mkdir();

            try{
                //Write Charcter to player's list of characters
                FileWriter fr = new FileWriter(new File("Users/"+account+"/Character/list.con"),true);
                fr.write(character+"\n");
                fr.close();

                //Write character to the name taken list
                fr = new FileWriter(new File("Users/allcharacters.con"),true);
                fr.write(character+"\n");
                fr.close();

                sendMessage("char created",address,port);

                print("** "+account+" has registered a new character: "+character+" **");
            }catch(IOException e){
                e.printStackTrace();
            }

            return;
        }




        //File Transfer {Receiving a file}
        if(msg.startsWith("frecv")){
            value = msg.substring(5);
            sendMessage("prt"+avail_port,address,port);

            log(address+ " is saving a file! ["+value+"]");
            Thread frecv = new Thread(new FileReciever(avail_port++,value));
            frecv.start();

            return;
        }

        //File Transfer {Sending a file}
        if(msg.startsWith("ftran")){
            value = msg.substring(5);

            sendMessage("prt"+avail_port,address,port);

            log(address+ " requested a file! ["+value+"]");
            Thread ftran = new Thread(new FileSender(avail_port++,value));
            ftran.start();

            return;
        }



        //Check if Account Exist
        if(msg.startsWith("accexist")){
            value = msg.substring(8);
            File temp_file = new File("Users/"+value);

            if(temp_file.exists() && temp_file.isDirectory()){

                sendMessage("true",address,port);
            }else{
                sendMessage("false",address,port);
            }

            return;
        }

        //Check if Character Exist
        if(msg.startsWith("charexist")){
            value = msg.substring(9);
            boolean charExists = false;
            try{

                File temp_file = new File("Users/allcharacters.con");
                BufferedReader br = new BufferedReader(new FileReader(temp_file));
                try{
                    String line;
                    while((line = br.readLine()) != null){
                        if(line.equalsIgnoreCase(value)){
                            charExists = true;
                            sendMessage("true",address,port);
                            break;
                        }
                     }

                    br.close();

                    if(!charExists){
                        sendMessage("false",address,port);
                    }
                }catch(IOException f){
                    f.printStackTrace();
                }

            }catch(FileNotFoundException e){
                e.printStackTrace();
            }

            return;
        }


        //check a code
        if(msg.startsWith("checkcode")){

            value = msg.substring(9);
            log("Code Recieved: "+value);
            try{

                File temp_file = new File("Codes.con");
                BufferedReader br = new BufferedReader(new FileReader(temp_file));

                try{

                    String code;
                    String item = "";
                    String itemName = "";
                    int usageLeft = 0;
                    boolean codeFound = false;

                    //load codes
                    while((code = br.readLine()) != null){

                            item = br.readLine();
                            itemName = br.readLine();
                            usageLeft = Integer.parseInt(br.readLine());

                            codes.add(new Code(code,item,itemName,usageLeft));
                    }

                    br.close();

                    //check if code exist

                    for(Code i: codes){
                        log("comparing: "+i.code+"|"+value);
                        if(i.code.equals(value)){

                            codeFound = true;

                            if(i.usageLeft > 0){
                                sendMessage("validCode"+i.item+"!"+i.itemName,address,port);
                                i.usageLeft--;
                                log("Valid Code!");
                            }else{
                                sendMessage("codeUsed",address,port);
                                log("Code is used up!");
                            }

                            break;
                        }
                    }

                    if(codeFound == false){
                        sendMessage("wrongCode",address,port);
                        log("Wrong Code!");
                    }


                    //save codes
                    try{
                        FileWriter f_writer = new FileWriter("Codes.con",false);

                        for(Code i: codes){
                            log("writing code: "+i.code);
                            f_writer.write(i.code+"\n");
                            f_writer.write(i.item+"\n");
                            f_writer.write(i.itemName+"\n");
                            f_writer.write(i.usageLeft+"\n");

                        }
                        codes.clear();

                        f_writer.close();
                    }catch(IOException d){
                        d.printStackTrace();
                    }
                }catch(IOException f){
                    f.printStackTrace();
                }


            }catch(FileNotFoundException e){
                e.printStackTrace();
            }

            return;
        }

    }


    private void sendMessage(String s,InetAddress address, int port){

        try{
           send_buf = new byte[256];
           send_buf = s.getBytes();
            DatagramPacket packet = new DatagramPacket(send_buf, send_buf.length, address, port);
            socket.send(packet);
        }catch(IOException e){
            e.printStackTrace();
        }

    }

    private void passChatToAllPlayers(String s,Player sender){
        //print("message to send to all: " + s);
        //send_buf = new byte[256];
        send_buf = s.getBytes();

        //If it's from a player, don't message that player, otherwise send it to all
        if(sender != null){
            for(Player i: players){

                if(!i.equals(sender))
                    i.message(socket, send_buf);
            }
        }else{
            for(Player i: players){
                i.message(socket, send_buf);
            }
        }
    }


    public void sendMessageFromServer(String s) {
        passChatToAllPlayers(s,null);
    }


    private void handlePlayers(String name, InetAddress addr,int port) throws AWTException{
        //print("NAME IS:" + name);
        boolean playerExist = false;
        if (name.equals("")) return;

       //print("getting list...");

        for(Player i : players){
            //print("comparing "+i.name+" to "+name);
            if( i.name.equals(name)){
                playerExist = true;

                if(i.address.equals(addr) && i.port == port){
                //i.address = addr;
                //i.port = port;
                    i.CheckIn();
                }
                else
                {
                   //kick old player
                    sendMessageFromServer("log2"+i.name);

                    //register new address and port
                    i.address = addr;
                    i.port = port;
                }
                //print("PLAYER ALREADY EXIST!");
                break;
            }
        }

        //If player is new
        if(playerExist == false){
            print(name+" ("+addr.toString()+") has logged in!");
            log(name+" Has Logged in!");
            players.add(new Player(name,addr,port));
            sendMessageFromServer("apl"+name);

        }
    }

    private void autoRemovePlayers(){
        //System.out.println("auto removed players was called");
        for(Player i: players){
            if(i.dueForLogOut()){
                print(i.name+" has timed out.");
                sendMessageFromServer("rpl"+i.name);
                removePlayer(i.name);
                break;
            }
        }
    }
 /*   private String getPlayerName(InetAddress addr, int port){

        String name = "";
        for (Player i : players){
            System.out.println("Checking with "+i.name);
            if(i.address == addr && i.port == port){
                name = i.name;
                break;
            }
        }
        return name;
    }*/

    public void removePlayerFromGame(String s){

        for(Player i:players){

            if(i.name.equals(s)){
                i.status = "menu";
                i.inGame = 0;
                break;
            }
        }

    }
    public void kickAllPlayers(){

        for(Player i: players){
            log("Kicking "+i.name);
            send_buf = ("kick"+i.name).getBytes();
            i.message(socket, send_buf);
        }
        players.clear();
    }

    public void removePlayer(String s){
        for(Player i: players){
                if(i.name.equals(s)){
                    players.remove(i);
                    break;
                }
            }
    }
    protected void removeGame(Game g){
        games.remove(g);
    }

    public void print(String s) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        System.out.println("["+timestamp+"]:"+s+"\n");
    }

    public void log(String s){
        if(debugLog == true){
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            System.out.println("["+timestamp+"]:"+s+"\n");
        }
    }

    public void notification(String message$) throws AWTException {
        //Obtain only one instance of the SystemTray object
        SystemTray tray = SystemTray.getSystemTray();

        //If the icon is a file
        Image image = Toolkit.getDefaultToolkit().createImage("icon.png");

        TrayIcon trayIcon = new TrayIcon(image, "Tray Demo");
        //Let the system resize the image if needed
        trayIcon.setImageAutoSize(true);
        //Set tooltip text for the tray icon
        trayIcon.setToolTip("System tray icon demo");
        tray.add(trayIcon);

        trayIcon.displayMessage("Geo Fighter Server", message$, MessageType.INFO);
    }
}
