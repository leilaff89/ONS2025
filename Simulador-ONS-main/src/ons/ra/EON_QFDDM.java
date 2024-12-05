/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ons.ra;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import ons.*;
import static ons.ra.EON_FDM.convertIntegers;
import ons.util.Dijkstra;
import ons.util.FileManager;
import ons.util.WeightedGraph;
import ons.util.YenKSP;
import ons.Main;
import ons.util.DBManager;
import ons.util.Model;

/**
 *
 * @author Gab
 */
public class EON_QFDDM implements RA{

    private ControlPlaneForRA cp;
    private WeightedGraph graph;    
    
    @Override
    public void simulationInterface(ControlPlaneForRA cp) {
        this.cp = cp;
        this.graph = cp.getPT().getWeightedGraph();           
    }
    
    private WeightedGraph getPostDisasterGraph(PhysicalTopology pt){
        
        int nodes = pt.getNumNodes();
        WeightedGraph g = new WeightedGraph(nodes);
        for (int i = 0; i < nodes; i++) {
            for (int j = 0; j < nodes; j++) {
                if (pt.hasLink(i, j)) {
                    if(!pt.getLink(i, j).isIsInterupted()){
                        g.addEdge(i, j, pt.getLink(i, j).getWeight());
                    }else{                        
                        g.addEdge(i, j, Integer.MAX_VALUE);
                    }                        
                }
            }
        }        
        return g;
    }

    @Override   
    public void flowArrival(Flow flow) {
        int[] nodes;
        int[] links;
        long id;
        LightPath[] lps = new LightPath[1];
       ArrayList<Integer>[] paths = YenKSP.kDisruptedShortestPaths(cp.getPT().getWeightedGraph(), flow.getSource(), flow.getDestination(), 10);
       flow.setPaths(paths);  
        
        OUTER:
        for (ArrayList<Integer> path : paths) {

            nodes = convertIntegers(path);
            // If no possible path found, block the call
            if (nodes.length == 0) {
                cp.blockFlow(flow.getID());
                return;
            }

            // Create the links vector
            links = new int[nodes.length - 1];
            for (int j = 0; j < nodes.length - 1; j++) {
                links[j] = cp.getPT().getLink(nodes[j], nodes[j + 1]).getID();
            }

            // Get the size of the route in km
            double sizeRoute = 0;
            for (int i = 0; i < links.length; i++) {
                sizeRoute += ((EONLink) cp.getPT().getLink(links[i])).getWeight();
            }
            // Adaptative modulation:
            int modulation = Modulation.getBestModulation(sizeRoute);
            //System.out.println(modulation);
            flow.setModulation(modulation);
            // Calculates the required slots
            int requiredSlots = Modulation.convertRateToSlot(flow.getBwReq(), EONPhysicalTopology.getSlotSize(), modulation);
            
            if(requiredSlots>=100000)
                    continue OUTER;
            // Evaluate if each link have space to the required slots
            for (int i = 0; i < links.length; i++) {
                if (!((EONLink) cp.getPT().getLink(links[i])).hasSlotsAvaiable(requiredSlots)) {
                    cp.blockFlow(flow.getID());
                    return;
                }
            }

            // First-Fit spectrum assignment in some modulation 
            int[] firstSlot;
            for (int i = 0; i < links.length; i++) {
                // Try the slots available in each link
                firstSlot = ((EONLink) cp.getPT().getLink(links[i])).getSlotsAvailableToArray(requiredSlots);
                for (int j = 0; j < firstSlot.length; j++) {
                    // Now you create the lightpath to use the createLightpath VT
                    //Relative index modulation: BPSK = 0; QPSK = 1; 8QAM = 2; 16QAM = 3;
                    EONLightPath lp = cp.createCandidateEONLightPath(flow.getSource(), flow.getDestination(), links,
                            firstSlot[j], (firstSlot[j] + requiredSlots - 1), modulation);
                    
                    
                    // Now you try to establish the new lightpath, accept the call
                    if ((id = cp.getVT().createLightpath(lp)) >= 0) {
                        // Single-hop routing (end-to-end lightpath)
                        lps[0] = cp.getVT().getLightpath(id);
                        if (cp.acceptFlow(flow.getID(), lps)) {
                          //  System.out.println(path);
                          //FileManager.writeFlow(flow, /*path*/ requiredSlots, links, sizeRoute);
                            return;
                        } else {
                            // Something wrong
                            // Dealocates the lightpath in VT and try again                        
                            cp.getVT().deallocatedLightpath(id);
                        }
                    }
                }
            }          
        }
       
        // Block the call
        cp.blockFlow(flow.getID());        
       
    }
   
