package com.alkywallet.config;

import com.alkywallet.entity.Cuenta;
import com.alkywallet.entity.Role;
import com.alkywallet.entity.TipoMoneda;
import com.alkywallet.entity.Usuario;
import com.alkywallet.repository.CuentaRepository;
import com.alkywallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:admin@alkywallet.com}")
    private String adminEmail;

    @Value("${admin.password:Admin123$}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(@org.springframework.lang.NonNull String... args) {
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            log.info("Creando usuario ADMIN por defecto...");
            Usuario admin = Usuario.builder()
                    .nombre("Admin")
                    .apellido("Sistema")
                    .dni("00000000")
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .rol(Role.ADMIN)
                    .isDeleted(false)
                    .build();
            userRepository.save(admin);

            log.info("Creando cuenta ARS para el ADMIN...");
            Cuenta cuentaAdmin = Cuenta.builder()
                    .usuario(admin)
                    .saldo(java.math.BigDecimal.ZERO)
                    .tipoMoneda(TipoMoneda.ARS)
                    .isDeleted(false)
                    .build();
            cuentaRepository.save(cuentaAdmin);
            log.info("Inicialización de ADMIN completada.");
        }
    }
}
