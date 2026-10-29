package ca.pacific.frs.ui.components;

import ca.pacific.frs.domain.model.Money;
import ca.pacific.frs.ui.theme.Palette;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;

/** Table set-up shared by every list in the application: padding, hover row, sorting, renderers. */
public final class Tables {
    private static final String HOVER_ROW = "frs.hoverRow";

    private Tables() {
    }

    /** Creates a configured table for a model. */
    public static JTable create(ListTableModel<?> model) {
        JTable table = new JTable(model) {
            @Override
            public String getToolTipText(MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                int col = columnAtPoint(e.getPoint());
                if (row < 0 || col < 0) {
                    return null;
                }
                Object v = getValueAt(row, col);
                return v == null ? null : String.valueOf(v instanceof Labeled l ? l.label() : v);
            }
        };
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setIntercellSpacing(new java.awt.Dimension(0, 0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);
        table.putClientProperty("JTable.autoStartsEdit", Boolean.FALSE);

        table.setDefaultRenderer(Object.class, new TextRenderer());
        table.setDefaultRenderer(String.class, new TextRenderer());
        table.setDefaultRenderer(Integer.class, new TextRenderer(SwingConstants.RIGHT, null));
        table.setDefaultRenderer(Money.class, new TextRenderer(SwingConstants.RIGHT, null));
        table.setDefaultRenderer(LocalDateTime.class, new TextRenderer(SwingConstants.LEFT,
                v -> Formats.dateTime((LocalDateTime) v)));
        table.setDefaultRenderer(LocalDate.class, new TextRenderer(SwingConstants.LEFT,
                v -> Formats.date((LocalDate) v)));
        table.setDefaultRenderer(Labeled.class, new BadgeRenderer());

        installHover(table);
        // Enter activates the row like a double-click; keep Tab moving focus out of the table.
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "frs.activate");
        table.setFocusTraversalKeys(java.awt.KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        table.setFocusTraversalKeys(java.awt.KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "none");
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK), "none");

