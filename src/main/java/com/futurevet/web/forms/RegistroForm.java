package com.futurevet.web.forms;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistroForm {
    @NotBlank
    @Size(min = 2, max = 50)
    private String nome;

    public String getNome() {
        return nome;
    }

    public void setNome(String value) {
        this.nome = value;
    }

    @NotBlank
    @Email
    @Size(max = 50)
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String value) {
        this.email = value;
    }

    @NotBlank
    @Size(min = 8, max = 60)
    private String senha;

    public String getSenha() {
        return senha;
    }

    public void setSenha(String value) {
        this.senha = value;
    }

    @NotBlank
    private String confirmacao;

    public String getConfirmacao() {
        return confirmacao;
    }

    public void setConfirmacao(String value) {
        this.confirmacao = value;
    }

    @NotBlank
    @Pattern(regexp = "[0-9.\\-]{11,14}")
    private String cpf;

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String value) {
        this.cpf = value;
    }

    @Pattern(regexp = "[0-9() \\-]{0,20}")
    private String telefone;

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String value) {
        this.telefone = value;
    }
}
