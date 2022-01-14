/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myudpserver;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
/**
 *
 * @author Wilson
 */
public class MyUDPServer{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws AWTException{
        //Database Connection testing
        //seed();
        // Actual Server
        new MyUDPServer();

    }

    private UDPServer svr;

    MyUDPServer() throws AWTException{

        svr = new UDPServer();
        loadBannedIPs();
        svr.run();

    }

    // private void resetRanks(int r){

    //     print("Resetting all ranks to: "+r);
    //     File userFolders = new File("Users/");
    //     File characterFolders;
    //     File[] userFiles = userFolders.listFiles();
    //     File[] characterFiles;
    //     File user_rank_file;

    //     for (File file : userFiles){

    //         if (file.isDirectory()){

    //             //System.out.println("Getting Rank For: "+file.getName());

    //                 characterFolders = new File("Users/"+file.getName()+"/Character");
    //                 characterFiles = characterFolders.listFiles();

    //                 for(File file2 : characterFiles){

    //                     if(file2.isDirectory()){
    //                         //System.out.println(">>"+file2.getName());

    //                         user_rank_file = new File("Users/"+file.getName()+"/Character/"+file2.getName()+"/rp.con");
    //                         svr.log("Resetting Rank of ["+file.getName()+":"+file2.getName()+"] to "+r);
    //                         try{
    //                              FileWriter f_write = new FileWriter(user_rank_file,false);

    //                              f_write.write(r+"\n");
    //                              f_write.write("0\n");
    //                              f_write.write("0\n");

    //                              f_write.close();

    //                         }catch(IOException d){
    //                             d.printStackTrace();
    //                         }
    //                     }
    //                 }
    //                 characterFiles = null;
    //         }

    //     }
    //     print("All RPs resetted to: "+r);
    //     svr.sendMessageFromServer("loadrp");

    // }

    private void loadBannedIPs(){

        BannedIP bad_ip;

        try{

            BufferedReader fileReader = new BufferedReader(new FileReader("bannedips.con"));
            String line = fileReader.readLine();

            while(line != null){
                bad_ip = new BannedIP(line);
                svr.banned_ips.add(bad_ip);

                print("IP: ["+line+"] has been banned from file.");

                line = fileReader.readLine();
            }

            fileReader.close();
        }catch (Exception e){

            System.out.println(e);
        }

    }

    public void print(String s){
        System.out.println(s+"\n");
    }

    public static void seed(){
        Statement statement = null;
        ResultSet result = null;
        Connection DB = new ConnectDB().getConnection();

        //String query = "create table players(id SERIAL primary key, username varchar(200), password varchar(20))";
        // String query = "insert into players(username,password) values ('ken','123')";
        String query = "select * from players where username='ken'";

        try {
            statement = DB.createStatement();
            // statement.executeUpdate(query);
            result = statement.executeQuery(query);

            while(result.next()){
                System.out.println("Ken's password is "+result.getString("password"));
            }


            System.out.println("Finished Updating Table in DB!");
        } catch (Exception e) {
            System.out.println(e);
        }

    }
    // public class textFieldHandler implements ActionListener {

    //     public void actionPerformed(ActionEvent e) {

    //         String msg = input_field.getText();

    //         if(msg.length() == 0){

    //             //do nothing
    //         }
    //         else if(msg.length() >= 9 && msg.substring(0,9).equals("/announce")){

    //             msg = msg.substring(9);
    //             svr.sendMessageFromServer("announce"+msg);

    //             svr.print("ANNOUNCEMENT: "+msg);
    //         }
    //         else if(msg.length() >= 6 && msg.substring(0,6).equals("/banip")){

    //             BannedIP bad_ip = new BannedIP(msg.substring(7));

    //             svr.banned_ips.add(bad_ip);

    //             print("IP: ["+msg.substring(7)+"] has been banned.");

    //             //save this to file for next time the server loads
    //             try{
    //                 FileWriter f_write = new FileWriter("bannedips.con",true);

    //                 f_write.write(msg.substring(7)+"\n");

    //                 f_write.close();

    //             }catch(IOException d){
    //                 d.printStackTrace();
    //             }

    //         }
    //         else if (msg.equals("/debug")){

    //             if(svr.debugLog == true){
    //                 svr.debugLog = false;
    //                 svr.print("Debug Log: OFF");
    //             }
    //             else
    //             {
    //                 svr.debugLog = true;
    //                 svr.print("Debug Log: ON");
    //             }
    //         }
    //         else if (msg.length() >= 10 && msg.substring(0,8).equals("/resetrp")){

    //             resetRanks(Integer.parseInt(msg.substring(9)));
    //         }
    //         else if (msg.equals("/kickall")){

    //             print("Kicking all Players...");
    //             svr.kickAllPlayers();
    //             print("All Players Have Been Kicked.");
    //         }
    //         else if (msg.length() >= 5 && msg.substring(0,5).equals("/kick")){

    //             String player_name = msg.substring(5).trim();

    //             svr.sendMessageFromServer("kick"+player_name);
    //             //player_list.removeElement(player_name);
    //             svr.removePlayer(player_name);
    //             svr.print(player_name+" Has been kicked!");
    //         }

    //         else{

    //         svr.sendMessageFromServer("msg[Server]: "+input_field.getText());
    //         //System.out.println("normal message");
    //         svr.print("Server: "+input_field.getText());
    //         }


    //         input_field.setText("");
    //       }

    // }

}


