package com.futurevet.config;

import com.futurevet.model.Perfil;
import com.futurevet.repository.AnimalRepository;
import com.futurevet.repository.ConsultaRepository;
import com.futurevet.repository.UsuarioRepository;
import com.futurevet.repository.VacinaRepository;
import com.futurevet.web.forms.AgendaForm;
import com.futurevet.web.forms.AnimalForm;
import com.futurevet.web.forms.VacinaForm;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
public class DemoData implements CommandLineRunner {
    private static final String SENHA_DEMO = "Futurevet123!";
    private static final String CLINICA = "Clínica FutureVet";

    private final UsuarioRepository users;
    private final AnimalRepository animais;
    private final ConsultaRepository consultas;
    private final VacinaRepository vacinas;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public DemoData(UsuarioRepository users, AnimalRepository animais, ConsultaRepository consultas,
            VacinaRepository vacinas, PasswordEncoder encoder, Clock clock) {
        this.users = users;
        this.animais = animais;
        this.consultas = consultas;
        this.vacinas = vacinas;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        if (!users.todos().isEmpty())
            return;
        String senha = encoder.encode(SENHA_DEMO);
        users.criar(CLINICA, "clinica@futurevet.local", senha, 90000000001L, null, Perfil.ADMIN);
        long tutor = users.criar("Marina Costa", "tutor@futurevet.local", senha, 90000000002L, 11988887777L,
                Perfil.TUTOR);
        users.criar("Outro Tutor", "outro@futurevet.local", senha, 90000000003L, null, Perfil.TUTOR);

        long bento = animais.criar(animal("Bento", "CAO", "3 anos", "MEDIO", "Sem raça definida", "12.50"), tutor);
        long luna = animais.criar(animal("Luna", "GATO", "6 meses", "PEQUENO", "Sem raça definida", "3.20"), tutor);

        LocalDate hoje = LocalDate.now(clock);
        LocalDate umAnoAtras = hoje.minusMonths(12);
        vacinas.criar(vacina(bento, "V10", umAnoAtras, hoje.minusDays(10)), null);
        vacinas.criar(vacina(bento, "Antirrábica", umAnoAtras, hoje.plusDays(20)), null);
        vacinas.criar(vacina(luna, "V4 Felina", umAnoAtras, null), null);

        consultas.criar(consulta(bento, "Consulta preventiva", proximoDiaUtil(hoje.plusDays(2)), "Consultório 1"));
    }

    private static AnimalForm animal(String nome, String especie, String idade, String porte, String raca,
            String peso) {
        AnimalForm a = new AnimalForm();
        a.setNome(nome);
        a.setEspecie(especie);
        a.setIdade(idade);
        a.setPorte(porte);
        a.setRaca(raca);
        a.setPeso(new BigDecimal(peso));
        return a;
    }

    private static VacinaForm vacina(long animal, String nome, LocalDate data, LocalDate proximaDose) {
        VacinaForm v = new VacinaForm();
        v.setAnimalId(animal);
        v.setNome(nome);
        v.setData(data);
        v.setProximaDose(proximaDose);
        v.setLocal(CLINICA);
        return v;
    }

    private static AgendaForm consulta(long animal, String tipo, LocalDate data, String local) {
        AgendaForm c = new AgendaForm();
        c.setAnimalId(animal);
        c.setTipo(tipo);
        c.setData(data);
        c.setHora(LocalTime.of(10, 0));
        c.setLocal(local);
        return c;
    }

    private static LocalDate proximoDiaUtil(LocalDate data) {
        return data.getDayOfWeek() == DayOfWeek.SUNDAY ? data.plusDays(1) : data;
    }
}
