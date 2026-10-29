package ca.pacific.frs.domain.repository;

import ca.pacific.frs.domain.model.Role;
import ca.pacific.frs.domain.model.User;

import java.util.Optional;

public interface UserRepository {

    /** A user plus their stored password hash, for sign-in. */
    record StoredCredentials(User user, String passwordHash) {
    }

    Optional<StoredCredentials> findCredentials(String username);

    boolean usernameExists(String username);

    Optional<Long> findUserIdByCustomer(long customerId);

    long create(String username, String passwordHash, Role role, String fullName, String email, Long customerId);

    void updateProfile(long userId, String fullName, String email);
}
