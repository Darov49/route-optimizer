package ru.vyatsu.route_optimizer.scraper;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.vyatsu.route_optimizer.bean.Route;
import ru.vyatsu.route_optimizer.bean.StopSchedule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static ru.vyatsu.route_optimizer.constant.StringConstants.*;

@Service
public class RouteScraper {
    private WebDriver driver;

    @Value("${scraper.chromedriver}")
    private String chromeDriverPath;

    private void initializeDriver() {
        if (this.driver == null) {
            System.setProperty("webdriver.chrome.driver", chromeDriverPath);

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


    public List<Route> scrapeRoutes() {
        initializeDriver();
        List<Route> routes = new ArrayList<>();

        try {
            driver.get(CDSVYATKA_URL);
            Thread.sleep(200);

            // Список всех маршрутов
            WebElement dropdown = driver.findElement(By.id(ID_ROUTE_SELECT));
            List<WebElement> routeElements = dropdown.findElements(By.tagName(OPTION));

            // Получение информации о каждом маршруте
            for (WebElement element : routeElements) {
                String value = element.getAttribute(VALUE);
                String text = element.getText();

                // Опция "Все маршруты"
                if (value.equals(INVALID_VALUE) || !text.startsWith(TROLLEYBUS) && !text.startsWith(BUS)) {
                    continue;
                }

                if (text.startsWith(BUS) && !text.matches("Авт \\d{2,}:.*")) {
                    break;
                }

                routes.add(new Route(value, text));
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
            WebElement dropdown = driver.findElement(By.id(ID_ROUTE_SELECT));
            WebElement routeOption = dropdown.findElement(By.xpath("//option[@value='" + routeId + "']"));
            routeOption.click();
            Thread.sleep(200);

            // Список остановок для заданного маршрута
            WebElement stopDropdown = driver.findElement(By.id(ID_STOP_SELECT));
            List<WebElement> stopElements = stopDropdown.findElements(By.tagName(OPTION));

            // Получение информации о всех остановках для данного маршрута
            for (WebElement element : stopElements) {
                String value = element.getAttribute(VALUE);
                String text = element.getText();

                // Опция "Все остановки"
                if (value.equals(INVALID_VALUE)) {
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
            WebElement stopDropdown = driver.findElement(By.id(ID_STOP_SELECT));
            List<WebElement> stopOptions = stopDropdown.findElements(By.tagName(OPTION));

            // Игнорирование опции "Все остановки" и обработка остановок
            for (int i = 2; i <= stopOptions.size(); i++) {
                WebElement stopOption = stopDropdown.findElement(By.xpath(
                        "//*[@id='id_StopSelect']/option[" + i + "]"));
                // Выбор конкретной остановки
                if (stopOption.getAttribute(VALUE).equals(stopId)) {
                    stopOption.click();
                    break;
                }
            }
            Thread.sleep(200);

            // Кнопка "Показать расписание"
            WebElement scheduleButton = driver.findElement(By.id(ID_BUTTON_SCHEDULE));
            scheduleButton.click();
            Thread.sleep(200);

            String routeTableId = SCHEDULE_TABLE_NAME + routeName;

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
