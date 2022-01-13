/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myudpserver;

/**
 *
 * @author Gamer
 */
public class AutoKillTimer implements Runnable{
    
    private long last_checkIn_time = System.currentTimeMillis();
    
    private boolean running = true;
    private Game game;
    private int kill_time_length;
    
    AutoKillTimer(Game game, int kill_time_length ){
       
      this.game = game;
      this.kill_time_length = kill_time_length;
      
    }

    public void stopRunning(){
        this.running = false;
        //System.out.println("Kill Timer told to stop...");
    }

    public void run(){
        
        while(this.running){
            
            try{
            
            Thread.sleep(1000);
            long current_time = System.currentTimeMillis();
            
            System.out.print("");
        
            if( current_time - last_checkIn_time >= kill_time_length*1000){
                
                running = false;
                game.autoKillGame();
            }
            
            }catch(InterruptedException e){
                
            }
        }
        //System.out.println("Kill Timer has stopped running...");
    }
    

    public void CheckIn(){
        
        last_checkIn_time = System.currentTimeMillis();
    }
}
