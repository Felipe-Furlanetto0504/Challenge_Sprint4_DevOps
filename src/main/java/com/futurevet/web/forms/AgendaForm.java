package com.futurevet.web.forms;

import com.futurevet.model.Consulta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

public class AgendaForm {
    @NotNull
    private Long animalId;

    public Long getAnimalId() {
        return animalId;
    }

    public void setAnimalId(Long value) {
        this.animalId = value;
    }

    @NotBlank
    @Size(max = 30)
    private String tipo;

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String value) {
        this.tipo = value;
    }

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate data;

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate value) {
        this.data = value;
    }

    @NotNull
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime hora;

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime value) {
        this.hora = value;
    }

    @NotBlank
    @Size(max = 100)
    private String local;

    public String getLocal() {
        return local;
    }

    public void setLocal(String value) {
        this.local = value;
    }

    @Size(max = 255)
    private String observacao;

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String value) {
        this.observacao = value;
    }

    public static AgendaForm de(Consulta c) {
        AgendaForm f = new AgendaForm();
        f.setAnimalId(c.animalId());
        f.setTipo(c.tipo());
        f.setData(c.data());
        f.setHora(c.hora() == null ? null : LocalTime.parse(c.hora()));
        f.setLocal(c.local());
        f.setObservacao(c.observacao());
        return f;
    }
}
