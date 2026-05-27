package com.urielt.my_final_proj;

import com.vaadin.flow.component.page.AppShellConfigurator; // ודא שהאימפורט הזה קיים
import com.vaadin.flow.component.page.Push;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Push
public class AppMain implements AppShellConfigurator { // התיקון: הוספת מימוש הממשק כאן!

    public static void main(String[] args) {
        SpringApplication.run(AppMain.class, args);
		System.out.println("hello");
    }
}