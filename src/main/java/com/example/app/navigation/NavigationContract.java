package com.example.app.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Navigation Contract for Perazim Android Application.
 * Defines canonical 5 primary navigation tabs, tab IDs, titles, icon assets, and route contracts.
 * Implements Phase 0.4 and Guidebook §15 specifications.
 */
public final class NavigationContract {

    private NavigationContract() {
        // Prevent instantiation
    }

    // =========================================================================
    // TAB IDENTIFIERS (Guidebook §15)
    // =========================================================================

    public static final int TAB_ID_HOME = 0;
    public static final int TAB_ID_SERMONS = 1;
    public static final int TAB_ID_WORSHIP = 2;
    public static final int TAB_ID_FELLOWSHIP = 3;
    public static final int TAB_ID_PROFILE = 4;
    public static final int TOTAL_TAB_COUNT = 5;

    // =========================================================================
    // TAB TITLES
    // =========================================================================

    public static final String TAB_TITLE_HOME = "Home";
    public static final String TAB_TITLE_SERMONS = "Sermons";
    public static final String TAB_TITLE_WORSHIP = "Worship";
    public static final String TAB_TITLE_FELLOWSHIP = "Fellowship";
    public static final String TAB_TITLE_PROFILE = "Profile";

    // =========================================================================
    // TAB ICONS (Unicode Symbols / Drawables)
    // =========================================================================

    public static final String TAB_ICON_HOME = "🏠";
    public static final String TAB_ICON_SERMONS = "🎥";
    public static final String TAB_ICON_WORSHIP = "🎵";
    public static final String TAB_ICON_FELLOWSHIP = "👥";
    public static final String TAB_ICON_PROFILE = "👤";

    // =========================================================================
    // CANONICAL DEEP LINK ROUTES
    // =========================================================================

    public static final String ROUTE_SCHEME = "perazim";
    public static final String ROUTE_HOST_TABS = "tabs";

    public static final String ROUTE_HOME = "perazim://tabs/home";
    public static final String ROUTE_SERMONS = "perazim://tabs/sermons";
    public static final String ROUTE_WORSHIP = "perazim://tabs/worship";
    public static final String ROUTE_FELLOWSHIP = "perazim://tabs/fellowship";
    public static final String ROUTE_PROFILE = "perazim://tabs/profile";

    public static final String ROUTE_GIVING = "perazim://giving";
    public static final String ROUTE_PRAYER_WALL = "perazim://prayer_wall";
    public static final String ROUTE_DEVOTIONAL = "perazim://devotional";
    public static final String ROUTE_SETTINGS = "perazim://settings";
    public static final String ROUTE_OFFLINE_CACHE = "perazim://offline_cache";

    // =========================================================================
    // TAB DEFINITION OBJECT
    // =========================================================================

    public static class TabItem {
        private final int id;
        private final String title;
        private final String icon;
        private final String route;

        public TabItem(int id, String title, String icon, String route) {
            this.id = id;
            this.title = title;
            this.icon = icon;
            this.route = route;
        }

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getIcon() {
            return icon;
        }

        public String getRoute() {
            return route;
        }

        @Override
        public String toString() {
            return "TabItem{" +
                    "id=" + id +
                    ", title='" + title + '\'' +
                    ", icon='" + icon + '\'' +
                    ", route='" + route + '\'' +
                    '}';
        }
    }

    public static final TabItem TAB_HOME = new TabItem(TAB_ID_HOME, TAB_TITLE_HOME, TAB_ICON_HOME, ROUTE_HOME);
    public static final TabItem TAB_SERMONS = new TabItem(TAB_ID_SERMONS, TAB_TITLE_SERMONS, TAB_ICON_SERMONS, ROUTE_SERMONS);
    public static final TabItem TAB_WORSHIP = new TabItem(TAB_ID_WORSHIP, TAB_TITLE_WORSHIP, TAB_ICON_WORSHIP, ROUTE_WORSHIP);
    public static final TabItem TAB_FELLOWSHIP = new TabItem(TAB_ID_FELLOWSHIP, TAB_TITLE_FELLOWSHIP, TAB_ICON_FELLOWSHIP, ROUTE_FELLOWSHIP);
    public static final TabItem TAB_PROFILE = new TabItem(TAB_ID_PROFILE, TAB_TITLE_PROFILE, TAB_ICON_PROFILE, ROUTE_PROFILE);

    private static final List<TabItem> ALL_TABS;

    static {
        List<TabItem> tabs = new ArrayList<>(5);
        tabs.add(TAB_HOME);
        tabs.add(TAB_SERMONS);
        tabs.add(TAB_WORSHIP);
        tabs.add(TAB_FELLOWSHIP);
        tabs.add(TAB_PROFILE);
        ALL_TABS = Collections.unmodifiableList(tabs);
    }

    public static List<TabItem> getAllTabs() {
        return ALL_TABS;
    }

    public static TabItem getTabById(int id) {
        if (id >= 0 && id < ALL_TABS.size()) {
            return ALL_TABS.get(id);
        }
        return TAB_HOME;
    }
}
