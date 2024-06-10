package ru.vyatsu.route_optimizer.scraper;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Service;
import ru.vyatsu.route_optimizer.bean.Route;
import ru.vyatsu.route_optimizer.bean.StopSchedule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RouteScraper {

    private WebDriver driver;

    private void initializeDriver() {
        if (this.driver == null) {
            System.setProperty("webdriver.chrome.driver", "C:\\chrome driver\\chromedriver-win64\\chromedriver.exe");

            this.driver = new ChromeDriver();
            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        }
    }

    public void closeDriver() {
        if (this.driver != null) {
            this.driver.quit();
            this.driver = null;
        }
    }


    public List<Route> scrapeRoutes(String url) {
        initializeDriver();
        List<Route> routes = new ArrayList<>();

        try {
            driver.get(url);
            Thread.sleep(200);

            // Список всех маршрутов
            WebElement dropdown = driver.findElement(By.id("id_MarshSelect"));
            List<WebElement> routeElements = dropdown.findElements(By.tagName("option"));

            // Получение информации о каждом маршруте
            for (WebElement element : routeElements) {
                String value = element.getAttribute("value");
                String text = element.getText();

                // Опция "Все маршруты"
                if (value.equals("-1")) {
                    continue;
                }

                if (text.startsWith("Тролл") || (text.startsWith("Авт") && text.matches("Авт \\d{1,2}:.*"))) {
                    routes.add(new Route(value, text));
                } else if (text.startsWith("Авт") && text.matches("Авт \\d{3,}:.*")) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return routes;
    }

    public List<StopSchedule> getStopsForRoute(String routeId) {
        initializeDriver();
        List<StopSchedule> stops = new ArrayList<>();

        try {
            // Выбор конкретного маршрута
            WebElement dropdown = driver.findElement(By.id("id_MarshSelect"));
            WebElement routeOption = dropdown.findElement(By.xpath("//option[@value='" + routeId + "']"));
            routeOption.click();
            Thread.sleep(200);

            // Список остановок для заданного маршрута
            WebElement stopDropdown = driver.findElement(By.id("id_StopSelect"));
            List<WebElement> stopElements = stopDropdown.findElements(By.tagName("option"));

            // Получение информации о всех остановках для данного маршрута
            for (WebElement element : stopElements) {
                String value = element.getAttribute("value");
                String text = element.getText();

                // Опция "Все остановки"
                if (value.equals("-1")) {
                    continue;
                }

                // Имя маршрута
                String[] parts = text.split(" \\(код ");
                String name = parts[0].trim();
                stops.add(new StopSchedule(value, name, new ArrayList<>()));

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return stops;
    }

    public List<String> getScheduleForStop(String stopId, String routeName) throws InterruptedException {
        initializeDriver();
        List<String> schedule = new ArrayList<>();

        try {
            // Список остановок для заданного маршрута
            WebElement stopDropdown = driver.findElement(By.id("id_StopSelect"));
            List<WebElement> stopOptions = stopDropdown.findElements(By.tagName("option"));

            // Игнорирование опции "Все остановки" и обработка остановок
            for (int i = 2; i <= stopOptions.size(); i++) {
                WebElement stopOption = stopDropdown.findElement(By.xpath(
                        "//*[@id='id_StopSelect']/option[" + i + "]"));
                // Выбор конкретной остановки
                if (stopOption.getAttribute("value").equals(stopId)) {
                    stopOption.click();
                    break;
                }
            }
            Thread.sleep(200);

            // Кнопка "Показать расписание"
            WebElement scheduleButton = driver.findElement(By.id("id_buttonRaspis"));
            scheduleButton.click();
            Thread.sleep(200);

            String routeTableId = "tbl_div_marsh_name_" + routeName;

            // Таблица расписания
            List<WebElement> rows = driver.findElements(By.xpath("//*[@id='" + routeTableId
                    + "']//td[@class='minute']"));
            for (WebElement row : rows) {
                schedule.add(row.getText());
            }

            driver.navigate().back();
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return schedule;
    }
}
