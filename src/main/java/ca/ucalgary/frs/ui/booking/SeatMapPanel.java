package ca.ucalgary.frs.ui.booking;

import ca.ucalgary.frs.domain.model.Aircraft;
import ca.ucalgary.frs.domain.model.Seat;
import ca.ucalgary.frs.domain.model.SeatClass;
import ca.ucalgary.frs.domain.model.SeatMap;
import ca.ucalgary.frs.ui.theme.Palette;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Interactive cabin diagram. Click or use the arrow keys and Space/Enter to choose a free seat.
 * Fires {@link #PROPERTY_SELECTED} with the chosen {@link Seat}.
 */
public class SeatMapPanel extends JComponent {
    public static final String PROPERTY_SELECTED = "selectedSeat";

    private SeatMap map;
    private String currentSeatCode;
    private Seat selected;
    private Seat hover;
    private Seat cursor;

    public SeatMapPanel() {
        setFocusable(true);
        setOpaque(false);
        ToolTipManager.sharedInstance().registerComponent(this);
        getAccessibleContext().setAccessibleName("Seat map");
        getAccessibleContext().setAccessibleDescription("Use the arrow keys to move between seats and Space to choose one.");

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Seat s = seatAt(e.getPoint());
                if (s != hover) {
                    hover = s;
                    setCursor(s != null && isFree(s) ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR) : null);
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = null;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                Seat s = seatAt(e.getPoint());
                if (s != null) {
                    cursor = s;
                    choose(s);
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (cursor == null && map != null) {
                    cursor = selected != null ? selected : map.aircraft().allSeats().get(0);
                }
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
        bindKey(KeyEvent.VK_LEFT, 0, -1);
        bindKey(KeyEvent.VK_RIGHT, 0, 1);
        bindKey(KeyEvent.VK_UP, -1, 0);
        bindKey(KeyEvent.VK_DOWN, 1, 0);
        for (int key : new int[]{KeyEvent.VK_SPACE, KeyEvent.VK_ENTER}) {
            getInputMap().put(KeyStroke.getKeyStroke(key, 0), "choose");
        }
        getActionMap().put("choose", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (cursor != null) {
                    choose(cursor);
                }
            }
        });
    }

    @Override
    public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                @Override
                public javax.accessibility.AccessibleRole getAccessibleRole() {
                    return javax.accessibility.AccessibleRole.PANEL;
                }
            };
        }
        return accessibleContext;
    }

    /** Shows a flight's seats. {@code currentSeatCode} marks the passenger's existing seat (may be null). */
    public void setSeatMap(SeatMap map, String currentSeatCode) {
        Seat previous = selected;
        this.map = map;
        this.currentSeatCode = currentSeatCode;
        this.selected = null;
        this.hover = null;
        this.cursor = null;
        revalidate();
        repaint();
        if (previous != null) {
            firePropertyChange(PROPERTY_SELECTED, previous, null);
        }
    }

    public Seat selectedSeat() {
        return selected;
    }

    public void clearSelection() {
        Seat old = selected;
        selected = null;
        repaint();
        firePropertyChange(PROPERTY_SELECTED, old, null);
    }

    private void choose(Seat s) {
        if (!isFree(s)) {
            return;
        }
        Seat old = selected;
        selected = s;
        repaint();
        getAccessibleContext().setAccessibleDescription("Selected seat " + s.code());
        firePropertyChange(PROPERTY_SELECTED, old, s);
    }

    private boolean isFree(Seat s) {
        return map != null && (map.isAvailable(s) || s.code().equals(currentSeatCode));
    }

    private void bindKey(int key, int dRow, int dCol) {
        String name = "move" + key;
        getInputMap().put(KeyStroke.getKeyStroke(key, 0), name);
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (map == null) {
                    return;
                }
                Aircraft ac = map.aircraft();
                if (cursor == null) {
                    cursor = ac.allSeats().get(0);
                } else {
                    char[] letters = ac.letters();
                    int col = new String(letters).indexOf(cursor.letter()) + dCol;
                    int row = cursor.row() + dRow;
                    if (col >= 0 && col < letters.length && row >= 1 && row <= ac.totalRows()) {
                        cursor = new Seat(row, letters[col], ac.classOfRow(row));
                    }
                }
                scrollRectToVisible(seatBounds(cursor));
                repaint();
            }
        });
    }

    // ------------------------------------------------------------------ geometry

    private int seat() {
        return UIScale.scale(32);
    }

    private int gap() {
        return UIScale.scale(6);
    }

    private int aisle() {
        return UIScale.scale(26);
    }

    private int labelWidth() {
        return UIScale.scale(30);
    }

    private int header() {
        return UIScale.scale(26);
    }

    private int band() {
        return UIScale.scale(30);
    }

    private int bandsBefore(int row) {
        Aircraft ac = map.aircraft();
        int bands = ac.businessRows() > 0 ? 1 : 0;
        if (row > ac.businessRows()) {
            bands++;
        }
        return bands;
    }

    private int rowY(int row) {
        return header() + bandsBefore(row) * band() + (row - 1) * (seat() + gap());
    }

    private int colX(int index) {
        int x = labelWidth() + index * (seat() + gap());
        if (index > map.aircraft().aisleAfterIndex()) {
            x += aisle() - gap();
        }
        return x;
    }

    private Rectangle seatBounds(Seat s) {
        int idx = new String(map.aircraft().letters()).indexOf(s.letter());
        return new Rectangle(colX(idx), rowY(s.row()), seat(), seat());
    }

    private Seat seatAt(Point p) {
        if (map == null) {
            return null;
        }
        for (Seat s : map.aircraft().allSeats()) {
            if (seatBounds(s).contains(p)) {
                return s;
            }
        }
        return null;
    }

    @Override
    public Dimension getPreferredSize() {
        if (map == null) {
            return new Dimension(UIScale.scale(280), UIScale.scale(300));
        }
        Aircraft ac = map.aircraft();
        int width = colX(ac.letters().length - 1) + seat() + labelWidth();
        int height = rowY(ac.totalRows()) + seat() + gap() * 2;
        return new Dimension(width, height);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        Seat s = seatAt(e.getPoint());
        if (s == null) {
            return null;
        }
        String status = s.code().equals(currentSeatCode) ? "Your current seat"
                : map.isAvailable(s) ? "Available" : "Taken";
        return s.code() + " · " + s.seatClass().displayName() + " · " + status;
    }

    // ------------------------------------------------------------------ painting

    @Override
    protected void paintComponent(Graphics g) {
        if (map == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Aircraft ac = map.aircraft();
            Font base = getFont() != null ? getFont() : javax.swing.UIManager.getFont("Label.font");
            Font small = base.deriveFont(base.getSize2D() - 1f);
            Font bold = small.deriveFont(Font.BOLD);
            int arc = UIScale.scale(9);

            // column letters
            g2.setFont(bold);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Palette.muted());
            char[] letters = ac.letters();
            for (int i = 0; i < letters.length; i++) {
                String t = String.valueOf(letters[i]);
                g2.drawString(t, colX(i) + (seat() - fm.stringWidth(t)) / 2, fm.getAscent());
            }

            // cabin bands
            int right = colX(letters.length - 1) + seat();
            if (ac.businessRows() > 0) {
                paintBand(g2, "Business", header(), right, bold);
            }
            paintBand(g2, "Economy", header() + (ac.businessRows() > 0 ? band() : 0)
                    + ac.businessRows() * (seat() + gap()), right, bold);

            for (int row = 1; row <= ac.totalRows(); row++) {
                g2.setFont(small);
                fm = g2.getFontMetrics();
                g2.setColor(Palette.muted());
                String label = String.valueOf(row);
                int y = rowY(row);
                g2.drawString(label, labelWidth() - fm.stringWidth(label) - UIScale.scale(8),
                        y + (seat() + fm.getAscent() - fm.getDescent()) / 2);
            }

            for (Seat s : ac.allSeats()) {
                paintSeat(g2, s, seatBounds(s), arc, bold);
            }
        } finally {
            g2.dispose();
        }
    }

    private void paintBand(Graphics2D g2, String text, int y, int right, Font font) {
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int mid = y + band() / 2;
        g2.setColor(Palette.muted());
        int textX = labelWidth();
        g2.drawString(text.toUpperCase(), textX, mid + fm.getAscent() / 2 - 1);
        g2.setColor(Palette.border());
        int lineStart = textX + fm.stringWidth(text.toUpperCase()) + UIScale.scale(8);
        g2.drawLine(lineStart, mid, right, mid);
    }

    private void paintSeat(Graphics2D g2, Seat s, Rectangle r, int arc, Font font) {
        boolean mine = s.code().equals(currentSeatCode);
        boolean free = map.isAvailable(s) || mine;
        boolean isSelected = s.equals(selected);
        boolean isHover = s.equals(hover) && free;
        boolean business = s.seatClass() == SeatClass.BUSINESS;

        Color fill;
        Color stroke;
        Color text;
        if (isSelected) {
            fill = Palette.accent();
            stroke = Palette.accent();
            text = Palette.onAccent();
        } else if (!free) {
            fill = Palette.layer2();
            stroke = Palette.layer2();
            text = Palette.muted();
        } else {
            fill = isHover ? Palette.hover() : business ? Palette.accentSoft() : Palette.layer1();
            stroke = mine ? Palette.success() : isHover ? Palette.accent() : Palette.border();
            text = Palette.text();
        }
        g2.setColor(fill);
        g2.fillRoundRect(r.x, r.y, r.width, r.height, arc, arc);
        g2.setStroke(new BasicStroke(UIScale.scale(mine && !isSelected ? 2f : 1f)));
        g2.setColor(stroke);
        g2.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, arc, arc);

        if (!free) {
            int p = UIScale.scale(10);
            g2.setColor(Palette.muted());
            g2.setStroke(new BasicStroke(UIScale.scale(1.5f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(r.x + p, r.y + p, r.x + r.width - p, r.y + r.height - p);
            g2.drawLine(r.x + r.width - p, r.y + p, r.x + p, r.y + r.height - p);
        } else {
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            String label = String.valueOf(s.letter());
            g2.setColor(text);
            g2.drawString(label, r.x + (r.width - fm.stringWidth(label)) / 2,
                    r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2);
        }
        if (isFocusOwner() && s.equals(cursor)) {
            g2.setColor(Palette.accent());
            g2.setStroke(new BasicStroke(UIScale.scale(2f)));
            int o = UIScale.scale(3);
            g2.drawRoundRect(r.x - o, r.y - o, r.width + o * 2 - 1, r.height + o * 2 - 1, arc + o, arc + o);
        }
    }

    /** Small key explaining the seat colours. */
    public static JComponent legend() {
        javax.swing.JPanel p = new javax.swing.JPanel(new net.miginfocom.swing.MigLayout("insets 0, gap 14"));
        p.setOpaque(false);
        p.add(swatch("Economy", Palette::layer1, Palette::border, false));
        p.add(swatch("Business", Palette::accentSoft, Palette::border, false));
        p.add(swatch("Selected", Palette::accent, Palette::accent, false));
        p.add(swatch("Taken", Palette::layer2, Palette::layer2, true));
        return p;
    }

    private static JComponent swatch(String text, java.util.function.Supplier<Color> fill,
                                     java.util.function.Supplier<Color> border, boolean cross) {
        javax.swing.JLabel l = new javax.swing.JLabel(text);
        l.setIcon(new javax.swing.Icon() {
            @Override
            public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int s = getIconWidth();
                g2.setColor(fill.get());
                g2.fillRoundRect(x, y, s, s, UIScale.scale(5), UIScale.scale(5));
                g2.setColor(border.get());
                g2.drawRoundRect(x, y, s - 1, s - 1, UIScale.scale(5), UIScale.scale(5));
                if (cross) {
                    g2.setColor(Palette.muted());
                    int p = UIScale.scale(4);
                    g2.drawLine(x + p, y + p, x + s - p, y + s - p);
                    g2.drawLine(x + s - p, y + p, x + p, y + s - p);
                }
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return UIScale.scale(14);
            }

            @Override
            public int getIconHeight() {
                return UIScale.scale(14);
            }
        });
        ca.ucalgary.frs.ui.theme.Typography.mutedText(l);
        return l;
    }
}
