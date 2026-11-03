package com.futurevet.web.forms;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class ReforcoForm {
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
    @Min(1)
    @Max(60)
    private Integer intervalo;

    public Integer getIntervalo() {
        return intervalo;
    }

    public void setIntervalo(Integer value) {
        this.intervalo = value;
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
}
