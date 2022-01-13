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
public class Code {
    
    protected String code;
    protected String item;
    protected String itemName;
    protected int usageLeft;
    
    Code(String code, String item, String itemName, int usageLeft){
        
        this.code = code;
        this.item = item;
        this.itemName = itemName;
        this.usageLeft = usageLeft;
    }
    
}
