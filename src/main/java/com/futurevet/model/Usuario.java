package com.futurevet.model;

public record Usuario(long id, String nome, String email, String senha, Long cpf, Long telefone,
        String role) {

    public boolean admin() {
        return Perfil.ADMIN.name().equals(role);
    }
}
