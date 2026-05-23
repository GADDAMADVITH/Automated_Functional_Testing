import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.TimeoutException;

import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class LaunchTest {

    private static PrintWriter reportWriter;

    public static void main(String[] args) {

        // Initialize report file
        try {
            reportWriter = new PrintWriter(new FileWriter("test_report.txt", false));  // Overwrite file each run
            logToReport("===== TEST EXECUTION STARTED =====");
        } catch (IOException e) {
            System.out.println("Error creating report file: " + e.getMessage());
            return;
        }

        ensureChromeDriverAvailable();

        // Chrome setup – no custom profile to avoid cached issues
        ChromeOptions options = new ChromeOptions();

        // Disable password manager + leak popup
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));  // Safety net for elements
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));  // Longer for reliability

        try {
            driver.manage().window().maximize();

            logToConsoleAndReport("Opening saucedemo.com...");
            driver.get("https://www.saucedemo.com/");
            pause(1000);  // Slow down

            // Negative tests first (multiple scenarios)
            logToConsoleAndReport("Starting negative login tests...");

            // Negative Case 1: Wrong password
            testInvalidLogin(driver, wait, "standard_user", "wrong_password", "should show no match error");

            // Negative Case 2: Locked out user
            testInvalidLogin(driver, wait, "locked_out_user", "secret_sauce", "should show locked out error");

           
            logToConsoleAndReport("Negative tests completed. Now running positive flow...");

            // Positive login and full purchase flow
            logToConsoleAndReport("Performing positive login...");
            WebElement username = wait.until(ExpectedConditions.elementToBeClickable(By.id("user-name")));
            highlightElement(driver, username);
            username.clear();
            username.sendKeys("standard_user");
            pause(1500);

            WebElement password = driver.findElement(By.id("password"));
            highlightElement(driver, password);
            password.clear();
            password.sendKeys("secret_sauce");
            pause(1500);

            WebElement loginBtn = driver.findElement(By.id("login-button"));
            highlightElement(driver, loginBtn);
            loginBtn.click();
            pause(2000);

            // Confirm inventory fully loaded (wait for all 6 items)
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("inventory_container")));
            wait.until(ExpectedConditions.numberOfElementsToBe(By.className("inventory_item"), 6));
            logToConsoleAndReport("Login successful – full inventory loaded");

            // Add items (with per-item try-catch for debug)
            logToConsoleAndReport("Adding items to cart...");
            String[] itemIds = {
                "add-to-cart-sauce-labs-backpack",
                "add-to-cart-sauce-labs-bike-light",
                "add-to-cart-sauce-labs-fleece-jacket"
            };

            for (String itemId : itemIds) {
                try {
                    By locator = By.id(itemId);
                    wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
                    WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(locator));
                    highlightElement(driver, btn);
                    btn.click();
                    logToConsoleAndReport("Added item: " + itemId);
                    pause(1500);
                } catch (TimeoutException te) {
                    logToConsoleAndReport("Timeout adding " + itemId + " – trying fallback locator");
                    By fallback = By.cssSelector("[data-test='" + itemId + "']");
                    WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(fallback));
                    highlightElement(driver, btn);
                    btn.click();
                    pause(1500);
                }
            }

            // Verify cart badge
            WebElement cartBadge = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".shopping_cart_badge")
            ));
            String cartCount = cartBadge.getText();
            logToConsoleAndReport("Items in cart: " + cartCount);  // Should be "3"

            // Go to cart
            logToConsoleAndReport("Opening cart...");
            WebElement cartLink = wait.until(ExpectedConditions.elementToBeClickable(By.className("shopping_cart_link")));
            highlightElement(driver, cartLink);
            cartLink.click();
            pause(2000);

            // Checkout
            logToConsoleAndReport("Proceeding to checkout...");
            WebElement checkoutBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("checkout")));
            highlightElement(driver, checkoutBtn);
            checkoutBtn.click();
            pause(2000);

            // Fill details
            logToConsoleAndReport("Filling customer info...");
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("first-name")));

            WebElement firstName = driver.findElement(By.id("first-name"));
            highlightElement(driver, firstName);
            firstName.sendKeys("Maria");
            pause(1500);

            WebElement lastName = driver.findElement(By.id("last-name"));
            highlightElement(driver, lastName);
            lastName.sendKeys("Shaw");
            pause(1500);

            WebElement postalCode = driver.findElement(By.id("postal-code"));
            highlightElement(driver, postalCode);
            postalCode.sendKeys("500001");
            pause(1500);

            WebElement continueBtn = driver.findElement(By.id("continue"));
            highlightElement(driver, continueBtn);
            continueBtn.click();
            pause(2000);

            // Finish
            logToConsoleAndReport("Finishing order...");
            WebElement finishBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("finish")));
            highlightElement(driver, finishBtn);
            finishBtn.click();
            pause(2000);

            // Success check
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h2[contains(text(),'Thank you for your order!')]")
            ));
            logToConsoleAndReport("PASS: Order completed successfully! (Positive flow)");

        } catch (TimeoutException te) {
            logToConsoleAndReport("TIMEOUT: Element not found/clickable in time.");
            logToConsoleAndReport("Current URL: " + driver.getCurrentUrl());
            te.printStackTrace();
        } catch (Exception e) {
            logToConsoleAndReport("ERROR:");
            logToConsoleAndReport("Current URL: " + driver.getCurrentUrl());
            e.printStackTrace();
        } finally {
            try { Thread.sleep(5000); } catch (Exception ignored) {}  // Pause to see success page
            driver.quit();
            logToConsoleAndReport("===== TEST EXECUTION FINISHED =====");
            reportWriter.close();
        }
    }

    private static void ensureChromeDriverAvailable() {
        // Selenium 4.6+ can use Selenium Manager automatically. This is a fallback for cases where it
        // cannot download/resolve drivers (e.g., network restrictions).
        String alreadySet = System.getProperty("webdriver.chrome.driver");
        if (alreadySet != null && !alreadySet.isBlank()) return;

        Path cwd = Paths.get(System.getProperty("user.dir", "."));
        Path[] candidates = new Path[]{
            // When running from Automation\src (recommended by run scripts)
            cwd.resolve("..").resolve("chromedriver-win64").resolve("chromedriver.exe"),
            // When running from repo root
            cwd.resolve("Automation").resolve("chromedriver-win64").resolve("chromedriver.exe"),
            // When running from Automation folder
            cwd.resolve("chromedriver-win64").resolve("chromedriver.exe")
        };

        for (Path p : candidates) {
            try {
                Path normalized = p.normalize().toAbsolutePath();
                if (Files.exists(normalized)) {
                    System.setProperty("webdriver.chrome.driver", normalized.toString());
                    logToConsoleAndReport("Using local ChromeDriver: " + normalized);
                    return;
                }
            } catch (Exception ignored) {
                // best-effort only
            }
        }

        logToConsoleAndReport("ChromeDriver not set via webdriver.chrome.driver (will try Selenium Manager/PATH).");
    }

    // Method for negative login tests
    private static void testInvalidLogin(WebDriver driver, WebDriverWait wait, String usernameStr, String passwordStr, String expectedOutcome) {
        logToConsoleAndReport("Negative test: " + expectedOutcome + " (user: " + usernameStr + ")");

        driver.navigate().refresh();  // Reset to login page
        pause(1000);

        WebElement username = wait.until(ExpectedConditions.elementToBeClickable(By.id("user-name")));
        highlightElement(driver, username);
        username.clear();
        username.sendKeys(usernameStr);
        pause(1500);

        WebElement password = driver.findElement(By.id("password"));
        highlightElement(driver, password);
        password.clear();
        password.sendKeys(passwordStr);
        pause(1500);

        WebElement loginBtn = driver.findElement(By.id("login-button"));
        highlightElement(driver, loginBtn);
        loginBtn.click();
        pause(2000);

        // Check for error message
        try {
            WebElement errorMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-test='error']")
            ));
            String errorText = errorMsg.getText();
            logToConsoleAndReport("Error displayed: " + errorText);

            // Validate based on expected
            boolean passed = false;
            if (expectedOutcome.contains("locked out") && errorText.contains("locked out")) {
                passed = true;
            } else if (expectedOutcome.contains("no match") && errorText.contains("do not match")) {
                passed = true;
            } else if (expectedOutcome.contains("missing password") && errorText.contains("Password is required")) {
                passed = true;
            }

            if (passed) {
                logToConsoleAndReport("Negative test PASSED: Expected error shown");
            } else {
                logToConsoleAndReport("Negative test FAILED: Unexpected error message");
            }
        } catch (TimeoutException e) {
            logToConsoleAndReport("Negative test FAILED: No error message appeared (unexpected login?)");
        }
    }

    // Highlight element with red border for 1 second
    private static void highlightElement(WebDriver driver, WebElement element) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].style.border='3px solid red'", element);
        try { Thread.sleep(1000); } catch (Exception ignored) {}
        js.executeScript("arguments[0].style.border=''", element);
    }

    // Slow down automation
    private static void pause(long ms) {
        try { Thread.sleep(ms); } catch (Exception ignored) {}
    }

    // Log to both console and report file
    private static void logToConsoleAndReport(String message) {
        System.out.println(message);
        reportWriter.println(message);
    }

    // Log only to report (if needed)
    private static void logToReport(String message) {
        reportWriter.println(message);
    }
}