package com.futurevet.web.forms;

import com.futurevet.model.Animal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class AnimalForm {
    @NotBlank
    @Size(max = 30)
    private String nome;

    public String getNome() {
        return nome;
    }

    public void setNome(String value) {
        this.nome = value;
    }

    @NotBlank
    @Pattern(regexp = "CAO|GATO|COELHO|OUTRO")
    private String especie;

    public String getEspecie() {
        return especie;
    }

    public void setEspecie(String value) {
        this.especie = value;
    }

    @Size(max = 35)
    private String raca;

    public String getRaca() {
        return raca;
    }

    public void setRaca(String value) {
        this.raca = value;
    }

    @Size(max = 30)
    private String idade;

    public String getIdade() {
        return idade;
    }

    public void setIdade(String value) {
        this.idade = value;
    }

    @Pattern(regexp = "PEQUENO|MEDIO|GRANDE")
    private String porte;

    public String getPorte() {
        return porte;
    }

    public void setPorte(String value) {
        this.porte = value;
    }

    @DecimalMin("0.01")
    @DecimalMax("999.99")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal peso;

    public BigDecimal getPeso() {
        return peso;
    }

    public void setPeso(BigDecimal value) {
        this.peso = value;
    }

    public static AnimalForm de(Animal a) {
        AnimalForm f = new AnimalForm();
        f.setNome(a.nome());
        f.setEspecie(a.especie());
        f.setRaca(a.raca());
        f.setIdade(a.idade());
        f.setPorte(a.porte());
        f.setPeso(a.peso());
        return f;
    }
}
