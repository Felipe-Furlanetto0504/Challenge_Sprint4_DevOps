package com.futurevet.web.forms;

import com.futurevet.ia.AssistenteService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PerguntaForm {
    @NotBlank
    @Size(max = AssistenteService.LIMITE_PERGUNTA)
    private String pergunta;

    public String getPergunta() {
        return pergunta;
    }

    public void setPergunta(String value) {
        this.pergunta = value;
    }
}
