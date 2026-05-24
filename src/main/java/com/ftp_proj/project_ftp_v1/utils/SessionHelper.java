package com.ftp_proj.project_ftp_v1.utils;

import com.vaadin.flow.server.VaadinSession;

public class SessionHelper {
    public static String getSessionID() {
        return VaadinSession.getCurrent().getSession().getId();
    }

    public static Object getAttribute(String key) {
        return VaadinSession.getCurrent().getSession().getAttribute(key);
    }

    public static boolean isAttributeExists(String key) {
        Object obj = VaadinSession.getCurrent().getSession().getAttribute(key);
        if (obj == null)
            return false;
        return true;
    }

    public static void setAttribute(String key, Object val) {
        VaadinSession.getCurrent().getSession().setAttribute(key, val);
    }

    public static void removeAttribute(String key) {
        VaadinSession.getCurrent().getSession().removeAttribute(key);
    }

    public static void invalidate() {
        VaadinSession.getCurrent().getSession().invalidate();
    }

}
