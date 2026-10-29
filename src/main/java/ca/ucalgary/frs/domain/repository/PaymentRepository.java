package ca.ucalgary.frs.domain.repository;

import ca.ucalgary.frs.domain.model.Payment;

import java.util.List;

public interface PaymentRepository {
    Payment insert(Payment payment);

    void update(Payment payment);

    List<Payment> findByReservation(long reservationId);

    List<Payment> findByCustomer(long customerId);
}
