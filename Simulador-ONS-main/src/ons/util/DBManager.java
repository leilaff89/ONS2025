/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ons.util;
import ons.util.ModuloConexao;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;
import ons.EONLightPath;
import ons.EONLink;
import ons.Flow;
import ons.LightPath;
import ons.Link;
import ons.SimulationRunner;
import ons.TrafficGenerator;
import ons.ra.ControlPlaneForRA;
import ons.util.Model;
import java.util.List;

/**
 *
 * @author leila
 */
public class DBManager {
    
    public static void writeResult(Flow flow, int status){
        Connection conexao = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        conexao = ModuloConexao.connector();
        System.out.println(conexao);
        
        try{
            pst = conexao.prepareStatement("INSERT INTO resultado (id,restauracao, networkLoad,classe,event) VALUES (?,?,?,?,?)");
            pst.setLong(1, flow.getID());
            pst.setInt(2, status);
            pst.setDouble(3,TrafficGenerator.getLoad());
            pst.setInt(4, flow.getCOS());
            pst.setInt(5,TrafficGenerator.eventNum);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void writeFlows(ArrayList<Flow> flows, boolean interrompido, ControlPlaneForRA cp){
        Connection conexao = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        conexao = ModuloConexao.connector();
        System.out.println(conexao);
        
        for (Flow flow : flows) {
            try{
                WeightedGraph g = cp.getPT().getWeightedGraph();
                Link[][] adjMatrix = cp.getPT().getAdjMatrix();
                for(int i = 0; i < adjMatrix.length; i++){
                    for(int j = 0; j<adjMatrix[i].length; j++){
                        if(adjMatrix[i][j] == null){
                            g.setWeight(i, j, 0);
                            continue;
                        };
                        if(adjMatrix[i][j].isIsInterupted()){
                            g.setWeight(i, j, 0);
                        }
                        
                    }
                }
                
                ArrayList<Integer>[] paths = YenKSP.kDisruptedShortestPaths(g, flow.getSource(), flow.getDestination(), 10);
                pst = conexao.prepareStatement("INSERT INTO flow (id,networkLoad,src,dst,rate,bwReq,bwReqRestauration,totalBand,transmittedBand,duration,transmittedTime,tempoFaltante,classe,reqSlots,reqSlotsRestauration,caminhos,degradationTolerance,delayTolerance,delayToleranceTotal,weight,modulation,time, event, interrompido) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            
                pst.setLong(1, flow.getID());
                pst.setDouble(2, TrafficGenerator.getLoad());
                pst.setInt(3, flow.getSource());
                pst.setInt(4, flow.getDestination());
                pst.setInt(5,flow.getRate());
                pst.setInt(6, flow.getBwReq());
                pst.setDouble(7, flow.getBwReqRestauration());
                pst.setDouble(8, flow.getTotalBand());
                pst.setDouble(9, flow.getTransmittedBw2());
                pst.setDouble(10, flow.getDuration2());
                pst.setDouble(11,flow.getTransmittedTime2());
                pst.setDouble(12,flow.getMissingTime2());
                pst.setInt(13, flow.getCOS());
                pst.setInt(14, flow.getRequiredSlots2());
                pst.setInt(15, flow.getRequiredSlotsRestauration2());
                
                
                
                String path = "";
            
                if(paths.length > 0){
                    for(int j = 0; j < paths.length; j++){
                        path = path + paths[j].toString().replace(", ", ",");
                    }

                    path = path.replace("]", "],");
                    path = path.substring(0, path.length()-1);
                }
                pst.setString(16, path);
                pst.setFloat(17, flow.getServiceInfo().getDegradationTolerance());
                pst.setFloat(18, flow.getServiceInfo().getDelayTolerance());
                pst.setDouble(19, flow.getServiceInfo().getDelayTolerance()*flow.getMissingTime2());
                pst.setInt(20, flow.getServiceInfo().getWeight());
                pst.setInt(21,flow.getModulation());
                pst.setFloat(22, SimulationRunner.timer);
                pst.setInt(23, TrafficGenerator.eventNum);
                pst.setInt(24,interrompido ? 1 : 0);
            
                             
           
            
            pst.executeUpdate();
            
            } catch (SQLException e) {
                System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
    
    public static void truncate()
    {
        Connection conexao = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        conexao = ModuloConexao.connector();
        System.out.println(conexao);
        
        try {
            pst = conexao.prepareStatement("TRUNCATE TABLE vt;");
            pst.executeUpdate();
            pst = conexao.prepareStatement("TRUNCATE TABLE pt;");
            pst.executeUpdate();
         
            pst = conexao.prepareStatement("TRUNCATE TABLE model;");
            pst.executeUpdate();
            pst = conexao.prepareStatement("TRUNCATE TABLE flow;");
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void writeVT(ControlPlaneForRA cp){
        Connection conexao = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        conexao = ModuloConexao.connector();
        System.out.println(conexao);
        
        try{
            TreeSet<LightPath>[][] adjMatrix = cp.getVT().getAdjMatrix();
            
            for(int i = 0; i < adjMatrix.length; i++){
                for(int j = 0; j<adjMatrix[i].length; j++){
                    if(adjMatrix[i][j] == null) continue;
                    
                    Iterator<LightPath> value = adjMatrix[i][j].iterator();
                    while (value.hasNext()) {
                        pst = conexao.prepareStatement("INSERT INTO vt (id,srs,dst,bwAvailable,links,qtdSlots,time,event) VALUES (?,?,?,?,?,?,?,?)");
                        EONLightPath lp = (EONLightPath)value.next();
                        pst.setLong(1,lp.getID());
                        pst.setInt(2,lp.getSource());
                        pst.setInt(3,lp.getDestination());
                        pst.setInt(4,cp.getVT().getLightpathBWAvailable(lp.getID()));
                        int[] links = lp.getLinks();
                        String aux = "";
                       for(int m = 0; m<links.length-1;m++){
                           aux.concat(links[m]+";");
                       }
                        aux.concat(links[links.length-1] + "");
                        pst.setString(5, aux);
                        pst.setInt(6,lp.getSlots());
                        pst.setFloat(7, SimulationRunner.timer);
                        pst.setInt(8, TrafficGenerator.eventNum);
                        
                        
                        pst.executeUpdate();
                      

                    }
                }
            }
            
        } catch (SQLException e) {
                System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
    }
    
    public static void activate(){
        Connection conexao = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        conexao = ModuloConexao.connector();
        System.out.println(conexao);
        try{
            pst = conexao.prepareStatement("UPDATE controle_simulacao SET simulacao_ativa = 0");
            pst.executeUpdate();
        }catch (SQLException e) {
            System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        }
        
    }
    
    public static int waitSim(){
        Connection conexao = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        
        try{
            conexao = ModuloConexao.connector();
            System.out.println(conexao);
            
            String consultaSQL = "SELECT simulacao_ativa FROM controle_simulacao";
            pst = conexao.prepareStatement(consultaSQL);

            // Executar a consulta
            rs = pst.executeQuery();
            
            rs.next();
            return rs.getInt("simulacao_ativa");
        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
            return 1;
        } finally {
            // Fechar recursos (ResultSet, PreparedStatement, Connection)
            try {
                if (rs != null) rs.close();
                if (pst != null) pst.close();
                if (conexao != null) conexao.close();
            } catch (SQLException ex) {
                System.err.format("Erro ao fechar recursos: %s", ex.getMessage());
            }
        }
    }
    
    public static List<Model> getModel() {
        Connection conexao = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        List<Model> listaModel = new ArrayList<>();

        try {
            // Estabelecer a conexão com o banco de dados
            conexao = ModuloConexao.connector();
            System.out.println(conexao);

            // Consulta SQL SELECT
            String consultaSQL = "SELECT id, links, src, dst, bwReqRestauration, tempoFaltante, reqSlotsRestauration, caminho, slotsSelec, modulation FROM model";
            pst = conexao.prepareStatement(consultaSQL);

            // Executar a consulta
            rs = pst.executeQuery();

            // Processar os resultados
            while (rs.next()) {
                // Criar um objeto Model para cada linha da tabela
                Model model = new Model();
                model.setId(rs.getInt("id"));
                model.setLinks(rs.getString("links"));
                model.setSrc(rs.getString("src"));
                model.setDst(rs.getString("dst"));
                model.setBwReqRestauration(rs.getInt("bwReqRestauration"));
                model.setTempoFaltante(rs.getInt("tempoFaltante"));
                model.setReqSlotsRestauration(rs.getInt("reqSlotsRestauration"));
                model.setCaminho(rs.getString("caminho"));
                model.setSlotsSelec(rs.getString("slotsSelec"));
                model.setModulation(rs.getInt("modulation"));

                // Adicionar o objeto Model à lista
                listaModel.add(model);
            }

        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        } finally {
            // Fechar recursos (ResultSet, PreparedStatement, Connection)
            try {
                if (rs != null) rs.close();
                if (pst != null) pst.close();
                if (conexao != null) conexao.close();
            } catch (SQLException ex) {
                System.err.format("Erro ao fechar recursos: %s", ex.getMessage());
            }
        }

        // Retornar a lista de objetos Model
        return listaModel;
    }
    
    public static void writePT(ControlPlaneForRA cp){
        Connection conexao = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        conexao = ModuloConexao.connector();
        System.out.println(conexao);
        
        try{
            WeightedGraph g = cp.getPT().getWeightedGraph();
            Link[][] adjMatrix = cp.getPT().getAdjMatrix();
            for(int i = 0; i < adjMatrix.length; i++){
                for(int j = 0; j<adjMatrix[i].length; j++){
                    if(adjMatrix[i][j] == null) continue;
                        pst = conexao.prepareStatement("INSERT INTO pt (id,src,dst,slotsLivres,slots,time,event,interrupted, weight) VALUES (?,?,?,?,?,?,?,?,?)");
                        
                        EONLink link = (EONLink) adjMatrix[i][j];
                        pst.setInt(1,link.getID());
                        pst.setInt(2,link.getSource());
                        pst.setInt(3,link.getDestination());
                        int[] slots = link.getSlotsAvailableToArray(1);
                        pst.setInt(4,slots.length);
                        String aux = "[";
                        for(int k = 0; k<slots.length; k++){
                           aux = aux.concat(slots[k] + ","); 
                        }
                        aux = aux.concat("]");
                        aux = aux.replace(",]", "]");
                        pst.setString(5,aux);
                        pst.setFloat(6, SimulationRunner.timer);
                        pst.setInt(7, TrafficGenerator.eventNum);
                        pst.setInt(8, link.isIsInterupted()?0:1);
                        pst.setDouble(9, g.getWeight(i, j));
                        
                        
                        pst.executeUpdate();
                      

                    }
                }
            
            
        } catch (SQLException e) {
                System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
    }
}