    public boolean addLightPath(Flow flow){
       
        ArrayList<Integer> nodes = new ArrayList<Integer>();
        int[] links;
        long id;
        LightPath[] lps = new LightPath[1];    
       
        if (flow.getPaths() == null) {

            ArrayList<Integer>[] paths = YenKSP.kDisruptedShortestPaths(getPostDisasterGraph(cp.getPT()), flow.getSource(), flow.getDestination(), 3);
            flow.setPaths(paths);

        }
       
        OUTER:
        for (ArrayList<Integer> path : flow.getPaths()) {
            nodes = path;
            if (nodes.size() == 0) {
                continue;
            }
            // Create the links vector
            links = new int[nodes.size() - 1];
            for (int j = 0; j < nodes.size() - 1; j++) {
                links[j] = cp.getPT().getLink(nodes.get(j), nodes.get(j + 1)).getID();
            }
            // Get the size of the route in km
            double sizeRoute = 0;
            for (int i = 0; i < links.length; i++) {              
                sizeRoute += ((EONLink) cp.getPT().getLink(links[i])).getWeight();
            }
           
            // Adaptative modulation:
            int modulation = Modulation.getBestModulation(sizeRoute);

            // Calculates the required slots
            int requiredSlots = Modulation.convertRateToSlot((int) flow.getMaxRate(), EONPhysicalTopology.getSlotSize(), modulation);
            if(requiredSlots>=100000)
                    continue OUTER;
            // Evaluate if each link have space to the required slots
            for (int i = 0; i < links.length; i++) {
                if (((EONLink) cp.getPT().getLink(links[i])).isIsInterupted()||!((EONLink) cp.getPT().getLink(links[i])).hasSlotsAvaiable(requiredSlots)) {
                    continue;
                }
            }
            // First-Fit spectrum assignment in some modulation
            int[] firstSlot;
            for (int i = 0; i < links.length; i++) {
                // Try the slots available in each link
                firstSlot = ((EONLink) cp.getPT().getLink(links[i])).getSlotsAvailableToArray(requiredSlots);
                for (int j = 0; j < firstSlot.length; j++) {
                    // Now you create the lightpath to use the createLightpath VT
                    //Relative index modulation: BPSK = 0; QPSK = 1; 8QAM = 2; 16QAM = 3;
                    EONLightPath lp = cp.createCandidateEONLightPath(flow.getSource(), flow.getDestination(), links,
                            firstSlot[j], (firstSlot[j] + requiredSlots - 1), modulation);
                    // Now you try to establish the new lightpath, accept the call
                    if ((id = cp.getVT().createLightpath(lp)) >= 0) {
                        // Single-hop routing (end-to-end lightpath)
                        lps[0] = cp.getVT().getLightpath(id);
                        if (cp.upgradeFlow(flow, lps)) {
                            return true;
                        } else {
                            // Something wrong
                            // Dealocates the lightpath in VT and try again
                            cp.getVT().deallocatedLightpath(id);
                        }
                    }
                }
            }
        }

        return false;
       
    }    
   
    public boolean addLightPath2(Flow flow,int[] links,int firstSlot,int requiredSlots, int modulation){
        ArrayList<Integer> nodes = new ArrayList<Integer>();
        long id;
        LightPath[] lps = new LightPath[1];    

        for (int i = 0; i < links.length; i++) {

                EONLightPath lp = cp.createCandidateEONLightPath(flow.getSource(), flow.getDestination(), links,
                        firstSlot, (firstSlot + requiredSlots - 1), modulation);
                // Now you try to establish the new lightpath, accept the call
                if ((id = cp.getVT().createLightpath(lp)) >= 0) {
                    // Single-hop routing (end-to-end lightpath)
                    lps[0] = cp.getVT().getLightpath(id);
                    if (cp.upgradeFlow(flow, lps)) {
                        return true;
                    } else {
                        // Something wrong
                        // Dealocates the lightpath in VT and try again
                        cp.getVT().deallocatedLightpath(id);
                    }
                }
        }

        return false;
       
    }  
   
   
    @Override
    public void flowDeparture(long id) {
       
    }

