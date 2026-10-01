/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.ventatours;

import java.io.Serializable;

/**
 *
 * @author PC
 */
public class Voucher implements Serializable {
    Boolean confirmado;
    String codigoCompra;
    String motivo;
    int montoUSD;

    public Voucher(Boolean confirmado, String codigoCompra, String motivo, int montoUSD) {
        this.confirmado = confirmado;
        this.codigoCompra = codigoCompra;
        this.motivo = motivo;
        this.montoUSD = montoUSD;
    }

    public Boolean getConfirmado() {
        return confirmado;
    }

    public void setConfirmado(Boolean confirmado) {
        this.confirmado = confirmado;
    }

    public String getCodigoCompra() {
        return codigoCompra;
    }

    public void setCodigoCompra(String codigoCompra) {
        this.codigoCompra = codigoCompra;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public int getMontoUSD() {
        return montoUSD;
    }

    public void setMontoUSD(int montoUSD) {
        this.montoUSD = montoUSD;
    }

}
