/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myudpserver;

import java.net.InetAddress;

/**
 *
 * @author Gamer
 */
public class BannedIP {
    
    protected InetAddress ip;
    
    BannedIP(InetAddress i){
        
        ip = i;
       
    }
    
    BannedIP(String s){
        
        try{
            ip = InetAddress.getByName(s);
            //System.out.println("Banned: ["+ip+"]");
            
        }catch (Exception e){
            
            System.out.println(e);
        }
    }
}
