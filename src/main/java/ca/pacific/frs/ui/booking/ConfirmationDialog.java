package ca.pacific.frs.ui.booking;

import ca.pacific.frs.app.AppContext;
import ca.pacific.frs.control.confirmation.ConfirmationRenderer;
import ca.pacific.frs.control.confirmation.HtmlConfirmationRenderer;
import ca.pacific.frs.control.confirmation.PlainTextConfirmationRenderer;
import ca.pacific.frs.domain.model.BookingConfirmation;
import ca.pacific.frs.ui.components.AppDialog;
import ca.pacific.frs.ui.components.BackgroundTask;
import ca.pacific.frs.ui.components.Banner;
import ca.pacific.frs.ui.components.Toasts;
import ca.pacific.frs.ui.components.Tone;
import ca.pacific.frs.ui.theme.Palette;
import com.formdev.flatlaf.util.UIScale;
import net.miginfocom.swing.MigLayout;

import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

/** Shows a booking confirmation and lets the user save it as a text or HTML file. */
public final class ConfirmationDialog extends AppDialog {
    private final AppContext ctx;
    private final BookingConfirmation confirmation;

    private ConfirmationDialog(Component owner, AppContext ctx, BookingConfirmation c, boolean justBooked) {
        super(owner, justBooked ? "Booking confirmed" : "Booking confirmation",
                "Reference " + c.reservation().confirmationCode());
        this.ctx = ctx;
        this.confirmation = c;

        JPanel body = new JPanel(new MigLayout("insets 0, fill, wrap 1", "[grow, fill]", "[][grow, fill]"));
        Banner banner = new Banner();
        if (justBooked) {
            banner.show(Tone.SUCCESS, "Payment received. Your seat " + c.reservation().seat().code()
                    + " is confirmed. Keep your booking reference for check-in.");
        }
        body.add(banner, "hidemode 3, gapbottom 8");

        JEditorPane doc = new JEditorPane("text/html", new HtmlConfirmationRenderer(Palette.hex(Palette.text()),
                Palette.hex(Palette.muted()), Palette.hex(Palette.accent())).render(c));
        doc.setEditable(false);
        doc.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        doc.setCaretPosition(0);
        doc.getAccessibleContext().setAccessibleName("Booking confirmation");
        JScrollPane scroll = new JScrollPane(doc);
        scroll.setPreferredSize(new Dimension(UIScale.scale(520), UIScale.scale(440)));
        body.add(scroll);
        setBody(body);

        addSecondaryButton("Copy reference", () -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(c.reservation().confirmationCode()), null);
            Toasts.info(this, "Booking reference copied.");
        });
        addSecondaryButton("Save as…", this::save);
        addPrimaryButton("Done", this::dispose);
    }

    /** Loads the confirmation in the background, then shows it. */
    public static void open(Component owner, AppContext ctx, long reservationId, boolean justBooked) {
        BackgroundTask.of(() -> ctx.booking().confirmation(reservationId))
                .onSuccess(c -> new ConfirmationDialog(owner, ctx, c, justBooked).present())
                .start(owner);
    }

    private void save() {
        List<ConfirmationRenderer> renderers = List.of(new PlainTextConfirmationRenderer(), new HtmlConfirmationRenderer());
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save booking confirmation");
        chooser.setAcceptAllFileFilterUsed(false);
        for (ConfirmationRenderer r : renderers) {
            chooser.addChoosableFileFilter(new FileNameExtensionFilter(
                    r.formatName() + " (*." + r.fileExtension() + ")", r.fileExtension()));
        }
        String code = confirmation.reservation().confirmationCode();
        chooser.setSelectedFile(new File("Booking-" + code + ".txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        FileNameExtensionFilter filter = (FileNameExtensionFilter) chooser.getFileFilter();
        ConfirmationRenderer renderer = renderers.stream()
                .filter(r -> r.fileExtension().equals(filter.getExtensions()[0]))
                .findFirst().orElse(renderers.get(0));
        Path target = chooser.getSelectedFile().toPath();
        String fileName = target.getFileName().toString();
        if (!fileName.toLowerCase().endsWith("." + renderer.fileExtension())) {
            target = target.resolveSibling(fileName.replaceFirst("\\.[^.]*$", "") + "." + renderer.fileExtension());
        }
        Path finalTarget = target;
        BackgroundTask.run(() -> ctx.booking().exportConfirmation(confirmation.reservation().id(), finalTarget, renderer))
                .onSuccess(v -> Toasts.success(this, "Saved " + finalTarget.getFileName() + "."))
                .start(this);
    }
}
