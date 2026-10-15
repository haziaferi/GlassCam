package glasscam.cam.ui.themenextgen.layoutconfig;

import glasscam.settings.SettingKeys;

public class ManualItemConfig<T>
{
    SettingKeys.Key key;
    T header;
    int color;

    public ManualItemConfig(SettingKeys.Key key, T header)
    {
        this.key = key;
        this.header = header;
    }

    public ManualItemConfig(SettingKeys.Key key, T header, int color)
    {
        this(key,header);
        this.color = color;
    }

    public T getHeader() {
        return header;
    }

    public SettingKeys.Key getKey() {
        return key;
    }


    public int getColor() {
        return color;
    }
}
