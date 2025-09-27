# Integrating Your Plugin with Nefor Activator

This walkthrough shows how to hook any Bukkit/Spigot/Paper plugin into the Activator licensing flow. Follow the steps sequentially to ensure protected plugins refuse to start when the Activator license is missing or invalid.

## 1. Add the Activator API dependency

### Gradle (Kotlin DSL)
```kotlin
repositories {
    mavenCentral()
    // Your existing repositories
}

dependencies {
    compileOnly("dev.nefor.activator:activator-api:1.0.0-SNAPSHOT")
}
```

If you prefer shading the API to avoid runtime dependencies, apply the Shadow/Relocate plugin and point it at `dev.nefor.activator.api`.

### Maven
```xml
<dependencies>
    <dependency>
        <groupId>dev.nefor.activator</groupId>
        <artifactId>activator-api</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## 2. Declare Activator as a hard dependency

In your `plugin.yml`:
```yaml
depend: [Activator]
```
Bukkit/Paper will refuse to load your plugin if Activator is missing, giving you a first line of defense.

## 3. Enforce license at runtime

Pick one of the integration strategies below.

### Option A: One-liner guard in `onLoad`
```java
import dev.nefor.activator.api.ActivationGuard;

@Override
public void onLoad() {
    ActivationGuard.guardOrDisable(this, "your-product-id");
}
```
If Activator is absent or the product is not licensed, your plugin is immediately disabled and a `LicenseException` is thrown.

### Option B: Annotate and auto-register
```java
import dev.nefor.activator.api.RequiresLicense;

@RequiresLicense(productId = "your-product-id")
public final class YourPlugin extends JavaPlugin {
    @Override
    public void onLoad() {
        ActivationGuard.guardAnnotatedPlugin(this);
    }
}
```
Add optional `@FeatureFlag(productId = "your-product-id", value = "feature-id")` for granular checks.

### Option C: Subscribe to status changes
```java
import dev.nefor.activator.api.LicenseIntegrationSupport;
import dev.nefor.activator.api.LicenseListener;

@Override
public void onEnable() {
    LicenseIntegrationSupport.integrateAndSubscribe(this, new LicenseListener() {
        @Override public void onLicenseGranted() {
            getLogger().info("License granted; enabling premium features.");
        }

        @Override public void onLicenseRevoked(LicenseStatus reason) {
            getLogger().warning("License revoked: " + reason);
            getServer().getScheduler().runTask(YourPlugin.this, () -> getServer().getPluginManager().disablePlugin(YourPlugin.this));
        }
    });
}
```

## 4. Handle feature gating (optional)

Check flags before exposing premium features:
```java
LicenseService service = LicenseIntegrationSupport.loadServiceOrThrow();
if (service.hasFeature("your-product-id", "analytics")) {
    enableAnalytics();
}
```

## 5. Forward diagnostics if desired

Your plugin can surface Activator state to admins:
```java
LicenseService svc = LicenseIntegrationSupport.loadServiceOrThrow();
svc.getTokenMeta().ifPresent(token -> getLogger().info("License expires at " + token.getExpiresAt()));
```

## 6. Test locally
1. Drop both `activator-plugin-*.jar` and your plugin jar into the server `plugins/` folder.
2. Configure `plugins/Activator/config.yml` with a valid or mocked license.
3. Start the server and ensure your plugin logs the expected messages.
4. Remove/rename the Activator jar and confirm your plugin is blocked during load.

## 7. Packaging recommendations
- Shade the API into your jar if you cannot guarantee servers will ship the same version (`shadowJar` relocate `dev.nefor.activator.api` to your namespace).
- If you keep it `compileOnly`, document the minimum Activator plugin version required.

---

# Publishing the Project to a Git Repository

If you do not have a repository yet, you can create one on GitHub and push this project.

## 1. Create a GitHub repository
1. Sign in to <https://github.com/>.
2. Click **New repository**.
3. Choose a name (e.g. `NeforActivator`), set it to public or private, and leave “Initialize with README” unchecked if you already have one.
4. Click **Create repository** and copy the `git remote` URL shown (HTTPS or SSH).

## 2. Initialize and push from the local project
```powershell
cd C:\Users\dayab\Projects\Minecraft\NeforActivator

git init
git add .
git commit -m "Initial import of NeforActivator"
git remote add origin https://github.com/<your-account>/<repo>.git
git push -u origin main
```

Replace the remote URL with the one GitHub provided. If `main` does not exist yet, Git will create it on push. For SSH URLs, ensure your keys are configured (`ssh-keygen`, add to GitHub).

## 3. Updating the repository
After making changes:
```powershell
git add <files>
git commit -m "Describe the change"
git push
```

Optional: create branches (`git checkout -b feature/license-fix`), push them, and open pull requests on GitHub.
