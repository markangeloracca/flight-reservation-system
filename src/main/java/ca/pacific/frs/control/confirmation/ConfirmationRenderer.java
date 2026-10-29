package ca.pacific.frs.control.confirmation;

import ca.pacific.frs.domain.model.BookingConfirmation;

/** Strategy: turns a booking confirmation into a document in a particular format. */
public interface ConfirmationRenderer {
    String render(BookingConfirmation confirmation);

    /** File extension without the dot, e.g. {@code txt}. */
    String fileExtension();

    String formatName();
}
