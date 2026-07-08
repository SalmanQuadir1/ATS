package com.stie.model;

import javax.persistence.Embeddable;

@Embeddable
public class CandidateEducation {
    private String institution;
    private String degree;
    private String startYear;
    private String endYear;
    private Double cgpa;

    public CandidateEducation() {
    }

    public CandidateEducation(String institution, String degree, String startYear, String endYear, Double cgpa) {
        this.institution = institution;
        this.degree = degree;
        this.startYear = startYear;
        this.endYear = endYear;
        this.cgpa = cgpa;
    }

    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public String getStartYear() { return startYear; }
    public void setStartYear(String startYear) { this.startYear = startYear; }

    public String getEndYear() { return endYear; }
    public void setEndYear(String endYear) { this.endYear = endYear; }

    public Double getCgpa() { return cgpa; }
    public void setCgpa(Double cgpa) { this.cgpa = cgpa; }
}
