/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myudpserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;



/**
 *
 * @author Wilson
 */
public class Game implements Runnable{
    private DatagramSocket socket;
    private byte[] send_buf = new byte[256];
    private String game_name;
    protected String game_type;
    private InetAddress address;
    protected int port;
    private boolean running;
    protected boolean game_started;
    protected int playerCount;
    private ArrayList <Player> players = new ArrayList<Player>();
    private UDPServer main_server;
    private long last_message_time = System.currentTimeMillis();
    private AutoKillTimer kill_timer;
    private Thread kill_time_thread;

    Game(UDPServer mainServer, String name,int port,String gameType){

        try{
            socket = new DatagramSocket(port);
        }catch(SocketException e){
            e.printStackTrace();
        }
        this.port = port;
        game_name = name;
        game_type = gameType;
        running = true;
        game_started = false;
        main_server = mainServer;
        kill_timer = new AutoKillTimer(this,120);

        kill_time_thread = new Thread(kill_timer);
        kill_time_thread.start();

        print("Game ("+port+") was created ["+game_type+"]");
    }


    public void run(){

        while(running){
            try{
                checkCombatLoggedPlayers();
                byte[] buf = new byte[256];
                //print("waiting...");
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                socket.setSoTimeout(1000);
                try{
                    socket.receive(packet);
                    InetAddress address = packet.getAddress();
                    int port = packet.getPort();
                    String received = new String(packet.getData(),0,buf.length).trim();

                    log("G_STR["+this.port+"] >> "+received+" ["+playerCount+"/2]");

                    processData(received,address,port);
                }catch(SocketTimeoutException e){

                }


            }catch(IOException e){
                e.printStackTrace();
            }
        }

        socket.close();
    }

