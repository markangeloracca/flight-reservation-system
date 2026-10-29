package ca.pacific.frs.control;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Observer: controllers announce which kind of data changed after a successful commit; open views
 * subscribe and reload. Listeners may be called on any thread; UI listeners must hop to the EDT.
 */
public final class EventBus {

    public enum Topic { FLIGHTS, RESERVATIONS, PAYMENTS, CUSTOMERS, ROUTES, AIRCRAFT }

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    public void publish(Topic topic) {
        support.firePropertyChange(new PropertyChangeEvent(this, topic.name(), null, topic));
    }

    public void subscribe(Topic topic, PropertyChangeListener listener) {
        support.addPropertyChangeListener(topic.name(), listener);
    }

    public void unsubscribe(Topic topic, PropertyChangeListener listener) {
        support.removePropertyChangeListener(topic.name(), listener);
    }
}
