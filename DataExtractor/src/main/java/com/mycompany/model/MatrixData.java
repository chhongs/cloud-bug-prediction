package com.mycompany.model;

public class MatrixData {
    private String className;
    private boolean isBug;
    private String loc;
    private String wmc;
    private String dit;
    private String cbo;
    private String rfc;
    private String lcom;
    private String name;
    private String namePr;
    private String version;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Boolean getBug() {
        return isBug;
    }

    public void setBug(Boolean bug) {
        isBug = bug;
    }

    public String getLoc() {
        return loc;
    }

    public void setLoc(String loc) {
        this.loc = loc;
    }

    public void setWmc(String wmc) {
        this.wmc = wmc;
    }

    public String getWmc() {
        return wmc;
    }

    public void setDit(String dit) {
        this.dit = dit;
    }

    public String getDit() {
        return dit;
    }

    public void setCbo(String cbo) {
        this.cbo = cbo;
    }

    public String getCbo() {
        return cbo;
    }

    public void setRfc(String rfc) {
        this.rfc = rfc;
    }

    public String getRfc() {
        return rfc;
    }

    public void setLcom(String lcom) { this.lcom = lcom; }

    public String getLcom() {
        return lcom;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamePr() {
        return namePr;
    }

    public void setNamePr(String namePr) {
        this.namePr = namePr;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
