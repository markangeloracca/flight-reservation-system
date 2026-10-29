package ca.ucalgary.frs.control;

import ca.ucalgary.frs.domain.exception.AuthorizationException;
import ca.ucalgary.frs.domain.model.CustomerUser;
import ca.ucalgary.frs.domain.model.Permission;
import ca.ucalgary.frs.domain.model.User;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Optional;

/**
 * Who is signed in. Observable: views listen for {@link #PROPERTY_USER} to switch between
 * the sign-in screen and the role's workspace.
 */
public final class Session {
    public static final String PROPERTY_USER = "user";

    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
    private volatile User user;

    public Optional<User> currentUser() {
        return Optional.ofNullable(user);
    }

    public User requireUser() {
        User u = user;
        if (u == null) {
            throw new AuthorizationException("Please sign in first.");
        }
        return u;
    }

    public User require(Permission permission) {
        User u = requireUser();
        if (!u.can(permission)) {
            throw new AuthorizationException("Your role does not allow this action.");
        }
        return u;
    }

    public boolean can(Permission permission) {
        User u = user;
        return u != null && u.can(permission);
    }

    /** The signed-in customer's profile id, if the user is a customer. */
    public Optional<Long> customerId() {
        return user instanceof CustomerUser cu ? Optional.of(cu.customerId()) : Optional.empty();
    }

    void signIn(User newUser) {
        User old = this.user;
        this.user = newUser;
        changes.firePropertyChange(PROPERTY_USER, old, newUser);
    }

    void signOut() {
        User old = this.user;
        this.user = null;
        changes.firePropertyChange(PROPERTY_USER, old, null);
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        changes.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        changes.removePropertyChangeListener(l);
    }
}
