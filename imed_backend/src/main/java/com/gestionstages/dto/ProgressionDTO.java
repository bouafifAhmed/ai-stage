package com.gestionstages.dto;

public class ProgressionDTO {
    private long nombreJoursTotal;
    private long nombreJoursCouverts;
    private double pourcentage;

    public ProgressionDTO(long nombreJoursTotal, long nombreJoursCouverts, double pourcentage) {
        this.nombreJoursTotal = nombreJoursTotal;
        this.nombreJoursCouverts = nombreJoursCouverts;
        this.pourcentage = pourcentage;
    }

    public ProgressionDTO() {
    }

    public long getNombreJoursTotal() {
        return nombreJoursTotal;
    }

    public void setNombreJoursTotal(long nombreJoursTotal) {
        this.nombreJoursTotal = nombreJoursTotal;
    }

    public long getNombreJoursCouverts() {
        return nombreJoursCouverts;
    }

    public void setNombreJoursCouverts(long nombreJoursCouverts) {
        this.nombreJoursCouverts = nombreJoursCouverts;
    }

    public double getPourcentage() {
        return pourcentage;
    }

    public void setPourcentage(double pourcentage) {
        this.pourcentage = pourcentage;
    }
}
