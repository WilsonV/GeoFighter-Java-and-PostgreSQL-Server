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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;



public class Challenge implements Runnable{
    File challenge_file;
    FileWriter f_writer;
    Random rand = new Random();

    private boolean running = true;
    private long last_ran_time = System.currentTimeMillis();
    private long current_time;
    private int minute_to_run = 5;


    public void run(){

        while(running){

           try{

           Thread.sleep(1000);
           current_time = System.currentTimeMillis();

            if(current_time - last_ran_time >= (minute_to_run)*(60)*(1000)){

                //System.out.println("Calculating Ranks...");
                ///////START///////
                generateChallenge();

                //System.out.println("New Challenge Generated!");
                last_ran_time = System.currentTimeMillis();
                ///////END/////////
            }
           }catch(InterruptedException e){

           }
        }
    }

    Challenge(){

    }

    private void generateChallenge(){

        //int shirtNumber = Integer.parseInt( getShirt());

        try{

            f_writer = new FileWriter("Challenge/1.txt",false);

            //stage
            f_writer.write(getStage()+"\n");
            f_writer.write("Player\n");
            f_writer.write("50\n10\n50\nChallenger\nC\n");
            f_writer.write(getSet()+"\n");
            f_writer.write(getSkinColor()+"\n");
            f_writer.write(getHair()+"\n");
            f_writer.write(getFace()+"\n");
            f_writer.write(getShirtAndPants()+"\n");
            f_writer.write(getSpecial()+"\n");
            f_writer.write(getSpecial()+"\n");
            f_writer.write(getSpecial()+"\n");
            f_writer.write("50\n");
            f_writer.write(getHP()+"\n");
            f_writer.write(getStrength()+"\n");
            f_writer.write(getDefense()+"\n");
            f_writer.write(getCritical()+"\n");
            f_writer.write("tiny boss\n");
            f_writer.write("full special\n");


            f_writer.close();
        }catch(IOException e){

        }
    }


    private String getStage(){
        int num = rand.nextInt(5);
        String[] stage = {"Dojo","Throne Room","Server Room","Freezer","Graveyard","The Pit"};


        return stage[num];
    }

    private String getSet(){
        int num = rand.nextInt(24);
        String[] set = {"angelic","chimera","dragon","dual wield","fist of fire","gladiator"
        ,"green bow","heavyweight","kung fu","lecyclone","lightning strike","soldier","soul"
        ,"tai chi","tundra","agent","swordsman","ninjutsu","elastic","fist of dragons","fist of doom","mage","earth bound","ninja"};


        return set[num];
    }

    private String getSkinColor(){
        int num = rand.nextInt(31)+1;


        return ""+num;
    }

    private String getHair(){
        int num = rand.nextInt(27)+1;


        return ""+num;
    }

    private String getFace(){
        int num = rand.nextInt(18)+1;


        return ""+num;
    }

    private String getShirtAndPants(){
        int num = rand.nextInt(42);

        int[] shirts = {2,3,5,6,8,9,10,11,12,13,14,23,24,27,28,29,30,31,33,34,35,37,41,42,43,44,45,46,48,50,51,52,53,54,55,56,57,58,59,60,61,62,63};
        int[] pants  = {1,2,4,6,5,7,8 ,9 ,11,12,13,17,9, 21,22,23,24,9 ,28,30,31,34,38,39,40,41,42,43,44,45,49,46,47,47,47,10,49,47,48,49,10,47,50};

        return ""+shirts[num]+"\n"+pants[num];
    }

    private String getSpecial(){
        int num = rand.nextInt(46);
        String[] special = {"ultimate fist","hosenka","fatal strike","arial hadoken","splitting beam","charged fist"
        ,"shinkuu hadoken","chidori","eliminate","storm barrier","lightning strike","thunder storm","vera beam"
        ,"sashuken","gut crusher","raging blast","dive claw","guarding graves","reversal","soul burner"
        ,"assasination","arrow rain","leaf bomb","gut slash","smoke bomb","ninja star","lightning sword"
        ,"fire ball","solar disk","solar disk assult","forged hammer","unstopable","hold it","seeker","first prayer"
        ,"pure rage","dragon lust","angelic touch","angelic smite","tornado kick","sleep dart","energy volley","cyclone"
        ,"flaming shinkuu hadoken","energy bomb","lightning blade","battle cry"};


        return special[num];
    }


    private String getHP(){
        int num = rand.nextInt(10001)+10000;


        return ""+num;
    }

    private String getStrength(){
        int num = rand.nextInt(101)+50;


        return ""+num;
    }


    private String getDefense(){
        int num = rand.nextInt(56)+45;


        return ""+num;
    }

    private String getCritical(){
        int num = rand.nextInt(91)+10;


        return ""+num;
    }

}


