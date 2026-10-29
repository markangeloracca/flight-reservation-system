package ca.pacific.frs.ui.components;

import ca.pacific.frs.ui.theme.Palette;
import ca.pacific.frs.ui.theme.Typography;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import net.miginfocom.swing.MigLayout;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * Button showing a date range that opens a two-month calendar. Click a start date, then an end date, or pick a
 * preset. In the calendar the arrow keys move between days, Page Up/Down between months, and Enter/Space chooses.
 * Fires {@link #PROPERTY_RANGE} when the range changes.
 */
public class DateRangePicker extends JButton {
    public static final String PROPERTY_RANGE = "dateRange";

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final String[] WEEKDAYS = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};

    private LocalDate start;
    private LocalDate end;
    private JPopupMenu popup;

    public DateRangePicker(LocalDate start, LocalDate end) {
        super(Icons.muted("calendar", Icons.SMALL));
        setHorizontalAlignment(LEADING);
        putClientProperty(FlatClientProperties.STYLE, "margin: 4,10,4,12; iconTextGap: 8");
        setRange(start, end);
        addActionListener(e -> open());
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "openCalendar");
        getActionMap().put("openCalendar", keyAction(this::open));
    }

    public LocalDate start() {
        return start;
    }

    public LocalDate end() {
        return end;
    }

    /** Sets the range (dates are swapped if out of order) and fires {@link #PROPERTY_RANGE} if it changed. */
    public void setRange(LocalDate a, LocalDate b) {
        List<LocalDate> old = start == null ? null : List.of(start, end);
        start = a.isAfter(b) ? b : a;
        end = a.isAfter(b) ? a : b;
        setText(label(start, end));
        firePropertyChange(PROPERTY_RANGE, old, List.of(start, end));
    }

    static String label(LocalDate a, LocalDate b) {
        if (a.equals(b)) {
            return a.format(Formats.DATE);
        }
        if (a.getYear() == b.getYear()) {
            return a.format(DAY_MONTH) + " – " + b.format(DAY_MONTH_YEAR);
        }
        return a.format(DAY_MONTH_YEAR) + " – " + b.format(DAY_MONTH_YEAR);
    }

    private void open() {
        if (!isEnabled() || (popup != null && popup.isVisible())) {
            return;
        }
        Calendar calendar = new Calendar();
        popup = new JPopupMenu();
        popup.setLayout(new BorderLayout());
        popup.add(calendar);
        // Centre over the window's content rather than dropping down from the button
        JRootPane root = SwingUtilities.getRootPane(this);
        Dimension size = popup.getPreferredSize();
        Point at = root == null ? new Point(0, getHeight() + UIScale.scale(4))
                : SwingUtilities.convertPoint(root, (root.getWidth() - size.width) / 2,
                        (root.getHeight() - size.height) / 2, this);
        popup.show(this, at.x, at.y);
        calendar.requestFocusInWindow();
    }

    private void commit(LocalDate a, LocalDate b) {
        popup.setVisible(false);
        setRange(a, b);
        requestFocusInWindow();
    }

    private static AbstractAction keyAction(Runnable r) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                r.run();
            }
        };
    }

    /** Popup content: presets on the left, two consecutive months on the right. Holds the in-progress selection. */
    private final class Calendar extends JPanel {
        private LocalDate anchor = start;
        /** Second end of the range, or null while waiting for the user to choose it. */
        private LocalDate other = end;
        private LocalDate hover;
        private LocalDate cursor = start;
        private YearMonth first = YearMonth.from(start);
        private final JLabel[] titles = {Typography.strong(""), Typography.strong("")};
        private final MonthGrid[] grids = {new MonthGrid(0), new MonthGrid(1)};
        private final JLabel hint = Typography.caption(" ");

        Calendar() {
            super(new MigLayout("insets 12, gap 12", "[][][]", "[top]"));
            setFocusable(true);
            getAccessibleContext().setAccessibleName("Choose dates");

            LocalDate today = LocalDate.now();
            JPanel presets = new JPanel(new MigLayout("insets 0, wrap 1, gap 2", "[fill]"));
            presets.setOpaque(false);
            presets.add(Typography.small("Quick picks"), "gapbottom 4");
            presets.add(preset("Today", today, today));
            presets.add(preset("Next 7 days", today, today.plusDays(6)));
            presets.add(preset("Next 14 days", today, today.plusDays(13)));
            presets.add(preset("Next 30 days", today, today.plusDays(29)));
            presets.add(preset("This month", today.withDayOfMonth(1), today.with(TemporalAdjusters.lastDayOfMonth())));
            YearMonth next = YearMonth.from(today).plusMonths(1);
            presets.add(preset("Next month", next.atDay(1), next.atEndOfMonth()));

            JPanel months = new JPanel(new MigLayout("insets 0, gap 20 6", "[][]", "[][][]"));
            months.setOpaque(false);
            for (JLabel t : titles) {
                t.setHorizontalAlignment(SwingConstants.CENTER);
            }
            JPanel left = new JPanel(new MigLayout("insets 0, fillx", "[][grow, center][]"));
            left.setOpaque(false);
            left.add(navButton("chevron-left", "Previous month", -1));
            left.add(titles[0]);
            left.add(new JLabel(), "w 28!");
            JPanel right = new JPanel(new MigLayout("insets 0, fillx", "[][grow, center][]"));
            right.setOpaque(false);
            right.add(new JLabel(), "w 28!");
            right.add(titles[1]);
            right.add(navButton("chevron-right", "Next month", 1));
            months.add(left, "growx");
            months.add(right, "growx, wrap");
            months.add(grids[0]);
            months.add(grids[1], "wrap");
            months.add(hint, "span 2");

            add(presets);
            add(new JSeparator(SwingConstants.VERTICAL), "growy");
            add(months);

            bind(KeyEvent.VK_LEFT, d -> d.minusDays(1));
            bind(KeyEvent.VK_RIGHT, d -> d.plusDays(1));
            bind(KeyEvent.VK_UP, d -> d.minusWeeks(1));
            bind(KeyEvent.VK_DOWN, d -> d.plusWeeks(1));
            bind(KeyEvent.VK_PAGE_UP, d -> d.minusMonths(1));
            bind(KeyEvent.VK_PAGE_DOWN, d -> d.plusMonths(1));
            getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "pick");
            getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "pick");
            getActionMap().put("pick", keyAction(() -> pick(cursor)));
            getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
            getActionMap().put("close", keyAction(() -> popup.setVisible(false)));
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    repaintGrids();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    repaintGrids();
                }
            });
            refresh();
        }

        private JToggleButton preset(String text, LocalDate a, LocalDate b) {
            JToggleButton button = new JToggleButton(text, a.equals(start) && b.equals(end));
            button.setHorizontalAlignment(LEADING);
            button.setFocusable(false);
            button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TAB);
            button.putClientProperty(FlatClientProperties.STYLE, "tab.selectedBackground: $Frs.accentSoftColor;"
                    + "tab.selectedForeground: $Frs.accentColor; tab.underlineHeight: 0; arc: 8; margin: 4,10,4,10");
            button.addActionListener(e -> commit(a, b));
            return button;
        }

        private JButton navButton(String icon, String name, int months) {
            JButton button = new JButton(Icons.get(icon));
            button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
            button.setToolTipText(name);
            button.getAccessibleContext().setAccessibleName(name);
            button.setFocusable(false);
            button.addActionListener(e -> {
                first = first.plusMonths(months);
                refresh();
            });
            return button;
        }

        private void bind(int key, UnaryOperator<LocalDate> move) {
            String name = "move" + key;
            getInputMap().put(KeyStroke.getKeyStroke(key, 0), name);
            getActionMap().put(name, keyAction(() -> moveCursor(move.apply(cursor))));
        }

        private void moveCursor(LocalDate d) {
            cursor = d;
            hover = null;
            YearMonth m = YearMonth.from(d);
            if (m.isBefore(first)) {
                first = m;
            } else if (m.isAfter(first.plusMonths(1))) {
                first = m.minusMonths(1);
            }
            refresh();
        }

        /** First click starts a new range; the second click completes it and closes the popup. */
        private void pick(LocalDate d) {
            if (other == null) {
                commit(anchor, d);
                return;
            }
            anchor = d;
            other = null;
            cursor = d;
            refresh();
        }

        /** The range to draw: the chosen one, or a live preview from the anchor to the hovered/cursor date. */
        private LocalDate[] shown() {
            LocalDate b = other != null ? other : hover != null ? hover : cursor;
            return anchor.isAfter(b) ? new LocalDate[]{b, anchor} : new LocalDate[]{anchor, b};
        }

        private void refresh() {
            for (int i = 0; i < titles.length; i++) {
                titles[i].setText(first.plusMonths(i).format(MONTH));
            }
            hint.setText(other == null ? "Now choose the last day" : "Choose the first day, or a quick pick");
            getAccessibleContext().setAccessibleDescription(Formats.date(cursor) + ". " + hint.getText());
            repaintGrids();
        }

        private void repaintGrids() {
            for (MonthGrid g : grids) {
                g.repaint();
            }
        }

        /** One month of day cells, weeks starting on Sunday, with a weekday header row. */
        private final class MonthGrid extends JComponent {
            private final int offset;

            MonthGrid(int offset) {
                this.offset = offset;
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                MouseAdapter mouse = new MouseAdapter() {
                    @Override
                    public void mouseMoved(MouseEvent e) {
                        LocalDate d = dayAt(e.getPoint());
                        if (d != null && !d.equals(hover) || d == null && hover != null) {
                            hover = d;
                            repaintGrids();
                        }
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hover = null;
                        repaintGrids();
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                        Calendar.this.requestFocusInWindow();
                        LocalDate d = dayAt(e.getPoint());
                        if (d != null) {
                            pick(d);
                        }
                    }
                };
                addMouseListener(mouse);
                addMouseMotionListener(mouse);
            }

            private YearMonth month() {
                return first.plusMonths(offset);
            }

            private int lead() {
                return month().atDay(1).getDayOfWeek().getValue() % 7;
            }

            private static int cell() {
                return UIScale.scale(36);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(7 * cell(), 7 * cell());
            }

            private LocalDate dayAt(Point p) {
                int col = p.x / cell();
                int row = p.y / cell() - 1;
                int day = row * 7 + col - lead() + 1;
                return col < 7 && row >= 0 && day >= 1 && day <= month().lengthOfMonth() ? month().atDay(day) : null;
            }

            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int cs = cell();
                int pad = UIScale.scale(3);
                Font base = UIManager.getFont("Label.font");
                Font bold = base.deriveFont(Font.BOLD);

                g.setFont(base.deriveFont(base.getSize2D() * 0.85f));
                g.setColor(Palette.muted());
                for (int c = 0; c < 7; c++) {
                    centered(g, WEEKDAYS[c], c * cs, 0, cs);
                }

                LocalDate[] range = shown();
                LocalDate lo = range[0];
                LocalDate hi = range[1];
                LocalDate today = LocalDate.now();
                boolean focused = Calendar.this.isFocusOwner();
                YearMonth month = month();
                for (int day = 1; day <= month.lengthOfMonth(); day++) {
                    LocalDate d = month.atDay(day);
                    int i = lead() + day - 1;
                    int x = (i % 7) * cs;
                    int y = (i / 7 + 1) * cs;
                    boolean isLo = d.equals(lo);
                    boolean isHi = d.equals(hi);
                    boolean inside = !d.isBefore(lo) && !d.isAfter(hi);

                    if (inside && !lo.equals(hi)) {
                        int from = isLo ? x + cs / 2 : x;
                        int to = isHi ? x + cs / 2 : x + cs;
                        g.setColor(Palette.accentSoft());
                        g.fillRect(from, y + pad, to - from, cs - 2 * pad);
                    }
                    if (isLo || isHi) {
                        g.setColor(Palette.accent());
                        g.fillOval(x + pad, y + pad, cs - 2 * pad, cs - 2 * pad);
                    } else if (d.equals(hover)) {
                        g.setColor(Palette.hover());
                        g.fillOval(x + pad, y + pad, cs - 2 * pad, cs - 2 * pad);
                    }
                    if (focused && d.equals(cursor)) {
                        g.setColor(isLo || isHi ? Palette.onAccent() : Palette.accent());
                        g.setStroke(new BasicStroke(UIScale.scale(1.5f)));
                        int r = isLo || isHi ? 2 * pad : pad;
                        g.drawOval(x + r, y + r, cs - 2 * r, cs - 2 * r);
                    }

                    boolean isToday = d.equals(today);
                    g.setFont(isLo || isHi || isToday ? bold : base);
                    g.setColor(isLo || isHi ? Palette.onAccent() : isToday ? Palette.accent() : Palette.text());
                    centered(g, Integer.toString(day), x, y, cs);
                    if (isToday && !(isLo || isHi)) {
                        int dot = UIScale.scale(4);
                        g.fillOval(x + (cs - dot) / 2, y + cs - pad - dot - UIScale.scale(2), dot, dot);
                    }
                }
                g.dispose();
            }

            private static void centered(Graphics2D g, String s, int x, int y, int cs) {
                FontMetrics fm = g.getFontMetrics();
                g.drawString(s, x + (cs - fm.stringWidth(s)) / 2, y + (cs - fm.getHeight()) / 2 + fm.getAscent());
            }
        }
    }
}
