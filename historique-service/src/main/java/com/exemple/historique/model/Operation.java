package com.exemple.historique.model;

public class Operation {
    private int id;
    private String operateur;
    private int a;
    private int b;
    private double resultat;

    public Operation() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getOperateur() { return operateur; }
    public void setOperateur(String operateur) { this.operateur = operateur; }

    public int getA() { return a; }
    public void setA(int a) { this.a = a; }

    public int getB() { return b; }
    public void setB(int b) { this.b = b; }

    public double getResultat() { return resultat; }
    public void setResultat(double resultat) { this.resultat = resultat; }
}