package com.verdantdays.repository;

import com.verdantdays.model.BillSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillSettingsRepository extends JpaRepository<BillSettings, Integer> {
}
