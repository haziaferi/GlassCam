package glasscam.settings.mode;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.sonyremote.sonystuff.XmlElement;
import glasscam.settings.SettingKeys;
import glasscam.utils.XmlUtil;

/**
 * Created by KillerInk on 04.01.2018.
 */

public class GlobalBooleanSettingMode extends AbstractSettingMode implements BooleanSettingModeInterface {

    private boolean value;

    public GlobalBooleanSettingMode(SettingKeys.Key key) {
        super(key);
    }

    @Override
    public boolean get()
    {
        return value;
    }

    @Override
    public void set(boolean bool) {
        this.value = bool;
    }

    @Override
    public String getXmlString() {
        String t = "<setting name = \""+ GlassCamApplication.getStringFromRessources(settingKey.getRessourcesStringID()) +"\" type = \""+ GlobalBooleanSettingMode.class.getSimpleName() +"\">";
        t+= XmlUtil.getTagStringWithValue("value", String.valueOf(value));
        t += "</setting>\r\n";
        return t;
    }

    @Override
    public void loadXmlNode(XmlElement node) {
        set(node.findChild("value").getBooleanValue());
    }
}
