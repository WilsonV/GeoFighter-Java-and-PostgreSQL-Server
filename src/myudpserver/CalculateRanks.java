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

    public class Rank{
        String characterName;
        Integer points;
        Integer wins;
        Integer loses;

        Rank(String name,String point,String win,String lost){
            characterName = name;
            points = Integer.parseInt(point);
            wins = Integer.parseInt(win);
            loses = Integer.parseInt(lost);
        }
    }

    private boolean running = true;
    private long rank_last_ran_time = System.currentTimeMillis();
    private long rank_current_time = System.currentTimeMillis();;
    private int rank_minute_to_run = 5;

    //Stuff for rank files
    BufferedReader br;

    ArrayList<Rank> ranksList = new ArrayList<Rank>();
    // ArrayList<String> rank_names = new ArrayList<String>();
    // ArrayList<Integer> rank_values = new ArrayList<Integer>();
    // ArrayList<Integer> rank_wins = new ArrayList<Integer>();
    // ArrayList<Integer> rank_losses = new ArrayList<Integer>();

    File userFolders = new File("Users/");
    //File characterFolders;

    //File[] userFiles;
    //File[] characterFiles;
    File user_rank_file;

    public void run(){

        while(running){

            try{

            rank_current_time = System.currentTimeMillis();

            if(rank_current_time - rank_last_ran_time >= (rank_minute_to_run)*(60)*(1000)){

                //System.out.println("Calculating Ranks...");
                ///////START///////
                getRanks();
                sortRank();
                saveRank();

                //clear ranks
                // rank_name.clear();
                // rank_value.clear();
                // rank_wins.clear();
                // rank_losses.clear();

                //System.out.println("Ranks Calculated!");
                rank_last_ran_time = System.currentTimeMillis();
                ///////END/////////
            }
            Thread.sleep(1000);
            }catch(InterruptedException e){

            }
        }
    }

    private void getRanks(){
        //System.out.println("Getting ranks to save....");
        String tempRank = "";
        String tempWins = "";
        String tempLosses = "";

        File[] userFiles = userFolders.listFiles();

        for (File file : userFiles){

            if (file.isDirectory()){

                //System.out.println("Getting Rank For: "+file.getName());

                    File characterFolders = new File("Users/"+file.getName()+"/Character");
                    File[] characterFilesList = characterFolders.listFiles();

                    for(File characterFolder : characterFilesList){

                        if(characterFolder.isDirectory()){
                            //System.out.println(">>"+file2.getName());


                                user_rank_file = new File("Users/"+file.getName()+"/Character/"+characterFolder.getName()+"/rp.con");

                            try{
                                br = new BufferedReader(new FileReader(user_rank_file));

                                //add the name
                                //rank_name.add(characterFolder.getName());

                                // add the rank
                                tempRank = br.readLine();
                                if(tempRank == null || tempRank == "")
                                    tempRank = "0";

                               // rank_value.add(Integer.parseInt(tempRank));

                                 // add the wins
                                tempWins = br.readLine();
                                if(tempWins == null || tempWins == "")
                                    tempWins = "0";

                                //rank_wins.add(Integer.parseInt(tempWins));

                                 // add the losses
                                tempLosses = br.readLine();
                                if(tempLosses == null || tempLosses == "")
                                    tempLosses = "0";

                                //rank_losses.add(Integer.parseInt(tempLosses));

                                //Add Rank for character
                                ranksList.add(new Rank(characterFolder.getName(), tempRank, tempWins, tempLosses));
                                //System.out.println("New Rank Object"+(ranksList.get(ranksList.size()-1)) );

                                br.close();
                                }catch(IOException e){

                                System.out.println(characterFolder.getName()+"'s RP file not found");
                            }
                        }
                    }
            }

        }
    }

    private void  sortRank(){
        //System.out.println("Sorting ranks....");
        boolean changeMade = true;
        Rank temp_rank;

        while(changeMade){
            changeMade = false;
            for(int i = 1; i < ranksList.size(); i++){

                if(ranksList.get(i).points > ranksList.get(i-1).points){

                    temp_rank = ranksList.get(i);


                    ranksList.set(i,ranksList.get(i-1));

                    ranksList.set(i-1,temp_rank);

                    changeMade = true;
                }

            }
        }
    }

    private void saveRank(){
        //System.out.println("Saving ranks to file....");
        try{

            FileWriter f_writer = new FileWriter("rnks.con",false);

            for(int i = 0; i<ranksList.size(); i++){
                f_writer.write(
                    ranksList.get(i).characterName+"!"+
                    ranksList.get(i).points+"@"+
                    ranksList.get(i).wins+"#"+
                    ranksList.get(i).loses+"\n");
            }
            f_writer.close();



        }catch(IOException e){

            System.out.println("Failed To Save Ranks");
        }


    }
}
