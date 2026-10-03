package com.bitejiuyeke.biteportalservice.flash.utils;

import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.biteportalservice.flash.constants.FlashcodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * 对预览首页截图，保存到 user-code/{appId}/screenshot.png。
 *
 * @author Emrys
 */
@Slf4j
public class SeleniumUtil {

    private static final String SCREENSHOT_FILE = "screenshot.png";
    /** 门户镜像构建时安装，版本与 google-chrome 一致。 */
    private static final Path CHROME_DRIVER = Path.of("/usr/local/bin/chromedriver");

    public static Path screenshot(Long appId, String url) {
        if (appId == null) {
            throw new ServiceException("应用ID不能为空");
        }
        if (url == null || url.isBlank()) {
            throw new ServiceException("预览地址不能为空");
        }

        Path appDir = Paths.get(FlashcodeConstant.USER_CODE_DIR, String.valueOf(appId))
                .toAbsolutePath()
                .normalize();
        Path photo = appDir.resolve(SCREENSHOT_FILE).normalize();
        if (!photo.startsWith(appDir)) {
            throw new ServiceException("非法截图路径");
        }

        WebDriver driver = null;
        try {
            Files.createDirectories(appDir);
            driver = openChrome();
            driver.get(url);
            waitPageReady(driver);
            File temp = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(temp.toPath(), photo, StandardCopyOption.REPLACE_EXISTING);
            log.info("首页截图完成, appId={}, url={}, file={}", appId, url, photo);
            return photo;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("首页截图失败, appId={}, url={}", appId, url, e);
            throw new ServiceException("首页截图失败: " + e.getMessage());
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    /**
     * 使用镜像内的 ChromeDriver。未安装时才交给 Selenium Manager，避免容器去外网下载驱动。
     */
    private static ChromeDriver openChrome() {
        ChromeOptions options = chromeOptions();
        if (!Files.isExecutable(CHROME_DRIVER)) {
            return new ChromeDriver(options);
        }
        ChromeDriverService service = new ChromeDriverService.Builder()
                .usingDriverExecutable(CHROME_DRIVER.toFile())
                .build();
        return new ChromeDriver(service, options);
    }

    private static ChromeOptions chromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1280,720");
        options.addArguments("--hide-scrollbars");
        return options;
    }

    private static void waitPageReady(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(webDriver -> "complete".equals(
                String.valueOf(((JavascriptExecutor) webDriver).executeScript("return document.readyState"))));
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
