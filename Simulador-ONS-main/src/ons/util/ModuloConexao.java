/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ons.util;

import java.sql.*;

/**
 *
 * @author leila
 */
public class ModuloConexao {
    
    public static Connection connector(){
        java.sql.Connection conexao = null;
        
        String driver = "com.mysql.cj.jdbc.Driver";
        String url = "jdbc:mysql://192.168.12.221:3306/eon";
        //String url = "jdbc:mysql://localhost:3306/eon";
        String user = "gustavo";
        //String user = "root";
        String password = "utfpr";
        
        while(true){
            try{
                Class.forName(driver);
                conexao = DriverManager.getConnection(url,user,password);
                return conexao;
            } catch(Exception e){
            }
        }
    }
    
}
