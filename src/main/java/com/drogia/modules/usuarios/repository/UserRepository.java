package com.drogia.modules.usuarios.repository;

import com.drogia.modules.usuarios.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a datos de la entidad User. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    /** Busca por usuario de acceso o por correo. */
    @Query("select u from User u where u.username = :value or u.email = :value")
    Optional<User> findByUsernameOrEmail(@Param("value") String value);
}
