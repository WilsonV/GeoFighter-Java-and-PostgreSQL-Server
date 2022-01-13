/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myudpserver;

import java.io.IOException;
import java.net.*;

/**
 *
 * @author Wilson
 */
public class Player {
    
    protected String name;
    protected InetAddress address;
    protected int port;
    protected String account;
    protected int inGame;
    protected boolean isHost;
    protected String status;
    protected int RPtoLose;
    protected int RPtoEarn;
    protected int stage;
    protected int health;
    protected String team;
    protected String connection_status;
    protected long last_checkIn_time = System.currentTimeMillis();
    protected long last_disconnected_time;
    protected int auto_log_out_time = 90;
    
    Player(){
        CheckIn();
    }
    Player(String n,InetAddress a, int p){
        name = n;
        address = a;
        port = p;
        status = "menu";
        connection_status = "connected";
    }
    
    Player(InetAddress a, int p){
        address = a;
        port = p;
        status = "menu";
        connection_status = "connected";
    }
    
    protected void message(DatagramSocket socket,byte[] buf){
        
        try{
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
            socket.send(packet);
        }catch(IOException e){
            System.out.println("Failed to send message to "+this.name );
            e.printStackTrace();
        }
    }
    
    public void CheckIn(){
        
        last_checkIn_time = System.currentTimeMillis();
        connection_status = "connected";
    }
    
    public boolean dueForLogOut(){
        long current_time = System.currentTimeMillis();
            
        if( current_time - last_checkIn_time >= auto_log_out_time*1000){
               return true; 
        }
        
        return false;
    }
}
