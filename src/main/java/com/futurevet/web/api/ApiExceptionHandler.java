package com.futurevet.web.api;

import com.futurevet.ia.AssistenteIndisponivelException;
import com.futurevet.internacionalizacao.Mensagens;
import com.futurevet.service.RegraException;
import com.futurevet.web.api.dto.ApiError;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.futurevet.web.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {
    private final Mensagens mensagens;

    public ApiExceptionHandler(Mensagens mensagens) {
        this.mensagens = mensagens;
    }

    @ExceptionHandler(AssistenteIndisponivelException.class)
    public ResponseEntity<ApiError> assistenteIndisponivel(AssistenteIndisponivelException e) {
        return erro(HttpStatus.SERVICE_UNAVAILABLE, mensagens.texto(e));
    }

    @ExceptionHandler(RegraException.class)
    public ResponseEntity<ApiError> regra(RegraException e) {
        return erro(HttpStatus.BAD_REQUEST, mensagens.texto(e));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> credenciais() {
        return erro(HttpStatus.UNAUTHORIZED, mensagens.texto("erro.login.invalido"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> negado() {
        return erro(HttpStatus.FORBIDDEN, mensagens.texto("erro.acesso.registro"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> conflito() {
        return erro(HttpStatus.CONFLICT, mensagens.texto("erro.integridade"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacao(MethodArgumentNotValidException e) {
        List<String> detalhes = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage()).toList();
        return ResponseEntity.badRequest()
                .body(new ApiError(HttpStatus.BAD_REQUEST.value(), mensagens.texto("erro.dados-invalidos"), detalhes));
    }

    private static ResponseEntity<ApiError> erro(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), mensagem));
    }
}
