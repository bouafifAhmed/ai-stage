package com.gestionstages.repository;

import com.gestionstages.model.Role;
import com.gestionstages.model.Utilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<Utilisateur> findByRole(Role role, Pageable pageable);

    Optional<Utilisateur> findByIdAndRole(Long id, Role role);

    Page<Utilisateur> findByRoleAndActif(Role role, boolean actif, Pageable pageable);

    Page<Utilisateur> findByRoleAndFiliereContainingIgnoreCase(
            Role role,
            String filiere,
            Pageable pageable
    );

    Page<Utilisateur> findByRoleAndActifAndFiliereContainingIgnoreCase(
            Role role,
            boolean actif,
            String filiere,
            Pageable pageable
    );
}
