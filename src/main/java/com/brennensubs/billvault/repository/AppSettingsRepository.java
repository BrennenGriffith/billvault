package com.brennensubs.billvault.repository;

import com.brennensubs.billvault.model.AppSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingsRepository extends JpaRepository<AppSettings, Long> {}