    private void processData(String msg,InetAddress address,int port){
        //print("processing: " + msg);

        //check in with timer
        kill_timer.CheckIn();

        if(msg.equals("start")){
            game_started = true;

        }

        //for when player that is NOT host is joinging a room that is restarting
        if(msg.equals("ready?")){
            log(address+" is asking if room is ready");
            if(playerCount == 1){
                log("room is ready");
                sendMessage("yes",address,port);
            }
            else
            {
                log("room is NOT ready");
                sendMessage("no"+playerCount,address,port);
            }

            return;
        }

        if(msg.equals("restart")){

            log("Game ("+this.port+") is RESTARTING.");
            for(Player i:players){
                main_server.removePlayerFromGame(i.name);
            }
            players.clear();
            playerCount = 0;
            game_started = false;

            return;
        }


        if(msg.substring(0,3).equals("acc")){

            String account_name = msg.substring(3);

            //Which player was this from...
            for(Player i: players){

                if(i.address.equals(address) && i.port == port){
                    i.account = account_name;
                    break;
                }
            }

            return;
        }

        if(msg.substring(0,3).equals("chr")){

            String character_name = msg.substring(3);

            //Which player was this from...
            for(Player i: players){

                if(i.address.equals(address) && i.port == port){
                    i.name = character_name;
                    break;
                }
            }

            return;
        }

        if(msg.substring(0,3).equals("lfe")){

            int temp_health = Integer.parseInt(msg.substring(msg.indexOf("!")+1,msg.indexOf(".") ) );

            //Which player was this from...
            for(Player i: players){

                if(i.address.equals(address) && i.port == port){
                    i.health = temp_health;
                    break;
                }
            }

            //do NOT return, because this needs to go to the players too.
        }

        if(msg.equals("lhq")){


            //Which player was this from...
            for(Player i: players){

                if(i.address.equals(address) && i.port == port){
                    send_buf =  ("ohl"+i.health).getBytes();
                    i.message(socket, send_buf);
                    break;
                }
            }

            return;
        }

        if(msg.substring(0,3).equals("rtl")){

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

        //Add player if not added already and keep player at max
        if(playerAlreadyExist(address,port,msg)){
            send_buf = msg.getBytes();
            for(Player i: players){
                //print("Relaying message to player!");
                if(!i.address.equals(address) || i.port != port){
                    i.message(socket,send_buf);

                }
            }
        }

        //player left the room, minus the count so more can join and delete the player
        if(msg.substring(0,3).equals("ext")){

             for(Player i: players){

                if(i.address.equals(address) && i.port == port){
                    players.remove(i);
                    playerCount--;
                    log("Player left game...port:["+this.port+"] Players: ["+playerCount+"/2]");

                    //if no one is left in the room, kill the game [Bug fix]
                    if(playerCount <= 0){
                        running = false;
                        kill_timer.stopRunning();
                        print("Game ("+this.port+") has ended, no one is in the room");
                        main_server.removeGame(this);
                    }
                    i = null;
                    break;
                }
            }

            //do not return
        }





        if(msg.equals("kill room")){

            for(Player i:players){
                main_server.removePlayerFromGame(i.name);
            }

            running = false;
            kill_timer.stopRunning();
            print("Game ("+this.port+") has ended, the host left.");
            main_server.removeGame(this);
        }

    }

    private boolean playerAlreadyExist(InetAddress addr,int port,String s){
        //print("NAME IS:" + name);
        boolean playerExist = false;

       //print("getting list...");

        for(Player i : players){
            //print("comparing "+i.name+" to "+name);
            if( i.address.equals(addr) && i.port == port){
                playerExist = true;
                if(i.connection_status.equals("lost"))
                    tellOtherPlayersAmBack(i);

                i.CheckIn();
                //print("PLAYER ALREADY EXIST!");
                break;
            }
        }

        //If player is new
        if(playerExist == false && playerCount < 2 && s.equals("enter")){
            //System.out.println("Added new player from message:"+s);
            players.add(new Player(addr,port));
            playerCount++;
            log("Adding new player...port:["+port+"] Players: ["+playerCount+"/2]");

            sendMessage("ready",addr,port);
            sendMessage("cht[Server]:Hello",addr,port);
        }

        return playerExist;
    }

    private void sendMessage(String s,InetAddress address, int port){

        try{
           //send_buf = new byte[256];
           send_buf = s.getBytes();
            DatagramPacket packet = new DatagramPacket(send_buf, send_buf.length, address, port);
            socket.send(packet);
        }catch(IOException e){
            e.printStackTrace();
        }

    }

    private void print(String s) {
        main_server.print(s);
    }

    private void log(String s){
        main_server.log(s);
    }

    public void autoKillGame(){

            //Write Kill Room To Players in the room
            String msg = "kill room";
            //send_buf = new byte[256];
            send_buf = msg.getBytes();

            for(Player i: players){
                //print("Relaying message to player!");
                if(!i.address.equals(address) || i.port != port){
                    i.message(socket,send_buf);
                }

                //remove player from this game on the main server
                main_server.removePlayerFromGame(i.name);
            }

            //Print Kill Room, and Remove Game

            running = false;
            //kill_time_thread.stopRunning();
            print("Game ("+this.port+") has auto-decayed");
            main_server.removeGame(this);


    }

    private void checkCombatLoggedPlayers(){
        String line;
        int rp;
        int wins;
        int losses;
        long current_time = System.currentTimeMillis();
        long lost_connection_time;

        if(game_started == true){
            for(Player i: players){
                //check to see if the player has sent a message in last 10 seconds
                if(current_time - i.last_checkIn_time >= 10*1000){

                    i.connection_status = "lost";
                }
                //else{
               //     i.connection_status = "connected";
                //}

                //if the player is over the amount of time for a log out
                if(i.dueForLogOut() && game_type.equals("PvP")){
                    log(i.name+" has timed out of a game.");
                    messagePlayers("lfe"+i.name+"!0");
                    messagePlayers("act"+i.name+"!drop dead");
                    main_server.removePlayerFromGame(i.name);

                    try{
                        File temp_file = new File("Users/"+i.account+"/Character/"+i.name+"/rp.con");
                        BufferedReader br = new BufferedReader(new FileReader(temp_file));

                        try{
                            //load rp
                            line = br.readLine();
                            if(line == null || line == "")
                                line = "0";

                            rp = Integer.parseInt(line);

                             //load wins
                            line = br.readLine();
                            if(line == null || line == "")
                                line = "0";

                            wins = Integer.parseInt(line);

                             //load losses
                            line = br.readLine();
                            if(line == null || line == "")
                                line = "0";

                            losses = Integer.parseInt(line);

                            //close stream
                            br.close();



                            log(i.name+"'s rp is "+rp);
                            log(i.name+"'s wins is "+wins);
                            log(i.name+"'s losses is "+losses);

                            //deduct 100 rp or more
                            if(i.RPtoLose > 100)
                                rp=rp-i.RPtoLose;
                            else
                                rp=rp-100;

                            if(rp<0)rp=0;

                            losses++; //add 1 to losses

                            log(i.name+"'s new rp is "+rp);
                            //write rp back
                            FileWriter fr = new FileWriter(new File("Users/"+i.account+"/Character/"+i.name+"/rp.con"),false);
                            fr.write(rp+"\n");
                            fr.write(wins+"\n");
                            fr.write(losses+"\n");
                            fr.close();

                        }catch(IOException f){
                            f.printStackTrace();
                        }
                    }catch(FileNotFoundException e){
                        e.printStackTrace();
                    }
                    players.remove(i);
                    break;
                }
            }

            //inform other players of players who lost connection
            for(Player i: players){
                for(Player j: players){

                    if(i != j){ //do not send to self
                        if(j.connection_status.equals("lost") && (current_time - j.last_disconnected_time >= 1000)){
                            lost_connection_time = j.auto_log_out_time - (current_time - j.last_checkIn_time)/1000;
                            send_buf = (j.connection_status+"@"+j.name+"!"+lost_connection_time).getBytes();
                            i.message(socket, send_buf);
                            j.last_disconnected_time = System.currentTimeMillis(); //wait 1 sec before sending next disconnect time
                        }
                    }
                }
            }
        }
    }

    private void tellOtherPlayersAmBack(Player p){

        send_buf = ("conn"+p.name).getBytes();
        for(Player i: players){

            if(i != p){

                i.message(socket, send_buf);
            }
        }

    }

    /*private void messageSender(DatagramSocket socket,byte[] buf,InetAddress address,int port){

        try{
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
            socket.send(packet);
        }catch(IOException e){
            System.out.println("Failed to send message to "+address+":"+port );
            e.printStackTrace();
        }
    }*/

    private void messagePlayers(String s){
        send_buf = new byte[256];
        send_buf = s.getBytes();

        for(Player i: players){

            i.message(socket, send_buf);
        }

    }
}
