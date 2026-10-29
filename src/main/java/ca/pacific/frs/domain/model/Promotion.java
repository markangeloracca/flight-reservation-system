package ca.pacific.frs.domain.model;

/** A monthly promotion. One active promotion is picked at random each month. */
public record Promotion(Long id, String title, String description, int discountPercent, boolean active) {
    public Promotion {
        Validate.required("title", "Title", title);
        Validate.range("discountPercent", "Discount", discountPercent, 0, 90);
    }
}
