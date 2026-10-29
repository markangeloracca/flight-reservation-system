package ca.ucalgary.frs.control.confirmation;

import ca.ucalgary.frs.domain.model.BookingConfirmation;

/** Strategy: turns a booking confirmation into a document in a particular format. */
public interface ConfirmationRenderer {
    String render(BookingConfirmation confirmation);

    /** File extension without the dot, e.g. {@code txt}. */
    String fileExtension();

    String formatName();
}
