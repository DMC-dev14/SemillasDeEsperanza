package com.donation.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donation.app.model.Rol;

public interface RolRepository extends JpaRepository<Rol, Long>{
    
}
