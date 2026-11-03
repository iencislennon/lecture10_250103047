package task5;

// =============================================================
// Task 5: Decoupled Multi-Cloud Storage Bridge with Access Control Proxy
// Combined Patterns: Bridge Pattern + Proxy Pattern
// =============================================================

// --- 1. Security Context ---
class UserSession {
    private final String username;
    private final String role; // "ADMIN", "ENGINEER", "GUEST"[cite: 10]

    public UserSession(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}

// --- 2. Bridge Pattern: Implementor Interface ---
interface StorageDriver {
    byte[] loadRawBlob(String resourceUri);

    String getStorageType();
}

class S3StorageDriver implements StorageDriver {
    @Override
    public byte[] loadRawBlob(String resourceUri) {
        System.out.printf("[AWS S3 CLIENT] Performing HTTPS GET on bucket uri: s3://%s%n", resourceUri);
        return new byte[] { 0x1F, (byte) 0x8B, 0x08, 0x00 }; // Dummy byte payload[cite: 10]
    }

    @Override
    public String getStorageType() {
        return "AMAZON_S3_CLOUD";
    }
}

class NfsStorageDriver implements StorageDriver {
    @Override
    public byte[] loadRawBlob(String resourceUri) {
        System.out.printf("[NFS POSIX CLIENT] Performing direct IO read on /mnt/nfs/%s%n", resourceUri);
        return new byte[] { 0x4F, 0x67, 0x67, 0x53 }; // Dummy byte payload[cite: 10]
    }

    @Override
    public String getStorageType() {
        return "LOCAL_NFS_CLUSTER";
    }
}

// --- 3. Proxy Pattern: Protective Security & Telemetry Proxy ---
class StorageDriverProxy implements StorageDriver {
    private final StorageDriver realDriver;
    private final UserSession session;

    public StorageDriverProxy(StorageDriver realDriver, UserSession session) {
        if (realDriver == null || session == null) {
            throw new IllegalArgumentException("Dependencies cannot be null");
        }
        this.realDriver = realDriver;
        this.session = session;
    }

    @Override
    public byte[] loadRawBlob(String resourceUri) {
        if ("GUEST".equalsIgnoreCase(session.getRole())) {
            System.out.printf("[PROXY BLOCKED] Access Denied for user '%s' [Role: %s] on resource: %s%n",
                    session.getUsername(), session.getRole(), resourceUri);
            return new byte[0]; // Empty payload on unauthorized access[cite: 11]
        }
        System.out.printf("[PROXY AUDIT] Authorized user '%s' [Role: %s] accessing backend: %s%n",
                session.getUsername(), session.getRole(), realDriver.getStorageType());
        return realDriver.loadRawBlob(resourceUri);
    }

    @Override
    public String getStorageType() {
        return "PROXY_GUARDED(" + realDriver.getStorageType() + ")";
    }
}

// --- 4. Bridge Pattern: Abstraction (DigitalAsset) ---
abstract class DigitalAsset {
    protected final StorageDriver driver; // Bridge reference to Implementor[cite: 11]
    protected final String assetPath;

    public DigitalAsset(StorageDriver driver, String assetPath) {
        this.driver = driver;
        this.assetPath = assetPath;
    }

    public abstract void render();
}

class VideoAsset extends DigitalAsset {
    public VideoAsset(StorageDriver driver, String assetPath) {
        super(driver, assetPath);
    }

    @Override
    public void render() {
        System.out.printf("Initializing Video Player for: %s%n", assetPath);
        byte[] payload = driver.loadRawBlob(assetPath);
        if (payload.length > 0) {
            System.out.printf("SUCCESS: Video playback buffered %d bytes via %s%n", payload.length,
                    driver.getStorageType());
        } else {
            System.out.println("FAILURE: Video buffer empty. Playback aborted.");
        }
    }
}

class AudioAsset extends DigitalAsset {
    public AudioAsset(StorageDriver driver, String assetPath) {
        super(driver, assetPath);
    }

    @Override
    public void render() {
        System.out.printf("Initializing Audio Stream for: %s%n", assetPath);
        byte[] payload = driver.loadRawBlob(assetPath);
        if (payload.length > 0) {
            System.out.printf("SUCCESS: Audio bitstream synchronized %d bytes via %s%n", payload.length,
                    driver.getStorageType());
        } else {
            System.out.println("FAILURE: Audio stream inaccessible.");
        }
    }
}

// --- 5. Verification Runner ---
public class Task5Runner {
    public static void main(String[] args) {
        StorageDriver cloudS3 = new S3StorageDriver();
        StorageDriver localNfs = new NfsStorageDriver();
        
        UserSession adminUser = new UserSession("sufyan", "ADMIN");
        UserSession guestUser = new UserSession("anonymous", "GUEST");

        System.out.println(">>> 1. Authorized Video Playback via S3 (Admin Role) <<<");
        StorageDriver authorizedProxy = new StorageDriverProxy(cloudS3, adminUser);
        DigitalAsset video = new VideoAsset(authorizedProxy, "trailers/hyperion_4k.mp4");
        video.render();

        System.out.println("\n>>> 2. Unauthorized Audio Streaming via NFS (Guest Role - Blocked) <<<");
        StorageDriver blockedProxy = new StorageDriverProxy(localNfs, guestUser);
        DigitalAsset audio = new AudioAsset(blockedProxy, "master_audio/soundtrack.flac");
        audio.render();
    }
}```[cite:10,11,12]