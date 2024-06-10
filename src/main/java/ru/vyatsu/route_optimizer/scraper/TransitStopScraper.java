package ru.vyatsu.route_optimizer.scraper;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Service;
import ru.vyatsu.route_optimizer.bean.Stop;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class TransitStopScraper {
    public Set<Stop> scrapeStops(String url) {
        System.setProperty("webdriver.chrome.driver", "C:\\chrome driver\\chromedriver-win64\\chromedriver.exe");
        WebDriver driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

        Set<Stop> stops = new LinkedHashSet<>();
        try {
            driver.get(url);

            WebElement dropdown = driver.findElement(By.id("id_StopSelect"));
            List<WebElement> stopElements = dropdown.findElements(By.tagName("option"));

            for (WebElement element : stopElements) {
                String value = element.getAttribute("value");

                if (!value.equals("-1")) {
                    String text = element.getText();
                    String[] parts = text.split(" \\(код ");
                    String name = parts[0].trim();
                    stops.add(new Stop(value, name));
                }
            }
        } finally {
            driver.quit();
        }
        return stops;
    }
}
