/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ons.util;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author leila
 */
public class Model {
    private int id;
    private String src;
    private String dst;
    private int bwReqRestauration;
    private int tempoFaltante;
    private int reqSlotsRestauration;
    private String caminho;
    private String slotsSelec;
    private String links;
    private int modulation;

    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getDst() {
        return dst;
    }

    public void setDst(String dst) {
        this.dst = dst;
    }

    public int getBwReqRestauration() {
        return bwReqRestauration;
    }

    public void setBwReqRestauration(int bwReqRestauration) {
        this.bwReqRestauration = bwReqRestauration;
    }

    public int getTempoFaltante() {
        return tempoFaltante;
    }

    public void setTempoFaltante(int tempoFaltante) {
        this.tempoFaltante = tempoFaltante;
    }

    public int getReqSlotsRestauration() {
        return reqSlotsRestauration;
    }

    public void setReqSlotsRestauration(int reqSlotsRestauration) {
        this.reqSlotsRestauration = reqSlotsRestauration;
    }

    public int[] getCaminho() {
        List<Integer> caminhoList = new ArrayList<Integer>();
        String caminhos[] = caminho.split(",");
        for(String a: caminhos)
        {
            caminhoList.add(Integer.parseInt(a));
        }
        return caminhoList.stream().mapToInt(Integer::intValue).toArray();
    }
    
    public int[] getLinks() {
        List<Integer> linksList = new ArrayList<Integer>();
        String linksStrList[] = links.substring(1,links.length()-1).replaceAll("\\s", "").split(",");
        if(linksStrList[0] == "") return null;
        for(String a: linksStrList)
        {
            linksList.add(Integer.parseInt(a));
        }
        return linksList.stream().mapToInt(Integer::intValue).toArray();
    }
    
    public int getFirstSlot() {
        String slot[] = slotsSelec.split(",");
        System.out.println(slot[0]);
        if(slot[0].equals("\"[]\"")) return -1;
        return Integer.parseInt(slot[0]);
    }

    public void setCaminho(String caminho) {
        this.caminho = caminho;
    }
    
    public void setLinks(String links) {
        this.links = links;
    }

    public String getSlotsSelec() {
        return slotsSelec;
    }

    public void setSlotsSelec(String slotsSelec) {
        this.slotsSelec = slotsSelec;
    }
    // Construtor, getters e setters aqui...

    public int getModulation(){
        return this.modulation;
    }
    
    public void setModulation(int modulation)
    {
        this.modulation = modulation;
    }
    
    @Override
    public String toString() {
        // Implemente o método toString conforme necessário para representar os dados da classe
        return "Model{id=" + id + ", src='" + src + "', dst='" + dst + "', ...}";
    }
}
