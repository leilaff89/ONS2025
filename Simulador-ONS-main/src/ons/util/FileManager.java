/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ons.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import ons.EONLightPath;
import ons.Flow;
import ons.LightPath;
import ons.Path;
import ons.TrafficGenerator;
import ons.ra.EON_QFDDM;
import java.util.Iterator;
import ons.EONLink;
import ons.EONPhysicalTopology;
import ons.Link;
import ons.ra.ControlPlaneForRA;
import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 *
 * @author gusta
 */
public class FileManager {
    
    public static void changeLoad(int load, String file) {
        try {
            // Carrega o arquivo XML
            File arquivoXML = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document documento = dBuilder.parse(arquivoXML);

            // Localiza o elemento que contém o atributo "load"
            Element elementoTraffic = (Element) documento.getElementsByTagName("traffic").item(0);
            elementoTraffic.setAttribute("load", load + "");

            // Salva as alterações no arquivo XML
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(documento);
            StreamResult result = new StreamResult(arquivoXML);
            transformer.transform(source, result);

            System.out.println("Alterações feitas com sucesso!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
   
    
    
    public static void writeCSV(ArrayList<Flow> flows) {
      
        int r;
        double cost = 0;
        try {
            
            //Abre os arquivos csv
            FileWriter csvWriter = new FileWriter("simulations/testez.csv");
            FileWriter csvWriter2 = new FileWriter("simulations/teste2.csv");
            FileWriter csvWriter3 = new FileWriter("simulations/teste3.csv");
            
            //Cabeçalho dos arquivos
            csvWriter.append("Slots Demandados, Tempo Transmitido, Tempo Faltante, Classe, Degradacao Tolerada(%), Degradaco Tolerada, Degradacao Realizada %, Degradacao Realizada, Atraso (%), Atraso, Custo, Carga na Rede, Status, Tamanho(Path 1), Tamanho(Path 2), Tamanho(Path 3), Path 1, Path 2, Path 3, NumWaveInUse, NumWave, Trafego caminho 1(%),Trafego caminho 1, Trafego caminho 2(%),Trafego caminho 2, Trafego caminho 3(%),Trafego caminho 3\n");
            csvWriter2.append("getSource,getDestination,getNumWavesInUse,getNumWave,State,%1,%2,%3\n");
            csvWriter3.append("Transmitted Bw, Bw Req, Arrival time, Departure Time, Hold, Dep - Ar\n");
            
            for (Flow f : flows) {
                
               csvWriter3.append(f.getTransmittedBw()+",");
               csvWriter3.append(f.getBwReq()+",");
               csvWriter3.append(f.getArrivalEvent().getTime()+",");
               csvWriter3.append(f.getDepartureEvent().getTime()+",");
               csvWriter3.append(f.getHoldTime()+",");
               csvWriter3.append(f.getDepartureEvent().getTime()-f.getArrivalEvent().getTime()+",");
               csvWriter3.append("\n");
                
                ArrayList<Integer>[] paths = f.getPaths();
                r = 0;
                csvWriter.append(f.getRequiredSlotsRestauration() + ","); //slots demandados
                csvWriter.append(f.getMissingTime()*f.getTransmittedBw()/(f.getBwReq()-f.getTransmittedBw())+","); //Tempo Transmitido
                csvWriter.append(f.getMissingTime()+",");//Tempo Faltante
                csvWriter.append(f.getCOS()+","); //Classe
                csvWriter.append(f.getServiceInfo().getDegradationTolerance()+",");//Degradação Tolerada%
                csvWriter.append(f.getBwPerWave()*f.getNumWave()*f.getServiceInfo().getDegradationTolerance() + ","); // Degradação Tolerada
                csvWriter.append(1-f.calcDegradation()+","); //Degradação Realizada %
                csvWriter.append(f.getBwPerWave()*(f.getNumWave()-f.getNumWavesInUse())+",");//Degradação Realizada
                csvWriter.append(f.getServiceInfo().getDelayTolerance()+",");//Atraso %
                csvWriter.append(f.getHoldTime()*f.getServiceInfo().getDelayTolerance()+",");//Tempo de atraso
                switch(f.getCOS()) {
                    case 0: 
                        cost = 0.00000375;
                        break;
                    case 1:
                        cost = 0.000003;
                        break;
                    case 2:
                        cost = 0.0000015;
                        break;
                    case 3:
                        cost = 0;
                        break;
                }
                csvWriter.append(cost + ","); // Custo
                csvWriter.append(TrafficGenerator.getLoad()+","); // Carga na rede
      
                ArrayList<EONLightPath> usedLps = f.getUsedLps();
                
                
                r = 0;
                if(f.isDropped()) r += 4;
                if(f.isIsDelayed()) r += 2;
                if(f.getServiceInfo().getDegradationTolerance() > 0) r += 1;
                //if(f.calcDegradation()!=1 && f.calcDegradation()!=0) r +=1;
                
                
                
                csvWriter.append(r+","); // Status
                
               for(ArrayList<Integer> path : paths){
                    csvWriter.append(path.size()+",");
                }
               
               for(ArrayList<Integer> path : paths){
                    csvWriter.append("[");
                    for(int i = 0;i<path.size()-1;i++){
                        csvWriter.append(path.get(i)+"-");
                    }
                    csvWriter.append(path.get(path.size()-1)+"],");
                    
                }
               
               csvWriter.append(f.getNumWavesInUse()+",");
               csvWriter.append(f.getNumWave()+",");
               
               if(f.getPercentage()!=null){
                    float totalValue = 0;
                    for(Map.Entry<int[],Integer>entry:f.getPercentage().entrySet()){
                        totalValue+= entry.getValue();
                    }

                    for(Map.Entry<int[],Integer>entry:f.getPercentage().entrySet()){

                        csvWriter.append(entry.getValue()/totalValue + ","); // Tráfego por caminho %
                        csvWriter.append(entry.getValue() + ","); // Tráfego por caminho
                    }
                }
               
                //CsvWritter 2
                   
                csvWriter2.append(f.getSource()+",") ;
                csvWriter2.append(f.getDestination()+",");
                csvWriter2.append(f.getNumWavesInUse() + ",");
                csvWriter2.append(f.getNumWave() + ",");   
                csvWriter2.append(r+",");
               // for(ArrayList<Integer> path : paths){
                 //   csvWriter2.append(path+",");
                    
                //}
                
             csvWriter2.append(usedLps+",");
                
                
                
                
                
               
                
                csvWriter.append("\n");
                csvWriter2.append("\n");
               
    
            }
            csvWriter.flush();
            csvWriter.close();
            csvWriter2.flush();
            csvWriter2.close();
            csvWriter3.flush();
            csvWriter3.close();
            
        } catch (IOException ex) {
            Logger.getLogger(EON_QFDDM.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void writeFlows(ArrayList<Flow> flows, String name,ControlPlaneForRA cp) {
        
        
        
        try{
        
        FileWriter csvWriter = new FileWriter("simulations2/"+name);
        
        csvWriter.append("ID,Carga na Rede, Nó Fonte, Nó Destino, Rate, Largura de banda total, Largura de banda para restauração,Banda total, Banda transmitida, Tempo total para transmissão, Tempo transmitido, Tempo restante para transmissão, Classe, Total de slots, Slots necessários para restauração, Caminhos, Degradação%, Delay%, Delay, Weight, Modulation \n");
        
        for (Flow flow : flows) {
            
            ArrayList<Integer>[] paths = YenKSP.kDisruptedShortestPaths(cp.getPT().getWeightedGraph(), flow.getSource(), flow.getDestination(), 10);
            
            csvWriter.append(flow.getID() + ","); 
            csvWriter.append(TrafficGenerator.getLoad()+",");
            csvWriter.append(flow.getSource() + ",");
            csvWriter.append(flow.getDestination() + ",");
            csvWriter.append(flow.getRate() + ",");
            csvWriter.append(flow.getBwReq() + ",");
            csvWriter.append(flow.getBwReqRestauration() + ",");
            csvWriter.append(flow.getTotalBand()+",");
            csvWriter.append(flow.getTransmittedBw2()+",");
            csvWriter.append(flow.getDuration2() + ",");
            csvWriter.append(flow.getTransmittedTime2() + ",");
            csvWriter.append(flow.getMissingTime2() + ",");
            csvWriter.append(flow.getCOS() + ",");
            csvWriter.append(flow.getRequiredSlots2()+",");
            csvWriter.append(flow.getRequiredSlotsRestauration2()+",");
           
            for(int j = 0; j < paths.length; j++){
                csvWriter.append(paths[j].toString().replace(", ", ";"));
                if(j<paths.length-1) csvWriter.append(";");
            }
            csvWriter.append(",");
            csvWriter.append(flow.getServiceInfo().getDegradationTolerance()+",");
            csvWriter.append(flow.getServiceInfo().getDelayTolerance()+",");
            csvWriter.append(flow.getServiceInfo().getDelayTolerance()*flow.getMissingTime2()+",");
            
            csvWriter.append(flow.getServiceInfo().getWeight()+",");
            csvWriter.append(flow.getModulation() + ",");
                     
            csvWriter.append("\n");
            
            
            
        }
        csvWriter.flush();
        csvWriter.close();
            
        } catch (IOException ex) {
            Logger.getLogger(EON_QFDDM.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void writeVT(ControlPlaneForRA cp, String nbr) {
        
        
        try{
            FileWriter csvWriter = new FileWriter("simulations2/VT"+nbr+".csv");
            
            TreeSet<LightPath>[][] adjMatrix = cp.getVT().getAdjMatrix();
            
            for(int i = 0; i < adjMatrix.length; i++){
                for(int j = 0; j<adjMatrix[i].length; j++){
                    if(adjMatrix[i][j] == null) continue;
                    
                    Iterator<LightPath> value = adjMatrix[i][j].iterator();
                    while (value.hasNext()) {
                       EONLightPath lp = (EONLightPath)value.next();
                       csvWriter.append(lp.getID() + ",");
                       csvWriter.append(lp.getSource() + ",");
                       csvWriter.append(lp.getDestination() + ",");
                       csvWriter.append(cp.getVT().getLightpathBWAvailable(lp.getID()) + ",");
                       int[] links = lp.getLinks();
                       for(int m = 0; m<links.length-1;m++){
                           csvWriter.append(links[m]+";");
                       }
                       csvWriter.append(links[links.length-1]+",");
                       csvWriter.append(lp.getFirstSlot()+",");
                       csvWriter.append(lp.getLastSlot()+",");
                       csvWriter.append(lp.getSlots()+",");
                       csvWriter.append(lp.getBwAvailable()+",");
                       csvWriter.append(lp.getBw()+",");
                       
                       
                       
                       
                       
                       
                       csvWriter.append("\n");
                    }
                    
                }
                
            }
            
        csvWriter.flush();
        csvWriter.close();
            
        }catch (IOException ex) {
            Logger.getLogger(EON_QFDDM.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try{
            FileWriter csvWriter = new FileWriter("simulations2/PT"+nbr+".csv");
            Link[][] adjMatrix = cp.getPT().getAdjMatrix();
            for(int i = 0; i < adjMatrix.length; i++){
                for(int j = 0; j<adjMatrix[i].length; j++){
                    if(adjMatrix[i][j] == null) continue;
                    EONLink link = (EONLink) adjMatrix[i][j];
                    
                    csvWriter.append(link.getSource()+",");
                    csvWriter.append(link.getDestination()+",");
                    
                    int[] slots = link.getSlotsAvailableToArray(1);
                    
                    csvWriter.append(slots.length+",");
                    
                    for(int k = 0; k<slots.length; k++){
                       csvWriter.append(slots[k] + ";"); 
                    }
                    
                    csvWriter.append("\n");
                    
                }
            }
            
            csvWriter.flush();
            csvWriter.close();
            
        }catch (IOException ex) {
            Logger.getLogger(EON_QFDDM.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
