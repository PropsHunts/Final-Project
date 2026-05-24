package com.ftp_proj.project_ftp_v1.utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;

public class RouteHelper {
    
    public static <T extends Component> void navigateTo(Class<T> page) {
        UI.getCurrent().navigate(page); 
    }

    public static void navigateTo(String pageUrl) {
        UI.getCurrent().navigate(pageUrl);
    }
}