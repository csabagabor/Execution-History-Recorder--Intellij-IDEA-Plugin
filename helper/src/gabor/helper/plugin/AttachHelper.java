package gabor.helper.plugin;

import com.sun.tools.attach.spi.AttachProvider;
import sun.tools.attach.BsdAttachProvider;
import sun.tools.attach.LinuxAttachProvider;
import sun.tools.attach.WindowsAttachProvider;

public class AttachHelper {
    private AttachHelper() {
    }

    public static AttachProvider createLinuxProvider() {
        return new LinuxAttachProvider();
    }

    public static AttachProvider createMacProvider() {
        return new BsdAttachProvider();
    }

    public static AttachProvider createWindowsProvider() {
        return new WindowsAttachProvider();
    }
}
