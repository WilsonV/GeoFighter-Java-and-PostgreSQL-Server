/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myudpserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author Gamer
 */
public class CalculateRanks implements Runnable{
   
    private boolean running = true;
    private long rank_last_ran_time = System.currentTimeMillis();
    private long rank_current_time = System.currentTimeMillis();;
    private int rank_minute_to_run = 2;
    
    //Stuff for rank files
    BufferedReader br;
        
    ArrayList<String> rank_name = new ArrayList<String>();
    ArrayList<Integer> rank_value = new ArrayList<Integer>();
    ArrayList<Integer> rank_wins = new ArrayList<Integer>();
    ArrayList<Integer> rank_losses = new ArrayList<Integer>();
        
    File userFolders = new File("Users/");
    File characterFolders;
        
    File[] userFiles;
    File[] characterFiles;
    File user_rank_file;
    
    public void run(){
        
        while(running){
            
            try{
            Thread.sleep(1000);
            rank_current_time = System.currentTimeMillis();
            
            if(rank_current_time - rank_last_ran_time >= (rank_minute_to_run)*(60)*(1000)){
                
                //System.out.println("Calculating Ranks...");
                ///////START///////
                getRanks();
                sortRank(rank_name,rank_value,rank_wins,rank_losses);
                saveRank(rank_name,rank_value,rank_wins,rank_losses);
                
                //clear ranks
                rank_name.clear();
                rank_value.clear();
                rank_wins.clear();
                rank_losses.clear();
                
                //System.out.println("Ranks Calculated!");
                rank_last_ran_time = System.currentTimeMillis();
                ///////END/////////
            }
            
            }catch(InterruptedException e){
                
            }
        }
    }
    
    private void getRanks(){
        String tempRank = "";
        String tempWins = "";
        String tempLosses = "";
        
        userFiles = userFolders.listFiles();
        
        for (File file : userFiles){
                
            if (file.isDirectory()){
                
                //System.out.println("Getting Rank For: "+file.getName());
               
                    characterFolders = new File("Users/"+file.getName()+"/Character");
                    characterFiles = characterFolders.listFiles();
                
                    for(File file2 : characterFiles){
                    
                        if(file2.isDirectory()){
                            //System.out.println(">>"+file2.getName());
                            
                            
                                user_rank_file = new File("Users/"+file.getName()+"/Character/"+file2.getName()+"/rp.con");
                           
                            try{
                                br = new BufferedReader(new FileReader(user_rank_file));
                                
                                //add the name
                                rank_name.add(file2.getName());
                                
                                // add the rank
                                tempRank = br.readLine();
                                if(tempRank == null || tempRank == "")
                                    tempRank = "0";
                                
                                rank_value.add(Integer.parseInt(tempRank));
                                
                                 // add the wins
                                tempWins = br.readLine();
                                if(tempWins == null || tempWins == "")
                                    tempWins = "0";
                                
                                rank_wins.add(Integer.parseInt(tempWins));
                            
                                 // add the losses
                                tempLosses = br.readLine();
                                if(tempLosses == null || tempLosses == "")
                                    tempLosses = "0";
                                
                                rank_losses.add(Integer.parseInt(tempLosses));
                                
                                br.close();
                                }catch(IOException e){
                            
                                //log(file2.getName()+"'s RP file not found");
                            }
                        }
                    }
                    characterFiles = null;
            }
            
        }   
    }

    private void  sortRank(ArrayList<String> names,ArrayList<Integer> ranks,ArrayList<Integer> wins,ArrayList<Integer> losses ){
        boolean changeMade = true;
        String temp_name;
        int temp_wins;
        int temp_losses;
        int temp_rank;
        
        while(changeMade){
            changeMade = false;
            for(int i = 1; i < ranks.size(); i++){
                
                if(ranks.get(i) > ranks.get(i-1)){
                    
                    temp_rank = ranks.get(i);
                    temp_name = names.get(i);
                    temp_wins = wins.get(i);
                    temp_losses = losses.get(i);
                    
                    
                    ranks.set(i,ranks.get(i-1));
                    names.set(i,names.get(i-1));
                    wins.set(i, wins.get(i-1));
                    losses.set(i,losses.get(i-1));
                    
                    ranks.set(i-1,temp_rank);
                    names.set(i-1,temp_name);
                    wins.set(i-1,temp_wins);
                    losses.set(i-1,temp_losses);
                    
                    changeMade = true;
                }
                
            }
        }
    }
    
    private void saveRank(ArrayList<String> names,ArrayList<Integer> ranks,ArrayList<Integer> wins,ArrayList<Integer> losses){
        
        try{
        
            FileWriter f_writer = new FileWriter("rnks.con",false);
            
            for(int i = 0; i<names.size(); i++){
                f_writer.write(names.get(i)+"!"+ranks.get(i)+"@"+wins.get(i)+"#"+losses.get(i)+"\n");
            }
            f_writer.close();
            
            
        
        }catch(IOException e){
            
            System.out.println("Failed To Save Ranks");
        }
        
       
    }    
}
