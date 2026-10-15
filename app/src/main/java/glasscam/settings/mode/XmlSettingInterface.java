package glasscam.settings.mode;

import glasscam.cam.apis.sonyremote.sonystuff.XmlElement;

/**
 * Created by KillerInk on 04.01.2018.
 */

public interface XmlSettingInterface {
    String getXmlString();
    void loadXmlNode(XmlElement node);
}
