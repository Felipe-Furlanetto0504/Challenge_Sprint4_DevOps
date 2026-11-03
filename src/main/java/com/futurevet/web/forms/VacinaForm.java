package com.futurevet.web.forms;

import com.futurevet.model.Vacina;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class VacinaForm {
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
    private String nome;

    public String getNome() {
        return nome;
    }

    public void setNome(String value) {
        this.nome = value;
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

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate proximaDose;

    public LocalDate getProximaDose() {
        return proximaDose;
    }

    public void setProximaDose(LocalDate value) {
        this.proximaDose = value;
    }

    @Size(max = 100)
    private String local;

    public String getLocal() {
        return local;
    }

    public void setLocal(String value) {
        this.local = value;
    }

    public static VacinaForm de(Vacina v) {
        VacinaForm f = new VacinaForm();
        f.setAnimalId(v.animalId());
        f.setNome(v.nome());
        f.setData(v.data());
        f.setProximaDose(v.proximaDose());
        f.setLocal(v.local());
        return f;
    }
}