        // Sort chronological and money columns by value, not by their text.
        if (table.getRowSorter() instanceof TableRowSorter<?> sorter) {
            for (int c = 0; c < model.getColumnCount(); c++) {
                if (Labeled.class.isAssignableFrom(model.getColumnClass(c))) {
                    sorter.setComparator(c, Comparator.comparing(o -> ((Labeled) o).label()));
                }
            }
        }
        return table;
    }

    /** Runs {@code action} with the selected row on double-click or Enter. */
    public static <T> void onActivate(JTable table, ListTableModel<T> model, Consumer<T> action) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.rowAtPoint(e.getPoint()) >= 0) {
                    selected(table, model).ifPresent(action);
                }
            }
        });
        table.getActionMap().put("frs.activate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selected(table, model).ifPresent(action);
            }
        });
    }

    public static <T> java.util.Optional<T> selected(JTable table, ListTableModel<T> model) {
        int view = table.getSelectedRow();
        if (view < 0) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(model.row(table.convertRowIndexToModel(view)));
    }

    /** Re-selects an item after the rows were reloaded, if it is still present. */
    public static <T> void reselect(JTable table, ListTableModel<T> model, java.util.function.Predicate<T> match) {
        for (int i = 0; i < model.getRowCount(); i++) {
            if (match.test(model.row(i))) {
                int view = table.convertRowIndexToView(i);
                if (view >= 0) {
                    table.setRowSelectionInterval(view, view);
                    table.scrollRectToVisible(table.getCellRect(view, 0, true));
                }
                return;
            }
        }
    }

    public static void widths(JTable table, int... unscaledWidths) {
        for (int i = 0; i < unscaledWidths.length && i < table.getColumnCount(); i++) {
            if (unscaledWidths[i] > 0) {
                table.getColumnModel().getColumn(i).setPreferredWidth(UIScale.scale(unscaledWidths[i]));
            }
        }
    }

    public static JScrollPane scroll(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.putClientProperty(FlatClientProperties.STYLE, "border: 0,0,0,0");
        return sp;
    }

    private static void installHover(JTable table) {
        MouseAdapter hover = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                set(table.rowAtPoint(e.getPoint()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                set(-1);
            }

            private void set(int row) {
                Object old = table.getClientProperty(HOVER_ROW);
                if (old == null || (int) old != row) {
                    table.putClientProperty(HOVER_ROW, row);
                    table.repaint();
                }
            }
        };
        table.addMouseMotionListener(hover);
        table.addMouseListener(hover);
    }

    static boolean isHover(JTable table, int row) {
        Object v = table.getClientProperty(HOVER_ROW);
        return v instanceof Integer i && i == row;
    }

    // ------------------------------------------------------------ renderers

    /** Implemented by values shown as status chips. */
    public interface Labeled {
        String label();

        Tone tone();

        static Labeled of(String label, Tone tone) {
            return new Labeled() {
                @Override
                public String label() {
                    return label;
                }

                @Override
                public Tone tone() {
                    return tone;
                }

                @Override
                public String toString() {
                    return label;
                }
            };
        }
    }

    /** Text cell with padding, hover tint and an optional formatter. */
    public static class TextRenderer extends DefaultTableCellRenderer {
        private final int alignment;
        private final Function<Object, String> formatter;

        public TextRenderer() {
            this(SwingConstants.LEFT, null);
        }

        public TextRenderer(int alignment, Function<Object, String> formatter) {
            this.alignment = alignment;
            this.formatter = formatter;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Object shown = value == null ? "" : formatter != null ? formatter.apply(value) : value;
            // DefaultTableCellRenderer remembers the last background it was given; clear it so a hover
            // tint on one row does not leak into the rows painted after it.
            setBackground(null);
            super.getTableCellRendererComponent(table, shown, isSelected, false, row, column);
            setHorizontalAlignment(alignment);
            setBorder(BorderFactory.createEmptyBorder(0, UIScale.scale(12), 0, UIScale.scale(12)));
            if (!isSelected && isHover(table, row)) {
                setBackground(Palette.hover());
            }
            return this;
        }
    }

    /** Status chip cell. */
    public static class BadgeRenderer implements TableCellRenderer {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        private final Badge badge = new Badge();

        public BadgeRenderer() {
            panel.add(badge);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            panel.setBorder(BorderFactory.createEmptyBorder(
                    (table.getRowHeight() - badge.getPreferredSize().height) / 2, UIScale.scale(10), 0, 0));
            if (value instanceof Labeled l) {
                badge.set(l.label(), l.tone());
            } else {
                badge.set(value == null ? "" : value.toString(), Tone.NEUTRAL);
            }
            Color bg = isSelected ? table.getSelectionBackground()
                    : isHover(table, row) ? Palette.hover()
                    : row % 2 == 1 && javax.swing.UIManager.getColor("Table.alternateRowColor") != null
                    ? javax.swing.UIManager.getColor("Table.alternateRowColor") : table.getBackground();
            panel.setBackground(bg);
            return panel;
        }
    }

    /** Percentage (0-100) drawn as a thin bar with the number beside it. */
    public static class LoadRenderer extends JLabel implements TableCellRenderer {
        private int percent;
        private Color rowBackground;

        public LoadRenderer() {
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.RIGHT);
            setBorder(BorderFactory.createEmptyBorder(0, UIScale.scale(12), 0, UIScale.scale(12)));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            percent = value instanceof Integer i ? i : 0;
            setText(percent + "%");
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            rowBackground = isSelected ? table.getSelectionBackground()
                    : isHover(table, row) ? Palette.hover()
                    : row % 2 == 1 && javax.swing.UIManager.getColor("Table.alternateRowColor") != null
                    ? javax.swing.UIManager.getColor("Table.alternateRowColor") : table.getBackground();
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(rowBackground);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int pad = UIScale.scale(12);
                int textW = UIScale.scale(44);
                int barW = Math.max(0, getWidth() - pad * 2 - textW);
                int barH = UIScale.scale(6);
                int y = (getHeight() - barH) / 2;
                g2.setColor(Palette.layer2());
                g2.fillRoundRect(pad, y, barW, barH, barH, barH);
                g2.setColor(percent >= 90 ? Palette.danger() : percent >= 70 ? Palette.warning() : Palette.accent());
                g2.fillRoundRect(pad, y, barW * Math.min(100, percent) / 100, barH, barH, barH);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
