package com.backend.protection.repository;

import com.backend.protection.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, String> {
    List<Appointment> findByPatientIdOrderByAppointmentDateDesc(String patientId);
    List<Appointment> findByDoctorIdOrderByAppointmentDateDesc(String doctorId);
    List<Appointment> findByStatusOrderByAppointmentDateDesc(String status);
    long countByStatus(String status);
}
