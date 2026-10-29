package ca.pacific.frs.domain.repository;

import java.util.Optional;

/** Small key-value store for application state (for example, this month's promotion). */
public interface SettingsRepository {
    Optional<String> get(String key);

    void put(String key, String value);
}
