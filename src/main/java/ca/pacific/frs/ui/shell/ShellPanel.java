package ca.pacific.frs.ui.shell;

import ca.pacific.frs.app.AppContext;
import ca.pacific.frs.domain.model.User;
import ca.pacific.frs.ui.components.Icons;
import ca.pacific.frs.ui.theme.Palette;
import ca.pacific.frs.ui.theme.Theme;
import ca.pacific.frs.ui.theme.Typography;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import net.miginfocom.swing.MigLayout;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/** The signed-in workspace: sidebar navigation, page header and the current page. */
final class ShellPanel extends JPanel {
    private final List<Page> pages;
    private final List<NavButton> navButtons = new ArrayList<>();
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JLabel title = Typography.h1("");
    private final JLabel subtitle = Typography.caption("");
    private final JButton themeToggle = new JButton();
    private final PropertyChangeListener themeListener = e -> updateThemeIcon();
    private int current = -1;

    ShellPanel(AppContext ctx, User user, Runnable signOut) {
        super(new BorderLayout());
        this.pages = PageFactory.pagesFor(user, ctx);

        add(sidebar(user, signOut), BorderLayout.WEST);

        JPanel main = new JPanel(new MigLayout("insets 20 28 24 28, fill, wrap 1", "[grow, fill]", "[][grow, fill]"));
        JPanel header = new JPanel(new MigLayout("insets 0 0 12 0, fillx", "[grow][]", "[]0[]"));
        header.setOpaque(false);
        header.add(title);
        themeToggle.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        themeToggle.addActionListener(e -> Theme.toggle());
        header.add(themeToggle, "spany 2, aligny center, wrap");
        header.add(subtitle);
        main.add(header);
        content.setOpaque(false);
        for (int i = 0; i < pages.size(); i++) {
            content.add(pages.get(i).view(), String.valueOf(i));
        }
        main.add(content);
        add(main, BorderLayout.CENTER);

        updateThemeIcon();
        Theme.addListener(themeListener);
        if (!pages.isEmpty()) {
            navigate(0);
        }
    }

    private JComponent sidebar(User user, Runnable signOut) {
        JPanel side = new JPanel(new MigLayout("insets 20 14 16 14, fillx, wrap 1, gap 0 4", "[grow, fill]")) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Palette.layer1());
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Palette.border());
                g.fillRect(getWidth() - 1, 0, 1, getHeight());
            }
        };
        side.setPreferredSize(new Dimension(UIScale.scale(236), 10));

        JPanel brand = new JPanel(new MigLayout("insets 0 4 0 0, gap 10", "[][grow]", "[]0[]"));
        brand.setOpaque(false);
        brand.add(new Brand(34), "spany 2");
        brand.add(Typography.strong("Flight Reservation"), "wrap");
        brand.add(Typography.small("System"));
        side.add(brand, "gapbottom 24");
        side.add(Typography.small(user.role().displayName().toUpperCase()), "gapleft 14, gapbottom 6");

        ButtonGroup group = new ButtonGroup();
        String mod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() == java.awt.event.InputEvent.META_DOWN_MASK
                ? "⌘" : "Ctrl+";
        for (int i = 0; i < pages.size(); i++) {
            Page p = pages.get(i);
            NavButton b = new NavButton(p.title(), p.icon());
            int index = i;
            b.addActionListener(e -> navigate(index));
            if (i < 9) {
                b.setToolTipText(p.subtitle() + "  (" + mod + (i + 1) + ")");
            }
            group.add(b);
            navButtons.add(b);
            side.add(b);
        }

        side.add(new JLabel(), "pushy");
        JPanel me = new JPanel(new MigLayout("insets 10, gap 10, fillx", "[][grow, fill][]", "[]0[]")) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Palette.layer2());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), UIScale.scale(12), UIScale.scale(12));
                g2.dispose();
            }
        };
        me.setOpaque(false);
        me.add(new Avatar(initials(user.fullName())), "spany 2");
        JLabel name = Typography.strong(user.fullName());
        me.add(name, "wmin 0");
        JButton out = new JButton(Icons.muted("logout", Icons.SMALL));
        out.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        out.setToolTipText("Sign out");
        out.getAccessibleContext().setAccessibleName("Sign out");
        out.addActionListener(e -> signOut.run());
        me.add(out, "spany 2, wrap");
        me.add(Typography.small(user.username()), "wmin 0");
        side.add(me);
        return side;
    }

    private static String initials(String name) {
        StringBuilder sb = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (!part.isEmpty() && sb.length() < 2) {
                sb.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return sb.toString();
    }

    int pageCount() {
        return pages.size();
    }

    String pageTitle(int index) {
        return pages.get(index).title();
    }

    void navigate(int index) {
        if (index < 0 || index >= pages.size()) {
            return;
        }
        current = index;
        Page p = pages.get(index);
        navButtons.get(index).setSelected(true);
        title.setText(p.title());
        subtitle.setText(p.subtitle());
        cards.show(content, String.valueOf(index));
        p.onShow();
        SwingUtilities.invokeLater(p::focusSearch);
    }

    void focusSearch() {
        if (current >= 0) {
            pages.get(current).focusSearch();
        }
    }

    void dispose() {
        Theme.removeListener(themeListener);
        pages.forEach(Page::dispose);
    }

    private void updateThemeIcon() {
        boolean dark = Theme.isDark();
        themeToggle.setIcon(Icons.get(dark ? "sun" : "moon", Icons.MEDIUM));
        String tip = dark ? "Switch to light mode" : "Switch to dark mode";
        themeToggle.setToolTipText(tip);
        themeToggle.getAccessibleContext().setAccessibleName(tip);
    }

    /** Round badge with the user's initials. */
    private static final class Avatar extends JComponent {
        private final String text;

        Avatar(String text) {
            this.text = text;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(UIScale.scale(34), UIScale.scale(34));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Palette.accentSoft());
            g2.fillOval(0, 0, getWidth(), getHeight());
            g2.setColor(Palette.accent());
            g2.setFont(getFont().deriveFont(java.awt.Font.BOLD));
            var fm = g2.getFontMetrics();
            g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }
}