    @Override
    public void disasterArrival(DisasterArea area) {
        DBManager.truncate();
       
        TrafficGenerator.eventNum = (TrafficGenerator.eventNum + 1)%4;
       
        ArrayList<Flow> survivedFlows = cp.getMappedFlowsAsList();
       
        for (Flow f : survivedFlows) {
            if (f.isDegradeTolerant()) {
                cp.degradeFlow(f,f.getMaxDegradationNumberEon());
            }
        }

        ArrayList<Flow> interuptedFlows = new ArrayList<Flow>(cp.getInteruptedFlows());
       
        for(Flow f : interuptedFlows){
            f.updateMissingTime();
            ArrayList<Integer>[] paths = YenKSP.kDisruptedShortestPaths(getPostDisasterGraph(cp.getPT()), f.getSource(), f.getDestination(), 3);
            f.setPaths(paths);
        }
       
        ArrayList<Flow> allFlows = new ArrayList<Flow>();
        allFlows.addAll(interuptedFlows);
        allFlows.addAll(survivedFlows);
       
        //FileManager.writeFlows(survivedFlows,"flowsSobreviventes"+Main.numSim+".csv",cp);
        //FileManager.writeFlows(interuptedFlows,"flowsInterrompidos"+Main.numSim+".csv",cp);
        //FileManager.writeVT(cp,Main.numSim+"");
            DBManager.writeFlows(survivedFlows,true,cp);
            DBManager.writeFlows(interuptedFlows,false,cp);
            //DBManager.writeVT(cp);
            DBManager.writePT(cp);
       
        DBManager.activate();
       
        while(DBManager.waitSim() == 0){}
       
        List<Model> model = DBManager.getModel();

        ArrayList<Flow> flows = new ArrayList<Flow>();
        flows.addAll(interuptedFlows);
       
        while (model.size() > 0) {

            Model teste = model.get(0);
           
            for(Flow f: flows){
                if(teste.getId() == f.getID()){
                   
                    /*if(f.calcDegradation() >= 1-f.getServiceInfo().getDegradationTolerance()){
                        f.updateTransmittedBw();
                        cp.restoreFlow(f);
                        DBManager.writeResult(f, 5);
                        flows.remove(f);
                        model.remove(teste);
                        break;*/
                       
          /*}else*/ if(teste.getLinks() == null){

                        if(f.isDelayTolerant()){
                            cp.delayFlow(f);
                        }else{
                            DBManager.writeResult(f, 4);
                            cp.dropFlow(f);
                        }
                        flows.remove(f);
                        model.remove(teste);
                        break;
                   
                    }else if (addLightPath2(f,teste.getLinks(),teste.getFirstSlot(),teste.getReqSlotsRestauration(),teste.getModulation())){
                        cp.restoreFlow(f);
                        DBManager.writeResult(f, 1);
                        flows.remove(f);
                        model.remove(teste);
                        break;
                           
                    }else{
                       
                        if(f.isDelayTolerant()){
                            cp.delayFlow(f);
                        }else{
                            DBManager.writeResult(f, 0);
                            cp.dropFlow(f);
                        }
                        flows.remove(f);
                        model.remove(teste);
                    }
                       
                    f.updateTransmittedBw();
                    break;
                }
            }
        }  
    }
   

    @Override
    public void disasterDeparture() {

    }

    @Override
    public void delayedFlowDeparture(Flow f) {
        
    }

    public void delayedFlowArrival(Flow f) {
       
        ArrayList<Integer>[] paths = YenKSP.kShortestPaths(this.getPostDisasterGraph(cp.getPT()), f.getSource(), f.getDestination(), 3);
       
        f.setPaths(paths);
               
        while (f.calcDegradation() < 1 - f.getServiceInfo().getDegradationTolerance()) {

            if (!cp.upgradeFlow(f, null)) {

                if (!addLightPath(f)) {

                    break;

                }

            }

        }
       
        if (f.calcDegradation() < 1 - f.getServiceInfo().getDegradationTolerance()) {
            cp.dropFlow(f);
            DBManager.writeResult(f, 2);
        }else{
            cp.restoreFlow(f);  
            DBManager.writeResult(f, 3);
           
        }    
       
    }
   
}
